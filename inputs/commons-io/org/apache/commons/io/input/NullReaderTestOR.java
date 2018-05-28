/*
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
package org.apache.commons.io.input;

import org.junit.Test;

import java.io.EOFException;
import java.io.Reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * JUnit Test Case for {@link NullReader}.
 *
 */
public class NullReaderTestOR {

    @Test
    public void testEOFException() throws Exception {
        final Reader reader = new NullReaderTestOR.TestNullReader(2, false, true);
        assertEquals("Read 1",  0, reader.read());
        assertEquals("Read 2",  1, reader.read());
        try {
            final int result = reader.read();
            fail("Should have thrown an EOFException, value=[" + result + "]");
        } catch (final EOFException e) {
            // expected
        }
        reader.close();
    }

    // ------------- Test NullReader implementation -------------

    private static final class TestNullReader extends NullReader {
        public TestNullReader(final int size) {
            super(size);
        }
        public TestNullReader(final int size, final boolean markSupported, final boolean throwEofException) {
            super(size, markSupported, throwEofException);
        }
        @Override
        protected int processChar() {
            return (int)getPosition() - 1;
        }
        @Override
        protected void processChars(final char[] chars, final int offset, final int length) {
            final int startPos = (int)getPosition() - length;
            for (int i = offset; i < length; i++) {
                chars[i] = (char)(startPos + i);
            }
        }

    }
}
