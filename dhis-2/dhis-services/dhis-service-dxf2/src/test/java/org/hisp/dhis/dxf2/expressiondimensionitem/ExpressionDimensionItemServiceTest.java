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
package org.hisp.dhis.dxf2.expressiondimensionitem;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

/** Test for {@link ExpressionDimensionItemService}. */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ExpressionDimensionItemServiceTest {
  @Mock private IdentifiableObjectManager manager;

  @Mock private ExpressionService expressionService;

  private ExpressionDimensionItemService itemService;

  @BeforeEach
  void initialize() {
    itemService = new ExpressionDimensionItemService(manager, expressionService);
  }

  @Test
  void testGetExpressionItemsReturnsEmptyCollectionWhenCalledWithNullExpressionDimensionItem() {
    // Given
    DataDimensionItem dataDimensionItem = new DataDimensionItem();

    // When
    List<DimensionalItemObject> dimensionalItemObjects =
        itemService.getExpressionItems(dataDimensionItem);

    // Then
    assertEquals(0, dimensionalItemObjects.size(), "NPE assertion failed");
  }

  @Test
  void testGetExpressionItemsWithDecimalDigits() {
    // Given
    String expression = "1.25 + 2.44444444444444444444444444444344";
    ExpressionDimensionItem expressionDimensionItem =
        new ExpressionDimensionItem(expression, EMPTY);
    DataDimensionItem dataDimensionItem = new DataDimensionItem();

    dataDimensionItem.setExpressionDimensionItem(expressionDimensionItem);

    // When
    List<DimensionalItemObject> dimensionalItemObjects =
        itemService.getExpressionItems(dataDimensionItem);

    // Then
    assertEquals(0, dimensionalItemObjects.size());
  }

  @Test
  void testGetExpressionItemsWithIntegers() {
    // Given
    String expression = "10 + 24344";
    ExpressionDimensionItem expressionDimensionItem =
        new ExpressionDimensionItem(expression, EMPTY);
    DataDimensionItem dataDimensionItem = new DataDimensionItem();

    dataDimensionItem.setExpressionDimensionItem(expressionDimensionItem);

    // When
    List<DimensionalItemObject> dimensionalItemObjects =
        itemService.getExpressionItems(dataDimensionItem);

    // Then
    assertEquals(0, dimensionalItemObjects.size());
  }

  @Test
  void testGetExpressionItemsWithMixedValues() {
    // Given
    String expression = "10 + 24344.565656 / #{fbfJHSPpUQD.PT59n8BQbqM} - #{pq2XI5kz2BY}";
    ExpressionDimensionItem expressionDimensionItem =
        new ExpressionDimensionItem(expression, EMPTY);
    DataDimensionItem dataDimensionItem = new DataDimensionItem();

    dataDimensionItem.setExpressionDimensionItem(expressionDimensionItem);

    // When
    List<DimensionalItemObject> dimensionalItemObjects =
        itemService.getExpressionItems(dataDimensionItem);

    // Then
    assertEquals(2, dimensionalItemObjects.size());
  }

  @Test
  void testValidateWhenExpressionIsNull() {
    assertFalse(itemService.isValidExpressionItems(null));
  }

  @Test
  void testValidateWhenExpressionIsNullString() {
    assertFalse(itemService.isValidExpressionItems("null"));
  }

  @Test
  void testValidateWhenExpressionIsNullUppercaseString() {
    assertFalse(itemService.isValidExpressionItems("NULL"));
  }

  @Test
  void testValidateWhenExpressionIsBlank() {
    assertFalse(itemService.isValidExpressionItems(" "));
  }

  @Test
  void testValidateWhenExpressionIsEmpty() {
    assertFalse(itemService.isValidExpressionItems(""));
  }
}
