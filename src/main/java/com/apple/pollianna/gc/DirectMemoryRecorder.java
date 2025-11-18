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
package com.apple.pollianna.gc;

import com.apple.pollianna.LongValuePercentageRecorder;
import com.apple.pollianna.Util;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

public class DirectMemoryRecorder extends LongValuePercentageRecorder {

    public DirectMemoryRecorder() { super(); }

    public static long getDirectMemoryLimit() {
        return Util.getLongVMOptionValue("MaxDirectMemorySize")
                .orElse(Runtime.getRuntime().maxMemory());
    }

    private static final long limit = getDirectMemoryLimit();

    @Override // Thus we do not have to wait for a call to `record()` to retrieve this constant
    public long limit() {
        return limit;
    }

    private final BufferPoolMXBean bean = Util.getMXBean(BufferPoolMXBean.class, "direct");

    public void record() {
        if (bean != null) {
            record(bean.getMemoryUsed(), limit);
        }
    };
}
