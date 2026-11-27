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
package org.hisp.dhis.dataintegrity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that {@link DataIntegrityStore} passes check SQL to PostgreSQL as written, so checks can
 * use PostgreSQL syntax such as {@code ::} casts and the jsonb {@code ?} operator.
 */
@Transactional
class DataIntegrityStoreTest extends PostgresIntegrationTestBase {

  private static final DataIntegrityCheck CHECK = DataIntegrityCheck.builder().name("test").build();

  @Autowired private DataIntegrityStore store;

  @Test
  void testSummaryWithCastsAndJsonbOperator() {
    String sql =
        """
        SELECT COUNT(*)::int AS value, (100.0 * COUNT(*) / 2)::numeric AS percent
        FROM (VALUES ('{"a": 1}'::jsonb), ('{"b": 1}'::jsonb)) AS v(j)
        WHERE j ? 'a'
        """;

    DataIntegritySummary summary = store.querySummary(CHECK, sql);

    assertEquals(1, summary.getCount());
    assertEquals(50.0, summary.getPercentage());
  }

  @Test
  void testDetailsWithCastsAndRefs() {
    String sql =
        """
        SELECT 'uid1'::text, 'Name', 'value :notAParameter ' || 42::text, ARRAY['b', 'a']::text[]
        """;

    List<DataIntegrityIssue> issues = store.queryDetails(CHECK, sql).getIssues();

    assertEquals(1, issues.size());
    DataIntegrityIssue issue = issues.get(0);
    assertEquals("uid1", issue.getId());
    assertEquals("Name", issue.getName());
    assertEquals("value :notAParameter 42", issue.getComment());
    assertEquals(List.of("b", "a"), issue.getRefs());
  }

  @Test
  void testDetailsWithoutRefsColumn() {
    List<DataIntegrityIssue> issues =
        store.queryDetails(CHECK, "SELECT 'uid1', 'Name', NULL::text").getIssues();

    assertEquals(1, issues.size());
    assertEquals(List.of(), issues.get(0).getRefs());
  }
}
