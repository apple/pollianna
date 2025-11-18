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

import com.apple.pollianna.jvm.JvmMXBean;

/**
 * A Java command line agent for JVM metrics gathering via JMX.
 * <p>
 * This agent relies on all transitive dependencies to be available on the classpath.
 * <p>
 * Example command line that makes the agent create the default JVM observation bean:
 * <code>
 * java -javaagent:pollianna-1.16.1.jar [application and its arguments]
 * </code>
 * <p>
 * Example command line with arguments that makes the agent create specialized beans,
 * one of them with select attributes:
 * <code>
 * java -javaagent:pollianna-1.16.1.jar="GcAggregate;NmtSample|TotalReserved,MetaspaceCommitted" [application and its arguments]
 * </code>
 *
 * @see Pollianna#start(String...) for the complete argument syntax
 */
public final class PolliannaAgent {

    /**
     * Entry point of the agent when it is attached to a remote JVM.
     * Splits the string `arguments` into separate arguments, which are then passed to {@link Pollianna#start(String...)}.
     * If no arguments are given, the "Jvm" MXBean will be started.
     *
     * @see Pollianna#start(String...) for the complete argument syntax
     * @param arguments the semicolon-separated arguments with instructions what to start
     * @see JvmMXBean
     */
    public static void agentmain(String arguments) {
        Pollianna.start(arguments == null ? null : arguments.split(PolliannaArgumentParser.ARGUMENT_DELIMITER));
    }

    /**
     * Entry point of the command line agent.
     * Splits the string `arguments` into separate arguments, which are then passed to {@link Pollianna#start(String...)}.
     * If no arguments are given, the "Jvm" MXBean will be started.
     *
     * @see Pollianna#start(String...) for the complete argument syntax
     * @param arguments the semicolon-separated arguments with instructions what to start
     * @see JvmMXBean
     */
    public static void premain(String arguments) {
        agentmain(arguments);
    }
}
