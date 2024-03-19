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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.query.operators.NotTokenOperator;
import org.hisp.dhis.query.operators.TokenOperator;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.query.operators.TokenOperator} and {@link
 * org.hisp.dhis.query.operators.NotTokenOperator} implementation.
 *
 * @author Jan Bernitt
 */
class TokenOperatorTest {

  @Test
  void nullValue() {
    for (MatchMode mode : MatchMode.values()) {
      assertFalse(new TokenOperator<>("any", true, mode).test(null));
      assertFalse(new TokenOperator<>("any", false, mode).test(null));
      assertTrue(new NotTokenOperator<>("any", true, mode).test(null));
      assertTrue(new NotTokenOperator<>("any", false, mode).test(null));
    }
  }

  @Test
  void exactMatchOneWordOneTerm() {
    assertMatch(MatchMode.EXACT, "Hello!", "Hello");
    assertMatch(MatchMode.EXACT, "Hello!", "Hello!");
  }

  @Test
  void exactMatchOneWordManyTerms() {
    assertMatch(MatchMode.EXACT, "Hello!", "Hello Hello");
    assertMatch(MatchMode.EXACT, "Hello!", "Hello! Hello!");
  }

  @Test
  void exactMatchManyWordsOneTerm() {
    assertMatch(MatchMode.EXACT, "Hello World!", "Hello");
    assertMatch(MatchMode.EXACT, "Hello World!", "World");
  }

  @Test
  void exactMatchManyWordsManyTerms() {
    assertMatch(MatchMode.EXACT, "Hello World!", "Hello World");
    assertMatch(MatchMode.EXACT, "Hello World!", "Hello, World!");
    assertMatch(MatchMode.EXACT, "Hello there World!", "Hello, World!");
  }

  @Test
  void startsWithMatchOneWordOneTerm() {
    assertMatch(MatchMode.START, "Hello!", "Hello");
    assertMatch(MatchMode.START, "Hello!", "Hello!");
    assertMatch(MatchMode.START, "Hello!", "Hel");
  }

  @Test
  void startsWithMatchOneWordManyTerms() {
    assertMatch(MatchMode.START, "Hello!", "Hello Hello");
    assertMatch(MatchMode.START, "Hello!", "Hello! Hello!");
    assertMatch(MatchMode.START, "Hello!", "He! Hell");
  }

  @Test
  void startsWithMatchManyWordsOneTerm() {
    assertMatch(MatchMode.START, "Hello World!", "Hello");
    assertMatch(MatchMode.START, "Hello World!", "World");
    assertMatch(MatchMode.START, "Hello World!", "Hel");
    assertMatch(MatchMode.START, "Hello World!", "Wo");
  }

  @Test
  void startsWithMatchManyWordsManyTerms() {
    assertMatch(MatchMode.START, "Hello World!", "Hello World");
    assertMatch(MatchMode.START, "Hello World!", "Hello, World!");
    assertMatch(MatchMode.START, "Hello there World!", "Hello, World!");
    assertMatch(MatchMode.START, "Hello there World!", "Hel, Wor");
  }

  @Test
  void endsWithMatchOneWordOneTerm() {
    assertMatch(MatchMode.END, "Hello!", "Hello");
    assertMatch(MatchMode.END, "Hello!", "Hello!");
    assertMatch(MatchMode.END, "Hello!", "lo!");
  }

  @Test
  void endsWithMatchOneWordManyTerms() {
    assertMatch(MatchMode.END, "Hello!", "Hello Hello");
    assertMatch(MatchMode.END, "Hello!", "Hello! Hello!");
    assertMatch(MatchMode.END, "Hello!", "lo ello");
  }

  @Test
  void endsWithMatchManyWordsOneTerm() {
    assertMatch(MatchMode.END, "Hello World!", "Hello");
    assertMatch(MatchMode.END, "Hello World!", "World");
    assertMatch(MatchMode.END, "Hello World!", "lo");
    assertMatch(MatchMode.END, "Hello World!", "rld");
  }

