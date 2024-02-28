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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.tracker.export.FileUtil.gZipToStringContent;
import static org.hisp.dhis.tracker.export.FileUtil.mapZipEntryToStringContent;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Luca Cambi
 */
public class TrackerExportFileTests extends TrackerNtiApiTest {

  private static String trackedEntity;
  private static String event;
  private static final String ENROLLMENT = "MNWZ6hnuhSw";
  private static final String PROGRAM = "f1AyMswryyQ";
  private static final String PROGRAM_STAGE = "nlXNK4b7LVr";
  private static final String CATEGORY_OPTION_COMBO = "HllvX50cXC0";
  private static final String CATEGORY_OPTION = "xYerKDKCefk";
  private static final String DATA_ELEMENT = "BuZ5LGNfGEU";
  private static final String DATA_ELEMENT_VALUE = "20";
  private static final String ATTRIBUTE = "dIVt4l5vIOa";
  private static final String ATTRIBUTE_VALUE = "attribute_value";
  private static final String TRACKED_ENTITY_TYPE = "Q9GufDoplCL";
  private static final String ORG_UNIT = "O6uvpzGd5pu";
  private static final String POLYGON =
      "POLYGON ((-12.305267 8.777237, -11.770837 8.98885, -11.55937 8.311341, -12.495765 8.368669, -12.305267 8.777237))";

  @BeforeAll
  public void beforeAll() throws Exception {
    loginActions.loginAsSuperUser();

    String payload =
        String.format(
            "                {\n"
                + "                  \"trackedEntities\": [\n"
                + "                    {\n"
                + "                      \"orgUnit\": \"O6uvpzGd5pu\",\n"
                + "                      \"trackedEntity\": \"Kj6vYde4LHZ\",\n"
                + "                      \"trackedEntityType\": \"%s\",\n"
                + "                      \"enrollments\": [\n"
                + "                        {\n"
                + "                          \"orgUnit\": \"O6uvpzGd5pu\",\n"
                + "                          \"program\": \"f1AyMswryyQ\",\n"
                + "                          \"trackedEntity\": \"Kj6vYde4LHZ\",\n"
                + "                          \"enrollment\": \"MNWZ6hnuhSw\",\n"
                + "                          \"enrolledAt\": \"2019-08-19T00:00:00.000\",\n"
                + "                          \"deleted\": false,\n"
                + "                          \"occurredAt\": \"2019-08-19T00:00:00.000\",\n"
                + "                          \"status\": \"ACTIVE\",\n"
                + "                          \"notes\": [],\n"
                + "                          \"relationships\": [],\n"
                + "                          \"attributes\": [],\n"
                + "                          \"followUp\" : true,\n"
                + "                          \"events\": [\n"
                + "                            {\n"
                + "                              \"scheduledAt\": \"2019-08-19T13:59:13.688\",\n"
                + "                              \"program\": \"f1AyMswryyQ\",\n"
                + "                              \"event\": \"ZwwuwNp6gVd\",\n"
                + "                              \"programStage\": \"%s\",\n"
                + "                              \"orgUnit\": \"O6uvpzGd5pu\",\n"
                + "                              \"enrollment\": \"MNWZ6hnuhSw\",\n"
                + "                              \"status\": \"ACTIVE\",\n"
                + "                              \"occurredAt\": \"2019-08-01T00:00:00.000\",\n"
                + "                              \"deleted\": false,\n"
                + "                              \"dataValues\": [\n"
                + "                                {\n"
                + "                                  \"updatedAt\": \"2019-08-19T13:58:37.477\",\n"
                + "                                  \"storedBy\": \"admin\",\n"
                + "                                  \"dataElement\": \"%s\",\n"
                + "                                  \"value\": \"%s\",\n"
                + "                                  \"providedElsewhere\": false\n"
                + "                                }\n"
                + "                              ],\n"
                + "                              \"notes\": [],\n"
                + "                              \"relationships\": []\n"
                + "                            }\n"
                + "                          ]\n"
                + "                        }\n"
                + "                      ],\n"
                + "                      \"attributes\": [\n"
                + "                        {\n"
                + "                          \"displayName\": \"TA First name\",\n"
                + "                          \"valueType\": \"TEXT\",\n"
                + "                          \"attribute\": \"%s\",\n"
                + "                          \"value\": \"%s\"\n"
                + "                        }\n"
                + "                      ],\n"
                + "                       \"geometry\": {\n"
                + "                         \"type\": \"Polygon\",\n"
                + "                         \"coordinates\": [\n"
                + "                           [\n"
                + "                             [\n"
                + "                               -12.305267,\n"
                + "                               8.777237\n"
                + "                             ],\n"
                + "                             [\n"
                + "                               -11.770837,\n"
                + "                               8.98885\n"
                + "                             ],\n"
                + "                             [\n"
                + "                               -11.55937,\n"
                + "                               8.311341\n"
                + "                             ],\n"
                + "                             [\n"
                + "                               -12.495765,\n"
                + "                               8.368669\n"
                + "                             ],\n"
                + "                             [\n"
                + "                               -12.305267,\n"
                + "                               8.777237\n"
                + "                             ]\n"
                + "                           ]\n"
                + "                         ]\n"
                + "                       }\n"
                + "                    }\n"
                + "                  ]\n"
                + "                }\n"
                + "                ",
            TRACKED_ENTITY_TYPE,
            PROGRAM_STAGE,
            DATA_ELEMENT,
            DATA_ELEMENT_VALUE,
            ATTRIBUTE,
            ATTRIBUTE_VALUE);

    TrackerApiResponse response =
        trackerActions
            .postAndGetJobReport(JsonParser.parseString(payload).getAsJsonObject())
            .validateSuccessfulImport();

    trackedEntity = response.extractImportedTeis().get(0);
    event = response.extractImportedEvents().get(0);
  }

