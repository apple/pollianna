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

import java.lang.management.ManagementFactory;

public class LongDurationRecorder extends LongValueRecorder {

    public LongDurationRecorder() {
        super();
    }

    private static final long runtimeStartMillis = ManagementFactory.getRuntimeMXBean().getStartTime();

    /**
     * @return elapsed milliseconds since the observed JVM started
     * This conforms with the times that GcInfo.getStartTime() and GcInfo.getEndTime() report.
     */
    private long uptimeMillis() {
        return System.currentTimeMillis() - runtimeStartMillis;
    }

    private long intervalStartMillis = uptimeMillis();
    private double lastPortionValue = 0;

    protected double portion() {
        final long intervalMillis = uptimeMillis() - intervalStartMillis;
        if (intervalMillis <= 1) {
            return lastPortionValue; // Return previous value if under 1ms time has elapsed
        }
        lastPortionValue = ((double) total() / (double) intervalMillis) * 100.0;
        return lastPortionValue;
    }

    @Override
    protected synchronized void reset() {
        super.reset();
        intervalStartMillis = uptimeMillis();
    }

    @Override
    public synchronized LongDurationRecord getRecord() {
        final LongDurationRecord result = new LongDurationRecord(min(), avg(), max(), count(), portion());
        reset();
        return result;
    }
}
