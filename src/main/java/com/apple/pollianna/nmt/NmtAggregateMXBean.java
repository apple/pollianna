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

import java.lang.management.PlatformManagedObject;

/**
 * Bean interface for simple NMT metric aggregations of all available NMT categories.
 */
public interface NmtAggregateMXBean extends PlatformManagedObject {
    DoubleValueRecord getTotalPercent();
    LongValueRecord getTotalReserved();
    LongValueRecord getTotalCommitted();

    DoubleValueRecord getArenaChunkPercent();
    LongValueRecord getArenaChunkReserved();
    LongValueRecord getArenaChunkCommitted();

    DoubleValueRecord getArgumentsPercent();
    LongValueRecord getArgumentsReserved();
    LongValueRecord getArgumentsCommitted();

    DoubleValueRecord getClassesPercent();
    LongValueRecord getClassesReserved();
    LongValueRecord getClassesCommitted();

    DoubleValueRecord getCompilerPercent();
    LongValueRecord getCompilerReserved();
    LongValueRecord getCompilerCommitted();

    DoubleValueRecord getGcPercent();
    LongValueRecord getGcReserved();
    LongValueRecord getGcCommitted();

    DoubleValueRecord getInternalPercent();
    LongValueRecord getInternalReserved();
    LongValueRecord getInternalCommitted();

    DoubleValueRecord getJavaHeapPercent();
    LongValueRecord getJavaHeapReserved();
    LongValueRecord getJavaHeapCommitted();

    DoubleValueRecord getJvmciPercent();
    LongValueRecord getJvmciReserved();
    LongValueRecord getJvmciCommitted();

    DoubleValueRecord getMetaspacePercent();
    LongValueRecord getMetaspaceReserved();
    LongValueRecord getMetaspaceCommitted();

    DoubleValueRecord getModulesPercent();
    LongValueRecord getModulesReserved();
    LongValueRecord getModulesCommitted();

    DoubleValueRecord getNmtPercent();
    LongValueRecord getNmtReserved();
    LongValueRecord getNmtCommitted();

    DoubleValueRecord getObjectMonitorsPercent();
    LongValueRecord getObjectMonitorsReserved();
    LongValueRecord getObjectMonitorsCommitted();

    DoubleValueRecord getOtherPercent();
    LongValueRecord getOtherReserved();
    LongValueRecord getOtherCommitted();

    DoubleValueRecord getSafepointPercent();
    LongValueRecord getSafepointReserved();
    LongValueRecord getSafepointCommitted();

    DoubleValueRecord getServiceabilityPercent();
    LongValueRecord getServiceabilityReserved();
    LongValueRecord getServiceabilityCommitted();

    DoubleValueRecord getSharedClassSpacePercent();
    LongValueRecord getSharedClassSpaceReserved();
    LongValueRecord getSharedClassSpaceCommitted();

    DoubleValueRecord getStatisticsPercent();
    LongValueRecord getStatisticsReserved();
    LongValueRecord getStatisticsCommitted();

    DoubleValueRecord getStringDeduplicationPercent();
    LongValueRecord getStringDeduplicationReserved();
    LongValueRecord getStringDeduplicationCommitted();

    DoubleValueRecord getSymbolPercent();
    LongValueRecord getSymbolReserved();
    LongValueRecord getSymbolCommitted();

    DoubleValueRecord getSynchronizationPercent();
    LongValueRecord getSynchronizationReserved();
    LongValueRecord getSynchronizationCommitted();

    DoubleValueRecord getThreadPercent();
    LongValueRecord getThreadReserved();
    LongValueRecord getThreadCommitted();

    DoubleValueRecord getThreadStackPercent();
    LongValueRecord getThreadStackReserved();
    LongValueRecord getThreadStackCommitted();

    DoubleValueRecord getTracingPercent();
    LongValueRecord getTracingReserved();
    LongValueRecord getTracingCommitted();

    DoubleValueRecord getUnknownPercent();
    LongValueRecord getUnknownReserved();
    LongValueRecord getUnknownCommitted();
}
