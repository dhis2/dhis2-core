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
package org.hisp.dhis.organisationunit;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.organisationunit.OrgTree.OrgTreeEntry;
import org.hisp.dhis.sql.DirectiveQueryBuilder;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.SQL;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOrgTreeStore implements OrgTreeStore {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public List<OrgUnitPath> queryPageMatches(OrgTreeParams params) {
    return createTreeQuery(params, createQueryBackendAdapter()).stream(String.class)
        .map(OrgUnitPath::of)
        .toList();
  }

  @Override
  public int countTotalMatches(OrgTreeParams params) {
    return createTreeQuery(params, createQueryBackendAdapter()).count();
  }

  static DirectiveQueryBuilder createTreeQuery(OrgTreeParams params, SQL.QueryAPI api) {
    String sql =
        """
        --@if :groups || :search
        WITH
        --@end
        --@if :groups
        ou_groups_member_ids AS (
          SELECT DISTINCT ougm.organisationunitid
          FROM orgunitgroup oug
          JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid
          WHERE oug.uid = ANY(:groups)
        )
        --@end
        --@if :groups && :search
        ,
        --@end
        --@if :search
        ou_search_match_ids AS (
          SELECT organisationunitid FROM organisationunit
          WHERE immutable_unaccent(name) ILIKE immutable_unaccent(:search)
          UNION
          SELECT organisationunitid FROM organisationunit_translation
          WHERE locale = :locale AND property = :property
            AND value_unaccent ILIKE immutable_unaccent(:search)
        )
        --@end
        SELECT
        --@replace :count count(*)
        ou.path
        --@end
        FROM organisationunit ou
        --@if :groups
        JOIN ou_groups_member_ids USING (organisationunitid)
        --@end
        --@if :search
        JOIN ou_search_match_ids USING (organisationunitid)
        --@end
        WHERE 1=1
          --@if :level
          AND ou.hierarchylevel = :level
          --@end
          --@if :depth1
          AND (
            ou.uid = ANY(:roots)
            OR ou.parentid IN (SELECT organisationunitid FROM organisationunit WHERE uid = ANY(:roots))
          )
          --@else
          AND ou.patharray && (:roots)::varchar[]
            --@if :depth
              --@if :depth1
            AND (ou.uid = ANY(:roots)
                 OR ou.parentid IN (SELECT organisationunitid FROM organisationunit
                                    WHERE uid = ANY(:roots)))
              --@else
            AND EXISTS (
              SELECT 1 FROM organisationunit root
              WHERE root.uid = ANY(:roots)
                AND ou.hierarchylevel <= (root.hierarchylevel + :depth)
            )
              --@end
            --@end
          --@end
          --@if :lastPath
          AND ou.path > :lastPath
          --@end
        --@if :count
        --@else
        ORDER BY ou.path
        --@end
        --@if :pageSize
        LIMIT :pageSize
        --@end
        """;
    String search = params.searchLike();
    Integer depth = params.depth();
    DirectiveQueryBuilder builder =
        SQL.ofDirective(sql, api)
            .setParameter("level", params.level())
            .setParameter("search", search)
            .setParameter("roots", params.roots())
            .setParameter("groups", params.groups())
            .setParameter("currentlyOpen", params.currentlyOpen())
            .setParameter("depth", depth)
            .setCondition("depth1", depth != null && depth == 1)
            .setParameter("pageSize", params.pageSize());
    if (search != null) {
      builder
          .setParameter("locale", params.locale().toString())
          .setParameter("property", params.translationPropertyKey());
    }
    return builder;
  }

  @Override
  public Stream<OrgTreeEntry> streamEntries(StreamEntriesParams params) {
    String sql =
        """
      SELECT
          ou.path,
          coalesce(jsonb_get_translated_value(ou.translations, :property, :locale), ou.name) as displayName,
          --@replace :assumeNoLeaf false as leaf
          NOT EXISTS (
              SELECT 1
              FROM organisationunit child
              WHERE child.parentid = ou.organisationunitid
          ) AS leaf
          --@end
      FROM organisationunit ou
      WHERE ou.uid = ANY(:ou)
      ORDER BY ou.hierarchylevel, displayName""";

    DirectiveQueryBuilder query =
        createQuery(sql)
            .setParameter("ou", params.orgUnits())
            .setParameter("locale", params.locale().toString())
            .setParameter("property", params.property())
            .setCondition("assumeNoLeaf", params.assumeNoLeaf());
    return query.isNullParameter("ou") ? Stream.of() : query.stream(JdbcOrgTreeStore::entry);
  }

  private static OrgTreeEntry entry(SQL.Row row) {
    return new OrgTreeEntry(OrgUnitPath.of(row.getString(0)), row.getString(1), row.getBoolean(2));
  }

  private DirectiveQueryBuilder createQuery(@Language("sql") String sql) {
    return SQL.ofDirective(sql, createQueryBackendAdapter());
  }

  @Nonnull
  private SQL.QueryAPI createQueryBackendAdapter() {
    return NativeSQL.of(new NamedParameterJdbcTemplate(jdbcTemplate));
  }
}
