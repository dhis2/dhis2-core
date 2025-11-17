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
import org.hisp.dhis.common.UID;
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
  void testFilter_All() {
    DataExportParams params = DataExportParams.builder().includeDeleted(true).build();
    assertSQL(
        """
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
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE 1=1
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_AllNotDeleted() {
    DataExportParams params = DataExportParams.builder().build();
    assertSQL(
        """
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
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE dv.deleted = :deleted
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_Period() {
    DataExportParams params =
        DataExportParams.builder().periods(List.of(getPeriodFromIsoString("2020"))).build();
    assertSQL(
        """
        WITH
        pe_ids AS (
          SELECT periodid
          FROM period
          WHERE iso = :pe
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
        Set.of("pe", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_DataElement() {
    DataExportParams params =
        DataExportParams.builder().dataElements(List.of(UID.of("de123456789"))).build();
    assertSQL(
        """
        WITH
        de_ids AS (
          SELECT dataelementid
          FROM (
                  (SELECT cast(NULL as bigint) AS dataelementid WHERE false)
            UNION (SELECT de.dataelementid FROM dataelement de WHERE de.uid = :de )
          ) de_all
          WHERE dataelementid IS NOT NULL
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
        JOIN de_ids ON dv.dataelementid = de_ids.dataelementid
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE dv.deleted = :deleted
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("de", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_DataElementGroup() {
    DataExportParams params =
        DataExportParams.builder().dataElementGroups(List.of(UID.of("deg23456789"))).build();
    assertSQL(
        """
        WITH
        de_ids AS (
          SELECT dataelementid
          FROM (
                  (SELECT cast(NULL as bigint) AS dataelementid WHERE false)
            UNION (SELECT degm.dataelementid FROM dataelementgroupmembers degm       JOIN dataelementgroup deg ON degm.dataelementgroupid = deg.dataelementgroupid WHERE deg.uid = ANY(:deg))
          ) de_all
          WHERE dataelementid IS NOT NULL
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
        JOIN de_ids ON dv.dataelementid = de_ids.dataelementid
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE dv.deleted = :deleted
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("deg", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_OrgUnit() {
    DataExportParams params =
        DataExportParams.builder().organisationUnits(List.of(UID.of("ou123456789"))).build();
    assertSQL(
        """
        WITH
        ou_ids AS (
          SELECT organisationunitid
          FROM (
            (SELECT cast(NULL as bigint) AS organisationunitid WHERE false)
            UNION (SELECT ou.organisationunitid FROM organisationunit ou WHERE ou.uid = :ou )
          ) ou_all
          WHERE organisationunitid IS NOT NULL
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
        JOIN ou_ids ON dv.sourceid = ou_ids.organisationunitid
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE dv.deleted = :deleted
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("ou", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_OrgUnitWithChildren() {
    DataExportParams params =
        DataExportParams.builder()
            .organisationUnits(List.of(UID.of("ou123456789"), UID.of("ou987654321")))
            .includeDescendants(true)
            .build();
    assertSQL(
        """
        WITH
        ou_ids AS (
          SELECT organisationunitid
          FROM (
            (SELECT cast(NULL as bigint) AS organisationunitid WHERE false)
            UNION (SELECT ou.organisationunitid FROM organisationunit ou WHERE ou.uid = ANY(:ou))
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
        JOIN ou_with_descendants_ids ON dv.sourceid = ou_with_descendants_ids.organisationunitid
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE dv.deleted = :deleted
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("ou", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_OrgUnitGroup() {
    DataExportParams params =
        DataExportParams.builder().organisationUnitGroups(List.of(UID.of("oug23456789"))).build();
    assertSQL(
        """
      WITH
      ou_ids AS (
        SELECT organisationunitid
        FROM (
          (SELECT cast(NULL as bigint) AS organisationunitid WHERE false)
          UNION (SELECT ougm.organisationunitid FROM orgunitgroupmembers ougm            JOIN orgunitgroup oug ON ougm.orgunitgroupid = oug.orgunitgroupid            JOIN organisationunit ou ON ougm.organisationunitid = ou.organisationunitid            WHERE oug.uid = ANY(:oug) AND (:super OR ou.uid = ANY(:capture)))
        ) ou_all
        WHERE organisationunitid IS NOT NULL
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
      JOIN ou_ids ON dv.sourceid = ou_ids.organisationunitid
      JOIN dataelement de ON dv.dataelementid = de.dataelementid
      JOIN period pe ON dv.periodid = pe.periodid
      JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
      JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
      JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
      WHERE dv.deleted = :deleted
      ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("oug", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }

  @Test
  void testFilter_OrgUnitGroupWithChildren() {
    DataExportParams params =
        DataExportParams.builder()
            .organisationUnitGroups(List.of(UID.of("oug23456789"), UID.of("oug98765432")))
            .includeDescendants(true)
            .build();
    assertSQL(
        """
        WITH
        ou_ids AS (
          SELECT organisationunitid
          FROM (
            (SELECT cast(NULL as bigint) AS organisationunitid WHERE false)
            UNION (SELECT ougm.organisationunitid FROM orgunitgroupmembers ougm            JOIN orgunitgroup oug ON ougm.orgunitgroupid = oug.orgunitgroupid            JOIN organisationunit ou ON ougm.organisationunitid = ou.organisationunitid            WHERE oug.uid = ANY(:oug) AND (:super OR ou.uid = ANY(:capture)))
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
        JOIN ou_with_descendants_ids ON dv.sourceid = ou_with_descendants_ids.organisationunitid
        JOIN dataelement de ON dv.dataelementid = de.dataelementid
        JOIN period pe ON dv.periodid = pe.periodid
        JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE dv.deleted = :deleted
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("oug", "deleted", "super", "capture"),
        createExportQuery(params, createSpyQuery(), new SystemUser()));
  }
}
