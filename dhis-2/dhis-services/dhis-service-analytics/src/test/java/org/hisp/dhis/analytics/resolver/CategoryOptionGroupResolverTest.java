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
package org.hisp.dhis.analytics.resolver;

import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.test.TestBase.createCategoryOptionCombo;
import static org.hisp.dhis.test.TestBase.createCategoryOptionGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryOptionGroupStore;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 * @author Dusan Bernat
 */
@ExtendWith(MockitoExtension.class)
class CategoryOptionGroupResolverTest {
  @Mock private CategoryOptionGroupStore categoryOptionGroupStore;

  @Mock private CategoryOptionComboStore categoryOptionComboStore;

  @Mock private ExpressionService expressionService;

  @InjectMocks private CategoryOptionGroupResolver resolver;

  private String uid1;

  private String uid2;

  private String uid3;

  private CategoryOptionCombo coc1;

  private CategoryOptionCombo coc2;

  private CategoryOptionCombo coc3;

  private DimensionalItemId dimensionalItemId;

  private final String CATEGORY_OPTION_GROUP_PREFIX = "coGroup:";

  @BeforeEach
  public void setUp() {
    uid1 = CodeGenerator.generateUid();
    uid2 = CodeGenerator.generateUid();
    uid3 = CodeGenerator.generateUid();

    coc1 = createCategoryOptionCombo('X');
    coc2 = createCategoryOptionCombo('Y');
    coc3 = createCategoryOptionCombo('Z');
  }

  @Test
  void verifyExpressionIsResolvedProperly() {
    // arrange

    dimensionalItemId =
        new DimensionalItemId(
            DimensionItemType.DATA_ELEMENT_OPERAND,
            uid1,
            CATEGORY_OPTION_GROUP_PREFIX + uid2,
            uid3,
            createIndicatorExpression());

    String expression = createIndicatorExpression();

    when(expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION))
        .thenReturn(Sets.newHashSet(dimensionalItemId));
    when(categoryOptionGroupStore.loadByUid(anyString()))
        .thenReturn(createCategoryOptionGroup('A'));
    when(categoryOptionComboStore.getCategoryOptionCombosByGroupUid(anyString(), anyString()))
        .thenReturn(List.of(coc1, coc2, coc3));

    // act

    String resolvedExpression = resolver.resolve(expression);

    // assert

    assertResolvedExpressionEquals(resolvedExpression, coc1.getUid(), coc2.getUid(), coc3.getUid());
  }

  @Test
  void verifyExpressionIsNotResolvedWhenDimensionalItemIdHasNoItem() {
    // arrange

    dimensionalItemId =
        new DimensionalItemId(
            DimensionItemType.DATA_ELEMENT_OPERAND,
            uid1,
            CATEGORY_OPTION_GROUP_PREFIX + uid2,
            uid3);

    String expression = createIndicatorExpression();

    when(expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION))
        .thenReturn(Sets.newHashSet(dimensionalItemId));

    // act

    String resolvedExpression = resolver.resolve(expression);

    // assert

    assertEquals(expression, resolvedExpression);
  }

  @Test
  void verifyExpressionIsNotResolvedWhenCoPrefixNotInUid1() {
    // arrange

    dimensionalItemId =
        new DimensionalItemId(
            DimensionItemType.DATA_ELEMENT_OPERAND, uid1, uid2, uid3, createIndicatorExpression());

    String expression = createIndicatorExpression();

    when(expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION))
        .thenReturn(Sets.newHashSet(dimensionalItemId));

    // act

    String resolvedExpression = resolver.resolve(expression);

    // assert

    assertEquals(expression, resolvedExpression);
  }

  @Test
  void verifyExpressionIsNotResolvedWhenExpressionIsNotValid() {
    // arrange

    dimensionalItemId =
        new DimensionalItemId(
            DimensionItemType.DATA_ELEMENT_OPERAND, uid1, uid2, uid3, createIndicatorExpression());

    String expression = "lsdjflakjdflkajdslfhaglakujdhfg";

    when(expressionService.getExpressionDimensionalItemIds(expression, INDICATOR_EXPRESSION))
        .thenReturn(Sets.newHashSet(dimensionalItemId));

    // act

    String resolvedExpression = resolver.resolve(expression);

    // assert

    assertEquals(expression, resolvedExpression);
  }

  private String createIndicatorExpression() {
    return String.format("#{%s.coGroup:%s.%s}", uid1, uid2, uid3);
  }

  private void assertResolvedExpressionEquals(
      String result, String cocUid1, String cocUid2, String cocUid3) {
    assertTrue(
        result.equals(formatExpression(cocUid1, cocUid2, cocUid3))
            || result.equals(formatExpression(cocUid1, cocUid3, cocUid2))
            || result.equals(formatExpression(cocUid2, cocUid3, cocUid1))
            || result.equals(formatExpression(cocUid2, cocUid1, cocUid3))
            || result.equals(formatExpression(cocUid3, cocUid1, cocUid2))
            || result.equals(formatExpression(cocUid3, cocUid2, cocUid1)));
  }

  private String formatExpression(String coc1_uid, String coc2_uid, String coc3_uid) {
    return String.format(
        "(#{%s.%s.%s}+#{%s.%s.%s}+#{%s.%s.%s})",
        uid1, coc1_uid, uid3, uid1, coc2_uid, uid3, uid1, coc3_uid, uid3);
  }
}
