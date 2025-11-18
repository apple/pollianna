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

import com.apple.pollianna.LongDurationRecord;
import com.apple.pollianna.DoubleValueRecord;
import com.apple.pollianna.LongValueRecord;

/**
 * Bean implementation for aggregated GC metrics.
 */
public final class GcAggregateSeed extends GcSeed implements GcAggregateMXBean {
    public DoubleValueRecord getAllocationRate() { return aggregator.allocationRate.getRecord(); }
    public DoubleValueRecord getOccupancy() { return aggregator.occupancy.getRecord(); }
    public DoubleValueRecord getWorkload() { return aggregator.workload.getRecord(); }
    public LongDurationRecord getPause() { return aggregator.pause.getRecord(); }
    public LongDurationRecord getCycle() { return aggregator.cycle.getRecord(); }
    public long getDirectMemoryLimit() { return aggregator.directMemory.limit(); }
    public LongValueRecord getDirectMemory() { return aggregator.directMemory.getValueRecord(); }
    public DoubleValueRecord getDirectMemoryUsage() { return aggregator.directMemory.getPercentageRecord(); }
    public LongValueRecord getMetaspace() { return aggregator.metaspace.getValueRecord(); }
    public DoubleValueRecord getMetaspaceUsage() { return aggregator.metaspace.getPercentageRecord(); }
}
