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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import static java.util.Objects.isNull;
import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

/**
 * A byte array based {@link SeekableSource} with a max size of 2GB.
 *
 * @author Andrea Vacondio
 */
public class ByteArraySeekableSource extends BaseSeekableSource {
    private byte[] bytes;
    private long position;
    private String id;

    public ByteArraySeekableSource(byte[] bytes) {
        requireNotNullArg(bytes, "Input byte array cannot be null");
        this.bytes = bytes;
    }

    /**
     * @return a name based UUID of the bytes, computed the first time it's requested, since it's costly for big arrays and often never requested
     * @throws IllegalStateException if first requested after the source is closed
     */
    @Override
    public String id() {
        if (isNull(id)) {
            requireOpen();
            id = UUID.nameUUIDFromBytes(bytes).toString();
        }
        return id;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableSource position(long position) {
        requireArg(position >= 0, "Cannot set position to a negative value");
        this.position = Math.min(position, size());
        return this;
    }

    @Override
    public long size() {
        return bytes.length;
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

    private int readAt(ByteBuffer dst, long start) {
        if (!dst.hasRemaining()) {
            return 0;
        }
        if (start < size()) {
            int toCopy = (int) Math.min(dst.remaining(), size() - start);
            dst.put(bytes, (int) start, toCopy);
            return toCopy;
        }
        return -1;
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        if (position < size()) {
            return bytes[(int) position++] & 0xff;
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.bytes = new byte[0];
    }

    @Override
    public SeekableSource view(long startingPosition, long length) throws IOException {
        requireOpen();
        return new SeekableSourceView(this, this::readAt, startingPosition, length);
    }

}
