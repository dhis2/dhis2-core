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
package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;
import org.hisp.dhis.common.ValueType;

/**
 * @author Stian Sandvold
 */
public class TextPatternValidationUtils {
  public static boolean validateSegmentValue(TextPatternSegment segment, String value) {
    return segment.getMethod().getType().validateText(segment.getParameter(), value);
  }

  public static boolean validateTextPatternValue(TextPattern textPattern, String value) {
    StringBuilder builder = new StringBuilder();

    builder.append("^");

    textPattern
        .getSegments()
        .forEach(
            (segment) ->
                builder.append(
                    segment.getMethod().getType().getValueRegex(segment.getParameter())));

    builder.append("$");

    return Pattern.compile(builder.toString()).matcher(value).matches();
  }

  public static long getTotalValuesPotential(TextPatternSegment generatedSegment) {
    long res = 1;
    if (generatedSegment != null) {
      if (TextPatternMethod.SEQUENTIAL.equals(generatedSegment.getMethod())) {
        // Subtract by 1 since we don't use all zeroes.
        res = (long) (Math.pow(10, generatedSegment.getParameter().length()) - 1);
      } else if (TextPatternMethod.RANDOM.equals(generatedSegment.getMethod())) {
        for (char c : generatedSegment.getParameter().toCharArray()) {
          switch (c) {
            case '*':
              res = res * 62;
              break;
            case '#':
              res = res * 10;
              break;
            case 'X':
              res = res * 26;
              break;
            case 'x':
              res = res * 26;
              break;
            default:
              break;
          }
        }
      }
    }

    if (res < 0) {
      res = Long.MAX_VALUE;
    }

    return res;
  }

  public static boolean validateValueType(TextPattern textPattern, ValueType valueType) {
    if (ValueType.TEXT.equals(valueType)) {
      return true;
    } else if (ValueType.NUMBER.equals(valueType)) {
      boolean isAllNumbers = true;

      for (TextPatternSegment segment : textPattern.getSegments()) {
        isAllNumbers = isAllNumbers && isNumericOnly(segment);
      }

      return isAllNumbers;
    } else {
      return false;
    }
  }

  private static boolean isNumericOnly(TextPatternSegment segment) {
    if (TextPatternMethod.SEQUENTIAL.equals(segment.getMethod())) {
      return true;
    }

    if (TextPatternMethod.RANDOM.equals(segment.getMethod())) {
      return segment.getParameter().matches("^#+$");
    }

    if (TextPatternMethod.TEXT.equals(segment.getMethod())) {
      return segment.getParameter().matches("^[0-9]*$");
    }

    return false;
  }
}
