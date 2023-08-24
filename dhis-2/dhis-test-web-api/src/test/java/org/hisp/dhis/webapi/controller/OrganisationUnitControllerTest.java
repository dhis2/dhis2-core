/*
 * Copyright (c) 2004-2023, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonOrganisationUnit;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.organisationunit.OrganisationUnit} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class OrganisationUnitControllerTest extends DhisControllerConvenienceTest {
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
  void testGetIncludeChildren() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true", ou0).content(), "L0", "L1", "L1x");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true", ou1).content(), "L1", "L21", "L22");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}?includeChildren=true", ou21).content(), "L21", "L31");
  }

  @Test
  void testGetChildrenWithLevel() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children?level=1", ou1).content(), "L21", "L22");
    assertListOfOrganisationUnits(
        GET("/organisationUnits/{id}/children?level=2", ou1).content(), "L31", "L32");
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
  }

  @Test
  void testGetQuery() {
    assertListOfOrganisationUnits(GET("/organisationUnits?query=L21").content(), "L21");
  }

  @Test
  void testGetLevel() {
    assertListOfOrganisationUnits(GET("/organisationUnits?level=3").content(), "L21", "L22", "L2x");
  }

  @Test
  void testGetMaxLevel() {
    assertListOfOrganisationUnits(
        GET("/organisationUnits?maxLevel=2").content(), "L0", "L1", "L1x");
  }

  @Test
  void testGetAllOrganisationUnitsByLevel() {
    assertEquals(
        List.of("L0", "L1", "L1x", "L21", "L22", "L2x", "L31", "L32", "L3x"),
        toOrganisationUnitNames(GET("/organisationUnits?levelSorted=true").content()));
  }

  private void assertListOfOrganisationUnits(JsonObject response, String... names) {
    assertContainsOnly(List.of(names), toOrganisationUnitNames(response));
  }

  private List<String> toOrganisationUnitNames(JsonObject response) {
    return response
        .getList("organisationUnits", JsonIdentifiableObject.class)
        .toList(JsonIdentifiableObject::getDisplayName);
  }

  private String addOrganisationUnit(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{'name':'" + name + "', 'shortName':'" + name + "', 'openingDate':'2021'}"));
  }

  private String addOrganisationUnit(String name, String parentId) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{'name':'"
                + name
                + "', 'shortName':'"
                + name
                + "', 'openingDate':'2021', 'parent': "
                + objectReference(parentId)
                + " }"));
  }
}
