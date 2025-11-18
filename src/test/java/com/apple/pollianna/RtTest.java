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

import com.apple.pollianna.rt.MonitoringBeanAccess;
import com.apple.pollianna.rt.RtAggregateSeed;
import com.apple.pollianna.rt.RtSampleSeed;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.LogLevel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RtTest {

    private static void sleep(int nMilliSeconds) {
        try {
            Thread.sleep(nMilliSeconds);
        } catch (InterruptedException e) {
            fail("test interrupted");
        }
    }

    private static void createBuffers(boolean waitForSampling) {
        for (int i = 0; i < 3; i++) {
            TestUtil.createMappedBuffer();
            if (waitForSampling) {
                sleep(1100);
            }
        }
    }

    private static void startThreads(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            new Thread(() -> { 
                sleep(2000);
            }).start();
        }
        sleep(100);
    }

    @Test
    @Tag("fresh-jvm")
    public void testRtAggregate() {
        PeriodicAggregator.setIntervalSeconds(1);
        final RtAggregateSeed seed = new RtAggregateSeed();
        seed.startRecording();

        createBuffers(true);

        final LongValueRecord mapped1 = seed.getMappedMemory();
        assertTrue(mapped1.getMin() >= 0);
        assertTrue(mapped1.getAvg() > mapped1.getMin());
        assertTrue(mapped1.getMax() > mapped1.getAvg());

        Logger.log(LogTag.JFR, LogLevel.WARN, "When in trouble, or in doubt");
        Logger.log(LogTag.JFR, LogLevel.ERROR, "Run in circles, scream and shout");
        createBuffers(true);

        final LongValueRecord mapped2 = seed.getMappedMemory();
        assertTrue(mapped2.getMin() > mapped1.getMin());
        assertTrue(mapped2.getAvg() > mapped2.getMin());
        assertTrue(mapped2.getAvg() > mapped1.getAvg());
        assertTrue(mapped2.getMax() > mapped2.getAvg());
        assertTrue(mapped2.getMax() > mapped1.getMax());

        assertTrue(seed.getStartedThreadCount() == 0);
        final long peak = seed.getPeakThreadCount();
        assertTrue(peak > 0);

        final int nThreads1 = 3;
        startThreads(nThreads1);
        assertTrue(seed.getStartedThreadCount() == nThreads1);
        assertTrue(seed.getPeakThreadCount() >= nThreads1);

        // Cause the first batch of threads to terminate significantly earlier than the second:
        sleep(2500);

        final int nThreads2 = 5;
        startThreads(nThreads2);
        assertTrue(seed.getStartedThreadCount() == nThreads2);
        final long p1 = seed.getPeakThreadCount();
        assertTrue(p1 >= nThreads2);
        assertTrue(p1 < peak + nThreads1 + nThreads2);

        // Wait for the first batch of threads to terminate:
        sleep(1500);
        // Eliminate their count from the next interval:
        seed.getPeakThreadCount();

        final long p2 = seed.getPeakThreadCount();
        assertTrue(p2 >= nThreads2);
        assertTrue(seed.getPeakThreadCount() < peak + nThreads1 + nThreads2);

        seed.stopRecording();
    }

    @Test
    public void testRtSample() {
        final RtSampleSeed seed = new RtSampleSeed();

        createBuffers(false);
        seed.recordNow();

        final long mapped1 = seed.getMappedMemory();
        assertTrue(mapped1 > 0);
        int warning1 = seed.getWarningCount();
        int error1 = seed.getErrorCount();
        assertEquals(0, warning1);
        assertEquals(0, error1);

        createBuffers(false);
        Logger.log(LogTag.JFR, LogLevel.WARN, "When in trouble, or in doubt");
        Logger.log(LogTag.JFR, LogLevel.ERROR, "Run in circles, scream and shout");
        seed.recordNow();

        final long mapped2 = seed.getMappedMemory();
        assertTrue(mapped2 > mapped1);

        if (MonitoringBeanAccess.isAvailable()) {
            assertEquals(warning1 + 1, seed.getWarningCount());
            assertEquals(error1 + 1, seed.getErrorCount());
        } else {
            assertEquals(0, seed.getWarningCount());
            assertEquals(0, seed.getErrorCount());
        }
    }
}
