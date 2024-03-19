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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.Sorting;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MapViewRenderingStrategy;
import org.hisp.dhis.mapping.MapViewStore;
import org.hisp.dhis.mapping.ThematicMapType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Lars Helge Overland
 */
class AnalyticalObjectStoreTest extends TransactionalIntegrationTest {

  private IndicatorType itA;

  private Indicator inA;

  private Indicator inB;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private MapView mvA;

  private MapView mvB;

  private MapView mvC;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired
  @Qualifier("org.hisp.dhis.mapping.MapViewStore")
  private MapViewStore mapViewStore;

  @Autowired private AnalyticalObjectStore<Visualization> visualizationStore;

  @Override
  public void setUpTest() {
    itA = createIndicatorType('A');
    idObjectManager.save(itA);
    inA = createIndicator('A', itA);
    inB = createIndicator('B', itA);
    idObjectManager.save(inA);
    idObjectManager.save(inB);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    mvA = createMapView(MapView.LAYER_THEMATIC1);
    mvB = createMapView(MapView.LAYER_THEMATIC2);
    mvC = createMapView(MapView.LAYER_THEMATIC3);
    mvA.addDataDimensionItem(inA);
    mvA.getOrganisationUnits().add(ouA);
    mvB.addDataDimensionItem(inB);
    mvB.getOrganisationUnits().add(ouA);
    mvC.addDataDimensionItem(inA);
    mvC.getOrganisationUnits().add(ouB);

    mvA.setOrgUnitField("OU_Coordinate_Field");

    mapViewStore.save(mvA);
    mapViewStore.save(mvB);
    mapViewStore.save(mvC);
  }

  @Test
  void testGetByIndicator() {
    List<MapView> actual = mapViewStore.getAnalyticalObjects(inA);
    assertEquals(2, actual.size());
    assertTrue(actual.contains(mvA));
    assertTrue(actual.contains(mvC));
  }

  @Test
  void testGetByOrgansiationUnit() {
    List<MapView> actual = mapViewStore.getAnalyticalObjects(ouA);
    assertEquals(2, actual.size());
    assertTrue(actual.contains(mvA));
    assertTrue(actual.contains(mvB));
  }

  @Test
  void testAssertProperties() {
    MapView mapView = mapViewStore.getByUid(mvA.getUid());
    assertEquals(AggregationType.SUM, mapView.getAggregationType());
    assertEquals(ThematicMapType.CHOROPLETH, mapView.getThematicMapType());
    assertEquals(ProgramStatus.COMPLETED, mapView.getProgramStatus());
    assertEquals(
        OrganisationUnitSelectionMode.DESCENDANTS, mapView.getOrganisationUnitSelectionMode());
    assertEquals(MapViewRenderingStrategy.SINGLE, mapView.getRenderingStrategy());
    assertEquals(UserOrgUnitType.DATA_CAPTURE, mapView.getUserOrgUnitType());
    assertEquals("#ddeeff", mapView.getNoDataColor());
  }

  @Test
  void testSaveOrganisationUnitCoordinateField() {
    MapView mapView = mapViewStore.getByUid(mvA.getUid());
    assertEquals("OU_Coordinate_Field", mapView.getOrgUnitField());
  }

  @Test
  @DisplayName(
      "Get all visualizations that have a specific indicator as a dimension in its sorting column")
  void getVisualizationsWithIndicatorSortingTest() {
    // given 3 visualization exist, 2 of which have specific indicator sorting dimensions
    Visualization vizWith2IndicatorInSorting = createVisualization('z');
    Visualization vizWith1IndicatorInSorting = createVisualization('w');
    Visualization vizWithNoIndicatorSorting = createVisualization('x');
    Indicator indicatorDimension1 = createIndicator('z', itA);
    Indicator indicatorDimension2 = createIndicator('y', itA);
    Sorting sorting1 = new Sorting();
    sorting1.setDimension(indicatorDimension1.getUid());
    Sorting sorting2 = new Sorting();
    sorting2.setDimension(indicatorDimension2.getUid());

    // set 2 visualizations to have indicator sorting dimensions
    vizWith2IndicatorInSorting.setSorting(List.of(sorting1, sorting2));
    vizWith1IndicatorInSorting.setSorting(List.of(sorting2));

    idObjectManager.save(indicatorDimension1);
    idObjectManager.save(indicatorDimension2);
    idObjectManager.save(vizWith2IndicatorInSorting);
    idObjectManager.save(vizWith1IndicatorInSorting);
    idObjectManager.save(vizWithNoIndicatorSorting);

    // when looking for specific visualizations with indicator refs
    List<Visualization> matchedVisualizations =
        visualizationStore.getVisualizationsBySortingIndicator(
            List.of(indicatorDimension1.getUid(), indicatorDimension2.getUid()));

    // then only the 2 matching visualizations should be retrieved
    assertEquals(2, matchedVisualizations.size());
    assertTrue(matchedVisualizations.contains(vizWith1IndicatorInSorting));
    assertTrue(matchedVisualizations.contains(vizWith2IndicatorInSorting));

    // and the visualization with no indicator ref should not be retrieved
    assertFalse(matchedVisualizations.contains(vizWithNoIndicatorSorting));
  }
}
