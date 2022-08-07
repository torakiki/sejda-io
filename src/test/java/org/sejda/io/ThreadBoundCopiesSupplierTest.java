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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Andrea Vacondio
 */
public class ThreadBoundCopiesSupplierTest {

    @Test
    public void closeCopies() throws IOException {
        SeekableSource copy = mock(SeekableSource.class);
        ThreadBoundCopiesSupplier<SeekableSource> victim = new ThreadBoundCopiesSupplier<>(() -> copy);
        assertEquals(copy, victim.get());
        victim.close();
        verify(copy).close();
    }

    @Test
    public void differentCopyPerThread() throws InterruptedException, ExecutionException {
        Supplier<ByteArraySeekableSource> supplier = () -> new ByteArraySeekableSource(new byte[0]);
        ThreadBoundCopiesSupplier<ByteArraySeekableSource> victim = new ThreadBoundCopiesSupplier<>(supplier);
        ByteArraySeekableSource first = victim.get();
        assertNotNull(first);
        ByteArraySeekableSource second = CompletableFuture.supplyAsync(() -> {
            try {
                return victim.get();
            } catch (Exception e) {
                return null;
            }
        }).get();
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    @Test
    public void sameThreadSameCopy() throws InterruptedException, ExecutionException {
        Supplier<ByteArraySeekableSource> supplier = () -> new ByteArraySeekableSource(new byte[0]);
        ThreadBoundCopiesSupplier<ByteArraySeekableSource> victim = new ThreadBoundCopiesSupplier<>(supplier);
        assertEquals(victim.get(), victim.get());
        Executor executor = Executors.newSingleThreadExecutor();
        Supplier<ByteArraySeekableSource> completableSupplier = () -> {
            try {
                return victim.get();
            } catch (Exception e) {
                return null;
            }
        };
        ByteArraySeekableSource first = CompletableFuture.supplyAsync(completableSupplier, executor).get();
        ByteArraySeekableSource second = CompletableFuture.supplyAsync(completableSupplier, executor).get();
        assertEquals(first, second);
    }

    @Test
    @DisplayName("Each thread gets a different copy")
    public void eachThreadGetsACopy() {
        Supplier<ByteArraySeekableSource> supplier = () -> new ByteArraySeekableSource(new byte[0]);
        var victim = new ThreadBoundCopiesSupplier<>(supplier);
        int numberOfThreads = 1000;
        ConcurrentHashMap<ByteArraySeekableSource, Integer> results = new ConcurrentHashMap<>(numberOfThreads);

        var service = Executors.newFixedThreadPool(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> results.compute(victim.get(), (k, v) -> nonNull(v) ? 1 : 0));
        }
        service.shutdown();
        assertEquals(numberOfThreads, results.size());
        assertEquals(0, results.values().stream().filter(i -> i > 0).count());
    }
}
