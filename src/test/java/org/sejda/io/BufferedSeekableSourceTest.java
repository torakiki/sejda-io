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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class BufferedSeekableSourceTest extends BaseTestSeekableSource
{
    private ByteArraySeekableSource wrapped;
    private BufferedSeekableSource victim;

    @Before
    public void setUp()
    {
        wrapped = new ByteArraySeekableSource(new byte[] { 'a', 'b', 'c' });
        victim = new BufferedSeekableSource(wrapped);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failingConstructor()
    {
        new BufferedSeekableSource(null);
    }

    @Test
    public void constructor()
    {
        assertEquals(wrapped.id(), victim.id());
        assertEquals(wrapped.size(), victim.size());
    }

    @Override
    @Test
    public void close() throws IOException
    {
        victim.read();
        assertTrue(victim.isOpen());
        assertTrue(wrapped.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
        assertFalse(wrapped.isOpen());
    }

    @Test
    public void read() throws IOException
    {
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
    public void readBuff() throws IOException
    {
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
    public void position() throws IOException
    {
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

    @Override
    SeekableSource victim()
    {
        return victim;
    }
}
