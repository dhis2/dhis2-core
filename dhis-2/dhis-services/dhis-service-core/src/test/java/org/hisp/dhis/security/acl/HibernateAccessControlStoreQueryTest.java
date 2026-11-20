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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;

/** Tests the SQL of {@link HibernateAccessControlStore#createDataReadableAocQuery}. */
class HibernateAccessControlStoreQueryTest {

  @Test
  void testDataReadableAocQuery_noExplicitAocs_allAttributeAocs() {
    String sql = createDataReadableAocQuery(List.of());
    assertTrue(sql.contains("SELECT DISTINCT ccoc.categoryoptioncomboid"), sql);
    assertFalse(sql.contains(":aoc"), "no explicit AOCs must mean no AOC restriction: " + sql);
    assertFalse(sql.contains("JOIN categoryoptioncombo aoc"), "no join needed: " + sql);
  }

  @Test
  void testDataReadableAocQuery_explicitAocs_onlyThoseAreLookedUp() {
    String sql = createDataReadableAocQuery(List.of(UID.of("aoc23456789")));
    assertTrue(sql.contains("aoc.uid = :aoc"), "lookup must be limited to explicit AOCs: " + sql);
  }

  private static String createDataReadableAocQuery(List<UID> aocs) {
    AtomicReference<String> captured = new AtomicReference<>();
    HibernateAccessControlStore.createDataReadableAocQuery(
        SQL.spy(captured::set, (n, p) -> {}), UserDetails.empty().build(), aocs)
        .stream();
    return captured.get();
  }
}
