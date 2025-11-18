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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.BiConsumer;

class PolliannaArgumentParser {
    private boolean hasDeclaredBeans = false;
    boolean hasDeclaredBeans() { return hasDeclaredBeans; }

    private boolean isSurveyDisabled = false;

    static final String ARGUMENT_DELIMITER = ";";
    private static final char ATTRIBUTE_LIST_MARKER = '|';
    private static final String ATTRIBUTE_DELIMITER = ",";
    private static final char KEYWORD_MARKER = ':';
    private static final String FILEPATH_KEYWORD = "file";
    private static final String INTERVAL_KEYWORD = "interval";

    private void parse(String[] arguments,
                       BiConsumer<String, String> keywordConsumer,
                       BiConsumer<String, String[]> beanConsumer) {
        if (arguments == null || arguments.length == 0 || (arguments.length == 1 && arguments[0].isEmpty())) {
            return;
        }
        for (String argument : arguments) {
            argument = argument.trim();
            if (argument == null || argument.isEmpty()) {
                continue;
            }
            final int keywordMarkerIndex = argument.indexOf(KEYWORD_MARKER);
            if (keywordMarkerIndex > 0) {
                final String keyword = argument.substring(0, keywordMarkerIndex);
                final String value = argument.substring(keywordMarkerIndex + 1);
                switch (keyword) {
                    case FILEPATH_KEYWORD:
                        if (value == null || value.isEmpty()) {
                            System.err.println("could not read file config");
                            break;
                        }
                        try {
                            final String argumentsFromFile = new String(Files.readAllBytes(new File(value).toPath()), StandardCharsets.UTF_8).trim();
                            parse(argumentsFromFile.split(ARGUMENT_DELIMITER), keywordConsumer, beanConsumer);
                        } catch (IOException e) {
                            throw new RuntimeException("could not read file: " + value, e);
                        }
                        break;
                    case INTERVAL_KEYWORD:
                        if (value == null || value.isEmpty()) {
                            System.err.println("could not read interval config");
                            break;
                        }
                        try {
                            PeriodicAggregator.setIntervalSeconds(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("could not parse interval value: " + value, e);
                        }
                        break;
                    default:
                        if (keywordConsumer != null) {
                            keywordConsumer.accept(keyword, value);
                        }
                        break;
                }
            } else if (argument.equals("DisableSurvey")) {
                isSurveyDisabled = true;
            } else {
                hasDeclaredBeans = true;
                if (beanConsumer != null) {
                    String bean = argument;
                    String[] attributes = null;
                    final int attributeListIndex = argument.indexOf(ATTRIBUTE_LIST_MARKER);
                    if (attributeListIndex > 0) {
                        bean = argument.substring(0, attributeListIndex).trim();
                        attributes = argument.substring(attributeListIndex + 1).split(ATTRIBUTE_DELIMITER);
                    }
                    beanConsumer.accept(bean, attributes);
                }
            }
        }
    }

    private PolliannaArgumentParser(String[] arguments,
                                    BiConsumer<String, String> keywordConsumer,
                                    BiConsumer<String, String[]> beanConsumer) {
        parse(arguments, keywordConsumer, beanConsumer);
    }

    static PolliannaArgumentParser parseKeywords(String[] arguments, BiConsumer<String, String> keywordConsumer) {
        return new PolliannaArgumentParser(arguments, keywordConsumer, null);
    }

    static PolliannaArgumentParser parseBeans(String[] arguments, BiConsumer<String, String[]> beanConsumer) {
        final PolliannaArgumentParser parser =  new PolliannaArgumentParser(arguments, null, beanConsumer);
        if (!parser.hasDeclaredBeans) {
            beanConsumer.accept("Jvm", null);
        }
        if (!parser.isSurveyDisabled) {
            PolliannaConfiguration.startSurveyBean();
        }
        return parser;
    }
}
