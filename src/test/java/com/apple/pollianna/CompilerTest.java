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
import com.apple.pollianna.compiler.CompilerSampleSeed;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.apple.pollianna.Units.M;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompilerTest {
    @Test
    @Tag("fresh-jvm")
    public void testCompilerSample() {
        final CompilerSampleSeed seed = new CompilerSampleSeed();

        if (Util.getBooleanVMOptionValue("SegmentedCodeCache").orElse(false)) {
            assertEquals(0, seed.getCompilationTime());
            assertEquals(0, seed.getNonNMethodsCodeHeap());
            assertTrue(seed.getNonNMethodsCodeHeapLimit() > M);
            assertEquals(0, seed.getNonNMethodsCodeHeapUsage());
            assertEquals(0, seed.getProfiledNMethodsCodeHeap());
            assertTrue(seed.getProfiledNMethodsCodeHeapLimit() > 100 * M);
            assertEquals(0, seed.getProfiledNMethodsCodeHeapUsage());
            assertEquals(0, seed.getNonProfiledNMethodsCodeHeap());
            assertTrue(seed.getNonProfiledNMethodsCodeHeapLimit() > 100 * M);
            assertEquals(0, seed.getProfiledNMethodsCodeHeapUsage());
            assertEquals(0, seed.getCodeCache());
            assertTrue(seed.getCodeCacheLimit() > 100 * M);
            assertEquals(0, seed.getCodeCacheUsage());

            seed.recordNow();
            final long ct2 = seed.getCompilationTime();
            assertTrue(ct2 > 0);
            assertTrue(seed.getNonNMethodsCodeHeap() > 0);
            assertTrue(seed.getNonNMethodsCodeHeapLimit() > 0);
            assertTrue(seed.getNonNMethodsCodeHeapUsage() > 0);
            assertTrue(seed.getProfiledNMethodsCodeHeap() > 0);
            assertTrue(seed.getProfiledNMethodsCodeHeapLimit() > 0);
            assertTrue(seed.getProfiledNMethodsCodeHeapUsage() > 0);
            assertTrue(seed.getNonProfiledNMethodsCodeHeap() > 0);
            assertTrue(seed.getNonProfiledNMethodsCodeHeapLimit() > 0);
            assertTrue(seed.getProfiledNMethodsCodeHeapUsage() > 0);
            assertEquals(0, seed.getCodeCache());
            assertTrue(seed.getCodeCacheLimit() > 0);
            assertEquals(0, seed.getCodeCacheUsage());
        } else {
            assertEquals(0, seed.getCompilationTime());
            assertEquals(0, seed.getNonNMethodsCodeHeap());
            assertTrue(seed.getNonNMethodsCodeHeapLimit() > 0);
            assertEquals(0, seed.getNonNMethodsCodeHeapUsage());
            assertEquals(0, seed.getProfiledNMethodsCodeHeap());
            assertTrue(seed.getProfiledNMethodsCodeHeapLimit() > 0);
            assertEquals(0, seed.getProfiledNMethodsCodeHeapUsage());
            assertEquals(0, seed.getNonProfiledNMethodsCodeHeap());
            assertTrue(seed.getNonNMethodsCodeHeapLimit() > 0);
            assertEquals(0, seed.getProfiledNMethodsCodeHeapUsage());
            assertEquals(0, seed.getCodeCache());
            assertTrue(seed.getCodeCacheLimit() > 100 * M);
            assertEquals(0, seed.getCodeCacheUsage());

            seed.recordNow();
            final long ct2 = seed.getCompilationTime();
            assertTrue(ct2 > 0);
            assertEquals(0, seed.getNonNMethodsCodeHeap());
            assertTrue(seed.getNonNMethodsCodeHeapLimit() > 0);
            assertEquals(0, seed.getNonNMethodsCodeHeapUsage());
            assertEquals(0, seed.getProfiledNMethodsCodeHeap());
            assertTrue(seed.getProfiledNMethodsCodeHeapLimit() > 0);
            assertEquals(0, seed.getProfiledNMethodsCodeHeapUsage());
            assertEquals(0, seed.getNonProfiledNMethodsCodeHeap());
            assertTrue(seed.getNonNMethodsCodeHeapLimit() > 0);
            assertEquals(0, seed.getProfiledNMethodsCodeHeapUsage());
            assertTrue(seed.getCodeCache() > 0);
            assertTrue(seed.getCodeCacheLimit() > 0);
            assertTrue(seed.getCodeCacheUsage() > 0);
        }
    }

    @Test
    @Tag("fresh-jvm")
    public void testCompilerAggregate() {
        PeriodicAggregator.setIntervalSeconds(1);
        CompilerAggregateSeed seed = new CompilerAggregateSeed();
        seed.startRecording();

        if (Util.getBooleanVMOptionValue("SegmentedCodeCache").orElse(false)) {
            // Before sampling
            LongValueRecord compilationRecord1 = seed.getCompilationTime();
            assertEquals(0, compilationRecord1.avg);
            assertEquals(0, compilationRecord1.min);
            assertEquals(0, compilationRecord1.max);

            LongValueRecord profiledRecord1 = seed.getProfiledNMethodsCodeHeap();
            assertEquals(0, profiledRecord1.avg);
            assertEquals(0, profiledRecord1.min);
            assertEquals(0, profiledRecord1.max);

            long profiledLimitRecord1 = seed.getProfiledNMethodsCodeHeapLimit();
            assertTrue(profiledLimitRecord1 > 0);

            DoubleValueRecord profiledUsageRecord1 = seed.getProfiledNMethodsCodeHeapUsage();
            assertEquals(0, profiledUsageRecord1.avg);
            assertEquals(0, profiledUsageRecord1.min);
            assertEquals(0, profiledUsageRecord1.max);

            LongValueRecord codeCacheRecord1 = seed.getCodeCache();
            assertEquals(0, codeCacheRecord1.avg);
            assertEquals(0, codeCacheRecord1.min);
            assertEquals(0, codeCacheRecord1.max);

            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            } // Wait for a first recording to occur

            // After sampling
            LongValueRecord compilationRecord2 = seed.getCompilationTime();
            assertTrue(compilationRecord2.avg > 0);
            assertTrue(compilationRecord2.min > 0);
            assertTrue(compilationRecord2.max > 0);

            LongValueRecord profiledRecord2 = seed.getProfiledNMethodsCodeHeap();
            assertTrue(profiledRecord2.avg > 0);
            assertTrue(profiledRecord2.min > 0);
            assertTrue(profiledRecord2.max > 0);

            long profiledLimitRecord2 = seed.getProfiledNMethodsCodeHeapLimit();
            assertTrue(profiledLimitRecord2 > 0);

            DoubleValueRecord profiledUsageRecord2 = seed.getProfiledNMethodsCodeHeapUsage();
            assertTrue(profiledUsageRecord2.avg > 0);
            assertTrue(profiledUsageRecord2.min > 0);
            assertTrue(profiledUsageRecord2.max > 0);

            LongValueRecord codeCacheRecord2 = seed.getCodeCache();
            assertEquals(0, codeCacheRecord2.avg);
            assertEquals(0, codeCacheRecord2.min);
            assertEquals(0, codeCacheRecord2.max);

            // Trigger compilation
            int s = 0;
            for (int i = 0; i < 70000; i++) {
                s += i;
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            } // Wait for a first recording to occur

            // After compilation triggered
            LongValueRecord compilationRecord3 = seed.getCompilationTime();
            assertTrue(compilationRecord3.avg > compilationRecord2.avg);
            assertTrue(compilationRecord3.min >= compilationRecord2.min);
            assertTrue(compilationRecord3.max > compilationRecord2.max);

            LongValueRecord profiledRecord3 = seed.getProfiledNMethodsCodeHeap();
            assertTrue(profiledRecord3.avg > profiledRecord2.avg);
            assertTrue(profiledRecord3.min >= profiledRecord2.min);
            assertTrue(profiledRecord3.max > profiledRecord2.max);

            long profiledLimitRecord3 = seed.getProfiledNMethodsCodeHeapLimit();
            assertEquals(profiledLimitRecord2, profiledLimitRecord3);

            DoubleValueRecord profiledUsageRecord3 = seed.getProfiledNMethodsCodeHeapUsage();
            assertTrue(profiledUsageRecord3.avg > profiledUsageRecord2.avg);
            assertTrue(profiledUsageRecord3.min >= profiledUsageRecord2.min);
            assertTrue(profiledUsageRecord3.max > profiledUsageRecord2.max);

            LongValueRecord codeCacheRecord3 = seed.getCodeCache();
            assertEquals(0, codeCacheRecord3.avg);
            assertEquals(0, codeCacheRecord3.min);
            assertEquals(0, codeCacheRecord3.max);
        } else {
            // Before sampling
            LongValueRecord compilationRecord1 = seed.getCompilationTime();
            assertEquals(0, compilationRecord1.avg);
            assertEquals(0, compilationRecord1.min);
            assertEquals(0, compilationRecord1.max);

            LongValueRecord profiledRecord1 = seed.getProfiledNMethodsCodeHeap();
            assertEquals(0, profiledRecord1.avg);
            assertEquals(0, profiledRecord1.min);
            assertEquals(0, profiledRecord1.max);

            LongValueRecord codeCacheRecord1 = seed.getCodeCache();
            assertEquals(0, codeCacheRecord1.avg);
            assertEquals(0, codeCacheRecord1.min);
            assertEquals(0, codeCacheRecord1.max);

            long codeCacheLimitRecord1 = seed.getCodeCacheLimit();
            assertTrue(codeCacheLimitRecord1 > 0);

            DoubleValueRecord codeCacheUsageRecord1 = seed.getCodeCacheUsage();
            assertEquals(0, codeCacheUsageRecord1.avg);
            assertEquals(0, codeCacheUsageRecord1.min);
            assertEquals(0, codeCacheUsageRecord1.max);

            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            } // Wait for a first recording to occur

            // After sampling
            LongValueRecord compilationRecord2 = seed.getCompilationTime();
            assertTrue(compilationRecord2.avg > 0);
            assertTrue(compilationRecord2.min > 0);
            assertTrue(compilationRecord2.max > 0);

            LongValueRecord profiledRecord2 = seed.getProfiledNMethodsCodeHeap();
            assertEquals(0, profiledRecord2.avg );
            assertEquals(0, profiledRecord2.min);
            assertEquals(0, profiledRecord2.max);

            LongValueRecord codeCacheRecord2 = seed.getCodeCache();
            assertTrue(codeCacheRecord2.avg > 0);
            assertTrue(codeCacheRecord2.min > 0);
            assertTrue(codeCacheRecord2.max > 0);

            long codeCacheLimitRecord2 = seed.getCodeCacheLimit();
            assertEquals(codeCacheLimitRecord1, codeCacheLimitRecord2);

            DoubleValueRecord codeCacheUsageRecord2 = seed.getCodeCacheUsage();
            assertTrue(codeCacheUsageRecord2.avg > 0);
            assertTrue(codeCacheUsageRecord2.min > 0);
            assertTrue(codeCacheUsageRecord2.max > 0);

            // Trigger compilation
            int s = 0;
            for (int i = 0; i < 70000; i++) {
                s += i;
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            } // Wait for a first recording to occur

            // After compilation triggered
            LongValueRecord compilationRecord3 = seed.getCompilationTime();
            assertTrue(compilationRecord3.avg > compilationRecord2.avg);
            assertTrue(compilationRecord3.min >= compilationRecord2.min);
            assertTrue(compilationRecord3.max > compilationRecord2.max);

            LongValueRecord profiledRecord3 = seed.getProfiledNMethodsCodeHeap();
            assertEquals(0, profiledRecord3.avg );
            assertEquals(0, profiledRecord3.min);
            assertEquals(0, profiledRecord3.max);

            LongValueRecord codeCacheRecord3 = seed.getCodeCache();
            assertTrue(codeCacheRecord3.avg > codeCacheRecord2.avg);
            assertTrue(codeCacheRecord3.min >= codeCacheRecord2.min);
            assertTrue(codeCacheRecord3.max > codeCacheRecord2.max);

            long codeCacheLimitRecord3 = seed.getCodeCacheLimit();
            assertEquals(codeCacheLimitRecord2, codeCacheLimitRecord3);

            DoubleValueRecord codeCacheUsageRecord3 = seed.getCodeCacheUsage();
            assertTrue(codeCacheUsageRecord3.avg > codeCacheUsageRecord2.avg);
            assertTrue(codeCacheUsageRecord3.min >= codeCacheUsageRecord2.min);
            assertTrue(codeCacheUsageRecord3.max > codeCacheUsageRecord2.max);
        }
    }
}
