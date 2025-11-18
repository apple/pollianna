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

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import static com.apple.pollianna.Units.M;

public class TestUtil {
    private static ByteBuffer[] mappedBuffers = new ByteBuffer[1024];
    private static int nMappedBuffers = 0;

    public static void createMappedBuffer() {
        try {
            final Class c = TestUtil.class;
            final Path path = Paths.get(c.getResource(c.getSimpleName() + ".class").getPath()); // Java 8
            final FileChannel fileChannel = (FileChannel) Files.newByteChannel(path, EnumSet.of(StandardOpenOption.READ));
            final ByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            mappedBuffers[nMappedBuffers++ % mappedBuffers.length] = mappedByteBuffer;
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            throw new RuntimeException(e);
        }
    }

    private static ByteBuffer[] directBuffers = new ByteBuffer[1024];
    private static int nDirectBuffers = 0;

    public static void createDirectBuffer() {
        directBuffers[nDirectBuffers++ % directBuffers.length] = ByteBuffer.allocateDirect(M);
    }
}
