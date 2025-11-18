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

import com.apple.pollianna.LongDurationRecorder;
import com.apple.pollianna.LongValueRecorder;
import com.apple.pollianna.PeriodicAggregator;
import com.apple.pollianna.Util;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

public class CompilerAggregator extends PeriodicAggregator {
    private static final String MEMORY_POOL_NON_PROFILED_NMETHODS = "CodeHeap 'non-profiled nmethods'";
    private static final String MEMORY_POOL_PROFILED_NMETHODS = "CodeHeap 'profiled nmethods'";
    private static final String MEMORY_POOL_NON_NMETHODS = "CodeHeap 'non-nmethods'";
    private static final String MEMORY_POOL_CODE_CACHE = "CodeCache";
    private static final String MEMORY_POOL_CODE_CACHE_JDK8 = "Code Cache";

    private static final String VM_OPTION_NON_PROFILED_NMETHODS = "NonProfiledCodeHeapSize";
    private static final String VM_OPTION_PROFILED_NMETHODS = "ProfiledCodeHeapSize";
    private static final String VM_OPTION_NON_NMETHODS = "NonNMethodCodeHeapSize";
    private static final String VM_OPTION_CODE_CACHE_SIZE = "ReservedCodeCacheSize";

    private final CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
    public final LongValueRecorder compilation = new LongDurationRecorder();

    public final CodeHeapRecorder nonProfiledNMethodsCodeHeap
            = new CodeHeapRecorder(MEMORY_POOL_NON_PROFILED_NMETHODS, VM_OPTION_NON_PROFILED_NMETHODS);
    public final CodeHeapRecorder profiledNMethodsCodeHeap
            = new CodeHeapRecorder(MEMORY_POOL_PROFILED_NMETHODS, VM_OPTION_PROFILED_NMETHODS);
    public final CodeHeapRecorder nonNMethodsCodeHeap
            = new CodeHeapRecorder(MEMORY_POOL_NON_NMETHODS, VM_OPTION_NON_NMETHODS);
    public final CodeHeapRecorder legacyCodeCache
            = new CodeHeapRecorder(
                    Util.getJavaMajorVersion() > 8 ? MEMORY_POOL_CODE_CACHE : MEMORY_POOL_CODE_CACHE_JDK8,
                    VM_OPTION_CODE_CACHE_SIZE);

    private final Runnable poll = () -> {
        compilation.record(compilationBean.getTotalCompilationTime());
        nonProfiledNMethodsCodeHeap.record();
        profiledNMethodsCodeHeap.record();
        nonNMethodsCodeHeap.record();
        legacyCodeCache.record();
    };

    /**
     * @return the data gathering procedure to be executed periodically for this aggregator
     */
    protected Runnable runnable() {
        return poll;
    }
}
