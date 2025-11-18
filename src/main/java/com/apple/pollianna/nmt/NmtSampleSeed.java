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

/**
 * Provides raw samples of NMT data from the JVM.
 *
 * Either explicitly call `recordNow` whenever you want to record a new snapshot of NMT values
 * or have such recordings occur periodically in the background by calling `startRecording()`.
 * If you use both, then they will compete with each other.
 * Every new recording overwrites the preceding one.
 *
 * @see #recordNow()
 * @see #startRecording()
 */
public final class NmtSampleSeed extends NmtSeed implements NmtSampleMXBean {

    /**
     * Creates a new NMT sampling seed, which reports data from most recent NMT recording.
     *
     * @see #recordNow()
     * @see #startRecording()
     */
    public NmtSampleSeed() { super(); }

    /**
     * Update all values returned by getter methods of this class with fresh NMT data from the JVM right now.
     */
    public void recordNow() {
        aggregator.runnable().run();
    }

    public NmtUsage getTotal() { return aggregator.total.lastUsage(); }
    public NmtUsage getArenaChunk() { return aggregator.arenaChunk.lastUsage(); }
    public NmtUsage getArguments() { return aggregator.arguments.lastUsage(); }
    public NmtUsage getClasses() { return aggregator.classes.lastUsage(); }
    public NmtUsage getCompiler() { return aggregator.compiler.lastUsage(); }
    public NmtUsage getGc() { return aggregator.gc.lastUsage(); }
    public NmtUsage getInternal() { return aggregator.internal.lastUsage(); }
    public NmtUsage getJavaHeap() { return aggregator.javaHeap.lastUsage(); }
    public NmtUsage getJvmci() { return aggregator.jvmci.lastUsage(); }
    public NmtUsage getMetaspace() { return aggregator.metaspace.lastUsage(); }
    public NmtUsage getModules() { return aggregator.modules.lastUsage(); }
    public NmtUsage getNmt() { return aggregator.nmt.lastUsage(); }
    public NmtUsage getObjectMonitors() { return aggregator.objectMonitors.lastUsage(); }
    public NmtUsage getOther() { return aggregator.other.lastUsage(); }
    public NmtUsage getSafepoint() { return aggregator.safepoint.lastUsage(); }
    public NmtUsage getServiceability() { return aggregator.serviceability.lastUsage(); }
    public NmtUsage getSharedClassSpace() { return aggregator.sharedClassSpace.lastUsage(); }
    public NmtUsage getStatistics() { return aggregator.statistics.lastUsage(); }
    public NmtUsage getStringDeduplication() { return aggregator.stringDeduplication.lastUsage(); }
    public NmtUsage getSymbol() { return aggregator.symbol.lastUsage(); }
    public NmtUsage getSynchronization() { return aggregator.synchronization.lastUsage(); }
    public NmtUsage getThread() { return aggregator.thread.lastUsage(); }
    public NmtUsage getThreadStack() { return aggregator.threadStack.lastUsage(); }
    public NmtUsage getTracing() { return aggregator.tracing.lastUsage(); }
    public NmtUsage getUnknown() { return aggregator.unknown.lastUsage(); }
}
