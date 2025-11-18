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
package com.apple.pollianna.rt;

import java.lang.management.PlatformManagedObject;

import com.apple.pollianna.LongValueRecord;

/**
 *  Bean interface for miscellaneous non-GC aggregated JVM runtime metrics that do not require NMT tracking data access.
 */
public interface RtAggregateMXBean extends PlatformManagedObject {
    /**
     * Return an aggregate (last, min, average, max) of the number of bytes occupied by file-mapped buffers.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.MappedByteBuffer
     *
     * @return an aggregate (last, min, average, max) of the number of bytes occupied by file-mapped buffers
     */
    LongValueRecord getMappedMemory();

    /**
     * Returns the number of platform threads created and also started since last calling this method.
     * @see java.lang.management.ThreadMXBean#getTotalStartedThreadCount
     * 
     * @return the number of platform threads created and also started since last calling this method
     */
    long getStartedThreadCount();

    /**
     * Returns the peak number of live platform threads since last calling this method.
     * Makes calls to `ThreadMXBean.resetPeakThreadCount()`.
     * Do not use if there are competing calls to the latter.
     * @see java.lang.management.ThreadMXBean#getPeakThreadCount
     * @see java.lang.management.ThreadMXBean#resetPeakThreadCount
     * 
     * @return the number of platform threads created and also started since last calling this method
     */
    long getPeakThreadCount();
}
