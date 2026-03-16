/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/** Unit tests the de-serialisation from JSON in {@link DataEntryInput}. */
class DataEntryInputTest {

  @Test
  void testFromJson_Group() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "dataSet": "ds123456789",
          "completeDate": "2026-01-22",
          "orgUnit": "ou123456789",
          "period": "2026",
          "attributeOptionCombo": "aoc23456789",
          "dataValues": []
        }""");
    assertEquals("ds123456789", input.dataSet());
    assertEquals("2026-01-22", input.completionDate());
    assertEquals("ou123456789", input.orgUnit());
    assertEquals("2026", input.period());
    assertEquals("aoc23456789", input.attributeOptionCombo());
  }

  @Test
  void testFromJson_DeletionScope() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "deletion": {
            "orgUnits": [ "ou123456789", "ou987654321"],
            "periods": ["2022", "2023", "2024"],
            "elements": [
              {"dataElement": "de123456789" },
              {"dataElement": "de222222222", "categoryOptionCombo": "coc23456789"},
              {"dataElement": "de333333333", "categoryOptionCombo": "coc33333333", "attributeOptionCombo": "aoc23456789"}
            ]
          },
          "dataValues": []
        }""");
    DataEntryGroup.Input.Scope actual = input.deletion();
    assertNotNull(actual);
    assertEquals(List.of("ou123456789", "ou987654321"), actual.orgUnits());
    assertEquals(List.of("2022", "2023", "2024"), actual.periods());
    assertEquals(
        List.of(
            new DataEntryGroup.Input.Scope.Element("de123456789", null, null),
            new DataEntryGroup.Input.Scope.Element("de222222222", "coc23456789", null),
            new DataEntryGroup.Input.Scope.Element("de333333333", "coc33333333", "aoc23456789")),
        actual.elements());
  }

  @Test
  void testFromJson_DataValues() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "dataValues": [
            {
              "dataElement": "de123456789",
              "orgUnit": "ou123456789",
              "categoryOptionCombo": "coc23456789",
              "attributeOptionCombo": "aoc23456789",
              "period": "2021",
              "value": "value",
              "comment": "comment",
              "followUp": true,
              "deleted": true
            },
            {
              "dataElement": "de123456789",
              "orgUnit": "ou123456789",
              "categoryOptionCombo": {"c1234567890": "co123456789"},
              "attributeOptionCombo": "aoc23456789",
              "period": "2021",
              "value": "value",
              "comment": "comment",
              "followUp": true,
              "deleted": true
            }
          ]
        }""");
    assertEquals(2, input.values().size());
    DataEntryValue.Input dv1 = input.values().get(0);
    assertEquals("de123456789", dv1.dataElement());
    assertEquals("ou123456789", dv1.orgUnit());
    assertEquals("coc23456789", dv1.categoryOptionCombo());
    assertEquals("aoc23456789", dv1.attributeOptionCombo());
    assertEquals("2021", dv1.period());
    assertEquals("value", dv1.value());
    assertEquals("comment", dv1.comment());
    assertEquals(true, dv1.followUp());
    assertEquals(true, dv1.deleted());

    DataEntryValue.Input dv2 = input.values().get(1);
    assertEquals(Map.of("c1234567890", "co123456789"), dv2.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemes() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "idScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.CODE, ids.dataSets());
    assertEquals(IdProperty.CODE, ids.dataElements());
    assertEquals(IdProperty.CODE, ids.orgUnits());
    assertEquals(IdProperty.CODE, ids.categoryOptionCombos());
    assertEquals(IdProperty.CODE, ids.attributeOptionCombos());
    assertEquals(IdProperty.CODE, ids.categories());
    assertEquals(IdProperty.CODE, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesDataSet() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "dataSetIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.CODE, ids.dataSets());
    assertEquals(IdProperty.UID, ids.dataElements());
    assertEquals(IdProperty.UID, ids.orgUnits());
    assertEquals(IdProperty.UID, ids.categoryOptionCombos());
    assertEquals(IdProperty.UID, ids.attributeOptionCombos());
    assertEquals(IdProperty.UID, ids.categories());
    assertEquals(IdProperty.UID, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesDataElements() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "dataElementIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.UID, ids.dataSets());
    assertEquals(IdProperty.CODE, ids.dataElements());
    assertEquals(IdProperty.UID, ids.orgUnits());
    assertEquals(IdProperty.UID, ids.categoryOptionCombos());
    assertEquals(IdProperty.UID, ids.attributeOptionCombos());
    assertEquals(IdProperty.UID, ids.categories());
    assertEquals(IdProperty.UID, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesOrgUnits() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "orgUnitIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.UID, ids.dataSets());
    assertEquals(IdProperty.UID, ids.dataElements());
    assertEquals(IdProperty.CODE, ids.orgUnits());
    assertEquals(IdProperty.UID, ids.categoryOptionCombos());
    assertEquals(IdProperty.UID, ids.attributeOptionCombos());
    assertEquals(IdProperty.UID, ids.categories());
    assertEquals(IdProperty.UID, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesCategoryOptionCombos() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "categoryOptionComboIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.UID, ids.dataSets());
    assertEquals(IdProperty.UID, ids.dataElements());
    assertEquals(IdProperty.UID, ids.orgUnits());
    assertEquals(IdProperty.CODE, ids.categoryOptionCombos());
    assertEquals(IdProperty.UID, ids.attributeOptionCombos());
    assertEquals(IdProperty.UID, ids.categories());
    assertEquals(IdProperty.UID, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesAttributeOptionCombos() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "attributeOptionComboIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.UID, ids.dataSets());
    assertEquals(IdProperty.UID, ids.dataElements());
    assertEquals(IdProperty.UID, ids.orgUnits());
    assertEquals(IdProperty.UID, ids.categoryOptionCombos());
    assertEquals(IdProperty.CODE, ids.attributeOptionCombos());
    assertEquals(IdProperty.UID, ids.categories());
    assertEquals(IdProperty.UID, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesCategoryIdScheme() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "categoryIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.UID, ids.dataSets());
    assertEquals(IdProperty.UID, ids.dataElements());
    assertEquals(IdProperty.UID, ids.orgUnits());
    assertEquals(IdProperty.UID, ids.categoryOptionCombos());
    assertEquals(IdProperty.UID, ids.attributeOptionCombos());
    assertEquals(IdProperty.CODE, ids.categories());
    assertEquals(IdProperty.UID, ids.categoryOptions());
  }

  @Test
  void testFromJson_IdSchemesCategoryOptionIdScheme() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "categoryOptionIdScheme": "CODE",
          "dataValues": []
        }""");
    DataEntryGroup.Ids ids = input.ids();
    assertNotNull(ids);
    assertEquals(IdProperty.UID, ids.dataSets());
    assertEquals(IdProperty.UID, ids.dataElements());
    assertEquals(IdProperty.UID, ids.orgUnits());
    assertEquals(IdProperty.UID, ids.categoryOptionCombos());
    assertEquals(IdProperty.UID, ids.attributeOptionCombos());
    assertEquals(IdProperty.UID, ids.categories());
    assertEquals(IdProperty.CODE, ids.categoryOptions());
  }

  private static DataEntryGroup.Input fromJson(@Language("json") String json) {
    try (InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
      return DataEntryInput.fromJson(in, new ImportOptions()).get(0);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
