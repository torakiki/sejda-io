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

import org.sejda.commons.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

/**
 * {@link SeekableSource} wrapping an existing one and providing buffered read. When {@link #read()} is called, a {@link SeekableSources#INPUT_BUFFER_SIZE_PROPERTY} long chunk
 * of bytes is read from the underlying source and stored in memory. Subsequent reads are served from the in memory buffer until they fall outside its range, at that point a
 * new buffer is read from the wrapped source. {@link #read(ByteBuffer)} reads straight from the wrapped source.
 *
 * @author Andrea Vacondio
 */
public class BufferedSeekableSource implements SeekableSource {

    /**
     * Allocated on the first {@link #read()}, since many sources are only read in bulk through {@link #read(ByteBuffer)}, which doesn't use it
     */
    private ByteBuffer buffer = ByteBuffer.allocate(0);
    private final SeekableSource wrapped;
    private long position;
    private final long size;

    public BufferedSeekableSource(SeekableSource wrapped) {
        requireNotNullArg(wrapped, "Input decorated SeekableSource cannot be null");
        this.wrapped = wrapped;
        this.size = wrapped.size();
    }

    @Override
    public boolean isOpen() {
        return this.wrapped.isOpen();
    }

    @Override
    public void requireOpen() {
        this.wrapped.requireOpen();
    }

    /**
     * @return the unique id for this source
     */
    @Override
    public String id() {
        return this.wrapped.id();
    }

    @Override
    public long position() {
        return this.position;
    }

    @Override
    public SeekableSource position(long newPosition) throws IOException {
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        if (newPosition != this.position) {
            long newBufPosition = newPosition - position + buffer.position();
            if (newBufPosition >= 0 && newBufPosition < buffer.limit()) {
                buffer.position((int) newBufPosition);
            } else {
                buffer.limit(0);
                wrapped.position(newPosition);
            }
            this.position = Math.min(newPosition, size);
        }
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(wrapped);
        buffer = ByteBuffer.allocate(0);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        requireOpen();
        buffer.limit(0);
        wrapped.position(position);
        int read = wrapped.read(dst);
        if (read > 0) {
            position += read;
        }
        return read;
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        if (ensureBuffer() > 0) {
            position++;
            return buffer.get() & 0xff;
        }
        return -1;
    }

    private int ensureBuffer() throws IOException {
        if (!buffer.hasRemaining()) {
            allocateBufferIfNeeded();
            buffer.clear();
            wrapped.read(buffer);
            buffer.flip();
        }
        return buffer.remaining();
    }

    private void allocateBufferIfNeeded() {
        if (buffer.capacity() == 0) {
            buffer = ByteBuffer.allocate(Integer.getInteger(SeekableSources.INPUT_BUFFER_SIZE_PROPERTY, 8192));
        }
    }

    protected SeekableSource wrapped() {
        return wrapped;
    }

    @Override
    public SeekableSource view(long startingPosition, long length) throws IOException {
        requireOpen();
        return new BufferedSeekableSource(wrapped.view(startingPosition, length));
    }
}
