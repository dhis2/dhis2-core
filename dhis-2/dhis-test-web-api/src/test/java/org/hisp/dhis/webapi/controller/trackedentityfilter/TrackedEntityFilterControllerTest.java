/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.trackedentityfilter;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityFilterControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;

  private final String programId = "BFcipDERJnf";
  private final String trackedEntityAttributeId = "dIVt4l5vIOa";

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    manager.flush();
    manager.clear();
  }

  @Test
  void shouldCreateTrackedEntityFilter() {
    String operatorFilterId = createOperatorFilter("equals operator filter", "eq", "value");

    JsonTrackedEntityFilter trackedEntityFilter =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId)
            .content()
            .as(JsonTrackedEntityFilter.class);

    assertFalse(trackedEntityFilter.isEmpty());
    assertEquals(programId, trackedEntityFilter.getProgram());
    JsonObject attributeObject =
        trackedEntityFilter.getEntityQueryCriteria().getAttributeValueFilters().getObject(0);
    assertEquals(trackedEntityAttributeId, attributeObject.getString("attribute").string());
    assertEquals("value", attributeObject.getString("eq").string());
  }

  @Test
  void shouldUpdateTrackedEntityFilter() {
    String operatorFilterId = createOperatorFilter("equals operator filter", "eq", "value");

    String updatedName = "Updated tracked entity filter";
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/trackedEntityInstanceFilters/" + operatorFilterId,
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "attributeValueFilters": [{"eq": "value", "attribute": "%s"}]
                  },
                  "name": "%s"
                }
            """
                .formatted(programId, trackedEntityAttributeId, updatedName)));

    String response =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId).content().toString();
    assertTrue(
        response.contains(updatedName),
        "Could not find the tracked entity filter name: " + updatedName + " in the response");
  }

  @Test
  void shouldDeleteTrackedEntityFilter() {
    String operatorFilterId = createOperatorFilter("equals operator filter", "eq", "value");

    HttpResponse response = DELETE("/trackedEntityInstanceFilters/" + operatorFilterId);
    assertEquals(HttpStatus.OK, response.status());
  }

  @Test
  void shouldFailWhenAssignedUserModeProvidedAndNoAssignedUsers() {
    HttpResponse response =
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "assignedUserMode": "PROVIDED"
                  },
                  "name": "name"
                }
            """
                .formatted(programId));

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Assigned Users cannot be empty with PROVIDED assigned user mode",
        response.error().getMessage());
  }

  @Test
  void shouldFailWhenOrgUnitModeSelectedAndNoOrgUnitProvided() {
    HttpResponse response =
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "ouMode": "SELECTED"
                  },
                  "name": "name"
                }
            """
                .formatted(programId));

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Organisation Unit cannot be empty with SELECTED org unit mode",
        response.error().getMessage());
  }

  @Test
  void shouldFailWhenNonExistentAttributeProvided() {
    UID nonExistentAttribute = UID.generate();

    HttpResponse response =
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "attributeValueFilters": [{"eq": "value", "attribute": "%s"}]
                  },
                  "name": "name"
                }
            """
                .formatted(programId, nonExistentAttribute.getValue()));

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains("No tracked entity attribute found", response.error().getMessage());
  }

  @Test
  void shouldCreateTrackedEntityFilterWhenNullOperatorSupplied() {
    String operatorFilterId = createOperatorFilter("null operator filter", "null", true);

    JsonTrackedEntityFilter trackedEntityFilter =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId)
            .content()
            .as(JsonTrackedEntityFilter.class);

    assertFalse(trackedEntityFilter.isEmpty());
    assertEquals(programId, trackedEntityFilter.getProgram());
    JsonObject attributeObject =
        trackedEntityFilter.getEntityQueryCriteria().getAttributeValueFilters().getObject(0);
    assertEquals(trackedEntityAttributeId, attributeObject.getString("attribute").string());
    assertEquals(Boolean.TRUE, attributeObject.getBoolean("null").booleanValue());
  }

  @Test
  void shouldCreateTrackedEntityFilterWhenNonNullOperatorSupplied() {
    String operatorFilterId = createOperatorFilter("non null operator filter", "null", false);

    JsonTrackedEntityFilter trackedEntityFilter =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId)
            .content()
            .as(JsonTrackedEntityFilter.class);

    assertFalse(trackedEntityFilter.isEmpty());
    assertEquals(programId, trackedEntityFilter.getProgram());
    JsonObject attributeObject =
        trackedEntityFilter.getEntityQueryCriteria().getAttributeValueFilters().getObject(0);
    assertEquals(trackedEntityAttributeId, attributeObject.getString("attribute").string());
    assertEquals(Boolean.FALSE, attributeObject.getBoolean("null").booleanValue());
  }

  private String createOperatorFilter(String filterName, String operator, Object value) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "attributeValueFilters": [{"%s": %s, "attribute": "%s"}]
                  },
                  "name": "%s"
                }
            """
                .formatted(
                    programId,
                    operator,
                    formatValue(value),
                    trackedEntityAttributeId,
                    filterName)));
  }

  private String formatValue(Object value) {
    return (value instanceof Boolean) ? value.toString() : "\"" + value + "\"";
  }
}
