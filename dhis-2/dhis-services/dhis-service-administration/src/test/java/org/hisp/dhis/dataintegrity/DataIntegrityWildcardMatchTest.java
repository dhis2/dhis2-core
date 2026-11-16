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
package org.hisp.dhis.dataintegrity;

import static org.hisp.dhis.dataintegrity.DefaultDataIntegrityService.matchesWildcard;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DataIntegrityWildcardMatchTest {

  private static final String NAME = "program_rules_no_action";

  @Test
  void testMatches() {
    for (String pattern :
        List.of(
            "program*", "*action", "*rules*", NAME, "*", "p*r*o*n", NAME + "*", "**program**")) {
      assertTrue(matchesWildcard(NAME, pattern), pattern);
    }
  }

  @Test
  void testDoesNotMatch() {
    for (String pattern :
        List.of("programs*", "*actions", "rules*", "program_rules", NAME + "_x", "x*")) {
      assertFalse(matchesWildcard(NAME, pattern), pattern);
    }
  }

  @Test
  void testEmpty() {
    assertTrue(matchesWildcard("", ""));
    assertTrue(matchesWildcard("", "*"));
    assertFalse(matchesWildcard("", "a"));
    assertFalse(matchesWildcard("a", ""));
  }

  /** The matcher replaced a regex where {@code *} became {@code .*}; results must be the same. */
  @Test
  void testSameResultAsRegex() {
    Random random = new Random(42);
    String alphabet = "ab_*";
    for (int i = 0; i < 5000; i++) {
      String text = randomString(random, "ab_", 8);
      String pattern = randomString(random, alphabet, 6);
      assertEquals(
          text.matches(pattern.replace("*", ".*")),
          matchesWildcard(text, pattern),
          () -> "text '" + text + "' pattern '" + pattern + "'");
    }
  }

  private static String randomString(Random random, String alphabet, int maxLength) {
    StringBuilder sb = new StringBuilder();
    int length = random.nextInt(maxLength + 1);
    for (int i = 0; i < length; i++) {
      sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return sb.toString();
  }
}
