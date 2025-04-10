/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.subexpression;

import static org.hisp.dhis.analytics.AggregationType.AVERAGE;
import static org.hisp.dhis.analytics.AggregationType.COUNT;
import static org.hisp.dhis.analytics.AggregationType.MAX;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.common.DimensionItemType.SUBEXPRESSION_DIMENSION_ITEM;
import static org.hisp.dhis.subexpression.SubexpressionDimensionItem.getItemColumnName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SubexpressionDimensionItem}.
 *
 * @author Jim Grace
 */
class SubexpressionDimenstionItemTest {
  @Test
  void testConstructor() {
    String subexSql = "dummy SQL";
    Set<DimensionalItemObject> items = Set.of(new DataElement("DE Name"));
    QueryModifiers queryMods = QueryModifiers.builder().aggregationType(AVERAGE).build();

    SubexpressionDimensionItem target = new SubexpressionDimensionItem(subexSql, items, queryMods);

    assertEquals(subexSql, target.getSubexSql());
    assertEquals(items, target.getItems());
    assertEquals(queryMods, target.getQueryMods());
    assertNotNull(target.getUid());
    assertEquals(11, target.getUid().length());
    assertEquals(AVERAGE, target.getAggregationType());
    assertEquals(SUBEXPRESSION_DIMENSION_ITEM, target.getDimensionItemType());
  }

  @Test
  void testGetItemColumnName() {
    // Test for coc and aoc = null when missing
    assertEquals("\"de\"", getItemColumnName("de", null, null, null));
    assertEquals("\"de_co\"", getItemColumnName("de", "co", null, null));
    assertEquals("\"de_co_ao\"", getItemColumnName("de", "co", "ao", null));
    assertEquals("\"de__ao\"", getItemColumnName("de", null, "ao", null));
  }

  @Test
  void testGetItemColumnNameWithAggregationType() {
    QueryModifiers mods = QueryModifiers.builder().aggregationType(MAX).build();

    assertEquals("\"de_agg_MAX\"", getItemColumnName("de", "", "", mods));
    assertEquals("\"de_co_agg_MAX\"", getItemColumnName("de", "co", "", mods));
    assertEquals("\"de_co_ao_agg_MAX\"", getItemColumnName("de", "co", "ao", mods));
    assertEquals("\"de__ao_agg_MAX\"", getItemColumnName("de", "", "ao", mods));
  }

  @Test
  void testGetItemColumnNameWithPeriodOffset() {
    QueryModifiers mods = QueryModifiers.builder().periodOffset(-2).build();

    assertEquals("\"de_minus_2\"", getItemColumnName("de", "", "", mods));
    assertEquals("\"de_co_minus_2\"", getItemColumnName("de", "co", "", mods));
    assertEquals("\"de_co_ao_minus_2\"", getItemColumnName("de", "co", "ao", mods));
    assertEquals("\"de__ao_minus_2\"", getItemColumnName("de", "", "ao", mods));

    mods = QueryModifiers.builder().periodOffset(3).build();

    assertEquals("\"de_plus_3\"", getItemColumnName("de", "", "", mods));
    assertEquals("\"de_co_plus_3\"", getItemColumnName("de", "co", "", mods));
    assertEquals("\"de_co_ao_plus_3\"", getItemColumnName("de", "co", "ao", mods));
    assertEquals("\"de__ao_plus_3\"", getItemColumnName("de", "", "ao", mods));
  }

  @Test
  void testGetItemColumnNameWithPeriodOffsetAndAggregationType() {
    QueryModifiers mods = QueryModifiers.builder().periodOffset(-1).aggregationType(SUM).build();

    assertEquals("\"de_minus_1_agg_SUM\"", getItemColumnName("de", "", "", mods));
    assertEquals("\"de_co_minus_1_agg_SUM\"", getItemColumnName("de", "co", "", mods));
    assertEquals("\"de_co_ao_minus_1_agg_SUM\"", getItemColumnName("de", "co", "ao", mods));
    assertEquals("\"de__ao_minus_1_agg_SUM\"", getItemColumnName("de", "", "ao", mods));

    mods = QueryModifiers.builder().periodOffset(1).aggregationType(COUNT).build();

    assertEquals("\"de_plus_1_agg_COUNT\"", getItemColumnName("de", "", "", mods));
    assertEquals("\"de_co_plus_1_agg_COUNT\"", getItemColumnName("de", "co", "", mods));
    assertEquals("\"de_co_ao_plus_1_agg_COUNT\"", getItemColumnName("de", "co", "ao", mods));
    assertEquals("\"de__ao_plus_1_agg_COUNT\"", getItemColumnName("de", "", "ao", mods));
  }
}
