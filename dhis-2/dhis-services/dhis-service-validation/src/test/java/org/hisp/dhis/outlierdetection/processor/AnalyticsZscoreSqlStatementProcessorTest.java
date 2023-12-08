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
package org.hisp.dhis.outlierdetection.processor;

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsZscoreSqlStatementProcessorTest {
  private OutlierSqlStatementProcessor subject;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  @BeforeEach
  public void setUp() {
    subject = new AnalyticsZScoreSqlStatementProcessor();

    deA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    deB = createDataElement('B', ValueType.INTEGER, AggregationType.SUM);
    deC = createDataElement('C', ValueType.NUMBER, AggregationType.SUM);

    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
  }

  @Test
  void testGetSqlStatement() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 3, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .build();
    String sql = subject.getSqlStatement(request);
    String expected =
        "select * from (select ax.dx as de_uid, ax.ou as ou_uid, ax.co as coc_uid, ax.ao as aoc_uid, ax.de_name, ax.ou_name, ax.coc_name, ax.aoc_name, ax.value, ax.pestartdate as pe_start_date, ax.petype as pt_name,  ax.avg_middle_value as middle_value, ax.std_dev, ax.mad, abs(ax.value::double precision -  ax.avg_middle_value) as middle_value_abs_dev, (case when ax.std_dev = 0 then 0       else abs(ax.value::double precision -  ax.avg_middle_value ) / ax.std_dev        end) as z_score,  ax.avg_middle_value - (ax.std_dev * :threshold) as lower_bound,  ax.avg_middle_value + (ax.std_dev * :threshold) as upper_bound from analytics ax where dataelementid in  (:data_element_ids) and (ax.\"path\" like '/ouabcdefghA%' or ax.\"path\" like '/ouabcdefghB%') and ax.pestartdate >= :start_date and ax.peenddate <= :end_date) t1 where t1.z_score > :threshold order by middle_value_abs_dev desc limit :max_results ";
    assertEquals(expected, sql);
  }

  @Test
  void testGetSqlStatementWithZScore() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 3, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withAlgorithm(OutlierDetectionAlgorithm.Z_SCORE)
            .build();
    String sql = subject.getSqlStatement(request);
    assertTrue(sql.contains("avg_middle_value"));
  }

  @Test
  void testGetSqlStatementWithModifiedZScore() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 3, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withAlgorithm(OutlierDetectionAlgorithm.MOD_Z_SCORE)
            .build();
    String sql = subject.getSqlStatement(request);
    assertTrue(sql.contains("percentile_middle_value"));
  }

  @Test
  void testGetSqlStatementWithNullRequest() {
    assertEquals(StringUtils.EMPTY, subject.getSqlStatement(null));
  }
}
