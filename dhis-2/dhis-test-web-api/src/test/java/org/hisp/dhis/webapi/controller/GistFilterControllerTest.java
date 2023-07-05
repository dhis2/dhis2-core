/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.gist.GistQuery.Filter} related features of the Gist API.
 *
 * @author Jan Bernitt
 */
class GistFilterControllerTest extends AbstractGistControllerTest {

  @Test
  void testFilter_Null() {
    assertEquals(2, GET("/users/gist?filter=skype:null&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=surname:null&headless=true").content().size());
  }

  @Test
  void testFilter_NotNull() {
    assertEquals(2, GET("/users/gist?filter=code:!null&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=skype:!null&headless=true").content().size());
  }

  @Test
  void testFilter_Eq() {
    assertEquals(1, GET("/users/gist?filter=code:eq:Codeadmin&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=code:eq:Hans&headless=true").content().size());
  }

  @Test
  void testFilter_NotEq() {
    assertEquals(2, GET("/users/gist?filter=code:!eq:Paul&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=code:neq:Paul&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=code:ne:Paul&headless=true").content().size());
    assertEquals(1, GET("/users/gist?filter=code:ne:Codeadmin&headless=true").content().size());
  }

  @Test
  void testFilter_Empty() {
    OrganisationUnit ou = organisationUnitService.getOrganisationUnit(orgUnitId);
    ou.setComment("");
    organisationUnitService.updateOrganisationUnit(ou);
    assertEquals(
        1, GET("/organisationUnits/gist?filter=comment:empty&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=surname:empty&headless=true").content().size());
  }

  @Test
  void testFilter_NotEmpty() {
    OrganisationUnit ou = organisationUnitService.getOrganisationUnit(orgUnitId);
    ou.setComment("");
    organisationUnitService.updateOrganisationUnit(ou);
    assertEquals(2, GET("/users/gist?filter=surname:!empty&headless=true").content().size());
    assertEquals(
        0, GET("/organisationUnits/gist?filter=comment:!empty&headless=true").content().size());
  }

  @Test
  void testFilter_LessThan() {
    assertEquals(2, GET("/users/gist?filter=lastUpdated:lt:now&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=lastUpdated:lt:2000-01-01&headless=true").content().size());
  }

  @Test
  void testFilter_LessThanOrEqual() {
    assertEquals(2, GET("/users/gist?filter=created:le:now&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=created:lte:now&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=created:lte:2000-01-01&headless=true").content().size());
  }

  @Test
  void testFilter_GreaterThan() {
    assertEquals(
        2, GET("/users/gist?filter=lastUpdated:gt:2000-01-01&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=lastUpdated:gt:2525-01-01&headless=true").content().size());
  }

  @Test
  void testFilter_GreaterThanOrEqual() {
    assertEquals(
        2, GET("/users/gist?filter=created:gte:2000-01-01&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=created:ge:2000-01-01&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=created:ge:2525-01-01&headless=true").content().size());
  }

  @Test
  void testFilter_Like() {
    assertEquals(1, GET("/users/gist?filter=surname:like:mi&headless=true").content().size());
    assertEquals(
        1, GET("/users/gist?filter=surname:like:?urnameuserA&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:like:Surna*&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=surname:like:Zulu&headless=true").content().size());
  }

  @Test
  void testFilter_NotLike() {
    assertEquals(2, GET("/users/gist?filter=username:!like:mike&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:!like:?min&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:!like:ap*&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:!like:Sur?in&headless=true").content().size());
  }

  @Test
  void testFilter_ILike() {
    assertEquals(1, GET("/users/gist?filter=surname:ilike:Mi&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=surname:ilike:?headless&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:ilike:Sur*&headless=true").content().size());
    assertEquals(0, GET("/users/gist?filter=surname:ilike:Zulu&headless=true").content().size());
  }

  @Test
  void testFilter_NotILike() {
    assertEquals(2, GET("/users/gist?filter=username:!ilike:Mike&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:!ilike:?min&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:!ilike:aP*&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=surname:!ilike:Sur?in&headless=true").content().size());
  }

  @Test
  void testFilter_StartsWith() {
    assertEquals(1, GET("/users/gist?filter=username:$like:ad&headless=true").content().size());
    assertEquals(1, GET("/users/gist?filter=username:$ilike:Adm&headless=true").content().size());
    assertEquals(
        1, GET("/users/gist?filter=username:startsWith:Admi&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=username:startsWith:bat&headless=true").content().size());
  }

  @Test
  void testFilter_NotStartsWith() {
    assertEquals(2, GET("/users/gist?filter=firstName:!$like:mike&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=firstName:!$ilike:bat&headless=true").content().size());
    assertEquals(
        2, GET("/users/gist?filter=firstName:!startsWith:tic&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=firstName:!startsWith:Firs&headless=true").content().size());
  }

