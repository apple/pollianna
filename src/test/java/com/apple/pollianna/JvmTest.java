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

import com.apple.pollianna.compiler.CompilerAggregateSeed;
import com.apple.pollianna.jvm.JvmSeed;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JvmTest {

    private static void createBuffers() {
        for (int i = 0; i < 3; i++) {
            TestUtil.createDirectBuffer();
            System.gc();

            // Give GC notifications enough time to arrive
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }
    }

    private static int triggerCompilation() {
        int result = 0;
        for (int outer = 0; outer < 20; outer++) {
            for (int middle = 0; middle < 10000; middle++) {
                for (int inner = 0; inner < 10; inner++) {
                    result += (outer * middle + inner) % 1000;
                    result -= (middle - inner) % 500;
                    result ^= outer;
                }
            }
        }
        return result;
    }

    @Test
    @Tag("fresh-jvm")
    public void testJvm() {
        PeriodicAggregator.setIntervalSeconds(1);
        final JvmSeed seed = new JvmSeed();
        seed.startRecording();

        final CompilerAggregateSeed compilerSeed = new CompilerAggregateSeed();
        compilerSeed.startRecording();

        createBuffers();
        
        // Wait for the seed to sample the direct memory usage
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        final double direct1 = seed.getDirectMemoryUsageMax();
        assertTrue(direct1 > 0.0);

        createBuffers();

        final double direct2 = seed.getDirectMemoryUsageMax();
        assertTrue(direct2 > direct1);

        // Wait for initial sampling to occur
        try { Thread.sleep(3000); } catch (Exception e) {}

        final double codeCacheSegmentUsageMax = seed.getCodeCacheSegmentUsageMax();
        assertTrue(codeCacheSegmentUsageMax > 0.0);
        
        // Verify that the max matches one of the four possible code heap segments
        if (Util.getBooleanVMOptionValue("SegmentedCodeCache").orElse(false)) {
            assertTrue(codeCacheSegmentUsageMax == compilerSeed.getNonProfiledNMethodsCodeHeapUsage().getMax() ||
                       codeCacheSegmentUsageMax == compilerSeed.getProfiledNMethodsCodeHeapUsage().getMax() ||
                       codeCacheSegmentUsageMax == compilerSeed.getNonNMethodsCodeHeapUsage().getMax());
        } else {
            assertTrue(codeCacheSegmentUsageMax == compilerSeed.getCodeCacheUsage().getMax());
        }

        seed.stopRecording();
    }
}
