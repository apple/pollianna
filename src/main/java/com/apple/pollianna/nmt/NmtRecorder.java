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

import com.apple.pollianna.LongValueRecorder;
import com.apple.pollianna.PercentageRecorder;

public class NmtRecorder extends PercentageRecorder {
    public final String name;

    public NmtRecorder(String name) {
        this.name = name;
    }

    public final LongValueRecorder reserved = new LongValueRecorder();
    public final LongValueRecorder committed = new LongValueRecorder();

    private NmtUsage lastUsage = new NmtUsage(0, 0);
    public NmtUsage lastUsage() { return lastUsage; }

    public void record(NmtUsage usage) {
        lastUsage = usage;
        reserved.record(usage.reserved);
        committed.record(usage.committed);
        super.record(usage.committed, usage.reserved);
    }
}
