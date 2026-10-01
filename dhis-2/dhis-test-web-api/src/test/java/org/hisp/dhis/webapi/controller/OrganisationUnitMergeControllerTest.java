/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.web.WebClientUtils.objectReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.merge.MergeLock;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@code /organisationUnits/merge} endpoint. On Postgres, as the merge lock is a Postgres
 * advisory lock.
 */
class OrganisationUnitMergeControllerTest extends DhisControllerIntegrationTest {
  @Autowired private DataSource dataSource;

  private String ou21, ou22;

  @BeforeEach
  void setUp() {
    String ou0 = addOrganisationUnit("L0", null);
    String ou1 = addOrganisationUnit("L1", ou0);
    ou21 = addOrganisationUnit("L21", ou1);
    ou22 = addOrganisationUnit("L22", ou1);
    addOrganisationUnit("L31", ou21);
    addOrganisationUnit("L32", ou22);
  }

  @Test
  void testMergeRejectedWhileAnotherMergeHoldsTheMergeLock() throws SQLException {
    // Simulate a merge in progress on another connection, e.g. another cluster instance
    try (Connection other = dataSource.getConnection()) {
      assertTrue(callMergeLockFunction(other, "pg_try_advisory_lock"));
      try {
        assertEquals(
            ErrorCode.E1505,
            POST("/organisationUnits/merge", mergeBody(ou21, ou22))
                .error(HttpStatus.CONFLICT)
                .getErrorCode());
      } finally {
        callMergeLockFunction(other, "pg_advisory_unlock");
      }
    }

    GET("/organisationUnits/{id}", ou21).content(HttpStatus.OK);
  }

  @Test
  void testMergeRequiresMergeAuthority() {
    switchToNewUser("guest");

    JsonWebMessage response =
        POST("/organisationUnits/merge", mergeBody(ou21, ou22))
            .content(HttpStatus.FORBIDDEN)
            .as(JsonWebMessage.class);
    assertEquals("Access is denied", response.getMessage());
    assertNotNull(manager.get(OrganisationUnit.class, ou21));
  }

  @Test
  void testMerge() {
    assertStatus(HttpStatus.OK, POST("/organisationUnits/merge", mergeBody(ou21, ou22)));

    GET("/organisationUnits/{id}", ou21).content(HttpStatus.NOT_FOUND);
    assertContainsOnly(
        List.of("L22", "L31", "L32"),
        toOrganisationUnitNames(GET("/organisationUnits/{id}/children", ou22).content()));
  }

  private static String mergeBody(String source, String target) {
    return """
        {"sources": ["%s"], "target": "%s", "deleteSources": true}"""
        .formatted(source, target);
  }

  private static boolean callMergeLockFunction(Connection connection, String function)
      throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement("select " + function + "(?)")) {
      ps.setLong(1, MergeLock.LOCK_KEY);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        return rs.getBoolean(1);
      }
    }
  }

  private List<String> toOrganisationUnitNames(JsonObject response) {
    return response
        .getList("organisationUnits", JsonIdentifiableObject.class)
        .toList(JsonIdentifiableObject::getDisplayName);
  }

  private String addOrganisationUnit(String name, String parentId) {
    String parent = parentId == null ? "" : ", 'parent': " + objectReference(parentId);
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{'name':'%s', 'shortName':'%s', 'openingDate':'2021'%s}"
                .formatted(name, name, parent)));
  }
}
