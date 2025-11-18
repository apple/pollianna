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

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Retrieves NMT data from a `NativeMemoryTrackingMXBean`, if one is available in the running JDK.
 * Otherwise, silently returns zero-like default values.
 */
public class NmtAccess {
    // All NMT access is performed by reflection since the official JDK API
    // does not contain a `NativeMemoryTrackingMXBean`.

    private final PlatformManagedObject bean;
    private final Method getUsageInfoMethod, isValidMethod;

    private final Class nmtUsageInfoClass;
    private final Method getVmTotalCommittedMethod, getVmTotalReservedMethod, getNmtUsagePerCategoryMethod;

    private final Class nmtUsagePerCategoryClass;
    private final Method getNameMethod, getReservedMethod, getCommittedMethod;

    /**
     * Assigns all required handles to a bean, classes and methods.
     * @throws Exception all kinds of exceptions if any of the required handles is not available as expected
     */
    NmtAccess() throws Exception {
        final Class nmtMXBeanClass = Class.forName("com.sun.management.NativeMemoryTrackingMXBean");
        isValidMethod = nmtMXBeanClass.getDeclaredMethod("isValid");
        getUsageInfoMethod = nmtMXBeanClass.getDeclaredMethod("getUsage");

        bean = ManagementFactory.getPlatformMXBean(nmtMXBeanClass);
        if (!(Boolean) isValidMethod.invoke(bean) || getUsageInfoMethod.invoke(bean) == null) {
            throw new UnsupportedOperationException("no NMT data available");
        }

        nmtUsageInfoClass = Class.forName("com.sun.management.NMTUsage");
        getVmTotalCommittedMethod = nmtUsageInfoClass.getDeclaredMethod("getVmTotalCommitted");
        getVmTotalReservedMethod = nmtUsageInfoClass.getDeclaredMethod("getVmTotalReserved");
        getNmtUsagePerCategoryMethod = nmtUsageInfoClass.getDeclaredMethod("getNMTUsagePerCategory");

        nmtUsagePerCategoryClass = Class.forName("com.sun.management.NMTUsagePerCategory");
        getNameMethod = nmtUsagePerCategoryClass.getDeclaredMethod("getname");
        getReservedMethod = nmtUsagePerCategoryClass.getDeclaredMethod("getReserved");
        getCommittedMethod = nmtUsagePerCategoryClass.getDeclaredMethod("getCommitted");
    }

    private static NmtAccess make() {
        try {
            return new NmtAccess();
        } catch (Exception e) {
        }
        return null;
    }

    private static final NmtAccess INSTANCE = make();

    public static boolean isAvailable() { return INSTANCE != null; }

    public static Object getUsageInfo() {
        if (INSTANCE != null) {
            try {
                return INSTANCE.getUsageInfoMethod.invoke(INSTANCE.bean);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static long getVmTotalCommitted(Object nmtUsage) {
        if (nmtUsage != null) {
            try {
                return (Long) INSTANCE.getVmTotalCommittedMethod.invoke(nmtUsage);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static long getVmTotalReserved(Object nmtUsage) {
        if (nmtUsage != null) {
            try {
                return (Long) INSTANCE.getVmTotalReservedMethod.invoke(nmtUsage);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static Map<String, NmtUsage> getNmtUsagePerCategory(Object nmtUsage) {
        if (nmtUsage != null) {
            try {
                final HashMap<String, NmtUsage> result = new HashMap<String, NmtUsage>();
                Object[] usageArray = (Object[]) INSTANCE.getNmtUsagePerCategoryMethod.invoke(nmtUsage);
                for (Object usageObject : usageArray) {
                    result.put((String) INSTANCE.getNameMethod.invoke(usageObject),
                               new NmtUsage((Long) INSTANCE.getReservedMethod.invoke(usageObject),
                                            (Long) INSTANCE.getCommittedMethod.invoke(usageObject)));
                }
                return result;
            } catch (Exception e) {
            }
        }
        return null;
    }
}
