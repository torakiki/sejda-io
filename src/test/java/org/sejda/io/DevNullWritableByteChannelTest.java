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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class DevNullWritableByteChannelTest {

    private DevNullWritableByteChannel victim;
    private ByteBuffer src = ByteBuffer.wrap(new byte[] { '1', '1', '2', '1', '1' });

    @BeforeEach
    public void setUp() {
        victim = new DevNullWritableByteChannel();
    }

    @Test
    public void write() {
        assertEquals(5, victim.write(src));
    }

    @Test
    public void close() {
        assertTrue(victim.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
    }
}
