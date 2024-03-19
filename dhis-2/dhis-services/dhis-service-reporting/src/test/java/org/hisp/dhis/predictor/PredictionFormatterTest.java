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

import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.FILE_RESOURCE;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.predictor.PredictionFormatter.formatPrediction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.Test;

/**
 * @author Jim Grace
 */
class PredictionFormatterTest extends DhisConvenienceTest {

  @Test
  void testFormatPredictionNumber() {
    DataElement deNumber = createDataElement('N', NUMBER, NONE);
    assertEquals("1234568.0", formatPrediction(1234567.89, deNumber));
    assertEquals("3.142", formatPrediction(3.14159265359, deNumber));
    assertNull(formatPrediction(0.0, deNumber));
    assertNull(formatPrediction(null, deNumber));
    // Value is not Double
    assertNull(formatPrediction(100, deNumber));
    assertNull(formatPrediction("Text", deNumber));
    assertNull(formatPrediction(Double.NaN, deNumber));
    assertNull(formatPrediction(Double.POSITIVE_INFINITY, deNumber));
    assertNull(formatPrediction(Double.NEGATIVE_INFINITY, deNumber));
    deNumber.setZeroIsSignificant(true);
    assertEquals("0.0", formatPrediction(0.0, deNumber));
    assertEquals("0.0", formatPrediction(null, deNumber));
  }

  @Test
  void testFormatPredictionInteger() {
    DataElement deInteger = createDataElement('I', INTEGER, NONE);
    assertEquals("1234568", formatPrediction(1234567.89, deInteger));
    assertEquals("3", formatPrediction(3.14159265359, deInteger));
    assertNull(formatPrediction(0.0, deInteger));
    assertNull(formatPrediction(null, deInteger));
    // Value is not Double
    assertNull(formatPrediction(100, deInteger));
    assertNull(formatPrediction("Text", deInteger));
    assertNull(formatPrediction(Double.NaN, deInteger));
    assertNull(formatPrediction(Double.POSITIVE_INFINITY, deInteger));
    assertNull(formatPrediction(Double.NEGATIVE_INFINITY, deInteger));
    deInteger.setZeroIsSignificant(true);
    assertEquals("0", formatPrediction(0.0, deInteger));
    assertEquals("0", formatPrediction(null, deInteger));
  }

  @Test
  void testFormatPredictionText() {
    DataElement deText = createDataElement('T', TEXT, NONE);
    assertEquals("Some text", formatPrediction("Some text", deText));
    assertEquals("2021-09-10", formatPrediction("2021-09-10", deText));
    assertEquals("1234568.0", formatPrediction(1234567.89, deText));
    assertEquals("3.142", formatPrediction(3.14159265359, deText));
    assertEquals("0.0", formatPrediction(0.0, deText));
    assertEquals("", formatPrediction(null, deText));
    assertEquals("100", formatPrediction(100, deText));
    assertEquals("false", formatPrediction(false, deText));
    assertEquals("true", formatPrediction(true, deText));
    assertNull(formatPrediction(Double.NaN, deText));
    assertNull(formatPrediction(Double.POSITIVE_INFINITY, deText));
    assertNull(formatPrediction(Double.NEGATIVE_INFINITY, deText));
  }

  @Test
  void testFormatPredictionBoolean() {
    DataElement deBoolean = createDataElement('B', BOOLEAN, NONE);
    assertEquals("true", formatPrediction(true, deBoolean));
    assertEquals("false", formatPrediction(false, deBoolean));
    assertEquals("false", formatPrediction(null, deBoolean));
    assertNull(formatPrediction(100.0, deBoolean));
    assertNull(formatPrediction("Some text", deBoolean));
  }

  @Test
  void testFormatPredictionDate() {
    DataElement deDate = createDataElement('D', DATE, NONE);
    assertEquals("2021-09-10", formatPrediction("2021-09-10", deDate));
    assertNull(formatPrediction(null, deDate));
    assertNull(formatPrediction(100.0, deDate));
    assertNull(formatPrediction("Some text", deDate));
    assertNull(formatPrediction(false, deDate));
  }

  @Test
  void testFormatPredictionOther() {
    DataElement deFileResource = createDataElement('F', FILE_RESOURCE, NONE);
    assertNull(formatPrediction(null, deFileResource));
    assertNull(formatPrediction(100.0, deFileResource));
    assertNull(formatPrediction("Some text", deFileResource));
    assertNull(formatPrediction(false, deFileResource));
  }
}
