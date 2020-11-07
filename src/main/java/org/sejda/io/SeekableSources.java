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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.sejda.commons.util.IOUtils;

/**
 * This class consists of solely static methods to create the most appropriate {@link SeekableSource} based on the given input or to bridge {@link SeekableSource}s to the more
 * traditional {@link InputStream} or other standard I/O classes.
 * 
 * @author Andrea Vacondio
 *
 */
public final class SeekableSources {

    /**
     * Threshold size in bytes where the SeekableSources method will switch to {@link MemoryMappedSeekableSource#MemoryMappedSeekableSource(File)}
     */
    public static final String MAPPED_SIZE_THRESHOLD_PROPERTY = "org.sejda.io.mapped.size.threshold";
    /**
     * Threshold size in bytes where the SeekableSources method will switch to {@link MemoryMappedSeekableSource#MemoryMappedSeekableSource(File)}
     */
    public static final String DISABLE_MEMORY_MAPPED_PROPERTY = "org.sejda.io.mapped.disabled";
    /**
     * Buffer size for {@link BufferedSeekableSource}
     */
    public static final String INPUT_BUFFER_SIZE_PROPERTY = "org.sejda.io.buffered.input.size";
    /**
     * Size of the pages used by {@link MemoryMappedSeekableSource}
     */
    public static final String MEMORY_MAPPED_PAGE_SIZE_PROPERTY = "org.sejda.io.memory.mapped.page.size";

    private static final long MB_16 = 1 << 24;

    private SeekableSources() {
        // utility
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link File}. An attempt is made to return the best {@link SeekableSource} implementation based on the size of the
     * file and bitness of the JVM.
     * 
     * @param file
     * @return a {@link SeekableSource} from the given file.
     * @throws IOException
     */
    public static SeekableSource seekableSourceFrom(File file) throws IOException {
        requireNonNull(file);
        if (!"32".equals(System.getProperty("sun.arch.data.model"))
                && !Boolean.getBoolean(DISABLE_MEMORY_MAPPED_PROPERTY)
                && file.length() > Long.getLong(MAPPED_SIZE_THRESHOLD_PROPERTY, MB_16)) {
            return new BufferedSeekableSource(new MemoryMappedSeekableSource(file));
        }
        return new BufferedSeekableSource(new FileChannelSeekableSource(file));
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is read an stored in a byte array with a max size of 2GB.
     * 
     * @param stream
     * @return a {@link SeekableSource} from the given stream.
     * @throws IOException
     */
    public static SeekableSource inMemorySeekableSourceFrom(InputStream stream) throws IOException {
        requireNonNull(stream);
        return new ByteArraySeekableSource(IOUtils.toByteArray(stream));
    }

    /**
     * Factory method to create a {@link SeekableSource} from a byte array.
     * 
     * @param bytes
     * @return a {@link SeekableSource} wrapping the given byte array.
     */
    public static SeekableSource inMemorySeekableSourceFrom(byte[] bytes) {
        requireNonNull(bytes);
        return new ByteArraySeekableSource(bytes);
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is copied to a temporary file.
     *
     * @param stream
     * @return a {@link SeekableSource} from the given stream.
     * @throws IOException
     */
    public static SeekableSource onTempFileSeekableSourceFrom(InputStream stream) throws IOException {
        return onTempFileSeekableSourceFrom(stream, "SejdaIO");
    }

    /**
     * Factory method to create a {@link SeekableSource} from a {@link InputStream}. The whole stream is copied to a temporary file.
     * 
     * @param stream
     * @param filenameHint name to use for the temp file that will be created
     * @return a {@link SeekableSource} from the given stream.
     * @throws IOException
     */
    public static SeekableSource onTempFileSeekableSourceFrom(InputStream stream, String filenameHint) throws IOException {
        requireNonNull(stream);
        File tempDir = Files.createTempDirectory("SejdaIODir").toFile();
        File temp = new File(tempDir, filenameHint);
        if(temp.exists()) {
            throw new RuntimeException("Temp file collision: "+ temp.getAbsolutePath());
        }
        
        Files.copy(stream, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return new BufferedSeekableSource(new FileChannelSeekableSource(temp) {
            @Override
            public void close() throws IOException {
                super.close();
                Files.deleteIfExists(temp.toPath());
            }
        });
    }
}
