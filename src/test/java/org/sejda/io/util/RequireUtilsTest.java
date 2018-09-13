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

import static org.sejda.io.util.RequireUtils.requireArg;
import static org.sejda.io.util.RequireUtils.requireIOCondition;
import static org.sejda.io.util.RequireUtils.requireNotBlank;
import static org.sejda.io.util.RequireUtils.requireNotNullArg;
import static org.sejda.io.util.RequireUtils.requireState;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class RequireUtilsTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullArg() {
        requireNotNullArg(null, "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void falseConditionArg() {
        requireArg(false, "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgNotBlank() {
        requireNotBlank(null, "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyArgNotBlank() {
        requireNotBlank("", "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankArgNotBlank() {
        requireNotBlank(" ", "message");
    }

    @Test(expected = IOException.class)
    public void faseConditionIO() throws IOException {
        requireIOCondition(false, "message");
    }

    @Test(expected = IllegalStateException.class)
    public void falseState() {
        requireState(false, "message");
    }

    @Test
    public void positiveArg() throws IOException {
        requireArg(true, "message");
        requireNotNullArg(new Object(), "message");
        requireNotBlank("ChuckNorris", "message");
        requireIOCondition(true, "message");
        requireState(true, "message");
    }
}
