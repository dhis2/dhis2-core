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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;
import static org.hisp.dhis.util.DateUtils.dateIsValid;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.system.util.MathUtils;

/**
 * Formats a predicted value as a {@see DataValue} string.
 *
 * @author Jim Grace
 */
public class PredictionFormatter {
  /**
   * Formats a predicted value as a {@see DataValue} string.
   *
   * @param value
   * @param outputDataElement
   * @return formatted {@see DataValue} string
   */
  public static String formatPrediction(final Object value, final DataElement outputDataElement) {
    ValueType valueType = outputDataElement.getValueType();

    if (valueType.isNumeric() || (valueType.isText() && value instanceof Double)) {
      return formatNumericPrediction(value, outputDataElement);
    }

    if (valueType.isText()) {
      return formatTextPrediction(value);
    }

    if (valueType.isDate()) {
      return formatDatePrediction(value);
    }

    if (valueType.isBoolean()) {
      return formatBooleanPrediction(value);
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Formats a numeric predicted value as a string. */
  private static String formatNumericPrediction(
      final Object value, final DataElement outputDataElement) {
    final Object val = (value == null) ? 0.0 : value;

    if (!(val instanceof Double)) {
      return null;
    }

    Double dval = (Double) val;

    if (dval.isNaN()
        || dval.isInfinite()
        || dataValueIsZeroAndInsignificant(Double.toString(dval), outputDataElement)) {
      return null;
    }

    if (outputDataElement.getValueType().isInteger()) {
      return Long.toString(Math.round(dval));
    }

    return Double.toString(MathUtils.roundFraction(dval, 4));
  }

  /** Formats a date predicted value as a string. */
  private static String formatDatePrediction(final Object value) {
    if (value instanceof String && dateIsValid((String) value)) {
      return (String) value;
    }

    return null;
  }

  /** Formats a text predicted value as a string. */
  private static String formatTextPrediction(final Object value) {
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  /** Formats a boolean predicted value as a string. */
  private static String formatBooleanPrediction(final Object value) {
    if (value == null) {
      return "false";
    }

    if (value instanceof Boolean) {
      return ((Boolean) value).toString();
    }

    return null;
  }
}
