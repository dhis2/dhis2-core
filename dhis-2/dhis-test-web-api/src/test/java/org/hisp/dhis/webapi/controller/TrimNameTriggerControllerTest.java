/*
 * Copyright (c) 2004-2024, University of Oslo
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
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests that a unique name violation caused by the {@code trg_trim_name} trigger trimming the
 * {@code name} on update is reported to the client, so that the duplicate name can be fixed, and
 * that a blank name, which the trigger would trim to an empty string, is rejected as missing.
 */
class TrimNameTriggerControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private JdbcTemplate jdbcTemplate;

  private String untrimmedUid;

  @BeforeEach
  void setUp() {
    postDataElement("ANC", "ANC short");
    untrimmedUid = postDataElement("ANC other", "ANC other short");
    setNameBypassingTrigger(untrimmedUid, "ANC ");
  }

  @Test
  void updateOfObjectWhoseTrimmedNameIsDuplicateReturnsConflict() {
    JsonObject response =
        PUT(
                "/dataElements/" + untrimmedUid,
                """
                {
                  "name": "ANC ",
                  "shortName": "%s",
                  "description": "changed",
                  "domainType": "AGGREGATE",
                  "aggregationType": "SUM",
                  "valueType": "NUMBER"
                }"""
                    .formatted("ANC other short"))
            .content(HttpStatus.CONFLICT);

    assertContains("Key (name)=(ANC) already exists", response.getString("message").string());
    assertEquals("ANC ", selectName(untrimmedUid));
  }

  @Test
  void metadataImportOfObjectWhoseTrimmedNameIsDuplicateReturnsConflict() {
    JsonObject response =
        POST(
                "/metadata",
                """
                {
                  "dataElements": [{
                    "id": "%s",
                    "name": "ANC ",
                    "shortName": "%s",
                    "description": "changed",
                    "domainType": "AGGREGATE",
                    "aggregationType": "SUM",
                    "valueType": "NUMBER"
                  }]
                }"""
                    .formatted(untrimmedUid, "ANC other short"))
            .content(HttpStatus.CONFLICT);

    assertContains("Key (name)=(ANC) already exists", response.getString("message").string());
    assertEquals("ANC ", selectName(untrimmedUid));
  }

  @Test
  void createOfObjectWithBlankNameReturnsConflict() {
    JsonWebMessage message =
        POST(
                "/dataElements",
                """
                {
                  "name": " \\t ",
                  "shortName": "Blank name",
                  "domainType": "AGGREGATE",
                  "aggregationType": "SUM",
                  "valueType": "NUMBER"
                }""")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    assertEquals(
        "Missing required property `name`", message.findErrorReport(ErrorCode.E4000).getMessage());
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from dataelement where shortname = 'Blank name'", Integer.class));
  }

  @Test
  void updateOfObjectToBlankNameReturnsConflict() {
    String uid = postDataElement("Blank update", "Blank update short");

    JsonWebMessage message =
        PUT(
                "/dataElements/" + uid,
                """
                {
                  "name": "   ",
                  "shortName": "Blank update short",
                  "domainType": "AGGREGATE",
                  "aggregationType": "SUM",
                  "valueType": "NUMBER"
                }""")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    assertEquals(
        "Missing required property `name`", message.findErrorReport(ErrorCode.E4000).getMessage());
    assertEquals("Blank update", selectName(uid));
  }

  @Test
  void createOfObjectWithPaddedNameReturnsTrimmedName() {
    String uid = postDataElement(" \\t Padded create \\n ", "Padded create short");

    assertEquals("Padded create", GET("/dataElements/" + uid).content().getString("name").string());
    assertEquals("Padded create", selectName(uid));
  }

  @Test
  void updateOfObjectWithPaddedNameReturnsTrimmedName() {
    String uid = postDataElement("Padded update", "Padded update short");

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/dataElements/" + uid,
            """
            {
              "name": "  Padded update changed  ",
              "shortName": "Padded update short",
              "domainType": "AGGREGATE",
              "aggregationType": "SUM",
              "valueType": "NUMBER"
            }"""));

    assertEquals(
        "Padded update changed", GET("/dataElements/" + uid).content().getString("name").string());
    assertEquals("Padded update changed", selectName(uid));
  }

  @Test
  void patchOfObjectWithPaddedNameReturnsTrimmedName() {
    String uid = postDataElement("Padded patch", "Padded patch short");

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/dataElements/" + uid,
            """
            [{"op": "replace", "path": "/name", "value": " \\t Padded patch changed \\n "}]"""));

    assertEquals(
        "Padded patch changed", GET("/dataElements/" + uid).content().getString("name").string());
    assertEquals("Padded patch changed", selectName(uid));
  }

  @Test
  void metadataImportCreateOfObjectsWithPaddedNamesReturnsTrimmedNames() {
    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
            {
              "dataElements": [{
                "id": "DeImportCr1",
                "name": " \\t Imported data element \\n ",
                "shortName": "Imported data element short",
                "domainType": "AGGREGATE",
                "aggregationType": "SUM",
                "valueType": "NUMBER"
              }],
              "organisationUnits": [{
                "id": "OuImportCr1",
                "name": "  Imported org unit  ",
                "shortName": "Imported org unit short",
                "openingDate": "2020-01-01"
              }]
            }"""));

    assertEquals(
        "Imported data element",
        GET("/dataElements/DeImportCr1").content().getString("name").string());
    assertEquals("Imported data element", selectName("DeImportCr1"));
    assertEquals(
        "Imported org unit",
        GET("/organisationUnits/OuImportCr1").content().getString("name").string());
    assertEquals(
        "Imported org unit",
        jdbcTemplate.queryForObject(
            "select name from organisationunit where uid = 'OuImportCr1'", String.class));
  }

  @Test
  void metadataImportUpdateOfObjectWithPaddedNameReturnsTrimmedName() {
    String uid = postDataElement("Import update", "Import update short");

    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata?importStrategy=UPDATE",
            """
            {
              "dataElements": [{
                "id": "%s",
                "name": "  Import update changed  ",
                "shortName": "Import update short",
                "domainType": "AGGREGATE",
                "aggregationType": "SUM",
                "valueType": "NUMBER"
              }]
            }"""
                .formatted(uid)));

    assertEquals(
        "Import update changed", GET("/dataElements/" + uid).content().getString("name").string());
    assertEquals("Import update changed", selectName(uid));
  }

  @Test
  void metadataImportWithSkipValidationOfObjectWithPaddedNameReturnsTrimmedName() {
    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata?skipValidation=true",
            """
            {
              "dataElements": [{
                "id": "DeImportSk1",
                "name": " \\t Skip validation \\n ",
                "shortName": "Skip validation short",
                "domainType": "AGGREGATE",
                "aggregationType": "SUM",
                "valueType": "NUMBER"
              }]
            }"""));

    assertEquals(
        "Skip validation", GET("/dataElements/DeImportSk1").content().getString("name").string());
    assertEquals("Skip validation", selectName("DeImportSk1"));
  }

  @Test
  void metadataImportOfObjectsWhoseTrimmedNamesAreDuplicatesReturnsConflict() {
    HttpResponse response =
        POST(
            "/metadata",
            """
            {
              "dataElements": [{
                "id": "DeImportDu1",
                "name": "Import duplicate",
                "shortName": "Import duplicate short 1",
                "domainType": "AGGREGATE",
                "aggregationType": "SUM",
                "valueType": "NUMBER"
              }, {
                "id": "DeImportDu2",
                "name": " Import duplicate ",
                "shortName": "Import duplicate short 2",
                "domainType": "AGGREGATE",
                "aggregationType": "SUM",
                "valueType": "NUMBER"
              }]
            }""");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from dataelement where uid in ('DeImportDu1', 'DeImportDu2')",
            Integer.class));
  }

  private String postDataElement(String name, String shortName) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataElements",
            """
            {
              "name": "%s",
              "shortName": "%s",
              "domainType": "AGGREGATE",
              "aggregationType": "SUM",
              "valueType": "NUMBER"
            }"""
                .formatted(name, shortName)));
  }

  private void setNameBypassingTrigger(String uid, String name) {
    jdbcTemplate.execute("alter table dataelement disable trigger trg_trim_name");
    try {
      jdbcTemplate.update("update dataelement set name = ? where uid = ?", name, uid);
    } finally {
      jdbcTemplate.execute("alter table dataelement enable trigger trg_trim_name");
    }
  }

  private String selectName(String uid) {
    return jdbcTemplate.queryForObject(
        "select name from dataelement where uid = ?", String.class, uid);
  }
}
