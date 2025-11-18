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
package com.apple.pollianna.jvm;

import java.lang.management.PlatformManagedObject;

/**
 * Bean interface for essential JVM metrics, excluding NMT-derived ones.
 */
public interface JvmMXBean extends PlatformManagedObject {
    /**
     * Return the maximum Java heap workload percentage detected by global garbage collections
     * since the previous call to this method.
     * <p>
     * The heap workload is the amount of memory used by live objects
     * compared to the total space available to non-ephemeral objects,
     * which includes all heap regions except those in which brand-new objects are allocated.
     * <p>
     * In terms of garbage collection memory pools, this means that old generation pools
     * and young generation survivor space pools are included in total available workload space,
     * but young generation eden space pools are excluded.
     * <p>
     * Since garbage collection thrashing (back-to-back collections)
     * will occur before heap workload reaches 100%,
     * this percentage may provide an early resource shortage warning
     * for latency-sensitive applications.
     *
     * @return the maximum Java heap workload percentages detected by global garbage collections
     * since the previous call to this method.
     */
    double getGcWorkloadMax();

    /**
     * Return the maximum observed Java object allocation rate between subsequent garbage collections,
     * in Mbytes per second.
     *
     * @return the maximum observed allocation rate between garbage collections, in Mbytes per second.
     */
    double getGcAllocationRateMax();

    /**
     * Return the maximum garbage collection pause duration in milliseconds,
     * since the previous call.
     *
     * "Garbage collection pauses" are time intervals during which the garbage collector
     * causes the application execution to be suspended.
     *
     * @return the maximum garbage collection pause duration in milliseconds
     */
    long getGcPauseMax();

    /**
     * Return the maximum portion of application runtime occupied/displaced by garbage collection pauses,
     * since the previous call.
     *
     * "Garbage collection pauses" are time intervals during which the garbage collector
     * causes the application execution to be suspended.
     *
     * @return the maximum portion of application runtime occupied/displaced by garbage collection pauses
     */
    double getGcPausePortion();

    /**
     * Return the maximum observed usage percentage of the memory available for direct buffers.
     * When a direct memory buffer allocation would lead to exceeding 100%, the JVM will throw `OutOfMemoryError`.
     *
     * @see java.nio.Buffer#isDirect
     * @return the maximum observed usage percentage of the memory available for direct buffers
     */
    double getDirectMemoryUsageMax();

    /**
     * Return the maximum observed usage percentage of any code cache segment or the single-segment legacy code cache,
     * whichever is the highest.
     * When any segment or the legacy code cache reaches near 100% usage,
     * it is probable that subsequent compiled method evictions attempts will fail,
     * due to poor "cache" behavior implementation in the JVM.
     * The JVM will then stop compiling any methods and
     * run increasing amounts of bytecode in interpreted mode,
     * which can lead to significant application slowdown.
     *
     * @return the maximum observed usage percentage of any code cache segment or the single-segment legacy code cache.
     */
    double getCodeCacheSegmentUsageMax();
}
