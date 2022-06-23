package org.sejda.io;
/*
 * Copyright 2022 Sober Lemur S.a.s. di Vacondio Andrea and Sejda BV
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

import org.sejda.commons.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

/**
 * A decorator for a {@link SeekableSource} that adds the concept of offset.
 *
 * @author Andrea Vacondio
 */
class OffsettableSeekableSourceImpl implements OffsettableSeekableSource {

    private SeekableSource wrapped;
    private long offset = 0;

    public OffsettableSeekableSourceImpl(SeekableSource wrapped) {
        requireNotNullArg(wrapped, "Input decorated SeekableSource cannot be null");
        this.wrapped = wrapped;
    }

    @Override
    public String id() {
        return this.wrapped.id();
    }

    @Override
    public boolean isOpen() {
        return this.wrapped.isOpen();
    }

    @Override
    public void requireOpen() throws IOException {
        this.wrapped.requireOpen();
    }

    @Override
    public long size() {
        return wrapped.size() - offset;
    }

    @Override
    public void offset(long offset) throws IOException{
        requireArg(offset >= 0, "Cannot set a negative offset");
        requireArg((wrapped.size() - offset) >= 0, "Invalid offset bigger then the wrapped source size");
        this.offset = offset;
        this.wrapped.position(this.wrapped.position() + offset);
    }

    @Override
    public long position() throws IOException {
        return this.wrapped.position() - offset;
    }

    @Override
    public SeekableSource position(long position) throws IOException {
        return this.wrapped.position(position + offset);
    }

    @Override
    public int read() throws IOException {
        return this.wrapped.read();
    }

    @Override
    public SeekableSource view(long startingPosition, long length) throws IOException {
        return this.wrapped.view(startingPosition + offset, length);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return this.wrapped.read(dst);
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(wrapped);
    }

}
