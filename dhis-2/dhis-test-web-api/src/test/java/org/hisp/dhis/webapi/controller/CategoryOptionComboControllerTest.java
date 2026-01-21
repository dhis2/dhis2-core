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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.api.TestCategoryMetadata;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonCategoryOptionCombo;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryOptionComboControllerTest extends DhisControllerConvenienceTest {

  @Autowired private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    CategoryCombo catComboA = createCategoryCombo('A');
    CategoryCombo catComboB = createCategoryCombo('B');
    CategoryCombo catComboC = createCategoryCombo('C');
    categoryService.addCategoryCombo(catComboA);
    categoryService.addCategoryCombo(catComboB);
    categoryService.addCategoryCombo(catComboC);

    CategoryOption catOptA = createCategoryOption('A');
    CategoryOption catOptB = createCategoryOption('B');
    CategoryOption catOptC = createCategoryOption('C');
    categoryService.addCategoryOption(catOptA);
    categoryService.addCategoryOption(catOptB);
    categoryService.addCategoryOption(catOptC);

    CategoryOptionCombo cocA =
        createCategoryOptionCombo("CatOptCombo A", "CocUid0001", catComboA, catOptA);
    CategoryOptionCombo cocB =
        createCategoryOptionCombo("CatOptCombo B", "CocUid0002", catComboB, catOptB);
    CategoryOptionCombo cocC =
        createCategoryOptionCombo("CatOptCombo C", "CocUid0003", catComboC, catOptC);
    categoryService.addCategoryOptionCombo(cocA);
    categoryService.addCategoryOptionCombo(cocB);
    categoryService.addCategoryOptionCombo(cocC);
  }

  @Test
  @DisplayName(
      "Default CategoryOptionCombo should be present in payload when defaults are INCLUDE by default")
  void getAllCatOptionCombosIncludingDefaultsTest() {
    JsonArray categoryCombos =
        GET("/categoryOptionCombos")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categoryOptionCombos");

    assertEquals(
        Set.of("CatOptCombo C", "CatOptCombo B", "CatOptCombo A", "default"),
        categoryCombos.stream()
            .map(jcoc -> jcoc.as(JsonCategoryOptionCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet()),
        "Returned catOptionCombos equal custom catOptions and default catOption");
  }

  @Test
  @DisplayName("Default CategoryOptionCombo should not be present in payload when EXCLUDE defaults")
  void catOptionCombosExcludingDefaultTest() {
    JsonArray catOptionCombos =
        GET("/categoryOptionCombos?defaults=EXCLUDE")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("categoryOptionCombos");

    Set<String> catOptionComboNames =
        catOptionCombos.stream()
            .map(jcoc -> jcoc.as(JsonCategoryOptionCombo.class))
            .map(JsonIdentifiableObject::getDisplayName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of("CatOptCombo C", "CatOptCombo B", "CatOptCombo A"),
        catOptionComboNames,
        "Returned catOptionCombos include custom catOptions only");

    assertFalse(
        catOptionComboNames.contains("default"), "default catOptionCombo is not in payload");
  }

  @Test
  @DisplayName("A PUT request updating a COC CC should be ignored")
  void updateCategoryOptionComboCatComboRejectedTest() {
    // Given a COC exists with a CC
    TestCategoryMetadata categoryMetadata1 = setupCategoryMetadata("put1");
    String cocUid1 = categoryMetadata1.coc1().getUid();
    TestCategoryMetadata categoryMetadata2 = setupCategoryMetadata("put2");

    // when a COC update is submitted with a different CC
    System.out.println("cocUid1 = " + cocUid1);
    PUT(
            "/categoryOptionCombos/" + cocUid1,
            cocCcUpdated(categoryMetadata2.cc1().getUid(), categoryMetadata1))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // then the updated COC still has it's original CC
    JsonCategoryOptionCombo updated = getCoc(cocUid1);
    assertEquals(
        categoryMetadata1.coc1().getCategoryCombo().getUid(), updated.getCategoryCombo().getId());
  }

  @Test
  @DisplayName("A PUT request updating a COC COs should be ignored")
  void updateCategoryOptionComboCatOptionRejectedTest() {
    // Given a COC exists with COs
    TestCategoryMetadata categoryMetadata1 = setupCategoryMetadata("put3");
    String cocUid1 = categoryMetadata1.coc1().getUid();
    TestCategoryMetadata categoryMetadata2 = setupCategoryMetadata("put4");

    // when a COC update is submitted with different COs
    PUT(
            "/categoryOptionCombos/" + cocUid1,
            cocCoUpdated(categoryMetadata2.co1().getUid(), categoryMetadata1))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // then the updated COC still has it's original COs
    JsonCategoryOptionCombo updated = getCoc(cocUid1);
    assertEquals(
        categoryMetadata1.coc1().getCategoryOptions().stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet()),
        updated.getCategoryOptions().stream()
            .map(JsonIdentifiableObject::getId)
            .collect(Collectors.toSet()));
  }

  @Test
  @DisplayName("A PUT request updating a COC code and ignoreApproval fields should be successful")
  void updateCategoryOptionComboCodeAndApprovalTest() {
    // given a COC exists with a code
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("put5");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);

    assertNull(coc.getCode());
    assertFalse(coc.getIgnoreApproval());

    // when updating the code and ignoreApproval values
    PUT("/categoryOptionCombos/" + cocUid, cocCodeAndApprovalUpdated("new code xyz", true))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // then they should both be updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);

    assertEquals("new code xyz", updated.getCode());
    assertTrue(updated.getIgnoreApproval());
  }

  @Test
  @DisplayName(
      "A PUT request updating a COC code field only should not affect the ignoreApproval value")
  void updateCategoryOptionComboCodeOnlyTest() {
    // given a COC exists with a code
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("put6");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);

    assertNull(coc.getCode());
    assertFalse(coc.getIgnoreApproval());

    // and the ignoreApproval value is true
    PUT("/categoryOptionCombos/" + cocUid, cocApprovalOnlyUpdated(true))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    JsonCategoryOptionCombo cocUpdated = getCoc(cocUid);
    assertTrue(cocUpdated.getIgnoreApproval());

    // when updating the code value only
    PUT("/categoryOptionCombos/" + cocUid, cocCodeOnlyUpdated("new code xyz 1"))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // then the code value should be updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertEquals("new code xyz 1", updated.getCode());

    // and the ignoreApproval field should still be true
    assertTrue(updated.getIgnoreApproval());
  }

  @Test
  @DisplayName(
      "A PUT request updating a COC ignoreApproval field only should not affect the code value")
  void updateCategoryOptionComboIgnoreApprovalOnlyTest() {
    // given a COC exists with a code
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("put7");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);

    assertNull(coc.getCode());
    assertFalse(coc.getIgnoreApproval());

    // and the code value is set
    PUT("/categoryOptionCombos/" + cocUid, cocCodeOnlyUpdated("code set"))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    JsonCategoryOptionCombo cocUpdated = getCoc(cocUid);
    assertEquals("code set", cocUpdated.getCode());

    // when updating the ignoreApproval value only
    PUT("/categoryOptionCombos/" + cocUid, cocApprovalOnlyUpdated(true))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // then the ignoreApproval value should be updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertTrue(updated.getIgnoreApproval());

    // and the code value should not have changed
    assertEquals("code set", updated.getCode());
  }

  @Test
  @DisplayName("A PUT request adding an attribute value to a COC is successful")
  void addAttributeValuesTest() {
    // given a COC exists with no attribute values
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("put-attr");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);
    assertTrue(coc.getAttributeValues().isEmpty());

    // when adding new attribute values
    POST(
            "/attributes",
            """
            {
                "id": "AttrUid0001",
                "name": "Alt name",
                "valueType": "TEXT"
            }
            """)
        .content(HttpStatus.CREATED);

    PUT(
            "/categoryOptionCombos/" + cocUid,
            cocAttributeValuesOnlyUpdated("AttrUid0001", "new alt name"))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // then they are updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertEquals("new alt name", updated.getAttributeValues().get(0).getValue());
  }

  @Test
  @DisplayName("A PUT request updating an attribute value for a COC is successful")
  void updateAttributeValuesTest() {
    // given a COC exists with an attribute value
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("put-attr2");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);
    assertTrue(coc.getAttributeValues().isEmpty());

    String avUid = postAttributeValue("Att Val 1");
    PUT("/categoryOptionCombos/" + cocUid, cocAttributeValuesOnlyUpdated(avUid, "val created"))
        .content(HttpStatus.OK);

    JsonCategoryOptionCombo cocWithAv = getCoc(cocUid);
    assertEquals("val created", cocWithAv.getAttributeValues().get(0).getValue());

    // when updating the attribute values
    PUT("/categoryOptionCombos/" + cocUid, cocAttributeValuesOnlyUpdated(avUid, "val updated"))
        .content(HttpStatus.OK);

    // then they are updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertEquals("val updated", updated.getAttributeValues().get(0).getValue());
  }

  @Test
  @DisplayName("A PATCH request with updatable fields should succeed")
  void patchValidFieldsTest() {
    // given a COC exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("patch-valid");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);
    assertTrue(coc.getAttributeValues().isEmpty());
    assertNull(coc.getCode());
    assertFalse(coc.getIgnoreApproval());

    // when sending a PATCH request to replace attributeValues, code & ignoreApproval
    String avUid = postAttributeValue("Att Val 2");
    PATCH(
            "/categoryOptionCombos/" + cocUid,
            """
            [
                 {
                     "op": "replace",
                     "path": "/code",
                     "value": "new code zzz123"
                 },
                 {
                     "op": "replace",
                     "path": "/ignoreApproval",
                     "value": true
                 },
                 {
                     "op": "replace",
                     "path": "/attributeValues",
                     "value": [
                         {
                             "value": "new alt name 12",
                             "attribute": {
                                 "id": "%s"
                             }
                         }
                     ]
                 }
             ]
            """
                .formatted(avUid))
        .content(HttpStatus.OK);

    // then they are updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertEquals("new code zzz123", updated.getCode());
    assertTrue(updated.getIgnoreApproval());
    assertEquals("new alt name 12", updated.getAttributeValues().get(0).getValue());

    // and other unrelated fields are not affected
    assertEquals(cocUid, updated.getId());
    assertEquals(categoryMetadata.coc1().getName(), updated.getName());
    assertEquals(
        categoryMetadata.coc1().getCategoryOptions().stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet()),
        updated.getCategoryOptions().stream()
            .map(JsonIdentifiableObject::getId)
            .collect(Collectors.toSet()));
  }

  @Test
  @DisplayName(
      "A valid PATCH request excluding ignoreApproval does not change its value from true to false")
  void patchIgnoreApprovalUnchangedTest() {
    // given a COC exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("patch-approval");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);
    assertFalse(coc.getIgnoreApproval());

    // and the ignoreApproval value is true
    PUT("/categoryOptionCombos/" + cocUid, cocApprovalOnlyUpdated(true))
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertTrue(updated.getIgnoreApproval());

    // when sending a PATCH request to replace attributeValues & code
    String avUid = postAttributeValue("Att Val 2z");
    PATCH(
            "/categoryOptionCombos/" + cocUid,
            """
                [
                     {
                         "op": "replace",
                         "path": "/code",
                         "value": "new code zzz123x"
                     },
                     {
                         "op": "replace",
                         "path": "/attributeValues",
                         "value": [
                             {
                                 "value": "new alt name zz12",
                                 "attribute": {
                                     "id": "%s"
                                 }
                             }
                         ]
                     }
                 ]
                """
                .formatted(avUid))
        .content(HttpStatus.OK);

    // then code & attributeValues are updated
    JsonCategoryOptionCombo updated2 = getCoc(cocUid);
    assertEquals("new code zzz123x", updated2.getCode());
    assertEquals("new alt name zz12", updated2.getAttributeValues().get(0).getValue());

    // and ignoreApproval is unchanged
    assertTrue(updated2.getIgnoreApproval());
  }

  @Test
  @DisplayName("A PATCH request with a non-updatable field should fail")
  void patchInvalidFieldsTest() {
    // given a COC exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("patch-invalid");
    String cocUid = categoryMetadata.coc1().getUid();
    JsonCategoryOptionCombo coc = getCoc(cocUid);
    assertTrue(coc.getAttributeValues().isEmpty());
    assertNull(coc.getCode());
    assertFalse(coc.getIgnoreApproval());

    // when sending a PATCH request includes a property that is not updatable (name)
    String avUid = postAttributeValue("Att Val 2");
    JsonMixed response =
        PATCH(
                "/categoryOptionCombos/" + cocUid,
                """
                      [
                           {
                               "op": "replace",
                               "path": "/name",
                               "value": "new code zzz123"
                           },
                           {
                               "op": "replace",
                               "path": "/ignoreApproval",
                               "value": true
                           },
                           {
                               "op": "replace",
                               "path": "/attributeValues",
                               "value": [
                                   {
                                       "value": "new alt name 12",
                                       "attribute": {
                                           "id": "%s"
                                       }
                                   }
                               ]
                           }
                       ]
                      """
                    .formatted(avUid))
            .content(HttpStatus.CONFLICT);

    // then the request is rejected
    assertEquals(
        "Only properties [attributeValues, code, ignoreApproval] are updatable for Category option combo",
        response.getString("message").string());
    assertEquals("E1134", response.getString("errorCode").string());

    // and the properties are not updated
    JsonCategoryOptionCombo updated = getCoc(cocUid);
    assertFalse(updated.getIgnoreApproval());
    assertTrue(updated.getAttributeValues().isEmpty());
  }

  private String postAttributeValue(String name) {
    return POST(
            "/attributes",
            """
            {
                "name": "%s",
                "valueType": "TEXT"
            }
            """
                .formatted(name))
        .content(HttpStatus.CREATED)
        .as(JsonWebMessage.class)
        .getResponse()
        .getString("uid")
        .string();
  }

  private JsonCategoryOptionCombo getCoc(String uid) {
    return GET("/categoryOptionCombos/" + uid)
        .content(HttpStatus.OK)
        .as(JsonCategoryOptionCombo.class);
  }

  private String coc() {
    return """
          {
            "name": "Cat Opt Combo 1",
            "categoryCombo": {
              "id": "bjDvmb4bfuf"
            },
            "categoryOptions": [
              {
                "id": "xYerKDKCefk"
              }
            ]
          }
          """;
  }

  private String cocCcUpdated(String ccId, TestCategoryMetadata categoryMetadata) {
    return """
          {
            "name": "Cat Opt Combo 1",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
          """
        .formatted(ccId, categoryMetadata.co1().getUid(), categoryMetadata.co3().getUid());
  }

  private String cocCoUpdated(String coId, TestCategoryMetadata categoryMetadata) {
    return """
          {
            "name": "Cat Opt Combo 1",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
          """
        .formatted(categoryMetadata.cc1().getUid(), categoryMetadata.co1().getUid(), coId);
  }

  private String cocCodeAndApprovalUpdated(String newCode, boolean ignoreApproval) {
    return """
          {
            "code": "%s",
            "ignoreApproval": %b
          }
          """
        .formatted(newCode, ignoreApproval);
  }

  private String cocCodeOnlyUpdated(String newCode) {
    return """
          {
            "code": "%s"
          }
          """
        .formatted(newCode);
  }

  private String cocApprovalOnlyUpdated(boolean ignoreApproval) {
    return """
          {
             "ignoreApproval": %b
          }
          """
        .formatted(ignoreApproval);
  }

  private String cocAttributeValuesOnlyUpdated(String id, String value) {
    return """
          {
             "attributeValues": [
                 {
                     "value": "%s",
                     "attribute": {
                         "id": "%s"
                     }
                 }
             ]
          }
          """
        .formatted(value, id);
  }
}
