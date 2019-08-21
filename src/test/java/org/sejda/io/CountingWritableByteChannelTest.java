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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CountingWritableByteChannelTest {
    private ByteArrayOutputStream out;
    private CountingWritableByteChannel victim;
    private WritableByteChannel wrapped;
    private ByteBuffer src = ByteBuffer.wrap(new byte[] { '1', '1', '2', '1', '1' });

    @BeforeEach
    public void setUp() {
        out = new ByteArrayOutputStream();
        wrapped = Channels.newChannel(out);
        victim = new CountingWritableByteChannel(wrapped);
    }

    @Test
    public void nullConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CountingWritableByteChannel(null);
        }, "Cannot decorate a null instance");
    }

    @Test
    public void count() throws Exception {
        assertEquals(0, victim.count());
        victim.write(src);
        assertEquals(5, victim.count());
    }

    @Test
    public void closedWrite() throws Exception {
        victim.close();
        assertThrows(ClosedChannelException.class, () -> {
            victim.write(src);
        });
    }

    @Test
    public void write() throws Exception {
        victim.write(src);
        assertTrue(Arrays.equals(out.toByteArray(), src.array()));
    }

    @Test
    public void isOpen() {
        assertTrue(victim.isOpen());
        assertTrue(wrapped.isOpen());
    }

    @Test
    public void close() throws Exception {
        assertTrue(victim.isOpen());
        assertTrue(wrapped.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
        assertFalse(wrapped.isOpen());
    }

    @Test
    public void testFromWritableByteChannel() throws Exception {
        victim = CountingWritableByteChannel.from(Channels.newChannel(out));
        victim.write(src);
        assertTrue(Arrays.equals(out.toByteArray(), src.array()));
    }

    @Test
    public void fromOutputStream() throws Exception {
        victim = CountingWritableByteChannel.from(out);
        victim.write(src);
        assertTrue(Arrays.equals(out.toByteArray(), src.array()));
    }

    @Test
    public void fromFile() throws Exception {
        Path tempFile = Files.createTempFile("SAMBox", null);
        try {
            assertEquals(0, Files.size(tempFile));
            victim = CountingWritableByteChannel.from(tempFile.toFile());
            victim.write(src);
            assertTrue(Arrays.equals(Files.readAllBytes(tempFile), src.array()));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void fromString() throws Exception {
        Path tempFile = Files.createTempFile("SAMBox", null);
        try {
            assertEquals(0, Files.size(tempFile));
            victim = CountingWritableByteChannel.from(tempFile.toAbsolutePath().toString());
            victim.write(src);
            assertTrue(Arrays.equals(Files.readAllBytes(tempFile), src.array()));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}
