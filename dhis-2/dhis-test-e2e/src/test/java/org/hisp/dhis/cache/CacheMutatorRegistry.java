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
package org.hisp.dhis.cache;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.restassured.http.ContentType;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.SchemasActions;
import org.hisp.dhis.test.e2e.actions.SystemSettingActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.UserRoleActions;
import org.hisp.dhis.test.e2e.actions.metadata.AttributeActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.actions.metadata.OrgUnitActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.dto.schemas.PropertyType;
import org.hisp.dhis.test.e2e.dto.schemas.Schema;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.utils.DataGenerator;

final class CacheMutatorRegistry {
  private static final Set<String> FIXTURE_ONLY_METADATA_SCHEMAS =
      Set.of(
          "relationshipTypes",
          "messageConversations",
          "users",
          "organisationUnitLevels",
          "programRuleActions",
          "programRuleVariables",
          "eventCharts",
          "programIndicators",
          "programStages",
          "programStageWorkingLists",
          "oAuth2Clients");

  private final LoginActions loginActions = new LoginActions();
  private final UserActions userActions = new UserActions();
  private final UserRoleActions userRoleActions = new UserRoleActions();
  private final OrgUnitActions orgUnitActions = new OrgUnitActions();
  private final SystemSettingActions systemSettingActions = new SystemSettingActions();
  private final AttributeActions attributeActions = new AttributeActions();
  private final MetadataActions metadataActions = new MetadataActions();
  private final SchemasActions schemasActions = new SchemasActions();
  private final RestApiActions userGroups = new RestApiActions("/userGroups");
  private final RestApiActions userSettings = new RestApiActions("/userSettings");
  private final RestApiActions dataStore = new RestApiActions("/dataStore");
  private final RestApiActions userDataStore = new RestApiActions("/userDataStore");
  private final RestApiActions dataStatistics = new RestApiActions("/dataStatistics");
  private final CacheResourceLocator resourceLocator;
  private final Map<String, Schema> metadataSchemas = new ConcurrentHashMap<>();

  CacheMutatorRegistry(CacheResourceLocator resourceLocator) {
    this.resourceLocator = resourceLocator;
  }

  void mutate(CacheDependency dependency) {
    loginActions.loginAsSuperUser();

    switch (dependency) {
      case USER -> mutateUser();
      case USER_ROLE -> userRoleActions.createWithAuthorities("F_SYSTEM_SETTING");
      case USER_GROUP ->
          userGroups.create(
              JsonObjectBuilder.jsonObject()
                  .addProperty("name", "Cache user group " + DataGenerator.randomString())
                  .build());
      case ORGANISATION_UNIT -> orgUnitActions.createOrgUnitWithParent(Constants.ORG_UNIT_IDS[0]);
      case ORGANISATION_UNIT_LEVEL -> mutateMetadataSchema("organisationUnitLevels");
      case SYSTEM_SETTING ->
          mutateSystemSetting("applicationTitle", "Cache title " + DataGenerator.randomString());
      case USER_SETTING -> mutateUserSetting("keyStyle", "cache-" + DataGenerator.randomString());
      case DATASTORE_ENTRY -> mutateDataStoreEntry();
      case USER_DATASTORE_ENTRY -> mutateUserDataStoreEntry();
      case ATTRIBUTE -> attributeActions.createAttribute("TEXT", false, "organisationUnit");
      case FILE_RESOURCE -> mutateFileResource();
      case CATEGORY -> mutateMetadataSchema("categories");
      case CATEGORY_OPTION_GROUP_SET -> mutateMetadataSchema("categoryOptionGroupSets");
      case DATA_ELEMENT_GROUP_SET -> mutateMetadataSchema("dataElementGroupSets");
      case ORGANISATION_UNIT_GROUP_SET -> mutateMetadataSchema("organisationUnitGroupSets");
      case DATA_SET -> mutateMetadataSchema("dataSets");
      case DATA_APPROVAL_LEVEL -> mutateMetadataSchema("dataApprovalLevels");
      case DATA_APPROVAL_WORKFLOW -> mutateMetadataSchema("dataApprovalWorkflows");
      case DATA_STATISTICS -> dataStatistics.postNoBody("/snapshot").validateStatus(201);
      case DATA_STATISTICS_EVENT -> mutateDataStatisticsEvent();
    }
  }

