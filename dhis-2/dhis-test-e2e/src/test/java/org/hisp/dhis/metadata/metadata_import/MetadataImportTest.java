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
package org.hisp.dhis.metadata.metadata_import;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.SystemActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.dto.ObjectReport;
import org.hisp.dhis.test.e2e.dto.TypeReport;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.hisp.dhis.test.e2e.utils.SharingUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
class MetadataImportTest extends ApiTest {
  private MetadataActions metadataActions;
  private RestApiActions dataElementActions;
  private SystemActions systemActions;

  @BeforeAll
  public void before() {
    metadataActions = new MetadataActions();
    systemActions = new SystemActions();
    dataElementActions = new RestApiActions("dataElements");

    new LoginActions().loginAsSuperUser();
  }

  @Test
  @DisplayName("importing existing metadata should not override created date - JSON")
  void shouldNotOverrideCreatedDateJsonTest() {
    // given
    String uid =
        metadataActions
            .importMetadata(
                """
                {
                    "dataElements": [
                        {
                            "id": "DeUid000001",
                            "name": "test 1",
                            "aggregationType": "NONE",
                            "domainType": "AGGREGATE",
                            "valueType": "TEXT",
                            "shortName": "datael1"
                        }
                    ]
                }
                """,
                "async=false")
            .extractObjectUid("DataElement")
            .get(0);

    // confirm created date
    String created = dataElementActions.get(uid).validate().extract().path("created");

    // when updating existing data element name
    metadataActions.importMetadata(
        """
        {
            "dataElements": [
                {
                    "id": "DeUid000001",
                    "name": "test 1 update",
                    "aggregationType": "NONE",
                    "domainType": "AGGREGATE",
                    "valueType": "TEXT",
                    "shortName": "datael1"
                }
            ]
        }
        """);

    // then confirm created date has not changed and name is updated
    dataElementActions
        .get(uid)
        .validate()
        .body("created", equalTo(created))
        .body("name", equalTo("test 1 update"));
  }

  @Test
  @DisplayName("importing existing metadata should not override created date - CSV")
  void shouldNotOverrideCreatedDateCsvTest() {
    // given
    String uid =
        metadataActions
            .importMetadataWithContentType(
                new File("src/test/resources/metadata/dataElements/dataElementCreate.csv"),
                "application/csv",
                "importStrategy=CREATE_AND_UPDATE",
                "mergeMode=REPLACE",
                "format=csv",
                "firstRowIsHeader=true",
                "classKey=DATA_ELEMENT",
                "async=false")
            .extractObjectUid("DataElement")
            .get(0);

    // confirm created date
    String created = dataElementActions.get(uid).validate().extract().path("created");

    // when updating existing data element name
    metadataActions.importMetadataWithContentType(
        new File("src/test/resources/metadata/dataElements/dataElementUpdate.csv"),
        "application/csv",
        "importStrategy=CREATE_AND_UPDATE",
        "mergeMode=REPLACE",
        "format=csv",
        "firstRowIsHeader=true",
        "classKey=DATA_ELEMENT",
        "async=false");

    // then confirm created date has not changed and name is updated
    dataElementActions
        .get(uid)
        .validate()
        .body("created", equalTo(created))
        .body("shortName", equalTo("ANC 1st visit_m update"));
  }

  @ParameterizedTest(name = "withImportStrategy[{0}]")
  @CsvSource({"CREATE, ignored, 409", "CREATE_AND_UPDATE, updated, 200"})
  void shouldUpdateExistingMetadata(
      String importStrategy, String expected, int expectedStatusCode) {
    // arrange
    JsonObject exported = metadataActions.get().getBody();

    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(
        "async=false", "importReportMode=FULL", "importStrategy=" + importStrategy);

    // act
    ApiResponse response = metadataActions.post(exported, queryParamsBuilder);

    // assert
    response
        .validate()
        .statusCode(expectedStatusCode)
        .rootPath("response")
        .body("stats", notNullValue())
        .rootPath("response.stats")
        .body("total", greaterThan(0))
        .body("created", Matchers.equalTo(0))
        .body("deleted", Matchers.equalTo(0))
        .body("total", equalTo(response.extract("response.stats." + expected)));

    List<Map<?, ?>> typeReports = response.extractList("response.typeReports.stats");

    typeReports.forEach(
        x -> {
          assertEquals(
              x.get(expected), x.get("total"), expected + " for " + x + " not equals to total");
        });
  }

