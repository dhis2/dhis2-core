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
package org.hisp.dhis.dataitem.query.shared;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesColumnsFor;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesJoinsOn;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for NameTranslationStatement.
 *
 * @author maikel arabori
 */
class NameTranslationStatementTest {

  @Test
  void testTranslationNamesJoinsOn() {
    // Given
    final String table = "indicator";
    // When
    final String actualStatement = translationNamesJoinsOn(table);
    // Then
    assertThat(
        actualStatement,
        containsString("left join jsonb_to_recordset(indicator.translations) as indicator"));
    assertThat(
        actualStatement,
        containsString(
            "indicator_displayname(value TEXT, locale TEXT, property TEXT) on indicator"));
    assertThat(actualStatement, not(containsString("program")));
  }

  @Test
  void testTranslationNamesJoinsOnWhenTableIsNull() {
    // Given
    final String nullTable = null;
    // When
    final String actualStatement = translationNamesJoinsOn(nullTable);
    // Then
    assertThat(actualStatement, containsString(EMPTY));
  }

  @Test
  void testTranslationNamesJoinsOnWhenTableIsBlank() {
    // Given
    final String blankTable = " ";
    // When
    final String actualStatement = translationNamesJoinsOn(blankTable);
    // Then
    assertThat(actualStatement, containsString(EMPTY));
  }

  @Test
  void testTranslationNamesJoinsOnWithProgram() {
    // Given
    final String table = "indicator";
    // When
    final String actualStatement = translationNamesJoinsOn(table, true);
    // Then
    assertThat(
        actualStatement,
        containsString("left join jsonb_to_recordset(indicator.translations) as indicator"));
    assertThat(
        actualStatement,
        containsString(
            "indicator_displayname(value TEXT, locale TEXT, property TEXT) on indicator"));
    assertThat(
        actualStatement,
        containsString(
            "left join jsonb_to_recordset(program.translations) as p_displayname(value TEXT, locale TEXT, property TEXT)"));
    assertThat(
        actualStatement,
        containsString(
            "left join jsonb_to_recordset(program.translations) as p_displayshortname(value TEXT, locale TEXT, property TEXT)"));
  }

  @Test
  void testTranslationNamesColumnsForWhenTableIsNull() {
    // When
    final String actualStatement = translationNamesJoinsOn(null);
    // Then
    assertThat(actualStatement, containsString(EMPTY));
  }

  @Test
  void testTranslationNamesColumnsForWhenTableIsEmpty() {
    // Given
    final String emptyTable = null;
    // When
    final String actualStatement = translationNamesJoinsOn(emptyTable);
    // Then
    assertThat(actualStatement, containsString(EMPTY));
  }

  @Test
  void testTranslationNamesColumnsFor() {
    // Given
    final String table = "indicator";
    // When
    final String actualStatement = translationNamesColumnsFor(table);
    // Then
    assertThat(
        actualStatement,
        containsString(
            "(case when indicator_displayname.value is not null then indicator_displayname.value"));
    assertThat(
        actualStatement,
        containsString(
            "(case when indicator_displayshortname.value is not null then indicator_displayshortname.value"));
    assertThat(actualStatement, not(containsString("program")));
  }

  @Test
  void testTranslationNamesColumnsForWithProgram() {
    // Given
    final String table = "indicator";
    // When
    final String actualStatement = translationNamesColumnsFor(table, true);
    // Then
    assertThat(
        actualStatement,
        containsString(
            "(case when indicator_displayname.value is not null then indicator_displayname.value"));
    assertThat(
        actualStatement,
        containsString(
            "(case when indicator_displayshortname.value is not null then indicator_displayshortname.value"));
    assertThat(
        actualStatement,
        containsString(
            "(case when p_displayname.value is not null then p_displayname.value else program.name end) as i18n_first_name"));
    assertThat(
        actualStatement,
        containsString(
            "(case when p_displayshortname.value is not null then p_displayshortname.value else program.shortname end) as i18n_first_shortname"));
  }

  @Test
  void testTranslationNamesColumnsForWithProgramWhenTableIsNull() {
    // Given
    final String nullTable = null;
    // When
    final String actualStatement = translationNamesColumnsFor(nullTable, true);
    // Then
    assertThat(actualStatement, containsString(EMPTY));
  }

  @Test
  void testTranslationNamesColumnsForWithProgramWhenTableIsEmpty() {
    // Given
    final String emptyTable = null;
    // When
    final String actualStatement = translationNamesColumnsFor(emptyTable, true);
    // Then
    assertThat(actualStatement, containsString(EMPTY));
  }
}
