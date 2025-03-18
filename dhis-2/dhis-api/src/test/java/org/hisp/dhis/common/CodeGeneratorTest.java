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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @author bobj
 */
@Slf4j
class CodeGeneratorTest {

  @Test
  void testGetUid() {
    int numberOfCodes = 500;
    Set<String> codes = new HashSet<>();
    for (int n = 0; n < numberOfCodes; ++n) {
      String code = CodeGenerator.generateUid();
      // Test syntax
      assertTrue(code.substring(0, 1).matches("[a-zA-Z]"));
      assertTrue(code.matches("[0-9a-zA-Z]{11}"));
      // Test uniqueness
      assertTrue(codes.add(code));
    }
  }

  @Test
  void testUidIsValid() {
    assertTrue(CodeGenerator.isValidUid("mq4jAnN6fg3"));
    assertTrue(CodeGenerator.isValidUid("QX4LpiTZmUH"));
    assertTrue(CodeGenerator.isValidUid("rT1hdSWjfDC"));
    assertFalse(CodeGenerator.isValidUid("1T1hdSWjfDC"));
    assertFalse(CodeGenerator.isValidUid("QX4LpiTZmUHg"));
    assertFalse(CodeGenerator.isValidUid("1T1hdS_WjfD"));
    assertFalse(CodeGenerator.isValidUid("11111111111"));
  }

  @Test
  void testGetRandomUrlToken() {
    assertNotNull(CodeGenerator.getRandomSecureToken());
    assertNotNull(CodeGenerator.getRandomSecureToken());
    assertNotNull(CodeGenerator.getRandomSecureToken());
    assertEquals(
        CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE,
        (CodeGenerator.getRandomSecureToken()).length());
    assertEquals(
        CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE,
        (CodeGenerator.getRandomSecureToken()).length());
    assertEquals(
        CodeGenerator.SECURE_RANDOM_TOKEN_MIN_SIZE,
        (CodeGenerator.getRandomSecureToken()).length());
  }

  @Test
  void testGenerateUid_Timestamp() {
    // all numbers are just randomly selected
    // the main point is that they always generate the same UID
    // and that very similar numbers generate very different UIDs
    long time = 33249832492304L;
    assertEquals("D1dL1bY3aA0", CodeGenerator.generateUid(time));
    assertEquals("eG9bO6eHB0a", CodeGenerator.generateUid(time + 1));
    assertEquals("D7dL1bY3aC0", CodeGenerator.generateUid(time + 2));
    assertEquals("eA9bO6eHB2a", CodeGenerator.generateUid(time + 3));
    assertEquals("oE9bO8kLK2y", CodeGenerator.generateUid(time - 3933));
  }
}
