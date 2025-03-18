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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for controller tests of the Gist API that require an actual postgres DB.
 *
 * <p>This usually is because of the use of JSONB functions. Another reason is use of generated
 * columns.
 *
 * @author Jan Bernitt
 */
abstract class AbstractGistControllerPostgresTest extends PostgresControllerIntegrationTestBase {

  protected String userGroupId;
  protected String orgUnitId;
  protected String dataSetId;
  protected User userA;

  @BeforeEach
  void setUp() {
    userA = createUserWithAuth("userGist", "ALL");

    switchContextToUser(userA);

    userGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups/", "{'name':'groupX', 'users':[{'id':'" + getAdminUid() + "'}]}"));
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/{id}?importReportMode=ERRORS",
            getAdminUid(),
            Body("[{'op': 'add', 'path': '/birthday', 'value': '1980-12-12'}]")));
    orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'unitA', 'shortName':'unitA', 'openingDate':'2021-01-01'}"));
    dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'set1', 'shortName':'set1', 'organisationUnits': [{'id':'"
                    + orgUnitId
                    + "'}], 'periodType':'Daily'}"));
  }
}
