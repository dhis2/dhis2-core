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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createIndicatorType;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class QueryPlannerGroupByAggregationTypeTest {
  private QueryPlanner subject;

  @Mock private PartitionManager partitionManager;

  @BeforeEach
  public void setUp() {
    subject = new DefaultQueryPlanner(partitionManager);
  }

  @Test
  void verifyMultipleDataElementIsAggregatedWithTwoQueryGroupWhenDataTypeIsDifferent() {
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));
    // DataQueryParams with **two** DataElement with different data type as
    // dimension
    DataQueryParams queryParams =
        DataQueryParams.newBuilder()
            .withDimensions(
                // PERIOD DIMENSION
                List.of(
                    new BaseDimensionalObject("pe", DimensionType.PERIOD, periods),
                    new BaseDimensionalObject(
                        "dx",
                        DimensionType.DATA_X,
                        DISPLAY_NAME_DATA_X,
                        "display name",
                        List.of(
                            createDataElement('A', new CategoryCombo()),
                            createDataElement(
                                'B',
                                ValueType.TEXT,
                                AggregationType.COUNT,
                                DataElementDomain.AGGREGATE)))))
            .withFilters(
                List.of(
                    // OU FILTER
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")))))
            .withAggregationType(AnalyticsAggregationType.AVERAGE)
            .build();

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(2));

    assertThat(
        dataQueryGroups.getAllQueries(),
        hasItem(
            both(hasProperty(
                    "aggregationType", hasProperty("aggregationType", is(AggregationType.AVERAGE))))
                .and(
                    hasProperty(
                        "aggregationType", hasProperty("dataType", is(DataType.NUMERIC))))));

    assertThat(
        dataQueryGroups.getAllQueries(),
        hasItem(
            both(hasProperty(
                    "aggregationType", hasProperty("aggregationType", is(AggregationType.AVERAGE))))
                .and(hasProperty("aggregationType", hasProperty("dataType", is(DataType.TEXT))))));
  }

  @Test
  void verifySingleNonDataElementRetainAggregationTypeButNullDataType() {
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));
    // DataQueryParams with **one** Indicator
    DataQueryParams queryParams =
        DataQueryParams.newBuilder()
            .withDimensions(
                // PERIOD DIMENSION
                List.of(
                    new BaseDimensionalObject("pe", DimensionType.PERIOD, periods),
                    new BaseDimensionalObject(
                        "dx",
                        DimensionType.DATA_X,
                        DISPLAY_NAME_DATA_X,
                        "display name",
                        List.of(createIndicator('A', createIndicatorType('A'))))))
            .withFilters(
                List.of(
                    // OU FILTER
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")))))
            .withAggregationType(AnalyticsAggregationType.AVERAGE)
            .build();

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(1));

    assertThat(
        dataQueryGroups.getAllQueries(),
        hasItem(
            both(hasProperty(
                    "aggregationType", hasProperty("aggregationType", is(AggregationType.AVERAGE))))
                .and(hasProperty("aggregationType", hasProperty("dataType", is(nullValue()))))));
  }

  @Test
  void verifyASingleDataElementAsFilterRetainAggregationTypeAndAggregationDataType() {
    // DataQueryParams with **one** DataElement as filter
    DataQueryParams queryParams =
        createDataQueryParams(
            new BaseDimensionalObject(
                "dx",
                DimensionType.DATA_X,
                DISPLAY_NAME_DATA_X,
                "display name",
                List.of(createDataElement('A', ValueType.INTEGER, AggregationType.MAX))));

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(1));
    DataQueryParams dataQueryParam = dataQueryGroups.getAllQueries().get(0);

    assertTrue(dataQueryParam.getAggregationType().isAggregationType(AggregationType.MAX));

    // Expect the datatype = NUMERIC (which will allow the SQL generator to
    // pick-up
    // the proper SQL function)
    assertThat(dataQueryParam.getAggregationType().getDataType(), is(DataType.NUMERIC));
    assertThat(dataQueryParam.getPeriods(), hasSize(1));
    assertThat(dataQueryParam.getFilterDataElements(), hasSize(1));
    assertThat(dataQueryParam.getFilterOrganisationUnits(), hasSize(1));
  }

  @Test
  void verifyMultipleDataElementAsFilterRetainAggregationTypeAndAggregationDataType() {
    // DataQueryParams with **two** DataElement as filter
    // Both have DataType NUMERIC and AggregationType SUM
    DataQueryParams queryParams =
        createDataQueryParams(
            new BaseDimensionalObject(
                "dx",
                DimensionType.DATA_X,
                DISPLAY_NAME_DATA_X,
                "display name",
                List.of(
                    createDataElement('A', new CategoryCombo()),
                    createDataElement('B', new CategoryCombo()))));

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(1));
    DataQueryParams dataQueryParam = dataQueryGroups.getAllQueries().get(0);

    assertTrue(dataQueryParam.getAggregationType().isAggregationType(AggregationType.SUM));
    assertThat(dataQueryParam.getAggregationType().getDataType(), is(DataType.NUMERIC));
    assertThat(dataQueryParam.getPeriods(), hasSize(1));
    assertThat(dataQueryParam.getFilterDataElements(), hasSize(2));
    assertThat(dataQueryParam.getFilterOrganisationUnits(), hasSize(1));
  }

  @Test
  void verifyMultipleDataElementAsFilterHavingDifferentAggTypeDoNotRetainAggregationType() {
    // DataQueryParams with **two** DataElement as filter
    // Both have DataType NUMERIC but different AggregationType
    DataQueryParams queryParams =
        createDataQueryParams(
            new BaseDimensionalObject(
                "dx",
                DimensionType.DATA_X,
                DISPLAY_NAME_DATA_X,
                "display name",
                List.of(
                    createDataElement('A', new CategoryCombo()),
                    createDataElement('B', ValueType.INTEGER, AggregationType.COUNT))));

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(1));
    DataQueryParams dataQueryParam = dataQueryGroups.getAllQueries().get(0);
    // Aggregation type defaults to SUM
    assertDefaultAggregationType(dataQueryParam);
    assertThat(dataQueryParam.getAggregationType().getDataType(), is(nullValue()));
    assertThat(dataQueryParam.getPeriods(), hasSize(1));
    assertThat(dataQueryParam.getFilterDataElements(), hasSize(2));
    assertThat(dataQueryParam.getFilterOrganisationUnits(), hasSize(1));
  }

  @Test
  void verifyMultipleDataElementAsFilterHavingDifferentAggTypeRetainAggregationType() {
    // DataQueryParams with **two** DataElement as filter
    // Both have DataType NUMERIC but different AggregationType
    // Aggregation type is overridden (COUNT)
    DataQueryParams queryParams =
        createDataQueryParamsWithAggregationType(
            new BaseDimensionalObject(
                "dx",
                DimensionType.DATA_X,
                DISPLAY_NAME_DATA_X,
                "display name",
                List.of(
                    createDataElement('A', new CategoryCombo()),
                    createDataElement('B', ValueType.INTEGER, AggregationType.COUNT))),
            AnalyticsAggregationType.COUNT);

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(1));
    DataQueryParams dataQueryParam = dataQueryGroups.getAllQueries().get(0);
    // Aggregation type defaults to SUM
    assertDefaultAggregationType(dataQueryParam);
    assertThat(dataQueryParam.getAggregationType().getDataType(), is(nullValue()));
    assertThat(dataQueryParam.getPeriods(), hasSize(1));
    assertThat(dataQueryParam.getFilterDataElements(), hasSize(2));
    assertThat(dataQueryParam.getFilterOrganisationUnits(), hasSize(1));
  }

  @Test
  void verifyMultipleDataElementAsFilterHavingDifferentDataTypeDoNotRetainAggregationType() {
    // DataQueryParams with **two** DataElement as filter
    // One Data Element has Type Numeric
    // Aggregation type is overridden (COUNT)
    DataQueryParams queryParams =
        createDataQueryParamsWithAggregationType(
            new BaseDimensionalObject(
                "dx",
                DimensionType.DATA_X,
                DISPLAY_NAME_DATA_X,
                "display name",
                List.of(
                    createDataElement('A', new CategoryCombo()),
                    createDataElement('B', ValueType.TEXT, AggregationType.COUNT))),
            AnalyticsAggregationType.COUNT);

    DataQueryGroups dataQueryGroups =
        subject.planQuery(
            queryParams,
            QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build());

    assertThat(dataQueryGroups.getAllQueries(), hasSize(1));
    DataQueryParams dataQueryParam = dataQueryGroups.getAllQueries().get(0);
    // Aggregation type defaults to SUM
    assertDefaultAggregationType(dataQueryParam);
    assertThat(dataQueryParam.getAggregationType().getDataType(), is(nullValue()));
    assertThat(dataQueryParam.getPeriods(), hasSize(1));
    assertThat(dataQueryParam.getFilterDataElements(), hasSize(2));
    assertThat(dataQueryParam.getFilterOrganisationUnits(), hasSize(1));
  }

  private DataQueryParams createDataQueryParams(BaseDimensionalObject filterDataElements) {
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));

    return DataQueryParams.newBuilder()
        .withDimensions(
            // PERIOD DIMENSION
            List.of(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
        .withFilters(
            List.of(
                // OU FILTER
                new BaseDimensionalObject(
                    "ou",
                    DimensionType.ORGANISATION_UNIT,
                    null,
                    DISPLAY_NAME_ORGUNIT,
                    List.of(new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2"))),
                // DATA ELEMENT AS FILTER
                filterDataElements))
        .build();
  }

  private DataQueryParams createDataQueryParamsWithAggregationType(
      BaseDimensionalObject filterDataElements, AnalyticsAggregationType analyticsAggregationType) {
    return createDataQueryParams(filterDataElements)
        .copyTo(DataQueryParams.newBuilder().withAggregationType(analyticsAggregationType).build());
  }

  private void assertDefaultAggregationType(DataQueryParams dataQueryParam) {
    assertTrue(dataQueryParam.getAggregationType().isAggregationType(AggregationType.SUM));
  }
}
