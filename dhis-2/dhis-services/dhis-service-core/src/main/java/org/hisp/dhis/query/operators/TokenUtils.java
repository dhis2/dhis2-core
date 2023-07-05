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
package org.hisp.dhis.query.operators;

import static java.util.Arrays.asList;

import java.util.List;
import org.hibernate.criterion.MatchMode;

/**
 * @author Henning Håkonsen
 */
public class TokenUtils {
  private TokenUtils() {
    throw new UnsupportedOperationException("util");
  }

  public static List<String> getTokens(String value) {
    return asList(value.replaceAll("[^\\p{L}0-9]", " ").split("[\\s@&.?$+-]+"));
  }

  public static StringBuilder createRegex(String value) {
    StringBuilder regex = new StringBuilder();

    List<String> tokens = getTokens(value);

    if (tokens.isEmpty()) {
      return regex;
    }

    for (String token : getTokens(value)) {
      regex.append("(?=.*").append(token).append(")");
    }
    return regex;
  }

  public static <T> boolean test(
      T value, String searchTerm, boolean caseSensitive, MatchMode mode) {
    if (value == null) {
      return false;
    }
    String searchString = caseSensitive ? searchTerm : searchTerm.toLowerCase();
    String valueString = caseSensitive ? value.toString() : value.toString().toLowerCase();
    List<String> searchTokens = getTokens(searchString);
    List<String> valueTokens = getTokens(valueString);
    return searchTokens.stream().allMatch(searchToken -> testToken(searchToken, valueTokens, mode));
  }

  private static boolean testToken(String searchToken, List<String> valueTokens, MatchMode mode) {
    switch (mode) {
      case EXACT:
        return valueTokens.stream().anyMatch(token -> token.equals(searchToken));
      case START:
        return valueTokens.stream().anyMatch(token -> token.startsWith(searchToken));
      case END:
        return valueTokens.stream().anyMatch(token -> token.endsWith(searchToken));
      default:
      case ANYWHERE:
        return valueTokens.stream().anyMatch(token -> token.contains(searchToken));
    }
  }
}
