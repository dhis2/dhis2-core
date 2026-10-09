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

/** Exercises the metadata import and merge path of the JPA-mapped data element group set. */
@Transactional
class DataElementGroupSetControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String GROUPS =
      "'dataElementGroups':["
          + "{'id':'DeGroupUi01','name':'DEG1','shortName':'DEG1'},"
          + "{'id':'DeGroupUi02','name':'DEG2','shortName':'DEG2'},"
          + "{'id':'DeGroupUi03','name':'DEG3','shortName':'DEG3'}]";

  private static final String GROUP_SET_UID = "DeGrpSetU01";

  @Test
  void testImportKeepsMemberOrder() {
    importMetadata(groupSet("DeGroupUi03", "DeGroupUi01", "DeGroupUi02"));

    assertEquals(List.of("DeGroupUi03", "DeGroupUi01", "DeGroupUi02"), memberUids());
  }

  @Test
  void testReimportReordersAndRemovesMembers() {
    importMetadata(groupSet("DeGroupUi01", "DeGroupUi02", "DeGroupUi03"));
    importMetadata(groupSet("DeGroupUi03", "DeGroupUi01"));

    assertEquals(List.of("DeGroupUi03", "DeGroupUi01"), memberUids());
  }

  @Test
  void testPutOfExportedObjectRoundTrips() {
    importMetadata(groupSet("DeGroupUi02", "DeGroupUi01"));
    String exported = GET("/dataElementGroupSets/" + GROUP_SET_UID).content(HttpStatus.OK).toJson();

    assertStatus(HttpStatus.OK, PUT("/dataElementGroupSets/" + GROUP_SET_UID, exported));

    assertEquals(List.of("DeGroupUi02", "DeGroupUi01"), memberUids());
  }

  @Test
  void testExposesItemsAndGroupSetsOnBothSides() {
    importMetadata(groupSet("DeGroupUi01"));

    JsonObject groupSet =
        GET("/dataElementGroupSets/" + GROUP_SET_UID + "?fields=items[id],dimensionType")
            .content(HttpStatus.OK);
    assertEquals("DATA_ELEMENT_GROUP_SET", groupSet.getString("dimensionType").string());
    assertEquals("DeGroupUi01", groupSet.getArray("items").getObject(0).getString("id").string());

    JsonArray groupSets =
        GET("/dataElementGroups/DeGroupUi01?fields=groupSets[id]")
            .content(HttpStatus.OK)
            .getArray("groupSets");
    assertEquals(GROUP_SET_UID, groupSets.getObject(0).getString("id").string());
  }

  /**
   * Same property set as before the migration, minus {@code formName}/{@code displayFormName} which
   * have no column in {@code dataelementgroupset}.
   */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/dataElementGroupSet").content(HttpStatus.OK);
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
                "compulsory",
                "created",
                "createdBy",
                "dataDimension",
                "dataDimensionType",
                "dataElementGroup",
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
    return ("{%s,'dataElementGroupSets':[{'id':'%s','name':'DEGS1','shortName':'DEGS1',"
            + "'dataDimension':true,'dataElementGroups':[%s]}]}")
        .formatted(GROUPS, GROUP_SET_UID, String.join(",", refs));
  }

  private List<String> memberUids() {
    JsonArray groups =
        GET("/dataElementGroupSets/" + GROUP_SET_UID + "?fields=dataElementGroups[id]")
            .content(HttpStatus.OK)
            .getArray("dataElementGroups");
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
