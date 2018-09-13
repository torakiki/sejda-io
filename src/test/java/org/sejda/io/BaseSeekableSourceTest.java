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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class BaseSeekableSourceTest
{

    private BaseSeekableSource victim;

    @Before
    public void setUp()
    {
        victim = new BaseSeekableSource("id")
        {
            @Override
            public int read(ByteBuffer dst) 
            {
                return 0;
            }

            @Override
            public SeekableSource view(long startingPosition, long length) 
            {
                return null;
            }

            @Override
            public long size()
            {
                return 0;
            }

            @Override
            public int read()
            {
                return 0;
            }

            @Override
            public SeekableSource position(long position) 
            {
                return null;
            }

            @Override
            public long position() 
            {
                return 0;
            }
        };
    }

    @Test
    public void isOpen() throws IOException
    {
        assertTrue(victim.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
    }

    @Test
    public void requireOpen() throws IOException 
    {
        assertTrue(victim.isOpen());
        victim.requireOpen();
    }

    @Test(expected = IllegalStateException.class)
    public void failingRequireOpen() throws IOException
    {
        assertTrue(victim.isOpen());
        victim.close();
        victim.requireOpen();
    }

    @Test
    public void id()
    {
        assertEquals("id", victim.id());
    }

    @Test
    public void inputStreamFrom() {
        assertNotNull(victim.asInputStream());
    }

}
