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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Durable dual-generation store backed by {@code authz_version}.
 *
 * @author Morten Svanæs
 */
@RequiredArgsConstructor
public class JdbcAuthzVersionStore implements AuthzVersionStore {

  private static final String SCOPE_USER = "user";
  private static final String SCOPE_ROLE = "role";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public long getUserGen(@Nonnull String username) {
    return getGen(SCOPE_USER, username);
  }

  @Override
  public long getRoleGen(@Nonnull String roleUid) {
    return getGen(SCOPE_ROLE, roleUid);
  }

  @Override
  public long bumpUserGen(@Nonnull String username) {
    return bumpGen(SCOPE_USER, username);
  }

  @Override
  public long bumpRoleGen(@Nonnull String roleUid) {
    return bumpGen(SCOPE_ROLE, roleUid);
  }

  @Override
  public void bumpUserGens(@Nonnull Collection<String> usernames) {
    for (String username : usernames) {
      if (username != null && !username.isBlank()) {
        bumpUserGen(username);
      }
    }
  }

  private long getGen(String scope, String keyName) {
    List<Long> rows =
        jdbcTemplate.query(
            "select gen from authz_version where scope = ? and key_name = ?",
            (rs, rowNum) -> rs.getLong(1),
            scope,
            keyName);
    return rows.isEmpty() ? 0L : rows.get(0);
  }

  private long bumpGen(String scope, String keyName) {
    jdbcTemplate.update(
        """
        insert into authz_version (scope, key_name, gen, updated_at)
        values (?, ?, 1, now())
        on conflict (scope, key_name)
        do update set gen = authz_version.gen + 1, updated_at = now()
        """,
        scope,
        keyName);
    return getGen(scope, keyName);
  }
}
