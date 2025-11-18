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
package com.apple.pollianna.rt;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import com.apple.pollianna.LongDeltaRecorder;
import com.apple.pollianna.LongValueRecord;

public class RtAggregateSeed extends RtSeed implements RtAggregateMXBean {
    public RtAggregateSeed() { super(); }

    public LongValueRecord getMappedMemory() { return aggregator.mappedMemory.getRecord(); }

    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final LongDeltaRecorder startedThreadRecorder = new LongDeltaRecorder();

    public long getStartedThreadCount() {
        if (threadBean == null) {
            return 0;
        }
        return startedThreadRecorder.record(threadBean.getTotalStartedThreadCount());
    }

    public long getPeakThreadCount() {
        if (threadBean == null) {
            return 0;
        }
        final long result = threadBean.getPeakThreadCount();
        threadBean.resetPeakThreadCount();
        return result;
    }
}
