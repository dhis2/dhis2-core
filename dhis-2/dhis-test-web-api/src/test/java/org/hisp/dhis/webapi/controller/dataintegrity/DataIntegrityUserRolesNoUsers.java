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

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.Test;

class DataIntegrityUserRolesNoUsers extends AbstractDataIntegrityIntegrationTest {
  private static final String CHECK_NAME = "user_roles_with_no_users";

  private static final String DETAILS_ID_TYPE = "userRoles";

  private String userRoleUid;

  @Test
  void testUserRolesNoUsers() {
    userRoleUid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userRoles", "{ 'name': 'Test role', 'authorities': ['F_DATAVALUE_ADD'] }"));
    // Note that two user roles already exist as part of the setup in the
    // AbstractDataIntegrityIntegrationTest class
    // Thus, there should be three roles in total, two of which are valid since they already have
    // users associated with them.
    postSummary(CHECK_NAME);

    JsonDataIntegritySummary summary = getSummary(CHECK_NAME);
    assertEquals(1, summary.getCount());
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK_NAME, 50, userRoleUid, "Test role", null, true);
  }
}
