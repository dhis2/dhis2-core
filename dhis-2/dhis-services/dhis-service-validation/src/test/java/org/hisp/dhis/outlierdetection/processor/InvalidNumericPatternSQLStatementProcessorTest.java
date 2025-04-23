/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.outlierdetection.processor;

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NumericPatternSqlStatementProcessorTest {

  private OutlierSqlStatementProcessor subject;

  private DataElement deA;
  private DataElement deB;
  private DataElement deC;
  private OrganisationUnit ouA;
  private OrganisationUnit ouB;

  @BeforeEach
  void setUp() {
    subject = new InvalidNumericPatternSqlStatementProcessor();

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
        """
      select de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid,
      de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name,
      pe.startdate as pe_start_date, pt.name as pt_name,
      dv.value as raw_value, dv.followup as follow_up,
      NULL,
      NULL as bound_abs_dev,
      NULL as lower_bound,
      NULL as upper_bound
      from datavalue dv
      inner join dataelement de on dv.dataelementid = de.dataelementid
      inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid
      inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid
      inner join period pe on dv.periodid = pe.periodid
      inner join periodtype pt on pe.periodtypeid = pt.periodtypeid
      inner join organisationunit ou on dv.sourceid = ou.organisationunitid
      where dv.dataelementid in (:data_element_ids)
      and pe.startdate >= :start_date
      and pe.enddate <= :end_date
      and (ou."path" like '/ouabcdefghA%' or ou."path" like '/ouabcdefghB%')
      and dv.deleted is false
      and (trim(dv.value) !~ '^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$' or length(split_part(trim(dv.value), '.', 1)) > 307)
      limit :max_results;
      """;

    assertEquals(normalizeSql(expected), normalizeSql(sql));
  }

  @Test
  void testGetSqlStatementWithNullRequest() {
    assertEquals(StringUtils.EMPTY, subject.getSqlStatement(null));
  }

  private static String normalizeSql(String sql) {
    return sql.trim().replaceAll("\\s+", " ");
  }
}
