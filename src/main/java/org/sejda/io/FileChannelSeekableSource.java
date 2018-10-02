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

import org.sejda.commons.util.IOUtils;

/**
 * A {@link SeekableSource} implementation based on {@link FileChannel}.
 * 
 * @author Andrea Vacondio
 */
public class FileChannelSeekableSource extends BaseSeekableSource {
    private FileChannel channel;
    private File file;
    private long size;
    private ThreadBoundCopiesSupplier<FileChannelSeekableSource> localCopiesSupplier = new ThreadBoundCopiesSupplier<>(
            () -> new FileChannelSeekableSource(file));

    public FileChannelSeekableSource(File file) throws IOException {
        super(ofNullable(file).map(File::getAbsolutePath).orElseThrow(() -> {
            return new IllegalArgumentException("Input file cannot be null");
        }));
        this.channel = new RandomAccessFile(file, "r").getChannel();
        this.size = channel.size();
        this.file = file;
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public SeekableSource position(long newPosition) throws IOException {
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        channel.position(newPosition);
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void close() throws IOException {
        super.close();
        IOUtils.close(localCopiesSupplier);
        IOUtils.close(channel);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        requireOpen();
        return channel.read(dst);
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        if (channel.read(buffer) > 0) {
            buffer.flip();
            return buffer.get() & 0xff;
        }
        return -1;
    }

    @Override
    public SeekableSource view(long startingPosition, long length) throws IOException {
        requireOpen();
        return new SeekableSourceView(localCopiesSupplier, id(), startingPosition, length);
    }
}
