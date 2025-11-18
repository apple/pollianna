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

import java.beans.ConstructorProperties;

/**
 * Holds the minimum, average, and maximum of a `long` value
 * that has been repeatedly sampled over a polling interval.
 */
public class LongValueRecord {
    protected final long min;
    protected final long avg;
    protected final long max;

    @ConstructorProperties({"min", "avg", "max"}) // Java 8
    public LongValueRecord(long min, long avg, long max) {
        this.min = min;
        this.avg = avg;
        this.max = max;
    }

    public long getMin() { return min; }

    public long getAvg() { return avg; }

    public long getMax() { return max; }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("{min = " + min + ", ");
        buf.append("avg = " + avg + ", ");
        buf.append("max = " + max + " }");
        return buf.toString();
    }
}
