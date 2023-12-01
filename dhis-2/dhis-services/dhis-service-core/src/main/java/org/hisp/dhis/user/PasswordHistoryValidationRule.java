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

import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_ALREADY_USED_BEFORE;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Created by zubair on 08.03.17. */
public class PasswordHistoryValidationRule implements PasswordValidationRule {
  private static final int HISTORY_LIMIT = 24;

  private final PasswordEncoder passwordEncoder;

  private final UserService userService;

  private final CurrentUserService currentUserService;

  public PasswordHistoryValidationRule(
      PasswordEncoder passwordEncoder,
      UserService userService,
      CurrentUserService currentUserService) {
    this.passwordEncoder = passwordEncoder;
    this.userService = userService;
    this.currentUserService = currentUserService;
  }

  @Override
  public PasswordValidationResult validate(CredentialsInfo credentials) {
    if (!isRuleApplicable(credentials)) {
      return PasswordValidationResult.VALID;
    }

    User user = userService.getUserByUsername(credentials.getUsername());

    List<String> previousPasswords = user.getPreviousPasswords();
    for (String encodedPassword : previousPasswords) {
      if (passwordEncoder.matches(credentials.getPassword(), encodedPassword)) {
        return new PasswordValidationResult(PASSWORD_ALREADY_USED_BEFORE, HISTORY_LIMIT);
      }
    }

    // remove one item from password history if size exceeds HISTORY_LIMIT
    if (previousPasswords.size() == HISTORY_LIMIT) {
      previousPasswords.remove(0);
      userService.updateUser(user);
    }

    return PasswordValidationResult.VALID;
  }

  private boolean isRuleApplicable(CredentialsInfo credentials) {
    User user = userService.getUserByUsername(credentials.getUsername());

    if (!userService.userNonExpired(user)) {
      return true;
    }

    // no need to check password history in case of new user
    return !credentials.isNewUser()
        && currentUserService.getCurrentUsername().equals(credentials.getUsername());
  }
}
