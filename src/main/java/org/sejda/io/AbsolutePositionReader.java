/*
 * Created on 20/10/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.io;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Reads bytes from an absolute position of a source, without changing the source position.
 */
@FunctionalInterface
interface AbsolutePositionReader {

    /**
     * Reads bytes into the given buffer starting at the given position of the source. The buffer limit is not changed.
     *
     * @return the number of bytes read, 0 if the buffer has no space left, or -1 if the position is at or past the end of the source
     */
    int read(ByteBuffer dst, long position) throws IOException;
}
