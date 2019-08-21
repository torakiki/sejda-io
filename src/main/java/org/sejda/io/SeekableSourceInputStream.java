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

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.commons.util.RequireUtils.requireState;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Bridge between {@link SeekableSource} and {@link InputStream}
 * 
 * @author Andrea Vacondio
 */
class SeekableSourceInputStream extends InputStream {
    private SeekableSource wrapped;

    SeekableSourceInputStream(SeekableSource wrapped) {
        requireNotNullArg(wrapped, "Cannot decorate a null instance");
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        return getSource().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (available() > 0) {
            return getSource().read(ByteBuffer.wrap(b, 0, Math.min(b.length, available())));
        }
        return -1;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (available() > 0) {
            return getSource().read(ByteBuffer.wrap(b, Math.min(b.length, offset),
                    Math.min(length, Math.min(b.length - offset, available()))));
        }
        return -1;
    }

    @Override
    public int available() throws IOException {
        SeekableSource source = getSource();
        return (int) Math.max(0, (source.size() - source.position()));
    }

    @Override
    public long skip(long offset) throws IOException {
        SeekableSource source = getSource();
        long start = source.position();
        return source.forward(Math.min(offset, available())).position() - start;
    }

    private SeekableSource getSource() {
        requireState(wrapped.isOpen(), "The SeekableSource has been closed");
        return wrapped;
    }
}
