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
package org.hisp.dhis.datavalue.hibernate;

import static org.hisp.dhis.datavalue.DataExportParams.Order.*;
import static org.hisp.dhis.datavalue.hibernate.HibernateDataExportStore.createExportQuery;
import static org.hisp.dhis.period.Period.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.sql.AbstractQueryBuilderTest;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SQL generation as performed by {@link
 * HibernateDataExportStore#createExportQuery(DataExportParams, SQL.QueryAPI, UserDetails, long[])}
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
        Set.of(),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
        Set.of("deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
  }

  @Test
  void testFilter_Period() {
    DataExportParams params = DataExportParams.builder().periods(List.of(of("2020"))).build();
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
        Set.of("pe", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
        Set.of("de", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
        Set.of("deg", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
        Set.of("ou", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
          FROM ou_ids
          JOIN organisationunit root USING (organisationunitid)
          JOIN organisationunit ou ON ou.patharray && ARRAY[root.uid]
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
        Set.of("ou", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
            UNION (SELECT ougm.organisationunitid FROM orgunitgroupmembers ougm            JOIN orgunitgroup oug ON ougm.orgunitgroupid = oug.orgunitgroupid            WHERE oug.uid = ANY(:ougSuper))
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
        Set.of("ougSuper", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
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
            UNION (SELECT ougm.organisationunitid FROM orgunitgroupmembers ougm            JOIN orgunitgroup oug ON ougm.orgunitgroupid = oug.orgunitgroupid            WHERE oug.uid = ANY(:ougSuper))
          ) ou_all
          WHERE organisationunitid IS NOT NULL
        ),
        ou_with_descendants_ids AS (
          SELECT DISTINCT ou.organisationunitid
          FROM ou_ids
          JOIN organisationunit root USING (organisationunitid)
          JOIN organisationunit ou ON ou.patharray && ARRAY[root.uid]
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
        Set.of("ougSuper", "deleted"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
  }

  @Test
  void testFilter_OrgUnitGroup_NoSuper() {
    DataExportParams params =
        DataExportParams.builder().organisationUnitGroups(List.of(UID.of("oug23456789"))).build();
    UserDetails currentUser = UserDetails.empty().userOrgUnitIds(Set.of("ou123456789")).build();
    assertSQL(
        """
        WITH
        ou_ids AS (
          SELECT organisationunitid
          FROM (
            (SELECT cast(NULL as bigint) AS organisationunitid WHERE false)
            UNION (SELECT ougm.organisationunitid FROM orgunitgroupmembers ougm            JOIN orgunitgroup oug ON ougm.orgunitgroupid = oug.orgunitgroupid            JOIN organisationunit ou ON ougm.organisationunitid = ou.organisationunitid            WHERE oug.uid = ANY(:oug) AND ou.uid = ANY(:capture))
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
          AND EXISTS (SELECT 1 FROM unnest(CAST(:aocAllowed AS bigint[])) a(id) WHERE a.id = dv.attributeoptioncomboid)
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("oug", "capture", "deleted", "aocAllowed"),
        createExportQuery(params, createSpyQuery(), currentUser, new long[] {11L}));
  }

  @Test
  void testFilter_LastUpdated() {
    Date lastUpdated = new Date();
    DataExportParams params =
        DataExportParams.builder()
            .lastUpdated(lastUpdated)
            .includeDeleted(true)
            .orders(List.of(PE, CREATED, DE))
            .build();
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
        WHERE dv.lastupdated >= :lastUpdated
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("lastUpdated"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
    // pin the actual bound value: the WHERE clause must be backed by the exact filter date,
    // not merely a same-named parameter with an unrelated (or null) value
    assertEquals(lastUpdated, paramValue("lastUpdated"));
  }

  @Test
  void testFilter_LastUpdatedDuration() {
    Duration lastUpdatedDuration = Duration.ofDays(10000);
    long before = System.currentTimeMillis();
    DataExportParams params =
        DataExportParams.builder()
            .lastUpdatedDuration(lastUpdatedDuration)
            .includeDeleted(true)
            .orders(List.of(PE, CREATED, DE))
            .build();
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
        WHERE dv.lastupdated >= :lastUpdated
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("lastUpdated"),
        createExportQuery(params, createSpyQuery(), new SystemUser(), null));
    long after = System.currentTimeMillis();
    // pin that lastUpdatedDuration is actually converted into a real cutoff date
    // (now - duration), not left null/ignored, which would silently erase the WHERE line
    Object bound = paramValue("lastUpdated");
    assertTrue(bound instanceof Date, "lastUpdated parameter must be bound to a real Date");
    long boundMillis = ((Date) bound).getTime();
    long durationMillis = lastUpdatedDuration.toMillis();
    assertTrue(
        boundMillis >= before - durationMillis && boundMillis <= after - durationMillis,
        () ->
            "expected cutoff around now - duration, was: %s (now window: [%d, %d])"
                .formatted(bound, before - durationMillis, after - durationMillis));
  }

  @Test
  void testAocAccess_allowedAocs_semiJoinOnIds() {
    UserDetails user = UserDetails.empty().build();
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
        WHERE EXISTS (SELECT 1 FROM unnest(CAST(:aocAllowed AS bigint[])) a(id) WHERE a.id = dv.attributeoptioncomboid)
        ORDER BY pe.startdate, pe.enddate, dv.created, deid""",
        Set.of("aocAllowed"),
        createExportQuery(params, createSpyQuery(), user, new long[] {11L, 12L}));
    assertArrayEquals(new long[] {11L, 12L}, (long[]) paramValue("aocAllowed"));
  }

  @Test
  void testAocAccess_noList_noAocFilter() {
    DataExportParams params = DataExportParams.builder().includeDeleted(true).build();
    AtomicReference<String> captured = new AtomicReference<>();
    createExportQuery(params, SQL.spy(captured::set, (n, p) -> {}), new SystemUser(), null)
        .stream();
    String sql = captured.get();
    assertFalse(sql.contains("aocAllowed"), "no list must mean no AOC filter: " + sql);
    assertFalse(sql.contains("aoc_access"), "CTE must be gone: " + sql);
  }

  @Test
  void testAocAccess_emptyList_rejected() {
    // an empty list would erase the filter line and so lift the restriction
    DataExportParams params = DataExportParams.builder().includeDeleted(true).build();
    UserDetails user = UserDetails.empty().build();
    SQL.QueryAPI api = createSpyQuery();
    long[] none = new long[0];
    assertThrows(IllegalArgumentException.class, () -> createExportQuery(params, api, user, none));
  }

  @Test
  void testAocAccess_explicitAocs_keepAllowedListToo() {
    DataExportParams params =
        DataExportParams.builder()
            .includeDeleted(true)
            .attributeOptionCombos(List.of(UID.of("aoc23456789")))
            .build();
    AtomicReference<String> captured = new AtomicReference<>();
    createExportQuery(
        params, SQL.spy(captured::set, (n, p) -> {}), UserDetails.empty().build(), new long[] {11L})
        .stream();
    String sql = captured.get();
    assertTrue(sql.contains("aoc.uid = :aoc"), "explicit AOC filter expected: " + sql);
    assertTrue(sql.contains(":aocAllowed"), "allowed list must stay for explicit AOCs: " + sql);
  }

  @Test
  void testAllowedAocQuery_noExplicitAocs_allAttributeAocs() {
    AtomicReference<String> captured = new AtomicReference<>();
    createAllowedAocQueryWithSpy(List.of(), List.of(), captured);
    String sql = captured.get();
    assertTrue(sql.contains("SELECT DISTINCT ccoc.categoryoptioncomboid"), sql);
    assertFalse(sql.contains(":ds"), "no data sets must mean no data set scope: " + sql);
    assertFalse(sql.contains(":aoc"), "no explicit AOCs must mean no AOC restriction: " + sql);
    assertFalse(sql.contains("JOIN categoryoptioncombo aoc"), "no join needed: " + sql);
  }

  @Test
  void testAllowedAocQuery_explicitAocs_onlyThoseAreLookedUp() {
    AtomicReference<String> captured = new AtomicReference<>();
    createAllowedAocQueryWithSpy(List.of(UID.of("aoc23456789")), List.of(), captured);
    String sql = captured.get();
    assertTrue(sql.contains("aoc.uid = :aoc"), "lookup must be limited to explicit AOCs: " + sql);
  }

  @Test
  void testAllowedAocQuery_dataSets_onlyTheirAttributeCombos() {
    AtomicReference<String> captured = new AtomicReference<>();
    createAllowedAocQueryWithSpy(List.of(), List.of(UID.of("ds123456789")), captured);
    String sql = captured.get();
    assertTrue(
        sql.contains("cc.categorycomboid IN (SELECT ds.categorycomboid FROM dataset ds")
            && sql.contains("WHERE ds.uid = :ds"),
        "lookup must be limited to the data sets' attribute combos: " + sql);
  }

  private void createAllowedAocQueryWithSpy(
      List<UID> aocs, List<UID> dataSets, AtomicReference<String> captured) {
    HibernateDataExportStore.createAllowedAocQuery(
        SQL.spy(captured::set, (n, p) -> {}), UserDetails.empty().build(), aocs, dataSets)
        .stream();
  }
}
