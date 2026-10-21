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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of views that must hold for every {@link SeekableSource} implementation: views and their parent don't move each other's position, also when
 * read on different threads, as SAMBox does when its async writer reads stream contents while the parser reads the document, and views honor their
 * boundaries.
 */
public class SeekableSourceViewConformanceTest {

    private static final int SOURCE_SIZE = 3000;
    private static final int HANDED_OFF_VIEWS = 20;
    private static final long TIMEOUT_SECONDS = 10;

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

    @ParameterizedTest
    @MethodSource("sources")
    public void viewReadAfterParentIsClosed(SourceFactory factory) throws IOException {
        var source = factory.open(file);
        var view = source.view(100, 200);
        view.read();
        source.close();
        assertThrows(IllegalStateException.class, view::read);
        assertThrows(IllegalStateException.class, () -> view.read(ByteBuffer.allocate(10)));
    }

    @ParameterizedTest
    @MethodSource("sources")
    public void viewIsClosedWithItsSource(SourceFactory factory) throws IOException {
        var source = factory.open(file);
        var view = source.view(100, 200);
        var stream = view.asInputStream();
        source.close();
        assertFalse(view.isOpen());
        assertThrows(IOException.class, stream::read);
    }

    /**
     * A view and its parent read at the same time on different threads return the right bytes, so the view can't be reading through the parent's
     * position.
     */
    @ParameterizedTest
    @MethodSource("sources")
    public void viewReadOnPlatformThreadWhileParentIsRead(SourceFactory factory) throws Exception {
        assertViewReadWhileParentIsRead(factory, Executors.newSingleThreadExecutor());
    }

    /**
     * Same as {@link #viewReadOnPlatformThreadWhileParentIsRead(SourceFactory)} with the view read on a virtual thread, so views don't depend on the
     * reading thread being a platform thread.
     */
    @ParameterizedTest
    @MethodSource("sources")
    public void viewReadOnVirtualThreadWhileParentIsRead(SourceFactory factory) throws Exception {
        assertViewReadWhileParentIsRead(factory, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Views created on one thread and read on another while the first keeps reading the parent: the SAMBox pattern, where the parser creates the stream
     * views and the async writer reads them.
     */
    @ParameterizedTest
    @MethodSource("sources")
    public void viewsHandedOffToAnotherThread(SourceFactory factory) throws Exception {
        var writer = Executors.newSingleThreadExecutor();
        try (var source = factory.open(file)) {
            var handedOff = new LinkedBlockingQueue<HandedOffView>();
            var writes = writer.submit(() -> {
                for (var i = 0; i < HANDED_OFF_VIEWS; i++) {
                    var next = handedOff.poll(TIMEOUT_SECONDS, SECONDS);
                    assertNotNull(next, "No view was handed off");
                    readAndVerify(next.view(), next.start());
                }
                return null;
            });
            try {
                for (var i = 0; i < HANDED_OFF_VIEWS; i++) {
                    var start = i * 100L;
                    handedOff.put(new HandedOffView(start, source.view(start, 200)));
                    readAndVerify(source, 0);
                }
                writes.get(TIMEOUT_SECONDS, SECONDS);
            } finally {
                writer.shutdownNow();
            }
        }
    }

    private void assertViewReadWhileParentIsRead(SourceFactory factory, ExecutorService viewReader) throws Exception {
        try (var source = factory.open(file)) {
            var view = source.view(100, 2000);
            try {
                var viewReads = viewReader.submit(() -> {
                    readRepeatedly(view, 100);
                    return null;
                });
                readRepeatedly(source, 0);
                viewReads.get(TIMEOUT_SECONDS, SECONDS);
            } finally {
                viewReader.shutdownNow();
            }
        }
    }

    private static void readRepeatedly(SeekableSource source, long offset) throws IOException {
        for (var i = 0; i < 100; i++) {
            readAndVerify(source, offset);
        }
    }

    /**
     * Reads the whole source from its beginning, alternating buffer and single byte reads, and verifies each byte against the test file content starting at
     * the given offset.
     */
    private static void readAndVerify(SeekableSource source, long offset) throws IOException {
        source.position(0);
        var buffer = ByteBuffer.allocate(64);
        var index = 0L;
        for (var read = source.read(buffer); read != -1; read = source.read(buffer)) {
            assertArrayEquals(expectedBytes(offset + index, read), Arrays.copyOf(buffer.array(), read));
            index += read;
            buffer.clear();
            var single = source.read();
            if (single == -1) {
                break;
            }
            assertEquals(expectedByte(offset + index), single);
            index++;
        }
        assertEquals(source.size(), index);
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

    private record HandedOffView(long start, SeekableSource view) {
    }
}
