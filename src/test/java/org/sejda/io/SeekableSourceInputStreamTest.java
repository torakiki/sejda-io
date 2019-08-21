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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourceInputStreamTest {

    private SeekableSource source;
    private SeekableSourceInputStream victim;

    @BeforeEach
    public void setUp() {
        source = mock(SeekableSource.class);
        when(source.isOpen()).thenReturn(true);
        victim = new SeekableSourceInputStream(source);
    }

    @Test
    public void nullSource() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SeekableSourceInputStream(null);
        }, "Cannot decorate a null instance");
    }

    @Test
    public void read() throws IOException {
        victim.read();
        verify(source).read();
    }

    @Test
    public void readClosed() {
        when(source.isOpen()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> {
            victim.read();
        });
    }

    @Test
    public void readByteArray() throws IOException {
        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        byte[] b = new byte[10];
        when(source.size()).thenReturn(20L);
        when(source.position()).thenReturn(0L);
        victim.read(b);
        verify(source).read(captor.capture());
        ByteBuffer captured = captor.getValue();
        assertEquals(10, captured.capacity());
        assertEquals(0, captured.position());
        assertTrue(captured.hasArray());
    }

    @Test
    public void readByteArrayClosed() {
        when(source.isOpen()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> {
            victim.read(new byte[10]);
        });
    }

    @Test
    public void readByteArrayWithPos() throws IOException {
        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        byte[] b = new byte[10];
        when(source.size()).thenReturn(20L);
        when(source.position()).thenReturn(0L);
        victim.read(b, 5, 2);
        verify(source).read(captor.capture());
        ByteBuffer captured = captor.getValue();
        assertEquals(10, captured.capacity());
        assertEquals(5, captured.position());
        assertEquals(7, captured.limit());
        assertTrue(captured.hasArray());
    }

    @Test
    public void readByteArrayWithPosClosed() {
        when(source.isOpen()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> {
            victim.read(new byte[10], 5, 2);
        });
    }

    @Test
    public void readByteArrayWhenEndOfStream() throws IOException {
        ByteArraySeekableSource source = new ByteArraySeekableSource(new byte[] { -1, 1, 0, 1 });
        SeekableSourceInputStream victim = new SeekableSourceInputStream(source);
        source.position(4);
        assertEquals(-1, source.read());
        byte[] b = new byte[10];
        assertEquals(-1, victim.read(b, 5, 2));
    }

    @Test
    public void available() throws IOException {
        when(source.size()).thenReturn(20L);
        when(source.position()).thenReturn(3L);
        assertEquals(17, victim.available());
    }

    @Test
    public void availableNotNegative() throws IOException {
        ByteArraySeekableSource source = new ByteArraySeekableSource(new byte[] { -1, 1, 0, 1 });
        SeekableSourceInputStream victim = new SeekableSourceInputStream(source);
        source.position(10);
        assertEquals(0, victim.available());
    }

    @Test
    public void close() throws IOException {
        victim.close();
        verify(source, never()).close();
    }

    @Test
    public void skip() throws IOException {
        ByteArraySeekableSource source = new ByteArraySeekableSource(new byte[] { -1, 1, 0, 1 });
        SeekableSourceInputStream victim = new SeekableSourceInputStream(source);
        assertEquals(0, source.position());
        victim.skip(2);
        assertEquals(2, source.position());
        assertEquals(2, victim.skip(1000));
    }

    @Test
    public void skipClosed() {
        when(source.isOpen()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> {
            victim.skip(5);
        });
    }
}
