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

/** Created by zubair on 08.03.17. */
@FunctionalInterface
public interface PasswordValidationRule {

  /**
   * Validates user password to make sure it comply with requirements related to password strength.
   *
   * <p>Not all rules are applicable all the time. If a rule does not apply it returns {@link
   * PasswordValidationResult#VALID}.
   *
   * @param credentialsInfo info to check
   * @return {@link PasswordValidationResult}
   */
  PasswordValidationResult validate(CredentialsInfo credentialsInfo);

  /**
   * Utility method to chain multiple {@link PasswordValidationRule}s to a complex rule with a
   * defined sequence in which rules are checked.
   *
   * @param next Rule to check in case this is valid
   * @return result of this check if invalid, otherwise result of next check
   */
  default PasswordValidationRule then(PasswordValidationRule next) {
    return credentialsInfo -> {
      PasswordValidationResult result = validate(credentialsInfo);
      return !result.isValid() ? result : next.validate(credentialsInfo);
    };
  }
}
