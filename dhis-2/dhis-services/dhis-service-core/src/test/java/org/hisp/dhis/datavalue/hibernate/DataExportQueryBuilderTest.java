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

import java.util.Set;
import org.hisp.dhis.datavalue.DataExportStoreParams;
import org.hisp.dhis.sql.AbstractQueryBuilderTest;
import org.hisp.dhis.sql.SQL;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SQL generation as performed by {@link
 * HibernateDataExportStore#createExportQuery(DataExportStoreParams, SQL.QueryAPI)}
 *
 * @author Jan Bernitt
 */
class DataExportQueryBuilderTest extends AbstractQueryBuilderTest {

  @Test
  void testFilter_Period() {
    DataExportStoreParams params =
        new DataExportStoreParams()
            .setOrderForSync(true) // bypass user context logic
            .setPeriods(Set.of(getPeriodFromIsoString("2020")));
    assertSQL(
        """
        SELECT
          de.uid AS deid,
          pe.iso,
          ou.uid AS ouid,
          coc.uid AS cocid,
          aoc.uid AS aocid,
          dv.value,
          dv.comment,
          dv.followup,
          dv.storedby,
          dv.created,
          dv.lastupdated,
          dv.deleted
        FROM datavalue dv
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE pe.iso = :pe
          AND dv.deleted = :deleted
        ORDER BY pe.startdate , dv.created , deid""",
        Set.of("pe", "deleted"),
        createExportQuery(params, createQueryAPI()));
  }
}
