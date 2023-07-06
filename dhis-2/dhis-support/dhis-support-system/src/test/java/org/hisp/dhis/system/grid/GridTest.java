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
package org.hisp.dhis.system.grid;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.feedback.ErrorCode.E7230;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Reference;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class GridTest {
  private Grid gridA;

  private Grid gridB;

  private GridHeader headerA;

  private GridHeader headerB;

  private GridHeader headerC;

  @BeforeEach
  void setUp() {
    gridA = new ListGrid();
    gridB = new ListGrid();
    headerA =
        new GridHeader(
            "ColA",
            "colA",
            ValueType.TEXT,
            false,
            true,
            null,
            null,
            "programStage",
            new RepeatableStageParams());
    headerB = new GridHeader("ColB", "colB", ValueType.TEXT, false, true);
    headerC = new GridHeader("ColC", "colC", ValueType.TEXT, true, false);
    gridA.addHeader(headerA);
    gridA.addHeader(headerB);
    gridA.addHeader(headerC);
    gridA.addRow();
    gridA.addValue(11);
    gridA.addValue(12);
    gridA.addValue(13);
    gridA.addRow();
    gridA.addValue(21);
    gridA.addValue(22);
    gridA.addValue(23);
    gridA.addRow();
    gridA.addValue(31);
    gridA.addValue(32);
    gridA.addValue(33);
    gridA.addRow();
    gridA.addValue(41);
    gridA.addValue(42);
    gridA.addValue(43);
    gridB.addRow();
    gridB.addValue(11);
    gridB.addValue(12);
    gridB.addValue(13);
  }

  @Test
  void testAddGrid() {
    gridA.addRows(gridB);
    assertEquals(5, gridA.getHeight());
  }

  @Test
  void testAddHeaders() {
    Grid grid = new ListGrid();
    GridHeader headerA = new GridHeader("DataElementA", "Data element A");
    GridHeader headerB = new GridHeader("DataElementB", "Data element B");
    GridHeader headerC = new GridHeader("DataElementC", "Data element C");
    grid.addHeader(headerA);
    grid.addHeader(headerB);
    assertEquals(2, grid.getHeaders().size());
    assertEquals(headerA, grid.getHeaders().get(0));
    assertEquals(headerB, grid.getHeaders().get(1));
    grid.addHeader(1, headerC);
    assertEquals(3, grid.getHeaders().size());
    assertEquals(headerA, grid.getHeaders().get(0));
    assertEquals(headerC, grid.getHeaders().get(1));
    assertEquals(headerB, grid.getHeaders().get(2));
  }

  @Test
  void testColumnIsEmpty() {
    Grid grid =
        new ListGrid()
            .addRow()
            .addValuesVar("A1", null, "A3", null)
            .addRow()
            .addValuesVar("B1", null, "B3", null)
            .addRow()
            .addValuesVar(null, null, "C3", null)
            .addRow()
            .addValuesVar("D1", null, null, null);
    assertFalse(grid.columnIsEmpty(0));
    assertTrue(grid.columnIsEmpty(1));
    assertFalse(grid.columnIsEmpty(2));
    assertTrue(grid.columnIsEmpty(3));
  }

  @Test
  void testRemoveEmptyColumns() {
    Grid grid =
        new ListGrid()
            .addHeader(new GridHeader("H1"))
            .addHeader(new GridHeader("H2"))
            .addHeader(new GridHeader("H3"))
            .addHeader(new GridHeader("H4"))
            .addRow()
            .addValuesVar("A1", null, "A3", null)
            .addRow()
            .addValuesVar("B1", null, "B3", null)
            .addRow()
            .addValuesVar(null, null, "C3", null)
            .addRow()
            .addValuesVar("D1", null, null, null);
    assertEquals(4, grid.getHeaders().size());
    assertEquals(4, grid.getWidth());
    grid.removeEmptyColumns();
    assertEquals(2, grid.getWidth());
  }

  @Test
  void testRemoveEmptyColumnsWithoutHeaders() {
    Grid grid =
        new ListGrid()
            .addRow()
            .addValuesVar("A1", null, "A3", null)
            .addRow()
            .addValuesVar("B1", null, "B3", null)
            .addRow()
            .addValuesVar(null, null, "C3", null)
            .addRow()
            .addValuesVar("D1", null, null, null);
    assertEquals(4, grid.getWidth());
    grid.removeEmptyColumns();
    assertEquals(2, grid.getWidth());
  }

  @Test
  void testAddHeaderList() {
    Grid grid = new ListGrid();
    GridHeader headerA = new GridHeader("DataElementA", "Data element A");
    GridHeader headerB = new GridHeader("DataElementB", "Data element B");
    GridHeader headerC = new GridHeader("DataElementC", "Data element C");
    GridHeader headerD = new GridHeader("DataElementC", "Data element D");
    GridHeader headerE = new GridHeader("DataElementC", "Data element E");
    grid.addHeader(headerA);
    grid.addHeader(headerB);
    assertEquals(2, grid.getHeaders().size());
    List<GridHeader> headers = Lists.newArrayList(headerC, headerD, headerE);
    grid.addHeaders(1, headers);
    assertEquals(5, grid.getHeaders().size());
    assertEquals(headerA, grid.getHeaders().get(0));
    assertEquals(headerC, grid.getHeaders().get(1));
    assertEquals(headerD, grid.getHeaders().get(2));
    assertEquals(headerE, grid.getHeaders().get(3));
    assertEquals(headerB, grid.getHeaders().get(4));
  }

  @Test
  void testSubstituteMetaData() {
    Map<Object, Object> metaData = new HashMap<>();
    metaData.put(11, "Eleven");
    metaData.put(12, "Twelve");
    metaData.put(21, "TwentyOne");
    metaData.put(22, "TwentyTwo");
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals(12, gridA.getValue(0, 1));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals(22, gridA.getValue(1, 1));
    gridA.substituteMetaData(metaData);
    assertEquals("Eleven", gridA.getValue(0, 0));
    assertEquals("Twelve", gridA.getValue(0, 1));
    assertEquals("TwentyOne", gridA.getValue(1, 0));
    assertEquals("TwentyTwo", gridA.getValue(1, 1));
  }

  @Test
  void testSubstituteMetaDataForIndexA() {
    Map<Object, Object> metaData = new HashMap<>();
    metaData.put(11, "Eleven");
    metaData.put(12, "Twelve");
    metaData.put(21, "TwentyOne");
    metaData.put(22, "TwentyTwo");
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals(12, gridA.getValue(0, 1));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals(22, gridA.getValue(1, 1));
    gridA.substituteMetaData(1, 1, metaData);
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals("Twelve", gridA.getValue(0, 1));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals("TwentyTwo", gridA.getValue(1, 1));
  }

  @Test
  void testSubstituteMetaDataForIndexB() {
    Map<Object, Object> metaData = new HashMap<>();
    metaData.put(11, "Twelve");
    metaData.put(21, "TwentyTwo");
    metaData.put(31, "ThirtyTwo");
    metaData.put(41, "FourtyTwo");
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals(31, gridA.getValue(2, 0));
    assertEquals(41, gridA.getValue(3, 0));
    assertEquals(12, gridA.getValue(0, 1));
    assertEquals(22, gridA.getValue(1, 1));
    assertEquals(32, gridA.getValue(2, 1));
    assertEquals(42, gridA.getValue(3, 1));
    gridA.substituteMetaData(0, 1, metaData);
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals(31, gridA.getValue(2, 0));
    assertEquals(41, gridA.getValue(3, 0));
    assertEquals("Twelve", gridA.getValue(0, 1));
    assertEquals("TwentyTwo", gridA.getValue(1, 1));
    assertEquals("ThirtyTwo", gridA.getValue(2, 1));
    assertEquals("FourtyTwo", gridA.getValue(3, 1));
  }

  @Test
  void testGetHeight() {
    assertEquals(4, gridA.getHeight());
  }

  @Test
  void testGetWidth() {
    assertEquals(3, gridA.getWidth());
  }

  @Test
  void testGetRow() {
    List<Object> rowA = gridA.getRow(0);
    assertTrue(rowA.size() == 3);
    assertTrue(rowA.contains(11));
    assertTrue(rowA.contains(12));
    assertTrue(rowA.contains(13));
    List<Object> rowB = gridA.getRow(1);
    assertTrue(rowB.size() == 3);
    assertTrue(rowB.contains(21));
    assertTrue(rowB.contains(22));
    assertTrue(rowB.contains(23));
  }

  @Test
  void testGetHeaders() {
    assertEquals(3, gridA.getHeaders().size());
  }

  @Test
  void tetsGetVisibleHeaders() {
    assertEquals(2, gridA.getVisibleHeaders().size());
    assertTrue(gridA.getVisibleHeaders().contains(headerA));
    assertTrue(gridA.getVisibleHeaders().contains(headerB));
  }

  @Test
  void testGetRows() {
    assertEquals(4, gridA.getRows().size());
    assertEquals(3, gridA.getWidth());
  }

  @Test
  void testGetGetVisibleRows() {
    assertEquals(4, gridA.getVisibleRows().size());
    assertEquals(2, gridA.getVisibleRows().get(0).size());
    assertEquals(2, gridA.getVisibleRows().get(1).size());
    assertEquals(2, gridA.getVisibleRows().get(2).size());
    assertEquals(2, gridA.getVisibleRows().get(3).size());
  }

  @Test
  void testGetColumn() {
    List<Object> column1 = gridA.getColumn(1);
    assertEquals(4, column1.size());
    assertTrue(column1.contains(12));
    assertTrue(column1.contains(22));
    assertTrue(column1.contains(32));
    assertTrue(column1.contains(42));
    List<Object> column2 = gridA.getColumn(2);
    assertEquals(4, column2.size());
    assertTrue(column2.contains(13));
    assertTrue(column2.contains(23));
    assertTrue(column2.contains(33));
    assertTrue(column2.contains(43));
  }

  @Test
  void testAddColumn() {
    List<Object> columnValues = new ArrayList<>();
    columnValues.add(14);
    columnValues.add(24);
    columnValues.add(34);
    columnValues.add(44);
    gridA.addColumn(columnValues);
    List<Object> column3 = gridA.getColumn(3);
    assertEquals(4, column3.size());
    assertTrue(column3.contains(14));
    assertTrue(column3.contains(24));
    assertTrue(column3.contains(34));
    assertTrue(column3.contains(44));
    List<Object> row2 = gridA.getRow(1);
    assertEquals(4, row2.size());
    assertTrue(row2.contains(21));
    assertTrue(row2.contains(22));
    assertTrue(row2.contains(23));
    assertTrue(row2.contains(24));
  }

  @Test
  void testAddColumnAtIndex() {
    List<Object> columnValues = new ArrayList<>();
    columnValues.add(14);
    columnValues.add(24);
    columnValues.add(34);
    columnValues.add(44);
    gridA.addColumn(1, columnValues);
    List<Object> column1 = gridA.getColumn(1);
    assertEquals(4, column1.size());
    assertTrue(column1.contains(14));
    assertTrue(column1.contains(24));
    assertTrue(column1.contains(34));
    assertTrue(column1.contains(44));
    List<Object> column2 = gridA.getColumn(2);
    assertEquals(4, column2.size());
    assertTrue(column2.contains(12));
    assertTrue(column2.contains(22));
    assertTrue(column2.contains(32));
    assertTrue(column2.contains(42));
    List<Object> row2 = gridA.getRow(1);
    assertEquals(4, row2.size());
    assertTrue(row2.contains(21));
    assertTrue(row2.contains(24));
    assertTrue(row2.contains(22));
    assertTrue(row2.contains(23));
  }

  @Test
  void testAddAndPopulateColumnsBeforeA() {
    assertEquals(3, gridA.getWidth());
    Map<Object, List<?>> valueMap = new HashMap<>();
    valueMap.put(12, Lists.newArrayList(101, 102, 103));
    valueMap.put(22, Lists.newArrayList(201, 202, 203));
    valueMap.put(32, Lists.newArrayList(301, 302, 303));
    gridA.addAndPopulateColumnsBefore(1, valueMap, 3);
    assertEquals(6, gridA.getWidth());
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals(101, gridA.getValue(0, 1));
    assertEquals(102, gridA.getValue(0, 2));
    assertEquals(103, gridA.getValue(0, 3));
    assertEquals(12, gridA.getValue(0, 4));
    assertEquals(13, gridA.getValue(0, 5));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals(201, gridA.getValue(1, 1));
    assertEquals(202, gridA.getValue(1, 2));
    assertEquals(203, gridA.getValue(1, 3));
    assertEquals(22, gridA.getValue(1, 4));
    assertEquals(23, gridA.getValue(1, 5));
    assertEquals(31, gridA.getValue(2, 0));
    assertEquals(301, gridA.getValue(2, 1));
    assertEquals(302, gridA.getValue(2, 2));
    assertEquals(303, gridA.getValue(2, 3));
    assertEquals(32, gridA.getValue(2, 4));
    assertEquals(33, gridA.getValue(2, 5));
  }

  @Test
  void testAddAndPopulateColumnsBeforeB() {
    assertEquals(3, gridA.getWidth());
    Map<Object, List<?>> valueMap = new HashMap<>();
    valueMap.put(22, Lists.newArrayList(201, 202));
    valueMap.put(32, Lists.newArrayList(301));
    gridA.addAndPopulateColumnsBefore(1, valueMap, 2);
    assertEquals(5, gridA.getWidth());
    assertEquals(11, gridA.getValue(0, 0));
    assertEquals(null, gridA.getValue(0, 1));
    assertEquals(null, gridA.getValue(0, 2));
    assertEquals(12, gridA.getValue(0, 3));
    assertEquals(13, gridA.getValue(0, 4));
    assertEquals(21, gridA.getValue(1, 0));
    assertEquals(201, gridA.getValue(1, 1));
    assertEquals(202, gridA.getValue(1, 2));
    assertEquals(22, gridA.getValue(1, 3));
    assertEquals(23, gridA.getValue(1, 4));
    assertEquals(31, gridA.getValue(2, 0));
    assertEquals(301, gridA.getValue(2, 1));
    assertEquals(null, gridA.getValue(2, 2));
    assertEquals(32, gridA.getValue(2, 3));
    assertEquals(33, gridA.getValue(2, 4));
  }

  @Test
  void testRemoveColumn() {
    assertEquals(3, gridA.getWidth());
    gridA.removeColumn(2);
    assertEquals(2, gridA.getWidth());
  }

  @Test
  void testRemoveColumnByHeader() {
    assertEquals(3, gridA.getWidth());
    gridA.removeColumn(headerB);
    assertEquals(2, gridA.getWidth());
  }

  @Test
  void testRemoveCurrentWriteRow() {
    assertEquals(4, gridA.getRows().size());
    gridA.addRow();
    gridA.addValue(51);
    gridA.addValue(52);
    gridA.addValue(53);
    assertEquals(5, gridA.getRows().size());
    gridA.removeCurrentWriteRow();
    assertEquals(4, gridA.getRows().size());
    gridA.addRow();
    gridA.addValue(51);
    gridA.addValue(52);
    gridA.addValue(53);
    assertEquals(5, gridA.getRows().size());
  }

  @Test
  void testLimit() {
    assertEquals(4, gridA.getRows().size());
    gridA.limitGrid(2);
    assertEquals(2, gridA.getRows().size());
    List<Object> rowA = gridA.getRow(0);
    assertTrue(rowA.contains(11));
    List<Object> rowB = gridA.getRow(1);
    assertTrue(rowB.contains(21));
    gridA.limitGrid(0);
    assertEquals(2, gridA.getRows().size());
  }

  @Test
  void testLimitShortList() {
    assertEquals(4, gridA.getRows().size());
    gridA.limitGrid(6);
    assertEquals(4, gridA.getRows().size());
    gridA.limitGrid(4);
    assertEquals(4, gridA.getRows().size());
  }

  @Test
  void testLimits() {
    assertEquals(4, gridA.getRows().size());
    gridA.limitGrid(1, 3);
    assertEquals(2, gridA.getRows().size());
    List<Object> rowA = gridA.getRow(0);
    assertTrue(rowA.contains(21));
    List<Object> rowB = gridA.getRow(1);
    assertTrue(rowB.contains(31));
  }

  @Test
  void testSortA() {
    Grid grid = new ListGrid();
    grid.addRow().addValue(1).addValue("a");
    grid.addRow().addValue(2).addValue("b");
    grid.addRow().addValue(3).addValue("c");
    grid.sortGrid(2, 1);
    List<Object> row1 = grid.getRow(0);
    assertTrue(row1.contains("c"));
    List<Object> row2 = grid.getRow(1);
    assertTrue(row2.contains("b"));
    List<Object> row3 = grid.getRow(2);
    assertTrue(row3.contains("a"));
  }

  @Test
  void testSortB() {
    Grid grid = new ListGrid();
    grid.addRow().addValue(3).addValue("a");
    grid.addRow().addValue(2).addValue("b");
    grid.addRow().addValue(1).addValue("c");
    grid.sortGrid(1, -1);
    List<Object> row1 = grid.getRow(0);
    assertTrue(row1.contains(1));
    List<Object> row2 = grid.getRow(1);
    assertTrue(row2.contains(2));
    List<Object> row3 = grid.getRow(2);
    assertTrue(row3.contains(3));
  }

  @Test
  void testSortC() {
    Grid grid = new ListGrid();
    grid.addRow().addValue(1).addValue("c");
    grid.addRow().addValue(3).addValue("a");
    grid.addRow().addValue(2).addValue("b");
    grid.sortGrid(1, 1);
    List<Object> row1 = grid.getRow(0);
    assertTrue(row1.contains(3));
    List<Object> row2 = grid.getRow(1);
    assertTrue(row2.contains(2));
    List<Object> row3 = grid.getRow(2);
    assertTrue(row3.contains(1));
  }

  @Test
  void testSortD() {
    Grid grid = new ListGrid();
    grid.addRow().addValue("a").addValue("a").addValue(5.2);
    grid.addRow().addValue("b").addValue("b").addValue(0.0);
    grid.addRow().addValue("c").addValue("c").addValue(108.1);
    grid.addRow().addValue("d").addValue("d").addValue(45.0);
    grid.addRow().addValue("e").addValue("e").addValue(4043.9);
    grid.addRow().addValue("f").addValue("f").addValue(0.1);
    grid = grid.sortGrid(3, 1);
    List<Object> row1 = grid.getRow(0);
    assertTrue(row1.contains(4043.9));
    List<Object> row2 = grid.getRow(1);
    assertTrue(row2.contains(108.1));
    List<Object> row3 = grid.getRow(2);
    assertTrue(row3.contains(45.0));
    List<Object> row4 = grid.getRow(3);
    assertTrue(row4.contains(5.2));
    List<Object> row5 = grid.getRow(4);
    assertTrue(row5.contains(0.1));
    List<Object> row6 = grid.getRow(5);
    assertTrue(row6.contains(0.0));
  }

  @Test
  void testSortE() {
    Grid grid = new ListGrid();
    grid.addRow().addValue("two").addValue(2);
    grid.addRow().addValue("null").addValue(null);
    grid.addRow().addValue("three").addValue(3);
    grid.sortGrid(2, 1);
    List<Object> row1 = grid.getRow(0);
    assertTrue(row1.contains("three"));
    List<Object> row2 = grid.getRow(1);
    assertTrue(row2.contains("two"));
    List<Object> row3 = grid.getRow(2);
    assertTrue(row3.contains("null"));
  }

  @Test
  void testSortF() {
    Grid grid = new ListGrid();
    grid.addRow().addValue("two").addValue(2);
    grid.addRow().addValue("null").addValue(null);
    grid.addRow().addValue("one").addValue(1);
    grid.sortGrid(2, -1);
    List<Object> row1 = grid.getRow(0);
    assertTrue(row1.contains("null"));
    List<Object> row2 = grid.getRow(1);
    assertTrue(row2.contains("one"));
    List<Object> row3 = grid.getRow(2);
    assertTrue(row3.contains("two"));
  }

  @Test
  void testGridRowComparator() {
    List<List<Object>> lists = new ArrayList<>();
    List<Object> l1 = List.of("b", "b", 50);
    List<Object> l2 = List.of("c", "c", 400);
    List<Object> l3 = List.of("a", "a", 6);
    lists.add(l1);
    lists.add(l2);
    lists.add(l3);
    Comparator<List<Object>> comparator = new ListGrid.GridRowComparator(2, -1);
    Collections.sort(lists, comparator);
    assertEquals(l3, lists.get(0));
    assertEquals(l1, lists.get(1));
    assertEquals(l2, lists.get(2));
  }

  @Test
  void testAddRegressionColumn() {
    gridA = new ListGrid();
    gridA.addRow();
    gridA.addValue(10.0);
    gridA.addRow();
    gridA.addValue(50.0);
    gridA.addRow();
    gridA.addValue(20.0);
    gridA.addRow();
    gridA.addValue(60.0);
    gridA.addRegressionColumn(0, true);
    List<Object> column = gridA.getColumn(1);
    assertTrue(column.size() == 4);
    assertTrue(column.contains(17.0));
    assertTrue(column.contains(29.0));
    assertTrue(column.contains(41.0));
    assertTrue(column.contains(53.0));
  }

  @Test
  void testAddCumulativeColumn() {
    gridA = new ListGrid();
    gridA.addRow();
    gridA.addValue(10.0);
    gridA.addRow();
    gridA.addValue(50.0);
    gridA.addRow();
    gridA.addValue(20.0);
    gridA.addRow();
    gridA.addValue(60.0);
    gridA.addCumulativeColumn(0, true);
    List<Object> column = gridA.getColumn(1);
    assertTrue(column.size() == 4);
    assertTrue(column.contains(10.0));
    assertTrue(column.contains(60.0));
    assertTrue(column.contains(80.0));
    assertTrue(column.contains(140.0));
  }

  @Test
  void testGetMetaColumnIndexes() {
    List<Integer> expected = new ArrayList<>();
    expected.add(0);
    expected.add(1);
    assertEquals(expected, gridA.getMetaColumnIndexes());
  }

  @Test
  void testGetUniqueValues() {
    gridA.addRow();
    gridA.addValue(11);
    gridA.addValue(12);
    gridA.addValue(13);
    Set<Object> expected = new HashSet<>();
    expected.add(12);
    expected.add(22);
    expected.add(32);
    expected.add(42);
    assertEquals(expected, gridA.getUniqueValues("ColB"));
  }

  @Test
  void testGetAsMap() {
    Map<String, Integer> map = gridA.getAsMap(2, "-");
    assertEquals(4, map.size());
    assertEquals(Integer.valueOf(13), map.get("11-12"));
    assertEquals(Integer.valueOf(23), map.get("21-22"));
    assertEquals(Integer.valueOf(33), map.get("31-32"));
    assertEquals(Integer.valueOf(43), map.get("41-42"));
  }

  @Test
  void testJRDataSource() throws Exception {
    assertTrue(gridA.next());
    assertEquals(11, gridA.getFieldValue(new MockJRField("colA")));
    assertEquals(12, gridA.getFieldValue(new MockJRField("colB")));
    assertEquals(13, gridA.getFieldValue(new MockJRField("colC")));
    assertTrue(gridA.next());
    assertEquals(21, gridA.getFieldValue(new MockJRField("colA")));
    assertEquals(22, gridA.getFieldValue(new MockJRField("colB")));
    assertEquals(23, gridA.getFieldValue(new MockJRField("colC")));
    assertTrue(gridA.next());
    assertEquals(31, gridA.getFieldValue(new MockJRField("colA")));
    assertEquals(32, gridA.getFieldValue(new MockJRField("colB")));
    assertEquals(33, gridA.getFieldValue(new MockJRField("colC")));
    assertTrue(gridA.next());
    assertEquals(41, gridA.getFieldValue(new MockJRField("colA")));
    assertEquals(42, gridA.getFieldValue(new MockJRField("colB")));
    assertEquals(43, gridA.getFieldValue(new MockJRField("colC")));
    assertFalse(gridA.next());
  }

  @Test
  void testAddValuesAsList() {
    Grid grid = new ListGrid();
    grid.addRow().addValuesAsList(Lists.newArrayList("colA1", "colB1", "colC1"));
    grid.addRow().addValuesAsList(Lists.newArrayList("colA2", "colB2", "colC2"));
    assertEquals(2, grid.getHeight());
    assertEquals(3, grid.getWidth());
    assertEquals("colB1", grid.getRow(0).get(1));
    assertEquals("colC2", grid.getRow(1).get(2));
  }

  @Test
  void testRetainColumns() {
    // Given
    GridHeader headerA = new GridHeader("headerA", "Header A");
    GridHeader headerB = new GridHeader("headerB", "Header B");
    GridHeader headerC = new GridHeader("headerC", "Header C");

    Grid grid = new ListGrid();
    grid.addHeader(headerA);
    grid.addHeader(headerB);
    grid.addHeader(headerC);
    grid.addRow().addValue(1).addValue("a").addValue("a-1");
    grid.addRow().addValue(2).addValue("b").addValue("b-1");
    grid.addRow().addValue(3).addValue("c").addValue("c-1");

    Set<String> headers = new LinkedHashSet<>(List.of("headerA", "headerB"));

    // When
    grid.retainColumns(headers);

    // Then
    assertThat(grid.getHeaderWidth(), is(equalTo(2)));
    assertThat(grid.getHeaders().get(0).getName(), is(equalTo("headerA")));
    assertThat(grid.getHeaders().get(1).getName(), is(equalTo("headerB")));
  }

  @Test
  void testRepositionHeadersA() {
    // Given
    GridHeader headerA = new GridHeader("headerA", "Header A");
    GridHeader headerB = new GridHeader("headerB", "Header B");
    GridHeader headerC = new GridHeader("headerC", "Header C");

    Grid grid = new ListGrid();
    grid.addHeader(headerA);
    grid.addHeader(headerB);
    grid.addHeader(headerC);
    grid.addRow().addValue(1).addValue("a").addValue("a-1");
    grid.addRow().addValue(2).addValue("b").addValue("b-1");
    grid.addRow().addValue(3).addValue("c").addValue("c-1");

    List<String> headers = List.of("headerC", "headerB", "headerA");

    // When
    grid.repositionHeaders(headers);

    // Then
    assertThat(grid.getHeaderWidth(), is(equalTo(3)));
    assertThat(grid.getHeaders().get(0).getName(), is(equalTo("headerC")));
    assertThat(grid.getHeaders().get(1).getName(), is(equalTo("headerB")));
    assertThat(grid.getHeaders().get(2).getName(), is(equalTo("headerA")));
  }

  @Test
  void testGetIndexOfHeader() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader("headerA", "Header A"));
    grid.addHeader(new GridHeader("headerB", "Header B"));
    grid.addHeader(new GridHeader("headerC", "Header C"));
    grid.addRow().addValue(1).addValue("a").addValue("a-1");
    grid.addRow().addValue(2).addValue("b").addValue("b-1");
    grid.addRow().addValue(3).addValue("c").addValue("c-1");

    assertEquals(0, grid.getIndexOfHeader("headerA"));
    assertEquals(1, grid.getIndexOfHeader("headerB"));
    assertEquals(2, grid.getIndexOfHeader("headerC"));
    assertEquals(-1, grid.getIndexOfHeader("headerX"));
  }

  @Test
  void testHeaderExists() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader("headerA", "Header A"));
    grid.addHeader(new GridHeader("headerB", "Header B"));
    grid.addHeader(new GridHeader("headerC", "Header C"));
    grid.addRow().addValue(1).addValue("a").addValue("a-1");
    grid.addRow().addValue(2).addValue("b").addValue("b-1");
    grid.addRow().addValue(3).addValue("c").addValue("c-1");

    assertTrue(grid.headerExists("headerA"));
    assertTrue(grid.headerExists("headerB"));
    assertTrue(grid.headerExists("headerC"));
    assertFalse(grid.headerExists("headerX"));
  }

  @Test
  void testRepositionHeadersUsingInvalidHeader() {
    // Given
    GridHeader headerA = new GridHeader("headerA", "Header A");
    GridHeader headerB = new GridHeader("headerB", "Header B");
    GridHeader headerC = new GridHeader("headerC", "Header C");

    Grid grid = new ListGrid();
    grid.addHeader(headerA);
    grid.addHeader(headerB);
    grid.addHeader(headerC);
    grid.addRow().addValue(1).addValue("a").addValue("a-1");
    grid.addRow().addValue(2).addValue("b").addValue("b-1");
    grid.addRow().addValue(3).addValue("c").addValue("c-1");

    List<String> headers = List.of("invalidHeader", "headerB", "headerA");

    // When
    IllegalQueryException expectedException =
        assertThrows(IllegalQueryException.class, () -> grid.repositionHeaders(headers));

    // Then
    assertThat(
        expectedException.getMessage(), is(equalTo("Header param `invalidHeader` does not exist")));
    assertThat(expectedException.getErrorCode(), is(equalTo(E7230)));
  }

  @Test
  void testRepositionHeadersB() {
    // Given
    GridHeader headerA = new GridHeader("headerA", "Header A");
    GridHeader headerB = new GridHeader("headerB", "Header B");
    GridHeader headerC = new GridHeader("headerC", "Header C");

    Grid grid = new ListGrid();
    grid.addHeader(headerA);
    grid.addHeader(headerB);
    grid.addHeader(headerC);
    grid.addRow().addValue(1).addValue("a").addValue("a-1");
    grid.addRow().addValue(2).addValue("b").addValue("b-1");
    grid.addRow().addValue(3).addValue("c").addValue("c-1");

    List<String> headers = List.of("headerC", "headerB", "headerA");
    List<Integer> columnIndexes = grid.repositionHeaders(headers);

    // When
    grid.repositionColumns(columnIndexes);

    // Then
    assertThat(grid.getHeaderWidth(), is(equalTo(3)));

    // first row + repositioned columns
    assertThat(grid.getRow(0).get(0), is(equalTo("a-1")));
    assertThat(grid.getRow(0).get(1), is(equalTo("a")));
    assertThat(grid.getRow(0).get(2), is(equalTo(1)));

    // second row + repositioned columns
    assertThat(grid.getRow(1).get(0), is(equalTo("b-1")));
    assertThat(grid.getRow(1).get(1), is(equalTo("b")));
    assertThat(grid.getRow(1).get(2), is(equalTo(2)));

    // third row + repositioned columns
    assertThat(grid.getRow(2).get(0), is(equalTo("c-1")));
    assertThat(grid.getRow(2).get(1), is(equalTo("c")));
    assertThat(grid.getRow(2).get(2), is(equalTo(3)));
  }

  @Test
  void testAddReference() {
    String jsonString =
        "{ \"id\" : \n"
            + "      {\n"
            + "         \"firstName\": \"something\",\n"
            + "         \"lastName\" : \"something\"\n"
            + "      }\n"
            + "}";

    ObjectMapper mapper = new ObjectMapper();

    JsonNode node = null;
    try {
      node = mapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      fail();
    }

    Reference reference = new Reference("uuid", node);

    gridA.addReference(reference);

    // assert
    assertEquals(1, gridA.getRefs().size());

    assertNull(gridB.getRefs());
  }

  @Test
  void testRepeatableStageParamInHeaderTest() {
    // arrange act assert
    assertEquals(
        "startIndex:0 count:1 startDate:null endDate: null",
        gridA.getHeaders().get(0).getRepeatableStageParams());

    assertNull(gridA.getHeaders().get(1).getRepeatableStageParams());

    assertEquals(0, gridA.getHeaders().get(0).getStageOffset());

    assertNull(gridA.getHeaders().get(1).getStageOffset());
  }
}
