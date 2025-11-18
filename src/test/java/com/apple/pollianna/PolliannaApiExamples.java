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
import com.apple.pollianna.PeriodicAggregator;
import com.apple.pollianna.Pollianna;
import com.apple.pollianna.DoubleValueRecord;
import com.apple.pollianna.compiler.CompilerAggregateSeed;
import com.apple.pollianna.compiler.CompilerSampleSeed;
import com.apple.pollianna.gc.GcAggregateSeed;
import com.apple.pollianna.gc.GcSampleSeed;
import com.apple.pollianna.jvm.JvmSeed;
import com.apple.pollianna.nmt.NmtAccess;
import com.apple.pollianna.nmt.NmtAggregateSeed;
import com.apple.pollianna.nmt.NmtSampleSeed;
import com.apple.pollianna.rt.RtAggregateSeed;
import com.apple.pollianna.rt.RtSampleSeed;

/**
 * Illustrates how to use Pollianna APIs to obtain JVM metrics.
 *
 * Methods in the `Pollianna` class registers JMX beans,
 * so that JVM metrics can be retrieved from JMX.
 * Such JMX beans are singletons, one per bean name (e.g., "GcAggregate", "NmtSample").
 *
 * Alternatively, metric values can be queried by method calls on "seed" objects created by constructors,
 * and then it is up to the users how to further disseminate them.
 * Such seeds are not singletons.
 * Each will provide its own independent JVM observations.
 * However, at most one per seed class is expected to be needed.
 *
 * A "seed" is the inside of a "bean", just without the shell, the JMX wrapping.
 * Both approaches can be used simultaneously.
 */
public final class PolliannaApiExamples {

    // Helpers for creating some non-zero output

    private static byte[][] persistentStuff1 = new byte[1000][1000];
    private static byte[][] persistentStuff2 = new byte[1000][1000];

    private static void createGarbage() {
        for (int i = 0; i < 100; i++) {
            final byte[][] ephemeralStuff = new byte[1000][1000];;
            persistentStuff2 = ephemeralStuff;
        }
    }

    /**
     * Examples of starting JMX beans.
     *
     * For additional information and a full explanation of the syntax for the `start()` method
     * see `Pollianna.start()`.
     *
     * @see Pollianna#start
     */
    static void jmxExamples() {
        // Simplest.
        // This starts GC aggregating and publishes select JMX metrics by a `JvmMXBean`.
        // If available in the JDK version in use, select NMT aggregating is included.
        Pollianna.start();

        // Same as the above default, by explicit bean choice.
        Pollianna.start("Jvm");

        // Start the GcAggregate bean and the NmtSample bean, with a few select attributes.
        Pollianna.start("GcAggregate|WorkloadAvg,AllocationRateAvg,GcPausePortion;NmtSample|ThreadStackReserved;RtSample|DirectMemoryMax");

        // Stop all reporting and aggregating for all Pollianna beans created by `start()` and unregister them from JMX.
        // This does not affect non-bean, non-JMX seeds created by constructors.
        Pollianna.stop();
    }

    // Non-JMX metric access by API calls.
    // None of these calls register any JMX beans.

    static void intervalExample() {
        // Change the NMT recording interval to 1 second.
        PeriodicAggregator.setIntervalSeconds(2);
        // Once any seed or JMX bean starts recording, the rate is fixed and subsequent calls have no effect.
    }

    static void jvmExamples() {
        // Just get non-NMT data
        final JvmSeed jvmSeed = new JvmSeed();
        jvmSeed.startRecording();
        System.out.println("Jvm - GC pause % of runtime: " + jvmSeed.getGcPausePortion());
        System.out.println("Jvm - Max % of available direct memory used: " + jvmSeed.getDirectMemoryUsageMax());
    }

    static void gcSampleExample() {
        final GcSampleSeed gcSampling = new GcSampleSeed();
        gcSampling.startRecording(); // Start listening to GC events

        // This is only here to create some non-zero example data
        System.gc();
        createGarbage();
        System.gc();
        createGarbage();
        System.gc();
        createGarbage();
        System.gc();

        final double allocationRate = gcSampling.getAllocationRate();
        System.out.println("GcSample - Sampled Java object allocation rate (MiB/s): " + allocationRate);

        gcSampling.stopRecording(); // Stop listening to GC events
    }

