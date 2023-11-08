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

public class ZScoreSqlStatementProcessorTest {
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
    subject = new ZScoreSqlStatementProcessor();

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
        "select dvs.de_uid, dvs.ou_uid, dvs.coc_uid, dvs.aoc_uid, dvs.de_name, dvs.ou_name, dvs.coc_name, dvs.aoc_name, dvs.value, dvs.follow_up, dvs.pe_start_date, dvs.pt_name, stats.middle_value as middle_value, stats.std_dev as std_dev, abs(dvs.value::double precision - stats.middle_value) as middle_value_abs_dev, abs(dvs.value::double precision - stats.middle_value) / stats.std_dev as z_score, stats.middle_value - (stats.std_dev * :threshold) as lower_bound, stats.middle_value + (stats.std_dev * :threshold) as upper_bound from (select dv.dataelementid, dv.sourceid, dv.periodid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, pe.startdate as pe_start_date, pt.name as pt_name, dv.value as value, dv.followup as follow_up from datavalue dv inner join dataelement de on dv.dataelementid = de.dataelementid inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid inner join period pe on dv.periodid = pe.periodid inner join periodtype pt on pe.periodtypeid = pt.periodtypeid inner join organisationunit ou on dv.sourceid = ou.organisationunitid where dv.dataelementid in (:data_element_ids) and pe.startdate >= :start_date and pe.enddate <= :end_date and (ou.\"path\" like '/ouabcdefghA%' or ou.\"path\" like '/ouabcdefghB%') and dv.deleted is false) as dvs inner join (select dv.dataelementid as dataelementid, dv.sourceid as sourceid, dv.categoryoptioncomboid as categoryoptioncomboid, dv.attributeoptioncomboid as attributeoptioncomboid, avg(dv.value::double precision) as middle_value, stddev_pop(dv.value::double precision) as std_dev from datavalue dv inner join period pe on dv.periodid = pe.periodid inner join organisationunit ou on dv.sourceid = ou.organisationunitid where dv.dataelementid in (:data_element_ids) and (ou.\"path\" like '/ouabcdefghA%' or ou.\"path\" like '/ouabcdefghB%') and dv.deleted is false group by dv.dataelementid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid) as stats on dvs.dataelementid = stats.dataelementid and dvs.sourceid = stats.sourceid and dvs.categoryoptioncomboid = stats.categoryoptioncomboid and dvs.attributeoptioncomboid = stats.attributeoptioncomboid where stats.std_dev != 0.0 and (abs(dvs.value::double precision - stats.middle_value) / stats.std_dev) >= :threshold order by middle_value_abs_dev desc limit :max_results;";
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
    assertTrue(sql.contains("avg(dv.value::double precision)"));
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
    assertTrue(
        sql.contains("percentile_cont(0.5) within group(order by dv.value::double precision)"));
  }

  @Test
  void testGetSqlStatementWithNullRequest() {
    assertEquals(StringUtils.EMPTY, subject.getSqlStatement(null));
  }
}
