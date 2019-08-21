/*
 * Copyright 2018 Sober Lemur S.a.s. di Vacondio Andrea
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourceViewTest extends BaseTestSeekableSource {
    private SeekableSourceView victim;
    private Path tempFile;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("SejdaIO", null);
        Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        victim = new SeekableSourceView(() -> new FileChannelSeekableSource(tempFile.toFile()), "id", 50, 100);
    }

    @AfterEach
    public void after() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void nullSourceConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SeekableSourceView(null, "id", 50, 100);
        }, "Input decorated SeekableSource cannot be null");
    }

    @Test
    public void negativeStartPositionConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SeekableSourceView(() -> new ByteArraySeekableSource(new byte[] { -1 }), "id", -10, 100);
        }, "Starting position cannot be negative");
    }

    @Test
    public void outOfBoundsStartPositionConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SeekableSourceView(() -> new ByteArraySeekableSource(new byte[] { -1, 2 }), "id", 3, 100);
        }, "Starting position cannot be higher then wrapped source size");
    }

    @Test
    public void nullNonPositiveLengthConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SeekableSourceView(() -> new ByteArraySeekableSource(new byte[] { -1 }), "id", 0, 0);
        }, "View length must be positive");
    }

    @Test
    public void size() {
        assertEquals(100, victim.size());
    }

    @Test
    public void sizeTrimmed() throws IOException {
        assertEquals(2,
                new SeekableSourceView(() -> new ByteArraySeekableSource(new byte[] { -1, 2 }), "id", 0, 100).size());
    }

    @Override
    @Test
    public void view() throws IOException {
        assertThrows(RuntimeException.class, () -> {
            victim().view(0, 2);
        }, "Cannot create a view of a view");
    }

    @Override
    @Test
    public void viewClosed() throws IOException {
        victim().close();
        assertThrows(RuntimeException.class, () -> {
            victim().view(0, 2);
        }, "Cannot create a view of a view");
    }

    @Test
    public void parentClosed() throws IOException {
        ByteArraySeekableSource wrapped = new ByteArraySeekableSource(new byte[] { -1 });
        victim = new SeekableSourceView(() -> wrapped, "id", 0, 1);
        wrapped.close();
        assertTrue(victim.isOpen());
        assertThrows(IllegalStateException.class, () -> {
            victim.read();
        });
    }

    @Test
    public void closeDoesntCloseParent() throws IOException {
        ByteArraySeekableSource wrapped = new ByteArraySeekableSource(new byte[] { -1 });
        victim = new SeekableSourceView(() -> wrapped, "id", 0, 1);
        victim.close();
        assertTrue(wrapped.isOpen());
    }

    @Override
    SeekableSource victim() {
        return victim;
    }

    @Test
    public void read() throws IOException {
        assertEquals(0, victim.position());
        assertNotNull(victim.read());
        assertNotNull(victim.read());
        assertEquals(2, victim.position());
        victim.position(victim.size());
        assertEquals(-1, victim.read());
    }

    @Test
    public void readBuff() throws IOException {
        victim.position(1);
        ByteBuffer dst = ByteBuffer.allocate(10);
        victim.read(dst);
        dst.flip();
        assertEquals(10, dst.remaining());
        victim.position(victim.size());
        ByteBuffer empty = ByteBuffer.allocate(10);
        victim.read(empty);
        empty.flip();
        assertFalse(empty.hasRemaining());
    }

    @Test
    public void readBigBuff() throws IOException {
        ByteBuffer dst = ByteBuffer.allocate(8000);
        assertEquals(100, victim.read(dst));
    }

}
