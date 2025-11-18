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

import com.apple.pollianna.DoubleValueRecord;
import com.apple.pollianna.LongValueRecord;

public final class NmtAggregateSeed extends NmtSeed implements NmtAggregateMXBean {
    public NmtAggregateSeed() { super(); }

    public DoubleValueRecord getTotalPercent() { return aggregator.total.getRecord(); }
    public LongValueRecord getTotalReserved() { return aggregator.total.reserved.getRecord(); }
    public LongValueRecord getTotalCommitted() { return aggregator.total.committed.getRecord(); }

    public DoubleValueRecord getArenaChunkPercent() { return aggregator.arenaChunk.getRecord(); }
    public LongValueRecord getArenaChunkReserved() { return aggregator.arenaChunk.reserved.getRecord(); }
    public LongValueRecord getArenaChunkCommitted() { return aggregator.arenaChunk.committed.getRecord(); }

    public DoubleValueRecord getArgumentsPercent() { return aggregator.arguments.getRecord(); }
    public LongValueRecord getArgumentsReserved() { return aggregator.arguments.reserved.getRecord(); }
    public LongValueRecord getArgumentsCommitted() { return aggregator.arguments.committed.getRecord(); }

    public DoubleValueRecord getClassesPercent() { return aggregator.classes.getRecord(); }
    public LongValueRecord getClassesReserved() { return aggregator.classes.reserved.getRecord(); }
    public LongValueRecord getClassesCommitted() { return aggregator.classes.committed.getRecord(); }

    public DoubleValueRecord getCompilerPercent() { return aggregator.compiler.getRecord(); }
    public LongValueRecord getCompilerReserved() { return aggregator.compiler.reserved.getRecord(); }
    public LongValueRecord getCompilerCommitted() { return aggregator.compiler.committed.getRecord(); }

    public DoubleValueRecord getGcPercent() { return aggregator.gc.getRecord(); }
    public LongValueRecord getGcReserved() { return aggregator.gc.reserved.getRecord(); }
    public LongValueRecord getGcCommitted() { return aggregator.gc.committed.getRecord(); }

    public DoubleValueRecord getInternalPercent() { return aggregator.internal.getRecord(); }
    public LongValueRecord getInternalReserved() { return aggregator.internal.reserved.getRecord(); }
    public LongValueRecord getInternalCommitted() { return aggregator.internal.committed.getRecord(); }

    public DoubleValueRecord getJavaHeapPercent() { return aggregator.javaHeap.getRecord(); }
    public LongValueRecord getJavaHeapReserved() { return aggregator.javaHeap.reserved.getRecord(); }
    public LongValueRecord getJavaHeapCommitted() { return aggregator.javaHeap.committed.getRecord(); }

    public DoubleValueRecord getJvmciPercent() { return aggregator.jvmci.getRecord(); }
    public LongValueRecord getJvmciReserved() { return aggregator.jvmci.reserved.getRecord(); }
    public LongValueRecord getJvmciCommitted() { return aggregator.jvmci.committed.getRecord(); }

    public DoubleValueRecord getMetaspacePercent() { return aggregator.metaspace.getRecord(); }
    public LongValueRecord getMetaspaceReserved() { return aggregator.metaspace.reserved.getRecord(); }
    public LongValueRecord getMetaspaceCommitted() { return aggregator.metaspace.committed.getRecord(); }

    public DoubleValueRecord getModulesPercent() { return aggregator.modules.getRecord(); }
    public LongValueRecord getModulesReserved() { return aggregator.modules.reserved.getRecord(); }
    public LongValueRecord getModulesCommitted() { return aggregator.modules.committed.getRecord(); }

    public DoubleValueRecord getNmtPercent() { return aggregator.nmt.getRecord(); }
    public LongValueRecord getNmtReserved() { return aggregator.nmt.reserved.getRecord(); }
    public LongValueRecord getNmtCommitted() { return aggregator.nmt.committed.getRecord(); }

