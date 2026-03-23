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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.dml.DmlOperation;

/**
 * Utility for parsing DML SQL statements and extracting table name, operation type, and
 * parameter-to-column position mappings for PK extraction.
 */
@Slf4j
public class DmlSqlParser {

  /**
   * Bounded LRU cache for parse results keyed by SQL string. Hibernate typically uses ~200-300
   * distinct SQL templates, so 512 entries provides ample headroom.
   */
  private static final int PARSE_CACHE_MAX_SIZE = 512;

  private static final Cache<String, Optional<DmlFastResult>> FAST_PARSE_CACHE =
      new Cache2kBuilder<String, Optional<DmlFastResult>>() {}.entryCapacity(PARSE_CACHE_MAX_SIZE)
          .eternal(true)
          .permitNullValues(true)
          .build();

  private DmlSqlParser() {}

  /** Lightweight result containing only the operation type and table name. */
  public record DmlFastResult(DmlOperation operation, String tableName) {}

  /**
   * Fast-path parser that extracts only operation type and table name via simple string
   * tokenization — no full SQL AST parsing. Suitable for the hot path where only these two fields
   * are needed.
   *
   * @return the operation and table name, or empty if the query is not a recognised DML statement
   */
  public static Optional<DmlFastResult> parseFast(String query) {
    if (query == null || query.isEmpty()) {
      return Optional.empty();
    }

    if (FAST_PARSE_CACHE.containsKey(query)) {
      return FAST_PARSE_CACHE.peek(query);
    }

    String sql = stripLeadingComment(query);
    Optional<DmlFastResult> result = doParseFast(sql);
    FAST_PARSE_CACHE.put(query, result);
    return result;
  }

  private static Optional<DmlFastResult> doParseFast(String sql) {
    String[] tokens = sql.split("\\s+", 4);
    if (tokens.length < 2) {
      return Optional.empty();
    }

    String keyword = tokens[0].toUpperCase(java.util.Locale.ROOT);
    return switch (keyword) {
      case "INSERT" -> {
        // INSERT INTO tablename ...
        if (tokens.length >= 3 && tokens[1].toUpperCase(java.util.Locale.ROOT).equals("INTO")) {
          yield Optional.of(new DmlFastResult(DmlOperation.INSERT, normalizeTableName(tokens[2])));
        }
        yield Optional.empty();
      }
      case "UPDATE" -> // UPDATE tablename SET ...
          Optional.of(new DmlFastResult(DmlOperation.UPDATE, normalizeTableName(tokens[1])));

      case "DELETE" -> {
        // DELETE FROM tablename ...
        if (tokens.length >= 3 && tokens[1].toUpperCase(java.util.Locale.ROOT).equals("FROM")) {
          yield Optional.of(new DmlFastResult(DmlOperation.DELETE, normalizeTableName(tokens[2])));
        }
        yield Optional.empty();
      }
      default -> Optional.empty();
    };
  }

  /** Strips schema prefix and lowercases the table name. */
  private static String normalizeTableName(String rawName) {
    String name = rawName.toLowerCase(java.util.Locale.ROOT);
    int dotIdx = name.lastIndexOf('.');
    if (dotIdx >= 0) {
      name = name.substring(dotIdx + 1);
    }
    return name;
  }

  @Value
  @Builder
  public static class DmlParseResult {
    DmlOperation operation;
    String tableName;

    /**
     * Maps column names in WHERE clause (for UPDATE/DELETE) or INSERT column list to 1-based JDBC
     * parameter position. Only populated for simple {@code column = ?} equality predicates.
     */
    Map<String, Integer> columnToParamIndex;

    /** Column names from the SET clause of an UPDATE statement. Empty for INSERT/DELETE. */
    @Builder.Default Set<String> updatedColumns = Set.of();
  }

  /**
   * Returns {@code true} if the query is possibly a DML statement (INSERT/UPDATE/DELETE). Strips a
   * leading block comment but not line comments.
   */
  public static boolean isPossibleDml(String query) {
    if (query == null || query.isEmpty()) {
      return false;
    }

    int i = 0;
    int len = query.length();

    // Skip leading whitespace
    while (i < len && Character.isWhitespace(query.charAt(i))) {
      i++;
    }

    // Skip leading /* MDC comment */
    if (i + 1 < len && query.charAt(i) == '/' && query.charAt(i + 1) == '*') {
      int endComment = query.indexOf("*/", i + 2);
      if (endComment < 0) {
        return false;
      }
      i = endComment + 2;
      // Skip whitespace after comment
      while (i < len && Character.isWhitespace(query.charAt(i))) {
        i++;
      }
    }

    if (i + 2 >= len) {
      return false;
    }

    // Check first 3 characters to distinguish INSERT/UPDATE/DELETE from DROP/DESCRIBE/IF etc.
    String prefix = query.substring(i, Math.min(i + 3, len)).toUpperCase(java.util.Locale.ROOT);
    return prefix.startsWith("INS") || prefix.startsWith("UPD") || prefix.startsWith("DEL");
  }

  /** Strips a leading SQL block comment (e.g. MDC context) from the query. */
  static String stripLeadingComment(String query) {
    int i = 0;
    int len = query.length();

    while (i < len && Character.isWhitespace(query.charAt(i))) {
      i++;
    }

    if (i + 1 < len && query.charAt(i) == '/' && query.charAt(i + 1) == '*') {
      int endComment = query.indexOf("*/", i + 2);
      if (endComment >= 0) {
        i = endComment + 2;
        while (i < len && Character.isWhitespace(query.charAt(i))) {
          i++;
        }
      }
    }

    return i > 0 ? query.substring(i) : query;
  }
}
