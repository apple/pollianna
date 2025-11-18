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

import com.apple.pollianna.DoubleValueRecord;
import com.apple.pollianna.LongValueRecord;

import java.lang.management.PlatformManagedObject;

/**
 * Bean interface for GC metric samples.
 * Each sample value refers to the respective most recent GC event.
 */
public interface GcSampleMXBean extends PlatformManagedObject {

    /**
     * Return the heap occupancy percentages detected by the most recent global GC.
     * Heap occupancy is the amount of heap memory used by live objects
     * compared to the maximum configured heap size.
     *
     * @return the heap occupancy percentage detected by the most recent global GC
     */
    double getOccupancy();

    /**
     * Return the heap workload percentage detected by the most recent global GC.
     *
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
     * @return athe heap workload percentages detected by the most recent global GC
     */
    double getWorkload();

    /**
     * Return the heap allocation rate between the most recent two subsequent garbage collections, in MiB per second.
     *
     * @return the heap allocation rate between the most recent two subsequent garbage collections, in MiB per second
     */
    double getAllocationRate();

    /**
     * Return the duration of the most recent garbage collection pause, in milliseconds.
     * "Garbage collection pauses" are time intervals during which the garbage collector
     * causes the application execution to be suspended.
     *
     * @return the duration of the most recent garbage collection pause, in milliseconds
     */
    long getPause();

    /**
     * Return the duration of the most recent garbage collection cycle, in milliseconds.
     * For stop-the-world collectors such as Serial and Parallel a garbage collector cycle is identical to a single pause.
     * For partially (CMS, G1) or mostly (Shenandoah, ZGC) concurrent collectors,
     * a "garbage collection cycle" includes one or more garbage collection pauses and
     * any additional time that the garbage collector is running concurrently with the application.
     *
     * @return the duration of the most recent garbage collection cycle, in milliseconds
     */
    long getCycle();

    /**
     * Return the maximum number of bytes that can be occupied by direct buffers.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.ByteBuffer
     *
     * @return the maximum number of bytes that can be occupied by direct buffers
     */
    long getDirectMemoryLimit();

    /**
     * Return the number of bytes used by direct buffers after the most recent global GC.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.ByteBuffer
     *
     * @return the number of bytes occupied by direct buffers after the most recent global GC
     */
    long getDirectMemory();

    /**
     * Return the percentage of used by direct buffers after the most recent global GC.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.ByteBuffer
     *
     * @return the percentage of used by direct buffers after the most recent global GC
     */
    double getDirectMemoryUsage();

    /**
     * Return the number of bytes used in metaspace after the most recent global GC.
     *
     * @return the number of bytes used in metaspace after the most recent global GC
     */
    long getMetaspace();

    /**
     * Return the percentage of bytes used in metaspace after the most recent global GC.
     * This is only accurate if a limit for metaspace has been configured
     * with the JVM command line option '-XX:MaxMetaspaceSize'.
     *
     * @return the percentage of bytes used in metaspace after the most recent global GC
     */
    double getMetaspaceUsage();
}
