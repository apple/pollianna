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

import java.lang.management.PlatformManagedObject;

public interface NmtSampleMXBean extends PlatformManagedObject {
    NmtUsage getTotal();
    NmtUsage getArenaChunk();
    NmtUsage getArguments();
    NmtUsage getClasses();
    NmtUsage getCompiler();
    NmtUsage getGc();
    NmtUsage getInternal();
    NmtUsage getJavaHeap();
    NmtUsage getJvmci();
    NmtUsage getMetaspace();
    NmtUsage getModules();
    NmtUsage getNmt();
    NmtUsage getObjectMonitors();
    NmtUsage getOther();
    NmtUsage getSafepoint();
    NmtUsage getServiceability();
    NmtUsage getSharedClassSpace();
    NmtUsage getStatistics();
    NmtUsage getStringDeduplication();
    NmtUsage getSymbol();
    NmtUsage getSynchronization();
    NmtUsage getThread();
    NmtUsage getThreadStack();
    NmtUsage getTracing();
    NmtUsage getUnknown();
}
