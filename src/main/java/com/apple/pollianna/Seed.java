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

import javax.management.ObjectName;
import java.lang.management.PlatformManagedObject;
import java.util.List;

/**
 * "Seed" is a synonym for "BeanImpl" or "bean implementation", as a seed is what is inside a bean.
 */
public abstract class Seed implements PlatformManagedObject {
    protected Seed() { }

    public String beanName() {
        return getClass().getSimpleName().replace(Seed.class.getSimpleName(), "");
    }

    public ObjectName getObjectName() {
        try {
            return new ObjectName("com.apple.pollianna:type=" + beanName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract List<Aggregator> aggregators();

    private boolean isRecording = false;

    public boolean isRecording() {
        return isRecording;
    }

    public synchronized void startRecording() {
        if (!isRecording && aggregators() != null) {
            for (Aggregator a : aggregators()) {
                a.startAggregating();
            }
            isRecording = true;
        }
    }

    public synchronized void stopRecording() {
        if (isRecording && aggregators() != null) {
            for (Aggregator a : aggregators()) {
                a.stopAggregating();
            }
        }
        isRecording = false;
    }
}
