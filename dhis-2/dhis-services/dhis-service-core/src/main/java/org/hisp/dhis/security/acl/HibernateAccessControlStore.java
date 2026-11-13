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
package org.hisp.dhis.security.acl;

import static org.hisp.dhis.query.JpaQueryUtils.generateSQlQueryForSharingCheck;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Implements the {@link AccessControlStore} with native SQL on the caller's Hibernate session, so
 * the lookups run in the caller's transaction.
 */
@Repository
@RequiredArgsConstructor
public class HibernateAccessControlStore implements AccessControlStore {

  private final EntityManager entityManager;

  @Override
  @Nonnull
  public DataReadableAocs getDataReadableAttributeOptionCombos(
      @Nonnull UserDetails user, @CheckForNull Collection<UID> limitTo) {
    return getDataReadableAttributeOptionCombos(NativeSQL.of(getSession()), user, limitTo);
  }

  @Override
  @Nonnull
  public DataReadableAocs getDataReadableAttributeOptionCombos(
      @Nonnull NamedParameterJdbcTemplate jdbcTemplate,
      @Nonnull UserDetails user,
      @CheckForNull Collection<UID> limitTo) {
    return getDataReadableAttributeOptionCombos(NativeSQL.of(jdbcTemplate), user, limitTo);
  }

  /** The one lookup both public methods run, on the API they pass. */
  @Nonnull
  private static DataReadableAocs getDataReadableAttributeOptionCombos(
      SQL.QueryAPI api, UserDetails user, @CheckForNull Collection<UID> limitTo) {
    if (user.isSuper()) return DataReadableAocs.unrestricted();
    // a single bigint column comes back as a plain number per row, not as an Object[] row
    return DataReadableAocs.of(
        createDataReadableAocQuery(api, user, limitTo).stream(Number.class)
            .mapToLong(Number::longValue)
            .toArray());
  }

  static QueryBuilder createDataReadableAocQuery(
      SQL.QueryAPI api, UserDetails user, @CheckForNull Collection<UID> limitTo) {
    String accessSql = generateSQlQueryForSharingCheck("co.sharing", user, LIKE_READ_DATA);
    String sql =
        """
      SELECT DISTINCT ccoc.categoryoptioncomboid
      FROM categorycombos_optioncombos ccoc
      JOIN categorycombo cc ON cc.categorycomboid = ccoc.categorycomboid
      JOIN categoryoptioncombo aoc ON aoc.categoryoptioncomboid = ccoc.categoryoptioncomboid
      WHERE (cc.name = 'default'
          OR cc.datadimensiontype = 'ATTRIBUTE'
          AND NOT EXISTS (
            SELECT 1
            FROM categoryoptioncombos_categoryoptions coc_co
            JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
            WHERE coc_co.categoryoptioncomboid = ccoc.categoryoptioncomboid
              AND NOT (:access)))
        AND aoc.uid = ANY(:aoc)""";
    return SQL.of(sql, api)
        .setParameter("aoc", limitTo)
        .setDynamicClause("access", accessSql)
        .eraseNullParameterLines()
        .eraseNullParameterJoinLine("aoc", "aoc")
        .useEqualsOverInForParameters("aoc");
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
