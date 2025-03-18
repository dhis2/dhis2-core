/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents SQL conditions that can be used in WHERE and HAVING clauses. This interface provides a
 * type-safe way to build SQL conditions using composition and various condition types (AND, OR,
 * NOT).
 */
public sealed interface Condition
    permits Condition.And,
        Condition.Not,
        Condition.Or,
        Condition.Raw,
        NotCondition,
        SimpleCondition {

  Pattern WHERE_AND_PATTERN = Pattern.compile("^(?i)(where|and)\\b.*");
  Pattern WHERE_AND_REPLACE_PATTERN = Pattern.compile("^(?i)(where|and)\\s+");

  /**
   * Converts the condition to its SQL string representation.
   *
   * @return the SQL string representation of the condition
   */
  String toSql();

  /**
   * Represents a raw SQL condition string. Automatically removes leading "WHERE" or "AND" keywords.
   *
   * <p>Examples:
   *
   * <pre>{@code
   * // These will produce the same SQL:
   * new Raw("active = true")            -> "active = true"
   * new Raw("WHERE active = true")      -> "active = true"
   * new Raw("AND active = true")        -> "active = true"
   *
   * // Complex conditions are preserved:
   * new Raw("WHERE age >= 18 AND status IN ('ACTIVE', 'PENDING')")
   *     -> "age >= 18 AND status IN ('ACTIVE', 'PENDING')"
   *
   * // Case insensitive keyword removal:
   * new Raw("where active = true")      -> "active = true"
   * new Raw("WHERE active = true")      -> "active = true"
   * new Raw("And active = true")        -> "active = true"
   * }</pre>
   */
  record Raw(String sql) implements Condition {
    @Override
    public String toSql() {
      if (sql == null || sql.trim().isEmpty()) {
        return "";
      }

      // Remove only the first occurrence of WHERE or AND
      String cleaned = sql.trim();
      if (WHERE_AND_PATTERN.matcher(cleaned.toLowerCase()).matches()) {
        cleaned = WHERE_AND_REPLACE_PATTERN.matcher(cleaned).replaceFirst("");
      }

      return cleaned.trim();
    }
  }

  /**
   * Represents multiple conditions combined with AND operator. Empty conditions are filtered out
   * from the final SQL.
   */
  record And(List<Condition> conditions) implements Condition {
    @Override
    public String toSql() {
      return conditions.stream()
          .map(Condition::toSql)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.joining(" and "));
    }
  }

  /**
   * Represents multiple conditions combined with OR operator. Empty conditions are filtered out and
   * each condition is wrapped in parentheses.
   */
  record Or(List<Condition> conditions) implements Condition {
    @Override
    public String toSql() {
      return conditions.stream()
          .map(Condition::toSql)
          .filter(s -> !s.isEmpty())
          .map(sql -> "(" + sql + ")")
          .collect(Collectors.joining(" or "));
    }
  }

  /** Represents a negated condition. If the inner condition is empty, returns an empty string. */
  record Not(Condition condition) implements Condition {
    @Override
    public String toSql() {
      String sql = condition.toSql();
      return sql.isEmpty() ? "" : "not (" + sql + ")";
    }
  }

  /**
   * Creates a condition from a raw SQL string.
   *
   * @param sql the SQL condition string
   * @return a new Raw condition
   */
  static Condition raw(String sql) {
    return new Raw(sql);
  }

  /**
   * Combines multiple conditions with AND operator.
   *
   * @param conditions the conditions to combine
   * @return a new And condition containing all provided conditions
   */
  static Condition and(Condition... conditions) {
    return new And(Arrays.asList(conditions).stream().filter(Objects::nonNull).toList());
  }

  /**
   * Combines a collection of conditions with AND operator.
   *
   * @param conditions the collection of conditions to combine
   * @return a new And condition containing all provided conditions
   */
  static Condition and(Collection<Condition> conditions) {
    return new And(new ArrayList<>(conditions));
  }

  /**
   * Combines multiple conditions with OR operator.
   *
   * @param conditions the conditions to combine
   * @return a new Or condition containing all provided conditions
   */
  static Condition or(Condition... conditions) {
    return new Or(Arrays.asList(conditions));
  }

  /**
   * Combines a collection of conditions with OR operator.
   *
   * @param conditions the collection of conditions to combine
   * @return a new Or condition containing all provided conditions
   */
  static Condition or(Collection<Condition> conditions) {
    return new Or(new ArrayList<>(conditions));
  }

  /**
   * Creates a negated condition.
   *
   * @param condition the condition to negate
   * @return a new Not condition wrapping the provided condition
   */
  static Condition not(Condition condition) {
    return new Not(condition);
  }
}
