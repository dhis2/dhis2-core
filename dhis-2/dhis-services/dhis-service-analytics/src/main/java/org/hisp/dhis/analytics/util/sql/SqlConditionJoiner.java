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

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * A utility class for joining multiple SQL conditions into a single WHERE clause. This class is
 * designed to handle conditions that may or may not include the "WHERE" keyword and ensures proper
 * formatting of the final SQL WHERE clause.
 */
@UtilityClass
public class SqlConditionJoiner {

  /**
   * Joins multiple SQL conditions into a single WHERE clause, separated by the <code>AND</code>
   * operator. Conditions that are null, empty, or consist solely of whitespace are ignored.
   *
   * <p><b>Behavior:</b>
   *
   * <ul>
   *   <li>Returns an empty string if no valid conditions are provided (null, empty, or all
   *       conditions are blank).
   *   <li>Removes leading "WHERE" or " where" from individual conditions before joining.
   *   <li>Joins valid conditions with the <code>AND</code> operator.
   *   <li>Adds a "WHERE" prefix to the final result if at least one valid condition is present.
   * </ul>
   *
   * <p><b>Examples:</b>
   *
   * <ul>
   *   <li><code>joinSqlConditions("column1 = 1", "column2 = 2")</code> returns <code>
   *       "where column1 = 1 and column2 = 2"</code>
   *   <li><code>joinSqlConditions("where column1 = 1", "where column2 = 2")</code> returns <code>
   *       "where column1 = 1 and column2 = 2"</code>
   *   <li><code>joinSqlConditions("", null, "column1 = 1")</code> returns <code>"where column1 = 1"
   *       </code>
   *   <li><code>joinSqlConditions()</code> returns <code>""</code>
   *   <li><code>joinSqlConditions(null, "", "   ")</code> returns <code>""</code>
   * </ul>
   *
   * @param conditions The SQL conditions to join. Can be null, empty, or contain null/blank
   *     strings.
   * @return A single WHERE clause combining the valid conditions with <code>AND</code> operators.
   *     Returns an empty string if no valid conditions are provided.
   */
  public static String joinSqlConditions(String... conditions) {

    if (conditions == null || conditions.length == 0) {
      return "";
    }

    // Filter out null, empty, or blank conditions
    String joinedConditions =
        Arrays.stream(conditions)
            .filter(condition -> condition != null && !condition.trim().isEmpty())
            .map(
                condition -> {
                  // Remove leading "where" or " where" and trim
                  String cleanedCondition = condition.trim();
                  if (cleanedCondition.toLowerCase().startsWith("where")) {
                    cleanedCondition = cleanedCondition.substring(5).trim();
                  }
                  return cleanedCondition;
                })
            .filter(
                cleanedCondition ->
                    !cleanedCondition.isEmpty()) // Filter out empty strings after cleaning
            .collect(Collectors.joining(" and "));

    // Return the result with "where" prefix if there are valid conditions
    return joinedConditions.isEmpty() ? "" : "where " + joinedConditions;
  }
}
