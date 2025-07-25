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
package org.hisp.dhis.user;

import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.user.PasswordValidationService")
public class DefaultPasswordValidationService implements PasswordValidationService {
  private final PasswordValidationRule rule;

  @Autowired
  public DefaultPasswordValidationService(
      PasswordEncoder passwordEncoder,
      UserService userService,
      SystemSettingsProvider settingsProvider) {
    this(
        new PasswordMandatoryValidationRule()
            .then(new PasswordLengthValidationRule(settingsProvider))
            .then(new DigitPatternValidationRule())
            .then(new UpperCasePatternValidationRule())
            .then(new LowerCasePatternValidationRule())
            .then(new SpecialCharacterValidationRule())
            .then(new PasswordDictionaryValidationRule())
            .then(new UserParameterValidationRule())
            .then(new PasswordHistoryValidationRule(passwordEncoder, userService)));
  }

  public DefaultPasswordValidationService(PasswordValidationRule rule) {
    this.rule = rule;
  }

  @Override
  public PasswordValidationResult validate(CredentialsInfo credentials) {
    return rule.validate(credentials);
  }
}
