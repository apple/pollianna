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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import com.apple.pollianna.LongValuePercentageRecorder;
import com.apple.pollianna.Util;

public class MetaspaceRecorder extends LongValuePercentageRecorder {

    public MetaspaceRecorder() { super(); }

    private final MemoryPoolMXBean bean = Util.getMXBean(MemoryPoolMXBean.class, "Metaspace");

    public void record() {
        if (bean != null) {
            long limit = bean.getUsage().getMax();
            if (limit < 0) {
                // No metaspace limit configured.
                // The default is unlimited.
                // As a proxy, we use all committed non-Java-heap memory,
                // which is a gross overestimate that may change dynamically.
                limit = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted();
            }
            record(bean.getUsage().getUsed(), limit);
        }
    };
}
