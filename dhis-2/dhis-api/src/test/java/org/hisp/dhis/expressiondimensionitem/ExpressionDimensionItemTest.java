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
package org.hisp.dhis.expressiondimensionitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ExpressionDimensionItem}. */
class ExpressionDimensionItemTest {
  @Test
  void testToIndicator() {
    // Given
    ExpressionDimensionItem expressionDimensionItem = new ExpressionDimensionItem();
    expressionDimensionItem.setExpression(
        "#{R4KStuS8qt7.LbkJRbDblhe} / #{o0fOD1HLuv8.LbkJRbDblhe}");
    expressionDimensionItem.setUid("anyUid");
    expressionDimensionItem.setCode("anyCode");
    expressionDimensionItem.setName("anyName");
    expressionDimensionItem.setDescription("anyDescription");

    // When
    Indicator indicator = expressionDimensionItem.toIndicator();

    // Then
    assertEquals("anyUid", indicator.getUid());
    assertEquals("anyCode", indicator.getCode());
    assertEquals("anyName", indicator.getName());
    assertEquals(
        "#{R4KStuS8qt7.LbkJRbDblhe} / #{o0fOD1HLuv8.LbkJRbDblhe}", indicator.getNumerator());
    assertEquals("1", indicator.getDenominator());
    assertEquals(1, indicator.getIndicatorType().getFactor());
    assertNull(indicator.getDescription());
    assertNull(indicator.getDecimals());
    assertFalse(indicator.isAnnualized());
    assertTrue(indicator.getIndicatorType().isNumber());
  }

  @Test
  void testEqualItemsHaveEqualHashCodes() {
    ExpressionDimensionItem a = item("Short", "Description");
    ExpressionDimensionItem b = item("Short", "Description");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testItemsWithDifferentShortNamesAreNotEqual() {
    assertNotEquals(item("Short", "Description"), item("Other", "Description"));
  }

  @Test
  void testItemsWithDifferentDescriptionsAreNotEqual() {
    assertNotEquals(item("Short", "Description"), item("Short", "Other"));
  }

  @Test
  void testItemsWithDifferentQueryModsAreNotEqual() {
    ExpressionDimensionItem a = item("Short", "Description");
    ExpressionDimensionItem b = item("Short", "Description");
    b.setQueryMods(QueryModifiers.builder().periodOffset(1).build());

    assertNotEquals(a, b);
  }

  @Test
  void testDisplayFormNameFallsBackToNameWhenFormNameIsEmpty() {
    ExpressionDimensionItem item = item("Short", "Description");
    assertEquals("anyName", item.getDisplayFormName());

    item.setFormName("Form name");
    assertEquals("Form name", item.getDisplayFormName());
  }

  @Test
  void testSetTranslationsToNullKeepsAnEmptySet() {
    ExpressionDimensionItem item = item("Short", "Description");
    item.setTranslations(null);

    assertTrue(item.getTranslations().isEmpty());
  }

  @Test
  void testAggregationTypeComesFromQueryModsOnly() {
    ExpressionDimensionItem item = item("Short", "Description");
    item.setAggregationType(AggregationType.SUM);
    assertNull(item.getAggregationType());
    assertFalse(item.hasAggregationType());

    item.setQueryMods(QueryModifiers.builder().aggregationType(AggregationType.AVERAGE).build());
    assertEquals(AggregationType.AVERAGE, item.getAggregationType());
    assertTrue(item.hasAggregationType());
  }

  @Test
  void testLegendSetsAreNotHeldAndReturnAFreshMutableList() {
    ExpressionDimensionItem item = item("Short", "Description");
    item.setLegendSets(List.of(new LegendSet()));

    List<LegendSet> legendSets = item.getLegendSets();
    assertTrue(legendSets.isEmpty());
    legendSets.add(new LegendSet());
    assertNotSame(legendSets, item.getLegendSets());
    assertTrue(item.getLegendSets().isEmpty());
    assertNull(item.getLegendSet());
    assertFalse(item.hasLegendSet());
  }

  @Test
  void testAggregateExportCombosAreNotHeld() {
    ExpressionDimensionItem item = item("Short", "Description");
    item.setAggregateExportCategoryOptionCombo("CocUid00001");
    item.setAggregateExportAttributeOptionCombo("AocUid00001");

    assertNull(item.getAggregateExportCategoryOptionCombo());
    assertNull(item.getAggregateExportAttributeOptionCombo());
    assertFalse(item.hasAggregateExportCategoryOptionCombo());
    assertFalse(item.hasAggregateExportAttributeOptionCombo());
  }

  @Test
  void testSetUserSetsCreatorAndSharingOwner() {
    User user = new User();
    user.setUid("UserUid0001");
    ExpressionDimensionItem item = item("Short", "Description");
    item.setUser(user);

    assertEquals(user, item.getCreatedBy());
    assertEquals("UserUid0001", item.getSharing().getOwner());
  }

  private static ExpressionDimensionItem item(String shortName, String description) {
    ExpressionDimensionItem item = new ExpressionDimensionItem("#{fbfJHSPpUQD}", description);
    item.setUid("anyUid");
    item.setCode("anyCode");
    item.setName("anyName");
    item.setShortName(shortName);
    return item;
  }
}
