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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the metadata import and merge path of the JPA-mapped option group set. */
@Transactional
class OptionGroupSetControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String OPTION_SET =
      "'optionSets':[{'id':'OptSetUi001','name':'OS1','valueType':'TEXT'}]";

  private static final String GROUPS =
      "'optionGroups':["
          + "{'id':'OptGroupU01','name':'OG1','shortName':'OG1'},"
          + "{'id':'OptGroupU02','name':'OG2','shortName':'OG2'},"
          + "{'id':'OptGroupU03','name':'OG3','shortName':'OG3'}]";

  private static final String GROUP_SET_UID = "OptGrpSetU1";

  @Test
  void testImportKeepsMemberOrder() {
    importMetadata(groupSet("OptGroupU03", "OptGroupU01", "OptGroupU02"));

    assertEquals(List.of("OptGroupU03", "OptGroupU01", "OptGroupU02"), memberUids());
  }

  @Test
  void testReimportReordersAndRemovesMembers() {
    importMetadata(groupSet("OptGroupU01", "OptGroupU02", "OptGroupU03"));
    importMetadata(groupSet("OptGroupU03", "OptGroupU01"));

    assertEquals(List.of("OptGroupU03", "OptGroupU01"), memberUids());
  }

  @Test
  void testPutOfExportedObjectRoundTrips() {
    importMetadata(groupSet("OptGroupU02", "OptGroupU01"));
    String exported = GET("/optionGroupSets/" + GROUP_SET_UID).content(HttpStatus.OK).toJson();

    assertStatus(HttpStatus.OK, PUT("/optionGroupSets/" + GROUP_SET_UID, exported));

    assertEquals(List.of("OptGroupU02", "OptGroupU01"), memberUids());
  }

  @Test
  void testExposesItemsDimensionTypeAndOptionSet() {
    importMetadata(groupSet("OptGroupU01"));

    JsonObject groupSet =
        GET("/optionGroupSets/" + GROUP_SET_UID + "?fields=items[id],dimensionType,optionSet[id]")
            .content(HttpStatus.OK);
    assertEquals("OPTION_GROUP_SET", groupSet.getString("dimensionType").string());
    assertEquals("OptGroupU01", groupSet.getArray("items").getObject(0).getString("id").string());
    assertEquals("OptSetUi001", groupSet.getObject("optionSet").getString("id").string());
  }

  /**
   * Same property set as before the migration, minus {@code formName}/{@code displayFormName} which
   * have no column in {@code optiongroupset}.
   */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/optionGroupSet").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    JsonObject description = null;
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      if ("description".equals(name)) {
        description = property;
      }
    }

    Set<String> expected =
        new TreeSet<>(
            Set.of(
                "access",
                "aggregationType",
                "allItems",
                "attributeValues",
                "code",
                "created",
                "createdBy",
                "dataDimension",
                "dataDimensionType",
                "description",
                "dimension",
                "dimensionItemKeywords",
                "dimensionType",
                "displayDescription",
                "displayName",
                "displayShortName",
                "filter",
                "href",
                "id",
                "item",
                "lastUpdated",
                "lastUpdatedBy",
                "legendSet",
                "name",
                "optionGroup",
                "optionSet",
                "program",
                "programStage",
                "repetition",
                "sharing",
                "shortName",
                "translation",
                "user",
                "valueType"));
    assertEquals(expected, names);
    assertEquals(Integer.MAX_VALUE, description.getNumber("length").intValue());
  }

  private static String groupSet(String... memberUids) {
    List<String> refs = new ArrayList<>();
    for (String uid : memberUids) {
      refs.add("{'id':'" + uid + "'}");
    }
    return ("{%s,%s,'optionGroupSets':[{'id':'%s','name':'OGS1','dataDimension':true,"
            + "'optionSet':{'id':'OptSetUi001'},'optionGroups':[%s]}]}")
        .formatted(OPTION_SET, GROUPS, GROUP_SET_UID, String.join(",", refs));
  }

  private List<String> memberUids() {
    JsonArray groups =
        GET("/optionGroupSets/" + GROUP_SET_UID + "?fields=optionGroups[id]")
            .content(HttpStatus.OK)
            .getArray("optionGroups");
    List<String> uids = new ArrayList<>();
    for (int i = 0; i < groups.size(); i++) {
      uids.add(groups.getObject(i).getString("id").string());
    }
    return uids;
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }
}
