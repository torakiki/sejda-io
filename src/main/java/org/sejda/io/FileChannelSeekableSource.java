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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireArg;

/**
 * A {@link SeekableSource} implementation based on {@link FileChannel}.
 *
 * @author Andrea Vacondio
 */
public class FileChannelSeekableSource extends BaseSeekableSource {
    private final FileChannel channel;
    private Path path;
    private final long size;
    private final ThreadBoundCopiesSupplier<FileChannelSeekableSource> localCopiesSupplier = new ThreadBoundCopiesSupplier<>(
            () -> new FileChannelSeekableSource(path));

    public FileChannelSeekableSource(Path path) {
        super(ofNullable(path).map(Path::toAbsolutePath).map(Path::toString)
                .orElseThrow(() -> new IllegalArgumentException("Input path cannot be null")));
        try {
            this.channel = FileChannel.open(path, StandardOpenOption.READ);
            this.size = channel.size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.path = path;
    }

    public FileChannelSeekableSource(File file) {
        this(ofNullable(file).map(File::toPath)
                .orElseThrow(() -> new IllegalArgumentException("Input file cannot be null")));
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
