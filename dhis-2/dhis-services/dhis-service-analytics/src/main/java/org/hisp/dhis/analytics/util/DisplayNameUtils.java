/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.db.sql.SqlBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DisplayNameUtils {
  /**
   * TODO Refactor and change this, too much code vs benefit.
   *
   * <p>This method will extract/compose the display name, based on the tracker JSON objects living
   * in the 'originColumn'. This method will return the display name respecting these rules:
   *
   * <p>If (last name, first name and username) are populated => Last name, first name (username)
   *
   * <p>If (only username is populated) => username
   *
   * <p>If (only first name is populated) => first name
   *
   * <p>If (only last name is populated) => last name
   *
   * <p>If (only last name and first name are populated) => last name, first name
   *
   * <p>If (only last name and username are populated) => last name (username)
   *
   * <p>If (only first name and username are populated) => first name (username)
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

    // Helper methods for the CASE conditions
    Function<String, String> isEmpty =
        expression -> sqlBuilder.coalesce(expression, "''") + " = ''";

    Function<String, String> isNotEmpty =
        expression -> sqlBuilder.coalesce(expression, "''") + " <> ''";

    return String.format(
        "case"
            +
            // All empty
            " when %s and %s and %s then null"
            +
            // Username only
            " when %s and %s and %s then %s"
            +
            // FirstName only
            " when %s and %s and %s then %s"
            +
            // Surname only
            " when %s and %s and %s then %s"
            +
            // Surname and FirstName
            " when %s and %s and %s then %s"
            +
            // FirstName and Username
            " when %s and %s and %s then %s"
            +
            // Surname and Username
            " when %s and %s and %s then %s"
            +
            // All fields
            " else %s end as %s",
        // All empty
        isEmpty.apply(surname),
        isEmpty.apply(firstName),
        isEmpty.apply(username),

        // Username only
        isEmpty.apply(surname),
        isEmpty.apply(firstName),
        isNotEmpty.apply(username),
        username,

        // FirstName only
        isEmpty.apply(surname),
        isNotEmpty.apply(firstName),
        isEmpty.apply(username),
        firstName,

        // Surname only
        isNotEmpty.apply(surname),
        isEmpty.apply(firstName),
        isEmpty.apply(username),
        surname,

        // Surname and FirstName
        isNotEmpty.apply(surname),
        isNotEmpty.apply(firstName),
        isEmpty.apply(username),
        formatNames(sqlBuilder, surname, "', '", firstName),

        // FirstName and Username
        isEmpty.apply(surname),
        isNotEmpty.apply(firstName),
        isNotEmpty.apply(username),
        formatNames(sqlBuilder, firstName, "' ('", username, "')'"),

        // Surname and Username
        isNotEmpty.apply(surname),
        isEmpty.apply(firstName),
        isNotEmpty.apply(username),
        formatNames(sqlBuilder, surname, "' ('", username, "')'"),

        // All fields
        formatNames(sqlBuilder, surname, "', '", firstName, "' ('", username, "')'"),
        columnAlias);
  }

  private static String extractJsonValue(
      SqlBuilder sqlBuilder, String tablePrefix, String originColumn, String path) {
    String jsonExtracted = sqlBuilder.jsonExtract(tablePrefix, originColumn, path);
    return sqlBuilder.trim(jsonExtracted);
  }

  private static String formatNames(SqlBuilder sqlBuilder, String... elements) {
    return sqlBuilder.concat(elements);
  }
}
