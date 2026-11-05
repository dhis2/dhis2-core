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
package org.hisp.dhis.organisationunit.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrgTreeParams;
import org.hisp.dhis.sql.DirectiveQueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/**
 * Verifies the SQL text produced by {@link HibernateOrgTreeStore#createTreeQuery}. Only the
 * assembled SQL is asserted on — parameter binding is a separate concern and the spy query accepts
 * it silently.
 *
 * <p>Note: these tests assume the {@code WITH} keyword in the template is gated on {@code --@if
 * :useJoin || :groups}. Without that, the {@link #minimalExistsScope()} and {@link
 * #groupsWithoutUseJoin()} cases emit a dangling {@code WITH} followed by {@code SELECT}.
 */
class OrgTreeStoreQueryBuilderTest {

  private static final UID ROOT = UID.of("aaaaaaaaaaa");
  private static final UID GROUP = UID.of("bbbbbbbbbbb");

  private static OrgTreeParams params(
      List<UID> groups,
      Integer level,
      Boolean currentlyOpen,
      String search,
      Integer depth) {
    return new OrgTreeParams(
        1,
        10,
        Locale.ENGLISH,
        List.of(ROOT),
        groups,
        level,
        currentlyOpen,
        search,
        false,
        depth);
  }

  private static void assertSQL(@Language("sql") String expected, OrgTreeParams params) {
    List<String> captured = new ArrayList<>();
    SQL.QueryAPI api = SQL.spy(captured::add, (n, p) -> {});
    DirectiveQueryBuilder builder = HibernateOrgTreeStore.createTreeQuery(params, api);
    assertNotNull(builder.stream().toList());
    assertEquals(1, captured.size(), "Expected exactly one SQL query");
    assertEquals(expected, captured.get(0));
  }

  // ============================== Cases ==============================

  @Test
  void minimalExistsScope() {
    // Large hierarchy -> useExists=true, useJoin=false. No groups -> no CTEs -> no WITH.
    assertSQL(
        """
        SELECT
        ou.path
        FROM organisationunit ou
        WHERE 1=1
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
        ORDER BY ou.path
        """,
        params(List.of(), null, null, null, null));
  }

  @Test
  void withLevel() {
    assertSQL(
        """
        SELECT
        ou.path
        FROM organisationunit ou
        WHERE 1=1
          AND ou.hierarchylevel = :level
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
        ORDER BY ou.path
        """,
        params(List.of(), 2, null, null, null));
  }

  @Test
  void withDepthAddsLineInsideExists() {
    assertSQL(
        """
        SELECT
        ou.path
        FROM organisationunit ou
        WHERE 1=1
          AND (EXISTS (
              SELECT 1
              FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND ou.hierarchylevel <= (root.hierarchylevel + :depth)
                AND (ou.patharray && ARRAY[root.uid])
          ))
        ORDER BY ou.path
        """,
        params(List.of(), null, null, null, 2));
  }

  @Test
  void groupsAndRoots() {
    assertSQL(
        """
        WITH
        groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        SELECT
        ou.path
        FROM organisationunit ou
        JOIN groups_member_ids oug ON ou.organisationunitid = oug.organisationunitid
        WHERE 1=1
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
        ORDER BY ou.path
        """,
        params(List.of(GROUP), null, null, null, null));
  }

  @Test
  void rootNoDepthAsFilter() {
    assertSQL(
        """
        SELECT
        ou.path
        FROM organisationunit ou
        WHERE 1=1
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
        ORDER BY ou.path
        """,
        params(List.of(), null, null, null, null));
  }

  @Test
  void groupsAsJoinRootsAsFilter() {
    assertSQL(
        """
        WITH
        groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        SELECT
        ou.path
        FROM organisationunit ou
        JOIN groups_member_ids oug ON ou.organisationunitid = oug.organisationunitid
        WHERE 1=1
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
        ORDER BY ou.path
        """,
        params(List.of(GROUP), null, null, null, null));
  }

  @Test
  void allFiltersButDepthCombined() {
    assertSQL(
        """
        WITH
        groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        SELECT
        ou.path
        FROM organisationunit ou
        JOIN groups_member_ids oug ON ou.organisationunitid = oug.organisationunitid
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1
          AND ou.hierarchylevel = :level
          AND :currentlyOpen = ((ou.openingdate IS NULL || ou.openingdate <= now()) && (ou.closeddate IS NULL || ou.closeddate > now()))
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
          AND (t.value_unaccent ILIKE immutable_unaccent(:search) OR immutable_unaccent(ou.name) ILIKE immutable_unaccent(:search))
        ORDER BY ou.path
        """,
        params(List.of(GROUP), 2, true, "foo", null));
  }

  @Test
  void depthForcesExists() {
    assertSQL(
        """
        SELECT
        ou.path
        FROM organisationunit ou
        WHERE 1=1
          AND (EXISTS (
              SELECT 1
              FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND ou.hierarchylevel <= (root.hierarchylevel + :depth)
                AND (ou.patharray && ARRAY[root.uid])
          ))
        ORDER BY ou.path
        """,
        params(List.of(), null, null, null, 2));
  }

  @Test
  void hierarchyLevelFilter() {
    assertSQL(
        """
        SELECT
        ou.path
        FROM organisationunit ou
        WHERE 1=1
          AND ou.hierarchylevel = :level
          AND ou.patharray && ARRAY(
            SELECT DISTINCT root.uid
            FROM organisationunit root
            WHERE root.uid = ANY(:roots)
          )
        ORDER BY ou.path
        """,
        params(List.of(), 2, null, null, null));
  }

  @Test
  void nullBoundParameterIsNotForwardedToTheBackend() {
    List<String> sqls = new ArrayList<>();
    Map<String, SQL.Param> sent = new LinkedHashMap<>();
    SQL.QueryAPI api = SQL.spy(sqls::add, sent::put);

    SQL.ofDirective("SELECT 1\n-- @if :x\nWHERE x = :x\n-- @end", api)
        .setParameter("x", (String) null)
        .stream()
        .toList();

    assertTrue(sent.isEmpty());
  }

  @Test
  void emptyArrayBoundParameterIsNotForwardedToTheBackend() {
    List<String> sqls = new ArrayList<>();
    Map<String, SQL.Param> sent = new LinkedHashMap<>();
    SQL.QueryAPI api = SQL.spy(sqls::add, sent::put);

    SQL.ofDirective("SELECT 1\n-- @if :ids\nWHERE x = ANY(:ids)\n-- @end", api)
        .setParameter("ids", new long[0])
        .stream()
        .toList();

    assertTrue(sent.isEmpty());
  }

  @Test
  void nonNullBoundParameterIsForwardedToTheBackend() {
    List<String> sqls = new ArrayList<>();
    Map<String, SQL.Param> sent = new LinkedHashMap<>();
    SQL.QueryAPI api = SQL.spy(sqls::add, sent::put);

    SQL.ofDirective("SELECT 1\n-- @if :x\nWHERE x = :x\n-- @end", api)
        .setParameter("x", "hello")
        .stream()
        .toList();

    assertEquals(Set.of("x"), sent.keySet());
  }
}
