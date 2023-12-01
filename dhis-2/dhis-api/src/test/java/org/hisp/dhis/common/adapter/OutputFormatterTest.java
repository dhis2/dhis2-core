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
package org.hisp.dhis.common.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.common.adapter.OutputFormatter.maybeFormat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the OutputFormatter class.
 *
 * @author maikel arabori
 */
class OutputFormatterTest {

  @Test
  void testMaybeFormatWhenObjectIsDoubleUnder10Million() {
    // Given
    final Double under10Million = 10000d;
    // When
    final Object result = maybeFormat(under10Million);
    // Then
    assertInstanceOf(String.class, result);
    assertThat(result, is("10000.0"));
  }

  @Test
  void testMaybeFormatWhenObjectIsDoubleOver10Million() {
    // Given
    final Double over10Million = 10000001d;
    // When
    final Object result = maybeFormat(over10Million);
    // Then
    assertInstanceOf(String.class, result);
    assertThat(result, is("10000001.0"));
  }

  @Test
  void testMaybeFormatWhenObjectIsDoubleWithDecimalDigitsOver10Million() {
    // Given
    final Double over10Million = 10000000.2575899d;
    // When
    final Object result = maybeFormat(over10Million);
    // Then
    assertInstanceOf(String.class, result);
    assertThat(result, is("10000000.2575899"));
  }

  @Test
  void testMaybeFormatWhenObjectIsNull() {
    // Given
    final Object nullObject = null;
    // When
    final Object result = maybeFormat(nullObject);
    // Then
    assertThat(result, is(nullObject));
  }

  @Test
  void testMaybeFormatWhenObjectIsNotSupportedShort() {
    // Given
    final Short notSupportedObject = 44;
    // When
    final Object result = maybeFormat(notSupportedObject);
    // Then
    assertInstanceOf(Short.class, result);
    assertThat(result, is(notSupportedObject));
  }

  @Test
  void testMaybeFormatWhenObjectIsNotSupportedLong() {
    // Given
    final Long notSupportedObject = 4455555l;
    // When
    final Object result = maybeFormat(notSupportedObject);
    // Then
    assertInstanceOf(Long.class, result);
    assertThat(result, is(notSupportedObject));
  }
}
