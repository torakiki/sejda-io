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

import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.commons.util.RequireUtils.requireState;

/**
 * {@link SeekableSource} representing a view over a portion of a parent {@link SeekableSource}. A view is closed when the parent {@link SeekableSource} is closed.
 * Reading the view doesn't modify the parent position.
 *
 * @author Andrea Vacondio
 */
class SeekableSourceView extends BaseSeekableSource {
    private final SeekableSource parent;
    private final AbsolutePositionReader reader;
    private final long startingPosition;
    private final long length;
    private long currentPosition;

    /**
     * Creates a view over the given portion of the parent, read through the given reader.
     */
    SeekableSourceView(SeekableSource parent, AbsolutePositionReader reader, long startingPosition, long length) {
        requireNotNullArg(parent, "Parent SeekableSource cannot be null");
        requireNotNullArg(reader, "Absolute position reader cannot be null");
        requireArg(startingPosition >= 0, "Starting position cannot be negative");
        requireArg(length > 0, "View length must be positive");
        requireArg(startingPosition < parent.size(), "Starting position cannot be higher than the parent source size");
        this.parent = parent;
        this.reader = reader;
        this.startingPosition = startingPosition;
        this.length = Math.min(length, parent.size() - startingPosition);
    }

    @Override
    public String id() {
        return parent.id() + "[" + startingPosition + "," + length + "]";
    }

    @Override
    public long position() {
        return currentPosition;
    }

    @Override
    public SeekableSource position(long newPosition) {
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        this.currentPosition = Math.min(length, newPosition);
        return this;
    }

    @Override
    public long size() {
        return length;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        requireOpen();
        if (!dst.hasRemaining()) {
            return 0;
        }
        if (hasAvailable()) {
            var window = dst.slice(dst.position(), (int) Math.min(dst.remaining(), available()));
            int read = reader.read(window, startingPosition + currentPosition);
            if (read > 0) {
                dst.position(dst.position() + read);
                currentPosition += read;
            }
            return read;
        }
        return -1;
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        if (hasAvailable()) {
            var buffer = ByteBuffer.allocate(1);
            if (reader.read(buffer, startingPosition + currentPosition) > 0) {
                currentPosition++;
                return buffer.get(0) & 0xff;
            }
        }
        return -1;
    }

    private boolean hasAvailable() {
        return available() > 0;
    }

    private long available() {
        return length - currentPosition;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.currentPosition = 0;
    }

    @Override
    public boolean isOpen() {
        return super.isOpen() && parent.isOpen();
    }

    @Override
    public void requireOpen() {
        requireState(parent.isOpen(), "The parent SeekableSource has been closed");
        super.requireOpen();
    }

    /**
     * Cannot create a view of a view. This method throws {@link RuntimeException}.
     */
    @Override
    public SeekableSource view(long startingPosition, long length) {
        throw new RuntimeException("Cannot create a view of a view");
    }

}
