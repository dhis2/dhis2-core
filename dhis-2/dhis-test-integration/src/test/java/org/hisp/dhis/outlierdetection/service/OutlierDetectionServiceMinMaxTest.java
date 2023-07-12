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
package org.hisp.dhis.outlierdetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.Lists;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class OutlierDetectionServiceMinMaxTest extends IntegrationTestBase {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private MinMaxDataElementService minMaxService;

  @Autowired private DataValueService dataValueService;

  @Autowired private OutlierDetectionService subject;

  private DataElement deA;

  private DataElement deB;

  private Period m01, m02, m03, m04, m05, m06, m07, m08, m09, m10, m11, m12;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private CategoryOptionCombo coc;

  @Override
  public void setUpTest() {
    MonthlyPeriodType pt = new MonthlyPeriodType();
    m01 = pt.createPeriod("202001");
    m02 = pt.createPeriod("202002");
    m03 = pt.createPeriod("202003");
    m04 = pt.createPeriod("202004");
    m05 = pt.createPeriod("202005");
    m06 = pt.createPeriod("202006");
    m07 = pt.createPeriod("202007");
    m08 = pt.createPeriod("202008");
    m09 = pt.createPeriod("202009");
    m10 = pt.createPeriod("202010");
    m11 = pt.createPeriod("202011");
    m12 = pt.createPeriod("202012");
    addPeriods(m01, m02, m03, m04, m05, m06, m07, m08, m09, m10, m11, m12);
    deA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    deB = createDataElement('B', ValueType.INTEGER, AggregationType.SUM);
    idObjectManager.save(deA);
    idObjectManager.save(deB);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    coc = categoryService.getDefaultCategoryOptionCombo();
  }

  @Test
  void testGetFromQuery() {
    OutlierDetectionQuery query = new OutlierDetectionQuery();
    query.setDe(Lists.newArrayList("deabcdefghA", "deabcdefghB"));
    query.setStartDate(getDate(2020, 1, 1));
    query.setEndDate(getDate(2020, 6, 1));
    query.setOu(Lists.newArrayList("ouabcdefghA", "ouabcdefghB"));
    query.setAlgorithm(OutlierDetectionAlgorithm.MIN_MAX);
    query.setMaxResults(200);
    OutlierDetectionRequest request = subject.getFromQuery(query);
    assertEquals(2, request.getDataElements().size());
    assertEquals(2, request.getOrgUnits().size());
    assertEquals(getDate(2020, 1, 1), request.getStartDate());
    assertEquals(getDate(2020, 6, 1), request.getEndDate());
    assertEquals(OutlierDetectionAlgorithm.MIN_MAX, request.getAlgorithm());
    assertEquals(200, request.getMaxResults());
  }

  @Test
  void testGetOutlierValues() {
    addMinMaxValues(
        new MinMaxDataElement(deA, ouA, coc, 40, 60), new MinMaxDataElement(deB, ouA, coc, 45, 65));
    // 34, 39, 68, 91, 42, 45, 68, 87 are outlier values out of range
    addDataValues(
        new DataValue(deA, m01, ouA, coc, coc, "50"),
        new DataValue(deA, m07, ouA, coc, coc, "51"),
        new DataValue(deA, m02, ouA, coc, coc, "34"),
        new DataValue(deA, m08, ouA, coc, coc, "59"),
        new DataValue(deA, m03, ouA, coc, coc, "58"),
        new DataValue(deA, m09, ouA, coc, coc, "39"),
        new DataValue(deA, m04, ouA, coc, coc, "68"),
        new DataValue(deA, m10, ouA, coc, coc, "52"),
        new DataValue(deA, m05, ouA, coc, coc, "51"),
        new DataValue(deA, m11, ouA, coc, coc, "58"),
        new DataValue(deA, m06, ouA, coc, coc, "40"),
        new DataValue(deA, m12, ouA, coc, coc, "91"),
        new DataValue(deB, m01, ouA, coc, coc, "42"),
        new DataValue(deB, m02, ouA, coc, coc, "48"),
        new DataValue(deB, m03, ouA, coc, coc, "45"),
        new DataValue(deB, m04, ouA, coc, coc, "46"),
        new DataValue(deB, m05, ouA, coc, coc, "49"),
        new DataValue(deB, m06, ouA, coc, coc, "68"),
        new DataValue(deB, m07, ouA, coc, coc, "48"),
        new DataValue(deB, m08, ouA, coc, coc, "49"),
        new DataValue(deB, m09, ouA, coc, coc, "52"),
        new DataValue(deB, m10, ouA, coc, coc, "47"),
        new DataValue(deB, m11, ouA, coc, coc, "11"),
        new DataValue(deB, m12, ouA, coc, coc, "87"));
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2021, 1, 1))
            .withOrgUnits(Lists.newArrayList(ouA))
            .withAlgorithm(OutlierDetectionAlgorithm.MIN_MAX)
            .build();
    OutlierDetectionResponse response = subject.getOutlierValues(request);
    assertEquals(8, response.getOutlierValues().size());
  }

  @Test
  void testGetOutlierValue() {
    addMinMaxValues(
        new MinMaxDataElement(deA, ouA, coc, 40, 60), new MinMaxDataElement(deB, ouA, coc, 45, 65));
    addDataValues(
        new DataValue(deA, m01, ouA, coc, coc, "32"),
        new DataValue(deA, m07, ouA, coc, coc, "42"),
        new DataValue(deB, m03, ouA, coc, coc, "45"),
        new DataValue(deB, m04, ouA, coc, coc, "62"));
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2021, 1, 1))
            .withOrgUnits(Lists.newArrayList(ouA))
            .withAlgorithm(OutlierDetectionAlgorithm.MIN_MAX)
            .build();
    OutlierDetectionResponse response = subject.getOutlierValues(request);
    assertEquals(1, response.getOutlierValues().size());
    OutlierValue outlier = response.getOutlierValues().get(0);
    assertEquals(deA.getUid(), outlier.getDe());
    assertEquals(ouA.getUid(), outlier.getOu());
    assertEquals(m01.getIsoDate(), outlier.getPe());
    assertEquals(32, outlier.getValue().intValue());
    assertEquals(40, outlier.getLowerBound().intValue());
    assertEquals(60, outlier.getUpperBound().intValue());
    assertEquals(8, outlier.getAbsDev().intValue());
    assertFalse(outlier.getFollowup());
  }

  private void addPeriods(Period... periods) {
    Stream.of(periods).forEach(periodService::addPeriod);
  }

  private void addMinMaxValues(MinMaxDataElement... minMaxValues) {
    Stream.of(minMaxValues).forEach(minMaxService::addMinMaxDataElement);
  }

  private void addDataValues(DataValue... dataValues) {
    Stream.of(dataValues).forEach(dataValueService::addDataValue);
  }
}
