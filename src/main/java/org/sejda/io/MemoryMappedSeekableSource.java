/*
 * Copyright 2018 Sober Lemur S.r.l.
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

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

/**
 * A {@link SeekableSource} implementation mapping the whole file into memory as a single {@link MemorySegment}, so files of any size can be opened.
 *
 * @author Andrea Vacondio
 *
 */
public class MemoryMappedSeekableSource extends BaseSeekableSource {
    private final Arena arena;
    private final MemorySegment segment;
    private long position;

    public MemoryMappedSeekableSource(Path path) throws IOException {
        requireNotNullArg(path, "Input path cannot be null");
        super(path.toAbsolutePath().toString());
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            this.arena = Arena.ofShared();
            try {
                this.segment = channel.map(MapMode.READ_ONLY, 0, channel.size(), arena);
            } catch (IOException | RuntimeException e) {
                arena.close();
                throw e;
            }
        }
    }

    public MemoryMappedSeekableSource(File file) throws IOException {
        requireNotNullArg(file, "Input file cannot be null");
        this(file.toPath());
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public long size() {
        return segment.byteSize();
    }

    @Override
    public SeekableSource position(long position) {
        requireArg(position >= 0, "Cannot set position to a negative value");
        this.position = Math.min(position, size());
        return this;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        requireOpen();
        int read = readAt(dst, position);
        if (read > 0) {
            position += read;
        }
        return read;
    }

    /**
     * Copies from the segment at absolute indexes only, so the source and its views can read it concurrently.
     */
    private int readAt(ByteBuffer dst, long start) {
        if (!dst.hasRemaining()) {
            return 0;
        }
        if (start >= size()) {
            return -1;
        }
        int length = (int) Math.min(dst.remaining(), size() - start);
        if (dst.hasArray()) {
            MemorySegment.copy(segment, JAVA_BYTE, start, dst.array(), dst.arrayOffset() + dst.position(), length);
        } else {
            MemorySegment.copy(segment, start, MemorySegment.ofBuffer(dst), 0, length);
        }
        dst.position(dst.position() + length);
        return length;
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        if (position < size()) {
            return segment.get(JAVA_BYTE, position++) & 0xff;
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (arena.scope().isAlive()) {
            arena.close();
        }
    }

    @Override
    public SeekableSource view(long startingPosition, long length) {
        requireOpen();
        return new SeekableSourceView(this, this::readAt, startingPosition, length);
    }

}
