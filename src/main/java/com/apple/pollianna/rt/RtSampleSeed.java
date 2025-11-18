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

public class RtSampleSeed extends RtSeed implements RtSampleMXBean {
    /**
     * Creates a new RT sampling seed, which reports data from most recent RT recording.
     *
     * @see #recordNow()
     * @see #startRecording()
     */
    public RtSampleSeed() { super(); }

    /**
     * Update all values returned by getter methods of this class with fresh RT data from the JVM right now.
     */
    public void recordNow() {
        aggregator.runnable().run();
    }

    public long getMappedMemory() { return aggregator.mappedMemory.last(); }

    @Override
    public int getWarningCount() {
        return MonitoringBeanAccess.getWarnings();
    }

    @Override
    public int getErrorCount() {
        return MonitoringBeanAccess.getErrors();
    }
}
