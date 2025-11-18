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

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

final class OTelReporter {

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

    private static final String DEFAULT_KEYSTORE_PASSWORD = "";

    static synchronized void start(OTelConfiguration configuration, String[] arguments) throws Exception {
        String keyStorePassword = DEFAULT_KEYSTORE_PASSWORD;
        if (configuration.keystorePasswordPath != null && !configuration.keystorePasswordPath.isEmpty()) {
            try (Stream<String> lines = Files.lines(Paths.get(configuration.keystorePasswordPath), StandardCharsets.UTF_8)) {
                keyStorePassword = lines.findFirst().orElse("");
            } catch (Exception e) {
                throw new Exception("could not read keystore password.", e);
            }
        }

        KeyManagerFactory keyManagerFactory = null;
        if (configuration.keystorePath != null && !configuration.keystorePath.isEmpty()) {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                try (FileInputStream fis = new FileInputStream(configuration.keystorePath)) {
                    keyStore.load(fis, keyStorePassword.toCharArray());
                }
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
            } catch (Exception e) {
                throw new Exception("could not load keystore.", e);
            }
        }

        X509TrustManager trustManager;
        SSLContext sslContext;
        try {
            trustManager = buildTrustManager(Paths.get(configuration.trustedRootPath));
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                new X509TrustManager[]{trustManager},
                null
            );
        } catch (Exception e) {
            throw new Exception("could not build SSL context.", e);
        }

        final ResourceBuilder resourceBuilder = Resource.builder().put("service.name", configuration.serviceName);

        for (Map.Entry<String, String> label : configuration.labels.entrySet()) {
            resourceBuilder.put(label.getKey(), label.getValue());
        }

        final SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(PeriodicMetricReader.builder(
                                  OtlpGrpcMetricExporter.builder()
                                      .setEndpoint(configuration.endpoint)
                                      .setHeaders(() -> configuration.headers)
                                      .setSslContext(sslContext, trustManager)
                                      .setRetryPolicy(RetryPolicy.getDefault())
                                      .build()
                                  )
                                  .setInterval(configuration.intervalSeconds, TimeUnit.SECONDS)
                                  .build()
            )
            .setResource(resourceBuilder.build())
            .build();

        final OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        Runtime.getRuntime().addShutdownHook(new Thread(sdk::close));
        discoverAndCreateGauges(sdk, configuration.meterPrefix, arguments);
    }

    private static String camelCaseToUnderscores(String camelCase) {
        return camelCase
            .replaceAll("([a-z])([A-Z])", "$1_$2") // Add underscore when lower case is followed by capital
            .toLowerCase();
    }

    private static String meterName(String meterPrefix, String beanName) {
        return camelCaseToUnderscores(meterPrefix + "_" + beanName);
    }

    private static String metricName(String meterPrefix, String beanName, MBeanAttributeInfo attributeInfo) {
        return meterName(meterPrefix, beanName) + "_" + camelCaseToUnderscores(attributeInfo.getName());
    }

    private static void createDoubleGaugeCallback(Meter meterBuilder, 
                                                  String meterPrefix,
                                                  String beanName,
                                                  DynamicSeed dynamicSeed,
                                                  MBeanAttributeInfo attributeInfo ) {
        final String metricName = metricName(meterPrefix, beanName, attributeInfo);
        try {            
            meterBuilder.gaugeBuilder(metricName).buildWithCallback(measurement -> {
                try {
                    Object value = dynamicSeed.getAttribute(attributeInfo.getName());
                    if (value != null) {
                        measurement.record((Double) value);
                    }
                } catch (final Exception e) {
                    System.err.println("Error recording double gauge " + metricName + ": " + e.getMessage());
                }
            });
        } catch (final Exception e) {
            System.err.println("Error creating callback for " + metricName  + ": " + e.getMessage());
        }
    }

    private static void createLongGaugeCallback(Meter meterBuilder,
                                                String meterPrefix,
                                                String beanName,
                                                DynamicSeed dynamicSeed,
                                                MBeanAttributeInfo attributeInfo ) {
        final String metricName = metricName(meterPrefix, beanName, attributeInfo);
        try {            
            meterBuilder.gaugeBuilder(metricName).buildWithCallback(measurement -> {
                try {
                    Object value = dynamicSeed.getAttribute(attributeInfo.getName());
                    if (value != null) {
                        measurement.record((Long) value);
                    }
                } catch (final Exception e) {
                    System.err.println("Error recording long gauge " + metricName + ": " + e.getMessage());
                }
            });
        } catch (final Exception e) {
            System.err.println("Error creating callback for " + metricName  + ": " + e.getMessage());
        }
    }

    private static void createBeanGauges(OpenTelemetrySdk sdk, String meterPrefix, String beanName, String[] attributes) {
        final Meter meterBuilder = sdk.getMeter(meterName(meterPrefix, beanName));
        DynamicSeed dynamicSeed = PolliannaConfiguration.startRecording(beanName, attributes);
        MBeanInfo beanInfo = dynamicSeed.getMBeanInfo();
        for (MBeanAttributeInfo attributeInfo : beanInfo.getAttributes()) {
            final String type = attributeInfo.getType();
            if (type.equals("double") || type.equals("java.lang.Double")) {
                createDoubleGaugeCallback(meterBuilder, meterPrefix, beanName, dynamicSeed, attributeInfo);
            } else if (type.equals("long") || type.equals("java.lang.Long")) {
                createLongGaugeCallback(meterBuilder, meterPrefix, beanName, dynamicSeed, attributeInfo);
            }
        }
    }

    private static void discoverAndCreateGauges(OpenTelemetrySdk sdk, String meterPrefix, String[] arguments) throws Exception {
        final PolliannaArgumentParser parser = PolliannaArgumentParser.parseBeans(arguments, new BiConsumer<String, String[]>() {
            public void accept(String beanName, String[] attributes) {
                createBeanGauges(sdk, meterPrefix, beanName, attributes);
            }
        });
        if (!parser.hasDeclaredBeans()) {
            createBeanGauges(sdk, meterPrefix, PolliannaConfiguration.jvmBeanName(), null);
        }
    }
}
