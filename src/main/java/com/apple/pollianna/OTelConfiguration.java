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

import java.util.HashMap;
import java.util.function.BiConsumer;

class OTelConfiguration {
    int intervalSeconds = 60;
    String meterPrefix = "pollianna";
    String serviceName, keystorePath, keystorePasswordPath, trustedRootPath, endpoint;
    final HashMap<String, String> labels = new HashMap<>();
    final HashMap<String, String> headers = new HashMap<>();

    private OTelConfiguration() {}

    private static final String OTEL_KEYWORD_PREFIX = "otel_";
    private static final String OTEL_METER_PREFIX_KEYWORD = OTEL_KEYWORD_PREFIX + "prefix";
    private static final String OTEL_SERVICE_NAME_KEYWORD = OTEL_KEYWORD_PREFIX + "service_name";
    private static final String OTEL_ENDPOINT_KEYWORD = OTEL_KEYWORD_PREFIX + "endpoint";
    private static final String OTEL_CLIENT_KEYSTORE_KEYWORD = OTEL_KEYWORD_PREFIX + "client_keystore";
    private static final String OTEL_CLIENT_KEYSTORE_PASSWORD_KEYWORD = OTEL_KEYWORD_PREFIX + "client_keystore_password";
    private static final String OTEL_TRUSTED_ROOT_KEYWORD = OTEL_KEYWORD_PREFIX + "trusted_root";
    private static final String OTEL_HEADERS_KEYWORD = OTEL_KEYWORD_PREFIX + "headers";
    private static final String OTEL_LABELS_KEYWORD = OTEL_KEYWORD_PREFIX + "labels";
    private static final String OTEL_INTERVAL_KEYWORD = OTEL_KEYWORD_PREFIX + "interval";
    private static final String KEY_VALUE_PAIR_DELIMITER = ",";
    private static final char ASSIGNMENT_MARKER = '=';

    private static void parseKeyValuePairs(HashMap<String, String> result, String keyValuePairs) {
        if (keyValuePairs == null) {
            return;
        }
        for (String pair : keyValuePairs.split(KEY_VALUE_PAIR_DELIMITER)) {
            final String[] parts = pair.split(String.valueOf(ASSIGNMENT_MARKER));
            if (parts.length != 2) {
                throw new RuntimeException("could not parse key-value pair: " + pair);
            }
            final String key = parts[0];
            String value = parts[1];
            if (value.startsWith("$")) {
                // Evaluate env vars.
                final String envValue = System.getenv(value.substring(1));
                if (envValue != null) {
                    value = envValue;
                }
            }
            result.put(key, value);
        }
    }

    private void consumeKeyword(String keyword, String value) {
        switch (keyword) {
            case OTEL_METER_PREFIX_KEYWORD:
                meterPrefix = value;
                break;
            case OTEL_SERVICE_NAME_KEYWORD:
                serviceName = value;
                break;
            case OTEL_ENDPOINT_KEYWORD:
                endpoint = value;
                break;
            case OTEL_CLIENT_KEYSTORE_KEYWORD:
                keystorePath = value;
                break;
            case OTEL_CLIENT_KEYSTORE_PASSWORD_KEYWORD:
                keystorePasswordPath = value;
                break;
            case OTEL_TRUSTED_ROOT_KEYWORD:
                trustedRootPath = value;
                break;
            case OTEL_HEADERS_KEYWORD:
                parseKeyValuePairs(headers, value);
                break;
            case OTEL_LABELS_KEYWORD:
                parseKeyValuePairs(labels, value);
                break;
            case OTEL_INTERVAL_KEYWORD:
                if (value == null || value.isEmpty()) {
                    System.err.println("could not read OTel interval config");
                    break;
                }
                try {
                    intervalSeconds = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("could not parse OTel interval value: " + value, e);
                }
                break;
            default:
                throw new IllegalArgumentException("unknown keyword: " + keyword);
        }
    }

    private void check() {
        if (serviceName == null) {
            throw new RuntimeException("OTel service name is required");
        }
        if (endpoint == null) {
            throw new RuntimeException("OTel endpoint is required");
        }
        if (keystorePath == null) {
            throw new RuntimeException("OTel keystore path is required");
        }
        if (trustedRootPath == null) {
            throw new RuntimeException("OTel trusted root path is required");
        }
    }

    private boolean isPopulated = false;

    static OTelConfiguration create(String[] arguments) {
        final OTelConfiguration otel = new OTelConfiguration();
        PolliannaArgumentParser.parseKeywords(arguments, new BiConsumer<String, String>() {
            public void accept(String keyword, String value) {
                otel.consumeKeyword(keyword, value);
                otel.isPopulated = true;
            }
        });
        if (otel.isPopulated) {
            otel.check();
            return otel;
        }
        return null;
    }
}
