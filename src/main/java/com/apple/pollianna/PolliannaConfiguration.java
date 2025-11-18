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

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.management.InstanceNotFoundException;

import com.apple.pollianna.compiler.CompilerAggregateMXBean;
import com.apple.pollianna.compiler.CompilerAggregateSeed;
import com.apple.pollianna.compiler.CompilerSampleMXBean;
import com.apple.pollianna.compiler.CompilerSampleSeed;
import com.apple.pollianna.gc.GcAggregateMXBean;
import com.apple.pollianna.gc.GcAggregateSeed;
import com.apple.pollianna.gc.GcSampleMXBean;
import com.apple.pollianna.gc.GcSampleSeed;
import com.apple.pollianna.jvm.JvmMXBean;
import com.apple.pollianna.jvm.JvmSeed;
import com.apple.pollianna.nmt.NmtAccess;
import com.apple.pollianna.nmt.NmtAggregateMXBean;
import com.apple.pollianna.nmt.NmtAggregateSeed;
import com.apple.pollianna.nmt.NmtSampleMXBean;
import com.apple.pollianna.nmt.NmtSampleSeed;
import com.apple.pollianna.rt.RtAggregateMXBean;
import com.apple.pollianna.rt.RtAggregateSeed;
import com.apple.pollianna.rt.RtSampleMXBean;
import com.apple.pollianna.rt.RtSampleSeed;
import com.apple.pollianna.survey.SurveySeed;

class PolliannaConfiguration {      
    private static final Seed staticSurveySeed = new SurveySeed();
    private static final Seed staticJvmSeed = new JvmSeed();

    private static final Seed[] staticSeeds = {
        staticJvmSeed, 
        new CompilerAggregateSeed(),
        new CompilerSampleSeed(),
        new GcAggregateSeed(),
        new GcSampleSeed(),
        new NmtAggregateSeed(),
        new NmtSampleSeed(),
        new RtAggregateSeed(),
        new RtSampleSeed()
    };

    private static DynamicSeed[] createDynamicSeeds() {
        return new DynamicSeed[] {
            new DynamicSeed(JvmMXBean.class, new JvmSeed()),
            new DynamicSeed(CompilerAggregateMXBean.class, new CompilerAggregateSeed()),
            new DynamicSeed(CompilerSampleMXBean.class, new CompilerSampleSeed()),
            new DynamicSeed(GcAggregateMXBean.class, new GcAggregateSeed()),
            new DynamicSeed(GcSampleMXBean.class, new GcSampleSeed()),
            new DynamicSeed(NmtAggregateMXBean.class, new NmtAggregateSeed()),
            new DynamicSeed(NmtSampleMXBean.class, new NmtSampleSeed()),
            new DynamicSeed(RtAggregateMXBean.class, new RtAggregateSeed()),
            new DynamicSeed(RtSampleMXBean.class, new RtSampleSeed())
        };
    }

    private static DynamicSeed[] dynamicSeeds = createDynamicSeeds();

    private PolliannaConfiguration() {}

    private static synchronized void startBean(Seed seed) {
        if (!seed.isRecording()) {
            try {
                ManagementFactory.getPlatformMBeanServer().registerMBean(seed, seed.getObjectName());
                seed.startRecording();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static synchronized void stopBean(Seed seed) {
        if (seed.isRecording()) {
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(seed.getObjectName());
                seed.stopRecording();
            } catch (InstanceNotFoundException infe) {
                // Ignore if the bean was not registered
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void startSurveyBean() {
        startBean(staticSurveySeed);
    }

    static void startJvmBean() {
        startBean(staticJvmSeed);
    }

    static String jvmBeanName() {
        return staticJvmSeed.beanName();
    }

    /**
     * Unregister all beans and stop aggregating any data
     */
    static void stopAllBeans() {
        stopBean(staticSurveySeed);
        for (Seed staticSeed : staticSeeds) {
            stopBean(staticSeed);
        }
        for (Seed dynamicSeed : dynamicSeeds) {
            stopBean(dynamicSeed);
        }
        // Reset any effects of `setIncludedAttributeNames()` on any dynamic seed:
        dynamicSeeds = createDynamicSeeds();
    }

    private static boolean startMatchingBean(String beanName, String[] attributes, Seed staticSeed, DynamicSeed dynamicSeed) {
        if (!staticSeed.beanName().equals(beanName)) {
            return false;
        }
        if (beanName.startsWith("Nmt") && !NmtAccess.isAvailable()) {
            // TODO: warning
            return false;
        }
        if (attributes != null) {
            final HashSet<String> attributeSet = new HashSet<String>(Arrays.stream(attributes).map(a -> a.trim()).collect(Collectors.toList()));
            if (!attributeSet.isEmpty()) {
                if (staticSeed.isRecording()) {
                    // TODO: warning
                    stopBean(staticSeed); // can only use either the static or the dynamic bean, not both
                }
                dynamicSeed.setIncludedAttributeNames(attributeSet);
                startBean(dynamicSeed);
            } else {
                // TODO: warning
            }
        } else {
            if (dynamicSeed.isRecording()) {
                // TODO: warning
                stopBean(dynamicSeed); // can only use either the static or the dynamic bean, not both
            }
            startBean(staticSeed);
        }
        return true;
    }

    static void startBean(String beanName, String[] attributes) {
        for (int i = 0; i < staticSeeds.length; i++) {
            if (startMatchingBean(beanName, attributes, staticSeeds[i], dynamicSeeds[i])) {
                return;
            }
        }
        throw new IllegalArgumentException("unknown Pollianna bean specified: " + beanName);
    }

    private static DynamicSeed startMatchingRecording(String beanName, String[] attributes, Seed staticSeed, DynamicSeed dynamicSeed) {
        if (!staticSeed.beanName().equals(beanName)) {
            return null;
        }
        if (beanName.startsWith("Nmt") && !NmtAccess.isAvailable()) {
            throw new IllegalArgumentException("NMT bean specified, but NMT reporting not available: " + beanName);
        }
        if (attributes != null) {
            final HashSet<String> attributeSet = new HashSet<String>(Arrays.stream(attributes).map(a -> a.trim()).collect(Collectors.toList()));
            if (!attributeSet.isEmpty()) {
                dynamicSeed.setIncludedAttributeNames(attributeSet);
            }
        }
        dynamicSeed.startRecording();
        return dynamicSeed;
    }

    /*
     * Start a dynamic seed with the given attributes or, if no attributes are specified,
     * with all available attributes in the seed's MXBean interface
     */
    static DynamicSeed startRecording(String beanName, String[] attributes) {
        for (int i = 0; i < staticSeeds.length; i++) {
            final DynamicSeed dynamicSeed = startMatchingRecording(beanName, attributes, staticSeeds[i], dynamicSeeds[i]);
            if (dynamicSeed != null) {
                return dynamicSeed;
            }
        }
        throw new IllegalArgumentException("unknown Pollianna bean specified: " + beanName);
    }
}