  @Test
  void testFilter_EndsWith() {
    assertEquals(1, GET("/users/gist?filter=firstName:like$:dmin&headless=true").content().size());
    assertEquals(1, GET("/users/gist?filter=firstName:ilike$:in&headless=true").content().size());
    assertEquals(
        1, GET("/users/gist?filter=firstName:endsWith:MIN&headless=true").content().size());
    assertEquals(
        0, GET("/users/gist?filter=firstName:endsWith:bat&headless=true").content().size());
  }

  @Test
  void testFilter_NotEndsWith() {
    assertEquals(2, GET("/users/gist?filter=firstName:!like$:mike&headless=true").content().size());
    assertEquals(2, GET("/users/gist?filter=firstName:!ilike$:bat&headless=true").content().size());
    assertEquals(
        2, GET("/users/gist?filter=firstName:!endsWith:tic&headless=true").content().size());
    assertEquals(
        1, GET("/users/gist?filter=firstName:!endsWith:MiN&headless=true").content().size());
  }

  @Test
  void testFilter_In() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "plus");
    String url =
        "/organisationUnits/{id}/dataSets/gist?filter=name:in:[plus3,plus5]&fields=name&order=name";
    JsonObject gist = GET(url, orgUnitId).content();
    assertEquals(asList("plus3", "plus5"), gist.getArray("dataSets").stringValues());
  }

  @Test
  void testFilter_NotIn() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "item");
    assertEquals(
        asList("item3", "item4"),
        GET(
                "/dataSets/gist?fields=name&filter=name:in:[item3,item4]&headless=true&order=name",
                orgUnitId)
            .content()
            .stringValues());
  }

  @Test
  void testFilter_In_Id() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "plus");
    List<String> dsUids =
        GET("/dataSets/gist?fields=id&filter=name:in:[plus2,plus6]&headless=true")
            .content()
            .stringValues();
    assertEquals(2, dsUids.size());
    String url = "/organisationUnits/{id}/dataSets/gist?filter=id:in:[{ds}]&fields=name&order=name";
    JsonObject gist = GET(url, orgUnitId, String.join(",", dsUids)).content();
    assertEquals(asList("plus2", "plus6"), gist.getArray("dataSets").stringValues());
  }

  @Test
  void testFilter_In_1toMany() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "in");
    String url =
        "/dataSets/gist?filter=organisationUnits.id:in:[{id}]&fields=name&filter=name:startsWith:in&headless=true";
    assertEquals(10, GET(url, orgUnitId).content().size());
  }

  @Test
  void testFilter_NotIn_1toMany() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "notIn");
    String url =
        "/dataSets/gist?filter=organisationUnits.id:!in:[{id}]&fields=name&filter=name:startsWith:notIn&headless=true";
    assertEquals(10, GET(url, "fakeUid").content().size());
  }

  @Test
  void testFilter_Empty_1toMany() {
    createDataSetsForOrganisationUnit(7, orgUnitId, "empty");
    String url =
        "/dataSets/gist?filter=sections:empty&fields=name&filter=name:startsWith:empty&headless=true";
    assertEquals(7, GET(url).content().size());
  }

  @Test
  void testFilter_NotEmpty_1toMany() {
    createDataSetsForOrganisationUnit(8, orgUnitId, "non-empty");
    String url =
        "/dataSets/gist?filter=organisationUnits:!empty&fields=name&filter=name:startsWith:non&headless=true";
    assertEquals(8, GET(url).content().size());
  }

  /**
   * When an property is a collection of identifiable objects one is allowed to use the {@code in}
   * filter on the collection property which has the same effect as if one would use {@code
   * property.id}.
   */
  @Test
  void testFilter_In_1toMany_ShortSyntax() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "plus");
    String url =
        "/dataSets/gist?filter=organisationUnits:in:[{id}]&fields=name&filter=name:startsWith:plus&headless=true";
    assertEquals(10, GET(url, orgUnitId).content().size());
  }

  @Test
  void testFilter_Like_1toMany() {
    createDataSetsForOrganisationUnit(10, orgUnitId, "plus");
    String orgUnitName = GET("/organisationUnits/{id}/name/gist", orgUnitId).content().string();
    String url =
        "/dataSets/gist?filter=organisationUnits.name:like:{name}&fields=name&filter=name:startsWith:plus&headless=true";
    assertEquals(10, GET(url, orgUnitName).content().size());
  }

  @Test
  void testFilter_Eq_1to1_1toMany() {
    // filter asks: does the user's have a user role which
    // name is equal to "Superuser"
    assertEquals(
        getSuperuserUid(),
        GET("/users/{id}/gist?fields=id&filter=userRoles.name:eq:Superuser", getSuperuserUid())
            .content()
            .string());
  }

  @Test
  void testFilter_Gt_1toMany() {
    String fields = "id,username";
    String filter = "created:gt:2021-01-01,userGroups:gt:0";
    JsonObject users =
        GET(
                "/userGroups/{uid}/users/gist?fields={fields}&filter={filter}&headless=true",
                userGroupId,
                fields,
                filter)
            .content()
            .getObject(0);
    assertTrue(users.has("id"));
    assertTrue(users.has("username"));
  }

  @Test
  void testFilter_GroupsOR() {
    createDataSetsForOrganisationUnit(5, orgUnitId, "alpha");
    createDataSetsForOrganisationUnit(5, orgUnitId, "beta");
    createDataSetsForOrganisationUnit(5, orgUnitId, "gamma");
    // both filters in group 2 are combined OR because root junction is
    // implicitly AND
    String url =
        "/dataSets/gist?filter=1:name:endsWith:3&filter=2:name:like:alpha&filter=2:name:like:beta&headless=true&order=name";
    JsonArray matches = GET(url).content();
    assertEquals(2, matches.size());
    assertEquals("alpha3", matches.getObject(0).getString("name").string());
    assertEquals("beta3", matches.getObject(1).getString("name").string());
  }

  @Test
  void testFilter_GroupAND() {
    createDataSetsForOrganisationUnit(5, orgUnitId, "alpha");
    createDataSetsForOrganisationUnit(5, orgUnitId, "beta");
    createDataSetsForOrganisationUnit(5, orgUnitId, "gamma");
    // both filters in group 2 are combined AND because the root junction is
    // set to OR
    String url =
        "/dataSets/gist?filter=1:name:eq:beta1&filter=2:name:like:alpha&filter=2:name:endsWith:4&headless=true&rootJunction=OR&order=name";
    JsonArray matches = GET(url).content();
    assertEquals(2, matches.size());
    assertEquals("alpha4", matches.getObject(0).getString("name").string());
    assertEquals("beta1", matches.getObject(1).getString("name").string());
  }

  @Test
  void testFilter_EqLength() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Mary", "Ringo", "John", "George");
    assertEquals(
        List.of("John", "Mary", "Paul", "set1"),
        GET("/dataSets/gist?fields=name&filter=name:eq:4&headless=true&order=name")
            .content()
            .stringValues());
    assertEquals(
        List.of("Peter", "Ringo"),
        GET("/dataSets/gist?fields=name&filter=name:eq:5&headless=true&order=name")
            .content()
            .stringValues());
    assertEquals(
        List.of("George"),
        GET("/dataSets/gist?fields=name&filter=name:eq:6&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_NeqLength() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Mary", "Ringo", "John", "George");
    assertEquals(
        List.of("George", "Peter", "Ringo"),
        GET("/dataSets/gist?fields=name&filter=name:neq:4&headless=true&order=name")
            .content()
            .stringValues());
    assertEquals(
        List.of("George", "John", "Mary", "Paul", "set1"),
        GET("/dataSets/gist?fields=name&filter=name:neq:5&headless=true&order=name")
            .content()
            .stringValues());
    assertEquals(
        List.of("John", "Mary", "Paul", "Peter", "Ringo", "set1"),
        GET("/dataSets/gist?fields=name&filter=name:neq:6&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_GeLength() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Mary", "Ringo", "John", "George");
    assertEquals(
        List.of("George", "Peter", "Ringo"),
        GET("/dataSets/gist?fields=name&filter=name:ge:5&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_GtLength() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Mary", "Ringo", "John", "George");
    assertEquals(
        List.of("George", "Peter", "Ringo"),
        GET("/dataSets/gist?fields=name&filter=name:gt:4&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_LtLength() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Mary", "Ringo", "John", "George");
    assertEquals(
        List.of("John", "Mary", "Paul", "set1"),
        GET("/dataSets/gist?fields=name&filter=name:lt:5&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_LeLength() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Mary", "Ringo", "John", "George");
    assertEquals(
        List.of("John", "Mary", "Paul", "set1"),
        GET("/dataSets/gist?fields=name&filter=name:le:4&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_Ieq() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Paula", "Ringo", "John", "George", "paul");
    assertEquals(
        List.of("Paul", "paul"),
        GET("/dataSets/gist?fields=name&filter=name:ieq:paul&headless=true&order=name")
            .content()
            .stringValues());
  }

  @Test
  void testFilter_IeqViaILike() {
    createDataSetsForOrganisationUnit(
        orgUnitId, "Peter", "Paul", "Paula", "Ringo", "John", "George", "paul");
    assertEquals(
        List.of("Paul", "paul"),
        GET("/dataSets/gist?fields=name&filter=name:ilike:paul&filter=name:eq:4&headless=true&order=name")
            .content()
            .stringValues());
  }
}
