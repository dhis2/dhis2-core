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
package org.hisp.dhis.webapi.controller.event.webrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.SortDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderCriteriaTest {

  @Test
  void fromOrderString() {
    List<OrderCriteria> order = OrderCriteria.fromOrderString("one:desc,, two:asc  ,three  ");

    assertEquals(
        List.of(
            OrderCriteria.of("one", SortDirection.DESC),
            OrderCriteria.of("two", SortDirection.ASC),
            OrderCriteria.of("three", SortDirection.ASC)),
        order);
  }

  @ValueSource(strings = {"one:desc", "one:Desc", "one:DESC"})
  @ParameterizedTest
  void fromOrderStringSortDirectionParsingIsCaseInsensitive(String source) {
    List<OrderCriteria> order = OrderCriteria.fromOrderString(source);

    assertEquals(List.of(OrderCriteria.of("one", SortDirection.DESC)), order);
  }

  @Test
  void fromOrderStringDefaultsToAscGivenFieldAndColon() {
    List<OrderCriteria> order = OrderCriteria.fromOrderString("one:");

    assertEquals(List.of(OrderCriteria.of("one", SortDirection.ASC)), order);
  }

  @Test
  void failGivenAnUnknownSortDirection() {
    assertThrows(IllegalArgumentException.class, () -> OrderCriteria.fromOrderString("one:wrong"));
  }

  @Test
  void fromOrderStringReturnsEmptyListGivenEmptyOrder() {
    List<OrderCriteria> orderCriteria = OrderCriteria.fromOrderString(" ");

    assertNotNull(orderCriteria);
    assertEquals(
        0, orderCriteria.size(), String.format("Expected 0 items, instead got %s", orderCriteria));
  }

  @Test
  void failGivenMoreThanTwoColons() {
    assertThrows(
        IllegalArgumentException.class, () -> OrderCriteria.fromOrderString("one:desc:wrong"));
  }

  @Test
  void failGivenEmptyField() {
    assertThrows(IllegalArgumentException.class, () -> OrderCriteria.valueOf(" :desc"));
  }
}
