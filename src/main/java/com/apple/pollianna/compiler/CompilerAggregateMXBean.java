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

import java.lang.management.PlatformManagedObject;

/**
 * Bean interface for aggregated compiler metrics.
 */
public interface CompilerAggregateMXBean extends PlatformManagedObject {
    /**
     * Returns the approximate accumulated elapsed time (in milliseconds) spent in compilation.
     * If multiple threads are used for compilation, this value is the sum of the approximate
     * times that each thread spent in compilation.
     *
     * @return the approximate accumulated elapsed time (in milliseconds) spent in compilation.
     */
    LongValueRecord getCompilationTime();

    /**
     * Returns an aggregate (last, min, average, max) of the number of bytes occupied by
     * Profiled NMethods in Code Heap.
     *
     * @return an aggregate (last, min, average, max) of the number of bytes occupied by
     * Profiled NMethods in Code Heap.
     */
    LongValueRecord getProfiledNMethodsCodeHeap();

    /**
     * Returns the maximum limit of bytes can be occupied by Profiled NMethods in Code Heap.
     *
     * @return the maximum limit of bytes can be occupied by Profiled NMethods in Code Heap.
     */
    long getProfiledNMethodsCodeHeapLimit();

    /**
     * Returns an aggregate (last, min, average, max) of the used percentage of Profiled
     * NMethods in Code Heap.
     *
     * @return an aggregate (last, min, average, max) of the used percentage of Profiled
     * NMethods in Code Heap.
     */
    DoubleValueRecord getProfiledNMethodsCodeHeapUsage();

    /**
     * Returns an aggregate (last, min, average, max) of the number of bytes occupied by
     * Non-profiled NMethods in Code Heap.
     *
     * @return an aggregate (last, min, average, max) of the number of bytes occupied by
     * Non-profiled NMethods in Code Heap.
     */
    LongValueRecord getNonProfiledNMethodsCodeHeap();

    /**
     * Returns the maximum limit of bytes can be occupied by Non-profiled NMethods in Code Heap.
     *
     * @return the maximum limit of bytes can be occupied by Non-profiled NMethods in Code Heap.
     */
    long getNonProfiledNMethodsCodeHeapLimit();

    /**
     * Returns an aggregate (last, min, average, max) of the used percentage of Non-Profiled
     * NMethods in Code Heap.
     *
     * @return an aggregate (last, min, average, max) of the used percentage of Non-Profiled
     * NMethods in Code Heap.
     */
    DoubleValueRecord getNonProfiledNMethodsCodeHeapUsage();

    /**
     * Returns an aggregate (last, min, average, max) of the number of bytes occupied by
     * Non-NMethods in Code Heap.
     *
     * @return an aggregate (last, min, average, max) of the number of bytes occupied by
     * Non-NMethods in Code Heap.
     */
    LongValueRecord getNonNMethodsCodeHeap();

    /**
     * Returns the maximum limit of bytes can be occupied by Non-NMethods in Code Heap.
     *
     * @return the maximum limit of bytes can be occupied by Non-NMethods in Code Heap.
     */
    long getNonNMethodsCodeHeapLimit();

    /**
     * Returns an aggregate (last, min, average, max) of the used percentage of Non-NMethods
     * in Code Heap.
     *
     * @return an aggregate (last, min, average, max) of the used percentage of Non-NMethods
     * in Code Heap.
     */
    DoubleValueRecord getNonNMethodsCodeHeapUsage();

    /**
     * Returns an aggregate (last, min, average, max) of the number of bytes occupied by
     * the legacy Code Cache.
     *
     * @return an aggregate (last, min, average, max) of the number of bytes occupied by
     * the legacy Code Cache.
     */
    LongValueRecord getCodeCache();

    /**
     * Returns the maximum limit of bytes can be occupied by the legacy Code Cache.
     *
     * @return the maximum limit of bytes can be occupied by the legacy Code Cache.
     */
    long getCodeCacheLimit();

    /**
     * Returns an aggregate (last, min, average, max) of the used percentage of the legacy
     * Code Cache.
     *
     * @return an aggregate (last, min, average, max) of the used percentage of the legacy
     * Code Cache.
     */
    DoubleValueRecord getCodeCacheUsage();
}
