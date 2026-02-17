/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdSchemeTest {

  @Test
  void testIsValidWithIdentifiableProperty() {
    assertTrue(IdScheme.isValid("UID"));
    assertTrue(IdScheme.isValid("CODE"));
    assertTrue(IdScheme.isValid("NAME"));
    assertTrue(IdScheme.isValid("ID"));
    assertTrue(IdScheme.isValid("UUID"));
  }

  @Test
  void testIsValidCaseInsensitive() {
    assertTrue(IdScheme.isValid("uid"));
    assertTrue(IdScheme.isValid("code"));
    assertTrue(IdScheme.isValid("Code"));
  }

  @Test
  void testIsValidWithAttribute() {
    assertTrue(IdScheme.isValid("ATTRIBUTE:DnrLSdo4hMl"));
  }

  @Test
  void testIsValidWithInvalidScheme() {
    assertFalse(IdScheme.isValid("FOO"));
    assertFalse(IdScheme.isValid("INVALID"));
    assertFalse(IdScheme.isValid(""));
  }

  @Test
  void testIsValidWithInvalidAttributeUid() {
    assertFalse(IdScheme.isValid("ATTRIBUTE:notavaliduid"));
    assertFalse(IdScheme.isValid("ATTRIBUTE:12345678901"));
  }

  @Test
  void testIsValidWithNull() {
    assertFalse(IdScheme.isValid(null));
  }
}
