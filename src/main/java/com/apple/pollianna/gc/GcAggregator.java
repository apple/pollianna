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
package com.apple.pollianna.gc;

import com.apple.pollianna.Aggregator;
import com.apple.pollianna.LongDurationRecorder;
import com.apple.pollianna.PercentageRecorder;
import com.apple.pollianna.RateRecorder;

import javax.management.ListenerNotFoundException;
import javax.management.NotificationEmitter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import static com.apple.pollianna.Units.*;

/**
 * Aggregates GC metrics by subscribing and to listening to GC events
 * from all available GarbageCollectorMXBean instances in the running JDK.
 */
public class GcAggregator implements Aggregator {

    public GcAggregator() { }

    private final GcNotificationListener listener = new GcNotificationListener();

    public void startAggregating() {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter emitter = (NotificationEmitter) gcBean;
            emitter.addNotificationListener(listener, null, this);
        }
    }

    public void stopAggregating() {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter emitter = (NotificationEmitter) gcBean;
            try {
                emitter.removeNotificationListener(listener, null, this);
            } catch (ListenerNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final RateRecorder allocationRate = new RateRecorder(MiB, MILLIS_PER_SECOND);
    public final PercentageRecorder occupancy = new PercentageRecorder();
    public final PercentageRecorder workload = new PercentageRecorder();
    public final LongDurationRecorder pause = new LongDurationRecorder();
    public final LongDurationRecorder cycle = new LongDurationRecorder();
    public final DirectMemoryRecorder directMemory = new DirectMemoryRecorder();
    public final MetaspaceRecorder metaspace = new MetaspaceRecorder();
}
