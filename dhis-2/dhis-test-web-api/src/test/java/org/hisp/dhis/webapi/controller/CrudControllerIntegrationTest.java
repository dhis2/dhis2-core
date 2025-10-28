/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonOrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
class CrudControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  @Autowired private UserSettingsService userSettingsService;

  @Autowired private SystemSettingsService settingsService;

  @Test
  void testGetNonAccessibleObject() {
    User admin = getCurrentUser();
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly', 'sharing':{'public':'--------','owner': '"
                    + admin.getUid()
                    + "'}}"));

    User testUser = createAndAddUser("test");
    injectSecurityContextUser(testUser);

    GET("/dataSets/{id}", id).content(HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetAccessibleObject() {
    User testUser = createAndAddUser("test", (OrganisationUnit) null, "F_DATASET_PRIVATE_ADD");
    injectSecurityContextUser(testUser);
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly', 'sharing':{'public':'--------','owner': '"
                    + testUser.getUid()
                    + "'}}"));

    DataSet dataSet = manager.get(DataSet.class, id);
    assertEquals(testUser.getUid(), dataSet.getSharing().getOwner());

    GET("/dataSets/{id}", id).content(HttpStatus.OK);
  }

  @Test
  @DisplayName("Search by token should use translations column instead of default columns")
  void testSearchByToken() throws Exception {
    setUpTranslation();
    User userA = createAndAddUser("userA", (OrganisationUnit) null, "ALL");
    userSettingsService.put("keyDbLocale", Locale.FRENCH, userA.getUsername());

    injectSecurityContextUser(userService.getUserByUsername(userA.getUsername()));

    JsonArray dataSets =
        GET("/dataSets?filter=identifiable:token:testToken").content().getArray("dataSets");

    log.error("dataSets: {}", dataSets);

    assertTrue(dataSets.isEmpty());
    assertFalse(
        GET("/dataSets?filter=identifiable:token:french").content().getArray("dataSets").isEmpty());
    assertFalse(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());
  }

  @Test
  @DisplayName("Search by token should use default properties instead of translations column")
  void testSearchTokenDefaultLocale() {
    setUpTranslation();
    User userA = createAndAddUser("userA", (OrganisationUnit) null, "ALL");

    JsonArray dataSets =
        GET("/dataSets?filter=identifiable:token:testToken").content().getArray("dataSets");

    assertTrue(dataSets.isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:french").content().getArray("dataSets").isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());

    assertFalse(
        GET("/dataSets?filter=identifiable:token:english")
            .content()
            .getArray("dataSets")
            .isEmpty());
  }

  @Test
  @DisplayName("Search by token should use default properties instead of translations column")
  void testSearchTokenWithNullLocale() throws Exception {
    setUpTranslation();
    doInTransaction(() -> settingsService.put("keyDbLocale", Locale.ENGLISH));
    settingsService.clearCurrentSettings();
    assertEquals(Locale.ENGLISH, settingsService.getCurrentSettings().getDbLocale());

    User userA = createAndAddUser("userA", null, "ALL");
    injectSecurityContextUser(userA);
    userSettingsService.put("keyDbLocale", null);

    assertTrue(
        GET("/dataSets?filter=identifiable:token:testToken")
            .content()
            .getArray("dataSets")
            .isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:french").content().getArray("dataSets").isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());

    assertFalse(
        GET("/dataSets?filter=identifiable:token:english")
            .content()
            .getArray("dataSets")
            .isEmpty());
  }

  @Test
  @DisplayName(
      "Search by token should use default properties if the translation value does not exist or does not match the search token")
  void testSearchTokenWithFallback() throws Exception {
    setUpTranslation();
    User userA = createAndAddUser("userA", (OrganisationUnit) null, "ALL");
    userSettingsService.put("keyDbLocale", Locale.FRENCH, userA.getUsername());

    injectSecurityContextUser(userService.getUserByUsername(userA.getUsername()));

    assertFalse(
        GET("/dataSets?filter=identifiable:token:english&locale=fr")
            .content()
            .getArray("dataSets")
            .isEmpty());
    assertFalse(
        GET("/dataSets?filter=identifiable:token:french").content().getArray("dataSets").isEmpty());
    assertFalse(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());
  }

  @Test
  @DisplayName("Should not apply token filter for UID if value has length < 4")
  void testIdentifiableTokenFilterLength() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits/",
            "{'name':'My Unit 1', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    String ou2 =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit 2', 'shortName':'OU2', 'openingDate': '2020-01-01'}"));

    JsonMixed response =
        GET("/organisationUnits?filter=identifiable:token:" + ou2.substring(0, 3)).content();
    assertEquals(0, response.getArray("organisationUnits").size());

    response = GET("/organisationUnits?filter=identifiable:token:" + ou2.substring(0, 4)).content();
    assertEquals(1, response.getArray("organisationUnits").size());
    assertEquals(ou2, response.getArray("organisationUnits").getObject(0).getString("id").string());
  }

  // -------------------------------------------------------------------------
  // DisplayName and DisplayDescription field filter tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Should filter data sets by displayName and return results from database query")
  void testFilterByDisplayName() {
    // Create data sets with translations
    String ds1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'Health Program Data', 'shortName': 'HPD', 'periodType':'Monthly'}"));

    String ds2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'Education Program Data', 'shortName': 'EPD', 'periodType':'Monthly'}"));

    // Add translations for the first dataset
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/dataSets/" + ds1Id + "/translations",
            """
            {
                'translations': [
                    {
                        'locale': 'fr',
                        'property': 'NAME',
                        'value': 'Données du programme de santé'
                    }
                ]
            }
            """));

    // Add translations for the second dataset
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/dataSets/" + ds2Id + "/translations",
            """
            {
                'translations': [
                    {
                        'locale': 'fr',
                        'property': 'NAME',
                        'value': 'Données du programme éducatif'
                    }
                ]
            }
            """));

    // Test filtering by displayName with English locale (default)
    // Should match "Health Program Data"
    JsonList<JsonIdentifiableObject> results =
        GET("/dataSets?fields=id,displayName&filter=displayName:like:Health")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertEquals(1, results.size(), "Should find 1 dataset matching 'Health'");
    assertEquals(ds1Id, results.get(0).getId());
    assertEquals("Health Program Data", results.get(0).getDisplayName());

    // Test filtering with a different match
    results =
        GET("/dataSets?fields=id,displayName&filter=displayName:like:Education")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertEquals(1, results.size(), "Should find 1 dataset matching 'Education'");
    assertEquals(ds2Id, results.get(0).getId());
    assertEquals("Education Program Data", results.get(0).getDisplayName());
  }

  @Test
  @DisplayName(
      "Should filter data sets by displayDescription and return results from database query")
  void testFilterByDisplayDescription() {
    // Create data sets with descriptions and translations
    String ds1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Dataset One',
                'shortName': 'DS1',
                'periodType': 'Monthly',
                'description': 'Collects monthly health indicators'
            }
            """));

    String ds2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Dataset Two',
                'shortName': 'DS2',
                'periodType': 'Monthly',
                'description': 'Collects education indicators'
            }
            """));

    // Add translation for description
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/dataSets/" + ds1Id + "/translations",
            """
            {
                'translations': [
                    {
                        'locale': 'es',
                        'property': 'DESCRIPTION',
                        'value': 'Recopila indicadores de salud mensuales'
                    }
                ]
            }
            """));

    // Test filtering by displayDescription (using English - default locale)
    JsonList<JsonIdentifiableObject> results =
        GET("/dataSets?fields=id,displayDescription&filter=displayDescription:like:health")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertEquals(1, results.size(), "Should find 1 dataset with description containing 'health'");
    assertEquals(ds1Id, results.get(0).getId());
    assertTrue(
        results.get(0).getDisplayDescription().toLowerCase().contains("health"),
        "Display description should contain 'health'");

    // Test filtering by displayDescription with different keyword
    results =
        GET("/dataSets?fields=id,displayDescription&filter=displayDescription:like:education")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertEquals(
        1, results.size(), "Should find 1 dataset with description containing 'education'");
    assertEquals(ds2Id, results.get(0).getId());
    assertTrue(
        results.get(0).getDisplayDescription().toLowerCase().contains("education"),
        "Display description should contain 'education'");
  }

  @Test
  @DisplayName("Should order data sets by displayName using database sorting")
  void testOrderByDisplayName() {
    // Create multiple data sets
    String ds1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    String ds2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST("/dataSets/", "{'name':'Apple Data', 'shortName': 'AD', 'periodType':'Monthly'}"));

    String ds3Id =
        assertStatus(
            HttpStatus.CREATED,
            POST("/dataSets/", "{'name':'Mango Data', 'shortName': 'MD', 'periodType':'Monthly'}"));

    // Test ordering by displayName ascending
    JsonList<JsonIdentifiableObject> results =
        GET("/dataSets?fields=id,displayName&order=displayName:asc")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertTrue(results.size() >= 3, "Should have at least 3 datasets");

    Map<String, Integer> indexMap = new HashMap<>();
    for (int i = 0; i < results.size(); i++) {
      indexMap.put(results.get(i).getId(), i);
    }
    int appleIndex = indexMap.get(ds2Id);
    int mangoIndex = indexMap.get(ds3Id);
    int zebraIndex = indexMap.get(ds1Id);

    assertTrue(appleIndex < mangoIndex, "Apple should come before Mango in alphabetical order");
    assertTrue(mangoIndex < zebraIndex, "Mango should come before Zebra in alphabetical order");

    // Test ordering by displayName descending
    results =
        GET("/dataSets?fields=id,displayName&order=displayName:desc")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertTrue(results.size() >= 3, "Should have at least 3 datasets");

    indexMap = new HashMap<>();
    for (int i = 0; i < results.size(); i++) {
      indexMap.put(results.get(i).getId(), i);
    }

    appleIndex = indexMap.get(ds2Id);
    zebraIndex = indexMap.get(ds1Id);

    assertTrue(
        zebraIndex < appleIndex, "Zebra should come before Apple in reverse alphabetical order");
  }

  @Test
  @DisplayName(
      "Should return displayName and displayDescription fields correctly when requested in field filter")
  void testFieldsFilterWithDisplayProperties() {
    // Create a data set with translations
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Test Dataset',
                'shortName': 'TD',
                'periodType': 'Monthly',
                'description': 'A test dataset for field filtering'
            }
            """));

    // Add translations
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/dataSets/" + dsId + "/translations",
            """
            {
                'translations': [
                    {
                        'locale': 'fr',
                        'property': 'NAME',
                        'value': 'Jeu de données de test'
                    },
                    {
                        'locale': 'fr',
                        'property': 'DESCRIPTION',
                        'value': 'Un jeu de données de test pour le filtrage des champs'
                    },
                    {
                        'locale': 'fr',
                        'property': 'SHORT_NAME',
                        'value': 'JDT'
                    }
                ]
            }
            """));

    // Test requesting displayName, displayDescription, and displayShortName fields
    JsonList<JsonIdentifiableObject> results =
        GET("/dataSets?fields=id,name,displayName,description,displayDescription,shortName,displayShortName&filter=id:eq:"
            + dsId)
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertEquals(1, results.size(), "Should return exactly 1 dataset");

    JsonIdentifiableObject dataset = results.get(0);

    // Verify that displayName returns the name (no locale specified, defaults to English)
    assertEquals("Test Dataset", dataset.getDisplayName(), "displayName should be 'Test Dataset'");
    assertEquals("Test Dataset", dataset.getName(), "name should be 'Test Dataset'");

    // Verify displayDescription
    assertEquals(
        "A test dataset for field filtering",
        dataset.getDisplayDescription(),
        "displayDescription should match the description");
    assertEquals(
        "A test dataset for field filtering", dataset.getDescription(), "description should match");

    // Verify displayShortName
    assertEquals("TD", dataset.getDisplayShortName(), "displayShortName should be 'TD'");
    assertEquals("TD", dataset.getShortName(), "shortName should be 'TD'");
  }

  @Test
  @DisplayName("Should combine displayName filter with other filters in database query")
  void testCombineDisplayNameFilterWithOtherFilters() {
    // Create multiple data sets
    String ds1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'Health Monthly Report', 'shortName': 'HMR', 'periodType':'Monthly'}"));

    String ds2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'Health Quarterly Report', 'shortName': 'HQR', 'periodType':'Quarterly'}"));

    String ds3Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'Education Monthly Report', 'shortName': 'EMR', 'periodType':'Monthly'}"));

    // Test combining displayName filter with periodType filter
    // Should return only datasets with "Health" in displayName AND periodType = Monthly
    JsonList<JsonIdentifiableObject> results =
        GET("/dataSets?fields=id,displayName,periodType&filter=displayName:like:Health&filter=periodType:eq:Monthly")
            .content()
            .getList("dataSets", JsonIdentifiableObject.class);

    assertEquals(
        1, results.size(), "Should find 1 dataset with 'Health' in name and Monthly period type");
    assertEquals(ds1Id, results.get(0).getId());
    assertEquals("Health Monthly Report", results.get(0).getDisplayName());
  }

  @Test
  void testPage2AndOrderByDisplayName() {
    setUpTestFilterByDisplayName();
    JsonList<JsonOrganisationUnit> ous =
        GET("/organisationUnits?order=displayName&paging=true&pageSize=2&page=2")
            .content(HttpStatus.OK)
            .getList("organisationUnits", JsonOrganisationUnit.class);
    assertEquals(2, ous.size());
    assertEquals("C", ous.get(0).getDisplayName());
    assertEquals("D", ous.get(1).getDisplayName());
  }

  @ParameterizedTest
  @CsvSource({
      "1, 2, A, B",
      "2, 2, C, D",
  })
  void testOrderByDisplayName(int page, int size, String firstItem, String secondItem) {
    setUpTestFilterByDisplayName();
    JsonList<JsonOrganisationUnit> ous =
        GET("/organisationUnits?order=displayName&paging=true&pageSize=2&page=" + page)
            .content(HttpStatus.OK)
            .getList("organisationUnits", JsonOrganisationUnit.class);
    assertEquals(size, ous.size());
    assertEquals(firstItem, ous.get(0).getDisplayName());
    assertEquals(secondItem, ous.get(1).getDisplayName());
  }

  @ParameterizedTest
  @CsvSource({
      "1, 2, E, D",
      "2, 2, C, B",
  })
  void testOrderByDisplayNameDesc(int page, int size, String firstItem, String secondItem) {
    setUpTestFilterByDisplayName();
    JsonList<JsonOrganisationUnit> ous =
        GET("/organisationUnits?order=displayName:desc&paging=true&pageSize=2&page=" + page)
            .content(HttpStatus.OK)
            .getList("organisationUnits", JsonOrganisationUnit.class);
    assertEquals(size, ous.size());
    assertEquals(firstItem, ous.get(0).getDisplayName());
    assertEquals(secondItem, ous.get(1).getDisplayName());
  }

  @Test
  void testFilterOuByDisplayName() {
    setUpTestFilterByDisplayName();
    JsonList<JsonOrganisationUnit> ous =
        GET("/organisationUnits?filter=displayName:in:[A,B,C]&paging=true&pageSize=2&page=2")
            .content(HttpStatus.OK)
            .getList("organisationUnits", JsonOrganisationUnit.class);
    assertEquals(1, ous.size());
    assertEquals("C", ous.get(0).getDisplayName());
  }

  private void setUpTestFilterByDisplayName() {
    manager.save(createOrganisationUnit("A"));
    manager.save(createOrganisationUnit("B"));
    manager.save(createOrganisationUnit("C"));
    manager.save(createOrganisationUnit("D"));
    manager.save(createOrganisationUnit("E"));
  }

  private void setUpTranslation() {
    injectSecurityContextUser(getAdminUser());

    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'english data Set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    PUT(
        "/dataSets/" + id + "/translations",
        "{'translations': [{'locale':'fr', 'property':'NAME', 'value':'french dataSet'}]}")
        .content(HttpStatus.NO_CONTENT);

    assertEquals(1, GET("/dataSets", id).content().getArray("dataSets").size());

    JsonArray translations =
        GET("/dataSets/{id}/translations", id).content().getArray("translations");
    assertEquals(1, translations.size());
  }
}
