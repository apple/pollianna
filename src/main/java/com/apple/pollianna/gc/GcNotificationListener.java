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

import com.apple.pollianna.PercentageRecorder;
import com.apple.pollianna.RateRecorder;
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gathers GarbageCollectorRecorder input from garbage collection notifications by a GarbageCollectorMXBean.
 */
final class GcNotificationListener implements NotificationListener {

    // A GC pool is a memory pool that forms part of the Java heap and holds Java objects.
    private final Map<String, Set<String>> gcNameToPools = new HashMap<String, Set<String>>();

    private void registerGcPools(String gcName, String... pools) {
        gcNameToPools.put(gcName, new HashSet<String>(Arrays.asList(pools)));
    }

    // A "global" GC traverses all live objects and can thus assess the live set.
    private final Set<String> globalGcNames = new HashSet<String>();

    private void registerGlobalGcPools(String gcName, String... pools) {
        registerGcPools(gcName, pools);
        globalGcNames.add(gcName);
    }

    // Referenced more than once:
    private static final String G1_OLD_GEN_POOL = "G1 Old Gen";
    private static final String SHENANDOAH_YOUNG_GEN_POOL = "Shenandoah Young Gen";
    private static final String SHENANDOAH_OLD_GEN_POOL = "Shenandoah Old Gen";
    private static final String ZGC_YOUNG_GEN_POOL = "ZGC Young Generation";
    private static final String C4_YOUNG_GEN_POOL = "GenPauseless New Gen";

    GcNotificationListener() {
        // -XX:+UseSerialGC
        final String[] serialPools = {"Tenured Gen", "Survivor Space", "Eden Space"};
        registerGcPools("Copy", serialPools);
        registerGlobalGcPools("MarkSweepCompact", serialPools);

        // -XX:+UseParallelGC
        final String[] parallelPools = {"PS Old Gen", "PS Survivor Space", "PS Eden Space"};
        registerGcPools("PS Scavenge", parallelPools);
        registerGlobalGcPools("PS MarkSweep", parallelPools);

        // -XX:+UseConcMarkSweepGC
        final String[] cmsPools = {"CMS Old Gen", "Par Survivor Space", "Par Eden Space"};
        registerGcPools("ParNew", cmsPools);
        registerGlobalGcPools("ConcurrentMarkSweep", cmsPools);

        // -XX:+UseG1GC
        final String[] g1Pools = {G1_OLD_GEN_POOL, "G1 Survivor Space", "G1 Eden Space"};
        registerGlobalGcPools("G1 Young Generation", g1Pools);
        registerGlobalGcPools("G1 Old Generation", g1Pools);

        // -XX:+UseShenandoahGC
        final String[] shenandoahPools = {"Shenandoah", SHENANDOAH_YOUNG_GEN_POOL, SHENANDOAH_OLD_GEN_POOL};
        registerGlobalGcPools("Shenandoah Cycles", shenandoahPools);

        // -XX:+UseZGC in JDK 11 (experimental)
        registerGlobalGcPools("ZGC", "ZHeap");

        // -XX:+UseZGC in JDK 17 or later
        registerGlobalGcPools("ZGC Cycles", "ZHeap");

        // -XX:+UseZGC -XX:+ZGenerational in JDK 21 or later
        final String[] genZgcPools = {"ZGC Old Generation", ZGC_YOUNG_GEN_POOL};
        registerGcPools("ZGC Minor Cycles", genZgcPools);
        registerGlobalGcPools("ZGC Major Cycles", genZgcPools);

        // C4 in Zing
        final String[] zingPools = {"GenPauseless Old Gen", C4_YOUNG_GEN_POOL};
        registerGcPools("GPGC New", zingPools);
        registerGlobalGcPools("GPGC Old", zingPools);
    }

    // Names of collections algorithms that indicate an application execution pause
    private final Set<String> pauseGcNames = new HashSet<String>(Arrays.asList(
            "Copy", "MarkSweepCompact",                 // -XX:+UseSerialGC
            "PS MarkSweep", "PS Scavenge",              // -XX:+UseParallelGC
            "ParNew", "ConcurrentMarkSweep",            // -XX:+UseConcMarkSweepGC
            "G1 Young Generation", "G1 Old Generation", // -XX:+UseG1GC
            "Shenandoah Pauses",                        // -XX:+UseShenandoahGC
            "ZGC Pauses",                               // -XX:+UseZGC, in JDK 17 or later
            "ZGC Minor Pauses",                         // -XX:+UseZGC -XX:+ZGenerational, in JDK 21 or later
            "ZGC Major Pauses"));                       // -XX:+UseZGC -XX:+ZGenerational, in JDK 21 or later

