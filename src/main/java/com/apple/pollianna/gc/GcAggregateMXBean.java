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

import java.lang.management.PlatformManagedObject;

/**
 * Bean interface for aggregated GC metrics.
 */
public interface GcAggregateMXBean extends PlatformManagedObject {

    /**
     * Return an aggregate (last, min, average, max) of heap occupancy percentages
     * detected by global garbage collections since the previous call to this method.
     * Heap occupancy is the amount of heap memory used by live objects
     * compared to the maximum configured heap size.
     *
     * @return an aggregate of the heap occupancy percentages detected by global garbage collections
     * since the previous call to this method.
     */
    DoubleValueRecord getOccupancy();

    /**
     * Return an aggregate (last, min, average, max) of heap workload percentages
     * detected by global garbage collections since the previous call to this method.
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
     * @return an aggregate of the heap workload percentages detected by global garbage collections
     * since the previous call to this method.
     */
    DoubleValueRecord getWorkload();

    /**
     * Return the Java object allocation rate between subsequent garbage collections, in Mbytes per second.
     *
     * @return the Java object allocation rate between subsequent garbage collections, in Mbytes per second.
     */
    DoubleValueRecord getAllocationRate();

    /**
     * Return a summary of the garbage collection pause durations in milliseconds since the previous call.
     * "Garbage collection pauses" are time intervals during which the garbage collector
     * causes the application execution to be suspended.
     *
     * @return a summary of the garbage collection pause durations in milliseconds since the previous call.
     */
    LongDurationRecord getPause();

    /**
     * Return a summary of the garbage collection cycle durations in milliseconds since the previous call.
     * For stop-the-world collectors such as Serial and Parallel a garbage collector cycle is identical to a single pause.
     * For partially (CMS, G1) or mostly (Shenandoah, ZGC) concurrent collectors,
     * a "garbage collection cycle" includes one or more garbage collection pauses and
     * any additional time that the garbage collector is running concurrently with the application.
     *
     * @return a summary of the garbage collection cycle durations in milliseconds since the previous call.
     */
    LongDurationRecord getCycle();

    /**
     * Return the maximum number of bytes that can be occupied by direct buffers.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.ByteBuffer
     *
     * @return the maximum number of bytes that can be occupied by direct buffers
     */
    long getDirectMemoryLimit();

    /**
     * Return an aggregate (last, min, average, max) of the number of bytes used by direct buffers after global GC.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.ByteBuffer
     *
     * @return an aggregate (last, min, average, max) of the number of bytes occupied by direct buffers after global GC
     */
    LongValueRecord getDirectMemory();

    /**
     * Return an aggregate (last, min, average, max) percentage of used by direct buffers after global GC.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.ByteBuffer
     *
     * @return an aggregate (last, min, average, max) percentage of used by direct buffers after global GC
     */
    DoubleValueRecord getDirectMemoryUsage();

    /**
     * Return an aggregate (last, min, average, max) of the number of bytes used in metaspace after the most recent global GC.
     *
     * @return an aggregate (last, min, average, max) of the number of bytes used in metaspace after the most recent global GC
     */
    LongValueRecord getMetaspace();

    /**
     * Return an aggregate (last, min, average, max) percentage of bytes used in metaspace after the most recent global GC.
     * This is only accurate if a limit for metaspace has been configured
     * with the JVM command line option '-XX:MaxMetaspaceSize'.
     *
     * @return an aggregate (last, min, average, max) percentage of bytes used in metaspace after the most recent global GC
     */
    DoubleValueRecord getMetaspaceUsage();
}
