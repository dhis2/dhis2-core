/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
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
    // Note that two user roles already exist due to the setup in the
    // AbstractDataIntegrityIntegrationTest class
    // Thus there should be 4 roles total. Only the Empty role should be flagged.
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK_NAME, 25, userRoleUid, "Empty role", null, true);
  }
}
