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
package com.apple.pollianna.jvm;

import com.apple.pollianna.Seed;
import com.apple.pollianna.Aggregator;
import com.apple.pollianna.compiler.CompilerAggregator;
import com.apple.pollianna.gc.GcAggregator;

import java.util.Arrays;
import java.util.List;

/**
 * Bean implementation for essential JVM metrics.
 */
public class JvmSeed extends Seed implements JvmMXBean {
    public JvmSeed() { super(); }

    @Override
    public String beanName() {
        return JvmSeed.class.getSimpleName().replace(Seed.class.getSimpleName(), "");
    }

    protected final GcAggregator gcAggregator = new GcAggregator();
    protected final CompilerAggregator compilerAggregator = new CompilerAggregator();

    @Override
    protected List<Aggregator> aggregators() {
        return Arrays.asList(gcAggregator, compilerAggregator);
    }

    public double getGcWorkloadMax() {
        return gcAggregator.workload.getRecord().getMax();
    }

    public double getGcAllocationRateMax() {
        return gcAggregator.allocationRate.getRecord().getMax();
    }

    public long getGcPauseMax() {
        return gcAggregator.pause.getRecord().getMax();
    }

    public double getGcPausePortion() {
        return gcAggregator.pause.getRecord().getPortion();
    }

    public double getDirectMemoryUsageMax() {
        return gcAggregator.directMemory.getPercentageRecord().getMax();
    }

    public double getCodeCacheSegmentUsageMax() {
        return Math.max(
            Math.max(compilerAggregator.nonProfiledNMethodsCodeHeap.getPercentageRecord().getMax(),
                     compilerAggregator.profiledNMethodsCodeHeap.getPercentageRecord().getMax()),
            Math.max(compilerAggregator.nonNMethodsCodeHeap.getPercentageRecord().getMax(),
                     compilerAggregator.legacyCodeCache.getPercentageRecord().getMax())
        );
    }
}
