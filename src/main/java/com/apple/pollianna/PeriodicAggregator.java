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
package com.apple.pollianna;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An aggregator with a data gathering procedure which is executed periodically.
 */
public abstract class PeriodicAggregator implements Aggregator {

    /**
     * @return the data gathering procedure to be executed periodically for this aggregator
     */
    protected abstract Runnable runnable();

    private static final Map<PeriodicAggregator, PeriodicAggregator> aggregators = new IdentityHashMap<PeriodicAggregator, PeriodicAggregator>();

    private static final Runnable runAggregators = () -> {
        synchronized (aggregators) {
            try {
                for (PeriodicAggregator aggregator : aggregators.keySet()) {
                    aggregator.runnable().run();
                }
            } catch (Exception e) {
                // TODO: warning
            }
        }
    };

    private static final int DEFAULT_INTERVAL_SECONDS = 10;
    private static int intervalSeconds = DEFAULT_INTERVAL_SECONDS;

    /**
     * Set the interval time after which all started data gathering procedures are periodically run.
     * If zero or a negative interval time is given, no procedures will be run.
     *
     * @param seconds the interval in seconds
     */
    public static void setIntervalSeconds(int seconds) {
        synchronized (aggregators) {
            if (seconds > 0) {
                intervalSeconds = seconds;
            }
        }
    }

    private static ScheduledExecutorService scheduler = null;

    private static void startAggregating(PeriodicAggregator periodicAggregator) {
        synchronized (aggregators) {
            if (scheduler == null) {
                scheduler = Executors.newScheduledThreadPool(1, r -> {
                    final Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                });
                scheduler.scheduleAtFixedRate(runAggregators, intervalSeconds, intervalSeconds, SECONDS);
            }
            aggregators.put(periodicAggregator, periodicAggregator);
        }
    }

    private static void stopAggregating(PeriodicAggregator periodicAggregator) {
        synchronized (aggregators) {
            aggregators.remove(periodicAggregator);
        }
    }

    protected PeriodicAggregator() { }

    /**
     * Starts this aggregator's data gathering procedure being executed periodically
     */
    public void startAggregating() {
        startAggregating(this);
    }

    /**
     * Stops this aggregator's data gathering procedure from being executed periodically
     */
    public void stopAggregating() {
        stopAggregating(this);
    }
}
