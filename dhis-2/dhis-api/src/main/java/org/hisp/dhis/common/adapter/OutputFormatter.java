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

import java.math.BigDecimal;

/**
 * Simple component responsible do enforce specific output/format to types where this is required.
 *
 * <p>Its goal is to provide methods that can improve the String representation of particular types.
 * Should be used specially for serialization purposes and in very specific cases.
 *
 * @author maikel arabori
 */
public class OutputFormatter {
  private static final int TEN_MILLION = 10000000;

  private OutputFormatter() {}

  /**
   * This method should support different formatters and apply them based on the input object type.
   *
   * <p>Currently only Double type is supported.
   *
   * @param object represents the object to be formatted.
   * @return a String containing the full value of the number object. If the type is not supported
   *     or is null it will return the given parameter object itself
   */
  public static Object maybeFormat(final Object object) {
    if (object instanceof Double) {
      return formatDouble((Double) object);
    }

    return object;
  }

  /**
   * This method will simply format a Double object to a non-scientific notation. It will happen
   * only if the given argument is a Double.
   *
   * <p>By default, depending on the type, Java will convert number types, that are equals or
   * greater than 10 million, to scientific notation when transformed to String.
   *
   * <p>For example, the Double 99999999 becomes 9.9999999E7 (scientific) when printed.
   *
   * <p>So, this method aims to avoid such representations and instead returns the full value as
   * String (in this example it would be "99999999.0").
   *
   * @param doubleValue represents the object that should be a Double
   * @return a String containing the full value of the number object. If the type is not supported
   *     or is null it will return the given parameter object itself
   * @throws NullPointerException if doubleValue is null
   */
  private static Object formatDouble(final Double doubleValue) {
    // Don't waste resources if the value is smaller than 10
    // million as the default representation will be the expected one.
    if (doubleValue >= TEN_MILLION) {
      // Needs to pass a String to the constructor, otherwise precision
      // is lost.
      final String numericValue = new BigDecimal((doubleValue).toString()).toPlainString();

      // Because toPlainString() does not print an extra ".0"
      // when the decimal digit is zero or absent.
      return handleDecimalDigit(handleDecimalDigit(numericValue));
    }

    return doubleValue.toString();
  }

  /**
   * This method appends a ".0" at the end of the value when it has not decimal digit. We use this
   * method to keep backward compatibility with the current behaviour.
   *
   * @param numericValue
   * @return the given numericValue + ".0"
   */
  private static String handleDecimalDigit(final String numericValue) {
    final boolean hasDecimalDigit = numericValue.contains(".");

    if (!hasDecimalDigit) {
      return numericValue + ".0";
    }

    return numericValue;
  }
}
