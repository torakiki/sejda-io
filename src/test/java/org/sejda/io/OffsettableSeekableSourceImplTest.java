package org.sejda.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Copyright 2022 Sober Lemur S.r.l. and Sejda BV
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
public class OffsettableSeekableSourceImplTest extends BaseTestSeekableSource {

    private ByteArraySeekableSource wrapped;
    private OffsettableSeekableSource victim;

    @BeforeEach
    public void setUp() throws IOException {
        wrapped = new ByteArraySeekableSource(new byte[] { 'd', 'e', 'f', 'a', 'b', 'c' });
        victim = SeekableSources.asOffsettable(wrapped);
        victim.offset(3);
    }

    @Test
    public void size() {
        assertEquals(3, victim.size());
    }

    @Test
    public void failingConstructor() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new OffsettableSeekableSourceImpl(null));
        assertEquals("Input decorated SeekableSource cannot be null", e.getMessage());
    }

    @Test
    public void negativeOffset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> victim.offset(-4));
        assertEquals("Cannot set a negative offset", e.getMessage());
    }

    @Test
    public void tooBigOffset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> victim.offset(7));
        assertEquals("Invalid offset bigger then the wrapped source size", e.getMessage());
    }

    @Test
    public void constructor() {
        assertEquals(wrapped.id(), victim.id());
    }

    @Override
    @Test
    public void close() throws IOException {
        victim.read();
        assertTrue(victim.isOpen());
        assertTrue(wrapped.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
        assertFalse(wrapped.isOpen());
    }

    @Test
    public void read() throws IOException {
        assertEquals(97, victim.read());
        assertEquals(1, victim.position());
        assertEquals(98, victim.read());
        assertEquals(2, victim.position());
        assertEquals(99, victim.read());
        assertEquals(3, victim.position());
        assertEquals(-1, victim.read());
        assertEquals(3, victim.position());
    }

    @Test
    public void readBuff() throws IOException {
        victim.position(1);
        ByteBuffer dst = ByteBuffer.allocate(10);
        victim.read(dst);
        dst.flip();
        assertEquals(2, dst.remaining());
        assertEquals(98, dst.get());
        assertEquals(99, dst.get());
        ByteBuffer empty = ByteBuffer.allocate(10);
        victim.read(empty);
        empty.flip();
        assertFalse(empty.hasRemaining());
    }

    @Test
    public void position() throws IOException {
        assertEquals(0, victim.position());
        assertEquals(97, victim.read());
        victim.position(0);
        assertEquals(0, victim.position());
        victim.position(0);
        assertEquals(0, victim.position());
        victim.position(2);
        assertEquals(2, victim.position());
        victim.read();
        assertEquals(3, victim.position());
        assertEquals(-1, victim.read());
        victim.position(2);
        assertEquals(2, victim.position());
        victim.position(20);
        assertEquals(3, victim.position());
        assertEquals(-1, victim.read());
    }

    @Test
    public void closedWrapped() throws IOException {
        this.wrapped.close();
        assertFalse(victim.isOpen());
    }

    @Override
    SeekableSource victim() {
        return victim;
    }
}