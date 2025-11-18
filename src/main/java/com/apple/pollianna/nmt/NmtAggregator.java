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

public class NmtAggregator extends AbstractNmtAggregator {
    NmtAggregator() { super(); }

    public final NmtRecorder arenaChunk = createRecorder("Arena Chunk");
    public final NmtRecorder arguments = createRecorder("Arguments");
    public final NmtRecorder classes = createRecorder("Class");
    public final NmtRecorder compiler = createRecorder("Compiler");
    public final NmtRecorder gc = createRecorder("GC");
    public final NmtRecorder internal = createRecorder("Internal");
    public final NmtRecorder logging = createRecorder("Logging");
    public final NmtRecorder javaHeap = createRecorder("Java Heap");
    public final NmtRecorder jvmci = createRecorder("JVMCI");
    public final NmtRecorder modules = createRecorder("Module");
    public final NmtRecorder nmt = createRecorder("Native Memory Tracking");
    public final NmtRecorder objectMonitors = createRecorder("Object Monitors");
    public final NmtRecorder other = createRecorder("Other");
    public final NmtRecorder safepoint = createRecorder("Safepoint");
    public final NmtRecorder serviceability = createRecorder("Serviceability");
    public final NmtRecorder sharedClassSpace = createRecorder("Shared class space");
    public final NmtRecorder statistics = createRecorder("Statistics");
    public final NmtRecorder stringDeduplication = createRecorder("String Deduplication");
    public final NmtRecorder symbol = createRecorder("Symbol");
    public final NmtRecorder synchronization = createRecorder("Synchronization");
    public final NmtRecorder thread = createRecorder("Thread");
    public final NmtRecorder threadStack = createRecorder("Thread Stack");
    public final NmtRecorder tracing = createRecorder("Tracing");
    public final NmtRecorder unknown = createRecorder("Unknown");
}