  void mutateMetadataSchema(Schema schema) {
    loginActions.loginAsSuperUser();

    File fixture = fixtureFor(schema.getPlural());
    if (fixture.exists()) {
      metadataActions.importAndValidateMetadata(fixture, "async=false");
      return;
    }

    if (requiresFixture(schema)) {
      throw new AssertionError(
          "Add a cache fixture at src/test/resources/cache/fixtures/"
              + schema.getPlural()
              + ".json to enable self-type invalidation coverage for "
              + schema.getRelativeApiEndpoint());
    }

    JsonObject object = DataGenerator.generateObjectForEndpoint(schema.getSingular());
    ApiResponse response = new RestApiActions(schema.getRelativeApiEndpoint()).post(object);

    assertTrue(
        response.statusCode() == 200 || response.statusCode() == 201,
        "Expected a successful metadata mutation for " + schema.getPlural());
  }

  void mutateMetadataSchema(String plural) {
    mutateMetadataSchema(findMetadataSchema(plural));
  }

  private void mutateUser() {
    String username = "cache-" + DataGenerator.randomString();
    userActions.addUser(username, Constants.USER_PASSWORD);
  }

  private void mutateSystemSetting(String key, String value) {
    systemSettingActions.post("/" + key, "text/plain", value, null).validateStatus(200);
  }

  private void mutateUserSetting(String key, String value) {
    userSettings.post("/" + key, "text/plain", value, null).validateStatus(200);
  }

  private void mutateDataStoreEntry() {
    ApiResponse response =
        dataStore.put("cache-e2e/mutation", "{\"value\":\"" + DataGenerator.randomString() + "\"}");
    assertTrue(
        response.statusCode() == 200 || response.statusCode() == 201,
        "Expected datastore mutation to succeed");
  }

  private void mutateUserDataStoreEntry() {
    ApiResponse response =
        userDataStore.put(
            "cache-e2e/mutation", "{\"value\":\"" + DataGenerator.randomString() + "\"}");
    assertTrue(
        response.statusCode() == 200 || response.statusCode() == 201,
        "Expected user datastore mutation to succeed");
  }

  private void mutateFileResource() {
    ApiResponse response =
        new ApiResponse(
            given()
                .queryParam("domain", "USER_AVATAR")
                .multiPart(
                    "file", new File("src/test/resources/fileResources/dhis2.png"), "image/png")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/fileResources"));

    assertTrue(response.statusCode() == 202, "Expected file resource upload to be accepted");
    String fileResourceId = response.extractString("response.fileResource.id");
    assertNotNull(fileResourceId, "Expected file resource id");
    TestRunStorage.addCreatedEntity("/fileResources", fileResourceId);
  }

  private void mutateDataStatisticsEvent() {
    QueryParamsBuilder queryParams =
        new QueryParamsBuilder()
            .add("eventType", "VISUALIZATION_VIEW")
            .add("favorite", resourceLocator.firstVisualizationId());

    ApiResponse response = dataStatistics.post("", null, queryParams);
    assertTrue(
        response.statusCode() == 200 || response.statusCode() == 201,
        "Expected data statistics event mutation to succeed");
  }

  private Schema findMetadataSchema(String plural) {
    return metadataSchemas.computeIfAbsent(
        plural,
        key ->
            schemasActions.getMetadataSchemas().stream()
                .filter(schema -> key.equals(schema.getPlural()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new AssertionError(
                            "Expected metadata schema for "
                                + key
                                + " to support cache invalidation tests")));
  }

  private boolean requiresFixture(Schema schema) {
    if (FIXTURE_ONLY_METADATA_SCHEMAS.contains(schema.getPlural())) {
      return true;
    }

    if (schema.getSingular() == null
        || schema.getRelativeApiEndpoint() == null
        || schema.getProperties() == null) {
      return true;
    }

    return schema.getRequiredProperties().stream()
        .map(property -> property.getPropertyType())
        .anyMatch(type -> type == PropertyType.COMPLEX || type == PropertyType.COLLECTION);
  }

  private File fixtureFor(String plural) {
    return new File("src/test/resources/cache/fixtures/" + plural + ".json");
  }
}
