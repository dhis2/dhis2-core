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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests creating, updating and exporting organisation unit levels through the API. */
@Transactional
class OrganisationUnitLevelControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String LEVEL_UID = "OuLevelUid1";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private ConfigurationService configurationService;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        "{'organisationUnitLevels':[{'id':'%s','code':'DISTRICT','name':'District','level':2}]}"
            .formatted(LEVEL_UID));

    JsonObject created = getLevel();
    assertEquals("DISTRICT", created.getString("code").string());
    assertEquals("District", created.getString("name").string());
    assertEquals(2, created.getNumber("level").intValue());
    assertFalse(created.has("offlineLevels"));

    importMetadata(
        "{'organisationUnitLevels':[{'id':'%s','code':'CHIEFDOM','name':'Chiefdom','level':3,'offlineLevels':1}]}"
            .formatted(LEVEL_UID));

    JsonObject updated = getLevel();
    assertEquals("CHIEFDOM", updated.getString("code").string());
    assertEquals("Chiefdom", updated.getString("name").string());
    assertEquals(3, updated.getNumber("level").intValue());
    assertEquals(1, updated.getNumber("offlineLevels").intValue());
  }

  @Test
  void testCreateViaOrganisationUnitLevelsEndpoint() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/organisationUnitLevels", "{'name':'Facility','level':4,'offlineLevels':2}"));

    JsonObject level = getLevel(uid);
    assertEquals("Facility", level.getString("name").string());
    assertEquals(4, level.getNumber("level").intValue());
    assertEquals(2, level.getNumber("offlineLevels").intValue());
  }

  @Test
  void testPutOfExportedObjectWithChanges() {
    importMetadata(
        "{'organisationUnitLevels':[{'id':'%s','name':'District','level':2}]}"
            .formatted(LEVEL_UID));
    String exported = getLevel().toJson();
    assertTrue(exported.contains("\"name\":\"District\""), exported);
    assertTrue(exported.contains("\"level\":2,"), exported);
    assertTrue(exported.contains("\"translations\":[]"), exported);
    String changed =
        exported
            .replace("\"name\":\"District\"", "\"name\":\"Region\",\"offlineLevels\":2")
            .replace("\"level\":2,", "\"level\":5,")
            .replace(
                "\"translations\":[]",
                "\"translations\":[{\"locale\":\"fr\",\"property\":\"NAME\",\"value\":\"Région\"}]");

    assertStatus(HttpStatus.OK, PUT("/organisationUnitLevels/" + LEVEL_UID, changed));

    JsonObject updated = getLevel();
    assertEquals("Region", updated.getString("name").string());
    assertEquals(5, updated.getNumber("level").intValue());
    assertEquals(2, updated.getNumber("offlineLevels").intValue());
    JsonArray translations = updated.getArray("translations");
    assertEquals(1, translations.size());
    assertEquals("Région", translations.getObject(0).getString("value").string());
  }

  @Test
  void testFilledLevelsAddAndUpdateByLevelNumber() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/filledOrganisationUnitLevels",
            "{'organisationUnitLevels':[{'name':'Country','level':1,'offlineLevels':1}]}"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/filledOrganisationUnitLevels",
            "{'organisationUnitLevels':[{'name':'Nation','level':1,'offlineLevels':2}]}"));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    JsonArray levels =
        GET("/organisationUnitLevels?fields=name,level,offlineLevels")
            .content(HttpStatus.OK)
            .getArray("organisationUnitLevels");
    assertEquals(1, levels.size());
    assertEquals("Nation", levels.getObject(0).getString("name").string());
    assertEquals(1, levels.getObject(0).getNumber("level").intValue());
    assertEquals(2, levels.getObject(0).getNumber("offlineLevels").intValue());
  }

  /** The keys of an exported level, as on a pre-migration instance, with href and access set. */
  @Test
  void testExportedProperties() {
    importMetadata(
        "{'organisationUnitLevels':[{'id':'%s','code':'DISTRICT','name':'District','level':2,'offlineLevels':1}]}"
            .formatted(LEVEL_UID));

    JsonObject level = getLevel();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "level",
            "name",
            "offlineLevels",
            "sharing",
            "translations"),
        Set.copyOf(level.names()));
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(level.getObject("sharing").names()));
    assertTrue(
        level.getString("href").string().endsWith("/organisationUnitLevels/" + LEVEL_UID),
        level.getString("href").string());
    assertTrue(level.getObject("access").getBoolean("read").booleanValue());
  }

  /** The nested level keys of a pre-migration instance: the identifiable properties only. */
  @Test
  void testConfigurationLevelsSerializeIdentifiableProperties() {
    importMetadata(
        "{'organisationUnitLevels':[{'id':'%s','name':'District','level':2,'offlineLevels':1}]}"
            .formatted(LEVEL_UID));
    Configuration configuration = configurationService.getConfiguration();
    OrganisationUnitLevel level = manager.get(OrganisationUnitLevel.class, LEVEL_UID);
    configuration.setOfflineOrganisationUnitLevel(level);
    configuration.setFacilityOrgUnitLevel(level);
    configurationService.setConfiguration(configuration);

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    JsonObject json = GET("/configuration").content(HttpStatus.OK);
    Set<String> expected =
        Set.of(
            "attributeValues",
            "created",
            "displayName",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "translations");
    assertEquals(expected, Set.copyOf(json.getObject("offlineOrganisationUnitLevel").names()));
    assertEquals(expected, Set.copyOf(json.getObject("facilityOrgUnitLevel").names()));
    assertEquals(
        "District", json.getObject("facilityOrgUnitLevel").getString("displayName").string());
  }

  /**
   * As on a pre-migration instance, a predictor lists its levels as references, and field filtering
   * still reaches every level property.
   */
  @Test
  void testPredictorLevelsSerializeAsReferences() {
    importMetadata(
        ("{'organisationUnitLevels':[{'id':'%s','name':'District','level':2}],"
                + "'dataElements':[{'id':'DataElemUid','name':'Output','shortName':'Output',"
                + "'valueType':'INTEGER','aggregationType':'SUM','domainType':'AGGREGATE'}],"
                + "'predictors':[{'id':'PredictoUid','name':'Predictor','shortName':'Predictor',"
                + "'output':{'id':'DataElemUid'},'generator':{'expression':'1'},'periodType':'Monthly',"
                + "'sequentialSampleCount':4,'annualSampleCount':3,"
                + "'organisationUnitLevels':[{'id':'%s'}],'organisationUnitDescendants':'SELECTED'}]}")
            .formatted(LEVEL_UID, LEVEL_UID));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    JsonArray levels =
        GET("/predictors/PredictoUid").content(HttpStatus.OK).getArray("organisationUnitLevels");
    assertEquals(1, levels.size());
    assertEquals(Set.of("id"), Set.copyOf(levels.getObject(0).names()));
    assertEquals(LEVEL_UID, levels.getObject(0).getString("id").string());

    JsonObject level =
        GET("/predictors/PredictoUid?fields=organisationUnitLevels[id,displayName,level]")
            .content(HttpStatus.OK)
            .getArray("organisationUnitLevels")
            .getObject(0);
    assertEquals("District", level.getString("displayName").string());
    assertEquals(2, level.getNumber("level").intValue());
  }

  /** Same property set and persisted properties as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/organisationUnitLevel").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    Set<String> persisted = new TreeSet<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      if (property.getBoolean("persisted").booleanValue()) {
        persisted.add(name);
      }
    }

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "level",
            "name",
            "offlineLevels",
            "sharing",
            "translation",
            "user"),
        names);
    assertEquals(
        Set.of(
            "code",
            "created",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "level",
            "name",
            "offlineLevels",
            "translation"),
        persisted);
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  private JsonObject getLevel() {
    return getLevel(LEVEL_UID);
  }

  /** Reads the level from the database, bypassing the session and the second-level cache. */
  private JsonObject getLevel(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/organisationUnitLevels/" + uid).content(HttpStatus.OK);
  }
}
