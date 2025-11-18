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

import com.apple.pollianna.jvm.JvmMXBean;

import java.util.function.BiConsumer;

/**
 *
 */
public class Pollianna {
    /**
     * Register JVM observation beans and begin their data recordings and aggregations.
     *
     * If no arguments are given, the "Jvm" MXBean will be started.
     * Any arguments are evaluated left-to-right.
     *
     * If an argument begins with the keyword `file` followed by a colon (':'),
     * then it specifies a file path and all arguments in that file,
     * separated by semicolons (';'), are evaluated.
     * Examples: "file:relative-path/pollianna-arguments.txt", "file:/absolute-path/pollianna-arguments.txt"
     *
     * If an argument begins with the keyword `interval` followed by a colon (':'),
     * then the rest of the argument specifies the interval time in seconds
     * to be used for periodic sampling (of NMT metrics). The default is 10.
     * Example: "interval:5".
     * It is recommended to use this keyword exactly once, as the first argument on the left.
     * Otherwise, beans to its left will remain unaffected.
     *
     * Every other kind of argument specifies a bean name.
     * If that name is followed by a pipe character ('|'),
     * then only the bean attributes listed after the colon will be exposed to JMX.
     * If an attribute has a non-primitive return type then its name has
     * the base name of a getter method in that type as a suffix.
     * Example: if the bean named before the colon has an attribute "Pause"
     * stemming from its getter method `getPause()`,
     * and the return type of `getPause()` has a getter method 'getMax()',
     * then the complete attribute name is "PauseMax".
     *
     * Example bean argument with three selected attributes:
     * "GcAggregate|PauseMax,CycleAvg,AllocationRateMax".
     *
     * If the same bean name is specified multiple times, only the right-most argument referring to it applies.
     *
     * The available beans are: `Jvm`, `GcAggregate`, `GcSample`, `RtAggregate`, `RtSample`,
     * `CompilerAggregate`, `CompilerSample`.
     * If the JDK in use supports NMT data discovery by a dedicated JMX bean,
     * then the `Jvm` bean has an expanded set of attributes that includes NMT-derived metrics and
     * these additional beans are available: `NmtAggregate`, `NmtSample`.
     *
     * @param arguments the arguments with instructions what to start
     * @see JvmMXBean
     */
    public static void start(String... arguments) {
        PolliannaConfiguration.stopAllBeans();
        final OTelConfiguration otelConfiguration = OTelConfiguration.create(arguments);
        if (otelConfiguration != null) {
            try {
                OTelReporter.start(otelConfiguration, arguments);
            } catch (Exception e) {
                throw new RuntimeException("could not start OTel reporter", e);
            }
        } else {
            PolliannaArgumentParser.parseBeans(arguments, new BiConsumer<String, String[]>() {
                public void accept(String beanName, String[] attributes) {
                    PolliannaConfiguration.startBean(beanName, attributes);
                }
            });
        }
    }

    public static void stop() {
        PolliannaConfiguration.stopAllBeans();
    }
}
