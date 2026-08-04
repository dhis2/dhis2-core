/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Lars Helge Overland
 */
class GridUtilsTest {

  @Test
  void testFromHtml() throws Exception {
    String html =
        IOUtils.toString(
            new ClassPathResource("customform.html").getInputStream(), StandardCharsets.UTF_8);
    List<Grid> grids = GridUtils.fromHtml(html, "TitleA");
    assertNotNull(grids);
    assertEquals(6, grids.size());
    assertEquals("TitleA", grids.get(0).getTitle());
  }

  @Test
  void testGetGridIndexByDimensionItem() {
    Period period1 = PeriodType.getPeriodFromIsoString("202010");
    period1.setUid(CodeGenerator.generateUid());
    Period period2 = PeriodType.getPeriodFromIsoString("202011");
    period2.setUid(CodeGenerator.generateUid());
    Period period3 = PeriodType.getPeriodFromIsoString("202012");
    period3.setUid(CodeGenerator.generateUid());
    List<DimensionalItemObject> periods = Lists.newArrayList(period1, period2, period3);
    List<Object> row = new ArrayList<>(3);
    // dimension
    row.add(CodeGenerator.generateUid());
    // period
    row.add(period2.getIsoDate());
    // value
    row.add(10.22D);
    assertEquals(1, GridUtils.getGridIndexByDimensionItem(row, periods, 2));
    List<Object> row2 = new ArrayList<>(3);
    // dimension
    row2.add(CodeGenerator.generateUid());
    // period
    row2.add("201901");
    // value
    row2.add(10.22D);
    assertEquals(2, GridUtils.getGridIndexByDimensionItem(row2, periods, 2));
  }

  @Test
  void testToXls() {
    List<Grid> grids = new ArrayList<>();
    Grid gridA = new ListGrid();
    gridA.setTitle("Grid");
    grids.add(gridA);
    Grid gridB = new ListGrid();
    gridB.setTitle("Grid");
    grids.add(gridA);
    grids.add(gridB);
    OutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(() -> GridUtils.toXls(grids, outputStream));
  }

  @Test
  void testToXlsx() {
    Grid grid = new ListGrid();
    grid.setTitle("Grid");
    OutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(() -> GridUtils.toXlsx(grid, outputStream));
  }

  /**
   * toXlsx streams through an SXSSFWorkbook, which keeps only a window of rows in memory.
   * Everything about the workbook it produces has to stay as it was: this reads the bytes back and
   * pins the sheet name, the title and subtitle rows, the header row, and - the part a streaming
   * rewrite is most likely to lose - the cell types and the two distinct number formats, integer
   * against decimal.
   */
  @Test
  void testToXlsxProducesTheSameWorkbookAsBefore() throws Exception {
    Grid grid = new ListGrid();
    grid.setTitle("My title");
    grid.setSubtitle("My subtitle");
    grid.addHeader(new GridHeader("colA", "Column A"));
    grid.addHeader(new GridHeader("colB", "Column B"));
    grid.addHeader(new GridHeader("colC", "Column C"));

    // More rows than the in-memory window, so at least one flush happens before the write.
    int rows = 2500;
    for (int i = 0; i < rows; i++) {
      grid.addRow();
      grid.addValue("text " + i);
      grid.addValue(Integer.valueOf(i));
      grid.addValue(Double.valueOf(i + 0.5));
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GridUtils.toXlsx(grid, out);

    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertEquals("My title", workbook.getSheetName(0));

      assertEquals("My title", sheet.getRow(0).getCell(0).getStringCellValue());
      assertEquals("My subtitle", sheet.getRow(2).getCell(0).getStringCellValue());

      Row headerRow = sheet.getRow(4);
      assertEquals("Column A", headerRow.getCell(0).getStringCellValue());
      assertEquals("Column B", headerRow.getCell(1).getStringCellValue());
      assertEquals("Column C", headerRow.getCell(2).getStringCellValue());

      Row firstDataRow = sheet.getRow(5);
      assertEquals(CellType.STRING, firstDataRow.getCell(0).getCellType());
      assertEquals(CellType.NUMERIC, firstDataRow.getCell(1).getCellType());
      assertEquals(CellType.NUMERIC, firstDataRow.getCell(2).getCellType());
      assertEquals("text 0", firstDataRow.getCell(0).getStringCellValue());
      assertEquals(0d, firstDataRow.getCell(1).getNumericCellValue());
      assertEquals(0.5d, firstDataRow.getCell(2).getNumericCellValue());

      // The integer column carries the built-in #,##0 format; the decimal column does not.
      assertEquals("#,##0", firstDataRow.getCell(1).getCellStyle().getDataFormatString());
      assertEquals("#.##########", firstDataRow.getCell(2).getCellStyle().getDataFormatString());

      // Every row survived the flushing, including the last one.
      Row lastDataRow = sheet.getRow(4 + rows);
      assertEquals("text " + (rows - 1), lastDataRow.getCell(0).getStringCellValue());
      assertEquals(rows - 1, (int) lastDataRow.getCell(1).getNumericCellValue());
      assertEquals(4 + rows, sheet.getLastRowNum());
    }
  }

  /**
   * A failing write must not leave the spilled rows behind: on a server exporting grids this size
   * that fills a disk.
   */
  @Test
  void testTemporaryFilesAreRemovedWhenTheWriteFails() throws Exception {
    Grid grid = new ListGrid();
    grid.setTitle("Grid");
    grid.addHeader(new GridHeader("colA", "Column A"));
    for (int i = 0; i < 2500; i++) {
      grid.addRow();
      grid.addValue("text " + i);
    }

    long before = spilledSheetCount();

    assertThrows(
        IOException.class,
        () ->
            GridUtils.toXlsx(
                grid,
                new OutputStream() {
                  @Override
                  public void write(int b) throws IOException {
                    throw new IOException("the client went away");
                  }
                }));

    // POI creates this directory the first time it spills a sheet. If it is missing, the export
    // never streamed at all and the count below would be vacuously right.
    assertTrue(Files.isDirectory(POI_TEMP_DIRECTORY), POI_TEMP_DIRECTORY + " should exist");
    assertEquals(before, spilledSheetCount());
  }

  /** Where POI's DefaultTempFileCreationStrategy puts the rows an SXSSFWorkbook has flushed. */
  private static final Path POI_TEMP_DIRECTORY =
      Path.of(System.getProperty("java.io.tmpdir"), "poifiles");

  private static long spilledSheetCount() throws IOException {
    if (!Files.isDirectory(POI_TEMP_DIRECTORY)) {
      return 0;
    }
    try (var paths = Files.list(POI_TEMP_DIRECTORY)) {
      return paths.filter(p -> p.getFileName().toString().startsWith("poi-sxssf-sheet")).count();
    }
  }
}
