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
package org.hisp.dhis.webapi.controller.event.webrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderCriteriaTest {

  @Test
  void fromOrderString() {

    List<OrderCriteria> orderCriteria =
        OrderCriteria.fromOrderString("one:desc,, two:asc  ,three  ");

    assertNotNull(orderCriteria);
    assertEquals(
        4, orderCriteria.size(), String.format("Expected 4 item, instead got %s", orderCriteria));
    assertEquals(OrderCriteria.of("one", SortDirection.DESC), orderCriteria.get(0));
    assertEquals(OrderCriteria.of("", SortDirection.ASC), orderCriteria.get(1));
    assertEquals(OrderCriteria.of(" two", SortDirection.ASC), orderCriteria.get(2));
    assertEquals(OrderCriteria.of("three", SortDirection.ASC), orderCriteria.get(3));
  }

  @ValueSource(strings = {"one:desc", "one:Desc", "one:DESC"})
  @ParameterizedTest
  void fromOrderStringSortDirectionParsingIsCaseInsensitive(String source) {

    List<OrderCriteria> orderCriteria = OrderCriteria.fromOrderString(source);

    assertNotNull(orderCriteria);
    assertEquals(
        1, orderCriteria.size(), String.format("Expected 1 item, instead got %s", orderCriteria));
    assertEquals(OrderCriteria.of("one", SortDirection.DESC), orderCriteria.get(0));
  }

  @Test
  void fromOrderStringDefaultsToAscGivenFieldAndColon() {

    List<OrderCriteria> orderCriteria = OrderCriteria.fromOrderString("one:");

    assertNotNull(orderCriteria);
    assertEquals(
        1, orderCriteria.size(), String.format("Expected 1 item, instead got %s", orderCriteria));
    assertEquals(OrderCriteria.of("one", SortDirection.ASC), orderCriteria.get(0));
  }

  @Test
  void fromOrderStringDefaultsSortDirectionToAscGivenAnUnknownSortDirection() {
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
  void fromOrderStringReturnsNullGivenOrderWithMoreThanTwoProperties() {

    List<OrderCriteria> orderCriteria = OrderCriteria.fromOrderString("one:desc:wrong");

    assertNotNull(orderCriteria);
    assertEquals(
        1, orderCriteria.size(), String.format("Expected 1 item, instead got %s", orderCriteria));
    assertNull(orderCriteria.get(0));
  }
}
