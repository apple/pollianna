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
package com.apple.pollianna.nmt;

import com.apple.pollianna.PeriodicAggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared NMT data aggregator code.
 * Automatically disabled if NMT access is not available.
 */
public abstract class AbstractNmtAggregator extends PeriodicAggregator {
    protected AbstractNmtAggregator() { super(); }

    public final NmtRecorder total = new NmtRecorder("total");

    protected final List<NmtRecorder> recorders = new ArrayList<NmtRecorder>();

    protected NmtRecorder createRecorder(String name) {
        final NmtRecorder recorder = new NmtRecorder(name);
        recorders.add(recorder);
        return recorder;
    }

    public final NmtRecorder metaspace = createRecorder("Metaspace");

    private final Runnable poll = () -> {
        Object usageInfo = NmtAccess.getUsageInfo();
        if (usageInfo != null) {
            total.record(new NmtUsage(NmtAccess.getVmTotalReserved(usageInfo), NmtAccess.getVmTotalCommitted(usageInfo)));
            Map<String, NmtUsage> categoryUsages = NmtAccess.getNmtUsagePerCategory(usageInfo);
            if (categoryUsages != null) {
                for (NmtRecorder recorder : recorders) {
                    recorder.record(categoryUsages.get(recorder.name));
                }
            }
        }
    };

    @Override
    protected Runnable runnable () { return poll; }

    @Override
    public void startAggregating() {
        if (NmtAccess.isAvailable()) {
            super.startAggregating();
        }
    }

    @Override
    public void stopAggregating() {
        if (NmtAccess.isAvailable()) {
            super.stopAggregating();
        }
    }
}
