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

/**
 * Functional interface for building SQL JOIN conditions. The interface takes a table alias and
 * returns the corresponding JOIN condition.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * // Simple join on id
 * JoinCondition idJoin = alias -> alias + ".user_id = users.id";
 * // Usage: "LEFT JOIN orders o ON o.user_id = users.id"
 *
 * // Join with multiple conditions
 * JoinCondition activeUserJoin = alias ->
 *     alias + ".user_id = users.id AND " +
 *     alias + ".status = 'ACTIVE'";
 * // Usage: "LEFT JOIN orders o ON o.user_id = users.id AND o.status = 'ACTIVE'"
 *
 * // Join with date range
 * JoinCondition dateRangeJoin = alias ->
 *     alias + ".start_date <= CURRENT_DATE AND " +
 *     alias + ".end_date >= CURRENT_DATE";
 * // Usage: "LEFT JOIN periods p ON p.start_date <= CURRENT_DATE AND p.end_date >= CURRENT_DATE"
 * }</pre>
 *
 * <p>Typical usage in SelectBuilder:
 *
 * <pre>{@code
 * SelectBuilder builder = new SelectBuilder()
 *     .from("users", "u")
 *     .leftJoin("orders", "o", alias -> alias + ".user_id = u.id");
 * }</pre>
 */
@FunctionalInterface
public interface JoinCondition {
  /**
   * Builds a JOIN condition string using the provided table alias.
   *
   * @param alias the alias of the table being joined
   * @return the SQL JOIN condition string
   */
  String build(String alias);
}
