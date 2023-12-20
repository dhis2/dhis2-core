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
package org.hisp.dhis.startup;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.user.DefaultUserService.TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME;

import java.util.Set;
import java.util.UUID;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class DefaultAdminUserPopulator extends TransactionContextStartupRoutine {

  public static final Set<String> ALL_RESTRICTIONS =
      Set.of(TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME);

  private final UserService userService;

  public DefaultAdminUserPopulator(UserService userService) {
    checkNotNull(userService);
    this.userService = userService;
  }

  @Override
  public void executeInTransaction() {
    // If there is no users in the system we assume we need a default admin
    // user.
    if (userService.getUserCount() > 0) {
      return;
    }

    // ---------------------------------------------------------------------
    // Assumes no UserRole called "Superuser" in database
    // ---------------------------------------------------------------------

    String username = "admin";
    String password = "district";

    User user = new User();
    user.setUid("M5zQapPyTZI");
    user.setUuid(UUID.fromString("6507f586-f154-4ec1-a25e-d7aa51de5216"));
    user.setUsername(username);
    user.setCode(username);
    user.setFirstName(username);
    user.setSurname(username);

    SystemUser actingUser = new SystemUser();

    userService.addUser(user, actingUser);

    UserRole userRole = new UserRole();
    userRole.setUid("yrB6vc5Ip3r");
    userRole.setCode("Superuser");
    userRole.setName("Superuser");
    userRole.setDescription("Superuser");
    userRole.setAuthorities(Authorities.getAllAuthorities());

    userService.addUserRole(userRole, actingUser);

    user.getUserRoles().add(userRole);

    userService.encodeAndSetPassword(user, password);

    userService.addUser(user, actingUser);

    UserRole twoFactorRole = new UserRole();
    twoFactorRole.setUid("jcK4oq1Ol8x");
    twoFactorRole.setCode("TwoFactor");
    twoFactorRole.setName("TwoFactor");
    twoFactorRole.setDescription("TwoFactor");
    twoFactorRole.setRestrictions(ALL_RESTRICTIONS);
  }
}
