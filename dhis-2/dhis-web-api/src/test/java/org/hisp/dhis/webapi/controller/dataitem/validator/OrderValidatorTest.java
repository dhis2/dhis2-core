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
package org.hisp.dhis.webapi.controller.dataitem.validator;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.dataitem.validator.OrderValidator.checkOrderParams;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OrderValidator.
 *
 * @author maikel arabori
 */
class OrderValidatorTest {

  @Test
  void testCheckOrderParamsWhenOrderAttributeIsNotSupported() {
    // Given
    final Set<String> orderings = new HashSet<>(singletonList("notSupportedAttribute:asc"));
    // When throws
    final IllegalQueryException thrown =
        assertThrows(IllegalQueryException.class, () -> checkOrderParams(orderings));
    // Then
    assertThat(thrown.getMessage(), containsString("Order not supported: `notSupportedAttribute`"));
  }

  @Test
  void testCheckOrderParamsWhenOrderingValueIsInvalid() {
    // Given
    final Set<String> orderings = new HashSet<>(singletonList("name:invalidOrdering"));
    // When throws
    final IllegalQueryException thrown =
        assertThrows(IllegalQueryException.class, () -> checkOrderParams(orderings));
    // Then
    assertThat(thrown.getMessage(), containsString("Order not supported: `invalidOrdering`"));
  }

  @Test
  void testCheckOrderParamsWhenOrderingFormIsInvalid() {
    // Given
    final Set<String> orderings = new HashSet<>(singletonList("name:asc:invalid"));
    // When throws
    final IllegalQueryException thrown =
        assertThrows(IllegalQueryException.class, () -> checkOrderParams(orderings));
    // Then
    assertThat(
        thrown.getMessage(), containsString("Unable to parse order param: `name:asc:invalid`"));
  }

  @Test
  void testCheckOrderParamsWithSuccess() {
    // Given
    final Set<String> orderings = new HashSet<>(singletonList("name:desc"));
    // When
    checkOrderParams(orderings);
  }
}