    private void recordAllocationRate(RateRecorder recorder, String gcName, GcInfo gcInfo) {
        final Set<String> usagePools = gcNameToPools.get(gcName);
        long usedBytesBeforeGc = 0;
        for (String pool : usagePools) {
            final MemoryUsage usage = gcInfo.getMemoryUsageBeforeGc().get(pool);
            if (usage != null) {
                usedBytesBeforeGc += usage.getUsed();
            }
        }
        long usedBytesAfterGc = 0;
        for (String pool : usagePools) {
            final MemoryUsage usage = gcInfo.getMemoryUsageAfterGc().get(pool);
            if (usage != null) {
                usedBytesAfterGc += usage.getUsed();
            }
        }
        recorder.recordSampleIntervalEnd(gcInfo.getStartTime(), usedBytesBeforeGc);
        recorder.recordSampleIntervalBegin(gcInfo.getEndTime(), usedBytesAfterGc);
    }

    // Occupancy: usage/max percentage in all usage pools combined,
    // after a collection that has established a live set.
    private void recordOccupancy(PercentageRecorder occupancyRecorder, String gcName, GcInfo gcInfo) {
        long usedOccupancyBytes = 0;
        long maxOccupancyBytes = 0;
        for (String pool : gcNameToPools.get(gcName)) {
            final MemoryUsage usage = gcInfo.getMemoryUsageAfterGc().get(pool);
            if (usage != null) {
                usedOccupancyBytes += usage.getUsed();
                if (pool.equals(ZGC_YOUNG_GEN_POOL) ||
                    pool.equals(SHENANDOAH_YOUNG_GEN_POOL) ||
                    pool.equals(C4_YOUNG_GEN_POOL)) {
                    // Generational ZGC, Generational Shenandoah, and C4
                    // report the whole heap as max size for each generation.
                    // By skipping one of the two we arrive at the correct combined max value.
                } else {
                    maxOccupancyBytes += usage.getMax();
                }
            }
        }
        occupancyRecorder.record(usedOccupancyBytes, maxOccupancyBytes);
    }

    // We assume that the young gen has a fixed minimum size
    // until proven otherwise by comparing its current sizes over time.
    boolean isMinYoungGenSizeFixed = true;
    long minYoungGenSize = 0L;

    private long getMinYoungGenSize(long youngGenSize) {
        if (youngGenSize > minYoungGenSize) {
            if (minYoungGenSize == 0L) {
                minYoungGenSize = youngGenSize;
            }
        } else if (youngGenSize < minYoungGenSize) {
            // Changing minimum young gen size detected.
            isMinYoungGenSizeFixed = false;

            // Henceforth, we will report occupancy as workload.
            return 0L;
        }
        return minYoungGenSize;
    }

    // Workload: usage/max percentage in all workload pools combined,
    // after a collection that has established a live set.
    // Unless minimum young gen size has been determined to be variable.
    // Then we report occupancy as workload.
    //
    // Workload pools happen to be the same that serve as live set indicator pools,
    // so we can refer to `gcNameToLiveSetPools` below to account for workload pools.
    private void recordWorkload(PercentageRecorder workloadRecorder, String gcName, GcInfo gcInfo) {
        long usedWorkloadBytes = 0;
        long maxWorkloadBytes = 0;
        for (String pool : gcNameToPools.get(gcName)) {
            final MemoryUsage usage = gcInfo.getMemoryUsageAfterGc().get(pool);
            if (usage != null) {
                if (pool.startsWith("G1 Eden")) {
                    if (isMinYoungGenSizeFixed) {
                        // We have to work around OpenJDK bug https://bugs.openjdk.org/browse/JDK-8202793:
                        // survivor and Eden space sizes are unknown. They are always reported as `-1`.
                        // AND both are already included in "old gen size".
                        // Eden space size is available as the amount of dedicated committed memory,
                        // because all of Eden gets to be used before every young collection.
                        maxWorkloadBytes -= getMinYoungGenSize(usage.getCommitted());
                    }
                } else {
                    usedWorkloadBytes += usage.getUsed();
                    if (pool.equals(ZGC_YOUNG_GEN_POOL) ||
                        pool.equals(SHENANDOAH_YOUNG_GEN_POOL) ||
                        pool.equals(C4_YOUNG_GEN_POOL)) {
                        // Generational ZGC, Generational Shenandoah, and C4
                        // report the whole heap as max size for each generation.
                        // By skipping one of the two we arrive at the correct combined max value.
                    } else {
                        maxWorkloadBytes += usage.getMax();
                        if (pool.contains("Eden")) {
                            if (isMinYoungGenSizeFixed) {
                                maxWorkloadBytes -= getMinYoungGenSize(usage.getMax());
                            }
                        }
                    }
                }
            }
        }
        workloadRecorder.record(usedWorkloadBytes, maxWorkloadBytes);
    }

