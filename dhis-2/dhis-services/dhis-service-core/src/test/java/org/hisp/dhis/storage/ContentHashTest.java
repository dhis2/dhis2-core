/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ContentHashTest {

  private static final String VALID_MD5 = "d41d8cd98f00b204e9800998ecf8427e";

  @Test
  void validMd5HexIsAccepted() {
    ContentHash hash = new ContentHash(VALID_MD5);
    assertEquals(VALID_MD5, hash.hex());
  }

  @Test
  void uppercaseHexIsAccepted() {
    String upper = VALID_MD5.toUpperCase();
    assertEquals(upper, new ContentHash(upper).hex());
  }

  @Test
  void tooShortHexIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new ContentHash("d41d8cd98f00b204"));
  }

  @Test
  void nonHexCharactersAreRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> new ContentHash("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
  }

  @Test
  void ofNullableWithNullReturnsNull() {
    assertNull(ContentHash.ofNullable(null));
  }

  @Test
  void ofNullableWithBlankReturnsNull() {
    assertNull(ContentHash.ofNullable("   "));
  }

  @Test
  void ofNullableWithValidHexReturnsHash() {
    ContentHash hash = ContentHash.ofNullable(VALID_MD5);
    assertEquals(VALID_MD5, hash.hex());
  }

  @Test
  void toStringReturnsHex() {
    assertEquals(VALID_MD5, new ContentHash(VALID_MD5).toString());
  }
}
