/*
 * Copyright 2018 Sober Lemur S.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.SPARSE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MemoryMappedSeekableSourceTest extends BaseTestSeekableSource {
    private MemoryMappedSeekableSource victim;
    private Path tempFile;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("SejdaIO", null);
        Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        victim = new MemoryMappedSeekableSource(tempFile.toFile());
    }

    @AfterEach
    public void after() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void nullFile() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryMappedSeekableSource((File) null),
                "Input file cannot be null");
    }

    @Test
    public void nullPath() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryMappedSeekableSource((Path) null),
                "Input file cannot be null");

    }

    @Test
    public void read() throws IOException {
        assertEquals(0, victim.position());
        assertNotEquals(-1, victim.read());
        assertNotEquals(-1, victim.read());
        assertEquals(2, victim.position());
        victim.position(victim.size());
        assertEquals(-1, victim.read());
    }

    @Test
    public void readUnsigned() throws IOException {
        var bytesFile = Files.write(Files.createTempFile("SejdaIO", null), new byte[] { 0, 127, -128, -1 });
        try (var source = new MemoryMappedSeekableSource(bytesFile)) {
            assertEquals(0, source.read());
            assertEquals(127, source.read());
            assertEquals(128, source.read());
            assertEquals(255, source.read());
            assertEquals(-1, source.read());
        } finally {
            Files.deleteIfExists(bytesFile);
        }
    }

    @Test
    public void viewRead() throws IOException {
        byte[] expected = Files.readAllBytes(tempFile);
        SeekableSource view = victim.view(40, 30);
        for (int i = 40; i < 70; i++) {
            assertEquals(expected[i] & 0xff, view.read());
        }
        assertEquals(-1, view.read());
    }

    @Test
    public void viewReadBuff() throws IOException {
        byte[] expected = Files.readAllBytes(tempFile);
        SeekableSource view = victim.view(30, 120);
        ByteBuffer dst = ByteBuffer.allocate(120);
        assertEquals(120, view.read(dst));
        assertArrayEquals(Arrays.copyOfRange(expected, 30, 150), dst.array());
    }

    @Test
    public void readBuff() throws IOException {
        ByteBuffer dst = ByteBuffer.allocate(20);
        victim.read(dst);
        dst.flip();
        assertEquals(20, dst.remaining());
        assertEquals(20, victim.position());
        victim.position(victim.size());
        ByteBuffer empty = ByteBuffer.allocate(10);
        victim.read(empty);
        empty.flip();
        assertFalse(empty.hasRemaining());
    }

    /**
     * Reads into a buffer whose position isn't 0, as {@link SeekableSourceInputStream} does when reading at an array offset.
     */
    @Test
    public void readBuffFromBufferPosition() throws IOException {
        byte[] expected = Files.readAllBytes(tempFile);
        victim.position(30);
        ByteBuffer dst = ByteBuffer.allocate(100).position(10);
        assertEquals(90, victim.read(dst));
        assertEquals(100, dst.position());
        assertArrayEquals(Arrays.copyOfRange(expected, 30, 120), Arrays.copyOfRange(dst.array(), 10, 100));
    }

    @Test
    public void readDirectBuffFromBufferPosition() throws IOException {
        byte[] expected = Files.readAllBytes(tempFile);
        victim.position(30);
        ByteBuffer dst = ByteBuffer.allocateDirect(100).position(10);
        assertEquals(90, victim.read(dst));
        assertEquals(100, dst.position());
        byte[] read = new byte[90];
        dst.get(10, read);
        assertArrayEquals(Arrays.copyOfRange(expected, 30, 120), read);
    }

    @Test
    public void readBuffAtEnd() throws IOException {
        byte[] expected = Files.readAllBytes(tempFile);
        victim.position(victim.size() - 70);
        ByteBuffer dst = ByteBuffer.allocate(120);
        assertEquals(70, victim.read(dst));
        assertArrayEquals(Arrays.copyOfRange(expected, expected.length - 70, expected.length), Arrays.copyOf(dst.array(), 70));
        assertEquals(-1, victim.read(ByteBuffer.allocate(10)));
    }

    /**
     * A file bigger than 4GB, sparse so it takes no disk space, read on both sides of the 2GB and 4GB boundaries and at its end.
     */
    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "The file must be sparse")
    public void readBigFile(@TempDir Path tempDir) throws IOException {
        long size = (5L << 30) + 123;
        long[] markers = { (1L << 31) - 5, (1L << 32) - 5, size - 10 };
        Path bigFile = tempDir.resolve("big.bin");
        try (var channel = FileChannel.open(bigFile, CREATE_NEW, WRITE, SPARSE)) {
            for (long start : markers) {
                channel.write(ByteBuffer.wrap(marker(start)), start);
            }
        }
        try (var source = new MemoryMappedSeekableSource(bigFile)) {
            assertEquals(size, source.size());
            for (long start : markers) {
                ByteBuffer dst = ByteBuffer.allocate(10);
                assertEquals(10, source.position(start).read(dst));
                assertArrayEquals(marker(start), dst.array());
                assertEquals(marker(start)[0] & 0xff, source.position(start).read());
                ByteBuffer viewDst = ByteBuffer.allocate(10);
                assertEquals(10, source.view(start, 10).read(viewDst));
                assertArrayEquals(marker(start), viewDst.array());
            }
            assertEquals(-1, source.position(size).read());
        }
    }

    private static byte[] marker(long start) {
        byte[] marker = new byte[10];
        for (int i = 0; i < marker.length; i++) {
            marker[i] = (byte) (start + i);
        }
        return marker;
    }

    @Override
    SeekableSource victim() {
        return victim;
    }

}
