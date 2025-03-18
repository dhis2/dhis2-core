/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.json.domain.JsonUserRole;
import org.junit.jupiter.api.Test;

class DataIntegrityUserRolesNoAuthorities extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK_NAME = "user_roles_no_authorities";

  private static final String DETAILS_ID_TYPE = "userRoles";

  private String userRoleUid;

  @Test
  void testUserRolesNoAuthorities() {
    userRoleUid =
        assertStatus(
            HttpStatus.CREATED, POST("/userRoles", "{ 'name': 'Empty role', 'authorities': [] }"));
    assertStatus(
        HttpStatus.CREATED,
        POST("/userRoles", "{ 'name': 'Good role', 'authorities': ['F_DATAVALUE_ADD'] }"));

    JsonObject content = GET("/userRoles?fields=id,authorities").content();
    JsonList<JsonUserRole> userRolesInSystem = content.getList("userRoles", JsonUserRole.class);
    assertEquals(3, userRolesInSystem.size());

    List<Integer> authorityCount =
        userRolesInSystem.stream()
            .map(userRole -> userRole.getList("authorities", JsonString.class).size())
            .toList();

    // Two of the roles have no authorities, one has one authority.
    assertEquals(Set.of(0, 1), new HashSet<>(authorityCount));
    assertEquals(1, Collections.frequency(authorityCount, 0));
    assertEquals(2, Collections.frequency(authorityCount, 1));

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK_NAME, 33, userRoleUid, "Empty role", null, true);
  }
}
