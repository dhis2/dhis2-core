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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonOrganisationUnit;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.organisationunit.OrganisationUnit} using (mocked) REST requests.
 *
 * @author David Mackessy
 */
class OrganisationUnitControllerTest extends DhisControllerConvenienceTest {
  private String ou0;

  @BeforeEach
  void setUp() {
    ou0 = addOrganisationUnit("L0");
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

  private String addOrganisationUnit(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            String.format(
                "{"
                    + "'name':'%s',"
                    + "'shortName':'%s',"
                    + "'openingDate':'2021',"
                    + "'description':'Org desc',"
                    + "'code':'Org code'"
                    + "}",
                name, name)));
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
}
