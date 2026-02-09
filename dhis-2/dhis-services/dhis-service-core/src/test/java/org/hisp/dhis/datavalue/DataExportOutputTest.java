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

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonMixed;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the JSON serialisation in {@link DataExportOutput}.
 *
 * @author Jan Bernitt
 */
class DataExportOutputTest {

  @Test
  void testToJson_Group() {
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(),
            "ds123456789",
            "2026",
            "ou123456789",
            "aoc23456789",
            null,
            null,
            Stream.empty());
    assertJson(
        """
      {
        "dataSet":"ds123456789",
        "period":"2026",
        "orgUnit":"ou123456789",
        "attributeOptionCombo":"aoc23456789",
        "dataValues":[]
      }""",
        group);
  }

  @Test
  void testToJson_Schema() {
    DataExportGroup.Ids ids =
        new DataExportGroup.Ids(
            IdProperty.CODE,
            IdProperty.CODE,
            IdProperty.CODE,
            IdProperty.CODE,
            IdProperty.CODE,
            IdProperty.CODE,
            IdProperty.CODE);
    DataExportGroup.Output group =
        new DataExportGroup.Output(ids, null, null, null, null, null, null, Stream.empty());
    assertJson(
        """
      {
        "dataSetIdScheme":"CODE",
        "dataElementIdScheme":"CODE",
        "orgUnitIdScheme":"CODE",
        "categoryOptionComboIdScheme":"CODE",
        "attributeOptionComboIdScheme":"CODE",
        "categoryIdScheme":"CODE",
        "categoryOptionIdScheme":"CODE",
        "dataValues":[]
      }""",
        group);
  }

  @Test
  void testToJson_Deletion() {
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(),
            null,
            null,
            null,
            null,
            null,
            new DataExportGroup.Scope(
                List.of("ou123456789", "ou987654321"),
                List.of("2022", "2023", "2024"),
                List.of(
                    new DataExportGroup.Scope.Element("de123456789", "coc23456789", "aoc23456789"),
                    new DataExportGroup.Scope.Element("de222222222", "coc22222222", null),
                    new DataExportGroup.Scope.Element("de333333333", null, null))),
            Stream.empty());
    assertJson(
        """
      {
        "deletion":{
          "orgUnits":["ou123456789","ou987654321"],
          "periods":["2022","2023","2024"],
          "elements":[
            {"dataElement":"de123456789","categoryOptionCombo":"coc23456789","attributeOptionCombo":"aoc23456789"},
            {"dataElement":"de222222222","categoryOptionCombo":"coc22222222"},
            {"dataElement":"de333333333"}]
          },
        "dataValues":[]
      }""",
        group);
  }

  @Test
  void testToJson_Values() {
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(),
            null,
            null,
            null,
            null,
            null,
            null,
            Stream.of(
                new DataExportValue.Output(
                    "de123456789",
                    "2020",
                    "ou123456789",
                    "coc23456789",
                    null,
                    "aoc23456789",
                    ValueType.TEXT,
                    "value",
                    "comment",
                    true,
                    "storedBy",
                    parseDate("2020-01-01"),
                    parseDate("2026-01-01"),
                    true)));
    assertJson(
        """
      {
        "dataValues":[
          {
            "dataElement":"de123456789",
            "period":"2020",
            "orgUnit":"ou123456789",
            "categoryOptionCombo":"coc23456789",
            "attributeOptionCombo":"aoc23456789",
            "value":"value",
            "storedBy":"storedBy",
            "created":"2019-12-31T23:00:00.000+0000",
            "lastUpdated":"2025-12-31T23:00:00.000+0000",
            "comment":"comment",
            "followup":true,
            "deleted":true
          }
        ]
      }""",
        group);
  }

  private void assertJson(@Language("JSON") String expected, DataExportGroup.Output actual) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportOutput.toJson(actual, out);
    String json = out.toString();
    if (!JsonMixed.of(expected).identicalTo(JsonMixed.of(json)))
      assertEquals(expected, json, "should be semantically identical");
  }
}
