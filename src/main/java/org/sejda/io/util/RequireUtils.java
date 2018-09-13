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

import java.io.IOException;

/**
 * Utility methods to check parameters and conditions validity.
 * 
 * @author Andrea Vacondio
 *
 */
public final class RequireUtils {

    private RequireUtils() {
        // utility class
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given argument is null
     * 
     * @param arg
     * @param exceptionMessage
     */
    public static void requireNotNullArg(Object arg, String exceptionMessage) {
        if (arg == null) {
            throw new IllegalArgumentException(exceptionMessage);
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given condition is not met
     * 
     * @param condition
     * @param exceptionMessage
     */
    public static void requireArg(boolean condition, String exceptionMessage) {
        if (!condition) {
            throw new IllegalArgumentException(exceptionMessage);
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given string is blank
     * 
     * @param value
     *            string
     * @param exceptionMessage
     */
    public static void requireNotBlank(String value, String exceptionMessage) {
        requireArg(value != null && value.trim().length() > 0, exceptionMessage);
    }

    /**
     * Throws an {@link IOException} if the given condition is not met
     * 
     * @param condition
     * @param exceptionMessage
     * @throws IOException
     */
    public static void requireIOCondition(boolean condition, String exceptionMessage) throws IOException {
        if (!condition) {
            throw new IOException(exceptionMessage);
        }
    }

    /**
     * Throws an {@link IllegalStateException} if the given condition is not met
     * 
     * @param condition
     * @param exceptionMessage
     * @throws IllegalStateException
     */
    public static void requireState(boolean condition, String exceptionMessage) {
        if (!condition) {
            throw new IllegalStateException(exceptionMessage);
        }
    }
}
