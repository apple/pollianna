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
package com.apple.pollianna.rt;

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.util.Optional;

public class MonitoringBeanAccess {
    private static final int DEFAULT_VALUE = 0;
    private static MonitoringBeanAccess instance;
    private final PlatformManagedObject bean;
    private final Method getJVMLoggingCountersMethod;
    private final Method getWarningsMethod;
    private final Method getErrorsMethod;

    private MonitoringBeanAccess() throws Exception {
        final Class beanClass = Class.forName("com.apple.jdk.MonitoringMXBean");
        getJVMLoggingCountersMethod = beanClass.getDeclaredMethod("getJVMLoggingCounters");

        bean = ManagementFactory.getPlatformMXBean(beanClass);
        if (getJVMLoggingCountersMethod.invoke(bean) == null) {
            throw new UnsupportedOperationException("no MonitoringMXBean data available");
        }

        final Class<?> jvmLoggingCountersClass = Class.forName("com.apple.jdk.JVMLoggingCounters");
        getWarningsMethod = jvmLoggingCountersClass.getDeclaredMethod("getWarnings");
        getErrorsMethod = jvmLoggingCountersClass.getDeclaredMethod("getErrors");
    }

    public static Optional<MonitoringBeanAccess> getOptionalInstance() {
        try {
            if (instance == null) {
                instance = new MonitoringBeanAccess();
            }
            return Optional.of(instance);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean isAvailable() { return getOptionalInstance().isPresent(); }

    public static int getWarnings() {
        return getOptionalInstance().map(MonitoringBeanAccess::warnings).orElse(DEFAULT_VALUE);
    }

    public static int getErrors() {
        return getOptionalInstance().map(MonitoringBeanAccess::errors).orElse(DEFAULT_VALUE);
    }

    private int warnings() {
        try {
            return (int) getWarningsMethod.invoke(getJVMLoggingCountersMethod.invoke(bean));
        } catch (ReflectiveOperationException e) {
            return DEFAULT_VALUE;
        }
    }

    private int errors() {
        try {
            return (int) getErrorsMethod.invoke(getJVMLoggingCountersMethod.invoke(bean));
        } catch (ReflectiveOperationException e) {
            return DEFAULT_VALUE;
        }
    }
}
