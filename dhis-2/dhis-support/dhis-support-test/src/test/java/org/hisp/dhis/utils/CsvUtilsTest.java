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
package org.hisp.dhis.utils;

import static org.hisp.dhis.utils.CsvUtils.getRowCountFromCsv;
import static org.hisp.dhis.utils.CsvUtils.getRowFromCsv;
import static org.hisp.dhis.utils.CsvUtils.getValueFromCsv;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CsvUtilsTest {

  @Test
  void testGetValueFromCsv() {
    String csv = "header1,header2\nrow1 value1,row1 value2\nrow2 value1,row2 value2";

    String header1 = getValueFromCsv(0, 0, csv);
    String header2 = getValueFromCsv(1, 0, csv);
    String row1Value1 = getValueFromCsv(0, 1, csv);
    String row1Value2 = getValueFromCsv(1, 1, csv);
    String row2Value1 = getValueFromCsv(0, 2, csv);
    String row2Value2 = getValueFromCsv(1, 2, csv);

    assertEquals("header1", header1);
    assertEquals("header2", header2);
    assertEquals("row1 value1", row1Value1);
    assertEquals("row1 value2", row1Value2);
    assertEquals("row2 value1", row2Value1);
    assertEquals("row2 value2", row2Value2);
  }

  @Test
  void testGetRowFromCsv() {
    String csv = "header1,header2\nrow1 value1,row1 value2\nrow2 value1,row2 value2";

    String headerRow = getRowFromCsv(0, csv);
    String firstRow = getRowFromCsv(1, csv);
    String secondRow = getRowFromCsv(2, csv);

    assertEquals("header1,header2", headerRow);
    assertEquals("row1 value1,row1 value2", firstRow);
    assertEquals("row2 value1,row2 value2", secondRow);
  }

  @Test
  void testGetRowCountFromCsv() {
    String csv = "header1,header2\nrow1 value1,row1 value2\nrow2 value1,row2 value2";

    int rowCount = getRowCountFromCsv(csv);
    assertEquals(3, rowCount);
  }
}
