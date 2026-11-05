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

import java.util.ArrayList;
import java.util.List;
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
      int hierarchySize,
      List<UID> groups,
      Integer level,
      Boolean currentlyOpen,
      String search,
      Integer depth) {
    return new OrgTreeParams(
        1,
        10,
        hierarchySize,
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
        SELECT ou.path
        FROM organisationunit ou
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1

          AND (EXISTS (
              SELECT 1
              FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND (ou.patharray && ARRAY[root.uid])
          ))
        ORDER BY ou.path
        """,
        params(200, List.of(), null, null, null, null));
  }

  @Test
  void withLevel() {
    assertSQL(
        """
        SELECT ou.path
        FROM organisationunit ou
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1
          AND ou.hierarchylevel = :level

          AND (EXISTS (
              SELECT 1
              FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND (ou.patharray && ARRAY[root.uid])
          ))
        ORDER BY ou.path
        """,
        params(200, List.of(), 2, null, null, null));
  }

  @Test
  void withDepthAddsLineInsideExists() {
    assertSQL(
        """
        SELECT ou.path
        FROM organisationunit ou
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
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
        params(200, List.of(), null, null, null, 2));
  }

  @Test
  void groupsWithoutUseJoin() {
    // WITH is present because of the groups CTE, but roots CTE is not (useExists=true).
    // The ',' between CTEs must not appear (useJoin=false).
    assertSQL(
        """
        WITH
        groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        SELECT ou.path
        FROM organisationunit ou
        JOIN groups_member_ids oug ON ou.organisationunitid = oug.organisationunitid
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1

          AND (EXISTS (
              SELECT 1
              FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND (ou.patharray && ARRAY[root.uid])
          ))
        ORDER BY ou.path
        """,
        params(200, List.of(GROUP), null, null, null, null));
  }

  @Test
  void smallHierarchyUsesJoinInsteadOfExists() {
    // hierarchySize <= 100 -> useExists=false, useJoin=true.
    // Roots CTE is emitted; EXISTS block is not. The trailing blank line before
    // ORDER BY comes from the template layout — harmless, kept for readability.
    assertSQL(
        """
        WITH
        roots_with_descendants_ids AS (
          SELECT DISTINCT ou.organisationunitid
          FROM organisationunit root
          JOIN organisationunit ou ON ou.patharray && ARRAY[root.uid]
          WHERE root.uid = ANY(:roots)
        )
        SELECT ou.path
        FROM organisationunit ou
        JOIN roots_with_descendants_ids scope ON ou.organisationunitid = scope.organisationunitid
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1

        ORDER BY ou.path
        """,
        params(50, List.of(), null, null, null, null));
  }

  @Test
  void smallHierarchyWithGroupsEmitsBothCtesWithComma() {
    // useJoin=true AND groups set -> both CTEs, connected by a ',' on its own line.
    assertSQL(
        """
        WITH
        roots_with_descendants_ids AS (
          SELECT DISTINCT ou.organisationunitid
          FROM organisationunit root
          JOIN organisationunit ou ON ou.patharray && ARRAY[root.uid]
          WHERE root.uid = ANY(:roots)
        )
        ,
        groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        SELECT ou.path
        FROM organisationunit ou
        JOIN groups_member_ids oug ON ou.organisationunitid = oug.organisationunitid
        JOIN roots_with_descendants_ids scope ON ou.organisationunitid = scope.organisationunitid
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1

        ORDER BY ou.path
        """,
        params(50, List.of(GROUP), null, null, null, null));
  }

  @Test
  void allFiltersCombined() {
    // Large hierarchy + groups + level + currentlyOpen + search -> useExists=true, useJoin=false,
    // roots CTE absent, groups CTE present, all three WHERE filters active.
    assertSQL(
        """
        WITH
        groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        SELECT ou.path
        FROM organisationunit ou
        JOIN groups_member_ids oug ON ou.organisationunitid = oug.organisationunitid
        LEFT JOIN organisationunit_translation t ON t.organisationunitid = ou.organisationunitid AND t.locale = :locale AND t.property = :property
        WHERE 1=1
          AND ou.hierarchylevel = :level
          AND :currentlyOpen = ((ou.openingdate IS NULL || ou.openingdate <= now()) && (ou.closeddate IS NULL || ou.closeddate > now()))
          AND (t.value_unaccent ILIKE immutable_unaccent(:search) OR immutable_unaccent(ou.name) ILIKE immutable_unaccent(:search))

          AND (EXISTS (
              SELECT 1
              FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND (ou.patharray && ARRAY[root.uid])
          ))
        ORDER BY ou.path
        """,
        params(200, List.of(GROUP), 2, true, "foo", null));
  }
}
