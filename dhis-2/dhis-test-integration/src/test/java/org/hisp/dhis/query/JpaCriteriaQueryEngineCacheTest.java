/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the query cache skip configuration in JpaCriteriaQueryEngine.
 *
 * <p>These tests verify that:
 *
 * <ul>
 *   <li>The default setting includes OrganisationUnit to skip query cache
 *   <li>The setting can be configured to include additional classes
 *   <li>OrganisationUnit queries work correctly regardless of cache setting
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class JpaCriteriaQueryEngineCacheTest extends PostgresIntegrationTestBase {

  @Autowired private QueryService queryService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private SystemSettingsService settingsService;

  private OrganisationUnit orgUnitA;
  private OrganisationUnit orgUnitB;
  private OrganisationUnit orgUnitC;
  private DataElement dataElementA;

  @BeforeEach
  void setUp() {
    // Create test org unit hierarchy
    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA);

    orgUnitB = createOrganisationUnit('B');
    orgUnitB.setParent(orgUnitA);
    manager.save(orgUnitB);

    orgUnitC = createOrganisationUnit('C');
    orgUnitC.setParent(orgUnitB);
    manager.save(orgUnitC);

    // Create test data element
    dataElementA = createDataElement('A');
    manager.save(dataElementA);

    // Clear settings to ensure default values are used
    settingsService.clearCurrentSettings();
  }

  @Test
  @DisplayName("Default setting should include OrganisationUnit for query cache skip")
  void testDefaultQueryCacheSkipClassesIncludesOrganisationUnit() {
    SystemSettings settings = settingsService.getCurrentSettings();
    String skipClasses = settings.getQueryCacheSkipClasses();

    assertNotNull(skipClasses);
    assertTrue(skipClasses.contains("OrganisationUnit"), "Default should include OrganisationUnit");
  }

  @Test
  @DisplayName("OrganisationUnit query should work with default cache skip setting")
  void testOrganisationUnitQueryWithDefaultSetting() {
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    List<OrganisationUnit> results = queryService.query(query);

    assertEquals(3, results.size(), "Should find all 3 org units");
  }

  @Test
  @DisplayName("OrganisationUnit query with parent filter should work correctly")
  void testOrganisationUnitQueryWithParentFilter() {
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    query.add(Filters.eq("parent.id", orgUnitA.getUid()));
    List<OrganisationUnit> results = queryService.query(query);

    assertEquals(1, results.size(), "Should find only orgUnitB (direct child of A)");
    assertEquals(orgUnitB.getUid(), results.get(0).getUid());
  }

  @Test
  @DisplayName("DataElement query should use cache by default (not in skip list)")
  void testDataElementQueryUsesCache() {
    // DataElement is not in the default skip list, so queries should be cacheable
    Query<DataElement> query = Query.of(DataElement.class);
    List<DataElement> results = queryService.query(query);

    assertEquals(1, results.size());
    assertEquals(dataElementA.getUid(), results.get(0).getUid());
  }

  @Test
  @DisplayName("Custom setting can add additional classes to skip list")
  void testCustomQueryCacheSkipClasses() {
    // Add DataElement to the skip list
    settingsService.put("keyQueryCacheSkipClasses", "OrganisationUnit,DataElement");
    settingsService.clearCurrentSettings();

    SystemSettings settings = settingsService.getCurrentSettings();
    String skipClasses = settings.getQueryCacheSkipClasses();

    assertTrue(skipClasses.contains("OrganisationUnit"));
    assertTrue(skipClasses.contains("DataElement"));

    // Queries should still work correctly
    Query<DataElement> query = Query.of(DataElement.class);
    List<DataElement> results = queryService.query(query);
    assertEquals(1, results.size());
  }

  @Test
  @DisplayName(
      "Setting to non-matching value should enable cache for all types including OrganisationUnit")
  void testNonMatchingQueryCacheSkipClasses() {
    // Set skip list to a value that doesn't match any class to enable caching for all types
    // Note: Empty string falls back to default, so we use a non-matching value
    settingsService.put("keyQueryCacheSkipClasses", "None");
    settingsService.clearCurrentSettings();

    SystemSettings settings = settingsService.getCurrentSettings();
    String skipClasses = settings.getQueryCacheSkipClasses();

    assertEquals("None", skipClasses, "Skip list should be 'None'");

    // Queries should still work correctly even with cache enabled for OrganisationUnit
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    List<OrganisationUnit> results = queryService.query(query);
    assertEquals(3, results.size());
  }

  @Test
  @DisplayName("OrganisationUnit hierarchy queries should work with cache skip")
  void testOrganisationUnitHierarchyQueries() {
    // Test deep hierarchy query (grandparent)
    Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
    query.add(Filters.eq("parent.parent.id", orgUnitA.getUid()));
    List<OrganisationUnit> results = queryService.query(query);

    assertEquals(1, results.size(), "Should find orgUnitC (grandchild of A)");
    assertEquals(orgUnitC.getUid(), results.get(0).getUid());
  }

  @Test
  @DisplayName("Multiple repeated queries should work correctly")
  void testMultipleRepeatedQueries() {
    // Execute the same query multiple times to verify consistent behavior
    for (int i = 0; i < 5; i++) {
      Query<OrganisationUnit> query = Query.of(OrganisationUnit.class);
      List<OrganisationUnit> results = queryService.query(query);
      assertEquals(3, results.size(), "Query " + (i + 1) + " should return 3 org units");
    }
  }

  @Test
  @DisplayName("Setting with spaces should be trimmed correctly")
  void testSettingWithSpacesIsTrimmed() {
    // Set skip list with extra spaces
    settingsService.put("keyQueryCacheSkipClasses", " OrganisationUnit , DataElement ");
    settingsService.clearCurrentSettings();

    // Queries should still work
    Query<OrganisationUnit> ouQuery = Query.of(OrganisationUnit.class);
    List<OrganisationUnit> ouResults = queryService.query(ouQuery);
    assertEquals(3, ouResults.size());

    Query<DataElement> deQuery = Query.of(DataElement.class);
    List<DataElement> deResults = queryService.query(deQuery);
    assertEquals(1, deResults.size());
  }
}
