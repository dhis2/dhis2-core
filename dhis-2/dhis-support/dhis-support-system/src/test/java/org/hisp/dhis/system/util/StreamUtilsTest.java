/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.system.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.hisp.dhis.commons.util.StreamUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author bobj
 */
class StreamUtilsTest {

  private BufferedInputStream zipStream;

  private BufferedInputStream gzipStream;

  private BufferedInputStream plainStream;

  @BeforeEach
  void setUp() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    zipStream = new BufferedInputStream(classLoader.getResourceAsStream("dxfA.zip"));
    gzipStream = new BufferedInputStream(classLoader.getResourceAsStream("Export.xml.gz"));
    plainStream = new BufferedInputStream(classLoader.getResourceAsStream("Export.xml"));
  }

  @AfterEach
  void tearDown() throws Exception {
    zipStream.close();
    gzipStream.close();
    plainStream.close();
  }

  @Test
  void testIsZip() {
    assertTrue(StreamUtils.isZip(zipStream));
    assertFalse(StreamUtils.isGZip(zipStream));
    assertFalse(StreamUtils.isZip(plainStream));
  }

  @Test
  void testIsGZip() {
    assertTrue(StreamUtils.isGZip(gzipStream));
    assertFalse(StreamUtils.isZip(gzipStream));
    assertFalse(StreamUtils.isGZip(plainStream));
  }

  @Test
  void testWrapAndCheckZip() throws Exception {
    Reader reader = new InputStreamReader(StreamUtils.wrapAndCheckCompressionFormat(zipStream));
    assertEquals('<', reader.read());
    assertEquals('?', reader.read());
    assertEquals('x', reader.read());
    assertEquals('m', reader.read());
    assertEquals('l', reader.read());
  }
}
