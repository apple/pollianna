#!/bin/bash
#
# Copyright (c) 2023-2025 Apple Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

flags=$(./gradlew printFlagsFinal)
if [[ "${flags}" =~ "UseSerialGC" ]]; then
    echo "Testing Serial GC"
    ./gradlew -PjvmArgs='-XX:+UseSerialGC -Xms1G -Xmx1G -XX:+UseCompressedOops -XX:MaxTenuringThreshold=0 -XX:NewRatio=3' test
else
    echo "Skipping Serial GC"
fi

if [[ "${flags}" =~ "UseParallelGC" ]]; then
    echo "Testing Parallel GC"
    ./gradlew -PjvmArgs='-XX:+UseParallelGC -Xms1G -Xmx1G -XX:+UseCompressedOops -XX:MaxTenuringThreshold=0 -XX:NewRatio=3' test
else
    echo "Skipping Parallel GC"
fi

if [[ "${flags}" =~ "UseConcMarkSweepGC" ]]; then
    echo "Testing CMS GC"
    ./gradlew -PjvmArgs='-XX:+UseConcMarkSweepGC -Xms1G -Xmx1G -XX:+UseCompressedOops -XX:MaxTenuringThreshold=0 -XX:NewRatio=3' test
else
    echo "Skipping CMS GC"
fi
if [[ "${flags}" =~ "UseG1GC" ]]; then
    echo "Testing G1 GC"
    ./gradlew -PjvmArgs='-XX:+UseG1GC -Xms1G -Xmx1G -XX:+UseCompressedOops -XX:MaxTenuringThreshold=0 -XX:NewRatio=3' test
else
    echo "Skipping G1 GC"
fi

if [[ "${flags}" =~ "UseShenandoahGC" ]]; then
    echo "Testing Shenandoah GC"
    ./gradlew -PjvmArgs='-XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms1G -Xmx1G -XX:ShenandoahInitFreeThreshold=0' test
else
    echo "Skipping Shenandoah GC"
fi

if [[ "${flags}" =~ "UseZGC" ]]; then
    echo "Testing ZGC"
    ./gradlew -PjvmArgs='-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms1G -Xmx1G' test
else
    echo "Skipping ZGC"
fi

if [[ "${flags}" =~ "ZGenerational" ]]; then
    echo "Testing Generational ZGC"
    ./gradlew -PjvmArgs='-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:+ZGenerational -Xms1G -Xmx1G' test
else
    echo "Skipping Generational ZGC"
fi

if [[ "${flags}" =~ "CodeCache" ]]; then
    echo "Testing Legacy Code Cache"
    ./gradlew -PjvmArgs='-XX:-SegmentedCodeCache -Xms1G -Xmx1G' test
else
    echo "Skipping Legacy Code Cache"
fi