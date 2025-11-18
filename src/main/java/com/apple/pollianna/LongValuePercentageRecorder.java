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

public class LongValuePercentageRecorder {

    public LongValuePercentageRecorder() { super(); }

    private final LongValueRecorder valueRecorder = new LongValueRecorder();
    private final PercentageRecorder percentageRecorder = new PercentageRecorder();

    private static long limit;

    public long limit() {
        return limit;
    }

    public void record(long numerator, long denominator) {
        limit = denominator;
        valueRecorder.record(numerator);
        percentageRecorder.record(numerator, denominator);
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