  @Test
  void endsWithMatchManyWordsManyTerms() {
    assertMatch(MatchMode.END, "Hello World!", "Hello World");
    assertMatch(MatchMode.END, "Hello World!", "Hello, World!");
    assertMatch(MatchMode.END, "Hello there World!", "Hello, World!");
    assertMatch(MatchMode.END, "Hello there World!", "ello orld");
  }

  @Test
  void anywhereMatchOneWordOneTerm() {
    assertMatch(MatchMode.ANYWHERE, "Hello!", "Hello");
    assertMatch(MatchMode.ANYWHERE, "Hello!", "Hello!");
    assertMatch(MatchMode.ANYWHERE, "Hello!", "ell");
  }

  @Test
  void anywhereMatchOneWordManyTerms() {
    assertMatch(MatchMode.ANYWHERE, "Hello!", "Hello Hello");
    assertMatch(MatchMode.ANYWHERE, "Hello!", "Hello! Hello!");
    assertMatch(MatchMode.ANYWHERE, "Hello!", "el ll");
  }

  @Test
  void anywhereMatchManyWordsOneTerm() {
    assertMatch(MatchMode.ANYWHERE, "Hello World!", "Hello");
    assertMatch(MatchMode.ANYWHERE, "Hello World!", "World");
    assertMatch(MatchMode.ANYWHERE, "Hello World!", "ll");
    assertMatch(MatchMode.ANYWHERE, "Hello World!", "or");
  }

  @Test
  void anywhereMatchManyWordsManyTerms() {
    assertMatch(MatchMode.ANYWHERE, "Hello World!", "Hello World");
    assertMatch(MatchMode.ANYWHERE, "Hello World!", "Hello, World!");
    assertMatch(MatchMode.ANYWHERE, "Hello there World!", "Hello, World!");
    assertMatch(MatchMode.ANYWHERE, "Hello there World!", "el er or");
  }

  private static void assertMatch(MatchMode mode, String value, String term) {
    String upperValue = value.toUpperCase();
    String lowerValue = value.toLowerCase();
    TokenOperator<String> caseSensitive = new TokenOperator<>(term, true, mode);
    TokenOperator<String> caseInsensitive = new TokenOperator<>(term, false, mode);
    assertTrue(caseSensitive.test(value));
    assertTrue(caseInsensitive.test(value));
    assertTrue(caseInsensitive.test(upperValue));
    assertTrue(caseInsensitive.test(lowerValue));
    if (!upperValue.equals(lowerValue)) {
      assertFalse(caseSensitive.test(upperValue) && caseSensitive.test(lowerValue));
    }
    // make it mismatch by searching for something not in value
    String zzzTerm = "zzz" + term + "zzz";
    TokenOperator<String> zzzCaseSensitive = new TokenOperator<>(zzzTerm, true, mode);
    TokenOperator<String> zzzCaseInsensitive = new TokenOperator<>(zzzTerm, false, mode);
    assertFalse(zzzCaseSensitive.test(value));
    assertFalse(zzzCaseInsensitive.test(value));
    assertFalse(zzzCaseInsensitive.test(upperValue));
    assertFalse(zzzCaseInsensitive.test(lowerValue));
    NotTokenOperator<String> notCaseSensitive = new NotTokenOperator<>(term, true, mode);
    NotTokenOperator<String> notCaseInsensitive = new NotTokenOperator<>(term, false, mode);
    assertFalse(notCaseSensitive.test(value));
    assertFalse(notCaseInsensitive.test(value));
    assertFalse(notCaseInsensitive.test(upperValue));
    assertFalse(notCaseInsensitive.test(lowerValue));
    if (!upperValue.equals(lowerValue)) {
      assertTrue(notCaseSensitive.test(upperValue) || notCaseSensitive.test(lowerValue));
    }
  }
}
