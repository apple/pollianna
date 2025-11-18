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

/**
 * Bean implementation for GC metric samples.
 */
public final class GcSampleSeed extends GcSeed implements GcSampleMXBean {
    public double getOccupancy() { return aggregator.occupancy.last(); }
    public double getWorkload() { return aggregator.workload.last(); }
    public double getAllocationRate() { return aggregator.allocationRate.last(); }
    public long getPause() { return aggregator.pause.last(); }
    public long getCycle() { return aggregator.cycle.last(); }
    public long getDirectMemoryLimit() { return aggregator.directMemory.limit(); }
    public long getDirectMemory() { return aggregator.directMemory.lastValue(); }
    public double getDirectMemoryUsage() { return aggregator.directMemory.lastPercentage(); }
    public long getMetaspace() { return aggregator.metaspace.lastValue(); }
    public double getMetaspaceUsage() { return aggregator.metaspace.lastPercentage(); }
}
