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
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createLegend;
import static org.hisp.dhis.test.TestBase.createLegendSet;
import static org.hisp.dhis.test.TestBase.createOption;
import static org.hisp.dhis.test.TestBase.createOptionSet;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.hisp.dhis.test.TestBase.createProgram;
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

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
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
    programA = createProgram('A', null, null, Sets.newHashSet(orgUnitA), null);

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
      when(organisationUnitResolver.resolveOrgUnits(
              any(EventQueryParams.class), any(QueryItem.class)))
          .thenReturn(List.of(orgUnitA.getUid()));
      when(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(any()))
          .thenReturn(Map.of());

      // When
      metadataItemsHandler.addMetadata(grid, params, List.of());

      // Then
      verify(organisationUnitResolver)
          .resolveOrgUnits(any(EventQueryParams.class), any(QueryItem.class));
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
}
