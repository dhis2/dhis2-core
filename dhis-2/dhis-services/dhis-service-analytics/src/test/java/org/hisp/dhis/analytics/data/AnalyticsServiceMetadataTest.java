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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.period.RelativePeriodEnum.THIS_QUARTER;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class AnalyticsServiceMetadataTest extends AnalyticsServiceBaseTest {
  @BeforeEach
  public void setUp() {
    Map<String, Object> aggregatedValues = new HashMap<>();
    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.DATA_VALUE), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(aggregatedValues));
  }

  @SuppressWarnings("unchecked")
  @Test
  void metadataContainsOuLevelData() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            // PERIOD
            .withPeriod(new Period(YearlyPeriodType.getPeriodFromIsoString("2017W10")))
            // DATA ELEMENTS
            .withDataElements(List.of(createDataElement('A', new CategoryCombo())))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(
                            new OrganisationUnit("aaa", "aaa", "OU_1", null, null, "c1"),
                            new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")),
                        new DimensionItemKeywords(
                            Lists.newArrayList(
                                buildOrgUnitLevel(2, "wjP19dkFeIk", "District", null),
                                buildOrgUnitLevel(1, "tTUf91fCytl", "Chiefdom", "OU_12345"))))))
            .build();

    initMock(params);

    Grid grid = target.getAggregatedDataValueGrid(params);

    Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");
    assertThat(
        items.get("wjP19dkFeIk"),
        allOf(
            hasProperty("name", is("District")),
            hasProperty("uid", is("wjP19dkFeIk")),
            hasProperty("code", is(nullValue()))));
    assertThat(
        items.get("tTUf91fCytl"),
        allOf(
            hasProperty("name", is("Chiefdom")),
            hasProperty("uid", is("tTUf91fCytl")),
            hasProperty("code", is("OU_12345"))));
  }

  @SuppressWarnings("unchecked")
  @Test
  void metadataContainsIndicatorGroupMetadata() {
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));

    IndicatorGroup indicatorGroup = new IndicatorGroup("ANC");
    indicatorGroup.setCode("COD_1000");
    indicatorGroup.setUid("wjP19dkFeIk");
    DataQueryParams params =
        DataQueryParams.newBuilder()
            // DATA ELEMENTS
            .withDimensions(
                Lists.newArrayList(
                    new BaseDimensionalObject("pe", DimensionType.PERIOD, periods),
                    new BaseDimensionalObject(
                        "dx",
                        DimensionType.DATA_X,
                        DISPLAY_NAME_DATA_X,
                        "display name",
                        Lists.newArrayList(
                            new Indicator(),
                            new Indicator(),
                            createDataElement('A', new CategoryCombo()),
                            createDataElement('B', new CategoryCombo())),
                        new DimensionItemKeywords(List.of(indicatorGroup)))))
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(
                            new OrganisationUnit("aaa", "aaa", "OU_1", null, null, "c1"),
                            new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")))))
            .withIgnoreLimit(true)
            .withSkipData(true)
            .build();

    initMock(params);

    Grid grid = target.getAggregatedDataValueGrid(params);
    Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");

    assertThat(
        items.get(indicatorGroup.getUid()),
        allOf(
            hasProperty("name", is(indicatorGroup.getName())),
            hasProperty("uid", is(indicatorGroup.getUid())),
            hasProperty("code", is(indicatorGroup.getCode()))));
  }

  @SuppressWarnings("unchecked")
  @Test
  void metadataContainsOuGroupData() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            // PERIOD
            .withPeriod(new Period(YearlyPeriodType.getPeriodFromIsoString("2017W10")))
            // DATA ELEMENTS
            .withDataElements(List.of(createDataElement('A', new CategoryCombo())))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(
                            new OrganisationUnit("aaa", "aaa", "OU_1", null, null, "c1"),
                            new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")),
                        new DimensionItemKeywords(
                            Lists.newArrayList(
                                new BaseNameableObject("tTUf91fCytl", "OU_12345", "Chiefdom"))))))
            .build();

    initMock(params);

    Grid grid = target.getAggregatedDataValueGrid(params);

    Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");
    assertThat(
        items.get("tTUf91fCytl"),
        allOf(
            hasProperty("name", is("Chiefdom")),
            hasProperty("uid", is("tTUf91fCytl")),
            hasProperty("code", is("OU_12345"))));
  }

  @SuppressWarnings("unchecked")
  @Test
  void metadataContainsDataElementGroupMetadata() {
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));

    DataElementGroup dataElementGroup = new DataElementGroup("ANC");
    dataElementGroup.setCode("COD_1000");
    dataElementGroup.setUid("wjP19dkFeIk");
    DataQueryParams params =
        DataQueryParams.newBuilder()
            // DATA ELEMENTS
            .withDimensions(
                List.of(
                    new BaseDimensionalObject("pe", DimensionType.PERIOD, periods),
                    new BaseDimensionalObject(
                        "dx",
                        DimensionType.DATA_X,
                        DISPLAY_NAME_DATA_X,
                        "display name",
                        List.of(
                            createDataElement('A', new CategoryCombo()),
                            createDataElement('B', new CategoryCombo())),
                        new DimensionItemKeywords(List.of(dataElementGroup)))))
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(new OrganisationUnit("aaa", "aaa", "OU_1", null, null, "c1")))))
            .withIgnoreLimit(true)
            .withSkipData(true)
            .build();

    initMock(params);

    Grid grid = target.getAggregatedDataValueGrid(params);
    Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");

    assertThat(
        items.get(dataElementGroup.getUid()),
        allOf(
            hasProperty("name", is(dataElementGroup.getName())),
            hasProperty("uid", is(dataElementGroup.getUid())),
            hasProperty("code", is(dataElementGroup.getCode()))));
  }

  @SuppressWarnings("unchecked")
  @Test
  void metadataContainsRelativePeriodItem() {

    List<DimensionalItemObject> periods = new ArrayList<>();

    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));

    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, periods);

    DimensionItemKeywords dimensionalKeywords = new DimensionItemKeywords();
    dimensionalKeywords.addKeyword(THIS_QUARTER.name(), "This quarter");
    periodDimension.setDimensionalKeywords(dimensionalKeywords);

    DataQueryParams params =
        DataQueryParams.newBuilder()
            // DATA ELEMENTS
            .withDimensions(
                Lists.newArrayList(
                    periodDimension,
                    new BaseDimensionalObject(
                        "dx",
                        DimensionType.DATA_X,
                        DISPLAY_NAME_DATA_X,
                        "display name",
                        Lists.newArrayList(
                            createDataElement('A', new CategoryCombo()),
                            createDataElement('B', new CategoryCombo())))))
            .withSkipData(true)
            .build();

    initMock(params);

    Grid grid = target.getAggregatedDataValueGrid(params);

    Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");
    assertTrue(items.containsKey(THIS_QUARTER.name()));
  }

  private OrganisationUnitLevel buildOrgUnitLevel(int level, String uid, String name, String code) {
    OrganisationUnitLevel oul = new OrganisationUnitLevel(level, name);
    oul.setUid(uid);
    oul.setCode(code);
    return oul;
  }
}
