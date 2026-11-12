/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.Iterator;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards that {@link DataExportStore#exportValues(DataExportParams)} streams its rows from a
 * server-side cursor. Without a JDBC fetch size the driver reads the whole result set into memory
 * before returning the first row; the export output is the same either way, so only this test
 * notices when the fetch size is lost (e.g. the store is given a template without one).
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
class DataExportStoreStreamingTest extends PostgresIntegrationTestBase {
  private @PersistenceContext EntityManager manager;

  @Autowired private DataDumpService dataDumpService;
  @Autowired private DataExportStore dataExportStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testExportValues_ReadsRowsThroughOpenCursor() {
    DataElement de = createDataElement('A');
    de.setValueType(ValueType.TEXT);
    manager.persist(de);
    OrganisationUnit ou = createOrganisationUnit('A');
    manager.persist(ou);
    // one row more than the JDBC fetch size (1000): a cursor that fits into a single fetch is
    // closed by the driver right away, so only a longer result shows whether rows are streamed
    PeriodType daily = PeriodType.getPeriodType(PeriodTypeEnum.DAILY);
    Date day = DateUtils.getDate(2024, 1, 1);
    CategoryOptionCombo coc = categoryService.getDefaultCategoryOptionCombo();
    DataValue[] dvs = new DataValue[1001];
    for (int i = 0; i < dvs.length; i++) {
      dvs[i] = createDataValue(de, daily.createPeriod(day), ou, "value", coc);
      day = DateUtils.addDays(day, 1);
    }
    assertEquals(
        dvs.length,
        dataDumpService.upsertValuesForJdbcTest(dvs),
        "test data values were not written");

    try (Stream<DataExportValue> values =
        dataExportStore.exportValues(DataExportParams.builder().includeDeleted(false).build())) {
      Iterator<DataExportValue> iter = values.iterator();
      assertTrue(iter.hasNext(), "export should return the test data values");
      assertEquals(1, countOpenExportCursors(), "export query is not read through a cursor");
    }
  }

  private int countOpenExportCursors() {
    // pgjdbc only uses a named portal (C_n) when a fetch size is set, without one the query runs
    // in the unnamed portal; the pattern is concatenated so this query's own statement text does
    // not match it
    Integer n =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM pg_cursors
            WHERE name <> '' AND statement LIKE '%FROM datavalue' || ' dv%'""",
            Integer.class);
    return n == null ? 0 : n;
  }
}
