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

/**
 * Records `long` sample values and produces the minimum, average, and maximum values
 * of all samples from a polling interval.
 */
public class LongValueRecorder extends LongSampleRecorder {

    public LongValueRecorder() { super(); }

    private long count = 0;
    private long total = 0;
    private long min = 0;
    private long max = 0;

    protected long count() { return count; }

    protected long min() {  return min;  }

    long avg() { return count <= 0 ? last() : total / count; }

    protected long max() { return max; }

    protected long total() { return total; }

    protected synchronized void reset() {
        count = 0;
        total = 0;
        min = last();
        max = last();
    }

    public synchronized void record(long value) {
        super.record(value);
        total += value;
        if (count <= 0 || value < min) {
            min = value;
        }
        if (count <= 0 || value > max) {
            max = value;
        }
        count++;
    }

    /**
     * Start a new polling interval and return a record of the observed sample values from the previous interval.
     *
     * @return a record of the observed sample values since the previous call
     */
    public synchronized LongValueRecord getRecord() {
        final LongValueRecord result = new LongValueRecord(min(), avg(), max());
        reset();
        return result;
    }
}
