/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.expressiondimensionitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;

/** Test for {@link ExpressionDimensionItemService} */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ExpressionDimensionItemServiceTests {
  @Mock private IdentifiableObjectManager manager;

  @Mock private IdentifiableObject object;

  private ExpressionDimensionItemService sut;

  @BeforeEach
  void initialize() {
    sut = new ExpressionDimensionItemService(manager);
  }

  @Test
  void testGetExpressionItemsReturnsEmptyCollectionWhenCalledWithNullExpressionDimensionItem() {
    // Given
    // When
    // Then
    assertEquals(0, sut.getExpressionItems(new DataDimensionItem()).size(), "NPE assertion failed");
  }

  @ParameterizedTest
  @CsvSource({
    "'fbfJHSPpUQD.pq2XI5kz2BY', 'fbfJHSPpUQD.PT59n8BQbqM'",
    "'pq2XI5kz2BY', 'fbfJHSPpUQD.PT59n8BQbqM'",
    "'pq2XI5kz2BY', 'PT59n8BQbqM'"
  })
  void testGetExpressionTokensReturnsCollectionOfTokens(String token1, String token2) {
    // Given
    // When
    List<String> tokens =
        sut.getExpressionTokens(
            ExpressionDimensionItemService.pattern, "#{" + token1 + "/#{" + token2 + "}");

    // Then
    assertEquals(2, tokens.size());
    assertEquals(token1, tokens.get(0));
    assertEquals(token2, tokens.get(1));
  }

  @ParameterizedTest
  @CsvSource({
    "'fbfJHSPpUQD.pq2XI5kz2BY', 'fbfJHSPpUQD.PT59n8BQbqM'",
    "'pq2XI5kz2BY', 'fbfJHSPpUQD.PT59n8BQbqM'",
    "'pq2XI5kz2BY', 'PT59n8BQbqM'"
  })
  void testValidateExpressionItemsReturnsTrue(String token1, String token2) {
    // Given
    //noinspection unchecked
    when(manager.get((Class<IdentifiableObject>) any(), anyString())).thenReturn(object);

    // When
    boolean valid = sut.isValidExpressionItems("#{" + token1 + "/#{" + token2 + "}");

    // Then
    assertTrue(valid);
  }

  @ParameterizedTest
  @CsvSource({
    "'fbfJHSPpUQD.pq2XI5kz2BY.fbfJHSPpUQD.PT59n8BQbqM', 'fbfJHSPpUQD.pq2XI5kz2BY'",
    "'fbfJHSPpUQD.pq2XI5kz2BY', ''"
  })
  void testValidateExpressionItemsReturnsFalse(String token1, String token2) {
    // Given
    // When
    boolean valid = sut.isValidExpressionItems("#{" + token1 + "/#{" + token2 + "}");

    // Then
    assertFalse(valid);
  }

  @Test
  void testValidateNumericExpressionItemsReturnsTrue() {
    // Given
    // When
    boolean valid = sut.isValidExpressionItems("123456789123456789");

    // Then
    assertTrue(valid);
  }
}
