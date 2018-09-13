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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourcesTest {

    private static String BITNESS;

    @BeforeClass
    public static void before() {
        BITNESS = System.getProperty("sun.arch.data.model");
        System.setProperty("sun.arch.data.model", "64");
    }

    @AfterClass
    public static void after() {
        System.setProperty("sun.arch.data.model", BITNESS);
    }

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void nullSeekableSourceFrom() throws IOException {
        SeekableSources.seekableSourceFrom(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullInMemorySeekableSourceFromBytes() {
        SeekableSources.inMemorySeekableSourceFrom((byte[]) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullInMemorySeekableSourceFromStream() throws IOException {
        SeekableSources.inMemorySeekableSourceFrom((InputStream) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullOnTempFileSeekableSourceFrom() throws IOException {
        SeekableSources.onTempFileSeekableSourceFrom(null);
    }

    @Test
    public void seekableSourceFrom() throws IOException {
        assertNotNull(SeekableSources.seekableSourceFrom(temp.newFile()));
    }

    @Test
    public void aboveThresholdSeekableSourceFrom() throws IOException {
        try {
            System.setProperty(SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            Path tempFile = Files.createTempFile("SejdaIO", null);
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            SeekableSource source = SeekableSources.seekableSourceFrom(tempFile.toFile());
            assertNotNull(source);
            assertTrue(source instanceof BufferedSeekableSource);
            assertTrue(((BufferedSeekableSource) source).wrapped() instanceof MemoryMappedSeekableSource);
        } finally {
            System.getProperties().remove(SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY);
        }
    }

    @Test
    public void disableMemoryMapped() throws IOException {
        try {
            System.setProperty(SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            System.setProperty(SeekableSources.DISABLE_MEMORY_MAPPED_PROPERTY, "true");
            Path tempFile = Files.createTempFile("SejdaIO", null);
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            SeekableSource source = SeekableSources.seekableSourceFrom(tempFile.toFile());
            assertNotNull(source);
            assertTrue(source instanceof BufferedSeekableSource);
            assertTrue(((BufferedSeekableSource) source).wrapped() instanceof FileChannelSeekableSource);
        } finally {
            System.getProperties().remove(SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY);
            System.getProperties().remove(SeekableSources.DISABLE_MEMORY_MAPPED_PROPERTY);
        }
    }

    @Test
    public void nonMappedMemoryFor32bits() throws IOException {
        try {
            System.setProperty(SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            System.setProperty("sun.arch.data.model", "32");
            Path tempFile = Files.createTempFile("SejdaIO", null);
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            SeekableSource source = SeekableSources.seekableSourceFrom(tempFile.toFile());
            assertNotNull(source);
            assertTrue(source instanceof BufferedSeekableSource);
            assertTrue(((BufferedSeekableSource) source).wrapped() instanceof FileChannelSeekableSource);
        } finally {
            System.getProperties().remove(SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY);
            System.setProperty("sun.arch.data.model", BITNESS);
        }
    }

    @Test
    public void inMemorySeekableSourceFromBytes() {
        assertNotNull(SeekableSources.inMemorySeekableSourceFrom(new byte[] { -1 }));
    }

    @Test
    public void inMemorySeekableSourceFromStream() throws IOException {
        assertNotNull(
                SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream("/pdf/simple_test.pdf")));
    }

    @Test
    public void onTempFileSeekableSourceFrom() throws IOException {
        assertNotNull(SeekableSources.onTempFileSeekableSourceFrom(new ByteArrayInputStream(new byte[] { -1 })));
    }

}
