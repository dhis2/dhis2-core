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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AnalyticsSqlUtils}.
 *
 * @author Luciano Fiandesio
 */
class AnalyticsSqlUtilsTest {
  @Test
  void testQuote() {
    assertEquals(
        "\"Some \"\"special\"\" value\"", AnalyticsSqlUtils.quote("Some \"special\" value"));
    assertEquals("\"Data element\"", AnalyticsSqlUtils.quote("Data element"));
  }

  @Test
  void testQuotedListOf() {
    assertEquals(
        List.of("\"a\"\"b\"\"c\"", "\"d\"\"e\"\"f\""),
        AnalyticsSqlUtils.quotedListOf("a\"b\"c", "d\"e\"f"));

    assertEquals(
        List.of("\"ab\"", "\"cd\"", "\"ef\""), AnalyticsSqlUtils.quotedListOf("ab", "cd", "ef"));
  }

  @Test
  void testQuoteWithAlias() {
    assertEquals("ougs.\"Short name\"", AnalyticsSqlUtils.quote("ougs", "Short name"));
    assertEquals("ous.\"uid\"", AnalyticsSqlUtils.quote("ous", "uid"));
  }

  @Test
  void testQuoteAliasCommaSeparate() {
    assertEquals(
        "ax.\"de\",ax.\"pe\",ax.\"ou\"",
        AnalyticsSqlUtils.quoteAliasCommaSeparate(List.of("de", "pe", "ou")));
    assertEquals(
        "ax.\"gender\",ax.\"date of \"\"birth\"\"\"",
        AnalyticsSqlUtils.quoteAliasCommaSeparate(List.of("gender", "date of \"birth\"")));
  }

  @Test
  void testQuoteWithFunction() {
    assertEquals(
        "min(\"value\") as \"value\",min(\"textvalue\") as \"textvalue\"",
        AnalyticsSqlUtils.quoteWithFunction("min", "value", "textvalue"));

    assertEquals(
        "max(\"daysxvalue\") as \"daysxvalue\",max(\"daysno\") as \"daysno\"",
        AnalyticsSqlUtils.quoteWithFunction("max", "daysxvalue", "daysno"));
  }

  @Test
  void testGetClosingParentheses() {
    assertEquals("", AnalyticsSqlUtils.getClosingParentheses(null));
    assertEquals("", AnalyticsSqlUtils.getClosingParentheses(""));
    assertEquals(")", AnalyticsSqlUtils.getClosingParentheses("from(select(select (*))"));
    assertEquals("))", AnalyticsSqlUtils.getClosingParentheses("(("));
  }

  @Test
  void testGetCoalesce_returns_defaultColumnName_when_coordinate_field_collection_is_empty() {
    // when
    String sqlSnippet =
        AnalyticsSqlUtils.getCoalesce(
            new ArrayList<>(), FallbackCoordinateFieldType.PSI_GEOMETRY.getValue());

    // then
    assertEquals(FallbackCoordinateFieldType.PSI_GEOMETRY.getValue(), sqlSnippet);
  }

  @Test
  void testGetCoalesceReturnsDefaultColumnNameWhenCoordinateFieldCollectionIsNull() {
    // when
    String sqlSnippet =
        AnalyticsSqlUtils.getCoalesce(null, FallbackCoordinateFieldType.PSI_GEOMETRY.getValue());

    // then
    assertEquals(FallbackCoordinateFieldType.PSI_GEOMETRY.getValue(), sqlSnippet);
  }

  @Test
  void testGetCoalesceReturnsCoalesceWhenCoordinateFieldCollectionIsNotEmpty() {
    // when
    String sqlSnippet =
        AnalyticsSqlUtils.getCoalesce(
            List.of("coorA", "coorB", "coorC"),
            FallbackCoordinateFieldType.PSI_GEOMETRY.getValue());

    // then
    assertEquals("coalesce(ax.\"coorA\",ax.\"coorB\",ax.\"coorC\")", sqlSnippet);
  }

  @Test
  void testGetCollate() {
    assertEquals(" collate \"Posix\" ", AnalyticsSqlUtils.getCollate("Posix"));
    assertEquals("", AnalyticsSqlUtils.getCollate(null));
    assertEquals("", AnalyticsSqlUtils.getCollate(""));
    assertEquals("", AnalyticsSqlUtils.getCollate(" "));
  }
}
