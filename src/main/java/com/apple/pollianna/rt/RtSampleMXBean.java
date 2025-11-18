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

/**
 *  Bean interface for miscellaneous non-GC sampled JVM runtime metrics that do not require NMT tracking data access.
 */

public interface RtSampleMXBean extends PlatformManagedObject {
    /**
     * Return the number of bytes occupied by file-mapped buffers.
     * @see java.nio.channels.FileChannel#map
     * @see java.nio.MappedByteBuffer
     *
     * @return the number of bytes occupied by file-mapped buffers
     */
    long getMappedMemory();

    /**
     * Return the number of alerts of severity WARNING since the JVM was started
     *
     * @return the number of alerts of severity WARNING since the JVM was started
     */
    int getWarningCount();

    /**
     * Return the number of alerts of severity ERROR since the JVM was started
     *
     * @return the number of alerts of severity ERROR since the JVM was started
     */
    int getErrorCount();
}