    static void gcAggregateExample() {
        final GcAggregateSeed gcAggregating = new GcAggregateSeed();
        gcAggregating.startRecording(); // Start listening to GC events

        // This is only here to create some non-zero example data
        System.gc();
        createGarbage();
        System.gc();
        createGarbage();
        System.gc();

        final DoubleValueRecord workload = gcAggregating.getWorkload();
        System.out.println("GcAggregate - Java heap workload % min: " + workload.getMin());
        System.out.println("GcAggregate - Java heap workload % avg: " + workload.getAvg());
        System.out.println("GcAggregate - Java heap workload % max: " + workload.getMax());

        gcAggregating.stopRecording(); // Stop listening to GC events
    }

    static void isNmtAvaliableExample() {
        final boolean isNmtAvailable = NmtAccess.isAvailable();
        System.out.println("NmtAccess - NMT data access is available from the currently running JDK: " + isNmtAvailable);
    }

    static void nmtSampleExamples() {
        // Periodic sampling: recorded values will be at most as old as the sampling interval length
        final NmtSampleSeed periodicNmtSampling = new NmtSampleSeed();
        periodicNmtSampling.startRecording();

        // This is only here to create some non-zero example data
        try { Thread.sleep(2100); } catch (Exception e) {} // Wait for a first recording to occur

        final double totalCommitted = periodicNmtSampling.getTotal().getCommitted();
        System.out.println("NmtSample - Periodic NMT sampling - recent total committed space (bytes): " + totalCommitted);

        periodicNmtSampling.stopRecording(); // Stop periodic sampling

        // Explicit sampling: record when you decide (just don't forget to retrieve fresh data when needed)
        final NmtSampleSeed explicitNmtSampling = new NmtSampleSeed();
        explicitNmtSampling.recordNow(); // Retrieve current NMT data right now
        final double totalReserved = explicitNmtSampling.getTotal().getReserved();
        System.out.println("NmtSample - Explicit NMT sampling - current total reserved space (bytes): " + totalReserved);
    }

    static void nmtAggregateExample() {
        final NmtAggregateSeed nmtAggregating = new NmtAggregateSeed();
        nmtAggregating.startRecording();

        // This is only here to create some non-zero example data
        try { Thread.sleep(2100); } catch (Exception e) {} // Wait for a first recording to occur

        final DoubleValueRecord internalMemoryPercentage = nmtAggregating.getInternalPercent();
        final double maxInternalMemoryPercentage = internalMemoryPercentage.getMax();
        System.out.println("NmtAggregate - Max internal memory % committed vs reserved: " + maxInternalMemoryPercentage);

        nmtAggregating.stopRecording(); // Stop periodic recording
    }

    static void rtSampleExamples() {
        // Periodic sampling: recorded values will be at most as old as the sampling interval length
        final RtSampleSeed periodicRtSampling = new RtSampleSeed();
        periodicRtSampling.startRecording();
        // ... run for a while ...
        System.out.println("NmtSample - Periodic NMT sampling - current mapped memory used (bytes): " + periodicRtSampling.getMappedMemory());

        periodicRtSampling.stopRecording(); // Stop periodic sampling

        // Explicit sampling: record when you decide (just don't forget to retrieve fresh data when needed)
        final RtSampleSeed explicitRtSampling = new RtSampleSeed();
        explicitRtSampling.recordNow(); // Retrieve current RT data right now
        System.out.println("NmtSample - Periodic NMT sampling - current mapped memory used (bytes): " + explicitRtSampling.getMappedMemory());
    }

    static void rtAggregateExample() {
        final RtAggregateSeed rtAggregate = new RtAggregateSeed();
        rtAggregate.startRecording();
        // ... run for a while ...
        System.out.println("RtAggregate - min mapped memory: " + rtAggregate.getMappedMemory().getMin());

        rtAggregate.stopRecording(); // Stop periodic recording
    }

