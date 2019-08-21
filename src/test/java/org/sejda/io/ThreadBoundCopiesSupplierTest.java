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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * @author Andrea Vacondio
 *
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
    public void differentCopyPerThread() throws IOException, InterruptedException, ExecutionException {
        SeekableSourceSupplier<ByteArraySeekableSource> supplier = () -> new ByteArraySeekableSource(new byte[0]);
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
    public void sameThreadSameCopy() throws IOException, InterruptedException, ExecutionException {
        SeekableSourceSupplier<ByteArraySeekableSource> supplier = () -> new ByteArraySeekableSource(new byte[0]);
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
}