  @Test
  public void shouldGetTrackedEntitiesFromCsvGzip() throws IOException {
    String s =
        gZipToStringContent(
            trackerActions
                .getTrackedEntitiesCsvGZip(
                    new QueryParamsBuilder()
                        .add("trackedEntityType", TRACKED_ENTITY_TYPE)
                        .add("orgUnit", ORG_UNIT)
                        .add("trackedEntities", trackedEntity))
                .validate()
                .statusCode(200)
                .contentType("application/csv+gzip;charset=utf-8")
                .extract()
                .response()
                .body()
                .asByteArray());

    try (CSVReader reader = new CSVReader(new StringReader(s))) {
      reader.readNext(); // header
      assertTrackedEntityCsv(reader.readNext());
    }
  }

  @Test
  public void shouldGetTrackedEntitiesFromCsv() throws IOException {
    byte[] s =
        trackerActions
            .getTrackedEntitiesCsv(
                new QueryParamsBuilder()
                    .add("trackedEntityType", TRACKED_ENTITY_TYPE)
                    .add("orgUnit", ORG_UNIT)
                    .add("trackedEntities", trackedEntity))
            .validate()
            .statusCode(200)
            .contentType("application/csv;charset=utf-8")
            .extract()
            .response()
            .body()
            .asByteArray();

    try (CSVReader reader = new CSVReader(new StringReader(new String(s)))) {
      reader.readNext(); // header
      assertTrackedEntityCsv(reader.readNext());
    }
  }

  private void assertTrackedEntityCsv(String[] record) {
    assertEquals(trackedEntity, record[0]); // trackedEntity
    assertEquals(TRACKED_ENTITY_TYPE, record[1]); // trackedEntityType
    assertNotNull(record[2]); // createdAt
    assertNotNull(record[4]); // updatedAt
    assertEquals(ORG_UNIT, record[6]); // orgUnit
    assertFalse(Boolean.parseBoolean(record[7])); // inactive
    assertFalse(Boolean.parseBoolean(record[8])); // deleted
    assertFalse(Boolean.parseBoolean(record[9])); // potentialDuplicate
    assertEquals(POLYGON, record[10]); // polygon
    assertNotNull(record[14]); // createdBy
    assertNotNull(record[15]); // updatedBy
    assertEquals(ATTRIBUTE, record[18]); // attributes -> attribute
    assertEquals(ATTRIBUTE_VALUE, record[20]); // attributes -> value
  }

  @Test
  public void shouldGetEventsFromJsonGZip() throws IOException {
    String s =
        gZipToStringContent(
            trackerActions
                .getEventsJsonGZip(new QueryParamsBuilder().add("events", event))
                .validate()
                .statusCode(200)
                .contentType("application/json+gzip;charset=utf-8")
                .extract()
                .response()
                .body()
                .asByteArray());

    JsonArray eventsJson = JsonParser.parseString(s).getAsJsonObject().getAsJsonArray("events");

    assertEventJson(eventsJson);
  }