    static void compilationSampleExamples() {
        // Periodic sampling: recorded values will be at most as old as the sampling interval length
        final CompilerSampleSeed periodicCompilerSampling = new CompilerSampleSeed();
        periodicCompilerSampling.startRecording();
        // ... run for a while ...
        try { Thread.sleep(2100); } catch (Exception e) {} // Wait for a first recording to occur

        System.out.println("CompilationSample - Periodic compilation sampling - current compilation time (ms): " + periodicCompilerSampling.getCompilationTime());
        System.out.println("CompilationSample - Periodic compilation sampling - current non-NMethod size (byte): " + periodicCompilerSampling.getNonNMethodsCodeHeap());
        System.out.println("CompilationSample - Periodic compilation sampling - current non-NMethod limit (byte): " + periodicCompilerSampling.getNonNMethodsCodeHeapLimit());
        System.out.println("CompilationSample - Periodic compilation sampling - current non-NMethod usage (%): " + periodicCompilerSampling.getNonNMethodsCodeHeapUsage());
        System.out.println("CompilationSample - Periodic compilation sampling - current non-profiled NMethod size (byte): " + periodicCompilerSampling.getNonProfiledNMethodsCodeHeap());
        System.out.println("CompilationSample - Periodic compilation sampling - current non-profiled NMethod limit (byte): " + periodicCompilerSampling.getNonProfiledNMethodsCodeHeapLimit());
        System.out.println("CompilationSample - Periodic compilation sampling - current non-profiled NMethod usage (%): " + periodicCompilerSampling.getNonProfiledNMethodsCodeHeapUsage());
        System.out.println("CompilationSample - Periodic compilation sampling - current profiled NMethod size (byte): " + periodicCompilerSampling.getProfiledNMethodsCodeHeap());
        System.out.println("CompilationSample - Periodic compilation sampling - current profiled NMethod limit (byte): " + periodicCompilerSampling.getProfiledNMethodsCodeHeapLimit());
        System.out.println("CompilationSample - Periodic compilation sampling - current profiled NMethod usage (%): " + periodicCompilerSampling.getProfiledNMethodsCodeHeapUsage());
        System.out.println("CompilationSample - Periodic compilation sampling - current Code Cache size (byte): " + periodicCompilerSampling.getCodeCache());
        System.out.println("CompilationSample - Periodic compilation sampling - current Code Cache limit (byte): " + periodicCompilerSampling.getCodeCacheLimit());
        System.out.println("CompilationSample - Periodic compilation sampling - current Code Cache usage (%): " + periodicCompilerSampling.getCodeCacheUsage());

        // Stop recording
        periodicCompilerSampling.stopRecording();

        // Explicit sampling: record when you decide (just don't forget to retrieve fresh data when needed)
        final CompilerSampleSeed explicitCompilerSampling = new CompilerSampleSeed();
        explicitCompilerSampling.recordNow();
        System.out.println("CompilationSample - Explicit compilation sampling - current compilation time (ms): " + explicitCompilerSampling.getCompilationTime());
        System.out.println("CompilationSample - Explicit compilation sampling - current non-NMethod size (byte): " + explicitCompilerSampling.getNonNMethodsCodeHeap());
        System.out.println("CompilationSample - Explicit compilation sampling - current non-NMethod limit (byte): " + explicitCompilerSampling.getNonNMethodsCodeHeapLimit());
        System.out.println("CompilationSample - Explicit compilation sampling - current non-NMethod usage (%): " + explicitCompilerSampling.getNonNMethodsCodeHeapUsage());
        System.out.println("CompilationSample - Explicit compilation sampling - current non-profiled NMethod size (byte): " + explicitCompilerSampling.getNonProfiledNMethodsCodeHeap());
        System.out.println("CompilationSample - Explicit compilation sampling - current non-profiled NMethod limit (byte): " + explicitCompilerSampling.getNonProfiledNMethodsCodeHeapLimit());
        System.out.println("CompilationSample - Explicit compilation sampling - current non-profiled NMethod usage (%): " + explicitCompilerSampling.getNonProfiledNMethodsCodeHeapUsage());
        System.out.println("CompilationSample - Explicit compilation sampling - current profiled NMethod size (byte): " + explicitCompilerSampling.getProfiledNMethodsCodeHeap());
        System.out.println("CompilationSample - Explicit compilation sampling - current profiled NMethod limit (byte): " + explicitCompilerSampling.getProfiledNMethodsCodeHeapLimit());
        System.out.println("CompilationSample - Explicit compilation sampling - current profiled NMethod usage (%): " + explicitCompilerSampling.getProfiledNMethodsCodeHeapUsage());
        System.out.println("CompilationSample - Explicit compilation sampling - current Code Cache size (byte): " + explicitCompilerSampling.getCodeCache());
        System.out.println("CompilationSample - Explicit compilation sampling - current Code Cache limit (byte): " + explicitCompilerSampling.getCodeCacheLimit());
        System.out.println("CompilationSample - Explicit compilation sampling - current Code Cache usage (%): " + explicitCompilerSampling.getCodeCacheUsage());
    }

