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

import java.io.IOException;

/**
 * A {@link SeekableSource} that can be offsetted by a given number of bytes
 *
 * @author Andrea Vacondio
 */
public interface OffsettableSeekableSource extends SeekableSource {

    /**
     * Sets the offset for this source
     *
     * @param offset
     * @throws IllegalArgumentException if the offset is negative or if it would lead to a negative size
     */
    void offset(long offset) throws IOException;
}
