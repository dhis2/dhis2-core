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
package org.hisp.dhis.user;

/**
 * Possible reasons for passwords to be invalid.
 *
 * @author Jan Bernitt
 */
public enum PasswordValidationError {
  PASSWORD_IS_MANDATORY("mandatory_parameter_missing", "Username or Password is missing"),
  PASSWORD_TOO_LONG_TOO_SHORT(
      "password_length_validation", "Password must have at least %d, and at most %d characters"),
  PASSWORD_MUST_HAVE_DIGIT("password_digit_validation", "Password must have at least one digit"),
  PASSWORD_MUST_HAVE_UPPER(
      "password_uppercase_validation", "Password must have at least one upper case"),
  PASSWORD_MUST_HAVE_SPECIAL(
      "password_specialcharacter_validation", "Password must have at least one special character"),
  PASSWORD_CONTAINS_RESERVED_WORD(
      "password_dictionary_validation", "Password must not have any generic word"),
  PASSWORD_CONTAINS_NAME_OR_EMAIL(
      "password_username_validation", "Username/Email must not be a part of password"),
  PASSWORD_ALREADY_USED_BEFORE(
      "password_history_validation", "Password must not be one of the previous %d passwords");

  private final String message;

  private final String i18nKey;

  PasswordValidationError(String i18nKey, String message) {
    this.message = message;
    this.i18nKey = i18nKey;
  }

  public String getMessage() {
    return message;
  }

  public String getI18nKey() {
    return i18nKey;
  }
}
