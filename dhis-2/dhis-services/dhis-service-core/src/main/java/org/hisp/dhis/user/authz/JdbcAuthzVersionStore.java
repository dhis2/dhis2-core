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
package org.hisp.dhis.user.authz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

/**
 * JDBC generation-stamp store for UserDetails soft-refresh.
 *
 * <p>Every bump advances the global epoch row in the same ambient transaction as the entity change
 * that triggered it (JdbcTemplate joins the caller's {@code @Transactional} boundary). Therefore a
 * reader can never observe a gen/epoch value without also seeing the committed data that caused it.
 * This atomic visibility guarantee is the linchpin of soft-refresh correctness.
 *
 * @author Morten Svanæs
 */
@Repository("org.hisp.dhis.user.authz.AuthzVersionStore")
public class JdbcAuthzVersionStore implements AuthzVersionStore {

  private static final String SCOPE_USER = "user";
  private static final String SCOPE_ROLE = "role";
  private static final String SCOPE_EPOCH = "epoch";
  private static final String KEY_EPOCH = "epoch";

  // Update-first bump: the hot path (row exists) is one portable UPDATE. Only the first-ever bump
  // of a key takes the insert path, which is dialect-branched because H2 (used by the H2 test
  // contexts, even in PostgreSQL mode) does not support ON CONFLICT at all, while on Postgres
  // ON CONFLICT DO NOTHING is the only race-safe insert that cannot abort the enclosing
  // transaction (a plain INSERT losing a concurrent race would poison the import transaction).
  private static final String INCREMENT_SQL =
      """
      update authz_version set gen = gen + 1, updated_at = now()
      where scope = :scope and key_name = :key
      """;

  private static final String INSERT_SQL =
      "insert into authz_version (scope, key_name, gen, updated_at) values (:scope, :key, 1, now())";

  private static final String INSERT_IGNORE_PG_SQL =
      INSERT_SQL + " on conflict (scope, key_name) do nothing";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /** Lazily resolved: true on PostgreSQL, false on H2 test databases. */
  private volatile Boolean onConflictSupported;

  public JdbcAuthzVersionStore(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public long getEpoch() {
    try {
      Long value =
          jdbcTemplate.queryForObject(
              "select gen from authz_version where scope = 'epoch' and key_name = 'epoch'",
              new MapSqlParameterSource(),
              Long.class);
      return value == null ? 0L : value;
    } catch (EmptyResultDataAccessException ex) {
      return 0L;
    }
  }

  @Override
  public long getMaxGen(@Nonnull String userUid, @Nonnull Collection<String> roleUids) {
    Long value;
    if (roleUids.isEmpty()) {
      value =
          jdbcTemplate.queryForObject(
              """
              select coalesce(max(gen), 0) from authz_version
              where scope = 'user' and key_name = :user
              """,
              new MapSqlParameterSource("user", userUid),
              Long.class);
    } else {
      value =
          jdbcTemplate.queryForObject(
              """
              select coalesce(max(gen), 0) from authz_version
              where (scope = 'user' and key_name = :user)
                 or (scope = 'role' and key_name in (:roles))
              """,
              new MapSqlParameterSource().addValue("user", userUid).addValue("roles", roleUids),
              Long.class);
    }
    return value == null ? 0L : value;
  }

  @Override
  public void bumpUserGen(@Nonnull String userUid) {
    bump(SCOPE_USER, userUid);
    // Epoch last: deadlock-avoidance order (entity key before global epoch).
    bump(SCOPE_EPOCH, KEY_EPOCH);
  }

  @Override
  public void bumpRoleGen(@Nonnull String roleUid) {
    bump(SCOPE_ROLE, roleUid);
    // Epoch last: deadlock-avoidance order (entity key before global epoch).
    bump(SCOPE_EPOCH, KEY_EPOCH);
  }

  @Override
  public void bumpUserGens(@Nonnull Collection<String> userUids) {
    // Distinct, non-blank, sorted for stable lock order.
    TreeSet<String> distinct = new TreeSet<>();
    for (String uid : userUids) {
      if (uid == null) {
        continue;
      }
      String trimmed = uid.trim();
      if (!trimmed.isEmpty()) {
        distinct.add(trimmed);
      }
    }
    if (distinct.isEmpty()) {
      return;
    }

    List<Map<String, Object>> batchMaps = new ArrayList<>(distinct.size());
    for (String uid : distinct) {
      batchMaps.add(Map.of("scope", SCOPE_USER, "key", uid));
    }
    int[] updated =
        jdbcTemplate.batchUpdate(INCREMENT_SQL, SqlParameterSourceUtils.createBatch(batchMaps));
    List<String> missing = new ArrayList<>();
    int i = 0;
    for (String uid : distinct) {
      if (updated[i++] == 0) {
        missing.add(uid);
      }
    }
    for (String uid : missing) {
      insertFresh(SCOPE_USER, uid);
    }

    // One epoch bump for the whole batch. Epoch last for deadlock avoidance.
    bump(SCOPE_EPOCH, KEY_EPOCH);
  }

  private void bump(String scope, String key) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("scope", scope).addValue("key", key);
    if (jdbcTemplate.update(INCREMENT_SQL, params) > 0) {
      return;
    }
    insertFresh(scope, key);
  }

  /** Cold path: first-ever bump of this key. */
  private void insertFresh(String scope, String key) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("scope", scope).addValue("key", key);
    if (isOnConflictSupported()) {
      if (jdbcTemplate.update(INSERT_IGNORE_PG_SQL, params) == 0) {
        // Lost the first-insert race; the winner has committed, increment its row.
        jdbcTemplate.update(INCREMENT_SQL, params);
      }
    } else {
      // H2 test contexts: no ON CONFLICT support, but no concurrent bumps either.
      jdbcTemplate.update(INSERT_SQL, params);
    }
  }

  private boolean isOnConflictSupported() {
    Boolean cached = onConflictSupported;
    if (cached == null) {
      cached =
          Boolean.TRUE.equals(
              jdbcTemplate
                  .getJdbcTemplate()
                  .execute(
                      (java.sql.Connection con) ->
                          "PostgreSQL"
                              .equalsIgnoreCase(con.getMetaData().getDatabaseProductName())));
      onConflictSupported = cached;
    }
    return cached;
  }
}
