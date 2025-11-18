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

import com.apple.pollianna.DoubleValueRecord;
import com.apple.pollianna.LongValueRecord;

public final class CompilerAggregateSeed extends CompilerSeed implements CompilerAggregateMXBean {
    @Override
    public LongValueRecord getCompilationTime() {
        return aggregator.compilation.getRecord();
    }

    @Override
    public LongValueRecord getProfiledNMethodsCodeHeap() {
        return aggregator.profiledNMethodsCodeHeap.getValueRecord();
    }

    @Override
    public long getProfiledNMethodsCodeHeapLimit() {
        return aggregator.profiledNMethodsCodeHeap.limit();
    }

    @Override
    public DoubleValueRecord getProfiledNMethodsCodeHeapUsage() {
        return aggregator.profiledNMethodsCodeHeap.getPercentageRecord();
    }

    @Override
    public LongValueRecord getNonProfiledNMethodsCodeHeap() {
        return aggregator.nonProfiledNMethodsCodeHeap.getValueRecord();
    }

    @Override
    public long getNonProfiledNMethodsCodeHeapLimit() {
        return aggregator.nonProfiledNMethodsCodeHeap.limit();
    }

    @Override
    public DoubleValueRecord getNonProfiledNMethodsCodeHeapUsage() {
        return aggregator.nonProfiledNMethodsCodeHeap.getPercentageRecord();
    }

    @Override
    public LongValueRecord getNonNMethodsCodeHeap() {
        return aggregator.nonNMethodsCodeHeap.getValueRecord();
    }

    @Override
    public long getNonNMethodsCodeHeapLimit() {
        return aggregator.nonNMethodsCodeHeap.limit();
    }

    @Override
    public DoubleValueRecord getNonNMethodsCodeHeapUsage() {
        return aggregator.nonNMethodsCodeHeap.getPercentageRecord();
    }

    @Override
    public LongValueRecord getCodeCache() {
        return aggregator.legacyCodeCache.getValueRecord();
    }

    @Override
    public long getCodeCacheLimit() {
        return aggregator.legacyCodeCache.limit();
    }

    @Override
    public DoubleValueRecord getCodeCacheUsage() {
        return aggregator.legacyCodeCache.getPercentageRecord();
    }
}
