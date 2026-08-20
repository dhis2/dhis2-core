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

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrgTree.OrgTreeEntry;
import org.hisp.dhis.organisationunit.OrgTreeParams;
import org.hisp.dhis.organisationunit.OrgTreeStore;
import org.hisp.dhis.organisationunit.OrgUnitPath;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HibernateOrgTreeStore implements OrgTreeStore {

  private final EntityManager entityManager;

  @Override
  public List<OrgUnitPath> queryPageMatches(OrgTreeParams params) {
    return createTreeQuery(params, NativeSQL.of(getSession())).stream(String.class)
        .map(OrgUnitPath::of)
        .toList();
  }

  @Override
  public int countTotalMatches(OrgTreeParams params) {
    return createTreeQuery(params, NativeSQL.of(getSession())).count();
  }

  static QueryBuilder createTreeQuery(OrgTreeParams params, SQL.QueryAPI api) {
    String sql =
        """
      WITH
      roots_with_descendants_ids AS (
        SELECT DISTINCT ou.organisationunitid
        FROM organisationunit root
        JOIN organisationunit ou
          ON (root.organisationunitid = ou.parentid OR ou.path LIKE root.path || '%')
          AND ou.hierarchylevel <= (root.hierarchylevel + :depth)
        WHERE root.uid = ANY(:roots)
      ),
      groups_member_ids AS (
        SELECT DISTINCT ougm.organisationunitid
        FROM orgunitgroup oug
        JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
        WHERE oug.uid = ANY(:groups)
      ),
      set_member_ids AS (
        SELECT DISTINCT ougm.organisationunitid
        FROM orgunitgroupset ougs
        JOIN orgunitgroupsetmembers ougsm ON ougs.orgunitgroupsetid = ougsm.orgunitgroupsetid
        JOIN orgunitgroupmembers ougm ON ougsm.orgunitgroupid = ougm.orgunitgroupid
        WHERE ougs.uid = ANY(:gropSets)
      )
      SELECT ou.path
      FROM organisationunit ou
      JOIN roots_with_descendants_ids ON ou.organisationunitid = roots_with_descendants_ids.organisationunitid
      JOIN groups_member_ids ON ou.organisationunitid = groups_member_ids.organisationunitid
      JOIN set_member_ids ON ou.organisationunitid = set_member_ids.organisationunitid
      WHERE 1=1
        AND ou.hierarchylevel = :level
        AND ou.name ilike :search
        AND ou.shortname ilike :shortsearch
        AND :currentlyOpen = ((ou.openingdate IS NULL || ou.openingdate <= now()) && (ou.closeddate IS NULL || ou.closeddate > now()))
      ORDER BY ou.path

      """;
    int offset = (params.page() - 1) * params.pageSize();
    return SQL.of(sql, api)
        .setParameter("level", params.level())
        .setParameter("search", params.shortName() ? null : "%" + params.search() + "%")
        .setParameter("shortsearch", params.shortName() ? "%" + params.search() + "%" : null)
        .setParameter("roots", params.roots())
        .setParameter("groups", params.groups())
        .setParameter("gropSets", params.groupSets())
        .setParameter("currentlyOpen", params.currentlyOpen())
        .setParameter("depth", params.depth())
        .eraseNullParameterLines()
        .eraseJoinLine("roots_with_descendants_ids", params.roots().isEmpty())
        .eraseJoinLine("groups_member_ids", params.groups().isEmpty())
        .eraseJoinLine("set_member_ids", params.groupSets().isEmpty())
        .setOffset(offset)
        .setLimit(params.pageSize());
  }

  @Override
  public Stream<OrgTreeEntry> streamEntries(Locale locale, Stream<UID> orgUnits) {
    String sql =
        """
      SELECT
          ou.path,
          ou.name as displayName,
          NOT EXISTS (
              SELECT 1
              FROM organisationunit child
              WHERE child.parentid = ou.organisationunitid
          ) AS leaf
      FROM organisationunit ou
      WHERE ou.uid = ANY(:ou)
      ORDER BY ou.hierarchylevel, displayName""";
    QueryBuilder query = createQuery(sql).setParameter("ou", orgUnits);
    return query.isNullParameter("ou") ? Stream.of() : query.stream(HibernateOrgTreeStore::entry);
  }

  private static OrgTreeEntry entry(SQL.Row row) {
    return new OrgTreeEntry(OrgUnitPath.of(row.getString(0)), row.getString(1), row.getBoolean(2));
  }

  private QueryBuilder createQuery(@Language("sql") String sql) {
    return SQL.of(sql, NativeSQL.of(getSession()));
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
