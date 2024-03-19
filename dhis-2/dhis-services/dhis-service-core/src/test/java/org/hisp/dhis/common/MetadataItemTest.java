/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class MetadataItemTest {
  private static final String serverUrl = "http://localhost:8080/dhis";

  @Test
  void testCreateForDataElement() {
    DataElement deA = createDataElement('A');
    deA.setValueType(ValueType.INTEGER_ZERO_OR_POSITIVE);
    deA.setAggregationType(AggregationType.AVERAGE_SUM_ORG_UNIT);

    MetadataItem miA = new MetadataItem("MIA", deA);

    assertEquals("MIA", miA.getName());
    assertEquals(ValueType.NUMBER, miA.getValueType());
    assertEquals(AggregationType.AVERAGE_SUM_ORG_UNIT, miA.getAggregationType());
  }

  @Test
  void testCreateForDataElementOperand() {
    DataElement deA = createDataElement('A');
    deA.setValueType(ValueType.BOOLEAN);
    deA.setAggregationType(AggregationType.COUNT);

    CategoryOptionCombo cocA = createCategoryOptionCombo('A');

    DataElementOperand doA = new DataElementOperand(deA, cocA);

    MetadataItem miA = new MetadataItem("MIA", doA);

    assertEquals("MIA", miA.getName());
    assertEquals(ValueType.BOOLEAN, miA.getValueType());
    assertEquals(AggregationType.COUNT, miA.getAggregationType());
  }

  @Test
  void testIconUrlForIndicator() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    Indicator indicator = createIndicator('A', new IndicatorType());
    indicator.setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, indicator);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForProgramIndicator() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    ProgramIndicator indicator = createProgramIndicator('A', createProgram('B'), "", "");
    indicator.setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, indicator);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForDataElement() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    DataElement dataElement =
        createDataElement('A', ValueType.TEXT, AggregationType.COUNT, DataElementDomain.AGGREGATE);
    dataElement.setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, dataElement);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForDataSet() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    DataSet dataElement = createDataSet('A');
    dataElement.setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, dataElement);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForTrackedEntityAttribute() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    TrackedEntityAttribute tea = createTrackedEntityAttribute('A');
    tea.setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, tea);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForReportingRate() {

    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    ReportingRate rr = new ReportingRate(createDataSet('A'));
    rr.getDataSet().setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, rr);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForProgramDataElementDimensionItem() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    ProgramDataElementDimensionItem dimensionItem =
        new ProgramDataElementDimensionItem(createProgram('A'), createDataElement('B'));
    dimensionItem.getDataElement().setStyle(style);

    // When
    MetadataItem item = new MetadataItem("any-name", serverUrl, dimensionItem);

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForDataDimensionItemWithDataElement() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    DataDimensionItem dimensionItem = new DataDimensionItem();
    dimensionItem.setDataElement(createDataElement('A'));
    dimensionItem.getDataElement().setStyle(style);

    // When
    MetadataItem item =
        new MetadataItem("any-name", serverUrl, dimensionItem.getDimensionalItemObject());

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForDataDimensionItemWithIndicator() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    DataDimensionItem dimensionItem = new DataDimensionItem();
    dimensionItem.setIndicator(createIndicator('A', new IndicatorType("test", 1, true)));
    dimensionItem.getIndicator().setStyle(style);

    // When
    MetadataItem item =
        new MetadataItem("any-name", serverUrl, dimensionItem.getDimensionalItemObject());

    // Then
    assertMetadataItemIconPath(item, style);
  }

  @Test
  void testIconUrlForDataDimensionItemWithProgramIndicator() {
    // Given
    ObjectStyle style = new ObjectStyle();
    style.setIcon("icon-name");

    DataDimensionItem dimensionItem = new DataDimensionItem();
    dimensionItem.setProgramIndicator(
        createProgramIndicator('A', createProgram('A'), "test_exp", ""));
    dimensionItem.getProgramIndicator().setStyle(style);

    // When
    MetadataItem item =
        new MetadataItem("any-name", serverUrl, dimensionItem.getDimensionalItemObject());

    // Then
    assertMetadataItemIconPath(item, style);
  }

  private void assertMetadataItemIconPath(MetadataItem metadataItem, ObjectStyle style) {
    assertEquals("any-name", metadataItem.getName());
    assertEquals(serverUrl, metadataItem.getServerBaseUrl());
    assertEquals(style.getIcon(), metadataItem.getStyle().getIcon());
    assertEquals(serverUrl + "/api/icons/icon-name/icon.svg", metadataItem.getStyle().getIcon());
  }
}
