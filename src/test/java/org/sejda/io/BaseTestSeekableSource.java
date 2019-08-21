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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sejda.commons.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public abstract class BaseTestSeekableSource {

    abstract SeekableSource victim();

    @AfterEach
    public void tearDown() throws IOException {
        IOUtils.close(victim());
    }

    @Test
    public void illegalPosition() {
        assertThrows(IllegalArgumentException.class, () -> {
            victim().position(-10);
        });
    }

    @Test
    public void viewClosed() throws IOException {
        victim().close();
        assertThrows(IllegalStateException.class, () -> {
            victim().view(0, 2);
        });
    }

    @Test
    public void view() throws IOException {
        assertNotNull(victim().view(0, 2));
    }

    @Test
    public void close() throws IOException {
        victim().read();
        assertTrue(victim().isOpen());
        victim().close();
        assertFalse(victim().isOpen());
    }

    @Test
    public void readClosed() throws IOException {
        victim().close();
        assertThrows(IllegalStateException.class, () -> {
            victim().read();
        });
    }

    @Test
    public void readByteBuffClosed() throws IOException {
        victim().close();
        assertThrows(IllegalStateException.class, () -> {
            victim().read(ByteBuffer.allocate(5));
        });
    }

    @Test
    public void forward() throws IOException {
        assertEquals(0, victim().position());
        assertEquals(1, victim().forward(1).position());
    }

    @Test
    public void invalidForward() throws IOException {
        assertEquals(0, victim().position());
        assertThrows(IllegalArgumentException.class, () -> {
            victim().forward(victim().size() + 1);
        });
    }

    @Test
    public void back() throws IOException {
        assertEquals(1, victim().forward(1).position());
        assertEquals(0, victim().back().position());
    }

    @Test
    public void invalidBack() throws IOException {
        assertEquals(0, victim().position());
        assertThrows(IllegalArgumentException.class, () -> {
            victim().back();
        });
    }

    @Test
    public void peek() throws IOException {
        assertEquals(0, victim().position());
        assertNotEquals(-1, victim().peek());
        assertEquals(0, victim().position());
    }

    @Test
    public void peekEOF() throws IOException {
        victim().position(victim().size());
        assertEquals(-1, victim().peek());
    }

    @Test
    public void peekBack() throws IOException {
        victim().position(victim().size());
        assertNotEquals(-1, victim().peekBack());
        assertEquals(victim().size(), victim().position());
    }

    @Test
    public void peekBackBeginning() throws IOException {
        assertEquals(0, victim().position());
        assertEquals(-1, victim().peekBack());
    }

}
