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
package com.apple.pollianna.survey;

import com.apple.pollianna.Aggregator;
import com.apple.pollianna.Seed;

import java.util.LinkedList;
import java.util.List;

public class SurveySeed extends Seed implements SurveySampleMXBean {
    public SurveySeed() { super(); }

    private final static String JVM_VERSION = System.getProperty("java.runtime.version");

    @Override
    protected List<Aggregator> aggregators() {
        return new LinkedList<>();
    }

    public String getJvmVersion() {
        return JVM_VERSION;
    }
}
