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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.AggregationType.COUNT;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengis.geometry.primitive.Point;

/**
 * Unit tests for {@link EnrollmentAggregateService}.
 *
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentAggregateServiceTest {

  @Mock private AnalyticsSecurityManager securityManager;

  @Mock private EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  @Mock private EventQueryValidator queryValidator;

  @Mock private EventQueryPlanner queryPlanner;

  @Mock private MetadataItemsHandler metadataHandler;

  @Mock private SchemeIdHandler schemeIdHandler;

  @InjectMocks private EnrollmentAggregateService service;

  @BeforeAll
  static void setup() {
    injectSecurityContextNoSettings(new SystemUser());
  }

  @Test
  void verifyHeaderCreationBasedOnQueryItemsAndDimensions() {
    // Given
    OrganisationUnit ouA = createOrganisationUnit('A');
    DataElement deA = createDataElement('A', TEXT, NONE);
    DataElement deB = createDataElement('B', ValueType.ORGANISATION_UNIT, NONE);
    DataElement deC = createDataElement('C', NUMBER, COUNT);
    DimensionalObject periods = new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, createPeriodDimensions("201701"));
    DimensionalObject orgUnits =
        new BaseDimensionalObject(
            ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, "ouA", List.of(ouA));
    QueryItem qiA = new QueryItem(deA, null, deA.getValueType(), deA.getAggregationType(), null);
    QueryItem qiB = new QueryItem(deB, null, deB.getValueType(), deB.getAggregationType(), null);
    QueryItem qiC = new QueryItem(deC, null, deC.getValueType(), deC.getAggregationType(), null);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(periods)
            .addDimension(orgUnits)
            .addItem(qiA)
            .addItem(qiB)
            .addItem(qiC)
            .withCoordinateFields(List.of(deB.getUid()))
            .withSkipData(true)
            .withSkipMeta(false)
            .build();

    // When
    when(securityManager.withUserConstraints(any(EventQueryParams.class))).thenReturn(params);
    Grid grid = service.getEnrollments(params);

    // Then
    List<GridHeader> headers = grid.getHeaders();
    assertThat(headers, is(notNullValue()));
    assertThat(headers, hasSize(6));

    assertHeaderWithColumn(headers.get(0), "value", "Value", NUMBER, Double.class.getName());
    assertHeaderWithColumn(headers.get(1), "ou", "ouA", TEXT, String.class.getName());
    assertHeaderWithColumn(headers.get(2), "pe", null, TEXT, String.class.getName());
    assertHeaderWithColumn(
        headers.get(3), deA.getUid(), deA.getName(), TEXT, String.class.getName());
    assertHeaderWithColumn(
        headers.get(4), deB.getUid(), deB.getName(), COORDINATE, Point.class.getName());
    assertHeaderWithColumn(
        headers.get(5), deC.getUid(), deC.getName(), NUMBER, Double.class.getName());
  }

  @Test
  void verifyHeaderCreationBasedOnQueryItemsAndDimensionsWithSameNamesMultiStage() {
    // Given
    OrganisationUnit ouA = createOrganisationUnit('A');

    ProgramStage psA = new ProgramStage("ps", new Program());
    psA.setUid("psA12345678");

    ProgramStage psB = new ProgramStage("ps", new Program());
    psB.setUid("psB12345678");

    DataElement deD = createDataElement('D', NUMBER, COUNT);
    deD.setName("same");

    DataElement deE = createDataElement('E', NUMBER, COUNT);
    deE.setName("same");

    DataElement deF = createDataElement('F', NUMBER, COUNT);
    deF.setName("unique");

    DataElement deB = createDataElement('B', ValueType.ORGANISATION_UNIT, NONE);
    DimensionalObject periods = new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, createPeriodDimensions("201701"));
    DimensionalObject orgUnits =
        new BaseDimensionalObject(
            ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, "ouA", List.of(ouA));

    QueryItem qiD = new QueryItem(deD, null, deD.getValueType(), deD.getAggregationType(), null);
    qiD.setProgramStage(psA);

    QueryItem qiE = new QueryItem(deE, null, deE.getValueType(), deE.getAggregationType(), null);
    qiE.setProgramStage(psB);

    QueryItem qiF = new QueryItem(deF, null, deF.getValueType(), deF.getAggregationType(), null);
    qiF.setProgramStage(psA);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(periods)
            .addDimension(orgUnits)
            .addItem(qiD)
            .addItem(qiE)
            .addItem(qiF)
            .withCoordinateFields(List.of(deB.getUid()))
            .withSkipData(true)
            .withSkipMeta(false)
            .build();

    // When
    when(securityManager.withUserConstraints(any(EventQueryParams.class))).thenReturn(params);
    Grid grid = service.getEnrollments(params);

    // Then
    List<GridHeader> headers = grid.getHeaders();
    assertThat(headers, is(notNullValue()));
    assertThat(headers, hasSize(6));

    assertHeaderWithColumn(headers.get(0), "value", "Value", NUMBER, Double.class.getName());
    assertHeaderWithColumn(headers.get(1), "ou", "ouA", TEXT, String.class.getName());
    assertHeaderWithColumn(headers.get(2), "pe", null, TEXT, String.class.getName());

    // Same item names with different program stages.
    assertHeaderWithDisplayColumn(
        headers.get(3),
        psA.getUid() + "." + deD.getUid(),
        deD.getName(),
        deD.getName() + " - " + psA.getName(),
        NUMBER,
        Double.class.getName());
    assertHeaderWithDisplayColumn(
        headers.get(4),
        psB.getUid() + "." + deE.getUid(),
        deE.getName(),
        deE.getName() + " - " + psB.getName(),
        NUMBER,
        Double.class.getName());
  }

  private void assertHeaderWithColumn(
      GridHeader expected, String name, String column, ValueType valueType, String type) {
    assertThat("Header name does not match", expected.getName(), is(name));
    assertThat("Header column name does not match", expected.getColumn(), is(column));
    assertThat("Header value type does not match", expected.getValueType(), is(valueType));
    assertThat("Header type does not match", expected.getType(), is(type));
  }

  private void assertHeaderWithDisplayColumn(
      GridHeader expected,
      String name,
      String column,
      String displayColumn,
      ValueType valueType,
      String type) {
    assertThat("Header name does not match", expected.getName(), is(name));
    assertThat("Header column name does not match", expected.getColumn(), is(column));
    assertThat(
        "Header displayColumn name does not match", expected.getDisplayColumn(), is(displayColumn));
    assertThat("Header value type does not match", expected.getValueType(), is(valueType));
    assertThat("Header type does not match", expected.getType(), is(type));
  }
}
