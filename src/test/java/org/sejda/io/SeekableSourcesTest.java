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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sejda.io.SeekableSources.DISABLE_MEMORY_MAPPED_PROPERTY;
import static org.sejda.io.SeekableSources.MAPPED_SIZE_THRESHOLD_PROPERTY;
import static org.sejda.io.SeekableSources.asOffsettable;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;
import static org.sejda.io.SeekableSources.onTempFileSeekableSourceFrom;
import static org.sejda.io.SeekableSources.seekableSourceFrom;
/**
 * @author Andrea Vacondio
 */
public class SeekableSourcesTest {

    private static String BITNESS;

    @BeforeAll
    public static void before() {
        BITNESS = System.getProperty("sun.arch.data.model");
        System.setProperty("sun.arch.data.model", "64");
    }

    @AfterAll
    public static void after() {
        System.setProperty("sun.arch.data.model", BITNESS);
    }

    @Test
    public void nullSeekableSourceFrom() {
        assertThrows(NullPointerException.class, () -> {
            seekableSourceFrom(null);
        });

    }

    @Test
    public void nullInMemorySeekableSourceFromBytes() {
        assertThrows(NullPointerException.class, () -> {
            inMemorySeekableSourceFrom((byte[]) null);
        });

    }

    @Test
    public void nullInMemorySeekableSourceFromStream() {
        assertThrows(NullPointerException.class, () -> {
            inMemorySeekableSourceFrom((InputStream) null);
        });
    }

    @Test
    public void nullOnTempFileSeekableSourceFrom() {
        assertThrows(NullPointerException.class, () -> {
            onTempFileSeekableSourceFrom(null);
        });
    }

    @Test
    public void nullOffsettableSource() {
        assertThrows(NullPointerException.class, () -> {
            asOffsettable(null);
        });
    }

    @Test
    public void seekableSourceFromTest(@TempDir Path temp) throws IOException {
        Path test = temp.resolve("test.txt");
        Files.createFile(test);
        assertNotNull(seekableSourceFrom(test.toFile()));
    }

    @Test
    public void aboveThresholdSeekableSourceFrom() throws IOException {
        try {
            System.setProperty(MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            Path tempFile = Files.createTempFile("SejdaIO", null);
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            SeekableSource source = seekableSourceFrom(tempFile.toFile());
            assertNotNull(source);
            assertTrue(source instanceof BufferedSeekableSource);
            assertTrue(((BufferedSeekableSource) source).wrapped() instanceof MemoryMappedSeekableSource);
        } finally {
            System.getProperties().remove(MAPPED_SIZE_THRESHOLD_PROPERTY);
        }
    }

    @Test
    public void disableMemoryMapped() throws IOException {
        try {
            System.setProperty(MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            System.setProperty(DISABLE_MEMORY_MAPPED_PROPERTY, "true");
            Path tempFile = Files.createTempFile("SejdaIO", null);
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            SeekableSource source = seekableSourceFrom(tempFile.toFile());
            assertNotNull(source);
            assertTrue(source instanceof BufferedSeekableSource);
            assertTrue(((BufferedSeekableSource) source).wrapped() instanceof FileChannelSeekableSource);
        } finally {
            System.getProperties().remove(MAPPED_SIZE_THRESHOLD_PROPERTY);
            System.getProperties().remove(DISABLE_MEMORY_MAPPED_PROPERTY);
        }
    }

    @Test
    public void nonMappedMemoryFor32bits() throws IOException {
        try {
            System.setProperty(MAPPED_SIZE_THRESHOLD_PROPERTY, "10");
            System.setProperty("sun.arch.data.model", "32");
            Path tempFile = Files.createTempFile("SejdaIO", null);
            Files.copy(getClass().getResourceAsStream("/pdf/simple_test.pdf"), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            SeekableSource source = seekableSourceFrom(tempFile.toFile());
            assertNotNull(source);
            assertTrue(source instanceof BufferedSeekableSource);
            assertTrue(((BufferedSeekableSource) source).wrapped() instanceof FileChannelSeekableSource);
        } finally {
            System.getProperties().remove(MAPPED_SIZE_THRESHOLD_PROPERTY);
            System.setProperty("sun.arch.data.model", BITNESS);
        }
    }

    @Test
    public void inMemorySeekableSourceFromBytes() {
        assertNotNull(inMemorySeekableSourceFrom(new byte[] { -1 }));
    }

    @Test
    public void inMemorySeekableSourceFromStream() throws IOException {
        assertNotNull(
                inMemorySeekableSourceFrom(getClass().getResourceAsStream("/pdf/simple_test.pdf")));
    }

    @Test
    public void asOffsettableTest() throws IOException {
        assertNotNull(asOffsettable(onTempFileSeekableSourceFrom(new ByteArrayInputStream(new byte[] { -1 }))));
    }

    @Test
    public void onTempFileSeekableSourceFromTest() throws IOException {
        assertNotNull(onTempFileSeekableSourceFrom(new ByteArrayInputStream(new byte[] { -1 })));
    }

    @Test
    public void onTempFileSeekableSourceFromWithFilenameHint() throws IOException {
        String filenameHint = "input.pdf";
        SeekableSource result = onTempFileSeekableSourceFrom(
                new ByteArrayInputStream(new byte[] { -1 }), filenameHint);
        assertThat(result.id(), endsWith(filenameHint));
    }

}
