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

import java.lang.management.PlatformManagedObject;

public interface CompilerSampleMXBean extends PlatformManagedObject {
    /**
     * Returns the elapsed time (in milliseconds) spent in compilation. If multiple threads
     * are used for compilation, this value is summation of the approximate time that each
     * thread spent in compilation.
     *
     * @return the elapsed time (in milliseconds) spent in compilation.
     */
    long getCompilationTime();

    /**
     * Returns the number of bytes occupied by Profiled NMethods in Code Heap.
     *
     * @return the number of bytes occupied by Profiled NMethods in Code Heap.
     */
    long getProfiledNMethodsCodeHeap();

    /**
     * Returns the maximum limit of bytes can be occupied by Profiled NMethods in Code Heap.
     *
     * @return the maximum limit of bytes can be occupied by Profiled NMethods in Code Heap.
     */
    long getProfiledNMethodsCodeHeapLimit();

    /**
     * Returns the used percentage of Profiled NMethods in Code Heap.
     *
     * @return the used percentage of Profiled NMethods in Code Heap.
     */
    double getProfiledNMethodsCodeHeapUsage();

    /**
     * Returns the number of bytes occupied by Non-profiled NMethods in Code Heap.
     *
     * @return the number of bytes occupied by Non-profiled NMethods in Code Heap.
     */
    long getNonProfiledNMethodsCodeHeap();

    /**
     * Returns the maximum limit of bytes can be occupied by Non-profiled NMethods in Code Heap.
     *
     * @return the maximum limit of bytes can be occupied by Non-profiled NMethods in Code Heap.
     */
    long getNonProfiledNMethodsCodeHeapLimit();

    /**
     * Returns the used percentage of Non-Profiled NMethods in Code Heap.
     *
     * @return the used percentage of Non-Profiled NMethods in Code Heap.
     */
    double getNonProfiledNMethodsCodeHeapUsage();

    /**
     * Returns the number of bytes occupied by Non-NMethods in Code Heap.
     *
     * @return the number of bytes occupied by Non-NMethods in Code Heap.
     */
    long getNonNMethodsCodeHeap();

    /**
     * Returns the maximum limit of bytes can be occupied by Non-NMethods in Code Heap.
     *
     * @return the maximum limit of bytes can be occupied by Non-NMethods in Code Heap.
     */
    long getNonNMethodsCodeHeapLimit();

    /**
     * Returns the used percentage of Non-NMethods in Code Heap.
     *
     * @return the used percentage of Non-NMethods in Code Heap.
     */
    double getNonNMethodsCodeHeapUsage();

    /**
     * Returns the number of bytes occupied by the legacy Code Cache.
     *
     * @return the number of bytes occupied by the legacy Code Cache.
     */
    long getCodeCache();

    /**
     * Returns the maximum limit of bytes can be occupied by the legacy Code Cache.
     *
     * @return the maximum limit of bytes can be occupied by the legacy Code Cache.
     */
    long getCodeCacheLimit();

    /**
     * Returns the used percentage of the legacy Code Cache.
     *
     * @return the used percentage of the legacy Code Cache.
     */
    double getCodeCacheUsage();
}