  @Test
  void shouldImportUniqueMetadataAndReturnObjectReports() throws Exception {
    // arrange
    JsonObject object =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/metadata/uniqueMetadata.json"));

    // act
    ApiResponse response =
        metadataActions.post(
            object,
            new QueryParamsBuilder()
                .addAll("async=false", "importReportMode=DEBUG", "importStrategy=CREATE"));

    // assert
    response
        .validate()
        .statusCode(200)
        .rootPath("response")
        .body("stats", notNullValue())
        .body("stats.total", greaterThan(0))
        .body("typeReports", notNullValue())
        .body("typeReports.stats", notNullValue())
        .body("typeReports.objectReports", Matchers.notNullValue());

    List<Map<?, ?>> stats = response.extractList("response.typeReports.stats");

    stats.forEach(
        x -> {
          assertEquals(x.get("total"), x.get("created"));
        });

    List<ObjectReport> objectReports = getObjectReports(response.getTypeReports());

    assertNotNull(objectReports);
    validateCreatedEntities(objectReports);
  }

  @Test
  void shouldReturnObjectReportsWhenSomeMetadataWasIgnoredAndAtomicModeFalse() throws Exception {
    // arrange
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(
        "async=false", "importReportMode=DEBUG", "importStrategy=CREATE", "atomicMode=NONE");

    JsonObject object =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/metadata/uniqueMetadata.json"));

    // act
    ApiResponse response = metadataActions.post(object, queryParamsBuilder);
    response.validate().statusCode(200);

    JsonObject newObj =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/metadata/uniqueMetadata.json"));

    // add one of the orgunits from already imported metadata to get it
    // ignored
    newObj
        .get("organisationUnits")
        .getAsJsonArray()
        .add(object.get("organisationUnits").getAsJsonArray().get(0));

    response = metadataActions.post(newObj, queryParamsBuilder);

    // assert
    response
        .validate()
        .statusCode(409)
        .rootPath("response")
        .body("stats", notNullValue())
        .body("stats.total", greaterThan(1))
        .body("stats.ignored", equalTo(1))
        .body("stats.created", equalTo((Integer) response.extract("response.stats.total") - 1));

    int total = (int) response.extract("response.stats.total");

    List<ObjectReport> objectReports = getObjectReports(response.getTypeReports());

    assertNotNull(objectReports);
    validateCreatedEntities(objectReports);

    assertThat(objectReports, hasItems(hasProperty("errorReports", notNullValue())));
    assertEquals(total, objectReports.size(), "Not all imported entities had object reports");
  }

  @Test
  void shouldReturnImportSummariesWhenImportingInvalidMetadataAsync() throws Exception {
    // arrange
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(
        "async=true",
        "importReportMode=DEBUG",
        "importStrategy=CREATE_AND_UPDATE",
        "atomicMode=NONE");

    JsonObject metadata =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/metadata/uniqueMetadata.json"));

    metadata
        .getAsJsonArray("organisationUnits")
        .get(0)
        .getAsJsonObject()
        .addProperty("shortName", RandomStringUtils.random(51));

    // act
    ApiResponse response = metadataActions.post(metadata, queryParamsBuilder);
    response
        .validate()
        .statusCode(200)
        .body(notNullValue())
        .body("response.name", startsWith("METADATA_IMPORT"))
        .body("response.jobType", equalTo("METADATA_IMPORT"));

    String taskId = response.extractString("response.id");

    // Validate that job was successful

    systemActions
        .waitUntilTaskCompleted("METADATA_IMPORT", taskId)
        .validate()
        .body("message", hasItem(containsString("Metadata import started")))
        .body("message", hasItem(containsString("Import complete with status")));
    ;

    // validate task summaries were created
    systemActions
        .waitForTaskSummaries("METADATA_IMPORT", taskId)
        .validate()
        .body(notNullValue())
        .body("status", equalTo("WARNING"))
        .body("typeReports", notNullValue())
        .rootPath("typeReports")
        .body("stats.total", everyItem(greaterThan(0)))
        .body("stats.ignored", hasSize(greaterThanOrEqualTo(1)))
        .body("objectReports", notNullValue())
        .body("objectReports", hasSize(greaterThanOrEqualTo(1)))
        .body("objectReports.errorReports", notNullValue());
  }

