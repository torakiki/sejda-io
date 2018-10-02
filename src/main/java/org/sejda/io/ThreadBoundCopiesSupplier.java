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

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sejda.commons.util.IOUtils;

/**
 * Component suppling per-thread copies of a {@link SeekableSource} using the provided supplier. When closed, all the generated copies are closed as well.
 * 
 * @author Andrea Vacondio
 */
public class ThreadBoundCopiesSupplier<T extends SeekableSource> implements Closeable, SeekableSourceSupplier<T> {

    private ConcurrentMap<Long, T> copies = new ConcurrentHashMap<>();

    private final SeekableSourceSupplier<T> supplier;

    public ThreadBoundCopiesSupplier(SeekableSourceSupplier<T> supplier) {
        requireNonNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public T get() throws IOException {
        long id = Thread.currentThread().getId();
        T copy = copies.get(id);
        if (isNull(copy)) {
            T newCopy = supplier.get();
            copy = copies.putIfAbsent(id, newCopy);
            if (isNull(copy)) {
                copy = newCopy;
            } else {
                IOUtils.closeQuietly(newCopy);
            }
        }
        return copy;
    }

    @Override
    public void close() {
        copies.values().stream().forEach(IOUtils::closeQuietly);
    }
}
