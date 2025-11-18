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

/**
 * Test application class that attempts to load OpenTelemetry classes reflectively
 * to test whether they are visible on the classpath,
 * validating Pollianna dependency isolation.
 */
public class JarTestApp {

    private static final String[] OTEL_CLASSES = {
        "io.opentelemetry.api.OpenTelemetry",
        "io.opentelemetry.sdk.OpenTelemetrySdk",
        "io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter"
    };

    public static void main(String[] args) {
        boolean foundAnyOtelClass = false;

        for (String className : OTEL_CLASSES) {
            try {
                Class.forName(className);
                foundAnyOtelClass = true;
            } catch (ClassNotFoundException e) {
            }
        }

        if (foundAnyOtelClass) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}
