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

public class DisplayNameUtils {
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
      final String originColumn, final String tablePrefix, final String columnAlias) {
    return ("case"
            // If all are empty, return null
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') = ''"
            + " then null"

            // If username only, return username
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') <> ''"
            + " then trim({prefix}.{column} ->> 'username')"

            // If firstName only, return firstName
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') <> ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') = ''"
            + " then trim({prefix}.{column} ->> 'firstName')"

            // If surname only, return surname
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') <> ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') = ''"
            + " then trim({prefix}.{column} ->> 'surname')"

            // If surname and firstName only, return surname + firstName
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') <> ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') <> ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') = ''"
            + " then concat(trim({prefix}.{column} ->> 'surname'), ', ', trim({prefix}.{column} ->> 'firstName'))"

            // If firstName and username only, return firstName + username
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') <> ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') <> ''"
            + " then concat(trim({prefix}.{column} ->> 'firstName'), ' (', trim({prefix}.{column} ->> 'username'), ')')"

            // If surname and username only, return surname + username
            + " when coalesce(trim({prefix}.{column} ->> 'surname'), '') <> ''"
            + " and coalesce(trim({prefix}.{column} ->> 'firstName'), '') = ''"
            + " and coalesce(trim({prefix}.{column} ->> 'username'), '') <> ''"
            + " then concat(trim({prefix}.{column} ->> 'surname'), ' (', trim({prefix}.{column} ->> 'username'), ')')"

            // If has all columns populated, return surname + firstName +
            // username
            + " else concat(trim({prefix}.{column} ->> 'surname'), ', ', trim({prefix}.{column} ->> 'firstName'), ' (', trim({prefix}.{column} ->> 'username'), ')') end"
            + " as {alias}")
        .replaceAll("\\{column}", originColumn)
        .replaceAll("\\{prefix}", tablePrefix)
        .replaceAll("\\{alias}", columnAlias);
  }
}
