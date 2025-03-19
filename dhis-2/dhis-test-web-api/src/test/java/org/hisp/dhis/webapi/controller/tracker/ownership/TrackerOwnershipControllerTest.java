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
package org.hisp.dhis.webapi.controller.tracker.ownership;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;

import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link TrackerOwnershipController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class TrackerOwnershipControllerTest extends PostgresControllerIntegrationTestBase {

  private String orgUnitAUid;

  private String orgUnitBUid;

  private String teUid;

  private String tetId;

  private String pId;

  private User regularUser;

  @BeforeEach
  void setUp() {
    orgUnitAUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    orgUnitBUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));

    OrganisationUnit orgUnitA = manager.get(OrganisationUnit.class, orgUnitAUid);
    OrganisationUnit orgUnitB = manager.get(OrganisationUnit.class, orgUnitBUid);
    regularUser = createAndAddUser(false, "regular-user", emptySet(), emptySet());
    regularUser.setTeiSearchOrganisationUnits(Set.of(orgUnitA, orgUnitB));
    manager.save(regularUser);
    User superuser =
        createAndAddUser(true, "superuser", Set.of(orgUnitA, orgUnitB), Set.of(orgUnitA, orgUnitB));
    injectSecurityContextUser(superuser);

    tetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityTypes/",
                "{'name': 'A', 'shortName':'A','sharing':{'external':false,'public':'rwrw----'}}"));

    pId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                """
                                {
                                  'name':'P1',
                                  'shortName':'P1',
                                  'programType':'WITH_REGISTRATION',
                                  'accessLevel':'PROTECTED',
                                  'trackedEntityType': {'id': '%s'},
                                  'organisationUnits': [{'id':'%s'},{'id':'%s'}],
                                  'sharing':{'external':false,'public':'rwrw----'}
                                }
                                """
                    .formatted(tetId, orgUnitAUid, orgUnitBUid)));

    teUid = CodeGenerator.generateUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?async=false",
            """
            {
             "trackedEntities": [
               {
                 "trackedEntity": "%s",
                 "trackedEntityType": "%s",
                 "orgUnit": "%s",
                 "enrollments": [
                  {
                    "orgUnit": "%s",
                    "program": "%s",
                    "occurredAt": "2025-02-26",
                    "enrolledAt": "2025-02-26"
                  }
                 ]
               }
             ]
            }
            """
                .formatted(teUid, tetId, orgUnitAUid, orgUnitAUid, pId)));
  }

  @Test
  void shouldUpdateTrackerProgramOwnerAndBeAccessibleFromTransferredOrgUnit() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Ownership transferred",
        PUT(
                "/tracker/ownership/transfer?trackedEntity={te}&program={prog}&orgUnit={orgUnit}",
                teUid,
                pId,
                orgUnitBUid)
            .content(HttpStatus.OK));
  }

  @Test
  void
      shouldUpdateTrackerProgramOwnerAndBeAccessibleFromTransferredOrgUnitUsingDeprecatedParameter() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Ownership transferred",
        PUT(
                "/tracker/ownership/transfer?trackedEntity={te}&program={prog}&ou={ou}",
                teUid,
                pId,
                orgUnitBUid)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldFailToTransferIfGivenDeprecatedAndNewOrgUnitParameter() {
    assertStartsWith(
        "Only one parameter of 'ou'",
        PUT(
                "/tracker/ownership/transfer?trackedEntity={te}&program={prog}&ou={ou}&orgUnit={orgUnit}",
                teUid,
                pId,
                orgUnitBUid,
                orgUnitBUid)
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldFailToUpdateWhenNoTrackedEntityIsPresent() {
    assertStartsWith(
        "Required parameter 'trackedEntity'",
        PUT("/tracker/ownership/transfer?program={prog}&ou={ou}", pId, orgUnitAUid)
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGrantTemporaryAccess() {
    injectSecurityContextUser(regularUser);
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Temporary Ownership granted",
        POST("/tracker/ownership/override?trackedEntity={te}&program={prog}&reason=42", teUid, pId)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldGrantTemporaryAccessWhenTEEnrolledInProgram() {
    teUid = CodeGenerator.generateUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?async=false",
            """
            {
             "trackedEntities": [
               {
                 "trackedEntity": "%s",
                 "trackedEntityType": "%s",
                 "orgUnit": "%s",
                 "enrollments": [
                   {
                    "program": "%s",
                    "orgUnit": "%s",
                    "status": "ACTIVE",
                    "enrolledAt": "2023-06-16",
                    "occurredAt': "2023-06-16"
                   }
                  ]
               }
             ]
            }
            """
                .formatted(teUid, tetId, orgUnitAUid, pId, orgUnitAUid)));

    injectSecurityContextUser(regularUser);
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Temporary Ownership granted",
        POST("/tracker/ownership/override?trackedEntity={te}&program={prog}&reason=42", teUid, pId)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldFailToOverrideWhenNoTrackedEntityIsGiven() {
    assertStartsWith(
        "Required parameter 'trackedEntity'",
        POST("/tracker/ownership/override?program=" + pId + "&reason=42")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }
}
