/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class DorisSqlBuilderTest {
  private final SqlBuilder sqlBuilder = new DorisSqlBuilder("pg_dhis");

  // Data types

  @Test
  void testDataType() {
    assertEquals("double", sqlBuilder.dataTypeDouble());
    assertEquals("datetime", sqlBuilder.dataTypeTimestamp());
  }

  // Capabilities

  @Test
  void testSupportsAnalyze() {
    assertFalse(sqlBuilder.supportsAnalyze());
  }

  @Test
  void testSupportsVacuum() {
    assertFalse(sqlBuilder.supportsVacuum());
  }

  @Test
  void testSingleQuote() {
    assertEquals("'jkhYg65ThbF'", sqlBuilder.singleQuote("jkhYg65ThbF"));
    assertEquals("'Age ''<5'' years'", sqlBuilder.singleQuote("Age '<5' years"));
    assertEquals("'Status \"not checked\"'", sqlBuilder.singleQuote("Status \"not checked\""));
  }

  @Test
  void testEscape() {
    assertEquals("Age group ''under 5'' years", sqlBuilder.escape("Age group 'under 5' years"));
    assertEquals("Level ''high'' found", sqlBuilder.escape("Level 'high' found"));
    assertEquals("C:\\\\Downloads\\\\File.doc", sqlBuilder.escape("C:\\Downloads\\File.doc"));
  }

  @Test
  void testSinqleQuotedCommaDelimited() {
    assertEquals(
        "'dmPbDBKwXyF', 'zMl4kciwJtz', 'q1Nqu1r1GTn'",
        sqlBuilder.singleQuotedCommaDelimited(
            List.of("dmPbDBKwXyF", "zMl4kciwJtz", "q1Nqu1r1GTn")));
    assertEquals("'1', '3', '5'", sqlBuilder.singleQuotedCommaDelimited(List.of("1", "3", "5")));
    assertEquals("", sqlBuilder.singleQuotedCommaDelimited(List.of()));
    assertEquals("", sqlBuilder.singleQuotedCommaDelimited(null));
  }

  // Utilities

  @Test
  void testQuote() {
    assertEquals(
        "`Treated \"malaria\" at facility`", sqlBuilder.quote("Treated \"malaria\" at facility"));
    assertEquals(
        "`Patients on ``treatment`` for TB`", sqlBuilder.quote("Patients on `treatment` for TB"));
    assertEquals("`quarterly`", sqlBuilder.quote("quarterly"));
    assertEquals("`Fully immunized`", sqlBuilder.quote("Fully immunized"));
  }

  @Test
  void testQuoteAlias() {
    assertEquals(
        "ax.`Treated \"malaria\" at facility`",
        sqlBuilder.quote("ax", "Treated \"malaria\" at facility"));
    assertEquals(
        "analytics.`Patients on ``treatment`` for TB`",
        sqlBuilder.quote("analytics", "Patients on `treatment` for TB"));
    assertEquals("analytics.`quarterly`", sqlBuilder.quote("analytics", "quarterly"));
    assertEquals("dv.`Fully immunized`", sqlBuilder.quote("dv", "Fully immunized"));
  }

  @Test
  void testQualifyTable() {
    assertEquals("pg_dhis.public.category", sqlBuilder.qualifyTable("category"));
    assertEquals(
        "pg_dhis.public.categories_categoryoptions",
        sqlBuilder.qualifyTable("categories_categoryoptions"));
  }
}
