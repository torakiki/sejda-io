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

import static org.sejda.commons.util.RequireUtils.requireNotBlank;
import static org.sejda.commons.util.RequireUtils.requireState;

import java.io.IOException;

/**
 * Abstract {@link SeekableSource} that provides base functionalities common to all the {@link SeekableSource}s.
 * 
 * @author Andrea Vacondio
 */
public abstract class BaseSeekableSource implements SeekableSource {

    private boolean open = true;
    private String id;

    public BaseSeekableSource(String id) {
        requireNotBlank(id, "SeekableSource id cannot be blank");
        this.id = id;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        this.open = false;
    }

    /**
     * @throws IllegalStateException
     *             if the source is closed
     * @throws IOException
     */
    @Override
    public void requireOpen() throws IOException {
        requireState(isOpen(), "The SeekableSource has been closed");
    }

    /**
     * @return the unique id for this source
     */
    @Override
    public String id() {
        return id;
    }
}
