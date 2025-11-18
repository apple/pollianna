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

import com.apple.pollianna.gc.GcAggregateMXBean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import static com.apple.pollianna.Units.K;
import static com.apple.pollianna.Units.M;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Run this with `-XX:+UseParallelGC -Xms1G -Xmx1G -XX:+UseCompressedOops -XX:MaxTenuringThreshold=0 -XX:NewRatio=3 -XX:NativeMemoryTracking=summary`
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class PolliannaTest {
    private static final double HEAP_SIZE = 1024.0 * M;
    private static final double MIN_LIVE_SET_SIZE = 100.0 * M;
    private static final double MAX_LIVE_SET_SIZE = 200.0 * M;
    private static final double AVERAGE_LIVE_SET_SIZE = (MAX_LIVE_SET_SIZE + MIN_LIVE_SET_SIZE) / 2.0;
    private static final double GARBAGE_SIZE = 50.0 * M;

    private byte[][][] permanent = new byte[(int) MIN_LIVE_SET_SIZE / M][K][K];
    private byte[][][] variable = null;

    private static void pacifySpotBugsUnreadField(Object object) {
    }

    private void makeLiveSetSize(double size) {
        pacifySpotBugsUnreadField(permanent);
        final int n = (int) (size - MIN_LIVE_SET_SIZE) / M;
        if (n <= 0) {
            variable = null;
        } else {
            variable = new byte[n][K][K];
        }
        pacifySpotBugsUnreadField(variable);
    }

    private Object garbage = null;

    private void makeGarbage() {
        pacifySpotBugsUnreadField(garbage);
        for (long i = 0; i < (long) GARBAGE_SIZE / K; i++) {
            garbage = new byte[K];
        }
        garbage = null;
        TestUtil.createMappedBuffer();
        TestUtil.createDirectBuffer();
    }

    private static double delta_percent(double expected, double actual) {
        return 100.0 * Math.abs(actual - expected) / expected;
    }

    private static void doInMillis(long durationMillis, Runnable procedure) throws Exception {
        final long beginMillis = System.currentTimeMillis();
        procedure.run();
        long restMillis = beginMillis + durationMillis - System.currentTimeMillis();
        if (restMillis < 0) {
            throw new Error("operation took longer than expected");
        }
        Thread.sleep(restMillis);
    }

    private static String gcNames() {
        String names = "";
        try {
            List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
                names += gcMxBean.getName();
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exp) {
            throw new RuntimeException(exp);
        }
        return names;
    }

    private static boolean isSingleGenerational() {
        return gcNames().contains("Shenandoah") || gcNames().contains("ZGC");
    }

    // Pacify SpotBugs to allow System.gc()
    private static void collectGarbage() {
        if (System.currentTimeMillis() > 0) { // always true
            System.gc();
        }
    }

    private void warmup() {
        makeLiveSetSize(MIN_LIVE_SET_SIZE);
        collectGarbage();
        makeLiveSetSize(MAX_LIVE_SET_SIZE);
        collectGarbage();
        makeLiveSetSize(MIN_LIVE_SET_SIZE);
        collectGarbage();
        makeLiveSetSize(MAX_LIVE_SET_SIZE);
        collectGarbage();
    }

    //@Test
    public void testIsSerial() {
        assertTrue(gcNames().contains("MarkSweepCompact"));
    }

    private GcAggregateMXBean gcAggregate;

    private long startTimeMillis = System.currentTimeMillis();

    private void startInterval() {
        // These calls demarcate the start of the respective polling interval
        gcAggregate.getOccupancy();
        gcAggregate.getWorkload();
        gcAggregate.getAllocationRate();
        gcAggregate.getPause();
        gcAggregate.getCycle();
        gcAggregate.getDirectMemory();
        gcAggregate.getDirectMemoryUsage();

        startTimeMillis = System.currentTimeMillis();
    }

    @BeforeAll
    public void setUp() {
        Pollianna.start("GcAggregate");
        try {
            final ObjectName objectName = new ObjectName("com.apple.pollianna:type=GcAggregate");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            gcAggregate = JMX.newMXBeanProxy(mbs, objectName,  GcAggregateMXBean.class);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        warmup();
        startInterval();
    }

    @Test
    public void testGc() throws Exception {
        for (int i = 0; i < 4; i++) {
            doInMillis(500, () -> {
                makeLiveSetSize(MIN_LIVE_SET_SIZE);
                makeGarbage();
                collectGarbage();
            });
            doInMillis(500, () -> {
                makeLiveSetSize(MAX_LIVE_SET_SIZE);
                makeGarbage();
                collectGarbage();
            });
        }

        // The following `bean.get...()` calls demarcate the end of the respective polling interval

        DoubleValueRecord occupancy = gcAggregate.getOccupancy();
        assertTrue(occupancy.getMin() > 75.0 * MIN_LIVE_SET_SIZE / HEAP_SIZE);
        assertTrue(occupancy.getMin() < 125.0 * MIN_LIVE_SET_SIZE / HEAP_SIZE);

        assertTrue(occupancy.getAvg() > 75.0 * AVERAGE_LIVE_SET_SIZE / HEAP_SIZE);
        assertTrue(occupancy.getAvg() < 125.0 * AVERAGE_LIVE_SET_SIZE / HEAP_SIZE);

        assertTrue(occupancy.getMax() > 75.0 * MAX_LIVE_SET_SIZE / HEAP_SIZE);
        assertTrue(occupancy.getMax() < 125.0 * MAX_LIVE_SET_SIZE / HEAP_SIZE);

        final double workloadSpace = isSingleGenerational() ? HEAP_SIZE : (double) HEAP_SIZE * 3.0 / 4.0;
        DoubleValueRecord workload = gcAggregate.getWorkload();

        assertTrue(workload.getMin() > 75.0 * MIN_LIVE_SET_SIZE / workloadSpace);
        assertTrue(workload.getMin() < 125.0 * MIN_LIVE_SET_SIZE / workloadSpace);

        assertTrue(workload.getAvg() > 75.0 * AVERAGE_LIVE_SET_SIZE / workloadSpace);
        assertTrue(workload.getAvg() < 125.0 * AVERAGE_LIVE_SET_SIZE / workloadSpace);

        assertTrue(workload.getMax() > 75.0 * MAX_LIVE_SET_SIZE / workloadSpace);
        assertTrue(workload.getMax() < 150.0 * MAX_LIVE_SET_SIZE / workloadSpace);

        double expectedLiveSetRate = (MAX_LIVE_SET_SIZE - MIN_LIVE_SET_SIZE) / M;
        double expectedGarbageRate = 2.0 * GARBAGE_SIZE / M;
        double expectedAverageAllocationRate = expectedLiveSetRate + expectedGarbageRate;
        DoubleValueRecord allocationRate = gcAggregate.getAllocationRate();

        assertTrue(allocationRate.getMin() >= 0.0);
        assertTrue(allocationRate.getMin() <= allocationRate.getAvg());

        assertTrue(allocationRate.getAvg() > 0.4 * expectedAverageAllocationRate);
        assertTrue(allocationRate.getAvg() < 2.5 * expectedAverageAllocationRate);

        assertTrue(allocationRate.getMax() >= allocationRate.getAvg());

        final long runTimeMillis = System.currentTimeMillis() - startTimeMillis;

        final LongDurationRecord pauseTime = gcAggregate.getPause();
        assertTrue(pauseTime.getCount() > 3);
        assertTrue(pauseTime.getMin() <= pauseTime.getAvg());
        assertTrue(pauseTime.getAvg() <= pauseTime.getMax());
        assertTrue(pauseTime.getPortion() * (double) runTimeMillis / 100.0 >= (double) pauseTime.getMax());

        final LongDurationRecord cycleTime = gcAggregate.getCycle();
        assertTrue(cycleTime.getCount() > 3);
        assertTrue(cycleTime.getMin() <= cycleTime.getAvg());
        assertTrue(cycleTime.getAvg() <= cycleTime.getMax());
        assertTrue(cycleTime.getPortion() * (double) runTimeMillis / 100.0 >= (double) cycleTime.getMax());

        final LongValueRecord directMemory = gcAggregate.getDirectMemory();
        assertTrue(directMemory.getMin() > 0);
        assertTrue(directMemory.getMin() < directMemory.getAvg());
        assertTrue(directMemory.getAvg() < directMemory.getMax());
        assertTrue(directMemory.getMax() < gcAggregate.getDirectMemoryLimit());

        final DoubleValueRecord directMemoryUsage = gcAggregate.getDirectMemoryUsage();
        assertTrue(directMemoryUsage.getMin() > 0.0);
        assertTrue(directMemoryUsage.getMin() < directMemoryUsage.getAvg());
        assertTrue(directMemoryUsage.getAvg() < directMemoryUsage.getMax());
        assertTrue(directMemoryUsage.getMax() < 100.0);

        final LongValueRecord metaspace = gcAggregate.getMetaspace();
        assertTrue(metaspace.getMin() > 0);
        assertTrue(metaspace.getMin() < metaspace.getAvg());
        assertTrue(metaspace.getAvg() < metaspace.getMax());
    }
}
