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

import static org.hisp.dhis.datavalue.hibernate.HibernateDataValueChangelogStore.createEntriesQuery;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataValueChangelogQueryParams;
import org.hisp.dhis.datavalue.DataValueChangelogType;
import org.hisp.dhis.sql.AbstractQueryBuilderTest;
import org.hisp.dhis.sql.SQL;
import org.junit.jupiter.api.Test;

/**
 * Unit tests the SQL generation as made by {@link
 * HibernateDataValueChangelogStore#createEntriesQuery(DataValueChangelogQueryParams,
 * SQL.QueryAPI)}.
 *
 * @author Jan Bernitt
 */
class DataValueChangelogQueryBuilderTest extends AbstractQueryBuilderTest {

  @Test
  void testFilterByTypes() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setTypes(List.of(DataValueChangelogType.CREATE, DataValueChangelogType.UPDATE));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      WHERE dva.audittype = ANY(:types)
      ORDER BY dva.created DESC""",
        Set.of("types"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByTypes_Single() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setTypes(List.of(DataValueChangelogType.UPDATE));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      WHERE dva.audittype = :types
      ORDER BY dva.created DESC""",
        Set.of("types"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testCountByTypes() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setTypes(List.of(DataValueChangelogType.UPDATE, DataValueChangelogType.CREATE));
    assertCountSQL(
        """
      SELECT count(*)
      FROM datavalueaudit dva
      WHERE dva.audittype = ANY(:types)""",
        Set.of("types"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testCountByTypes_Single() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setTypes(List.of(DataValueChangelogType.UPDATE));
    assertCountSQL(
        """
      SELECT count(*)
      FROM datavalueaudit dva
      WHERE dva.audittype = :types""",
        Set.of("types"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByDataElements() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setDataElements(List.of(UID.of("de123456789"), UID.of("de987654321")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN dataelement de ON dva.dataelementid = de.dataelementid
      WHERE de.uid = ANY(:de)
      ORDER BY dva.created DESC""",
        Set.of("de"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByDataElements_Single() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setDataElements(List.of(UID.of("de123456789")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN dataelement de ON dva.dataelementid = de.dataelementid
      WHERE de.uid = :de
      ORDER BY dva.created DESC""",
        Set.of("de"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByOrgUnits() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setOrgUnits(List.of(UID.of("ou123456789"), UID.of("ou987654321")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN organisationunit ou ON dva.organisationunitid = ou.organisationunitid
      WHERE ou.uid = ANY(:ou)
      ORDER BY dva.created DESC""",
        Set.of("ou"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByOrgUnits_Single() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setOrgUnits(List.of(UID.of("ou123456789")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN organisationunit ou ON dva.organisationunitid = ou.organisationunitid
      WHERE ou.uid = :ou
      ORDER BY dva.created DESC""",
        Set.of("ou"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByDataSets() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setDataSets(List.of(UID.of("ds123456789"), UID.of("ds987654321")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN datasetelement dse ON dva.dataelementid = dse.dataelementid
      JOIN dataset ds ON dse.datasetid = ds.datasetid
      WHERE ds.uid = ANY(:ds)
      ORDER BY dva.created DESC""",
        Set.of("ds"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByDataSets_Single() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setDataSets(List.of(UID.of("ds123456789")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN datasetelement dse ON dva.dataelementid = dse.dataelementid
      JOIN dataset ds ON dse.datasetid = ds.datasetid
      WHERE ds.uid = :ds
      ORDER BY dva.created DESC""",
        Set.of("ds"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByPeriods() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setPeriods(List.of(getPeriodFromIsoString("2021"), getPeriodFromIsoString("2022")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN period pe ON dva.periodid = pe.periodid
      WHERE pe.iso = ANY(:pe)
      ORDER BY dva.created DESC""",
        Set.of("pe"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByPeriods_Single() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setPeriods(List.of(getPeriodFromIsoString("2021")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN period pe ON dva.periodid = pe.periodid
      WHERE pe.iso = :pe
      ORDER BY dva.created DESC""",
        Set.of("pe"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByCategoryOptionCombo() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setCategoryOptionCombo(UID.of("coc23456789"));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      WHERE dva.categoryoptioncomboid = (SELECT coc.categoryoptioncomboid FROM categoryoptioncombo coc WHERE coc.uid = :coc)
      ORDER BY dva.created DESC""",
        Set.of("coc"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByAttributeOptionCombo() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams().setAttributeOptionCombo(UID.of("aoc23456789"));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      WHERE dva.attributeoptioncomboid = (SELECT aoc.categoryoptioncomboid FROM categoryoptioncombo aoc WHERE aoc.uid = :aoc)
      ORDER BY dva.created DESC""",
        Set.of("aoc"),
        createEntriesQuery(params, createSpyQuery()));
  }

  @Test
  void testFilterByMixed() {
    DataValueChangelogQueryParams params =
        new DataValueChangelogQueryParams()
            .setTypes(List.of(DataValueChangelogType.UPDATE))
            .setDataSets(List.of(UID.of("ds123456789"), UID.of("ds987654321")))
            .setPeriods(List.of(getPeriodFromIsoString("2022")));
    assertSQL(
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN period pe ON dva.periodid = pe.periodid
      JOIN datasetelement dse ON dva.dataelementid = dse.dataelementid
      JOIN dataset ds ON dse.datasetid = ds.datasetid
      WHERE dva.audittype = :types
        AND ds.uid = ANY(:ds)
        AND pe.iso = :pe
      ORDER BY dva.created DESC""",
        Set.of("types", "ds", "pe"),
        createEntriesQuery(params, createSpyQuery()));
  }
}
