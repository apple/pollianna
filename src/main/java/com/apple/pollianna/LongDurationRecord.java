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
 * Holds a summary of duration data.
 * Examples: GC pause or GC cycle durations.
 */
public class LongDurationRecord extends LongValueRecord {
    protected final long count;
    protected final double portion;

    @ConstructorProperties({"min", "avg", "max", "count", "portion"})  // Java 8
    public LongDurationRecord(long min, long avg, long max, long count, double portion) {
        super(min, avg, max);
        this.count = count;
        this.portion = portion;
    }

    /**
     * @return the number of observed events for which a duration was recorded
     */
    public long getCount() { return count; }

    /**
     * @return the portion of the sum of observed durations compared to the elapsed runtime over the same interval
     */
    public double getPortion() { return portion; }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("{min = " + min + ", ");
        buf.append("avg = " + avg + ", ");
        buf.append("max = " + max + ", ");
        buf.append("count = " + count + ", ");
        buf.append("portion = " + portion + "}");
        return buf.toString();
    }
}
