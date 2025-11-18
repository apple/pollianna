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

public class RateRecorder extends DoubleValueRecorder {
    private final double amountFactor, millisFactor;

    public RateRecorder(long amountFactor, long millisFactor) {
        super();
        this.amountFactor = amountFactor;
        this.millisFactor = millisFactor;
    }

    private double totalDelta = 0;
    private double totalTime = 0;

    @Override
    public double avg() {
        return count() <= 0 || totalTime <= 0 ? last() : totalDelta / totalTime;
    }

    private long beginMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
    private long beginAmount = 0;

    public synchronized void recordSampleIntervalEnd(long millis, long amount) {
        if (millis <= beginMillis || amount < beginAmount) {
            // concurrent events overlapped, skip this one
            return;
        }
        final double time = (double) (millis - beginMillis) / millisFactor;
        final double delta = (double) (amount - beginAmount) / amountFactor;
        totalTime += time;
        totalDelta += delta;
        super.record(delta / time);
    }

    public synchronized void recordSampleIntervalBegin(long millis, long amount) {
        beginMillis = millis;
        beginAmount = amount;
    }

    @Override
    protected synchronized void reset() {
        super.reset();
        totalDelta = 0;
        totalTime = 0;
    }
}
