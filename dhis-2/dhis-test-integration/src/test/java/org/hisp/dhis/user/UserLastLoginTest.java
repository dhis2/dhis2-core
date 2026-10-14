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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.hibernate.cfg.AvailableSettings;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.test.config.PostgresTestConfigOverride;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The last login is written outside the entity lifecycle, see {@link
 * UserStore#updateLastLogin(long, Date)}. Cached queries and entity updates must still see and keep
 * it. Runs with the query cache enabled, as in production, and with a transaction per step, as
 * cached query results are shared between transactions.
 *
 * @author Morten (Morty) <netroms@gmail.com>
 */
@ContextConfiguration(classes = {UserLastLoginTest.DhisConfig.class})
class UserLastLoginTest extends PostgresIntegrationTestBase {

  static class DhisConfig {
    @Bean
    public PostgresTestConfigOverride postgresTestConfigOverride() {
      PostgresTestConfigOverride override = new PostgresTestConfigOverride();
      override.put(AvailableSettings.USE_QUERY_CACHE, "true");
      return override;
    }
  }

  private static final String OPEN_ID = "linked-accounts";

  @Autowired private UserStore userStore;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testSwitchOfLinkedAccountIsSeenByCachedOpenIdQuery() {
    User userA = addUserWithOpenId("A");
    User userB = addUserWithOpenId("B");

    userService.setActiveLinkedAccounts(userA.getUsername(), userB.getUsername());
    assertEquals(userB.getUid(), userService.getUserByOpenId(OPEN_ID).getUid());

    userService.setActiveLinkedAccounts(userA.getUsername(), userA.getUsername());
    assertEquals(userA.getUid(), userService.getUserByOpenId(OPEN_ID).getUid());
  }

  @Test
  void testLoginIsSeenByCachedLastLoginQuery() {
    User user = createAndAddUser("loggedin");
    UserQueryParams loggedInSinceYesterday =
        new UserQueryParams().setLastLogin(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
    assertFalse(userUids(loggedInSinceYesterday).contains(user.getUid()));

    userService.setLastLogin(user.getUsername());

    assertTrue(userUids(loggedInSinceYesterday).contains(user.getUid()));
  }

  @Test
  void testUpdateOfUserLoadedBeforeLoginKeepsLastLogin() {
    User user = createAndAddUser("stale");

    transactionTemplate.execute(
        status -> {
          User loadedBeforeLogin = userService.getUser(user.getUid());
          userStore.updateLastLogin(loadedBeforeLogin.getId(), new Date());
          loadedBeforeLogin.setFirstName("changed");
          userService.updateUser(loadedBeforeLogin);
          return null;
        });

    assertNotNull(
        jdbcTemplate.queryForObject(
            "select lastlogin from userinfo where uid = ?", Date.class, user.getUid()));
  }

  private User addUserWithOpenId(String uniqueCharacter) {
    User user = makeUser(uniqueCharacter);
    user.setOpenId(OPEN_ID);
    userService.addUser(user);
    return user;
  }

  private List<String> userUids(UserQueryParams params) {
    return IdentifiableObjectUtils.getUids(userService.getUsers(params));
  }
}
