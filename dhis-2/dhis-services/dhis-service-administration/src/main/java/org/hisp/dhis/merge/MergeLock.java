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
package org.hisp.dhis.merge;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cluster-wide lock shared by all merge types, which ensures that only one merge of any type runs
 * at a time, also across instances in a clustered deployment. Backed by a transaction scoped
 * Postgres advisory lock, which is released automatically when the transaction commits or rolls
 * back.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class MergeLock {
  /** Key of the Postgres advisory lock shared by all merge types. */
  public static final long LOCK_KEY = 0x4D45_5247_454C_434BL; // "MERGELCK"

  private final JdbcTemplate jdbcTemplate;

  /**
   * Attempts to acquire the merge lock for the current transaction. Does not wait if the lock is
   * held by another merge. Must be called within a transaction.
   *
   * @return true if the lock was acquired, false if another merge is in progress.
   */
  public boolean tryAcquire() {
    Boolean acquired =
        jdbcTemplate.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, LOCK_KEY);

    return Boolean.TRUE.equals(acquired);
  }
}
