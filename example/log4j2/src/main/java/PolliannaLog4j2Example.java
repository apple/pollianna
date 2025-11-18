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
import com.apple.pollianna.gc.GcAggregateSeed;
import com.apple.pollianna.rt.RtAggregateSeed;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Illustrates how to use Pollianna APIs to obtain JVM metrics for long-term observation
 * and periodically send these values to Log4j.
 *
 * This program will not actually produce any interesting values,
 * it merely lists code for reuse in real applications.
 */
public class PolliannaLog4j2Example {

    public static void main(String[] args) {
        final GcAggregateSeed gc = new GcAggregateSeed();
        gc.startRecording(); // Start listening to GC events

        final RtAggregateSeed rt = new RtAggregateSeed();
        rt.startRecording(); // Start sampling (by default every 10 seconds)

        final Logger log = LogManager.getRootLogger();

        final var occupancy = gc.getOccupancy();
        final var workload = gc.getWorkload();
        final var allocationRate = gc.getAllocationRate();
        final var gcPause = gc.getPause();
        final var gcCycle = gc.getCycle();
        final var metaSpaceUsage = gc.getMetaspaceUsage();
        final var directMemory = gc.getDirectMemory();
        final var directMemoryUsage = gc.getDirectMemoryUsage();
        final var mappedMemory = rt.getMappedMemory();

        final String logLine = String.format(
            "occupancyMax=%.1f occupancyAvg=%.1f " +
            "workloadMax=%.1f workloadAvg=%.1f " +
            "allocationRateMax=%.1f allocationRateAvg=%.1f " +
            "gcPauseMax=%d gcPauseAvg=%d gcPausePortion=%.1f " +
            "gcCyclePortion=%.1f " +
            "metaSpaceUsageMax=%.1f " +
            "directMemoryMax=%d directMemoryUsageMax=%.1f directMemoryUsageAvg=%.1f " +
            "mappedMemoryMax=%d mappedMemoryAvg=%d",
            occupancy.getMax(), occupancy.getAvg(),
            workload.getMax(), workload.getAvg(),
            allocationRate.getMax(), allocationRate.getAvg(),
            gcPause.getMax(), gcPause.getAvg(), gcPause.getPortion(),
            gcCycle.getPortion(),
            metaSpaceUsage.getMax(),
            directMemory.getMax(), directMemoryUsage.getMax(), directMemoryUsage.getAvg(),
            mappedMemory.getMax(), mappedMemory.getAvg());

        log.info("{}", logLine);

        gc.stopRecording();
        rt.stopRecording();
    }
}
