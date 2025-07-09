/*
 * Copyright (c) 2004-2023, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.association.jdbc.JdbcOrgUnitAssociationsStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This Integration test using Postgres is necessary as the H2 DB doesn't work with {@link
 * JdbcOrgUnitAssociationsStore#checkOrganisationUnitsAssociations}. A ClassCastException is thrown.
 * H2 uses a different object type for its result set which doesn't allow the cast to String[] when
 * creating the orgUnit <-> program relationship map.
 *
 * @author David Mackessy
 */
@Transactional
class ProgramControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  @Autowired private ObjectMapper jsonMapper;

  @Autowired private OrganisationUnitService orgUnitService;

  public static final String PROGRAM_UID = "PrZMWi7rBga";

  public static final String ORG_UNIT_UID = "Orgunit1000";

  @BeforeEach
  public void testSetup() throws JsonProcessingException {
    DataElement dataElement1 = createDataElement('a');
    DataElement dataElement2 = createDataElement('b');
    dataElement1.setUid("deabcdefgha");
    dataElement2.setUid("deabcdefghb");
    TrackedEntityAttribute tea1 = createTrackedEntityAttribute('a');
    TrackedEntityAttribute tea2 = createTrackedEntityAttribute('b');
    tea1.setUid("TEA1nnnnnaa");
    tea2.setUid("TEA1nnnnnab");
    POST("/dataElements", jsonMapper.writeValueAsString(dataElement1)).content(HttpStatus.CREATED);
    POST("/dataElements", jsonMapper.writeValueAsString(dataElement2)).content(HttpStatus.CREATED);
    POST("/trackedEntityAttributes", jsonMapper.writeValueAsString(tea1))
        .content(HttpStatus.CREATED);
    POST("/trackedEntityAttributes", jsonMapper.writeValueAsString(tea2))
        .content(HttpStatus.CREATED);

    POST("/metadata", Path.of("program/create_program.json")).content(HttpStatus.OK);
  }

  @Test
  void testGistFilterCategoryMappings() {
    JsonObject noMapping = GET("/programs/gist?filter=categoryMappings:empty").content();
    JsonObject withMapping = GET("/programs/gist?filter=categoryMappings:!empty").content();
    assertEquals(0, withMapping.getArray("programs").size());
    assertEquals(1, noMapping.getArray("programs").size());
  }

  @Test
  void testFilterCategoryMappings() {
    JsonObject noMapping = GET("/programs?filter=categoryMappings:empty").content();
    JsonObject withMapping = GET("/programs?filter=categoryMappings:!empty").content();
    assertEquals(0, withMapping.getArray("programs").size());
    assertEquals(1, noMapping.getArray("programs").size());
  }

  @Test
  void shouldNotCopyTrackerProgramEnrollmentsWhenCopyingProgram() {
    OrganisationUnit orgUnit = orgUnitService.getOrganisationUnit(ORG_UNIT_UID);
    User user = createAndAddUser(true, "user", Set.of(orgUnit), Set.of(orgUnit));
    injectSecurityContextUser(user);

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/trackedEntityTypes",
            "{'description': 'add TET for Enrollment test','id':'TEType10000','name':'Tracked Entity Type 1', 'shortName':'TET1'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/trackedEntityAttributes/",
            "{'name':'attrA', 'id':'TEAttr10000','shortName':'attrA', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    POST(
            "/tracker?async=false",
            """
              {
              "trackedEntities": [
              {
                "trackedEntityType": "TEType10000",
                "orgUnit": "%s",
                "enrollments": [
                  {
                    "program": "PrZMWi7rBga",
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
                .formatted(ORG_UNIT_UID, ORG_UNIT_UID))
        .content(HttpStatus.OK);

    POST("/programs/%s/copy".formatted(PROGRAM_UID))
        .content(HttpStatus.CREATED)
        .as(JsonWebMessage.class);

    JsonWebMessage enrollmentsForOrgUnit =
        GET("/tracker/enrollments?orgUnit=%s&program=%s".formatted(ORG_UNIT_UID, PROGRAM_UID))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    JsonList<JsonEnrollment> enrollments =
        enrollmentsForOrgUnit.getList("enrollments", JsonEnrollment.class);
    Set<JsonEnrollment> originalProgramEnrollments =
        enrollments.stream()
            .filter(enrollment -> enrollment.getProgram().equals(PROGRAM_UID))
            .collect(Collectors.toSet());

    assertEquals(1, enrollments.size());
    assertEquals(1, originalProgramEnrollments.size());
  }

  @Test
  @Disabled("Some issue in the setup")
  void testCopyProgramWithNoPublicSharingWithUserAdded() {
    User userWithPublicAuths =
        switchToNewUser("test1", "F_PROGRAM_PUBLIC_ADD", "F_PROGRAM_INDICATOR_PUBLIC_ADD");

    switchToAdminUser();
    manager.save(userWithPublicAuths);
    PUT(
            "/programs/" + PROGRAM_UID,
            "{\n"
                + "    'id': '"
                + PROGRAM_UID
                + "',\n"
                + "    'name': 'test program',\n"
                + "    'shortName': 'test program',\n"
                + "    'programType': 'WITH_REGISTRATION',\n"
                + "    'sharing': {\n"
                + "        'public': '--------',\n"
                + "        'users': {\n"
                + "           '"
                + userWithPublicAuths.getUid()
                + "': {\n"
                + "              'id': '"
                + userWithPublicAuths.getUid()
                + "',\n"
                + "              'access': 'rw------'\n"
                + "        }\n"
                + "     }\n"
                + "    }\n"
                + "}")
        .content(HttpStatus.OK);

    switchContextToUser(userWithPublicAuths);

    assertStatus(HttpStatus.CREATED, POST("/programs/%s/copy".formatted(PROGRAM_UID)));
  }

  @Test
  @Disabled("Some issue in the setup")
  void testCopyProgramWithNoPublicSharingWithUserAddedWithWriteOnlyAccess() {
    User userWithPublicAuths =
        switchToNewUser("test1", "F_PROGRAM_PUBLIC_ADD", "F_PROGRAM_INDICATOR_PUBLIC_ADD");

    switchToAdminUser();
    manager.save(userWithPublicAuths);
    PUT(
            "/programs/" + PROGRAM_UID,
            "{\n"
                + "    'id': '"
                + PROGRAM_UID
                + "',\n"
                + "    'name': 'test program',\n"
                + "    'shortName': 'test program',\n"
                + "    'programType': 'WITH_REGISTRATION',\n"
                + "    'sharing': {\n"
                + "        'public': '--------',\n"
                + "        'users': {\n"
                + "           '"
                + userWithPublicAuths.getUid()
                + "': {\n"
                + "              'id': '"
                + userWithPublicAuths.getUid()
                + "',\n"
                + "              'access': '-w------'\n"
                + "        }\n"
                + "     }\n"
                + "    }\n"
                + "}")
        .content(HttpStatus.OK);

    switchContextToUser(userWithPublicAuths);

    assertStatus(HttpStatus.NOT_FOUND, POST("/programs/%s/copy".formatted(PROGRAM_UID)));
  }

  @Test
  @Disabled("Some issue in the setup")
  void testCopyProgramWithNoPublicSharingWithUserAddedWithReadOnlyAccess() {
    User userWithPublicAuths =
        switchToNewUser("test1", "F_PROGRAM_PUBLIC_ADD", "F_PROGRAM_INDICATOR_PUBLIC_ADD");

    switchToAdminUser();
    manager.save(userWithPublicAuths);
    PUT(
            "/programs/" + PROGRAM_UID,
            "{\n"
                + "    'id': '"
                + PROGRAM_UID
                + "',\n"
                + "    'name': 'test program',\n"
                + "    'shortName': 'test program',\n"
                + "    'programType': 'WITH_REGISTRATION',\n"
                + "    'sharing': {\n"
                + "        'public': '--------',\n"
                + "        'users': {\n"
                + "           '"
                + userWithPublicAuths.getUid()
                + "': {\n"
                + "              'id': '"
                + userWithPublicAuths.getUid()
                + "',\n"
                + "              'access': 'r-------'\n"
                + "        }\n"
                + "     }\n"
                + "    }\n"
                + "}")
        .content(HttpStatus.OK);

    switchContextToUser(userWithPublicAuths);

    assertStatus(HttpStatus.FORBIDDEN, POST("/programs/%s/copy".formatted(PROGRAM_UID)));
  }
}
