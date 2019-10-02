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

import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.commons.util.RequireUtils.requireState;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link SeekableSource} representing a view over a portion of a parent {@link SeekableSource}. A view becomes invalid if the parent {@link SeekableSource} is closed. The view
 * works on a thread local copy of the parent source so the parent position is not modified when a read method is called on the view.
 * 
 * @author Andrea Vacondio
 */
class SeekableSourceView extends BaseSeekableSource {
    private long startingPosition;
    private long length;
    private long currentPosition;
    private SeekableSourceSupplier<? extends SeekableSource> supplier;

    public SeekableSourceView(SeekableSourceSupplier<? extends SeekableSource> supplier, String id,
            long startingPosition, long length) throws IOException {
        super(id);
        requireArg(startingPosition >= 0, "Starting position cannot be negative");
        requireArg(length > 0, "View length must be positive");
        requireNotNullArg(supplier, "Input decorated SeekableSource cannot be null");
        this.startingPosition = startingPosition;
        this.currentPosition = 0;
        SeekableSource wrapped = supplier.get();
        requireArg(startingPosition < wrapped.size(), "Starting position cannot be higher then wrapped source size");
        this.length = Math.min(length, wrapped.size() - startingPosition);
        this.supplier = supplier;
    }

    @Override
    public long position() {
        return currentPosition;
    }

    @Override
    public SeekableSource position(long newPosition) throws IOException {
        SeekableSource wrapped = supplier.get();
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        this.currentPosition = Math.min(length, newPosition);
        wrapped.position(startingPosition + currentPosition);
        return this;
    }

    @Override
    public long size() {
        return length;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        SeekableSource wrapped = supplier.get();
        requireOpen();
        if (hasAvailable()) {
            wrapped.position(startingPosition + currentPosition);
            if (dst.remaining() > available()) {
                dst.limit(dst.position() + (int) available());
            }
            int read = wrapped.read(dst);
            if (read > 0) {
                currentPosition += read;
                return read;
            }
        }
        return -1;
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        SeekableSource wrapped = supplier.get();
        if (hasAvailable()) {
            wrapped.position(startingPosition + currentPosition);
            currentPosition++;
            return wrapped.read();
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
    public void requireOpen() throws IOException {
        super.requireOpen();
        SeekableSource wrapped = supplier.get();
        requireState(wrapped.isOpen(), "The original SeekableSource has been closed");
    }

    /**
     * Cannot create a view of a view. This method throws {@link RuntimeException}.
     * 
     * @throws RuntimeException
     */
    @Override
    public SeekableSource view(long startingPosition, long length) {
        throw new RuntimeException("Cannot create a view of a view");
    }

}
