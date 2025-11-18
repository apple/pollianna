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

import com.apple.pollianna.nmt.NmtAccess;
import com.apple.pollianna.nmt.NmtSampleSeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NmtSampleTest {

    private final Runnable run = () -> {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
    };

    @Test
    public void testNmtSample() {
        final NmtSampleSeed seed = new NmtSampleSeed();
        if (NmtAccess.isAvailable()) {
            seed.recordNow();
            final long committed1 = seed.getThread().getCommitted();
            new Thread(run).start();
            seed.recordNow();
            final long committed2 = seed.getThread().getCommitted();
            assertTrue(committed2 > committed1);
            seed.recordNow();
            final long committed3 = seed.getThread().getCommitted();
            assertTrue(committed3 == committed2);
            new Thread(run).start();
            new Thread(run).start();
            seed.recordNow();
            final long committed4 = seed.getThread().getCommitted();
            assertTrue(committed4 > committed3);
        } else {
            assertTrue(seed.getTotal().getReserved() == 0);
            seed.recordNow();
            assertTrue(seed.getTotal().getReserved() == 0);
        }
    }

}
