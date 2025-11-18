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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * A wrapper Java agent that provides complete classpath isolation
 * by loading all Pollianna classes and dependencies from an embedded JAR
 * using a custom classloader.
 *
 * @see PolliannaAgent
 */
public final class IsolatingPolliannaAgent {

    /**
     * Entry point of the agent when it is attached to a remote JVM.
     * Creates an isolated classloader and delegates to the real Pollianna agent.
     *
     * @param arguments the semicolon-separated arguments with instructions what to start
     * @see PolliannaAgent#agentmain
     */
    public static void agentmain(String arguments) {
        try {
            final AgentClassLoader classLoader = new AgentClassLoader();
            final Class<?> agentClass = classLoader.loadClass("com.apple.pollianna.PolliannaAgent");
            final Method agentMainMethod = agentClass.getMethod("agentmain", String.class);
            agentMainMethod.invoke(null, arguments);
        } catch (Exception e) {
            System.err.println("Failed to initialize Pollianna isolated agent: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Entry point of the command line agent.
     * Creates an isolated classloader and delegates to the real Pollianna agent.
     *
     * @param arguments the semicolon-separated arguments with instructions what to start
     * @see PolliannaAgent#premain
     */
    public static void premain(String arguments) {
        agentmain(arguments);
    }

    /**
     * Custom classloader that loads all Pollianna classes and dependencies from an embedded JAR.
     */
    private static class AgentClassLoader extends ClassLoader {

        private static final String EMBEDDED_JAR = "/pollianna-all.jar";

        public AgentClassLoader() {
            super(null);
        }

        private byte[] readEntryBytes(String entryName) throws IOException {
            try (InputStream embeddedJarStream = IsolatingPolliannaAgent.class.getResourceAsStream(EMBEDDED_JAR)) {
                if (embeddedJarStream == null) {
                    throw new IOException("Embedded JAR '" + EMBEDDED_JAR + "' not found");
                }
                try (JarInputStream jarInputStream = new JarInputStream(embeddedJarStream)) {
                    JarEntry entry;
                    while ((entry = jarInputStream.getNextJarEntry()) != null) {
                        if (entryName.equals(entry.getName())) {
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            byte[] data = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = jarInputStream.read(data)) != -1) {
                                buffer.write(data, 0, bytesRead);
                            }
                            return buffer.toByteArray();
                        }
                        jarInputStream.closeEntry();
                    }
                }
            }
            throw new IOException("Entry not found: " + entryName);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            final String classPath = name.replace('.', '/') + ".class";
            try {
                final byte[] classBytes = readEntryBytes(classPath);
                final CodeSource codeSource = new CodeSource(new URL("jar:memory:pollianna-all.jar!/" + classPath), (java.security.cert.Certificate[]) null);
                final ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, null);
                return defineClass(name, classBytes, 0, classBytes.length, protectionDomain);
            } catch (Exception e) {
                throw new ClassNotFoundException("Failed to load class: " + name, e);
            }
        }

        @Override
        protected URL findResource(String name) {
            try {
                readEntryBytes(name);
                return new URL("jar:memory:pollianna-all.jar!/" + name);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return super.findResources(name);
        }
    }
}