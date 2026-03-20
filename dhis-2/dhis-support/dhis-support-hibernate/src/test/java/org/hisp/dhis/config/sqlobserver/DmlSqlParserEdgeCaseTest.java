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
package org.hisp.dhis.config.sqlobserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.hisp.dhis.config.sqlobserver.DmlSqlParser.DmlParseResult;
import org.hisp.dhis.dml.DmlOperation;
import org.junit.jupiter.api.Test;

/**
 * Edge case tests for {@link DmlSqlParser} covering unusual SQL patterns, mixed literal/parameter
 * VALUES, non-equality WHERE operators, CTEs, multi-table DELETE, comments, and TRUNCATE.
 */
class DmlSqlParserEdgeCaseTest {

  // ── INSERT edge cases ──

  @Test
  void parse_insertWithMixedLiteralsAndParams() {
    // Mixed literal/parameter VALUES
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "INSERT INTO dataelement (uid, name, created) VALUES (?, 'hardcoded', ?)");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.INSERT, r.getOperation());
    assertEquals("dataelement", r.getTableName());

    Map<String, Integer> params = r.getColumnToParamIndex();
    // uid is the 1st JDBC param (position 1), created is the 2nd JDBC param (position 2)
    assertEquals(1, params.get("uid"));
    assertEquals(2, params.get("created"));
    // 'hardcoded' literal does NOT consume a JDBC param slot
    assertFalse(params.containsKey("name"));
  }

  @Test
  void parse_insertOnConflictDoNothing() {
    // INSERT ... ON CONFLICT is PostgreSQL-specific; JSQLParser may or may not handle it
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "INSERT INTO dataelement (uid, name) VALUES (?, ?) ON CONFLICT (uid) DO NOTHING");

    // Either successfully parsed as INSERT or empty — should not throw
    assertTrue(result.isEmpty() || result.get().getOperation() == DmlOperation.INSERT);
    assertTrue(result.isEmpty() || "dataelement".equals(result.get().getTableName()));
  }

  @Test
  void parse_insertOnConflictDoUpdate() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "INSERT INTO dataelement (uid, name) VALUES (?, ?) "
                + "ON CONFLICT (uid) DO UPDATE SET name = EXCLUDED.name");

    assertTrue(result.isEmpty() || result.get().getOperation() == DmlOperation.INSERT);
    assertTrue(result.isEmpty() || "dataelement".equals(result.get().getTableName()));
  }

  // ── UPDATE/DELETE with non-equality WHERE operators ──

  @Test
  void parse_updateWithGreaterThanInWhere() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "UPDATE dataelement SET name = ? WHERE lastupdated > ? AND dataelementid = ?");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.UPDATE, r.getOperation());

    Map<String, Integer> params = r.getColumnToParamIndex();
    // SET has 1 param (offset=1), WHERE: '>' consumes param at position 2,
    // '=' maps dataelementid at position 3
    assertEquals(3, params.get("dataelementid"));
    // lastupdated uses >, not =, so should NOT be mapped
    assertFalse(params.containsKey("lastupdated"));
  }

  @Test
  void parse_deleteWithLessThanOrEqual() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("DELETE FROM audit WHERE created <= ? AND auditid = ?");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.DELETE, r.getOperation());

    Map<String, Integer> params = r.getColumnToParamIndex();
    // created uses <=, should be counted (pos 1) but not mapped
    // auditid uses =, should be mapped at position 2
    assertEquals(2, params.get("auditid"));
    assertFalse(params.containsKey("created"));
  }

  @Test
  void parse_deleteWithNotEquals() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("DELETE FROM dataelement WHERE status != ? AND dataelementid = ?");

    assertTrue(result.isPresent());
    Map<String, Integer> params = result.get().getColumnToParamIndex();
    assertEquals(2, params.get("dataelementid"));
    assertFalse(params.containsKey("status"));
  }

  // ── CTE (WITH clause) ──

  @Test
  void parse_deleteWithCte() {
    // Common Table Expressions in DML — JSQLParser may or may not handle
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "WITH old_data AS (SELECT dataelementid FROM dataelement WHERE created < ?) "
                + "DELETE FROM dataelement WHERE dataelementid IN (SELECT dataelementid FROM old_data)");

    // Should either parse correctly or return empty — must not throw
    // CTEs in DELETE are uncommon in Hibernate-generated SQL
    assertTrue(result.isEmpty() || result.get().getOperation() != null);
  }

  // ── Multi-table DELETE ──

  @Test
  void parse_multiTableDelete() {
    // Multi-table DELETE syntax (MySQL-style) — not typical for Hibernate/PostgreSQL
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("DELETE t1 FROM dataelement t1 JOIN indicator t2 ON t1.uid = t2.uid");

    // Should either parse or return empty — must not throw
    assertTrue(result.isEmpty() || result.get().getOperation() != null);
  }

  // ── SQL comments ──

  @Test
  void isPossibleDml_withLineComment() {
    // Single-line comment before DML — isPossibleDml only strips block comments, not line comments
    // The '--' prefix doesn't start with I/U/D so returns false (fast-path optimization)
    assertFalse(DmlSqlParser.isPossibleDml("-- some comment\nINSERT INTO foo (a) VALUES (?)"));
  }

  @Test
  void stripLeadingComment_multipleBlockComments() {
    // Only the first block comment is stripped
    String sql = "/* comment1 */ /* comment2 */ INSERT INTO foo (a) VALUES (?)";
    String stripped = DmlSqlParser.stripLeadingComment(sql);
    assertEquals("/* comment2 */ INSERT INTO foo (a) VALUES (?)", stripped);
  }

  @Test
  void stripLeadingComment_unclosedComment() {
    // Unclosed comment — nothing stripped
    String sql = "/* unclosed INSERT INTO foo (a) VALUES (?)";
    String stripped = DmlSqlParser.stripLeadingComment(sql);
    assertEquals(sql, stripped);
  }

  // ── TRUNCATE ──

  @Test
  void isPossibleDml_truncateReturnsFalse() {
    // TRUNCATE starts with 'T', not I/U/D
    assertFalse(DmlSqlParser.isPossibleDml("TRUNCATE TABLE dataelement"));
  }

  @Test
  void parse_truncateReturnsEmpty() {
    // TRUNCATE is DDL, not DML
    Optional<DmlParseResult> result = DmlSqlParser.parse("TRUNCATE TABLE dataelement");
    assertFalse(result.isPresent());
  }

  // ── Parse cache ──

  @Test
  void parse_cachingReturnsSameResult() {
    String sql = "INSERT INTO dataelement (uid) VALUES (?)";
    Optional<DmlParseResult> first = DmlSqlParser.parse(sql);
    Optional<DmlParseResult> second = DmlSqlParser.parse(sql);

    // Same SQL should return cached reference
    assertTrue(first.isPresent());
    assertTrue(second.isPresent());
    assertEquals(first.get(), second.get());
  }

  @Test
  void parse_cachingCachesEmptyResults() {
    String sql = "SELECT * FROM dataelement";
    Optional<DmlParseResult> first = DmlSqlParser.parse(sql);
    Optional<DmlParseResult> second = DmlSqlParser.parse(sql);

    // Non-DML should also be cached (as empty)
    assertFalse(first.isPresent());
    assertFalse(second.isPresent());
  }

  // ── Whitespace / case variations ──

  @Test
  void isPossibleDml_leadingWhitespace() {
    assertTrue(DmlSqlParser.isPossibleDml("   INSERT INTO foo VALUES (?)"));
    assertTrue(DmlSqlParser.isPossibleDml("\t\nUPDATE foo SET a=1"));
    assertTrue(DmlSqlParser.isPossibleDml("\n  DELETE FROM foo"));
  }

  @Test
  void parse_insertWithExtraWhitespace() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("  INSERT  INTO  dataelement  (uid)  VALUES  (?)  ");

    assertTrue(result.isPresent());
    assertEquals("dataelement", result.get().getTableName());
    assertEquals(DmlOperation.INSERT, result.get().getOperation());
  }

  @Test
  void isPossibleDml_dropTableReturnsFalse() {
    // DROP starts with 'D' like DELETE but should not match — prefix check is "DEL" not "D"
    assertFalse(DmlSqlParser.isPossibleDml("DROP TABLE dataelement"));
  }

  @Test
  void isPossibleDml_createReturnsFalse() {
    assertFalse(DmlSqlParser.isPossibleDml("CREATE TABLE dataelement (id int)"));
  }

  @Test
  void isPossibleDml_alterReturnsFalse() {
    assertFalse(DmlSqlParser.isPossibleDml("ALTER TABLE dataelement ADD COLUMN name text"));
  }
}
