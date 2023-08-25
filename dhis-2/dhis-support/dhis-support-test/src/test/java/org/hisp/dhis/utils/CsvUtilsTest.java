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

