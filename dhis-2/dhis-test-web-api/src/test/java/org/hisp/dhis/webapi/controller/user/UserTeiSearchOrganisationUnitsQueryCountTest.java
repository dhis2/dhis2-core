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
package org.hisp.dhis.webapi.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression for DHIS2-21907: {@code GET /api/users} with {@code teiSearchOrganisationUnits} in the
 * field filter must not select from {@code userteisearchorgunits} once per listed user.
 *
 * <p>Author: Morten (Morty)
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class UserTeiSearchOrganisationUnitsQueryCountTest extends PostgresControllerIntegrationTestBase {

  private static final int USER_COUNT = 12;

  @Test
  @DisplayName("User list teiSearchOrganisationUnits are batch-loaded, not fetched per user")
  void teiSearchOrganisationUnitsAreNotLoadedPerUser() {
    OrganisationUnit ou = createOrganisationUnit('T');
    manager.save(ou);

    for (int i = 0; i < USER_COUNT; i++) {
      User user = createUserWithAuth("teiq" + i, "ALL");
      user.getTeiSearchOrganisationUnits().add(ou);
      userService.updateUser(user);
    }

    // Persist and drop first-level cache so the list request reloads collections from the DB.
    manager.flush();
    manager.clear();
    injectSecurityContextUser(getAdminUser());
    QueryCountDataSourceProxy.clearCapturedSql();

    // Avoid {placeholders} colliding with field-filter brackets; use filter + fixed pageSize.
    var response =
        GET(
            "/users?fields=id,username,teiSearchOrganisationUnits[id]"
                + "&filter=username:like:teiq&pageSize=50&paging=true");
    assertEquals(
        USER_COUNT,
        response.content().getArray("users").size(),
        "expected the seeded teiq* users in the list response");

    long teiSearchQueries =
        QueryCountDataSourceProxy.countCapturedSqlMatching("userteisearchorgunits");
    assertTrue(
        teiSearchQueries <= 1,
        "teiSearchOrganisationUnits must be batch-loaded, but userteisearchorgunits was queried "
            + teiSearchQueries
            + " times for "
            + USER_COUNT
            + " users");
  }
}
