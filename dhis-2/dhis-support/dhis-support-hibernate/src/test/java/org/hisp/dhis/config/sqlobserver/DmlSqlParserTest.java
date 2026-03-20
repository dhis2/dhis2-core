/*
 * Copyright (c) 2004-2024, University of Oslo
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
import java.util.Set;
import org.hisp.dhis.config.sqlobserver.DmlSqlParser.DmlParseResult;
import org.hisp.dhis.dml.DmlOperation;
import org.junit.jupiter.api.Test;

class DmlSqlParserTest {

  @Test
  void isPossibleDml_insertReturnsTrue() {
    assertTrue(DmlSqlParser.isPossibleDml("INSERT INTO foo (a) VALUES (?)"));
  }

  @Test
  void isPossibleDml_updateReturnsTrue() {
    assertTrue(DmlSqlParser.isPossibleDml("UPDATE foo SET a = ? WHERE id = ?"));
  }

  @Test
  void isPossibleDml_deleteReturnsTrue() {
    assertTrue(DmlSqlParser.isPossibleDml("DELETE FROM foo WHERE id = ?"));
  }

  @Test
  void isPossibleDml_selectReturnsFalse() {
    assertFalse(DmlSqlParser.isPossibleDml("SELECT * FROM foo"));
  }

  @Test
  void isPossibleDml_nullReturnsFalse() {
    assertFalse(DmlSqlParser.isPossibleDml(null));
  }

  @Test
  void isPossibleDml_emptyReturnsFalse() {
    assertFalse(DmlSqlParser.isPossibleDml(""));
  }

  @Test
  void isPossibleDml_withMdcComment_insertReturnsTrue() {
    assertTrue(
        DmlSqlParser.isPossibleDml(
            "/* controller='MetadataController',method='POST' */ INSERT INTO foo (a) VALUES (?)"));
  }

  @Test
  void isPossibleDml_withMdcComment_selectReturnsFalse() {
    assertFalse(
        DmlSqlParser.isPossibleDml(
            "/* controller='MetadataController',method='GET' */ SELECT * FROM foo"));
  }

  @Test
  void isPossibleDml_caseInsensitive() {
    assertTrue(DmlSqlParser.isPossibleDml("insert into foo (a) values (?)"));
    assertTrue(DmlSqlParser.isPossibleDml("update foo set a=1"));
    assertTrue(DmlSqlParser.isPossibleDml("delete from foo"));
  }

  @Test
  void parse_insertExtractsTableAndColumns() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("INSERT INTO dataelement (uid, name, shortname) VALUES (?, ?, ?)");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.INSERT, r.getOperation());
    assertEquals("dataelement", r.getTableName());

    Map<String, Integer> params = r.getColumnToParamIndex();
    assertEquals(1, params.get("uid"));
    assertEquals(2, params.get("name"));
    assertEquals(3, params.get("shortname"));
  }

  @Test
  void parse_updateExtractsTableAndWhereParams() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "UPDATE dataelement SET name = ?, shortname = ? WHERE dataelement" + "id = ?");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.UPDATE, r.getOperation());
    assertEquals("dataelement", r.getTableName());

    // SET clause has 2 params, WHERE has 1 param at offset 3
    Map<String, Integer> params = r.getColumnToParamIndex();
    assertEquals(3, params.get("dataelementid"));
  }

  @Test
  void parse_updateExtractsUpdatedColumns() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "UPDATE dataelement SET name = ?, shortname = ?, lastupdated = ? WHERE dataelementid = ?");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(Set.of("name", "shortname", "lastupdated"), r.getUpdatedColumns());
  }

  @Test
  void parse_insertHasEmptyUpdatedColumns() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("INSERT INTO dataelement (uid, name) VALUES (?, ?)");

    assertTrue(result.isPresent());
    assertTrue(result.get().getUpdatedColumns().isEmpty());
  }

  @Test
  void parse_deleteHasEmptyUpdatedColumns() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("DELETE FROM dataelement WHERE dataelementid = ?");

    assertTrue(result.isPresent());
    assertTrue(result.get().getUpdatedColumns().isEmpty());
  }

  @Test
  void parse_deleteExtractsTableAndWhereParams() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("DELETE FROM dataelement WHERE dataelementid = ?");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.DELETE, r.getOperation());
    assertEquals("dataelement", r.getTableName());

    Map<String, Integer> params = r.getColumnToParamIndex();
    assertEquals(1, params.get("dataelementid"));
  }

  @Test
  void parse_selectReturnsEmpty() {
    Optional<DmlParseResult> result = DmlSqlParser.parse("SELECT * FROM dataelement");
    assertFalse(result.isPresent());
  }

  @Test
  void parse_withMdcCommentStripsProperly() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse(
            "/* controller='MetadataController',method='POST' */ INSERT INTO dataelement (uid) VALUES (?)");

    assertTrue(result.isPresent());
    assertEquals("dataelement", result.get().getTableName());
    assertEquals(DmlOperation.INSERT, result.get().getOperation());
  }

  @Test
  void parse_schemaPrefixedTableStripsSchema() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("INSERT INTO public.dataelement (uid) VALUES (?)");

    assertTrue(result.isPresent());
    assertEquals("dataelement", result.get().getTableName());
  }

  @Test
  void parse_invalidSqlReturnsEmpty() {
    Optional<DmlParseResult> result = DmlSqlParser.parse("THIS IS NOT SQL");
    assertFalse(result.isPresent());
  }

  @Test
  void parse_nullReturnsEmpty() {
    assertFalse(DmlSqlParser.parse(null).isPresent());
  }

  @Test
  void parse_emptyReturnsEmpty() {
    assertFalse(DmlSqlParser.parse("").isPresent());
  }

  @Test
  void parse_deleteWithMultipleWhereConditions() {
    Optional<DmlParseResult> result =
        DmlSqlParser.parse("DELETE FROM datavalue WHERE dataelementid = ? AND periodid = ?");

    assertTrue(result.isPresent());
    DmlParseResult r = result.get();
    assertEquals(DmlOperation.DELETE, r.getOperation());
    assertEquals("datavalue", r.getTableName());

    Map<String, Integer> params = r.getColumnToParamIndex();
    assertEquals(1, params.get("dataelementid"));
    assertEquals(2, params.get("periodid"));
  }

  @Test
  void stripLeadingComment_removesComment() {
    assertEquals(
        "INSERT INTO foo (a) VALUES (?)",
        DmlSqlParser.stripLeadingComment("/* test */ INSERT INTO foo (a) VALUES (?)"));
  }

  @Test
  void stripLeadingComment_noComment() {
    assertEquals(
        "INSERT INTO foo (a) VALUES (?)",
        DmlSqlParser.stripLeadingComment("INSERT INTO foo (a) VALUES (?)"));
  }

  // ── parseFast tests ──

  @Test
  void parseFast_insert() {
    var result = DmlSqlParser.parseFast("INSERT INTO dataelement (uid) VALUES (?)");
    assertTrue(result.isPresent());
    assertEquals(DmlOperation.INSERT, result.get().operation());
    assertEquals("dataelement", result.get().tableName());
  }

  @Test
  void parseFast_update() {
    var result = DmlSqlParser.parseFast("UPDATE dataelement SET name = ? WHERE uid = ?");
    assertTrue(result.isPresent());
    assertEquals(DmlOperation.UPDATE, result.get().operation());
    assertEquals("dataelement", result.get().tableName());
  }

  @Test
  void parseFast_delete() {
    var result = DmlSqlParser.parseFast("DELETE FROM dataelement WHERE uid = ?");
    assertTrue(result.isPresent());
    assertEquals(DmlOperation.DELETE, result.get().operation());
    assertEquals("dataelement", result.get().tableName());
  }

  @Test
  void parseFast_withMdcComment() {
    var result =
        DmlSqlParser.parseFast(
            "/* controller='MetadataController' */ INSERT INTO dataelement (uid) VALUES (?)");
    assertTrue(result.isPresent());
    assertEquals(DmlOperation.INSERT, result.get().operation());
    assertEquals("dataelement", result.get().tableName());
  }

  @Test
  void parseFast_schemaPrefix() {
    var result = DmlSqlParser.parseFast("INSERT INTO public.dataelement (uid) VALUES (?)");
    assertTrue(result.isPresent());
    assertEquals("dataelement", result.get().tableName());
  }

  @Test
  void parseFast_caseInsensitive() {
    var insert = DmlSqlParser.parseFast("insert into foo (a) values (?)");
    assertTrue(insert.isPresent());
    assertEquals(DmlOperation.INSERT, insert.get().operation());

    var update = DmlSqlParser.parseFast("update foo set a = ?");
    assertTrue(update.isPresent());
    assertEquals(DmlOperation.UPDATE, update.get().operation());

    var delete = DmlSqlParser.parseFast("delete from foo where id = ?");
    assertTrue(delete.isPresent());
    assertEquals(DmlOperation.DELETE, delete.get().operation());
  }

  @Test
  void parseFast_selectReturnsEmpty() {
    assertFalse(DmlSqlParser.parseFast("SELECT * FROM dataelement").isPresent());
  }

  @Test
  void parseFast_nullReturnsEmpty() {
    assertFalse(DmlSqlParser.parseFast(null).isPresent());
  }

  @Test
  void parseFast_emptyReturnsEmpty() {
    assertFalse(DmlSqlParser.parseFast("").isPresent());
  }
}
