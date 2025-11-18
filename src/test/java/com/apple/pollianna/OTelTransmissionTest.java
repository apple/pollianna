/*
 * Copyright (c) 2023-2025 Apple Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apple.pollianna;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that uses Pollianna OTel configurations and the full OTel pipeline
 * to send AND receive metrics data and verify end-to-end functionality.
 */
class OTelTransmissionTest {

    private static final String TRUSTED_ROOT_PATH = "src/test/resources/test-root.pem";
    private static final String KEYSTORE_PATH = "src/test/resources/keystore.p12";
    private static final String PASSWORD_PATH = "src/test/resources/keystore.password";
    private static final String METRIC_PREFIX = "otel_test";
    private static final String METRIC_PREFIX_ = METRIC_PREFIX + "_";

    private Server grpcServer;

    private void startGrPcServer() throws Exception {
        grpcServer = ServerBuilder.forPort(0)
            .addService(new MetricsServiceGrpc.MetricsServiceImplBase() {
                @Override
                public void export(ExportMetricsServiceRequest request,
                                   StreamObserver<ExportMetricsServiceResponse> responseObserver) {
                    receivedMetricsData.add(request);
                    metricsReceivedLatch.countDown();
                    
                    // Send success response
                    final ExportMetricsServiceResponse response = ExportMetricsServiceResponse.newBuilder().build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            })
            .build()
            .start();
    }

    private void stopGrPcServer() {
        if (grpcServer != null) {
            grpcServer.shutdown();
            try {
                if (!grpcServer.awaitTermination(5, TimeUnit.SECONDS)) {
                    grpcServer.shutdownNow();
                }
            } catch (InterruptedException e) {
                grpcServer.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private PrintStream originalErr;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void setUp() throws Exception {
        // Capture stderr to monitor OTel setup messages
        originalErr = System.err;
        capturedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capturedErr));
        
        // Set system property for better networking
        System.setProperty("java.net.preferIPv4Stack", "true");
            
        startGrPcServer();
    }

    @AfterEach
    void tearDown() {
        Pollianna.stop();
        stopGrPcServer();
        System.setErr(originalErr);
    }

    private void causeNonZeroHeapMetrics() {
        final java.util.List<byte[]> memoryConsumers = new java.util.ArrayList<>();        
        for (int i = 0; i < 50; i++) {
            memoryConsumers.add(new byte[1024 * 1024]);
        }
        System.gc();
        for (int i = 0; i < 30; i++) {
            memoryConsumers.add(new byte[512 * 1024]);
        }
        System.gc();
    }

    private final List<ExportMetricsServiceRequest> receivedMetricsData = new CopyOnWriteArrayList<>();
    private CountDownLatch metricsReceivedLatch;

    private void resetReceivedData() {
        receivedMetricsData.clear();
        metricsReceivedLatch = new CountDownLatch(1);
    }

    private void receiveMetrics() throws InterruptedException {
        Thread.sleep(3000); // Wait a little for metrics to be collected after GC activity
        final boolean metricsReceived = metricsReceivedLatch.await(10, TimeUnit.SECONDS);
        if (!metricsReceived || receivedMetricsData.isEmpty()) {
            final String stderrOutput = capturedErr.toString();
            System.out.println("Stderr output: " + stderrOutput);
            fail("Expected to receive metrics data, but got none. " +
                 "This indicates the GRPC connection or metrics export failed.");
        }
    }

    private void checkMetric(String metricName, String[] expectedMetricNames, Metric metric, Set<String> valueNames) {
        for (int i = 0; i < expectedMetricNames.length; i++) {
            if (metricName.equals(METRIC_PREFIX_ + expectedMetricNames[i])) {
                if (valueNames.contains(expectedMetricNames[i])) {
                    assertTrue(metric.hasGauge());
                    assertTrue(metric.getGauge().getDataPointsCount() > 0);
                    assertTrue(metric.getGauge().getDataPoints(0).getAsDouble() > 0, "Expected value > 0 for " + metricName);
                }
                expectedMetricNames[i] = null;
                return;
            }
        }
        fail("Unexpected or duplicate metric received: " + metricName);
    }

    private void checkExpectedMetrics(ExportMetricsServiceRequest request, String[] expectedMetricNames, Set<String> valueMetricNames) {
        assertTrue(request.getResourceMetricsCount() > 0, "Should have received metrics");
        
        for (ResourceMetrics resourceMetrics : request.getResourceMetricsList()) {
            for (ScopeMetrics scopeMetrics : resourceMetrics.getScopeMetricsList()) {
                for (Metric metric : scopeMetrics.getMetricsList()) {
                    final String metricName = metric.getName();
                    assertTrue(metricName.startsWith(METRIC_PREFIX_), "Metric name should start with prefix: " + metricName);
                    checkMetric(metricName, expectedMetricNames, metric, valueMetricNames);
                }
            }
        }
        
        // Verify all expected metrics were found
        for (int i = 0; i < expectedMetricNames.length; i++) {
            assertNull(expectedMetricNames[i], "Expected metric not received: " + expectedMetricNames[i]);
        }
    }

    /**
     * Test that OTel sending and receiving works as expected with the default "Jvm" bean.
     * All of the "Jvm" bean metrics must show up and only these.
     */
    @Test
    void testDefaultMetricsTransmission() throws Exception {
        resetReceivedData();
        final String[] args = {
            "otel_prefix:" + METRIC_PREFIX,
            "otel_endpoint:http://localhost:" + grpcServer.getPort(),
            "otel_service_name:pollianna_data_transmission_test",
            "otel_client_keystore:" + KEYSTORE_PATH,
            "otel_client_keystore_password:" + PASSWORD_PATH,
            "otel_trusted_root:" + TRUSTED_ROOT_PATH,
            "otel_headers:X-TEST-WORKSPACE=test-playground,X-TEST-NAMESPACE=integration-test",
            "otel_labels:test=true,environment=junit",
            "otel_interval:2"
            // Not mentioning any beans implicitly activates the "Jvm" bean.
        };
        Pollianna.start(args);
        causeNonZeroHeapMetrics();
        receiveMetrics();

        checkExpectedMetrics(receivedMetricsData.get(0), new String[] { // expected metrics:
            "jvm_gc_workload_max",
            "jvm_gc_allocation_rate_max",
            "jvm_gc_pause_max",
            "jvm_gc_pause_portion",
            "jvm_direct_memory_usage_max",
            "jvm_code_cache_segment_usage_max"
        }, new HashSet<>(Arrays.asList( // for instance, these metrics must have non-zero values:
            "jvm_gc_workload_max",
            "jvm_gc_allocation_rate_max"
        )));
    }

    /**
     * Test that OTel sending and receiving works as expected
     * with explicitly selected beans and attributes.
     * All of the selected metrics must show up and only these.
     */
    @Test
    void testSelectMetricsTransmission() throws Exception {
        resetReceivedData();
        final String[] args = {
            "otel_prefix:" + METRIC_PREFIX,
            "otel_endpoint:http://localhost:" + grpcServer.getPort(),
            "otel_service_name:pollianna_multi_bean_test",
            "otel_client_keystore:" + KEYSTORE_PATH,
            "otel_client_keystore_password:" + PASSWORD_PATH,
            "otel_trusted_root:" + TRUSTED_ROOT_PATH,
            "otel_headers:X-TEST-WORKSPACE=test-playground,X-TEST-NAMESPACE=multi-bean-test",
            "otel_labels:test=true,environment=junit,test_type=multi_bean",
            "otel_interval:2",
            // Multiple different bean and attribute selections:
            "CompilerSample|CompilationTime,ProfiledNMethodsCodeHeapUsage",
            "GcAggregate|PauseMax,WorkloadMin,AllocationRateAvg",
            "Jvm|DirectMemoryUsageMax"
            // Survey bean is started automatically by default
        };
        Pollianna.start(args);
        causeNonZeroHeapMetrics();
        receiveMetrics();
        
        checkExpectedMetrics(receivedMetricsData.get(0), new String[] { // expected metrics:
            "compiler_sample_compilation_time",
            "compiler_sample_profiled_nmethods_code_heap_usage",
            "gc_aggregate_pause_max",
            "gc_aggregate_workload_min",
            "gc_aggregate_allocation_rate_avg",
            "jvm_direct_memory_usage_max"
        }, new HashSet<>(Arrays.asList( // for instance, these metrics must have non-zero values:
            "gc_aggregate_pause_max",
            "gc_aggregate_workload_min",
            "gc_aggregate_allocation_rate_avg"
        )));
    }
}
