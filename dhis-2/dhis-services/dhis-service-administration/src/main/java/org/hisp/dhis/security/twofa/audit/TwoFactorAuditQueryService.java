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
package org.hisp.dhis.security.twofa.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Native-SQL backed provider for the 2FA enrolment audit endpoints. Aggregates counts and lists
 * users directly against the {@code userinfo} / {@code userrolemembers} / {@code
 * userroleauthorities} tables, avoiding full-graph hydration of {@code User} entities and their
 * lazy {@code userRoles} collections.
 *
 * @author Morten Svanaes
 */
@Service
@RequiredArgsConstructor
public class TwoFactorAuditQueryService {

  public enum Status {
    ALL,
    ENABLED,
    DISABLED
  }

  private static final String ENABLED_TYPES_SQL_LIST = "('TOTP_ENABLED','EMAIL_ENABLED')";

  private final JdbcTemplate jdbcTemplate;

  /** Returns the row count of {@code userinfo} grouped by {@code twofactortype}. */
  public Map<TwoFactorType, Long> countByType() {
    Map<TwoFactorType, Long> result = new EnumMap<>(TwoFactorType.class);
    for (TwoFactorType type : TwoFactorType.values()) {
      result.put(type, 0L);
    }
    jdbcTemplate.query(
        "SELECT twofactortype, COUNT(*) FROM userinfo GROUP BY twofactortype",
        rs -> {
          String raw = rs.getString(1);
          if (raw != null) {
            try {
              result.put(TwoFactorType.valueOf(raw), rs.getLong(2));
            } catch (IllegalArgumentException ignore) {
              // Out-of-enum value in the column — drop it from the breakdown.
            }
          }
        });
    return result;
  }

  /**
   * Returns the count of users holding the {@code ALL} authority and how many of them have no
   * active 2FA. Done in a single query to keep the privileged-user detection on the DB side.
   */
  public PrivilegedCounts countPrivileged() {
    String sql =
        "SELECT COUNT(DISTINCT urm.userid) AS with_all,"
            + " COUNT(DISTINCT urm.userid) FILTER ("
            + "   WHERE u.twofactortype NOT IN "
            + ENABLED_TYPES_SQL_LIST
            + " ) AS with_all_missing"
            + " FROM userrolemembers urm"
            + " JOIN userroleauthorities ura ON ura.userroleid = urm.userroleid"
            + " JOIN userinfo u ON u.userinfoid = urm.userid"
            + " WHERE ura.authority = 'ALL'";
    PrivilegedCounts counts =
        jdbcTemplate.queryForObject(
            sql, (rs, n) -> new PrivilegedCounts(rs.getLong(1), rs.getLong(2)));
    return counts == null ? new PrivilegedCounts(0L, 0L) : counts;
  }

  /** Returns the number of users matching the given filter. */
  public int count(Status status, @CheckForNull List<TwoFactorType> types) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM userinfo WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendStatusClause(sql, status);
    appendTypeClause(sql, params, types);
    Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    return count == null ? 0 : count;
  }

  /**
   * Returns the matching user rows projected to the audit-row shape. {@code offset}/{@code limit}
   * are applied DB-side via {@code OFFSET} / {@code LIMIT}; pass {@code limit < 0} to return all
   * matches.
   */
  public List<UserAuditRow> list(
      Status status, @CheckForNull List<TwoFactorType> types, int offset, int limit) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT uid, username, name, twofactortype, lastlogin FROM userinfo WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendStatusClause(sql, status);
    appendTypeClause(sql, params, types);
    sql.append(" ORDER BY LOWER(username)");
    if (limit >= 0) {
      sql.append(" LIMIT ? OFFSET ?");
      params.add(limit);
      params.add(Math.max(0, offset));
    }
    return jdbcTemplate.query(
        sql.toString(),
        params.toArray(),
        (rs, n) ->
            new UserAuditRow(
                rs.getString("uid"),
                rs.getString("username"),
                rs.getString("name"),
                parseType(rs.getString("twofactortype")),
                rs.getTimestamp("lastlogin")));
  }

  private static void appendStatusClause(StringBuilder sql, Status status) {
    switch (status) {
      case ENABLED -> sql.append(" AND twofactortype IN ").append(ENABLED_TYPES_SQL_LIST);
      case DISABLED -> sql.append(" AND twofactortype NOT IN ").append(ENABLED_TYPES_SQL_LIST);
      case ALL -> {
        // no-op
      }
    }
  }

  private static void appendTypeClause(
      StringBuilder sql, List<Object> params, @CheckForNull List<TwoFactorType> types) {
    if (types == null || types.isEmpty()) return;
    sql.append(" AND twofactortype IN (");
    for (int i = 0; i < types.size(); i++) {
      sql.append(i == 0 ? "?" : ",?");
      params.add(types.get(i).name());
    }
    sql.append(")");
  }

  private static TwoFactorType parseType(@CheckForNull String raw) {
    if (raw == null) return TwoFactorType.NOT_ENABLED;
    try {
      return TwoFactorType.valueOf(raw);
    } catch (IllegalArgumentException e) {
      return TwoFactorType.NOT_ENABLED;
    }
  }

  public record PrivilegedCounts(long withAllAuthority, long withAllAuthorityMissing2FA) {}

  public record UserAuditRow(
      String uid, String username, String name, TwoFactorType twoFactorType, Date lastLogin) {}
}