  @Test
  public void shouldGetEventsFromJsonZip() throws IOException {
    Map<String, String> s =
        mapZipEntryToStringContent(
            trackerActions
                .getEventsJsonZip(new QueryParamsBuilder().add("events", event))
                .validate()
                .statusCode(200)
                .contentType("application/json+zip;charset=utf-8")
                .extract()
                .response()
                .body()
                .asByteArray());

    JsonArray eventsJson =
        JsonParser.parseString(s.get("events.json")).getAsJsonObject().getAsJsonArray("events");

    assertEventJson(eventsJson);
  }

  private static void assertEventJson(JsonArray eventsJson) {
    assertAll(
        () -> assertEquals(1, eventsJson.size()),
        () -> {
          JsonObject eventJson = eventsJson.get(0).getAsJsonObject();
          assertEquals(event, eventJson.get("event").getAsString());
          assertEquals(ORG_UNIT, eventJson.get("orgUnit").getAsString());
          assertEquals(ENROLLMENT, eventJson.get("enrollment").getAsString());
          assertEquals(PROGRAM_STAGE, eventJson.get("programStage").getAsString());
          assertEquals(PROGRAM, eventJson.get("program").getAsString());
          assertEquals(CATEGORY_OPTION_COMBO, eventJson.get("attributeOptionCombo").getAsString());
          assertEquals(CATEGORY_OPTION, eventJson.get("attributeCategoryOptions").getAsString());
          assertTrue(eventJson.get("followUp").getAsBoolean());
          assertNotNull(eventJson.get("createdAt"));
          assertNotNull(eventJson.get("updatedAt"));
          assertNotNull(eventJson.get("createdBy"));
          assertNotNull(eventJson.get("updatedBy"));

          JsonArray dataValues = eventJson.get("dataValues").getAsJsonArray();
          JsonObject dataValue = dataValues.get(0).getAsJsonObject();
          assertEquals(DATA_ELEMENT, dataValue.get("dataElement").getAsString());
          assertEquals(DATA_ELEMENT_VALUE, dataValue.get("value").getAsString());
        });
  }

  @Test
  public void shouldGetEventsFromCsvGZip() throws IOException {
    String s =
        gZipToStringContent(
            trackerActions
                .getEventsCsvGZip(new QueryParamsBuilder().add("events", event))
                .validate()
                .statusCode(200)
                .contentType("application/csv+gzip;charset=utf-8")
                .extract()
                .response()
                .body()
                .asByteArray());

    try (CSVReader reader = new CSVReader(new StringReader(s))) {
      reader.readNext(); // header
      assertEventCsv(reader.readNext());
    }
  }

  @Test
  public void shouldGetEventsFromCsvZip() throws IOException {
    Map<String, String> s =
        mapZipEntryToStringContent(
            trackerActions
                .getEventsCsvZip(new QueryParamsBuilder().add("events", event))
                .validate()
                .statusCode(200)
                .contentType("application/csv+zip;charset=utf-8")
                .extract()
                .response()
                .body()
                .asByteArray());

    try (CSVReader reader = new CSVReader(new StringReader(s.get("events.csv")))) {
      reader.readNext(); // header
      assertEventCsv(reader.readNext());
    }
  }

  private void assertEventCsv(String[] record) {
    assertEquals(event, record[0]); // event
    assertEquals("ACTIVE", record[1]); // status
    assertEquals(PROGRAM, record[2]); // program
    assertEquals(PROGRAM_STAGE, record[3]); // programStage
    assertEquals(ENROLLMENT, record[4]); // enrollment
    assertEquals(ORG_UNIT, record[5]); // orgUnit
    assertNotNull(record[6]); // createdAt
    assertNotNull(record[7]); // updatedAt
    assertTrue(Boolean.parseBoolean(record[11])); // followUp
    assertFalse(Boolean.parseBoolean(record[12])); // deleted
    assertNotNull(record[13]); // createdAt
    assertNotNull(record[15]); // updatedAt
    assertEquals(CATEGORY_OPTION_COMBO, record[20]); // attributeOptionCombo
    assertEquals(CATEGORY_OPTION, record[21]); // attributeCategoryOptions
    assertEquals(DATA_ELEMENT, record[23]); // dataElement
    assertEquals(DATA_ELEMENT_VALUE, record[24]); // value
  }
}
