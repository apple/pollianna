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

import com.apple.pollianna.LongValueRecorder;
import com.apple.pollianna.PeriodicAggregator;
import com.apple.pollianna.Util;

import java.lang.management.BufferPoolMXBean;

public class RtAggregator extends PeriodicAggregator {

    private final BufferPoolMXBean mappedMemoryBean = Util.getMXBean(BufferPoolMXBean.class, "mapped");
    public final LongValueRecorder mappedMemory = new LongValueRecorder();

    public RtAggregator() { super(); }

    private final Runnable poll = () -> {
        if (mappedMemoryBean != null) {
            mappedMemory.record(mappedMemoryBean.getMemoryUsed());
        }
    };

    @Override
    protected Runnable runnable () { return poll; }
}
