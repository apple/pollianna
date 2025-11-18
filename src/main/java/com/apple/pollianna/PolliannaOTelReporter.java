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

import com.apple.pollianna.jvm.JvmSeed;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class PolliannaOTelReporter {
    private static final int DEFAULT_INTERVAL_SECONDS = 60;
    private static final String DEFAULT_KEYSTORE_PASSWORD = "";

    private static final JvmSeed jvmSeed = new JvmSeed();

    private static ObservableDoubleMeasurement jvmGcWorkloadMax;
    private static ObservableDoubleMeasurement jvmGcAllocationRateMax;
    private static ObservableDoubleMeasurement jvmGcPauseMax;
    private static ObservableDoubleMeasurement jvmGcPausePortion;
    private static ObservableDoubleMeasurement jvmDirectMemoryUsageMax;

    public static synchronized void start(
            String serviceName,
            String endpoint,
            String keystorePath,
            String keystorePasswordPath,
            String trustedRootPath,
            Integer intervalSeconds,
            HashMap<String, String> additionalLabels,
            HashMap<String, String> headers) throws Exception {

        String keyStorePw = DEFAULT_KEYSTORE_PASSWORD;
        if (keystorePasswordPath != null && !keystorePasswordPath.isEmpty()) {
            try (Stream<String> lines = Files.lines(Paths.get(keystorePasswordPath), StandardCharsets.UTF_8)) {
                keyStorePw = lines.findFirst().orElse("");
            } catch (Exception e) {
                throw new Exception("could not read keystore password.", e);
            }
        }

        KeyManagerFactory keyManagerFactory = null;
        if (keystorePath != null && !keystorePath.isEmpty()) {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                try (FileInputStream fis = new FileInputStream(keystorePath)) {
                    keyStore.load(fis, keyStorePw.toCharArray());
                }
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePw.toCharArray());
            } catch (Exception e) {
                throw new Exception("could not load keystore.", e);
            }
        }

        X509TrustManager trustManager;
        SSLContext sslContext;
        try {
            trustManager = buildTrustManager(Paths.get(trustedRootPath));
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                new X509TrustManager[]{trustManager},
                null
            );
        } catch (Exception e) {
            throw new Exception("could not build SSL context.", e);
        }

        final ResourceBuilder resourceBuilder = Resource.builder()
                .put("service.name", serviceName);

        for (final Map.Entry<String, String> label : additionalLabels.entrySet()) {
            resourceBuilder.put(label.getKey(), label.getValue());
        }

        final SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(
                                        OtlpGrpcMetricExporter.builder()
                                                .setEndpoint(endpoint)
                                                .setHeaders(() -> headers)
                                                .setSslContext(sslContext, trustManager)
                                                .setRetryPolicy(RetryPolicy.getDefault())
                                                .build()
                                )
                                .setInterval(intervalSeconds != null ? intervalSeconds : DEFAULT_INTERVAL_SECONDS, TimeUnit.SECONDS)
                                .build()
                )
                .setResource(resourceBuilder.build())
                .build();

        final OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk
                .builder()
                .setMeterProvider(sdkMeterProvider)
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(openTelemetrySdk::close));

        final Meter meterBuilder = openTelemetrySdk.getMeter("pollianna_jvm");

        jvmGcWorkloadMax = meterBuilder.gaugeBuilder("pollianna_jvm_gc_workload_max").buildObserver();
        jvmGcAllocationRateMax = meterBuilder.gaugeBuilder("pollianna_jvm_gc_allocation_rate_max").buildObserver();
        jvmGcPauseMax = meterBuilder.gaugeBuilder("pollianna_jvm_gc_pause_max").buildObserver();
        jvmGcPausePortion = meterBuilder.gaugeBuilder("pollianna_jvm_gc_pause_portion").buildObserver();
        jvmDirectMemoryUsageMax = meterBuilder.gaugeBuilder("pollianna_jvm_direct_memory_usage_max").buildObserver();

        jvmSeed.startRecording();
        meterBuilder.batchCallback(() ->
                {
                    jvmGcWorkloadMax.record(jvmSeed.getGcWorkloadMax());
                    jvmGcAllocationRateMax.record(jvmSeed.getGcAllocationRateMax());
                    jvmGcPauseMax.record(jvmSeed.getGcPauseMax());
                    jvmGcPausePortion.record(jvmSeed.getGcPausePortion());
                    jvmDirectMemoryUsageMax.record(jvmSeed.getDirectMemoryUsageMax());
                },
                jvmGcWorkloadMax,
                jvmGcAllocationRateMax,
                jvmGcPauseMax,
                jvmGcPausePortion,
                jvmDirectMemoryUsageMax
        );
    }

    private static X509TrustManager buildTrustManager(Path path) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        final ByteArrayInputStream is = new ByteArrayInputStream(Files.readAllBytes(path));
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");

        for (int i = 0; is.available() > 0; ++i) {
            final X509Certificate cert = (X509Certificate) factory.generateCertificate(is);
            ks.setCertificateEntry("cert_" + i, cert);
        }

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return (X509TrustManager) tmf.getTrustManagers()[0];
    }
}