    boolean isG1OldGenAfterGcUsageLower = false;
    private long previousG1OldGenAfterGcUsage = 0;

    private boolean hasG1OldGenBeenCollected(GcInfo gcInfo) {
        final Map<String, MemoryUsage> afterGc = gcInfo.getMemoryUsageAfterGc();
        return afterGc.keySet().contains(G1_OLD_GEN_POOL) && (isG1OldGenAfterGcUsageLower ||
            afterGc.get(G1_OLD_GEN_POOL).getUsed() < gcInfo.getMemoryUsageBeforeGc().get(G1_OLD_GEN_POOL).getUsed());
    }

    private boolean isShenandoahYoungCollection(GcInfo gcInfo) {
        final Map<String, MemoryUsage> afterGc = gcInfo.getMemoryUsageAfterGc();
        if (!afterGc.containsKey(SHENANDOAH_YOUNG_GEN_POOL)) {
            // This must be single-generational Shenandoah
            return false;
        }
        final MemoryUsage afterGcUsage = afterGc.get(SHENANDOAH_OLD_GEN_POOL);
        if (afterGcUsage == null) {
            // Should not happen, but we checked anyway, for null safety below
            return true;
        }
        final MemoryUsage beforeGcUsage = gcInfo.getMemoryUsageBeforeGc().get(SHENANDOAH_OLD_GEN_POOL);
        if (beforeGcUsage == null) {
            // Should not happen, but we checked anyway, for null safety below
            return true;
        }
        // True if old gen has not been collected
        return beforeGcUsage.getUsed() <= afterGcUsage.getUsed();
    }

    @Override
    public synchronized void handleNotification(Notification notification, Object handBack) {
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }

        final CompositeData compositeData = (CompositeData) notification.getUserData();
        final GarbageCollectionNotificationInfo notificationInfo = GarbageCollectionNotificationInfo.from(compositeData);
        final String gcName = notificationInfo.getGcName();
        final GcInfo gcInfo = notificationInfo.getGcInfo();
        final GcAggregator aggregator = (GcAggregator) handBack;

        if (pauseGcNames.contains(gcName)) {
            aggregator.pause.record(gcInfo.getDuration());
        }
        if (gcNameToPools.containsKey(gcName)) {
            aggregator.cycle.record(gcInfo.getDuration());
            recordAllocationRate(aggregator.allocationRate, gcName, gcInfo);
            if (gcName.contains("G1")) {
                final MemoryUsage usage = gcInfo.getMemoryUsageAfterGc().get(G1_OLD_GEN_POOL);
                if (usage != null) {
                    isG1OldGenAfterGcUsageLower = usage.getUsed() < previousG1OldGenAfterGcUsage;
                    previousG1OldGenAfterGcUsage = usage.getUsed();
                }
            }
            if (globalGcNames.contains(gcName)) {
                if (!gcName.equals("G1 Young Generation") || hasG1OldGenBeenCollected(gcInfo)) {
                    if (!isShenandoahYoungCollection(gcInfo)) {
                        aggregator.directMemory.record();
                        aggregator.metaspace.record();
                        recordOccupancy(aggregator.occupancy, gcName, gcInfo);
                        recordWorkload(aggregator.workload, gcName, gcInfo);
                    }
                }
            }
        }
    }
}
