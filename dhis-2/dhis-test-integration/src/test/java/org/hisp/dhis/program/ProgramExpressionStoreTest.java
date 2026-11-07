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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests the persistence mapping of {@link ProgramExpression} to the programexpression table. */
@Transactional
class ProgramExpressionStoreTest extends PostgresIntegrationTestBase {

  @Autowired
  @Qualifier("org.hisp.dhis.program.ProgramExpressionStore")
  private GenericStore<ProgramExpression> store;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("saving takes the id from hibernate_sequence")
  void testIdComesFromHibernateSequence() {
    long before = lastSequenceValue();
    ProgramExpression first = save(new ProgramExpression("[DE:stage.deA]", "first"));
    ProgramExpression second = save(new ProgramExpression("[DE:stage.deB]", "second"));
    long after = lastSequenceValue();

    assertTrue(before < first.getId(), "first id must be allocated after " + before);
    assertTrue(first.getId() < second.getId());
    assertTrue(second.getId() <= after, "second id must not exceed the sequence value " + after);
  }

  @Test
  @DisplayName("expression and description are written to their columns")
  void testColumnsAreWritten() {
    ProgramExpression saved = save(new ProgramExpression("[PS:stage.DUE_DATE]", "due date"));

    assertEquals(
        List.of("[PS:stage.DUE_DATE]", "due date"),
        jdbcTemplate.queryForObject(
            "select expression, description from programexpression where programexpressionid = ?",
            (rs, row) -> List.of(rs.getString("expression"), rs.getString("description")),
            saved.getId()));
  }

  @Test
  @DisplayName("expression is a text column: a value longer than 255 characters round-trips")
  void testExpressionIsText() {
    String longExpression = "[DE:stage.de] + ".repeat(100);
    ProgramExpression saved = save(new ProgramExpression(longExpression, "long"));

    clearSession();

    assertEquals(longExpression, store.get(saved.getId()).getExpression());
  }

  @Test
  @DisplayName("description round-trips a value of the full 255 character column length")
  void testDescriptionHoldsMaxLength() {
    String description = "d".repeat(255);
    ProgramExpression saved = save(new ProgramExpression("[DE:stage.de]", description));

    clearSession();

    assertEquals(description, store.get(saved.getId()).getDescription());
  }

  @Test
  @DisplayName("expression and description are nullable")
  void testNullableColumns() {
    ProgramExpression saved = save(new ProgramExpression(null, null));

    clearSession();

    ProgramExpression reloaded = store.get(saved.getId());
    assertNull(reloaded.getExpression());
    assertNull(reloaded.getDescription());
  }

  @Test
  @DisplayName("updating expression and description of a loaded expression is persisted")
  void testUpdate() {
    ProgramExpression saved = save(new ProgramExpression("[DE:stage.old]", "old"));

    clearSession();

    ProgramExpression loaded = store.get(saved.getId());
    loaded.setExpression("[DE:stage.new]");
    loaded.setDescription("new");
    store.update(loaded);
    clearSession();

    ProgramExpression reloaded = store.get(saved.getId());
    assertEquals("[DE:stage.new]", reloaded.getExpression());
    assertEquals("new", reloaded.getDescription());
  }

  @Test
  @DisplayName("deleting an expression removes only its row")
  void testDelete() {
    ProgramExpression deleted = save(new ProgramExpression("[DE:stage.gone]", "gone"));
    ProgramExpression kept = save(new ProgramExpression("[DE:stage.kept]", "kept"));

    store.delete(store.get(deleted.getId()));
    clearSession();

    assertNull(store.get(deleted.getId()));
    assertEquals(0, countRows(deleted.getId()));
    assertEquals(1, countRows(kept.getId()));
  }

  private ProgramExpression save(ProgramExpression expression) {
    store.save(expression);
    entityManager.flush();
    return expression;
  }

  private long lastSequenceValue() {
    return jdbcTemplate.queryForObject("select last_value from hibernate_sequence", Long.class);
  }

  private int countRows(long id) {
    return jdbcTemplate.queryForObject(
        "select count(*) from programexpression where programexpressionid = ?", Integer.class, id);
  }
}
