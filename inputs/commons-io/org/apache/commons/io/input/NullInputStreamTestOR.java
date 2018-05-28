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
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * JUnit Test Case for {@link NullInputStream}.
 *
 */
public class NullInputStreamTestOR {

    @Test
    public void testEOFException() throws Exception {
        final InputStream input = new NullInputStreamTestOR.TestNullInputStream(2, false, true);
        assertEquals("Read 1",  0, input.read());
        assertEquals("Read 2",  1, input.read());
        try {
            final int result = input.read();
            fail("Should have thrown an EOFException, byte=[" + result + "]");
        } catch (final EOFException e) {
            // expected
        }
        input.close();
    }

    // ------------- Test NullInputStream implementation -------------

    private static final class TestNullInputStream extends NullInputStream {
        public TestNullInputStream(final int size) {
            super(size);
        }
        public TestNullInputStream(final int size, final boolean markSupported, final boolean throwEofException) {
            super(size, markSupported, throwEofException);
        }
        @Override
        protected int processByte() {
            return (int)getPosition() - 1;
        }
        @Override
        protected void processBytes(final byte[] bytes, final int offset, final int length) {
            final int startPos = (int)getPosition() - length;
            for (int i = offset; i < length; i++) {
                bytes[i] = (byte)(startPos + i);
            }
        }

    }
}
