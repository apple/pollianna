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
package com.apple.pollianna.compiler;

public final class CompilerSampleSeed extends CompilerSeed implements CompilerSampleMXBean {
    @Override
    public long getCompilationTime() {
        return aggregator.compilation.last();
    }

    @Override
    public long getProfiledNMethodsCodeHeap() {
        return aggregator.profiledNMethodsCodeHeap.lastValue();
    }

    @Override
    public long getProfiledNMethodsCodeHeapLimit() {
        return aggregator.profiledNMethodsCodeHeap.limit();
    }

    @Override
    public double getProfiledNMethodsCodeHeapUsage() {
        return aggregator.profiledNMethodsCodeHeap.lastPercentage();
    }

    @Override
    public long getNonProfiledNMethodsCodeHeap() {
        return aggregator.nonProfiledNMethodsCodeHeap.lastValue();
    }

    @Override
    public long getNonProfiledNMethodsCodeHeapLimit() {
        return aggregator.nonProfiledNMethodsCodeHeap.limit();
    }

    @Override
    public double getNonProfiledNMethodsCodeHeapUsage() {
        return aggregator.nonProfiledNMethodsCodeHeap.lastPercentage();
    }

    @Override
    public long getNonNMethodsCodeHeap() {
        return aggregator.nonNMethodsCodeHeap.lastValue();
    }

    @Override
    public long getNonNMethodsCodeHeapLimit() {
        return aggregator.nonNMethodsCodeHeap.limit();
    }

    @Override
    public double getNonNMethodsCodeHeapUsage() {
        return aggregator.nonNMethodsCodeHeap.lastPercentage();
    }

    @Override
    public long getCodeCache() {
        return aggregator.legacyCodeCache.lastValue();
    }

    @Override
    public long getCodeCacheLimit() {
        return aggregator.legacyCodeCache.limit();
    }

    @Override
    public double getCodeCacheUsage() {
        return aggregator.legacyCodeCache.lastPercentage();
    }

    public void recordNow() {
        aggregator.runnable().run();
    }
}
