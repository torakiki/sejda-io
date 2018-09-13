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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MemoryMappedSeekableSourceTest extends BaseTestSeekableSource
{
    private MemoryMappedSeekableSource victim;
    private Path tempFile;

    @Before
    public void setUp() throws Exception
    {
        tempFile = Files.createTempFile("SejdaIO", null);
        Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        victim = new MemoryMappedSeekableSource(tempFile.toFile());
    }

    @After
    public void after() throws IOException
    {
        System.getProperties().remove(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY);
        Files.deleteIfExists(tempFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failingConstructor() throws IOException
    {
        new MemoryMappedSeekableSource(null);
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
    public void pagedRead() throws IOException
    {
        System.setProperty(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, "50");
        Path tempFile = Files.createTempFile("SejdaIO", null);
        try
        {
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            victim = new MemoryMappedSeekableSource(tempFile.toFile());
            victim.position(49);
            assertNotNull(victim.read());
            assertNotNull(victim.read());
            assertNotNull(victim.read());
            assertEquals(52, victim.position());
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
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
    public void pagedReadBuff() throws IOException
    {
        System.setProperty(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, "50");
        Path tempFile = Files.createTempFile("SejdaIO", null);
        try
        {
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
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    SeekableSource victim()
    {
        return victim;
    }

}
