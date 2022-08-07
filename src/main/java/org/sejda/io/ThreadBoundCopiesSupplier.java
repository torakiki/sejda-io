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

import org.sejda.commons.util.IOUtils;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Component supplying per-thread copies of a {@link SeekableSource} using the provided supplier. When closed, all the generated copies are closed as well.
 *
 * @author Andrea Vacondio
 */
public class ThreadBoundCopiesSupplier<T extends SeekableSource> implements Closeable, Supplier<T> {

    private final ConcurrentMap<Long, T> copies = new ConcurrentHashMap<>();

    private final Supplier<T> supplier;

    public ThreadBoundCopiesSupplier(Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier);
    }

    @Override
    public T get() {
        return copies.computeIfAbsent(Thread.currentThread().getId(), k -> supplier.get());

    }

    @Override
    public void close() {
        copies.values().forEach(IOUtils::closeQuietly);
    }
}
