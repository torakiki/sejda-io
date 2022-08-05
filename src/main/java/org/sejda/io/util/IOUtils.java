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
package org.sejda.io.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.nonNull;

/**
 * Utility class with I/O related static methods
 */
public final class IOUtils {
    private static final Logger LOG = LoggerFactory.getLogger(IOUtils.class);

    private static final Optional<Consumer<ByteBuffer>> UNMAPPER;

    static {
        UNMAPPER = Optional.ofNullable(
                AccessController.doPrivileged((PrivilegedAction<Consumer<ByteBuffer>>) IOUtils::unmapper));
    }

    private IOUtils() {
        // hide
    }

    /**
     * Unmap memory mapped byte buffers. This is a hack waiting for a proper JVM provided solution expected in java 10 https://bugs.openjdk.java.net/browse/JDK-4724038 The issue
     * here is that even when closed, memory mapped byte buffers hold a lock on the underlying file until GC is executed and this in turns result in an error if the user tries to
     * move or delete the file.
     * 
     * @param buf
     */
    public static void unmap(ByteBuffer buf) {
        try {
            UNMAPPER.ifPresent(u -> u.accept(buf));
        } catch (Exception e) {
            LOG.error("Unable to unmap ByteBuffer.", e);
        }
    }

    /**
     * This is adapted from org.apache.lucene.store.MMapDirectory
     * 
     * @return
     */
    private static Consumer<ByteBuffer> unmapper() {
        final Lookup lookup = lookup();
        try {
            // *** sun.misc.Unsafe unmapping (Java 9+) ***
            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            // first check if Unsafe has the right method, otherwise we can give up
            // without doing any security critical stuff:
            final MethodHandle unmapper = lookup.findVirtual(unsafeClass, "invokeCleaner",
                    methodType(void.class, ByteBuffer.class));
            // fetch the unsafe instance and bind it to the virtual MH:
            final Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            final Object theUnsafe = f.get(null);
            return newBufferCleaner(ByteBuffer.class, unmapper.bindTo(theUnsafe));
        } catch (SecurityException se) {
            LOG.error(
                    "Unmapping is not supported because of missing permissions. Please grant at least the following permissions: RuntimePermission(\"accessClassInPackage.sun.misc\") "
                            + " and ReflectPermission(\"suppressAccessChecks\")",
                    se);

        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.error("Unmapping is not supported.", e);
        }
        return null;
    }

    private static Consumer<ByteBuffer> newBufferCleaner(final Class<?> unmappableBufferClass,
            final MethodHandle unmapper) {
        assert Objects.equals(methodType(void.class, ByteBuffer.class), unmapper.type());
        return (ByteBuffer buffer) -> {
            if (!buffer.isDirect()) {
                throw new IllegalArgumentException("unmapping only works with direct buffers");
            }
            if (!unmappableBufferClass.isInstance(buffer)) {
                throw new IllegalArgumentException("buffer is not an instance of " + unmappableBufferClass.getName());
            }
            final Throwable e = AccessController.doPrivileged((PrivilegedAction<Throwable>) () -> {
                try {
                    unmapper.invokeExact(buffer);
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });
            if (nonNull(e)) {
                LOG.error("Unable to unmap the mapped buffer", e);
            }
        };
    }

}
