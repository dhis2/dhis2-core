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
package org.hisp.dhis.analytics.tracker;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.EVENT;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createLegend;
import static org.hisp.dhis.test.TestBase.createLegendSet;
import static org.hisp.dhis.test.TestBase.createOption;
import static org.hisp.dhis.test.TestBase.createOptionSet;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataItemsHandlerTest {

  @Mock private AnalyticsSecurityManager securityManager;

  @Mock private UserService userService;

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @InjectMocks private MetadataItemsHandler metadataItemsHandler;

  private OrganisationUnit orgUnitA;
  private OrganisationUnit orgUnitB;
  private Program programA;
  private DataElement dataElementA;
  private DataElement dataElementB;
  private Option optionA;
  private Option optionB;
  private OptionSet optionSetA;
  private Legend legendA;
  private Legend legendB;
  private LegendSet legendSetA;

  @BeforeAll
  static void setUpClass() {
    injectSecurityContextNoSettings(new SystemUser());
  }

  @BeforeEach
  void setUp() {
    orgUnitA = createOrganisationUnit('A');
    orgUnitB = createOrganisationUnit('B');
    programA = createProgram('A', null, orgUnitA);

    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');

    optionA = createOption('A');
    optionB = createOption('B');
    optionSetA = createOptionSet('A', optionA, optionB);

    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 10d, 20d);
    legendSetA = createLegendSet('A', legendA, legendB);
  }

  @Nested
  @DisplayName("When skipMeta is true")
  class SkipMetaTests {

    @Test
    @DisplayName("should not add any metadata items to grid")
    void shouldNotSetMetadataWhenSkipMetaIsTrue() {
      // Given
      Grid grid = new ListGrid();
      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(true)
              .withEndpointAction(QUERY)
              .build();

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      // When skipMeta is true, no metadata items or dimensions should be added
      assertFalse(grid.getMetaData().containsKey(ITEMS.getKey()));
      assertFalse(grid.getMetaData().containsKey(DIMENSIONS.getKey()));
    }
  }

  @Nested
  @DisplayName("When skipMeta is false")
  class AddMetadataTests {

    @Test
    @DisplayName("should set metadata on grid with basic params")
    void shouldSetMetadataOnGridWithBasicParams() {
      // Given
      Grid grid = new ListGrid();
      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertTrue(grid.getMetaData().containsKey(ITEMS.getKey()));
      assertTrue(grid.getMetaData().containsKey(DIMENSIONS.getKey()));
    }

    @Test
    @DisplayName(
        "should use raw item ID as dimension key for option set items in non-query context")
    void shouldUseRawItemIdAsDimensionKeyForOptionSetItemsInNonQueryContext() {
      // Given
      Grid grid = new ListGrid();

      // 1. Create a Program Stage
      // (Assuming createProgramStage is available in TestBase, otherwise mock it)
      org.hisp.dhis.program.ProgramStage programStage = createProgramStage('S', programA);

      // 2. Create a QueryItem with an OptionSet AND associated with the ProgramStage
      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              optionSetA);
      queryItem.setProgramStage(programStage);

      // 3. Create params for a NON-QUERY action (AGGREGATE)
      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE) // This ensures itemOptions is empty
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());

      assertNotNull(dimensions);

      // THE BUG CHECK:
      // The bug causes the key to be "ProgramStageID.DataElementID"
      // The fix ensures the key is just "DataElementID"

      // 1. Assert the RAW ID is present
      assertTrue(
          dimensions.containsKey(dataElementA.getUid()),
          "Dimensions map should contain the raw Data Element UID key");

      // 2. Assert the PREFIXED ID is NOT present
      String prefixedId = programStage.getUid() + "." + dataElementA.getUid();
      assertFalse(
          dimensions.containsKey(prefixedId),
          "Dimensions map should NOT contain the ProgramStage prefixed UID key");
    }

    @Test
    @DisplayName("should include period dimension items in metadata")
    void shouldIncludePeriodDimensionItems() {
      // Given
      Grid grid = new ListGrid();
      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1", "2023Q2"), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey(PERIOD_DIM_ID));
      assertEquals(2, dimensions.get(PERIOD_DIM_ID).size());
    }

    @Test
    @DisplayName("should emit enrollment date periods under enrollmentdate key and pe")
    void shouldEmitEnrollmentDatePeriodsUnderSeparateKey() {
      // Given
      Grid grid = new ListGrid();

      // Create periods with ENROLLMENT_DATE dateField (simulating what
      // normalizeStaticDateDimension produces when dimension=ENROLLMENT_DATE:2023Q1)
      List<PeriodDimension> enrollmentPeriods =
          createPeriodDimensions("2023Q1").stream()
              .map(pd -> pd.setDateField("ENROLLMENT_DATE"))
              .toList();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(enrollmentPeriods, "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);

      // Enrollment date periods should be under "enrollmentdate" key
      assertTrue(
          dimensions.containsKey("enrollmentdate"),
          "Dimensions should contain 'enrollmentdate' key for ENROLLMENT_DATE periods");
      assertEquals(1, dimensions.get("enrollmentdate").size());

      // pe should remain populated for backwards-compatible clients
      List<String> pePeriods = dimensions.getOrDefault(PERIOD_DIM_ID, List.of());
      assertEquals(1, pePeriods.size(), "pe dimension should contain ENROLLMENT_DATE periods");

      // Metadata items should contain an entry for "enrollmentdate"
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(
          items.containsKey("enrollmentdate"),
          "Items should contain 'enrollmentdate' metadata entry");
      MetadataItem enrollmentDateItem = (MetadataItem) items.get("enrollmentdate");
      assertEquals("DateOfEnrollmentDescription", enrollmentDateItem.getName());
    }

    @Test
    @DisplayName("should use program custom enrollment date label in enrollmentdate metadata item")
    void shouldUseCustomEnrollmentDateLabel() {
      // Given
      Grid grid = new ListGrid();

      Program programWithCustomLabel = createProgram('C', null, orgUnitA);
      programWithCustomLabel.setEnrollmentDateLabel("Date of Registration");

      List<PeriodDimension> enrollmentPeriods =
          createPeriodDimensions("2023Q1").stream()
              .map(pd -> pd.setDateField("ENROLLMENT_DATE"))
              .toList();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programWithCustomLabel)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(enrollmentPeriods, "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey("enrollmentdate"));
      MetadataItem enrollmentDateItem = (MetadataItem) items.get("enrollmentdate");
      assertEquals("Date of Registration", enrollmentDateItem.getName());
    }

    @Test
    @DisplayName("should use program custom incident date label in incidentdate metadata item")
    void shouldUseCustomIncidentDateLabel() {
      // Given
      Grid grid = new ListGrid();

      Program programWithCustomLabel = createProgram('D', null, orgUnitA);
      programWithCustomLabel.setIncidentDateLabel("Date of Symptom Onset");

      List<PeriodDimension> incidentPeriods =
          createPeriodDimensions("2023Q1").stream()
              .map(pd -> pd.setDateField("INCIDENT_DATE"))
              .toList();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programWithCustomLabel)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(incidentPeriods, "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey("incidentdate"));
      MetadataItem incidentDateItem = (MetadataItem) items.get("incidentdate");
      assertEquals("Date of Symptom Onset", incidentDateItem.getName());
    }

    @Test
    @DisplayName(
        "should emit enrollmentdate in items and dimensions after periods consumed and re-added")
    void shouldEmitEnrollmentDateMetadataAfterPeriodsConsumedAndReAdded() {
      // Given - simulate the query path where withStartEndDatesForPeriods() converts
      // period dimensions into timeDateRanges and removes the period dimension,
      // then periods are re-added for metadata generation
      Grid grid = new ListGrid();

      List<PeriodDimension> enrollmentPeriods =
          createPeriodDimensions("2023Q1").stream()
              .map(pd -> pd.setDateField("ENROLLMENT_DATE"))
              .toList();

      // Build with periods, consume them via withStartEndDatesForPeriods(),
      // then re-add periods (simulating the retain-and-re-add pattern)
      EventQueryParams initialParams =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(enrollmentPeriods, "quarterly")
              .build();

      EventQueryParams params =
          new EventQueryParams.Builder(initialParams)
              .withStartEndDatesForPeriods()
              .withPeriods(enrollmentPeriods, "")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then - enrollmentdate should appear in items
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(
          items.containsKey("enrollmentdate"),
          "Items should contain 'enrollmentdate' metadata entry");
      MetadataItem enrollmentDateItem = (MetadataItem) items.get("enrollmentdate");
      assertEquals("DateOfEnrollmentDescription", enrollmentDateItem.getName());

      // Dimensions should contain enrollmentdate with period UIDs
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(
          dimensions.containsKey("enrollmentdate"),
          "Dimensions should contain 'enrollmentdate' key");
      assertEquals(1, dimensions.get("enrollmentdate").size());
      assertTrue(
          dimensions.getOrDefault(PERIOD_DIM_ID, List.of()).isEmpty(),
          "Query path should keep legacy behavior with empty pe dimension");
    }

    @Test
    @DisplayName("should emit incident date periods under incidentdate key and pe")
    void shouldEmitIncidentDatePeriodsUnderSeparateKey() {
      // Given
      Grid grid = new ListGrid();

      List<PeriodDimension> incidentPeriods =
          createPeriodDimensions("2023Q1").stream()
              .map(pd -> pd.setDateField("INCIDENT_DATE"))
              .toList();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(incidentPeriods, "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);

      assertTrue(
          dimensions.containsKey("incidentdate"),
          "Dimensions should contain 'incidentdate' key for INCIDENT_DATE periods");
      assertEquals(1, dimensions.get("incidentdate").size());

      List<String> pePeriods = dimensions.getOrDefault(PERIOD_DIM_ID, List.of());
      assertEquals(1, pePeriods.size(), "pe dimension should contain INCIDENT_DATE periods");
    }

    @Test
    @DisplayName("should separate mixed periods into pe and date-specific keys")
    void shouldSeparateMixedPeriodsIntoPeAndDateSpecificKeys() {
      // Given
      Grid grid = new ListGrid();

      // Mix of regular period and enrollment date period
      PeriodDimension regularPeriod = createPeriodDimensions("2023Q1").get(0);
      PeriodDimension enrollmentPeriod =
          createPeriodDimensions("2023Q2").get(0).setDateField("ENROLLMENT_DATE");

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(List.of(regularPeriod, enrollmentPeriod), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);

      // pe contains all periods
      assertTrue(dimensions.containsKey(PERIOD_DIM_ID));
      assertEquals(2, dimensions.get(PERIOD_DIM_ID).size());

      // Enrollment date period under enrollmentdate
      assertTrue(dimensions.containsKey("enrollmentdate"));
      assertEquals(1, dimensions.get("enrollmentdate").size());
    }

    @Test
    @DisplayName("should include organisation unit dimension items in metadata")
    void shouldIncludeOrgUnitDimensionItems() {
      // Given
      Grid grid = new ListGrid();
      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA, orgUnitB))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey(ORGUNIT_DIM_ID));
      assertEquals(2, dimensions.get(ORGUNIT_DIM_ID).size());
    }
  }

  @Nested
  @DisplayName("When request is coming from query endpoint")
  class QueryEndpointTests {

    @Test
    @DisplayName("should add metadata items for query with grid data")
    void shouldAddMetadataItemsForQueryWithGridData() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("value1");

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertTrue(grid.getMetaData().containsKey(ITEMS.getKey()));
      assertTrue(grid.getMetaData().containsKey(DIMENSIONS.getKey()));
    }

    @Test
    @DisplayName("should add keywords to metadata items")
    void shouldAddKeywordsToMetadataItems() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("value1");

      DimensionItemKeywords dimensionItemKeywords = new DimensionItemKeywords();
      dimensionItemKeywords.addKeyword("keywordUid", "Keyword Name");
      Keyword keyword = dimensionItemKeywords.getKeyword("keywordUid");

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of(keyword));

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey("keywordUid"));
    }

    @Test
    @DisplayName("should process items with option sets")
    void shouldProcessItemsWithOptionSets() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue(optionA.getCode());

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              optionSetA);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
    }

    @Test
    @DisplayName("should process items with legend sets")
    void shouldProcessItemsWithLegendSets() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("5");

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              legendSetA,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      // Legend items should be included
      assertTrue(items.containsKey(legendA.getUid()) || items.containsKey(legendB.getUid()));
    }
  }

  @Nested
  @DisplayName("When request is NOT coming from query endpoint")
  class NonQueryEndpointTests {

    @Test
    @DisplayName("should add metadata items for non-query request")
    void shouldAddMetadataItemsForNonQueryRequest() {
      // Given
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertTrue(grid.getMetaData().containsKey(ITEMS.getKey()));
      assertTrue(grid.getMetaData().containsKey(DIMENSIONS.getKey()));
    }

    @Test
    @DisplayName("should include legend metadata for non-query request")
    void shouldIncludeLegendMetadataForNonQueryRequest() {
      // Given
      Grid grid = new ListGrid();

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              legendSetA,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey(legendA.getUid()));
      assertTrue(items.containsKey(legendB.getUid()));
    }

    @Test
    @DisplayName("should include option metadata for non-query request")
    void shouldIncludeOptionMetadataForNonQueryRequest() {
      // Given
      Grid grid = new ListGrid();

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              optionSetA);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey(optionA.getUid()));
      assertTrue(items.containsKey(optionB.getUid()));
    }

    @Test
    @DisplayName("should exclude synthetic dimensions from query metadata")
    void shouldExcludeSyntheticDimensionsFromQueryMetadata() {
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("value1");

      BaseDimensionalObject requestedDimension = new BaseDimensionalObject();
      requestedDimension.setUid("kO3z4Dhc038.C31vHZqu0qU");
      requestedDimension.setDimensionType(DimensionType.CATEGORY_OPTION_GROUP_SET);
      requestedDimension.setDimensionName("C31vHZqu0qU");
      requestedDimension.setName("Funding Partner");
      requestedDimension.setItems(List.of());
      requestedDimension.setGroupUUID(UUID.randomUUID());

      BaseDimensionalObject syntheticDimension =
          new BaseDimensionalObject(
              "LFsZ8v5v7rq", DimensionType.CATEGORY, "LFsZ8v5v7rq", "Partner", List.of());

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addDimension(requestedDimension)
              .addDimension(syntheticDimension)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      metadataItemsHandler.addMetadata(grid, params, List.of());

      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey("kO3z4Dhc038.C31vHZqu0qU"));
      assertFalse(dimensions.containsKey("LFsZ8v5v7rq"));

      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey("kO3z4Dhc038.C31vHZqu0qU"));
      assertFalse(items.containsKey("LFsZ8v5v7rq"));
    }

    @Test
    @DisplayName("should include resolved org unit item for stage ou aggregate dimension")
    void shouldIncludeResolvedOrgUnitItemForStageOuAggregateDimension() {
      // Given
      Grid grid = new ListGrid();

      ProgramStage programStage = createProgramStage('S', programA);
      programStage.setUid("A03MvHHogjR");
      programStage.setName("Birth");

      QueryItem stageOuItem =
          new QueryItem(
                  new BaseDimensionalItemObject("ou"),
                  programA,
                  null,
                  ValueType.ORGANISATION_UNIT,
                  AggregationType.NONE,
                  null)
              .withCustomHeader(AnalyticsCustomHeader.forOrgUnit(programStage));
      stageOuItem.setProgramStage(programStage);
      stageOuItem.addFilter(new QueryFilter(QueryOperator.IN, "USER_ORGUNIT"));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withUserOrgUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(stageOuItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.resolveOrgUnits(
              any(EventQueryParams.class), any(QueryItem.class)))
          .thenReturn(List.of(orgUnitA.getUid()));
      when(organisationUnitResolver.resolveOrgUnitsForMetadata(
              any(EventQueryParams.class), any(QueryItem.class)))
          .thenReturn(List.of(orgUnitA.getUid()));
      when(organisationUnitResolver.loadOrgUnitDimensionalItem(orgUnitA.getUid(), IdScheme.UID))
          .thenReturn(orgUnitA);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);

      assertTrue(items.containsKey("A03MvHHogjR.ou"));
      assertTrue(items.containsKey(orgUnitA.getUid()));

      MetadataItem orgUnitItem = (MetadataItem) items.get(orgUnitA.getUid());
      assertEquals(orgUnitA.getDisplayName(), orgUnitItem.getName());
    }
  }

  @Nested
  @DisplayName("Organisation Unit Hierarchy Tests")
  class OrgUnitHierarchyTests {

    @Test
    @DisplayName("should add org unit hierarchy when hierarchyMeta is true")
    void shouldAddOrgUnitHierarchyWhenHierarchyMetaIsTrue() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue(orgUnitA.getUid());

      User mockUser = mock(User.class);
      when(mockUser.getOrganisationUnits()).thenReturn(Set.of(orgUnitA));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withHierarchyMeta(true)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(securityManager.getCurrentUser(params)).thenReturn(mockUser);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertTrue(grid.getMetaData().containsKey(ORG_UNIT_HIERARCHY.getKey()));
    }

    @Test
    @DisplayName("should add org unit name hierarchy when showHierarchy is true")
    void shouldAddOrgUnitNameHierarchyWhenShowHierarchyIsTrue() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue(orgUnitA.getUid());

      User mockUser = mock(User.class);
      when(mockUser.getOrganisationUnits()).thenReturn(Set.of(orgUnitA));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withShowHierarchy(true)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(securityManager.getCurrentUser(params)).thenReturn(mockUser);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertTrue(grid.getMetaData().containsKey(ORG_UNIT_NAME_HIERARCHY.getKey()));
    }

    @Test
    @DisplayName("should not add org unit hierarchy when both flags are false")
    void shouldNotAddOrgUnitHierarchyWhenFlagsAreFalse() {
      // Given
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withHierarchyMeta(false)
              .withShowHierarchy(false)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertFalse(grid.getMetaData().containsKey(ORG_UNIT_HIERARCHY.getKey()));
      assertFalse(grid.getMetaData().containsKey(ORG_UNIT_NAME_HIERARCHY.getKey()));
    }
  }

  @Nested
  @DisplayName("Value Dimension Tests")
  class ValueDimensionTests {

    @Test
    @DisplayName("should add value dimension metadata when present")
    void shouldAddValueDimensionMetadataWhenPresent() {
      // Given
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withValue(dataElementA)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey(dataElementA.getUid()));
    }
  }

  @Nested
  @DisplayName("Include Metadata Details Tests")
  class IncludeMetadataDetailsTests {

    @Test
    @DisplayName("should include metadata details when flag is true")
    void shouldIncludeMetadataDetailsWhenFlagIsTrue() {
      // Given
      Grid grid = new ListGrid();

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              legendSetA,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withIncludeMetadataDetails(true)
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);

      // Check that legend items include uid (details)
      MetadataItem legendItem = (MetadataItem) items.get(legendA.getUid());
      assertNotNull(legendItem);
      assertNotNull(legendItem.getUid());
    }

    @Test
    @DisplayName("should not include metadata details when flag is false")
    void shouldNotIncludeMetadataDetailsWhenFlagIsFalse() {
      // Given
      Grid grid = new ListGrid();

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              legendSetA,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withIncludeMetadataDetails(false)
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);

      MetadataItem legendItem = (MetadataItem) items.get(legendA.getUid());
      assertNotNull(legendItem);
      assertNull(legendItem.getUid());
    }
  }

  @Nested
  @DisplayName("Item Filters Tests")
  class ItemFiltersTests {

    @Test
    @DisplayName("should process item filters with option sets")
    void shouldProcessItemFiltersWithOptionSets() {
      // Given
      Grid grid = new ListGrid();

      QueryItem filterItem =
          new QueryItem(
              dataElementB,
              null,
              dataElementB.getValueType(),
              dataElementB.getAggregationType(),
              optionSetA);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItemFilter(filterItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey(dataElementB.getUid()));
    }

    @Test
    @DisplayName("should process item filters with legend sets")
    void shouldProcessItemFiltersWithLegendSets() {
      // Given
      Grid grid = new ListGrid();

      QueryItem filterItem =
          new QueryItem(
              dataElementB,
              legendSetA,
              dataElementB.getValueType(),
              dataElementB.getAggregationType(),
              null);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItemFilter(filterItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey(dataElementB.getUid()));
    }
  }

  @Nested
  @DisplayName("Organisation Unit Value Type Tests")
  class OrgUnitValueTypeTests {

    @Test
    @DisplayName("should resolve org unit items for org unit value type")
    void shouldResolveOrgUnitItemsForOrgUnitValueType() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue(orgUnitA.getUid());

      DataElement orgUnitDataElement = createDataElement('C');
      orgUnitDataElement.setValueType(ValueType.ORGANISATION_UNIT);

      QueryItem queryItem =
          new QueryItem(
              orgUnitDataElement,
              null,
              orgUnitDataElement.getValueType(),
              orgUnitDataElement.getAggregationType(),
              null);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.resolveOrgUnitsForMetadata(
              any(EventQueryParams.class), any(QueryItem.class)))
          .thenReturn(List.of(orgUnitA.getUid()));
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      verify(organisationUnitResolver)
          .resolveOrgUnitsForMetadata(any(EventQueryParams.class), any(QueryItem.class));
    }
  }

  @Nested
  @DisplayName("Empty Results Tests")
  class EmptyResultsTests {

    @Test
    @DisplayName("should handle empty grid rows for query endpoint")
    void shouldHandleEmptyGridRowsForQueryEndpoint() {
      // Given
      Grid grid = new ListGrid(); // Empty grid with no rows

      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              optionSetA);

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      assertTrue(grid.getMetaData().containsKey(ITEMS.getKey()));
    }
  }

  @Nested
  @DisplayName("Display Property Tests")
  class DisplayPropertyTests {

    @Test
    @DisplayName("should use display property for metadata item names")
    void shouldUseDisplayPropertyForMetadataItemNames() {
      // Given
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withDisplayProperty(DisplayProperty.SHORTNAME)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      assertNotNull(grid.getMetaData());
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
    }
  }

  @Nested
  @DisplayName("Custom Dimension Tests")
  class CustomDimensionTests {

    @Test
    @DisplayName("should include custom dimensions in metadata")
    void shouldIncludeCustomDimensionsInMetadata() {
      // Given
      Grid grid = new ListGrid();

      BaseDimensionalObject customDim =
          new BaseDimensionalObject("customDim", DimensionType.CATEGORY, List.of());

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addDimension(customDim)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey("customDim"));
    }
  }

  @Nested
  @DisplayName("Date Dimension Value Tests")
  class DateDimensionValueTests {

    @Test
    @DisplayName("should include dimension values for date items with period identifiers")
    void shouldIncludeDimensionValuesForDateItems() {
      // Given
      Grid grid = new ListGrid();

      org.hisp.dhis.program.ProgramStage programStage = createProgramStage('S', programA);
      programStage.setUid("A03MvHHogjR");

      org.hisp.dhis.common.BaseDimensionalItemObject eventDateItem =
          new org.hisp.dhis.common.BaseDimensionalItemObject("occurreddate");
      eventDateItem.setUid("occurreddate");
      eventDateItem.setName("Event date");

      QueryItem queryItem = new QueryItem(eventDateItem, null, ValueType.DATE, null, null);
      queryItem.setProgramStage(programStage);
      queryItem.addDimensionValue("202205");
      queryItem.setCustomHeader(
          org.hisp.dhis.common.AnalyticsCustomHeader.forEventDate(programStage));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);

      assertTrue(
          dimensions.containsKey("A03MvHHogjR.eventdate"),
          "Dimensions should contain key 'A03MvHHogjR.eventdate'");
      assertEquals(
          List.of("202205"),
          dimensions.get("A03MvHHogjR.eventdate"),
          "Dimension values should contain the period identifier '202205'");
    }

    @Test
    @DisplayName("should add period metadata items for date dimension values")
    void shouldAddPeriodMetadataItemsForDateDimensionValues() {
      // Given
      Grid grid = new ListGrid();

      org.hisp.dhis.program.ProgramStage programStage = createProgramStage('S', programA);
      programStage.setUid("A03MvHHogjR");

      org.hisp.dhis.common.BaseDimensionalItemObject eventDateItem =
          new org.hisp.dhis.common.BaseDimensionalItemObject("occurreddate");
      eventDateItem.setUid("occurreddate");
      eventDateItem.setName("Event date");

      QueryItem queryItem = new QueryItem(eventDateItem, null, ValueType.DATE, null, null);
      queryItem.setProgramStage(programStage);
      queryItem.addDimensionValue("202205");
      queryItem.setCustomHeader(
          org.hisp.dhis.common.AnalyticsCustomHeader.forEventDate(programStage));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);

      assertTrue(
          items.containsKey("202205"), "Items should contain period metadata entry for '202205'");
      MetadataItem periodItem = (MetadataItem) items.get("202205");
      assertNotNull(periodItem.getName(), "Period metadata item should have a name");
    }

    @Test
    @DisplayName(
        "should not replace existing metadata when period dimension value key already exists")
    void shouldNotReplaceExistingMetadataForDuplicatePeriodDimensionValueKey() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("value1");

      DimensionItemKeywords dimensionItemKeywords = new DimensionItemKeywords();
      dimensionItemKeywords.addKeyword("202205", "Existing Keyword Name");
      Keyword keyword = dimensionItemKeywords.getKeyword("202205");

      QueryItem queryItem =
          new QueryItem(
              dataElementA, null, ValueType.DATE, dataElementA.getAggregationType(), null);
      queryItem.addDimensionValue("202205");

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of(keyword));

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);

      MetadataItem periodItem = (MetadataItem) items.get("202205");
      assertNotNull(periodItem);
      assertEquals(
          "Existing Keyword Name",
          periodItem.getName(),
          "Existing metadata should not be replaced by period value metadata");
    }

    @Test
    @DisplayName("should ignore invalid period identifiers in date dimension values")
    void shouldIgnoreInvalidPeriodIdentifiersInDateDimensionValues() {
      // Given
      Grid grid = new ListGrid();

      QueryItem queryItem =
          new QueryItem(
              dataElementA, null, ValueType.DATE, dataElementA.getAggregationType(), null);
      queryItem.addDimensionValue("not-a-period");

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertFalse(items.containsKey("not-a-period"));
    }

    @Test
    @DisplayName("should ignore dimension values for non-date items")
    void shouldIgnoreDimensionValuesForNonDateItems() {
      // Given
      Grid grid = new ListGrid();

      QueryItem queryItem =
          new QueryItem(
              dataElementA, null, ValueType.TEXT, dataElementA.getAggregationType(), null);
      queryItem.addDimensionValue("202205");

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertFalse(items.containsKey("202205"));
    }
  }

  @Nested
  @DisplayName("Enrollment OU Dimension Tests")
  class EnrollmentOuDimensionTests {

    @Test
    @DisplayName("should include enrollment OU dimension items and metadata")
    void shouldIncludeEnrollmentOuDimensionItemsAndMetadata() {
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(AGGREGATE)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .withEnrollmentOuDimension(List.of(orgUnitA, orgUnitB))
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);

      metadataItemsHandler.addMetadata(grid, params, List.of());

      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey("enrollmentou"));
      assertEquals(2, dimensions.get("enrollmentou").size());
      assertTrue(dimensions.get("enrollmentou").contains(orgUnitA.getUid()));
      assertTrue(dimensions.get("enrollmentou").contains(orgUnitB.getUid()));

      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);
      assertTrue(items.containsKey(orgUnitA.getUid()));
      assertTrue(items.containsKey(orgUnitB.getUid()));
    }
  }

  @Nested
  @DisplayName("Program Status Metadata Tests")
  class ProgramStatusMetadataTests {

    @Test
    @DisplayName("should include programstatus key in dimensions with selected statuses")
    void shouldIncludeProgramStatusDimensions() {
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withEnrollmentStatuses(
                  new LinkedHashSet<>(List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)))
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      metadataItemsHandler.addMetadata(grid, params, List.of());

      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());

      assertNotNull(dimensions);
      assertTrue(dimensions.containsKey("programstatus"));
      assertEquals(List.of("ACTIVE", "COMPLETED"), dimensions.get("programstatus"));
    }

    @Test
    @DisplayName("should include programstatus and enrollment status items metadata")
    void shouldIncludeProgramStatusItemsMetadata() {
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withEnrollmentStatuses(new LinkedHashSet<>(List.of(EnrollmentStatus.ACTIVE)))
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      metadataItemsHandler.addMetadata(grid, params, List.of());

      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());

      assertNotNull(items);
      assertTrue(items.containsKey("programstatus"));
      assertEquals("Program status", ((MetadataItem) items.get("programstatus")).getName());

      assertTrue(items.containsKey("ACTIVE"));
      assertEquals("Active", ((MetadataItem) items.get("ACTIVE")).getName());

      assertFalse(items.containsKey("COMPLETED"));
      assertFalse(items.containsKey("CANCELLED"));
    }

    @Test
    @DisplayName("should include programstatus dimensions and items for event query endpoint")
    void shouldIncludeProgramStatusMetadataForEventQueryEndpoint() {
      Grid grid = new ListGrid();

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withEndpointItem(EVENT)
              .withEnrollmentStatuses(new LinkedHashSet<>(List.of(EnrollmentStatus.ACTIVE)))
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      metadataItemsHandler.addMetadata(grid, params, List.of());

      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());

      assertNotNull(dimensions);
      assertEquals(List.of("ACTIVE"), dimensions.get("programstatus"));

      assertNotNull(items);
      assertEquals("Program status", ((MetadataItem) items.get("programstatus")).getName());
      assertEquals("Active", ((MetadataItem) items.get("ACTIVE")).getName());
    }
  }

  @Nested
  @DisplayName("Custom Header Tests")
  class CustomHeaderTests {

    @Test
    @DisplayName("should use headerKey for metadata item key when item has custom header")
    void shouldUseHeaderKeyForMetadataItemKeyWhenItemHasCustomHeader() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("2024-01-01");

      org.hisp.dhis.program.ProgramStage programStage = createProgramStage('S', programA);
      programStage.setUid("A03MvHHogjR");
      programStage.setName("Birth");

      org.hisp.dhis.common.BaseDimensionalItemObject eventDateItem =
          new org.hisp.dhis.common.BaseDimensionalItemObject("occurreddate");
      eventDateItem.setUid("occurreddate");
      eventDateItem.setName("Event date");

      QueryItem queryItem = new QueryItem(eventDateItem, null, ValueType.DATETIME, null, null);
      queryItem.setProgramStage(programStage);
      queryItem.setCustomHeader(
          org.hisp.dhis.common.AnalyticsCustomHeader.forEventDate(programStage));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get(ITEMS.getKey());
      assertNotNull(items);

      // Should use headerKey() which converts "A03MvHHogjR.EVENT_DATE" -> "A03MvHHogjR.eventdate"
      assertTrue(
          items.containsKey("A03MvHHogjR.eventdate"),
          "Items should contain key 'A03MvHHogjR.eventdate' (from headerKey())");

      // Should NOT use raw key() which is "A03MvHHogjR.EVENT_DATE"
      assertFalse(
          items.containsKey("A03MvHHogjR.EVENT_DATE"),
          "Items should NOT contain raw key 'A03MvHHogjR.EVENT_DATE'");

      // Should NOT use the underlying UID "occurreddate"
      assertFalse(
          items.containsKey("A03MvHHogjR.occurreddate"),
          "Items should NOT contain the underlying UID 'A03MvHHogjR.occurreddate'");

      // Verify the metadata item value - should be just the label, NOT "Event date, Birth"
      MetadataItem metadataItem = (MetadataItem) items.get("A03MvHHogjR.eventdate");
      assertNotNull(metadataItem);
      assertEquals(
          "Event date",
          metadataItem.getName(),
          "Metadata item name should be just 'Event date' (not including stage name)");
    }

    @Test
    @DisplayName("should use headerKey for dimension key when item has custom header")
    void shouldUseHeaderKeyForDimensionKeyWhenItemHasCustomHeader() {
      // Given
      Grid grid = new ListGrid();
      grid.addRow();
      grid.addValue("2024-01-01");

      org.hisp.dhis.program.ProgramStage programStage = createProgramStage('S', programA);
      programStage.setUid("A03MvHHogjR");
      programStage.setName("Birth");

      org.hisp.dhis.common.BaseDimensionalItemObject eventDateItem =
          new org.hisp.dhis.common.BaseDimensionalItemObject("occurreddate");
      eventDateItem.setUid("occurreddate");
      eventDateItem.setName("Event date");

      QueryItem queryItem = new QueryItem(eventDateItem, null, ValueType.DATETIME, null, null);
      queryItem.setProgramStage(programStage);
      queryItem.setCustomHeader(
          org.hisp.dhis.common.AnalyticsCustomHeader.forEventDate(programStage));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withProgram(programA)
              .withSkipMeta(false)
              .withEndpointAction(QUERY)
              .withOrganisationUnits(List.of(orgUnitA))
              .withPeriods(createPeriodDimensions("2023Q1"), "quarterly")
              .addItem(queryItem)
              .build();

      when(userService.getUserByUsername(anyString())).thenReturn(null);
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get(DIMENSIONS.getKey());
      assertNotNull(dimensions);

      // Should use headerKey() which converts "A03MvHHogjR.EVENT_DATE" -> "A03MvHHogjR.eventdate"
      assertTrue(
          dimensions.containsKey("A03MvHHogjR.eventdate"),
          "Dimensions should contain key 'A03MvHHogjR.eventdate' (from headerKey())");
    }
  }
}
