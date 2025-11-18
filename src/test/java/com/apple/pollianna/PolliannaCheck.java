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
import com.apple.pollianna.LongValueRecord;
import com.apple.pollianna.Pollianna;
import com.apple.pollianna.DoubleValueRecord;
import com.apple.pollianna.LongDurationRecord;
import com.apple.pollianna.compiler.CompilerAggregateMXBean;
import com.apple.pollianna.gc.GcAggregateMXBean;
import com.apple.pollianna.jvm.JvmMXBean;
import com.apple.pollianna.nmt.NmtAccess;
import com.apple.pollianna.nmt.NmtAggregateMXBean;
import com.apple.pollianna.rt.RtAggregateSeed;
import com.apple.pollianna.rt.RtSampleSeed;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;

/**
 * Main program test class for Pollianna debugging without invoking JUnit.
 * Similar to com.apple.pollianna.PolliannaTest, but with more console output.
 *
 * Suggested JVM arguments to run these checks:
 *  -XX:+UseSerialGC -Xms1G -Xmx1G -verbose:gc -XX:MaxTenuringThreshold=0 -XX:NewRatio=3
 *  -XX:+UseParallelGC -Xms1G -Xmx1G -verbose:gc -XX:MaxTenuringThreshold=0 -XX:NewRatio=3
 *  -XX:+UseConcMarkSweepGC -Xms1G -Xmx1G -verbose:gc -XX:MaxTenuringThreshold=0 -XX:NewRatio=3
 *  -XX:+UseG1GC -Xms1G -Xmx1G -verbose:gc -XX:MaxTenuringThreshold=0 -XX:NewRatio=3
 *  -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms1G -Xmx1G -verbose:gc -XX:ShenandoahInitFreeThreshold=0
 *  -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms1G -Xmx1G -verbose:gc
 *
 * Also add:
 *  -XX:NativeMemoryTracking=summary
 */
final class PolliannaCheck {
    private static final int K = 1024;
    private static final int M = K * K;

    private static final double HEAP_SIZE = 1024.0 * M;
    private static final double MIN_LIVE_SET_SIZE = 100.0 * M;
    private static final double MAX_LIVE_SET_SIZE = 200.0 * M;
    private static final double AVERAGE_LIVE_SET_SIZE = (MAX_LIVE_SET_SIZE + MIN_LIVE_SET_SIZE) / 2.0;
    private static final double GARBAGE_SIZE = 50.0 * M;

    private static final byte[][][] permanent = new byte[(int) MIN_LIVE_SET_SIZE / M][K][K];
    private static byte[][][] variable = null;

    private static void makeLiveSetSize(double size) {
        System.out.println("makeLiveSet: " + size / M);
        final int n = (int) (size - MIN_LIVE_SET_SIZE) / M;
        if (n <= 0) {
            variable = permanent;
        } else {
            variable = new byte[n][K][K];
        }
    }

    static ByteBuffer[] mappedBuffers = new ByteBuffer[1024];
    private static int nMappedBuffers = 0;

    private static void createMappedBuffer() {
        try {
            final Class c = PolliannaCheck.class;
            final Path path = Paths.get(c.getResource(c.getSimpleName() + ".class").getPath()); // Java 8
            final FileChannel fileChannel = (FileChannel) Files.newByteChannel(path, EnumSet.of(StandardOpenOption.READ));
            final ByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            mappedBuffers[nMappedBuffers++ % mappedBuffers.length] = mappedByteBuffer;
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            throw new RuntimeException(e);
        }
    }

    static Object garbage = null;
    static ByteBuffer[] directBuffers = new ByteBuffer[1024];
    private static int nDirectBuffers = 0;

    private static void makeGarbage() {
        System.out.println("makeGarbage: " + GARBAGE_SIZE / M);
        for (long i = 0; i < (long) GARBAGE_SIZE / K; i++) {
            garbage = new byte[K];
        }
        garbage = null;
        directBuffers[nDirectBuffers++ % directBuffers.length] = ByteBuffer.allocateDirect(M);
        createMappedBuffer();
    }

