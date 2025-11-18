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

import com.apple.pollianna.Seed;
import com.apple.pollianna.Aggregator;

import java.util.Arrays;
import java.util.List;

/**
 * Common code for Gc metric bean implementations.
 */
public abstract class GcSeed extends Seed {
    protected GcSeed() { super(); }

    protected final GcAggregator aggregator = new GcAggregator();

    @Override
    protected List<Aggregator> aggregators() {
        return Arrays.asList(aggregator);
    }
}
