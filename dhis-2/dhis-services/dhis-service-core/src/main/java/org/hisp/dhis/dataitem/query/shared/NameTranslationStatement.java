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
package org.hisp.dhis.dataitem.query.shared;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;

import lombok.NoArgsConstructor;

/**
 * Provides common statements related to name translation.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class NameTranslationStatement {
  /**
   * This method will join the translations column for the given table.
   *
   * @param table the table containing the translation columns
   * @return the joins responsible to bring translated names
   */
  public static String translationNamesJoinsOn(String table) {
    if (isNotBlank(table)) {
      return translationNamesJoinsOn(table, false);
    }

    return EMPTY;
  }

  /**
   * This method will join the translations column for the given table. Depending on the program
   * flag it will also join the program table.
   *
   * @param table the table containing the translation columns
   * @param includeProgram if true, it will also join the program table
   * @return the joins responsible to bring translated names
   */
  public static String translationNamesJoinsOn(String table, boolean includeProgram) {
    StringBuilder joins = new StringBuilder();

    if (isNotBlank(table)) {
      joins
          .append(
              " left join jsonb_to_recordset("
                  + table
                  + ".translations) as "
                  + table
                  + "_displayname(value TEXT, locale TEXT, property TEXT) on "
                  + table
                  + "_displayname.locale = :"
                  + LOCALE
                  + " and "
                  + table
                  + "_displayname.property = 'NAME'")
          .append(
              " left join jsonb_to_recordset("
                  + table
                  + ".translations) as "
                  + table
                  + "_displayshortname(value TEXT, locale TEXT, property TEXT) on "
                  + table
                  + "_displayshortname.locale = :"
                  + LOCALE
                  + " and "
                  + table
                  + "_displayshortname.property = 'SHORT_NAME'");

      if (includeProgram) {
        joins
            .append(
                " left join jsonb_to_recordset(program.translations) as p_displayname(value TEXT, locale TEXT, property TEXT) on p_displayname.locale = :"
                    + LOCALE
                    + " and p_displayname.property = 'NAME'")
            .append(
                " left join jsonb_to_recordset(program.translations) as p_displayshortname(value TEXT, locale TEXT, property TEXT) on p_displayshortname.locale = :"
                    + LOCALE
                    + " and p_displayshortname.property = 'SHORT_NAME'");
      }
    }

    return joins.toString();
  }

  /**
   * This method will join the translations column for the given table. Depending on the program
   * flag it will also join the program table.
   *
   * @param table the table containing the translation columns
   * @param includeProgram if true, it will also join the program table
   * @param includeOption if true, it will also join the option table
   * @return the joins responsible to bring translated names
   */
  public static String translationNamesJoinsOn(
      String table, boolean includeProgram, boolean includeOption) {
    if (isNotBlank(table)) {
      StringBuilder joins = new StringBuilder(translationNamesJoinsOn(table, includeProgram));

      if (includeOption) {
        joins.append(
            " left join jsonb_to_recordset(optionvalue.translations) as o_displayname(value TEXT, locale TEXT, property TEXT) on o_displayname.locale = :"
                + LOCALE
                + " and o_displayname.property = 'NAME'");
      }

      return joins.toString();
    }

    return EMPTY;
  }

  /**
   * This method defines the values for the translatable columns, for the given table.
   *
   * @param table the table containing the translation columns
   * @return the columns containing the translated names
   */
  public static String translationNamesColumnsFor(String table) {
    if (isNotBlank(table)) {
      return translationNamesColumnsFor(table, false);
    }

    return EMPTY;
  }

  /**
   * This method defines the values for the translatable columns, for the given table. Depending on
   * the program flag it will also bring translatable columns from the program table.
   *
   * @param table the table containing the translation columns
   * @param includeProgram if true, it will also bring program columns
   * @return the columns containing the translated names
   */
  public static String translationNamesColumnsFor(String table, boolean includeProgram) {
    return translationNamesColumnsFor(table, includeProgram, false, true);
  }

  /**
   * This method defines the values for the translatable columns, for the given table. Depending on
   * the program flag it will also bring translatable columns from the program table.
   *
   * @param table the table containing the translation columns
   * @param includeProgram if true, it will also bring program columns
   * @param hasShortName whether the table has a shortname column
   * @return the columns containing the translated names
   */
  public static String translationNamesColumnsFor(
      String table, boolean includeProgram, boolean includeOption, boolean hasShortName) {
    StringBuilder columns = new StringBuilder();

    if (isNotBlank(table)) {
      if (includeProgram) {
        columns
            .append(
                ", (case when p_displayname.value is not null then p_displayname.value else program.name end) as i18n_first_name")
            .append(
                ", (case when p_displayshortname.value is not null then p_displayshortname.value else program.shortname end) as i18n_first_shortname")
            .append(translationNamesColumnsForItem(table, "i18n_second", hasShortName));
      } else {
        columns
            .append(translationNamesColumnsForItem(table, "i18n_first", hasShortName))
            .append(", cast (null as text) as i18n_second_name")
            .append(", cast (null as text) as i18n_second_shortname");
      }

      if (includeOption) {
        columns
            .append(
                ", (case when o_displayname.value is not null then o_displayname.value else optionvalue.name end) as i18n_third_name")
            .append(", cast (null as text) as i18n_third_shortname");
      } else {
        columns
            .append(", cast (null as text) as i18n_third_name")
            .append(", cast (null as text) as i18n_third_shortname");
      }
    }

    return columns.toString();
  }

  private static String translationNamesColumnsForItem(
      String table, String i18nColumnPrefix, boolean hasShortName) {
    StringBuilder columns = new StringBuilder();

    columns
        .append(
            ", (case when "
                + table
                + "_displayname.value is not null then "
                + table
                + "_displayname.value else "
                + table
                + ".name end) as "
                + i18nColumnPrefix
                + "_name")
        .append(
            ", (case when "
                + table
                + "_displayshortname.value is not null then "
                + table
                + "_displayshortname.value else "
                + (hasShortName ? table + ".shortname" : "cast (null as text)")
                + " end) as "
                + i18nColumnPrefix
                + "_shortname");

    return columns.toString();
  }
}
