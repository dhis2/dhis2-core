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
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Luca Cambi
 */
public class TrackerExportFileTests extends TrackerApiTest {

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
        """
                {
                  "trackedEntities": [
                    {
                      "orgUnit": "O6uvpzGd5pu",
                      "trackedEntity": "Kj6vYde4LHZ",
                      "trackedEntityType": "%s",
                      "enrollments": [
                        {
                          "orgUnit": "O6uvpzGd5pu",
                          "program": "f1AyMswryyQ",
                          "trackedEntity": "Kj6vYde4LHZ",
                          "enrollment": "MNWZ6hnuhSw",
                          "enrolledAt": "2019-08-19T00:00:00.000",
                          "deleted": false,
                          "occurredAt": "2019-08-19T00:00:00.000",
                          "status": "ACTIVE",
                          "notes": [],
                          "relationships": [],
                          "attributes": [],
                          "followUp" : true,
                          "events": [
                            {
                              "scheduledAt": "2019-08-19T13:59:13.688",
                              "program": "f1AyMswryyQ",
                              "event": "ZwwuwNp6gVd",
                              "programStage": "%s",
                              "orgUnit": "O6uvpzGd5pu",
                              "enrollment": "MNWZ6hnuhSw",
                              "status": "ACTIVE",
                              "occurredAt": "2019-08-01T00:00:00.000",
                              "deleted": false,
                              "dataValues": [
                                {
                                  "updatedAt": "2019-08-19T13:58:37.477",
                                  "storedBy": "admin",
                                  "dataElement": "%s",
                                  "value": "%s",
                                  "providedElsewhere": false
                                }
                              ],
                              "notes": [],
                              "relationships": []
                            }
                          ]
                        }
                      ],
                      "attributes": [
                        {
                          "displayName": "TA First name",
                          "valueType": "TEXT",
                          "attribute": "%s",
                          "value": "%s"
                        }
                      ],
                       "geometry": {
                         "type": "Polygon",
                         "coordinates": [
                           [
                             [
                               -12.305267,
                               8.777237
                             ],
                             [
                               -11.770837,
                               8.98885
                             ],
                             [
                               -11.55937,
                               8.311341
                             ],
                             [
                               -12.495765,
                               8.368669
                             ],
                             [
                               -12.305267,
                               8.777237
                             ]
                           ]
                         ]
                       }
                    }
                  ]
                }
                """
            .formatted(
                TRACKED_ENTITY_TYPE,
                PROGRAM_STAGE,
                DATA_ELEMENT,
                DATA_ELEMENT_VALUE,
                ATTRIBUTE,
                ATTRIBUTE_VALUE);

    TrackerApiResponse response =
        trackerImportExportActions
            .postAndGetJobReport(JsonParser.parseString(payload).getAsJsonObject())
            .validateSuccessfulImport();

    trackedEntity = response.extractImportedTeis().get(0);
    event = response.extractImportedEvents().get(0);
  }

  @Test
  public void shouldGetTrackedEntitiesFromCsvGzip() throws IOException, CsvValidationException {
    String s =
        gZipToStringContent(
            trackerImportExportActions
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
  public void shouldGetTrackedEntitiesFromCsv() throws IOException, CsvException {
    byte[] s =
        trackerImportExportActions
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
            trackerImportExportActions
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
            trackerImportExportActions
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
  public void shouldGetEventsFromCsvGZip() throws IOException, CsvValidationException {
    String s =
        gZipToStringContent(
            trackerImportExportActions
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
  public void shouldGetEventsFromCsvZip() throws IOException, CsvValidationException {
    Map<String, String> s =
        mapZipEntryToStringContent(
            trackerImportExportActions
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