    static void compilationAggregateExamples() {
        final CompilerAggregateSeed compilerAggregate = new CompilerAggregateSeed();
        compilerAggregate.startRecording();
        // ... run for a while ...
        try { Thread.sleep(2100); } catch (Exception e) {} // Wait for a first recording to occur

        System.out.println("CompilerAggregate - avg compilation time (ms): " + compilerAggregate.getCompilationTime().getAvg());
        System.out.println("CompilerAggregate - avg non-NMethod size (byte): " + compilerAggregate.getNonNMethodsCodeHeap().getAvg());
        System.out.println("CompilerAggregate - avg non-NMethod limit (byte): " + compilerAggregate.getNonNMethodsCodeHeapLimit());
        System.out.println("CompilerAggregate - avg non-NMethod usage (%): " + compilerAggregate.getNonNMethodsCodeHeapUsage().getAvg());
        System.out.println("CompilerAggregate - avg non-profiled NMethod size (byte): " + compilerAggregate.getNonProfiledNMethodsCodeHeap().getAvg());
        System.out.println("CompilerAggregate - avg non-profiled NMethod limit (byte): " + compilerAggregate.getNonProfiledNMethodsCodeHeapLimit());
        System.out.println("CompilerAggregate - avg non-profiled NMethod usage (%): " + compilerAggregate.getNonProfiledNMethodsCodeHeapUsage().getAvg());
        System.out.println("CompilerAggregate - avg profiled NMethod size (byte): " + compilerAggregate.getProfiledNMethodsCodeHeap().getAvg());
        System.out.println("CompilerAggregate - avg profiled NMethod limit (byte): " + compilerAggregate.getProfiledNMethodsCodeHeapLimit());
        System.out.println("CompilerAggregate - avg profiled NMethod usage (%): " + compilerAggregate.getNonProfiledNMethodsCodeHeapUsage().getAvg());
        System.out.println("CompilerAggregate - avg Code Cache size (byte): " + compilerAggregate.getCodeCache().getAvg());
        System.out.println("CompilerAggregate - avg Code Cache limit (byte): " + compilerAggregate.getCodeCacheLimit());
        System.out.println("CompilerAggregate - avg Code Cache usage (%): " + compilerAggregate.getCodeCacheUsage().getAvg());

        compilerAggregate.stopRecording(); // Stop periodic recording
    }

    public static void main(String[] args) {
        intervalExample(); // always call this first!

        // JMX approach
        jmxExamples(); // All output of this goes to JMX, which is not further inspected here.

        // Local API approach, no JMX
        jvmExamples();
        gcSampleExample();
        gcAggregateExample();
        isNmtAvaliableExample();
        nmtSampleExamples();
        nmtAggregateExample();
        rtSampleExamples();
        rtAggregateExample();
        compilationSampleExamples();
        compilationAggregateExamples();
    }
}
