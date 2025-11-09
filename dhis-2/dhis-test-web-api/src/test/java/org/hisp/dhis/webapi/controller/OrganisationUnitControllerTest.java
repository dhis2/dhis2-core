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

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonOrganisationUnit;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.organisationunit.OrganisationUnit} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class OrganisationUnitControllerTest extends PostgresControllerIntegrationTestBase {
  private String ou0, ou1, ou21, ou22;

  @BeforeEach
  void setUp() {
    ou0 = addOrganisationUnit("L0");
    ou1 = addOrganisationUnit("L1", ou0);
    ou21 = addOrganisationUnit("L21", ou1);
    ou22 = addOrganisationUnit("L22", ou1);
    addOrganisationUnit("L31", ou21);
    addOrganisationUnit("L32", ou22);

    // what should not be matched but exists
    String ou1x = addOrganisationUnit("L1x", ou0);
    String ou2x = addOrganisationUnit("L2x", ou1x);
    addOrganisationUnit("L3x", ou2x);
  }

  @Test
  void testGetChildren() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children", ou0).content(), "L0", "L1", "L1x");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children", ou1).content(), "L1", "L21", "L22");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children", ou21).content(), "L21", "L31");
  }

  @Test
  void testGetOrgUnitWithIeqFilter() {
    JsonWebMessage jsonWebMessage =
        GET("/organisationUnits?filter=name:ieq:l0").content().as(JsonWebMessage.class);
    JsonList<JsonOrganisationUnit> organisationUnits =
        jsonWebMessage.getList("organisationUnits", JsonOrganisationUnit.class);
    assertFalse(organisationUnits.isEmpty());
    assertEquals("L0", organisationUnits.get(0).getDisplayName());
  }

  @Test
  void getOrgUnitNameableFieldsTest() {
    JsonWebMessage jsonWebMessage =
        GET("/organisationUnits/" + ou0 + "?fields=:nameable").content().as(JsonWebMessage.class);
    JsonOrganisationUnit organisationUnit = jsonWebMessage.as(JsonOrganisationUnit.class);
    assertEquals("L0", organisationUnit.getName());
    assertEquals("L0", organisationUnit.getString("shortName").string());
    assertEquals("Org desc", organisationUnit.getString("description").string());
    assertEquals("Org code", organisationUnit.getString("code").string());
    assertNotNull(organisationUnit.getCreated());
    assertNotNull(organisationUnit.getLastUpdated());
    assertNotNull(organisationUnit.getId());
  }

  @Test
  void testGetIncludeChildren() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true", ou0).content(), "L0", "L1", "L1x");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true", ou1).content(), "L1", "L21", "L22");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true", ou21).content(), "L21", "L31");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true&filter=displayName:ilike:L2", ou1)
            .content(),
        "L21",
        "L22");
  }

  @Test
  void testGetChildrenWithLevel() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children?level=1", ou1).content(), "L21", "L22");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children?level=2", ou1).content(), "L31", "L32");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children?level=2&filter=displayName:ilike:31", ou1).content(),
        "L31");
  }

  @Test
  void testGetObjectWithLevel() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?level=1", ou1).content(), "L21", "L22");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?level=2", ou1).content(), "L31", "L32");
  }

  @Test
  void testGetDescendants() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/descendants", ou1).content(),
        "L1",
        "L21",
        "L22",
        "L31",
        "L32");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/descendants", ou21).content(), "L21", "L31");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/descendants?filter=displayName:ilike:L2", ou1).content(),
        "L21",
        "L22");
  }

  @Test
  void testGetIncludeDescendants() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeDescendants=true", ou1).content(),
        "L1",
        "L21",
        "L22",
        "L31",
        "L32");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeDescendants=true", ou21).content(), "L21", "L31");
  }

  @Test
  void testGetAncestors() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/ancestors", ou22).content(), "L22", "L1", "L0");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/ancestors?filter=displayName:ilike:22", ou22).content(),
        "L22");
  }

  @Test
  void testGetIncludeAncestors() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeAncestors=true", ou22).content(), "L22", "L1", "L0");
  }

  @Test
  void testGetParents() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/parents", ou21).content(), "L1", "L0");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/parents?filter=displayName:ilike:L0", ou21).content(), "L0");
  }

  @Test
  void testGetParents_Root() {
    assertListOfOrganisationUnits(GET("/organisationUnits/{id}/parents", ou0).content());
  }

  @Test
  void testGetQuery() {
    assertListOfOrganisationUnits(GET("/organisationUnits?query=L21").content(), "L21");
    assertListOfOrganisationUnits(
        GET("/organisationUnits?query=L21&filter=displayName:ilike:L2").content(), "L21");
  }

  @Test
  void testGetLevel() {
    assertListOfOrganisationUnits(GET("/organisationUnits?level=3").content(), "L21", "L22", "L2x");
  }

  @Test
  void testGetLevelAndQuery() {
    // just to show what the result without level filter looks like
    assertListOfOrganisationUnits(GET("/organisationUnits?query=x").content(), "L1x", "L2x", "L3x");
    // now the filter of level and query combined only L2x matches x and level 3
    assertListOfOrganisationUnits(GET("/organisationUnits?level=3&query=x").content(), "L2x");
  }

  @Test
  void testGetMaxLevel() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits?maxLevel=2").content(), "L0", "L1", "L1x");
  }

  @Test
  void testGetAllOrganisationUnitsByLevel() {
    assertEquals(
        List.of("L0", "L1x", "L1", "L2x", "L22", "L21", "L3x", "L31", "L32"),
        toOrganisationUnitNames(GET("/organisationUnits?levelSorted=true").content()));
  }

  @Test
  void testGetUserRoleUsersAreTransformed() {
    User user = makeUser("Y");
    user.setEmail("y@y.org");

    OrganisationUnit organisationUnit = manager.get(OrganisationUnit.class, ou0);
    user.addOrganisationUnit(organisationUnit);
    userService.addUser(user);

    JsonObject userInRole =
        GET("/organisationUnits/{id}?fields=users[*]", ou0)
            .content(HttpStatus.OK)
            .getArray("users")
            .getObject(0);

    assertFalse(userInRole.has("email"), "email should not be exposed");
    assertEquals(user.getUid(), userInRole.getString("id").string());
  }

  @Test
  void testGetWithinDataViewUserHierarchy() {
    // Create a new user with data view org units
    User user = makeUser("a");
    OrganisationUnit ou1Unit = manager.get(OrganisationUnit.class, ou1);
    user.getDataViewOrganisationUnits().add(ou1Unit);
    userService.addUser(user);

    switchToNewUser(user);

    // When withinDataViewUserHierarchy is true, should only get org units
    // at or below the data view org units (ou1)
    // Expected: L1, L21, L22, L31, L32 (ou1 and its descendants)
    assertListOfOrganisationUnits(
        GET("/organisationUnits?withinDataViewUserHierarchy=true").content(),
        "L1",
        "L21",
        "L22",
        "L31",
        "L32");
  }

  @Test
  void testGetWithinDataViewUserHierarchyAndLevel() {
    // Create a new user with data view org units
    User user = makeUser("b");
    OrganisationUnit ou1Unit = manager.get(OrganisationUnit.class, ou1);
    user.getDataViewOrganisationUnits().add(ou1Unit);
    userService.addUser(user);

    switchToNewUser(user);

    // Combine withinDataViewUserHierarchy with level filter
    // Should only get level 3 org units within the data view hierarchy
    assertListOfOrganisationUnits(
        GET("/organisationUnits?withinDataViewUserHierarchy=true&level=3").content(), "L21", "L22");
  }

  private void assertListOfOrganisationUnits(JsonObject response, String... names) {
    assertContainsOnly(List.of(names), toOrganisationUnitNames(response));
    assertEquals(names.length, response.getObject("pager").getNumber("total").intValue());
  }
}
