/*
 * Created on 20/10/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of views that must hold for every {@link SeekableSource} implementation: views and their parent don't move each other's position and views
 * honor their boundaries.
 */
public class SeekableSourceViewContractTest {

    private static final int SOURCE_SIZE = 3000;

    private Path file;

    static Stream<Named<SourceFactory>> sources() {
        return Stream.of(source("FileChannel", FileChannelSeekableSource::new),
                source("MemoryMapped", MemoryMappedSeekableSource::new),
                source("ByteArray", path -> new ByteArraySeekableSource(Files.readAllBytes(path))),
                source("Buffered FileChannel", path -> new BufferedSeekableSource(new FileChannelSeekableSource(path))),
                source("Buffered MemoryMapped", path -> new BufferedSeekableSource(new MemoryMappedSeekableSource(path))));
    }

    private static Named<SourceFactory> source(String name, SourceFactory factory) {
        return Named.of(name, factory);
    }

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        file = Files.write(tempDir.resolve("source.bin"), expectedBytes(0, SOURCE_SIZE));
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void creatingViewDoesntMoveParent(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            source.position(7);
            source.view(100, 200);
            assertEquals(7, source.position());
            assertEquals(expectedByte(7), source.read());
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void readingViewDoesntMoveParent(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            source.position(7);
            var view = source.view(100, 200);
            view.read();
            view.read(ByteBuffer.allocate(64));
            view.position(10);
            view.read();
            assertEquals(7, source.position());
            assertEquals(expectedByte(7), source.read());
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void readingParentDoesntMoveView(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            var view = source.view(100, 2000);
            assertEquals(expectedByte(100), view.read());
            source.position(2060);
            assertArrayEquals(expectedBytes(2060, 5), readBytes(source, 5));
            assertEquals(expectedByte(2065), source.read());
            assertEquals(1, view.position());
            assertArrayEquals(expectedBytes(101, 5), readBytes(view, 5));
            assertEquals(expectedByte(106), view.read());
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void viewsReadIndependently(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            var first = source.view(100, 200);
            var second = source.view(1050, 200);
            assertEquals(expectedByte(100), first.read());
            assertEquals(expectedByte(1050), second.read());
            assertArrayEquals(expectedBytes(101, 10), readBytes(first, 10));
            assertArrayEquals(expectedBytes(1051, 10), readBytes(second, 10));
            second.position(50);
            assertEquals(expectedByte(111), first.read());
            assertEquals(expectedByte(1100), second.read());
            assertEquals(12, first.position());
            assertEquals(51, second.position());
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void viewIsTrimmedToSourceEnd(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            var view = source.view(SOURCE_SIZE - 10, 100);
            assertEquals(10, view.size());
            assertArrayEquals(expectedBytes(SOURCE_SIZE - 10, 10), readBytes(view, 10));
            assertEquals(-1, view.read());
            assertEquals(-1, view.read(ByteBuffer.allocate(10)));
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void viewPositionIsClampedToViewSize(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            var view = source.view(100, 200);
            view.position(300);
            assertEquals(200, view.position());
            assertEquals(-1, view.read());
            assertEquals(-1, view.read(ByteBuffer.allocate(10)));
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void closingViewLeavesParentAndSiblingReadable(SourceFactory factory) throws IOException {
        try (var source = factory.open(file)) {
            var closed = source.view(100, 200);
            var sibling = source.view(500, 200);
            closed.read();
            closed.close();
            assertTrue(source.isOpen());
            assertEquals(expectedByte(0), source.read());
            assertEquals(expectedByte(500), sibling.read());
        }
    }

    private static byte[] readBytes(SeekableSource source, int length) throws IOException {
        var dst = ByteBuffer.allocate(length);
        assertEquals(length, source.read(dst));
        return dst.array();
    }

    private static int expectedByte(long index) {
        return (int) (index & 0xff);
    }

    private static byte[] expectedBytes(long start, int length) {
        var bytes = new byte[length];
        for (var i = 0; i < length; i++) {
            bytes[i] = (byte) (start + i);
        }
        return bytes;
    }

    @FunctionalInterface
    interface SourceFactory {
        SeekableSource open(Path file) throws IOException;
    }
}
