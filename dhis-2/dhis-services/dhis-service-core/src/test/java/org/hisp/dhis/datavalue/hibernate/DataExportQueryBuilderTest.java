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
package org.hisp.dhis.datavalue.hibernate;

import static org.hisp.dhis.datavalue.hibernate.HibernateDataExportStore.createExportQuery;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.sql.AbstractQueryBuilderTest;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SQL generation as performed by {@link
 * HibernateDataExportStore#createExportQuery(DataExportParams, SQL.QueryAPI, Supplier)}
 *
 * @author Jan Bernitt
 */
class DataExportQueryBuilderTest extends AbstractQueryBuilderTest {

  @Test
  void testFilter_Period() {
    DataExportParams params =
        DataExportParams.builder().periods(List.of(getPeriodFromIsoString("2020"))).build();
    assertSQL(
        """
      WITH
      de_ids AS (
        SELECT dataelementid
        FROM (
                (SELECT NULL::bigint AS dataelementid WHERE false)
        ) de_all
        WHERE dataelementid IS NOT NULL
      ),
      pe_ids AS (
        SELECT periodid
        FROM period
        WHERE iso = :pe
      ),
      ou_ids AS (
        SELECT organisationunitid
        FROM (
          (SELECT NULL::bigint AS organisationunitid WHERE false)
        ) ou_all
        WHERE organisationunitid IS NOT NULL
      ),
      ou_with_descendants_ids AS (
        SELECT DISTINCT ou.organisationunitid
        FROM organisationunit ou
        LEFT JOIN organisationunit parent_ou ON (ou.path LIKE parent_ou.path || '%')
        WHERE ou.organisationunitid IN (SELECT organisationunitid FROM ou_ids)
             OR parent_ou.organisationunitid IN (SELECT organisationunitid FROM ou_ids)
      )
      SELECT
        de.uid AS deid,
        pe.iso,
        ou.uid AS ouid,
        coc.uid AS cocid,
        aoc.uid AS aocid,
        de.valuetype,
        dv.value,
        dv.comment,
        dv.followup,
        dv.storedby,
        dv.created,
        dv.lastupdated,
        dv.deleted
      FROM datavalue dv
      JOIN pe_ids ON dv.periodid = pe_ids.periodid
      JOIN dataelement de ON dv.dataelementid = de.dataelementid
      JOIN period pe ON dv.periodid = pe.periodid
      JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
      JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
      JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
      WHERE dv.deleted = :deleted
      ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("pe", "deleted"), createExportQuery(params, createSpyQuery(), SystemUser::new));
  }
}
