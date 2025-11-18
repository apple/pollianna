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
package com.apple.pollianna.compiler;

import com.apple.pollianna.*;
import com.sun.management.HotSpotDiagnosticMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

public class CodeHeapRecorder {
    private final MemoryPoolMXBean bean;
    private final LongValueRecorder valueRecorder = new LongValueRecorder();
    private final PercentageRecorder percentageRecorder = new PercentageRecorder();
    private final long limit;

    public CodeHeapRecorder(String codeHeapName, String vmOption) {
        bean = Util.getMXBean(MemoryPoolMXBean.class, codeHeapName);
        limit = Util.getLongVMOptionValue(vmOption).orElse(Runtime.getRuntime().maxMemory());
    }

    public void record() {
        if (bean != null) {
            long used = bean.getUsage().getUsed();
            valueRecorder.record(used);
            percentageRecorder.record(used, limit);
        }
    }

    public long limit() {
        return limit;
    }

    public LongValueRecord getValueRecord() {
        return valueRecorder.getRecord();
    }

    public DoubleValueRecord getPercentageRecord() {
        return percentageRecorder.getRecord();
    }

    public long lastValue() {
        return valueRecorder.last();
    }

    public double lastPercentage() {
        return percentageRecorder.last();
    }
}
