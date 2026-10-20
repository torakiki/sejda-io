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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

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
        System.getProperties().remove(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY);
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
    public void pagedRead() throws IOException {
        System.setProperty(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, "50");
        Path tempFile = Files.createTempFile("SejdaIO", null);
        try {
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            victim = new MemoryMappedSeekableSource(tempFile);
            victim.position(49);
            assertNotEquals(-1, victim.read());
            assertNotEquals(-1, victim.read());
            assertNotEquals(-1, victim.read());
            assertEquals(52, victim.position());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void pagedViewRead() throws IOException {
        System.setProperty(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, "50");
        Path tempFile = Files.createTempFile("SejdaIO", null);
        try {
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            byte[] expected = Files.readAllBytes(tempFile);
            victim = new MemoryMappedSeekableSource(tempFile);
            SeekableSource view = victim.view(40, 30);
            for (int i = 40; i < 70; i++) {
                assertEquals(expected[i] & 0xff, view.read());
            }
            assertEquals(-1, view.read());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void pagedViewReadBuff() throws IOException {
        System.setProperty(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, "50");
        Path tempFile = Files.createTempFile("SejdaIO", null);
        try {
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            byte[] expected = Files.readAllBytes(tempFile);
            victim = new MemoryMappedSeekableSource(tempFile);
            SeekableSource view = victim.view(30, 120);
            ByteBuffer dst = ByteBuffer.allocate(120);
            assertEquals(120, view.read(dst));
            assertArrayEquals(Arrays.copyOfRange(expected, 30, 150), dst.array());
        } finally {
            Files.deleteIfExists(tempFile);
        }
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

    @Test
    public void pagedReadBuff() throws IOException {
        System.setProperty(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, "50");
        Path tempFile = Files.createTempFile("SejdaIO", null);
        try {
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            victim = new MemoryMappedSeekableSource(tempFile.toFile());
            ByteBuffer dst = ByteBuffer.allocate(70);
            victim.read(dst);
            dst.flip();
            assertEquals(70, dst.remaining());
            assertEquals(70, victim.position());
            victim.position(victim.size() - 70);
            ByteBuffer dst2 = ByteBuffer.allocate(120);
            victim.read(dst2);
            dst2.flip();
            assertEquals(70, dst2.remaining());
            victim.position(victim.size());
            ByteBuffer empty = ByteBuffer.allocate(10);
            victim.read(empty);
            empty.flip();
            assertFalse(empty.hasRemaining());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    SeekableSource victim() {
        return victim;
    }

}
