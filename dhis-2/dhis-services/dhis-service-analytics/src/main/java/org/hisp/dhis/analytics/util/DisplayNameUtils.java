/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.db.sql.SqlBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DisplayNameUtils {
  /**
   * Creates a display name from a user info JSON object.
   *
   * @param originColumn the original column from where the JSON values are extracted from
   * @param tablePrefix the prefix of the tracker table
   * @param columnAlias the alias of this column in the analytics database
   * @return the trimmed display name
   */
  public static String getDisplayName(
      String originColumn, String tablePrefix, String columnAlias, SqlBuilder sqlBuilder) {
    String surname = extractJsonValue(sqlBuilder, tablePrefix, originColumn, "surname");
    String firstName = extractJsonValue(sqlBuilder, tablePrefix, originColumn, "firstName");
    String username = extractJsonValue(sqlBuilder, tablePrefix, originColumn, "username");
    String expression = sqlBuilder.safeConcat(surname, "', '", firstName, "' ('", username, "')'");

    return String.format("%s as %s", expression, columnAlias);
  }

  private static String extractJsonValue(
      SqlBuilder sqlBuilder, String tablePrefix, String originColumn, String path) {
    String json = tablePrefix + "." + originColumn;
    String jsonExtracted = sqlBuilder.jsonExtract(json, path);
    return sqlBuilder.trim(jsonExtracted);
  }
}
