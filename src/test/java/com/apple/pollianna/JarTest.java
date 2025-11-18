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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify OpenTelemetry dependency isolation works correctly.
 *
 * These tests verify that:
 * 1. Without any JAR on classpath: OTel classes are not found
 * 2. With slim JAR on classpath: OTel classes are not found (no dependencies included)
 * 3. With fat JAR on classpath: OTel classes are found (dependencies included)
 * 4. With slim JAR as agent: OTel classes are not found (no dependencies)
 * 5. With fat JAR as agent: OTel classes are found (dependencies included)
 * 6. With isolated JAR as agent: OTel classes are not found (complete isolation)
 */
public class JarTest {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String BUILD_LIBS_DIR = PROJECT_DIR + "/build/libs";
    private static String VERSION;
    private static String SLIM_JAR;
    private static String FAT_JAR;
    private static String ISO_JAR;

    static {
        // Extract version from gradle.properties
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(new java.io.FileInputStream(PROJECT_DIR + "/gradle.properties"));
            VERSION = props.getProperty("version");
            if (VERSION == null) {
                throw new RuntimeException("Could not find version in gradle.properties");
            }

            SLIM_JAR = BUILD_LIBS_DIR + "/pollianna-" + VERSION + ".jar";
            FAT_JAR = BUILD_LIBS_DIR + "/pollianna-" + VERSION + "-all.jar";
            ISO_JAR = BUILD_LIBS_DIR + "/pollianna-" + VERSION + "-iso.jar";
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine version", e);
        }
    }

    @Test
    void testWithoutAnyJar_OTelClassesNotFound() throws Exception {
        final int result = runTestApp(null, null);
        assertEquals(1, result, "Expected OTel classes to be missing without any JAR");
    }

    @Test
    void testWithSlimJarOnClasspath_OTelClassesNotFound() throws Exception {
        final int result = runTestApp(SLIM_JAR, null);
        assertEquals(1, result, "Expected OTel classes to be missing with slim JAR on classpath");
    }

    @Test
    void testWithFatJarOnClasspath_OTelClassesFound() throws Exception {
        final int result = runTestApp(FAT_JAR, null);
        assertEquals(0, result, "Expected OTel classes to be present with fat JAR on classpath");
    }

    @Test
    void testWithSlimJarAsAgent_OTelClassesNotFound() throws Exception {
        final int result = runTestApp(null, SLIM_JAR);
        assertEquals(1, result, "Expected OTel classes to be missing with slim JAR as agent");
    }

    @Test
    void testWithFatJarAsAgent_OTelClassesFound() throws Exception {
        final int result = runTestApp(null, FAT_JAR);
        assertEquals(0, result, "Expected OTel classes to be present with fat JAR as agent");
    }

    @Test
    void testWithIsolatedJarAsAgent_OTelClassesNotFound() throws Exception {
        final int result = runTestApp(null, ISO_JAR);
        assertEquals(1, result, "Expected OTel classes to be missing with isolated JAR as agent (complete isolation)");
    }

    private int runTestApp(String classpathJar, String agentJar) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("java");
        if (agentJar != null) {
            command.add("-javaagent:" + agentJar);
        }
        command.add("-cp");
        if (classpathJar != null) {
            // Include both test classes and the JAR under test
            command.add("build/classes/java/test" + System.getProperty("path.separator") + classpathJar);
        } else {
            command.add("build/classes/java/test");
        }
        command.add(JarTestApp.class.getName());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(PROJECT_DIR));
        return pb.start().waitFor();
    }
}
