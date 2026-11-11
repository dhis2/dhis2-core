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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests the de-serialisation from JSON and CSV in {@link DataEntryInput}. */
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

  /*
   * JSON: scalar values that are not JSON strings.
   * Up to 2.42 the input was mapped with Jackson which coerced any JSON scalar into a String
   * property. Integrations rely on this, e.g. sending "value": 5 instead of "value": "5".
   */

  @ParameterizedTest
  @ValueSource(strings = {"5", "0", "-12", "3.14", "-0.5", "1e3", "1.5E-2", "12345678901234567890"})
  void testFromJson_ValueAsNumber(String number) {
    assertEquals(number, valueOf("\"value\": " + number).value());
  }

  @Test
  void testFromJson_ValueAsBoolean() {
    assertEquals("true", valueOf("\"value\": true").value());
    assertEquals("false", valueOf("\"value\": false").value());
  }

  @Test
  void testFromJson_ValueAsString() {
    assertEquals("5", valueOf("\"value\": \"5\"").value());
    assertEquals("", valueOf("\"value\": \"\"").value());
    assertEquals(" 5 ", valueOf("\"value\": \" 5 \"").value());
  }

  @Test
  void testFromJson_ValueNullOrAbsent() {
    assertNull(valueOf("\"value\": null").value());
    assertNull(fromJson("{\"dataValues\": [{\"comment\": \"x\"}]}").values().get(0).value());
  }

  @Test
  void testFromJson_CommentAsNumber() {
    assertEquals("42", valueOf("\"comment\": 42").comment());
  }

  @Test
  void testFromJson_PeriodAsNumber() {
    assertEquals("202501", valueOf("\"period\": 202501").period());
    assertEquals("2025", fromJson("{\"period\": 2025, \"dataValues\": []}").period());
  }

  @Test
  void testFromJson_IdsAsNumbers() {
    // numeric codes are common when using idScheme=CODE
    DataEntryValue.Input dv =
        valueOf(
            """
        "dataElement": 101, "orgUnit": 202, "categoryOptionCombo": 303,
        "attributeOptionCombo": 404""");
    assertEquals("101", dv.dataElement());
    assertEquals("202", dv.orgUnit());
    assertEquals("303", dv.categoryOptionCombo());
    assertNull(dv.categoryOptions());
    assertEquals("404", dv.attributeOptionCombo());
  }

  @Test
  void testFromJson_GroupIdsAsNumbers() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "dataSet": 1,
          "orgUnit": 2,
          "period": 202501,
          "attributeOptionCombo": 3,
          "dataValues": []
        }""");
    assertEquals("1", input.dataSet());
    assertEquals("2", input.orgUnit());
    assertEquals("202501", input.period());
    assertEquals("3", input.attributeOptionCombo());
    assertNull(input.attributeOptions());
  }

  @Test
  void testFromJson_CategoryOptionsAsNumbers() {
    DataEntryValue.Input dv = valueOf("\"categoryOptionCombo\": {\"c1234567890\": 7}");
    assertNull(dv.categoryOptionCombo());
    assertEquals(Map.of("c1234567890", "7"), dv.categoryOptions());
  }

  @Test
  void testFromJson_DeletionScopeAsNumbers() {
    DataEntryGroup.Input input =
        fromJson(
            """
        {
          "deletion": {
            "orgUnits": [1, "ou123456789"],
            "periods": [2024, "2025"],
            "elements": [{"dataElement": 5, "categoryOptionCombo": 6}]
          },
          "dataValues": []
        }""");
    DataEntryGroup.Input.Scope scope = input.deletion();
    assertNotNull(scope);
    assertEquals(List.of("1", "ou123456789"), scope.orgUnits());
    assertEquals(List.of("2024", "2025"), scope.periods());
    assertEquals(List.of(new DataEntryGroup.Input.Scope.Element("5", "6", null)), scope.elements());
  }

  @Test
  void testFromJson_DeletionScopeEmpty() {
    DataEntryGroup.Input.Scope scope =
        fromJson("{\"deletion\": {}, \"dataValues\": []}").deletion();
    assertNotNull(scope);
    assertEquals(List.of(), scope.orgUnits());
    assertEquals(List.of(), scope.periods());
    assertEquals(List.of(), scope.elements());
  }

  @Test
  void testFromJson_DeletionScopeElementWithoutDataElement() {
    assertJsonError("{\"deletion\": {\"elements\": [{\"categoryOptionCombo\": \"x\"}]}}");
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "\"true\"", "\"TRUE\"", "1"})
  void testFromJson_BooleansTrue(String bool) {
    DataEntryValue.Input dv = valueOf("\"followUp\": " + bool + ", \"deleted\": " + bool);
    assertEquals(true, dv.followUp());
    assertEquals(true, dv.deleted());
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "\"false\"", "\"FALSE\"", "0"})
  void testFromJson_BooleansFalse(String bool) {
    DataEntryValue.Input dv = valueOf("\"followUp\": " + bool + ", \"deleted\": " + bool);
    assertEquals(false, dv.followUp());
    assertEquals(false, dv.deleted());
  }

  @Test
  void testFromJson_BooleansNullOrAbsent() {
    DataEntryValue.Input dv = valueOf("\"followUp\": null");
    assertNull(dv.followUp());
    assertNull(dv.deleted());
    // like 2.42 (Jackson): an empty string is no value
    dv = valueOf("\"followUp\": \"\", \"deleted\": \"\"");
    assertNull(dv.followUp());
    assertNull(dv.deleted());
  }

  @Test
  void testFromJson_DryRunAsString() {
    ImportOptions options = new ImportOptions();
    fromJson("{\"dryRun\": \"true\", \"dataValues\": []}", options);
    assertTrue(options.isDryRun());
  }

  @Test
  void testFromJson_IdSchemeAsLowerCase() {
    DataEntryGroup.Input input = fromJson("{\"idScheme\": \"code\", \"dataValues\": []}");
    assertEquals(IdProperty.CODE, input.ids().dataElements());
  }

  @Test
  void testFromJson_DataValuesEmptyOrAbsent() {
    assertEquals(List.of(), fromJson("{\"dataValues\": []}").values());
    assertEquals(List.of(), fromJson("{}").values());
  }

  @Test
  void testFromJson_UnknownPropertiesIgnored() {
    DataEntryValue.Input dv =
        valueOf("\"storedBy\": \"admin\", \"created\": \"2025-01-01\", \"foo\": {\"bar\": [1]}");
    assertEquals("5", dv.value());
  }

  @Test
  void testFromJson_FollowupLowerCase() {
    // 2.42 and the 2.43 JSON export use "followup"
    assertEquals(true, valueOf("\"followup\": true").followUp());
    assertEquals(false, valueOf("\"followup\": \"false\"").followUp());
    // the documented name wins when both are present
    assertEquals(true, valueOf("\"followUp\": true, \"followup\": false").followUp());
  }

  @Test
  void testFromJson_DeletionScopeNullEntry() {
    assertJsonError("{\"deletion\": {\"orgUnits\": [null]}}");
    assertJsonError("{\"deletion\": {\"periods\": [\"2024\", null]}}");
  }

  /*
   * JSON: structurally wrong input must be a client error (400), never a 500.
   */

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"a\": 1}", "[]", "[\"5\"]"})
  void testFromJson_ValueAsObjectOrArray(String value) {
    assertJsonError(
        "{\"dataValues\": [{\"dataElement\": \"de123456789\", \"value\": %s}]}".formatted(value));
  }

  @Test
  void testFromJson_FollowUpNotABoolean() {
    assertJsonError("{\"dataValues\": [{\"followUp\": \"maybe\"}]}");
    assertJsonError("{\"dataValues\": [{\"followUp\": 2}]}");
    assertJsonError("{\"dataValues\": [{\"followUp\": \"1\"}]}");
  }

  @Test
  void testFromJson_DataValuesNotAnArray() {
    assertJsonError("{\"dataValues\": {}}");
    assertJsonError("{\"dataValues\": \"x\"}");
  }

  @Test
  void testFromJson_DataValueNotAnObject() {
    assertJsonError("{\"dataValues\": [\"x\"]}");
  }

  @Test
  void testFromJson_PeriodAsObject() {
    assertJsonError("{\"period\": {}, \"dataValues\": []}");
  }

  @Test
  void testFromJson_RootNotAnObject() {
    assertJsonError("[]");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "{",
        "{\"dataValues\": [}",
        "not json",
        "{\"dataValues\": [{\"value\": \"1\"}",
        "{\"dataValues\": [{\"value\": \"1\"}, {\"val",
        "{\"dataValues\": []} xyz",
        "{\"dataValues\": []}}"
      })
  void testFromJson_Malformed(String json) {
    assertJsonError(json);
  }

  /*
   * CSV: the parsing itself is tested in DataEntryImportCSVTest, these test the mapping and errors
   */

  @Test
  void testFromCsv_Basic() {
    List<DataEntryValue.Input> values =
        fromCsv(
            """
        dataElement,period,orgUnit,categoryOptionCombo,attributeOptionCombo,value,comment,followUp,deleted
        de123456789,202501,ou123456789,coc23456789,aoc23456789,5,hello,true,false
        de987654321,202502,ou123456789,,,7,,,
        """);
    assertEquals(2, values.size());
    DataEntryValue.Input dv1 = values.get(0);
    assertEquals("de123456789", dv1.dataElement());
    assertEquals("202501", dv1.period());
    assertEquals("ou123456789", dv1.orgUnit());
    assertEquals("coc23456789", dv1.categoryOptionCombo());
    assertEquals("aoc23456789", dv1.attributeOptionCombo());
    assertEquals("5", dv1.value());
    assertEquals("hello", dv1.comment());
    assertEquals(true, dv1.followUp());
    assertEquals(false, dv1.deleted());
    DataEntryValue.Input dv2 = values.get(1);
    assertEquals("7", dv2.value());
    assertNull(dv2.categoryOptionCombo());
    assertNull(dv2.categoryOptions());
    assertNull(dv2.comment());
    assertNull(dv2.followUp());
  }

  @Test
  void testFromCsv_CategoryOptionsColumns() {
    List<DataEntryValue.Input> values =
        fromCsv(
            """
        dataElement,period,orgUnit,value,c1234567890,c0987654321
        de123456789,202501,ou123456789,1,co123456789,co987654321
        """);
    assertNull(values.get(0).categoryOptionCombo());
    assertEquals(
        Map.of("c1234567890", "co123456789", "c0987654321", "co987654321"),
        values.get(0).categoryOptions());
  }

  @Test
  void testFromCsv_Empty() {
    assertCsvError("");
  }

  @Test
  void testFromCsv_UnterminatedQuote() {
    assertCsvError("dataElement,period,orgUnit,value\nde123456789,202501,ou123456789,\"5\n");
  }

  /** Same as up to 2.42: anything but "true" (any case) is false */
  @Test
  void testFromCsv_BooleanNotABoolean() {
    List<DataEntryValue.Input> values =
        fromCsv("dataElement,value,followUp\nde123456789,5,maybe\n");
    assertEquals(false, values.get(0).followUp());
  }

  /*
   * XML: malformed input must be a client error (400), never a 500.
   */

  @Test
  void testFromXml_Dxf2() throws Exception {
    List<DataEntryGroup.Input> groups =
        DataEntryInput.fromXml(
            toStream(
                """
            <dataValueSet xmlns="http://dhis2.org/schema/dxf/2.0">
              <dataValue dataElement="de123456789" period="202501" orgUnit="ou123456789" value="5"/>
            </dataValueSet>"""),
            new ImportOptions());
    assertEquals("5", groups.get(0).values().get(0).value());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "not xml",
        "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"><dataValue",
        "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\"><dataValue value=\"5\"/>",
        "<dataValueSet><dataValue value=\"5\"></dataValueSet>",
        "<adx xmlns=\"urn:ihe:qrph:adx:2015\"><group orgUnit=\"ou123456789\" period=\"2025-01-01/P1M\">",
        "<foo/>"
      })
  void testFromXml_Malformed(String xml) {
    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () -> DataEntryInput.fromXml(toStream(xml), new ImportOptions()));
    assertEquals(ErrorCode.E8007, ex.getCode());
  }

  private static DataEntryValue.Input valueOf(String properties) {
    // value is 5 unless overridden by the given properties
    String json =
        properties.contains("\"value\"")
            ? "{\"dataValues\": [{%s}]}".formatted(properties)
            : "{\"dataValues\": [{\"value\": \"5\", %s}]}".formatted(properties);
    List<DataEntryValue.Input> values = fromJson(json).values();
    assertEquals(1, values.size());
    return values.get(0);
  }

  private static void assertJsonError(String json) {
    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () -> DataEntryInput.fromJson(toStream(json), new ImportOptions()));
    assertEquals(ErrorCode.E8036, ex.getCode());
  }

  private static void assertCsvError(String csv) {
    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () -> DataEntryInput.fromCsv(toStream(csv), new ImportOptions()));
    assertEquals(ErrorCode.E8037, ex.getCode());
  }

  private static List<DataEntryValue.Input> fromCsv(String csv) {
    try (InputStream in = toStream(csv)) {
      return DataEntryInput.fromCsv(in, new ImportOptions()).get(0).values();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } catch (BadRequestException ex) {
      throw new AssertionError(ex.getMessage(), ex);
    }
  }

  private static DataEntryGroup.Input fromJson(@Language("json") String json) {
    return fromJson(json, new ImportOptions());
  }

  private static DataEntryGroup.Input fromJson(String json, ImportOptions options) {
    try (InputStream in = toStream(json)) {
      return DataEntryInput.fromJson(in, options).get(0);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } catch (BadRequestException ex) {
      throw new AssertionError(ex.getMessage(), ex);
    }
  }

  private static InputStream toStream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }
}
