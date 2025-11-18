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

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.util.Optional;

public class Util {
    public static <B extends PlatformManagedObject> B getMXBean(Class<B> type, String name) {
        for (B bean : ManagementFactory.getPlatformMXBeans(type)) {
            if (bean.getObjectName().getKeyProperty("name").equals(name)) {
                return bean;
            }
        }
        // TODO: warning
        return null;
    }

    public static Optional<Long> getLongVMOptionValue(String optionName) {
        final HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (bean == null) {
            // TODO: warning
        } else {
            try {
                final VMOption option = bean.getVMOption(optionName);
                if (option != null) {
                    final long result = Long.parseLong(option.getValue());
                    if (result > 0) {
                        return Optional.of(result);
                    }
                }
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Boolean> getBooleanVMOptionValue(String optionName) {
        final HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (bean == null) {
            // TODO: warning
        } else {
            try {
                final VMOption option = bean.getVMOption(optionName);
                if (option != null) {
                    return Optional.of(Boolean.parseBoolean(option.getValue()));
                }
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static int getJavaMajorVersion() {
        String[] version = ManagementFactory.getRuntimeMXBean().getSpecVersion().split("\\.");
        return Integer.parseInt(version.length > 1 ? version[1] : version[0]);
    }
}
