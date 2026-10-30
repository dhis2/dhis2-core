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
package org.hisp.dhis.program.function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholder token emitted by {@link D2RelationshipCount} in place of an inline correlated
 * subquery against {@code analytics_rs_relationship}. The downstream analytics layer expands the
 * placeholder either back to the correlated subquery (engines that support it) or to a {@code LEFT
 * JOIN} reference (engines that do not, e.g. ClickHouse).
 */
public final class RelationshipCountPlaceholder {

  private RelationshipCountPlaceholder() {}

  public static final Pattern PATTERN = Pattern.compile("__D2RELCNT__\\(uid='([^']*)'\\)__");

  public static String format(String relationshipTypeUid) {
    return "__D2RELCNT__(uid='%s')__"
        .formatted(relationshipTypeUid == null ? "" : relationshipTypeUid);
  }

  /**
   * Expands every {@code __D2RELCNT__(uid='...')__} placeholder in {@code sql} into the correlated
   * scalar subquery shape that {@code d2:relationshipCount} originally produced, using {@code
   * outerAlias} as the alias of the enclosing analytics row source.
   */
  public static String expandCorrelated(String sql, String outerAlias) {
    if (sql == null || sql.isEmpty()) {
      return sql;
    }
    Matcher matcher = PATTERN.matcher(sql);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(
          out, Matcher.quoteReplacement(correlatedSubquery(matcher.group(1), outerAlias)));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  /**
   * Renders the correlated scalar subquery against {@code analytics_rs_relationship} that {@code
   * d2:relationshipCount} historically produced inline.
   *
   * <p>Two shapes are emitted, both correlated to the enclosing row via {@code
   * <outerAlias>.trackedentity}:
   *
   * <ul>
   *   <li>When {@code relationshipTypeUid} is {@code null} or blank, the subquery sums {@code
   *       relationship_count} across all relationship types for the tracked entity.
   *   <li>When a UID is supplied, the subquery returns the single pre-aggregated {@code
   *       relationship_count} row for that relationship type.
   * </ul>
   *
   * <p>The output is intended for engines that support correlated subqueries (e.g. PostgreSQL).
   * Engines that do not (e.g. ClickHouse) must rewrite the placeholder via a join-based expander
   * instead of calling this method.
   *
   * @param relationshipTypeUid the relationship-type UID to filter on, or {@code null}/blank for
   *     all relationship types.
   * @param outerAlias the alias of the enclosing analytics row source used to correlate {@code
   *     trackedentityid} (e.g. {@code ax} or {@code subax}).
   * @return the SQL text of the correlated subquery, including the surrounding parentheses.
   */
  public static String correlatedSubquery(String relationshipTypeUid, String outerAlias) {
    boolean filtered = relationshipTypeUid != null && !relationshipTypeUid.isEmpty();
    if (!filtered) {
      return """
              (select sum(relationship_count)
               from analytics_rs_relationship arr
               where arr.trackedentityid = %s.trackedentity)
              """
          .formatted(outerAlias);
    }
    return """
            (select relationship_count
             from analytics_rs_relationship arr
             where arr.trackedentityid = %s.trackedentity and relationshiptypeuid = '%s')
            """
        .formatted(outerAlias, relationshipTypeUid);
  }
}
