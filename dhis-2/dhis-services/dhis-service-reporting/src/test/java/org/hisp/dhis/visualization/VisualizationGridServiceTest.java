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
package org.hisp.dhis.visualization;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VisualizationGridServiceTest {
  @Mock private VisualizationService visualizationService;

  @Mock private AnalyticsService analyticsService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CurrentUserService currentUserService;

  @Mock private I18nManager i18nManager;

  private VisualizationGridService visualizationGridService;

  @BeforeEach
  public void setUp() {
    visualizationGridService =
        new DefaultVisualizationGridService(
            visualizationService,
            analyticsService,
            organisationUnitService,
            currentUserService,
            i18nManager);
  }

  @Test
  void getVisualizationGridByUserWhenItHasOrganisationUnitLevels() {
    // Given
    final String anyVisualizationUid = "adbet5RTs";
    final Date anyRelativePeriodDate = new Date();
    final String anyOrganisationUnitUid = "ouiRzW5e";
    final User userStub = userStub();
    final List<Integer> orgUnitLevels = asList(1, 2);
    final List<OrganisationUnit> orgUnits = asList(new OrganisationUnit());
    final Map<String, Object> valueMap = valueMapStub();

    final Visualization visualizationStub = visualizationStub("abc123xy");
    visualizationStub.setOrganisationUnitLevels(orgUnitLevels);
    visualizationStub.setOrganisationUnits(orgUnits);
    final Visualization visualizationSpy = spy(visualizationStub);

    // When
    when(visualizationService.getVisualization(anyVisualizationUid)).thenReturn(visualizationSpy);
    when(analyticsService.getAggregatedDataValueMapping(visualizationSpy)).thenReturn(valueMap);
    final Grid expectedGrid =
        visualizationGridService.getVisualizationGrid(
            anyVisualizationUid, anyRelativePeriodDate, anyOrganisationUnitUid, userStub);

    // Then
    assertThat(expectedGrid.getRows(), hasSize(1));
    assertThat(expectedGrid.getRows().get(0), hasSize(7));
    assertThat(expectedGrid.getRows().get(0), hasItem("abc123xy"));
    assertThat(expectedGrid.getHeaders(), hasSize(7));
    assertThat(expectedGrid.getMetaColumnIndexes(), hasSize(7));
    assertThatHeadersAreTheExpectedOnes(expectedGrid);

    verify(organisationUnitService, times(1)).getOrganisationUnitsAtLevels(orgUnitLevels, orgUnits);
    verify(visualizationSpy, times(1)).clearTransientState();
  }

  @Test
  void getVisualizationGridByUserWhenItHasItemOrganisationUnitGroups() {
    // Given
    final String anyVisualizationUid = "adbet5RTs";
    final Date anyRelativePeriodDate = new Date();
    final String anyOrganisationUnitUid = "ouiRzW5e";
    final User userStub = userStub();
    final List<OrganisationUnit> orgUnits = asList(new OrganisationUnit());
    final List<OrganisationUnitGroup> orgUnitGroups = asList(new OrganisationUnitGroup());
    final Map<String, Object> valueMap = valueMapStub();

    final Visualization visualizationStub = visualizationStub("abc123xy");
    visualizationStub.setOrganisationUnits(orgUnits);
    visualizationStub.setItemOrganisationUnitGroups(orgUnitGroups);
    final Visualization visualizationSpy = spy(visualizationStub);

    // When
    when(visualizationService.getVisualization(anyVisualizationUid)).thenReturn(visualizationSpy);
    when(analyticsService.getAggregatedDataValueMapping(visualizationSpy)).thenReturn(valueMap);
    final Grid expectedGrid =
        visualizationGridService.getVisualizationGrid(
            anyVisualizationUid, anyRelativePeriodDate, anyOrganisationUnitUid, userStub);

    // Then
    assertThat(expectedGrid.getRows(), hasSize(1));
    assertThat(expectedGrid.getRows().get(0), hasSize(7));
    assertThat(expectedGrid.getRows().get(0), hasItem("abc123xy"));
    assertThat(expectedGrid.getHeaders(), hasSize(7));
    assertThat(expectedGrid.getMetaColumnIndexes(), hasSize(7));
    assertThatHeadersAreTheExpectedOnes(expectedGrid);

    verify(organisationUnitService, times(1)).getOrganisationUnits(orgUnitGroups, orgUnits);
    verify(visualizationSpy, times(1)).clearTransientState();
  }

  private void assertThatHeadersAreTheExpectedOnes(Grid expectedGrid) {
    final List<GridHeader> gridHeaders = expectedGrid.getHeaders();
    assertThat("Header must be present: dataid", gridContains(gridHeaders, "dataid"));
    assertThat("Header must be present: dataname", gridContains(gridHeaders, "dataname"));
    assertThat("Header must be present: datacode", gridContains(gridHeaders, "datacode"));
    assertThat(
        "Header must be present: datadescription", gridContains(gridHeaders, "datadescription"));
    assertThat(
        "Header must be present: reporting_month_name",
        gridContains(gridHeaders, "reporting_month_name"));
    assertThat(
        "Header must be present: param_organisationunit_name",
        gridContains(gridHeaders, "param_organisationunit_name"));
    assertThat(
        "Header must be present: organisation_unit_is_parent",
        gridContains(gridHeaders, "organisation_unit_is_parent"));
  }

  private boolean gridContains(final List<GridHeader> gridHeaders, final String columnHeader) {
    return gridHeaders.removeIf(gridHeader -> gridHeader.getColumn().equals(columnHeader));
  }

  private User userStub() {
    final User userStub = new User();
    userStub.setName("John");
    userStub.setSurname("Rambo");
    return userStub;
  }

  private Visualization visualizationStub(final String dimensionItem) {
    final List<String> rowsDimensions = asList("dx");
    final List<DimensionalItemObject> dimensionalItemObjects =
        asList(baseDimensionalItemObjectStub(dimensionItem));

    final Visualization visualization = new Visualization();
    visualization.setRowDimensions(rowsDimensions);
    visualization.setGridRows(asList(dimensionalItemObjects));
    return visualization;
  }

  private Map<String, Object> valueMapStub() {
    final Map<String, Object> valueMap = new HashMap<>();
    valueMap.put("key1", "value1");
    return valueMap;
  }

  private BaseDimensionalItemObject baseDimensionalItemObjectStub(final String dimensionItem) {
    final BaseDimensionalItemObject baseDimensionalItemObject =
        new BaseDimensionalItemObject(dimensionItem);
    baseDimensionalItemObject.setDescription("display " + dimensionItem);
    return baseDimensionalItemObject;
  }
}
