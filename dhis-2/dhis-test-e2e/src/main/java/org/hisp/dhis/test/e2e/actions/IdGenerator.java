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
package org.hisp.dhis.test.e2e.actions;

import java.security.SecureRandom;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class IdGenerator extends RestApiActions {
  private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String ALLOWED_CHARS = "0123456789" + LETTERS;
  private static final int CODE_SIZE = 11;
  private static final SecureRandom RANDOM = new SecureRandom();

  public IdGenerator() {
    super("/system");
  }

  /**
   * Generates a DHIS2 UID client-side, matching the server's {@code CodeGenerator} format: 11
   * characters, the first a letter and the remaining alphanumeric. This avoids a server round-trip
   * (previously {@code GET /api/system/id}) per generated id.
   */
  public String generateUniqueId() {
    char[] uid = new char[CODE_SIZE];
    // First char is a letter so the uid is a valid XML/HTML identifier, as required by DHIS2.
    uid[0] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
    for (int i = 1; i < CODE_SIZE; i++) {
      uid[i] = ALLOWED_CHARS.charAt(RANDOM.nextInt(ALLOWED_CHARS.length()));
    }
    return new String(uid);
  }
}
