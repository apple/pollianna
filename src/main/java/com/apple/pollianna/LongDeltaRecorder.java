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

/**
 * Tracks a cumulative `long` value and reports its change (delta) since its preceding observation.
 */
public class LongDeltaRecorder {

    public LongDeltaRecorder() { }

    private boolean initialized = false;
    private long previous = 0;

    /**
     * Return the delta of a cumulative value compared to the preceding call to this method.
     * The first call returns `0`.
     * 
     * @param value the current cumulative value
     * @return the delta of the cumulative `value` compared to the preceding call to this method
     */
    public synchronized long record(long value) {
        if (!initialized) {
            previous = value;
            initialized = true;
            return 0;
        }
        final long result = value - previous;
        previous = value;
        return result;
    }
}
