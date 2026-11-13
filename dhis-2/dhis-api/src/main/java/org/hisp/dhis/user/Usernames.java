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
package org.hisp.dhis.user;

import javax.annotation.CheckForNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Normalization of usernames as typed by a person, applied where a username enters an
 * authentication or account recovery path.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Usernames {

  /**
   * Removes whitespace at the start and end of a username, as typed or pasted by a person. Inner
   * characters are never changed.
   *
   * <p>Whitespace is any character for which {@link Character#isWhitespace(char)} or {@link
   * Character#isSpaceChar(char)} is true. This covers spaces, tabs and line breaks as well as the
   * non-breaking spaces that {@link String#strip()} leaves in place, which often come along when a
   * username is copied from an email or a document. Valid usernames consist of ASCII letters,
   * digits and {@code -_.@} only, so removing these characters never changes a valid username.
   *
   * @param username the username as received, may be null
   * @return the username without leading and trailing whitespace, or null if the input is null
   */
  @CheckForNull
  public static String stripEdgeWhitespace(@CheckForNull String username) {
    if (username == null) {
      return null;
    }
    int start = 0;
    int end = username.length();
    while (start < end && isWhitespace(username.charAt(start))) {
      start++;
    }
    while (end > start && isWhitespace(username.charAt(end - 1))) {
      end--;
    }
    return username.substring(start, end);
  }

  private static boolean isWhitespace(char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }
}
