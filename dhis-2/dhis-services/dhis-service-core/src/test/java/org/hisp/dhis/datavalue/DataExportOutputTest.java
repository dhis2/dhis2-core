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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonMixed;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the serialisation in {@link DataExportOutput}.
 *
 * <p>JSON, CSV and XML outputs are each covered independently. JSON tests use {@link
 * JsonMixed#identicalTo} so that key order and formatting don’t affect the assertion — only the
 * parsed shape. CSV and XML tests compare against explicit expected output because the formatting
 * rules (quoting, line endings, attribute presence) are the contract being verified.
 *
 * <p>Note on CSV encoding: {@link DataExportOutput#toCsv} uses UTF-8 via {@link
 * org.hisp.dhis.jsontree.Appender#ofUTF8}.
 *
 * @author Jan Bernitt
 */
class DataExportOutputTest {

  // ====== toJson ======

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
  void testToJson_PartialSchema_OnlyNonDefaultWritten() {
    // Mix of UID (default, omitted) and CODE (non-default, written) — only the non-default
    // ID schemes should appear in the output
    DataExportGroup.Ids ids =
        new DataExportGroup.Ids(
            IdProperty.UID,
            IdProperty.CODE,
            IdProperty.UID,
            IdProperty.CODE,
            IdProperty.UID,
            IdProperty.CODE,
            IdProperty.UID);
    DataExportGroup.Output group =
        new DataExportGroup.Output(ids, null, null, null, null, null, null, Stream.empty());
    assertJson(
        """
      {
        "dataElementIdScheme":"CODE",
        "categoryOptionComboIdScheme":"CODE",
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
                    "2020-01-01",
                    "2026-01-01",
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
            "created":"2020-01-01",
            "lastUpdated":"2026-01-01",
            "comment":"comment",
            "followup":true,
            "deleted":true
          }
        ]
      }""",
        group);
  }

  @Test
  void testToJson_GroupWithAttributeOptionsFallback() {
    // When attributeOptionCombo is null but attributeOptions is non-null, the map is written
    // as an object instead
    Map<String, String> attributeOptions = new LinkedHashMap<>();
    attributeOptions.put("aoc1", "value1");
    attributeOptions.put("aoc2", "value2");
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(),
            null,
            null,
            null,
            null, // attributeOptionCombo null
            attributeOptions,
            null,
            Stream.empty());
    assertJson(
        """
      {
        "attributeOptionCombo":{"aoc1":"value1","aoc2":"value2"},
        "dataValues":[]
      }""",
        group);
  }

  @Test
  void testToJson_ValueWithCategoryOptionsFallback() {
    // When categoryOptionCombo is null but categoryOptions is non-null, the map is written
    // as an object instead
    Map<String, String> categoryOptions = new LinkedHashMap<>();
    categoryOptions.put("coc1", "value1");
    categoryOptions.put("coc2", "value2");
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020",
            "ou1",
            null, // categoryOptionCombo null
            categoryOptions,
            "aoc1",
            ValueType.NUMBER,
            "42",
            null,
            null,
            null,
            null,
            null,
            false);
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(), null, null, null, null, null, null, Stream.of(dv));
    assertJson(
        """
      {
        "dataValues":[{
          "dataElement":"de1",
          "period":"2020",
          "orgUnit":"ou1",
          "categoryOptionCombo":{"coc1":"value1","coc2":"value2"},
          "attributeOptionCombo":"aoc1",
          "value":"42"
        }]
      }""",
        group);
  }

  @Test
  void testToJson_ValueFollowupFalse_StillPresent() {
    // followup is a Boolean; false is written, only null is omitted
    DataExportValue.Output dv = simpleValue(false, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String json = toJson(group);
    assertTrue(json.contains("\"followup\":false"), json);
  }

  @Test
  void testToJson_ValueFollowupNull_Omitted() {
    DataExportValue.Output dv = simpleValue(null, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String json = toJson(group);
    assertFalse(json.contains("\"followup\""), json);
  }

  @Test
  void testToJson_ValueDeletedFalse_Omitted() {
    // deleted is only written when true
    DataExportValue.Output dv = simpleValue(false, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String json = toJson(group);
    assertFalse(json.contains("\"deleted\""), json);
  }

  @Test
  void testToJson_ValueNullComment_Omitted() {
    // excludeNullMembers=true — null comment is omitted, not written as `"comment":null`
    DataExportValue.Output dv = simpleValue(false, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String json = toJson(group);
    assertFalse(json.contains("\"comment\""), json);
  }

  @Test
  void testToJson_MultipleValues() {
    DataExportValue.Output a = simpleValue(false, false);
    DataExportValue.Output b = simpleValue(false, false);
    DataExportGroup.Output group = groupWith(Stream.of(a, b));
    String json = toJson(group);
    // two entries in the dataValues array
    assertTrue(json.contains("\"dataValues\":[{"), json);
    int firstIndex = json.indexOf('{');
    int secondIndex = json.indexOf('{', firstIndex + 1);
    int thirdIndex = json.indexOf('{', secondIndex + 1);
    assertTrue(thirdIndex > secondIndex, "should contain two objects in dataValues: " + json);
  }

  // ====== toCsv ======

  @Test
  void testToCsv_HeaderOnly() {
    DataExportGroup.Output group = groupWith(Stream.empty());
    assertEquals(
        "dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,"
            + "value,storedby,lastupdated,comment,followup,deleted\n",
        toCsv(group));
  }

  @Test
  void testToCsv_SingleValue_AllFields() {
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020",
            "ou1",
            "coc1",
            null,
            "aoc1",
            ValueType.NUMBER,
            "42",
            "note",
            true,
            "admin",
            "2024-01-01",
            "2024-01-02",
            false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    assertEquals(
        """
        dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,value,storedby,lastupdated,comment,followup,deleted
        de1,2020,ou1,coc1,aoc1,42,admin,2024-01-02,note,true,false
        """,
        toCsv(group));
  }

  @Test
  void testToCsv_NullValueFieldsUseGroupDefaults() {
    // dv.period(), dv.orgUnit(), dv.attributeOptionCombo() null -> use group-level values
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            null, // period -> groupPeriod
            null, // orgUnit -> groupOrgUnit
            "coc1",
            null,
            null, // attributeOptionCombo -> groupAoc
            ValueType.NUMBER,
            "42",
            null,
            null,
            null,
            null,
            null,
            false);
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(),
            null,
            "2024", // group period
            "ouGroup", // group orgUnit
            "aocGroup", // group attributeOptionCombo
            null,
            null,
            Stream.of(dv));
    assertEquals(
        """
        dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,value,storedby,lastupdated,comment,followup,deleted
        de1,2024,ouGroup,coc1,aocGroup,42,,,,,false
        """,
        toCsv(group));
  }

  @Test
  void testToCsv_ValueFieldsTakePriorityOverGroupDefaults() {
    // Non-null value fields win over group-level defaults
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020", // takes priority over group period
            "ou2", // takes priority over group orgUnit
            "coc1",
            null,
            "aoc2", // takes priority over group aoc
            ValueType.NUMBER,
            "42",
            null,
            null,
            null,
            null,
            null,
            false);
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(),
            null,
            "2024",
            "ouGroup",
            "aocGroup",
            null,
            null,
            Stream.of(dv));
    assertEquals(
        """
        dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,value,storedby,lastupdated,comment,followup,deleted
        de1,2020,ou2,coc1,aoc2,42,,,,,false
        """,
        toCsv(group));
  }

  @Test
  void testToCsv_NullFollowUp_EmptyCell() {
    DataExportValue.Output dv = simpleValue(null, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String csv = toCsv(group);
    String[] rows = csv.split("\n");
    assertEquals(2, rows.length, csv);
    String[] cols = rows[1].split(",", -1);
    assertEquals("", cols[9], "followup cell should be empty when null: " + csv);
  }

  // ====== toXml(DataExportGroup.Output, OutputStream) — single group ======

  @Test
  void testToXml_SingleGroup_GroupAttributes() {
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(), "ds1", "2020", "ou1", "aoc1", null, null, Stream.empty());
    String xml = toXml(group);
    assertTrue(xml.contains("<dataValueSet"), xml);
    assertTrue(xml.contains("xmlns=\"http://dhis2.org/schema/dxf/2.0\""), xml);
    assertTrue(xml.contains("dataSet=\"ds1\""), xml);
    assertTrue(xml.contains("orgUnit=\"ou1\""), xml);
    assertTrue(xml.contains("attributeOptionCombo=\"aoc1\""), xml);
    // period is ADX-formatted by toAdxPeriod — verify the attribute exists without pinning format
    assertTrue(xml.contains("period="), xml);
  }

  @Test
  void testToXml_SingleGroup_IdSchemes() {
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
    String xml = toXml(group);
    assertTrue(xml.contains("dataSetIdScheme=\"CODE\""), xml);
    assertTrue(xml.contains("dataElementIdScheme=\"CODE\""), xml);
    assertTrue(xml.contains("orgUnitIdScheme=\"CODE\""), xml);
  }

  @Test
  void testToXml_SingleGroup_DataValueAttributes() {
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020",
            "ou1",
            "coc1",
            null,
            "aoc1",
            ValueType.NUMBER,
            "42",
            "note",
            true,
            "admin",
            "2024-01-01",
            "2024-01-02",
            true);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String xml = toXml(group);
    assertTrue(xml.contains("<dataValue "), xml);
    assertTrue(xml.contains("dataElement=\"de1\""), xml);
    assertTrue(xml.contains("categoryOptionCombo=\"coc1\""), xml);
    assertTrue(xml.contains("value=\"42\""), xml);
    assertTrue(xml.contains("storedBy=\"admin\""), xml);
    assertTrue(xml.contains("comment=\"note\""), xml);
    assertTrue(xml.contains("followUp=\"true\""), xml);
    assertTrue(xml.contains("deleted=\"true\""), xml);
  }

  @Test
  void testToXml_SingleGroup_FollowUpFalse_OmitsAttribute() {
    // followUp only written when Boolean.TRUE.equals(dv.followUp())
    DataExportValue.Output dv = simpleValue(false, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String xml = toXml(group);
    assertFalse(xml.contains("followUp="), xml);
  }

  @Test
  void testToXml_SingleGroup_DeletedFalse_OmitsAttribute() {
    DataExportValue.Output dv = simpleValue(false, false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String xml = toXml(group);
    assertFalse(xml.contains("deleted="), xml);
  }

  @Test
  void testToXml_SingleGroup_TextValueWritesValueDirectly() {
    // Single-group XML does not branch on type — value is written as-is
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020",
            "ou1",
            "coc1",
            null,
            "aoc1",
            ValueType.TEXT,
            "text value",
            null,
            null,
            null,
            null,
            null,
            false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String xml = toXml(group);
    assertTrue(xml.contains("value=\"text value\""), xml);
    assertFalse(xml.contains("<annotation>"), xml);
  }

  // ====== toXml(Stream<DataExportGroup.Output>, OutputStream) — ADX ======

  @Test
  void testToXmlAdx_SingleGroup() {
    DataExportGroup.Output group =
        new DataExportGroup.Output(
            new DataExportGroup.Ids(), "ds1", "2020", "ou1", "aoc1", null, null, Stream.empty());
    String xml = toXmlAdx(Stream.of(group));
    assertTrue(xml.contains("<adx"), xml);
    assertTrue(xml.contains("xmlns=\"urn:ihe:qrph:adx:2015\""), xml);
    assertTrue(xml.contains("<group"), xml);
    assertTrue(xml.contains("dataSet=\"ds1\""), xml);
  }

  @Test
  void testToXmlAdx_NumericValue_WritesValueDirectly() {
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020",
            "ou1",
            "coc1",
            null,
            "aoc1",
            ValueType.NUMBER,
            "42",
            null,
            null,
            null,
            null,
            null,
            false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String xml = toXmlAdx(Stream.of(group));
    assertTrue(xml.contains("value=\"42\""), xml);
    assertFalse(xml.contains("<annotation>"), xml);
  }

  @Test
  void testToXmlAdx_NonNumericValue_WritesZeroAndAnnotation() {
    // ADX requires numeric values in the value attribute; non-numeric goes to <annotation>
    DataExportValue.Output dv =
        new DataExportValue.Output(
            "de1",
            "2020",
            "ou1",
            "coc1",
            null,
            "aoc1",
            ValueType.TEXT,
            "text value",
            null,
            null,
            null,
            null,
            null,
            false);
    DataExportGroup.Output group = groupWith(Stream.of(dv));
    String xml = toXmlAdx(Stream.of(group));
    assertTrue(xml.contains("value=\"0\""), xml);
    assertTrue(xml.contains("<annotation>text value</annotation>"), xml);
  }

  @Test
  void testToXmlAdx_MultipleGroups() {
    DataExportGroup.Output a = groupWith("ds1", Stream.empty());
    DataExportGroup.Output b = groupWith("ds2", Stream.empty());
    String xml = toXmlAdx(Stream.of(a, b));
    assertTrue(xml.contains("dataSet=\"ds1\""), xml);
    assertTrue(xml.contains("dataSet=\"ds2\""), xml);
    int firstGroup = xml.indexOf("<group");
    int secondGroup = xml.indexOf("<group", firstGroup + 1);
    assertTrue(secondGroup > firstGroup, "should contain two group elements: " + xml);
  }

  // ====== helpers ======

  private static DataExportValue.Output simpleValue(Boolean followUp, boolean deleted) {
    return new DataExportValue.Output(
        "de1",
        "2020",
        "ou1",
        "coc1",
        null,
        "aoc1",
        ValueType.NUMBER,
        "42",
        null,
        followUp,
        "admin",
        "2024-01-01",
        "2024-01-02",
        deleted);
  }

  private static DataExportGroup.Output groupWith(Stream<DataExportValue.Output> values) {
    return groupWith("ds1", values);
  }

  private static DataExportGroup.Output groupWith(
      String dataSet, Stream<DataExportValue.Output> values) {
    return new DataExportGroup.Output(
        new DataExportGroup.Ids(), dataSet, "2020", "ou1", "aoc1", null, null, values);
  }

  private String toJson(DataExportGroup.Output group) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportOutput.toJson(group, out);
    return out.toString(UTF_8);
  }

  private String toCsv(DataExportGroup.Output group) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportOutput.toCsv(group, out);
    return out.toString(UTF_8);
  }

  private String toXml(DataExportGroup.Output group) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportOutput.toXml(group, out);
    return out.toString(UTF_8);
  }

  private String toXmlAdx(Stream<DataExportGroup.Output> groups) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportOutput.toXml(groups, out);
    return out.toString(UTF_8);
  }

  private void assertJson(@Language("JSON") String expected, DataExportGroup.Output actual) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportOutput.toJson(actual, out);
    String json = out.toString(UTF_8);
    if (!JsonMixed.of(expected).identicalTo(JsonMixed.of(json)))
      assertEquals(expected, json, "should be semantically identical");
  }
}
