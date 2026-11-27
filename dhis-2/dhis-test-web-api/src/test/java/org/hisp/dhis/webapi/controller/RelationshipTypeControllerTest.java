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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Transactional
class RelationshipTypeControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String RELATIONSHIP_TYPE_UID = "RelTypeCon1";

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  private String program;

  private String attrA;

  private String attrB;

  private String attrC;

  private String trackedEntityType;

  @BeforeEach
  void setUp() {
    trackedEntityType =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityTypes/",
                "{'name':'person', 'shortName':'person', 'description':'person', 'allowAuditLog':false }"));

    attrA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes/",
                "{'name':'attrA', 'shortName':'attrA', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    attrB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes/",
                "{'name':'attrB', 'shortName':'attrB', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    attrC =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes/",
                "{'name':'attrC', 'shortName':'attrC', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    program =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test program', 'id':'VoZMWi7rBgj', 'shortName':'test program','programType':'WITH_REGISTRATION', 'trackedEntityType': {"
                    + "'id': '"
                    + trackedEntityType
                    + "'},"
                    + " 'programTrackedEntityAttributes' :  [{ 'trackedEntityAttribute' :{'id': '"
                    + attrA
                    + "' }}, { 'trackedEntityAttribute' :{'id': '"
                    + attrB
                    + "' }}, { 'trackedEntityAttribute' :{'id': '"
                    + attrC
                    + "' }}] }"));
  }

  @Test
  void testPostingRelationshipTypes() {
    String relationshipTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/relationshipTypes/",
                "{'code': 'test-rel','description': 'test-rel','fromToName': 'A to B','toConstraint': { 'relationshipEntity': "
                    + "'TRACKED_ENTITY_INSTANCE','trackedEntityType': {'id': '"
                    + trackedEntityType
                    + "'}, 'program': {'id': '"
                    + program
                    + "'}, 'trackerDataView': {"
                    + "'attributes': ['"
                    + attrA
                    + "','"
                    + attrA
                    + "', '"
                    + attrB
                    + "','"
                    + attrC
                    + "']} }, "
                    + "'fromConstraint': { 'relationshipEntity':"
                    + "'TRACKED_ENTITY_INSTANCE' , 'trackedEntityType': {'id': '"
                    + trackedEntityType
                    + "'}, 'program': {'id': '"
                    + program
                    + "' }},'name': 'test-rel'}"));

    JsonList<JsonString> attributes =
        GET("/relationshipTypes/"
                + relationshipTypeId
                + "?fields=toConstraint[trackerDataView[attributes]]")
            .content()
            .getList("toConstraint.trackerDataView.attributes", JsonString.class);

    assertEquals(3, attributes.size());
    assertEquals(List.of(attrA, attrB, attrC), attributes.toList(JsonString::string));
  }

  @Test
  void testPutOfExportedTypeWithChangedFromConstraintUpdatesTheConstraint() throws Exception {
    String otherProgram =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                """
                {"name": "other program", "shortName": "other program",
                  "programType": "WITH_REGISTRATION", "trackedEntityType": {"id": "%s"},
                  "programTrackedEntityAttributes": [
                    {"trackedEntityAttribute": {"id": "%s"}},
                    {"trackedEntityAttribute": {"id": "%s"}}]}"""
                    .formatted(trackedEntityType, attrB, attrC)));
    String programStage = importRelationshipType();
    ObjectNode exported =
        (ObjectNode)
            JSON.readTree(
                GET("/relationshipTypes/" + RELATIONSHIP_TYPE_UID).content(HttpStatus.OK).toJson());
    ObjectNode fromConstraint = (ObjectNode) exported.get("fromConstraint");
    fromConstraint.putObject("program").put("id", otherProgram);
    ((ObjectNode) fromConstraint.get("trackerDataView"))
        .putArray("attributes")
        .add(attrC)
        .add(attrB);

    assertStatus(
        HttpStatus.OK, PUT("/relationshipTypes/" + RELATIONSHIP_TYPE_UID, exported.toString()));

    JsonObject type = reloadType();
    JsonObject from = type.getObject("fromConstraint");
    assertEquals("TRACKED_ENTITY_INSTANCE", from.getString("relationshipEntity").string());
    assertEquals(trackedEntityType, from.getString("trackedEntityType.id").string());
    assertEquals(otherProgram, from.getString("program.id").string());
    assertEquals(
        List.of(attrC, attrB),
        from.getList("trackerDataView.attributes", JsonString.class).toList(JsonString::string));
    JsonObject to = type.getObject("toConstraint");
    assertEquals("PROGRAM_STAGE_INSTANCE", to.getString("relationshipEntity").string());
    assertEquals(programStage, to.getString("programStage.id").string());
    assertEquals(
        2,
        jdbcTemplate.queryForObject("select count(*) from relationshipconstraint", Integer.class));
  }

  /**
   * Guards that no API property of the embedded constraints is dropped; null properties are
   * omitted.
   */
  @Test
  void testEmbeddedConstraintJsonKeys() {
    importRelationshipType();

    JsonObject type = reloadType();

    assertEquals(
        Set.of("relationshipEntity", "trackedEntityType", "program", "trackerDataView"),
        Set.copyOf(type.getObject("fromConstraint").names()));
    assertEquals(
        Set.of("relationshipEntity", "programStage", "trackerDataView"),
        Set.copyOf(type.getObject("toConstraint").names()));
    assertEquals(
        Set.of("attributes", "dataElements"),
        Set.copyOf(type.getObject("fromConstraint.trackerDataView").names()));
  }

  /**
   * Imports a relationship type from a tracked entity of the program to an event of a new program
   * stage, and returns the program stage's uid.
   */
  private String importRelationshipType() {
    String programStage =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programStages/",
                "{'name': 'test stage', 'program': {'id': '%s'}}".formatted(program)));
    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
            {"relationshipTypes": [{
              "id": "%s",
              "name": "Constraint type",
              "fromToName": "From",
              "toFromName": "To",
              "fromConstraint": {
                "relationshipEntity": "TRACKED_ENTITY_INSTANCE",
                "trackedEntityType": {"id": "%s"},
                "program": {"id": "%s"},
                "trackerDataView": {"attributes": ["%s"]}
              },
              "toConstraint": {
                "relationshipEntity": "PROGRAM_STAGE_INSTANCE",
                "programStage": {"id": "%s"},
                "trackerDataView": {"dataElements": []}
              }
            }]}"""
                .formatted(
                    RELATIONSHIP_TYPE_UID, trackedEntityType, program, attrA, programStage)));
    return programStage;
  }

  /**
   * The relationship type as exported after the session is flushed and cleared and the second-level
   * cache evicted, so it is read from the database.
   */
  private JsonObject reloadType() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/relationshipTypes/" + RELATIONSHIP_TYPE_UID).content(HttpStatus.OK);
  }
}