    public DoubleValueRecord getObjectMonitorsPercent() { return aggregator.objectMonitors.getRecord(); }
    public LongValueRecord getObjectMonitorsReserved() { return aggregator.objectMonitors.reserved.getRecord(); }
    public LongValueRecord getObjectMonitorsCommitted() { return aggregator.objectMonitors.committed.getRecord(); }

    public DoubleValueRecord getOtherPercent() { return aggregator.other.getRecord(); }
    public LongValueRecord getOtherReserved() { return aggregator.other.reserved.getRecord(); }
    public LongValueRecord getOtherCommitted() { return aggregator.other.committed.getRecord(); }

    public DoubleValueRecord getSafepointPercent() { return aggregator.safepoint.getRecord(); }
    public LongValueRecord getSafepointReserved() { return aggregator.safepoint.reserved.getRecord(); }
    public LongValueRecord getSafepointCommitted() { return aggregator.safepoint.committed.getRecord(); }

    public DoubleValueRecord getServiceabilityPercent() { return aggregator.serviceability.getRecord(); }
    public LongValueRecord getServiceabilityReserved() { return aggregator.serviceability.reserved.getRecord(); }
    public LongValueRecord getServiceabilityCommitted() { return aggregator.serviceability.committed.getRecord(); }

    public DoubleValueRecord getSharedClassSpacePercent() { return aggregator.sharedClassSpace.getRecord(); }
    public LongValueRecord getSharedClassSpaceReserved() { return aggregator.sharedClassSpace.reserved.getRecord(); }
    public LongValueRecord getSharedClassSpaceCommitted() { return aggregator.sharedClassSpace.committed.getRecord(); }

    public DoubleValueRecord getStatisticsPercent() { return aggregator.statistics.getRecord(); }
    public LongValueRecord getStatisticsReserved() { return aggregator.statistics.reserved.getRecord(); }
    public LongValueRecord getStatisticsCommitted() { return aggregator.statistics.committed.getRecord(); }

    public DoubleValueRecord getStringDeduplicationPercent() { return aggregator.stringDeduplication.getRecord(); }
    public LongValueRecord getStringDeduplicationReserved() { return aggregator.stringDeduplication.reserved.getRecord(); }
    public LongValueRecord getStringDeduplicationCommitted() { return aggregator.stringDeduplication.committed.getRecord(); }

    public DoubleValueRecord getSymbolPercent() { return aggregator.symbol.getRecord(); }
    public LongValueRecord getSymbolReserved() { return aggregator.symbol.reserved.getRecord(); }
    public LongValueRecord getSymbolCommitted() { return aggregator.symbol.committed.getRecord(); }

    public DoubleValueRecord getSynchronizationPercent() { return aggregator.synchronization.getRecord(); }
    public LongValueRecord getSynchronizationReserved() { return aggregator.synchronization.reserved.getRecord(); }
    public LongValueRecord getSynchronizationCommitted() { return aggregator.synchronization.committed.getRecord(); }

    public DoubleValueRecord getThreadPercent() { return aggregator.thread.getRecord(); }
    public LongValueRecord getThreadReserved() { return aggregator.thread.reserved.getRecord(); }
    public LongValueRecord getThreadCommitted() { return aggregator.thread.committed.getRecord(); }

    public DoubleValueRecord getThreadStackPercent() { return aggregator.threadStack.getRecord(); }
    public LongValueRecord getThreadStackReserved() { return aggregator.threadStack.reserved.getRecord(); }
    public LongValueRecord getThreadStackCommitted() { return aggregator.threadStack.committed.getRecord(); }

    public DoubleValueRecord getTracingPercent() { return aggregator.tracing.getRecord(); }
    public LongValueRecord getTracingReserved() { return aggregator.tracing.reserved.getRecord(); }
    public LongValueRecord getTracingCommitted() { return aggregator.tracing.committed.getRecord(); }

    public DoubleValueRecord getUnknownPercent() { return aggregator.unknown.getRecord(); }
    public LongValueRecord getUnknownReserved() { return aggregator.unknown.reserved.getRecord(); }
    public LongValueRecord getUnknownCommitted() { return aggregator.unknown.committed.getRecord(); }
}
