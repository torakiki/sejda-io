/*
 * Copyright 2018 Sober Lemur S.r.l.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrea Vacondio
 *
 */
public class BufferedCountingChannelWriterTest {

    private ByteArrayOutputStream out;
    private CountingWritableByteChannel channel;
    private BufferedCountingChannelWriter victim;

    @BeforeEach
    public void setUp() {
        out = new ByteArrayOutputStream();
        channel = CountingWritableByteChannel.from(out);
        victim = new BufferedCountingChannelWriter(channel);
    }

    @AfterEach
    public void after() {
        System.getProperties().remove(BufferedCountingChannelWriter.OUTPUT_BUFFER_SIZE_PROPERTY);
    }

    @Test
    public void nullConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new BufferedCountingChannelWriter(null),
                "Cannot write to a null channel");
    }

    @Test
    public void close() throws IOException {
        victim.close();
        assertFalse(channel.isOpen());
    }

    @Test
    public void flush() throws IOException {
        channel = mock(CountingWritableByteChannel.class);
        victim = new BufferedCountingChannelWriter(channel);
        victim.writeEOL();
        verify(channel, times(0)).write(any());
        victim.flush();
        verify(channel).write(any());
    }

    @Test
    public void closeFlushes() throws IOException {
        channel = mock(CountingWritableByteChannel.class);
        when(channel.isOpen()).thenReturn(true);
        victim = new BufferedCountingChannelWriter(channel);
        victim.writeEOL();
        verify(channel, times(0)).write(any());
        victim.close();
        verify(channel).write(any());
    }

    @Test
    public void writeEOL() throws IOException {
        victim.writeEOL();
        victim.close();
        assertArrayEquals(new byte[] { '\n' }, out.toByteArray());
    }

    @Test
    public void prettyPrintJustOneEOL() throws IOException {
        victim.writeEOL();
        victim.writeEOL();
        victim.write((byte) -1);
        victim.writeEOL();
        victim.writeEOL();
        victim.close();
        assertArrayEquals(new byte[] { '\n', -1, '\n' }, out.toByteArray());
    }

    @Test
    public void multipleCloseDontThrowException() throws IOException {
        victim.close();
        victim.writeEOL();
        victim.close();
    }

    @Test
    public void writeString() throws IOException {
        victim.write("ChuckNorris");
        victim.close();
        assertArrayEquals("ChuckNorris".getBytes(StandardCharsets.ISO_8859_1), out.toByteArray());
    }

    @Test
    public void writeBytesExceedingBuffer() throws IOException {
        System.getProperties().setProperty(BufferedCountingChannelWriter.OUTPUT_BUFFER_SIZE_PROPERTY, "4");
        victim = new BufferedCountingChannelWriter(channel);
        byte[] bytes = new byte[] { '1', '1', '2', '1', '1' };
        victim.write(bytes);
        assertEquals(5, victim.offset());
        victim.close();
        assertArrayEquals(bytes, out.toByteArray());
    }

    @Test
    public void writeBytesOverMultipleBuffers() throws IOException {
        System.getProperties().setProperty(BufferedCountingChannelWriter.OUTPUT_BUFFER_SIZE_PROPERTY, "4");
        victim = new BufferedCountingChannelWriter(channel);
        victim.write((byte) '0');
        byte[] bytes = "ChuckNorris".getBytes(StandardCharsets.ISO_8859_1);
        victim.write(bytes);
        assertEquals(12, victim.offset());
        victim.close();
        assertArrayEquals("0ChuckNorris".getBytes(StandardCharsets.ISO_8859_1), out.toByteArray());
    }

    @Test
    public void writeEOLAfterBytes() throws IOException {
        victim.writeEOL();
        victim.write("Chuck");
        victim.writeEOL();
        victim.close();
        assertArrayEquals("\nChuck\n".getBytes(StandardCharsets.ISO_8859_1), out.toByteArray());
    }

    @Test
    public void writeInputStreamOverMultipleBuffers() throws IOException {
        System.getProperties().setProperty(BufferedCountingChannelWriter.OUTPUT_BUFFER_SIZE_PROPERTY, "4");
        victim = new BufferedCountingChannelWriter(channel);
        victim.write((byte) '0');
        victim.write(new ByteArrayInputStream("ChuckNorris".getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals(12, victim.offset());
        victim.close();
        assertArrayEquals("0ChuckNorris".getBytes(StandardCharsets.ISO_8859_1), out.toByteArray());
    }

    @Test
    public void writeInputStreamDoesntFlushBeforeBufferIsFull() throws IOException {
        channel = mock(CountingWritableByteChannel.class);
        victim = new BufferedCountingChannelWriter(channel);
        victim.write("Chuck");
        victim.write(new ByteArrayInputStream("Norris".getBytes(StandardCharsets.ISO_8859_1)));
        verify(channel, times(0)).write(any());
    }

    @Test
    public void writeInputStreamDoesntCloseTheStream() throws IOException {
        InputStream stream = mock(InputStream.class);
        when(stream.read(any(), anyInt(), anyInt())).thenReturn(-1);
        victim.write(stream);
        verify(stream, never()).close();
    }

    @Test
    public void writeInputStream() throws IOException {
        byte[] bytes = new byte[] { '1', '1', '2', '1', '1' };
        victim.write(bytes);
        byte[] streamBytes = "ChuckNorris".getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayInputStream is = new ByteArrayInputStream(streamBytes);
        victim.write(is);
        victim.close();
        byte[] expected = Arrays.copyOf(bytes, bytes.length + streamBytes.length);
        System.arraycopy(streamBytes, 0, expected, bytes.length, streamBytes.length);
        assertArrayEquals(expected, out.toByteArray());
    }
}
