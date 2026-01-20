/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.outlier.service;

import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.MODIFIED_Z_SCORE;
import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.Z_SCORE;
import static org.hisp.dhis.test.TestBase.createCategoryOptionCombo;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.outlier.OutlierSqlStatementProcessor;
import org.hisp.dhis.analytics.outlier.data.DataDimension;
import org.hisp.dhis.analytics.outlier.data.OutlierRequest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsZscoreSqlStatementProcessorTest {
  private OutlierSqlStatementProcessor subject;

  private List<DataDimension> dataDimensions;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  @BeforeEach
  public void setUp() {
    subject = new AnalyticsZScoreSqlStatementProcessor();

    DataElement deA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    DataElement deB = createDataElement('B', ValueType.INTEGER, AggregationType.SUM);
    DataElement deC = createDataElement('C', ValueType.NUMBER, AggregationType.SUM);

    CategoryOptionCombo cocA = createCategoryOptionCombo('A');
    CategoryOptionCombo cocB = createCategoryOptionCombo('B');
    CategoryOptionCombo cocC = createCategoryOptionCombo('C');

    dataDimensions =
        List.of(
            new DataDimension(deA, cocA),
            new DataDimension(deB, cocB),
            new DataDimension(deC, cocC));

    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
  }

  @Test
  void testGetSqlStatementWithZScore() {
    OutlierRequest.OutlierRequestBuilder builder = OutlierRequest.builder();
    builder
        .dataDimensions(dataDimensions)
        .startDate(getDate(2020, 1, 1))
        .endDate(getDate(2020, 3, 1))
        .orgUnits(Lists.newArrayList(ouA, ouB))
        .algorithm(Z_SCORE)
        .build();
    String sql = subject.getSqlStatement(builder.build());
    assertTrue(sql.contains("avg_middle_value"));
  }

  @Test
  void testGetSqlStatementWithModifiedZScore() {
    OutlierRequest.OutlierRequestBuilder builder = OutlierRequest.builder();
    builder
        .dataDimensions(dataDimensions)
        .startDate(getDate(2020, 1, 1))
        .endDate(getDate(2020, 3, 1))
        .orgUnits(Lists.newArrayList(ouA, ouB))
        .algorithm(MODIFIED_Z_SCORE)
        .build();
    String sql = subject.getSqlStatement(builder.build());
    assertTrue(sql.contains("percentile_middle_value"));
  }

  @Test
  void testGetSqlStatementWithNullRequest() {
    assertEquals(StringUtils.EMPTY, subject.getSqlStatement(null));
  }

  @Test
  void testGetSqlStatementWithRelativePeriod() {
    Period period = new Period();
    period.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    period.setStartDate(DateUtils.parseDate("2023-01-01"));
    period.setEndDate(DateUtils.parseDate("2023-12-31"));
    List<PeriodDimension> periods = List.of(PeriodDimension.of(period));
    OutlierRequest.OutlierRequestBuilder builder = OutlierRequest.builder();
    builder
        .dataDimensions(dataDimensions)
        .periods(periods)
        .orgUnits(Lists.newArrayList(ouA, ouB))
        .build();
    String sql = subject.getSqlStatement(builder.build());
    String expected =
        "select * from (select ax.dx as de_uid, ax.ou as ou_uid, ax.co as coc_uid, ax.ao as aoc_uid, ax.value, ax.pestartdate as pe_start_date, ax.petype as pt_name,  ax.avg_middle_value as middle_value, ax.std_dev, ax.mad, abs(ax.value::double precision -  ax.avg_middle_value) as middle_value_abs_dev, (case when ax.std_dev = 0 then 0       else abs(ax.value::double precision -  ax.avg_middle_value ) / ax.std_dev        end) as z_score,  ax.avg_middle_value - (ax.std_dev * :threshold) as lower_bound,  ax.avg_middle_value + (ax.std_dev * :threshold) as upper_bound from analytics ax where ((ax.dataelementid = :data_element_id0 and ax.categoryoptioncomboid = :category_option_combo_id0) or (ax.dataelementid = :data_element_id1 and ax.categoryoptioncomboid = :category_option_combo_id1) or (ax.dataelementid = :data_element_id2 and ax.categoryoptioncomboid = :category_option_combo_id2)) and (ax.\"path\" like '/ouabcdefghA%' or ax.\"path\" like '/ouabcdefghB%') and ( ax.pestartdate >= :start_date0 and ax.peenddate <= :end_date0 )) t1 where t1.z_score > :threshold order by middle_value_abs_dev desc limit :max_results ";
    assertEquals(expected, sql);
  }

  @Test
  void testGetSqlStatementWithStartEndDate() {
    OutlierRequest.OutlierRequestBuilder builder = OutlierRequest.builder();
    builder
        .dataDimensions(dataDimensions)
        .startDate(getDate(2020, 1, 1))
        .endDate(getDate(2020, 3, 1))
        .orgUnits(Lists.newArrayList(ouA, ouB))
        .build();
    String sql = subject.getSqlStatement(builder.build());
    String expected =
        "select * from (select ax.dx as de_uid, ax.ou as ou_uid, ax.co as coc_uid, ax.ao as aoc_uid, ax.value, ax.pestartdate as pe_start_date, ax.petype as pt_name,  ax.avg_middle_value as middle_value, ax.std_dev, ax.mad, abs(ax.value::double precision -  ax.avg_middle_value) as middle_value_abs_dev, (case when ax.std_dev = 0 then 0       else abs(ax.value::double precision -  ax.avg_middle_value ) / ax.std_dev        end) as z_score,  ax.avg_middle_value - (ax.std_dev * :threshold) as lower_bound,  ax.avg_middle_value + (ax.std_dev * :threshold) as upper_bound from analytics ax where ((ax.dataelementid = :data_element_id0 and ax.categoryoptioncomboid = :category_option_combo_id0) or (ax.dataelementid = :data_element_id1 and ax.categoryoptioncomboid = :category_option_combo_id1) or (ax.dataelementid = :data_element_id2 and ax.categoryoptioncomboid = :category_option_combo_id2)) and (ax.\"path\" like '/ouabcdefghA%' or ax.\"path\" like '/ouabcdefghB%') and ax.pestartdate >= :start_date and ax.peenddate <= :end_date) t1 where t1.z_score > :threshold order by middle_value_abs_dev desc limit :max_results ";
    assertEquals(expected, sql);
  }
}
