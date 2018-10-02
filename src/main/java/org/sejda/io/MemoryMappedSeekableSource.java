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

import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireArg;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.sejda.io.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SeekableSource} implementation based on MappedByteBuffer. To overcome the int limit of the MappedByteBuffer, this source implement a pagination algorithm allowing to
 * open files of any size. The size of the pages can be configured using the {@link SeekableSources#MEMORY_MAPPED_PAGE_SIZE_PROPERTY} system property.
 * 
 * @author Andrea Vacondio
 *
 */
public class MemoryMappedSeekableSource extends BaseSeekableSource {
    private static final Logger LOG = LoggerFactory.getLogger(MemoryMappedSeekableSource.class);
    private static final long MB_256 = 1 << 28;

    private final long pageSize = Long.getLong(SeekableSources.MEMORY_MAPPED_PAGE_SIZE_PROPERTY, MB_256);
    private List<ByteBuffer> pages = new ArrayList<>();
    private long position;
    private long size;
    private ThreadBoundCopiesSupplier<MemoryMappedSeekableSource> localCopiesSupplier = new ThreadBoundCopiesSupplier<>(
            () -> new MemoryMappedSeekableSource(this));
    private Consumer<? super ByteBuffer> unmapper = IOUtils::unmap;

    public MemoryMappedSeekableSource(File file) throws IOException {
        super(ofNullable(file).map(File::getAbsolutePath).orElseThrow(() -> {
            return new IllegalArgumentException("Input file cannot be null");
        }));
        try (FileChannel channel = new RandomAccessFile(file, "r").getChannel()) {
            this.size = channel.size();
            int zeroBasedPagesNumber = (int) (channel.size() / pageSize);
            for (int i = 0; i <= zeroBasedPagesNumber; i++) {
                if (i == zeroBasedPagesNumber) {
                    pages.add(i, channel.map(MapMode.READ_ONLY, i * pageSize, channel.size() - (i * pageSize)));
                } else {
                    pages.add(i, channel.map(MapMode.READ_ONLY, i * pageSize, pageSize));
                }
            }
            LOG.debug("Created MemoryMappedSeekableSource with " + pages.size() + " pages");
        }
    }

    private MemoryMappedSeekableSource(MemoryMappedSeekableSource parent) {
        super(parent.id());
        this.size = parent.size;
        for (ByteBuffer page : parent.pages) {
            this.pages.add(page.duplicate());
        }
        // unmap doesn't work on duplicate, see Unsafe#invokeCleaner
        this.unmapper = null;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public SeekableSource position(long position) {
        requireArg(position >= 0, "Cannot set position to a negative value");
        this.position = Math.min(position, this.size);
        return this;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        requireOpen();
        int zeroBasedPagesNumber = (int) (position() / pageSize);
        ByteBuffer page = pages.get(zeroBasedPagesNumber);
        int relativePosition = (int) (position() - (zeroBasedPagesNumber * pageSize));
        if (relativePosition < page.limit()) {
            int read = readPage(dst, zeroBasedPagesNumber, relativePosition);
            while (dst.hasRemaining()) {
                int readBytes = readPage(dst, ++zeroBasedPagesNumber, 0);
                if (readBytes == 0) {
                    break;
                }
                read += readBytes;
            }
            position += read;
            return read;
        }
        return -1;
    }

    private int readPage(ByteBuffer dst, int pageNumber, int bufferPosition) {
        if (pageNumber < pages.size()) {
            ByteBuffer page = pages.get(pageNumber);
            page.position(bufferPosition);
            if (page.hasRemaining()) {
                int toRead = Math.min(dst.remaining(), page.remaining());
                byte[] bufToRead = new byte[toRead];
                page.get(bufToRead);
                dst.put(bufToRead);
                return toRead;
            }
        }
        return 0;
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        int zeroBasedPagesNumber = (int) (position() / pageSize);
        ByteBuffer page = pages.get(zeroBasedPagesNumber);
        int relativePosition = (int) (position() - (zeroBasedPagesNumber * pageSize));
        if (relativePosition < page.limit()) {
            position++;
            return page.get(relativePosition);
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        super.close();
        org.sejda.commons.util.IOUtils.close(localCopiesSupplier);
        Optional.ofNullable(unmapper).ifPresent(m -> pages.stream().forEach(m));
        pages.clear();
    }

    @Override
    public SeekableSource view(long startingPosition, long length) throws IOException {
        requireOpen();
        return new SeekableSourceView(localCopiesSupplier, id(), startingPosition, length);
    }

}
