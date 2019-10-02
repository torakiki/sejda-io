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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andrea Vacondio
 */
public class FileChannelSeekableSourceTest extends BaseTestSeekableSource
{
    private FileChannelSeekableSource victim;
    private Path tempFile;

    @BeforeEach
    public void setUp() throws Exception
    {
        tempFile = Files.createTempFile("SejdaIO", null);
        Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        victim = new FileChannelSeekableSource(tempFile.toFile());
    }

    @AfterEach
    public void after() throws IOException
    {
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void failingConstructor()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            new FileChannelSeekableSource(null);
        }, "Input file cannot be null");
    }

    @Test
    public void read() throws IOException
    {
        assertEquals(0, victim.position());
        assertNotNull(victim.read());
        assertNotNull(victim.read());
        assertEquals(2, victim.position());
        victim.position(victim.size());
        assertEquals(-1, victim.read());
    }

    @Test
    public void readBuff() throws IOException
    {
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
    public void reset() throws IOException
    {
        ByteBuffer dst = ByteBuffer.allocate(20);
        victim.read(dst);
        assertEquals(20, victim.position());
        victim.reset();
        assertEquals(0, victim.position());
    }

    @Test
    public void newInputStream() throws IOException
    {
        int length = 20;
        byte[] buffer1 = new byte[length];
        byte[] buffer2 = new byte[length];

        victim.asNewInputStream().read(buffer1);
        victim.asNewInputStream().read(buffer2);

        assertArrayEquals(buffer1, buffer2);
        assertEquals(length, victim.position());
    }

    @Override
    SeekableSource victim()
    {
        return victim;
    }

}