    static boolean singleGenerational() {
        final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        boolean isGenerationalShenandoah = false;
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().contains("Shenandoah Young") || pool.getName().contains("Shenandoah Old")) {
                isGenerationalShenandoah =true;
            }
        }
        if (!isGenerationalShenandoah) {
            for (MemoryPoolMXBean pool : pools) {
                if (pool.getName().contains("Shenandoah") || pool.getName().contains("ZHeap")) {
                    System.out.println("single-generational");
                    return true;
                }
            }
        }
        System.out.println("multi-generational");
        return false;
    }

    static boolean isG1GC() {
        final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().contains("G1")) {
                System.out.println("G1 GC detected");
                return true;
            }
        }
        return false;
    }

    static boolean diminishableYoungGen() {
        final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().contains("ZGC Young Generation") || pool.getName().contains("GenPauseless New Gen")) {
                System.out.println("young generation is diminishable");
                return true;
            }
        }
        return false;
    }

    private static double delta_percent(double expected, double actual) {
        return 100.0 * Math.abs(actual - expected) / expected;
    }

    private static void checkDelta(String name, double expectedDeltaPercentage, double expected, double actual) {
        double d = delta_percent(expected, actual);
        System.out.print(name + " - expected: " + expected + ", actual: " + actual +
                ", expected %d: " + expectedDeltaPercentage + ", actual %d: " + (long) d);
        if (d <= expectedDeltaPercentage) {
            System.out.println(", OK");
        } else {
            System.out.println(", ERROR");
        }
    }

    private static void check(String name, double expected, double actual) {
        checkDelta(name, 25.0, expected, actual);
    }

    private static void checkEqual(String name, double expected, double actual) {
        checkDelta(name, 0.0, expected, actual);
    }

    private static void checkRange(String name, double expectedMin, double expectedMax, double actual) {
        double target = (expectedMin + expectedMax) / 2.0;
        checkDelta(name, delta_percent(target, expectedMin), target, actual);
    }

    static void doInMillis(long durationMillis, Runnable procedure) throws Exception {
        final long beginMillis = System.currentTimeMillis();
        procedure.run();
        long restMillis = beginMillis + durationMillis - System.currentTimeMillis();
        System.out.println("rest millis: " + restMillis);
        if (restMillis < 0) {
            //throw new Error("operation took longer than expected");
        } else {
            Thread.sleep(restMillis);
        }
    }

    // Pacify SpotBugs to allow System.gc()
    private static void collectGarbage() {
        if (System.currentTimeMillis() > 0) { // always true
            System.gc();
        }
    }

    private static void warmup() {
        makeLiveSetSize(MIN_LIVE_SET_SIZE);
        collectGarbage();
        makeLiveSetSize(MAX_LIVE_SET_SIZE);
        collectGarbage();
        makeLiveSetSize(MIN_LIVE_SET_SIZE);
        collectGarbage();
        makeLiveSetSize(MAX_LIVE_SET_SIZE);
        collectGarbage();
        System.out.println("--- end warmup ---");
    }

    /**
     * Exercise the garbage collector and check if Pollianna observes plausible metric values.
     */
    public static void main(String[] args) {
        final double workloadSpace = singleGenerational() || diminishableYoungGen() ?
            HEAP_SIZE : HEAP_SIZE * 3.0 / 4.0;
        try {
            for(GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
                System.out.println("GC: " + gc.getName());
            }

            Pollianna.start("interval:2", "Jvm", "GcAggregate", "CompilerAggregate");

            warmup();

            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName gcObjectName = new ObjectName("com.apple.pollianna:type=GcAggregate");
            final GcAggregateMXBean gcAggregate = JMX.newMXBeanProxy(mbs, gcObjectName,  GcAggregateMXBean.class);
            final ObjectName jvmObjectName = new ObjectName("com.apple.pollianna:type=Jvm");
            final JvmMXBean jvmAggregate = JMX.newMXBeanProxy(mbs, jvmObjectName,  JvmMXBean.class);

            gcAggregate.getOccupancy();
            gcAggregate.getWorkload();
            jvmAggregate.getGcWorkloadMax();
            gcAggregate.getAllocationRate();
            jvmAggregate.getGcAllocationRateMax();
            gcAggregate.getPause();
            jvmAggregate.getGcPauseMax();
            gcAggregate.getCycle();
            gcAggregate.getDirectMemory();
            jvmAggregate.getDirectMemoryUsageMax();
            jvmAggregate.getCodeCacheSegmentUsageMax();
            gcAggregate.getDirectMemoryUsage();
            gcAggregate.getMetaspace();

            final RtAggregateSeed rtAggregate = new RtAggregateSeed();
            rtAggregate.startRecording();

            final RtSampleSeed rtSample = new RtSampleSeed();
            rtSample.startRecording();

            final long startTimeMillis = System.currentTimeMillis();

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
                // This provokes young generation collections
                // to test if Generational Shenandoah ignores events
                // that do not modify the old or global generation usage.
                // This is our way of telling if we are only dealing with
                // a young generation collection that does not modify the old generation.
                // Whereas other collectors present differing GC names to indicate this with certainty,
                // Generational Shenandoah does not.
                for (int j = 0; j < 50; j++) {
                    makeGarbage();
                }
            }

            DoubleValueRecord occupancy = gcAggregate.getOccupancy();
            check("occupancy min", 100.0 * MIN_LIVE_SET_SIZE / HEAP_SIZE, occupancy.getMin());
            check("occupancy average", 100.0 * AVERAGE_LIVE_SET_SIZE / HEAP_SIZE, occupancy.getAvg());
            check("occupancy max", 100.0 * MAX_LIVE_SET_SIZE / HEAP_SIZE, occupancy.getMax());
            System.out.println();

            DoubleValueRecord workload = gcAggregate.getWorkload();
            double workloadMax = jvmAggregate.getGcWorkloadMax();
            check("workload min", 100.0 * MIN_LIVE_SET_SIZE / workloadSpace, workload.getMin());
            check("workload average", 100.0 * AVERAGE_LIVE_SET_SIZE / workloadSpace, workload.getAvg());
            check("workload max", 100.0 * MAX_LIVE_SET_SIZE / workloadSpace, workload.getMax());
            checkEqual("Jvm workload max", workloadMax, workload.getMax());
            System.out.println();

            double expectedLiveSetRate = (MAX_LIVE_SET_SIZE - MIN_LIVE_SET_SIZE) / M;
            double expectedGarbageRate = 2.0 * GARBAGE_SIZE / M;
            double expectedResidualRate = 20.0;
            double expectedAverageAllocationRate = expectedLiveSetRate + expectedGarbageRate + expectedResidualRate;
            DoubleValueRecord allocationRate = gcAggregate.getAllocationRate();
            double allocationRateMax = jvmAggregate.getGcAllocationRateMax();
            checkRange("min rate average", 0.0, allocationRate.getAvg(), allocationRate.getMin());
            checkDelta("allocation rate average", 50.0, expectedAverageAllocationRate, allocationRate.getAvg());
            checkRange("max rate average", allocationRate.getAvg(), 50.0 * allocationRate.getAvg(), allocationRate.getMax());
            checkEqual("Jvm max rate", allocationRateMax, allocationRate.getMax());

            final long runTimeMillis = System.currentTimeMillis() - startTimeMillis;

            final LongDurationRecord pauseTime = gcAggregate.getPause();
            final double pauseTimeMax = jvmAggregate.getGcPauseMax();
            System.out.println("min pause time: " + pauseTime.getMin());
            System.out.println("average pause time: " + pauseTime.getAvg());
            System.out.println("max pause time: " + pauseTime.getMax());
            checkEqual("Jvm max pause time", pauseTimeMax, pauseTime.getMax());
            System.out.println("pause total: " + pauseTime.getPortion() * (double) runTimeMillis / 100.0);
            System.out.println("pause time portion: " + pauseTime.getPortion());
            System.out.println("# pauses: " + pauseTime.getCount());

            final LongDurationRecord cycleTime = gcAggregate.getCycle();
            System.out.println("min cycle time: " + cycleTime.getMin());
            System.out.println("average cycle time: " + cycleTime.getAvg());
            System.out.println("max cycle time: " + cycleTime.getMax());
            System.out.println("cycle total: " + cycleTime.getPortion() * (double) runTimeMillis / 100.0);
            System.out.println("cycle time portion: " + cycleTime.getPortion());
            System.out.println("# cycles: " + cycleTime.getCount());

            System.out.println("GC aggregate direct memory limit: " + gcAggregate.getDirectMemoryLimit());

            LongValueRecord directMemory = gcAggregate.getDirectMemory();
            System.out.println("GC direct memory min: " + directMemory.getMin());
            System.out.println("GC direct memory avg: " + directMemory.getAvg());
            System.out.println("GC direct memory max: " + directMemory.getMax());

            DoubleValueRecord directMemoryUsage = gcAggregate.getDirectMemoryUsage();
            double directMemoryUsageMax = jvmAggregate.getDirectMemoryUsageMax();
            System.out.println("GC direct memory usage min: " + directMemoryUsage.getMin());
            System.out.println("GC direct memory usage avg: " + directMemoryUsage.getAvg());
            System.out.println("GC direct memory usage max: " + directMemoryUsage.getMax());
            checkEqual("Jvm direct memory usage max", directMemoryUsageMax, directMemoryUsage.getMax());

            LongValueRecord metaspace = gcAggregate.getMetaspace();
            System.out.println("GC metaspace min: " + metaspace.getMin());
            System.out.println("GC metaspace avg: " + metaspace.getAvg());
            System.out.println("GC metaspace max: " + metaspace.getMax());

            System.out.println("mapped memory sample: " + rtSample.getMappedMemory());

            if (isG1GC()) {
                // Round two. Observe if we can coax G1 without minimum young gen size
                // into shrinking the young gen size below the initial size,
                // which represents an assumed minimum that can be dynamically falsified.
                // If so, then workload and occupancy should show the same values.
                final double expectedOccupancy = 85.0;
                doInMillis(500, () -> {
                    makeLiveSetSize(HEAP_SIZE * expectedOccupancy / 100.0);
                    makeGarbage();
                    collectGarbage();
                });

                occupancy = gcAggregate.getOccupancy();
                check("occupancy max", expectedOccupancy, occupancy.getMax());

                workload = gcAggregate.getWorkload();
                check("workload max", expectedOccupancy, workload.getMax());
                System.out.println();
            }

            if (!NmtAccess.isAvailable()) {
                System.out.println("NMT not available");
            } else {
                final ObjectName nmtObjectName = new ObjectName("com.apple.pollianna:type=NmtAggregate");
                final NmtAggregateMXBean nmtAggregate = JMX.newMXBeanProxy(mbs, nmtObjectName,  NmtAggregateMXBean.class);

                System.out.println("NMT total reserved: " + nmtAggregate.getTotalReserved().getMax());
                System.out.println("NMT metaspace max %: " + nmtAggregate.getMetaspacePercent().getMax());
            }

            // compilation seed
            final ObjectName compilerObjectName = new ObjectName("com.apple.pollianna:type=CompilerAggregate");
            final CompilerAggregateMXBean compilerAggregate = JMX.newMXBeanProxy(mbs, compilerObjectName, CompilerAggregateMXBean.class);

            System.out.println("Compilation time (ms): " + compilerAggregate.getCompilationTime());
            System.out.println("Non-NMethod code heap size (byte): " + compilerAggregate.getNonNMethodsCodeHeap());
            System.out.println("Non-NMethod code heap limit (byte): " + compilerAggregate.getNonNMethodsCodeHeapLimit());
            System.out.println("Non-NMethod code heap usage (%): " + compilerAggregate.getNonNMethodsCodeHeapUsage());
            System.out.println("Non-profiled NMethod code heap size (byte): " + compilerAggregate.getNonProfiledNMethodsCodeHeap());
            System.out.println("Non-profiled NMethod code heap limit (byte): " + compilerAggregate.getNonProfiledNMethodsCodeHeapLimit());
            System.out.println("Non-profiled NMethod code heap usage (%): " + compilerAggregate.getNonProfiledNMethodsCodeHeapUsage());
            System.out.println("Profiled NMethod code heap size (byte): " + compilerAggregate.getProfiledNMethodsCodeHeap());
            System.out.println("Profiled NMethod code heap limit (byte): " + compilerAggregate.getProfiledNMethodsCodeHeapLimit());
            System.out.println("Profiled NMethod code heap usage (%): " + compilerAggregate.getProfiledNMethodsCodeHeapUsage());
            System.out.println("Code Cache size (byte): " + compilerAggregate.getCodeCache());
            System.out.println("Code Cache limit (byte): " + compilerAggregate.getCodeCacheLimit());
            System.out.println("Code Cache usage (%): " + compilerAggregate.getCodeCacheUsage());

            final double codeCacheSegmentUsageMax = Math.max(
                Math.max(compilerAggregate.getNonNMethodsCodeHeapUsage().getMax(), compilerAggregate.getNonProfiledNMethodsCodeHeapUsage().getMax()),
                Math.max(compilerAggregate.getProfiledNMethodsCodeHeapUsage().getMax(), compilerAggregate.getCodeCacheUsage().getMax())
            );
            checkEqual("code cache segment usage max", codeCacheSegmentUsageMax, jvmAggregate.getCodeCacheSegmentUsageMax());
            
            System.out.println();
        } catch (Exception e) {
            System.out.println("---- Exception: " + e);
            System.out.println("---- cause: " + e.getCause());
        }
    }
}