  @Test
  void shouldImportMetadataAsync() throws Exception {
    JsonObject object =
        new FileReaderUtils()
            .readJsonAndGenerateData(new File("src/test/resources/metadata/uniqueMetadata.json"));
    // arrange
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(
        "async=false",
        "importReportMode=DEBUG",
        "importStrategy=CREATE_AND_UPDATE",
        "atomicMode=NONE");

    // import metadata so that we have references and can clean up

    // act
    ApiResponse response = metadataActions.post(object, queryParamsBuilder);

    // send async request
    queryParamsBuilder.add("async=true");

    response = metadataActions.post(object, queryParamsBuilder);

    response
        .validate()
        .statusCode(200)
        .body("response", notNullValue())
        .body("response.name", startsWith("METADATA_IMPORT"))
        .body("response.jobType", equalTo("METADATA_IMPORT"));

    String taskId = response.extractString("response.id");
    assertNotNull(taskId, "Task id was not returned");
    // Validate that job was successful

    systemActions
        .waitUntilTaskCompleted("METADATA_IMPORT", taskId)
        .validate()
        .body("message", notNullValue())
        .body("message", hasItem(containsString("Metadata import started")))
        .body("message", hasItem(containsString("Import complete with status")));

    // validate task summaries were created
    systemActions
        .waitForTaskSummaries("METADATA_IMPORT", taskId)
        .validate()
        .body(notNullValue())
        .body("status", equalTo("OK"))
        .body("stats", notNullValue())
        .body("stats.total", is(greaterThanOrEqualTo(0)))
        .body("stats.created", is(greaterThanOrEqualTo(0)))
        .body("stats.updated", is(greaterThanOrEqualTo(0)))
        .body("stats.deleted", is(greaterThanOrEqualTo(0)))
        .body("stats.ignored", is(greaterThanOrEqualTo(0)))
        .body("typeReports", notNullValue())
        .rootPath("typeReports")
        .body("stats", notNullValue())
        .body("objectReports", hasSize(greaterThan(0)));
  }

  @Test
  void shouldNotSkipSharing() {
    JsonObject object = generateMetadataObjectWithInvalidSharing();

    ApiResponse response =
        metadataActions.post(object, new QueryParamsBuilder().add("skipSharing=false"));

    response
        .validate()
        .statusCode(409)
        .rootPath("response")
        .body("status", equalTo("ERROR"))
        .body("stats.created", equalTo(0))
        .body(
            "typeReports[0].objectReports[0].errorReports[0].message",
            stringContainsInOrder("Invalid reference", "for association `userGroupAccesses`"));
  }

  @Test
  void shouldSkipSharing() {
    JsonObject metadata = generateMetadataObjectWithInvalidSharing();

    ApiResponse response =
        metadataActions.post(metadata, new QueryParamsBuilder().add("skipSharing=true"));

    response
        .validate()
        .statusCode(200)
        .rootPath("response")
        .body("status", is(oneOf("SUCCESS", "OK")))
        .body("stats.created", equalTo(1));
  }

  private JsonObject generateMetadataObjectWithInvalidSharing() {
    JsonObject dataElementGroup = DataGenerator.generateObjectForEndpoint("/dataElementGroup");

    dataElementGroup.add(
        "sharing",
        SharingUtils.createSharingObject(
            null, "rw------", Map.of(), Map.of("non-existing-id", "rwrw----")));

    JsonArray array = new JsonArray();
    array.add(dataElementGroup);

    JsonObject metadata = new JsonObject();
    metadata.add("dataElementGroups", array);

    return metadata;
  }

  private List<ObjectReport> getObjectReports(List<TypeReport> typeReports) {
    List<ObjectReport> objectReports = new ArrayList<>();

    typeReports.stream()
        .forEach(
            typeReport -> {
              objectReports.addAll(typeReport.getObjectReports());
            });

    return objectReports;
  }

  private void validateCreatedEntities(List<ObjectReport> objectReports) {
    objectReports.forEach(
        report -> {
          assertNotEquals("", report.getUid());
          assertNotEquals("", report.getKlass());
          assertNotEquals("", report.getIndex());
          assertNotEquals("", report.getDisplayName());
        });
  }
}
