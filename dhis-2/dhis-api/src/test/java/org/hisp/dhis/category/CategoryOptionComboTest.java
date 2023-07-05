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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CategoryOptionCombo}.
 *
 * @author Volker Schmidt
 */
class CategoryOptionComboTest {

  private Date jan1;

  private Date jan2;

  private Date jan4;

  private Date jan5;

  private Date jan6;

  private CategoryOption optionA;

  private CategoryOption optionB;

  private CategoryOption optionC;

  private CategoryOptionCombo optionComboA;

  private CategoryOptionCombo optionComboB;

  private CategoryOptionCombo optionComboC;

  private CategoryCombo categoryComboA;

  private CategoryCombo categoryComboB;

  private CategoryCombo categoryComboC;

  private DataElement dataElement;

  private DataSet dataSetA;

  private DataSet dataSetB;

  private DataSet dataSetC;

  private Program program;

  @BeforeEach
  void before() {
    jan1 = new DateTime(2000, 1, 1, 0, 0).toDate();
    jan2 = new DateTime(2000, 1, 2, 0, 0).toDate();
    jan4 = new DateTime(2000, 1, 4, 0, 0).toDate();
    jan5 = new DateTime(2000, 1, 5, 0, 0).toDate();
    jan6 = new DateTime(2000, 1, 6, 0, 0).toDate();
    optionA = new CategoryOption("optionA");
    optionB = new CategoryOption("optionB");
    optionC = new CategoryOption("optionC");
    optionB.setStartDate(jan1);
    optionB.setEndDate(jan4);
    optionC.setStartDate(jan2);
    optionC.setEndDate(jan5);
    optionComboA = new CategoryOptionCombo();
    optionComboB = new CategoryOptionCombo();
    optionComboC = new CategoryOptionCombo();
    optionComboA.setName("optionComboA");
    optionComboB.setName("optionComboB");
    optionComboC.setName("optionComboC");
    optionComboA.addCategoryOption(optionA);
    optionComboB.addCategoryOption(optionB);
    optionComboC.addCategoryOption(optionA);
    optionComboC.addCategoryOption(optionB);
    optionComboC.addCategoryOption(optionC);
    categoryComboA = new CategoryCombo();
    categoryComboB = new CategoryCombo();
    categoryComboC = new CategoryCombo();
    categoryComboA.getOptionCombos().add(optionComboA);
    categoryComboB.getOptionCombos().add(optionComboB);
    categoryComboC.getOptionCombos().add(optionComboC);
    optionComboA.setCategoryCombo(categoryComboA);
    optionComboB.setCategoryCombo(categoryComboB);
    optionComboC.setCategoryCombo(categoryComboC);
    dataElement = new DataElement("dataElementA");
    dataSetA = new DataSet("dataSetA", new DailyPeriodType());
    dataSetB = new DataSet("dataSetB", new DailyPeriodType());
    dataSetC = new DataSet("dataSetC", new DailyPeriodType());
    dataSetA.setCategoryCombo(categoryComboA);
    dataSetB.setCategoryCombo(categoryComboB);
    dataSetB.setCategoryCombo(categoryComboC);
    dataSetA.addDataSetElement(dataElement);
    dataSetB.addDataSetElement(dataElement);
    dataSetC.addDataSetElement(dataElement);
    dataSetA.setOpenPeriodsAfterCoEndDate(0);
    dataSetB.setOpenPeriodsAfterCoEndDate(1);
    dataSetC.setOpenPeriodsAfterCoEndDate(2);
    program = new Program();
  }

  @Test
  void hasDefault() {
    assertTrue(SystemDefaultMetadataObject.class.isAssignableFrom(CategoryOption.class));
  }

  @Test
  void testIsDefault() {
    categoryComboA.setName(CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME);
    assertTrue(optionComboA.isDefault());
  }

  @Test
  void testIsNotDefault() {
    categoryComboA.setName(CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME + "x");
    assertFalse(optionComboA.isDefault());
  }

  @Test
  void testGetDateRangeDataSet() {
    DateRange dateRange;
    // [Option combo date
    dateRange = optionComboA.getDateRange(dataSetA);
    // range: null]
    // setOpenPeriodsAfterCoEndDate:
    // +0
    assertNull(dateRange.getStartDate());
    assertNull(dateRange.getEndDate());
    // [Jan 1-4] +0
    dateRange = optionComboB.getDateRange(dataSetA);
    assertEquals(jan1, dateRange.getStartDate());
    assertEquals(jan4, dateRange.getEndDate());
    // [Jan 1-4] +1
    dateRange = optionComboB.getDateRange(dataSetB);
    assertEquals(jan1, dateRange.getStartDate());
    assertEquals(jan5, dateRange.getEndDate());
    // [Jan 1-4] +2
    dateRange = optionComboB.getDateRange(dataSetC);
    assertEquals(jan1, dateRange.getStartDate());
    assertEquals(jan6, dateRange.getEndDate());
    // [null, Jan 1-4,
    dateRange = optionComboC.getDateRange(dataSetA);
    // Jan 2-5] +0
    assertEquals(jan2, dateRange.getStartDate());
    assertEquals(jan4, dateRange.getEndDate());
    // [null, Jan 1-4,
    dateRange = optionComboC.getDateRange(dataSetB);
    // Jan 2-5] +1
    assertEquals(jan2, dateRange.getStartDate());
    assertEquals(jan5, dateRange.getEndDate());
    // [null, Jan 1-4,
    dateRange = optionComboC.getDateRange(dataSetC);
    // Jan 2-5] +2
    assertEquals(jan2, dateRange.getStartDate());
    assertEquals(jan6, dateRange.getEndDate());
  }

  @Test
  void testGetDateRangeDataElement() {
    DateRange dateRange;
    // [null] +0, +1,
    dateRange = optionComboA.getDateRange(dataElement);
    // +2
    assertNull(dateRange.getStartDate());
    assertNull(dateRange.getEndDate());
    // [Jan 1-4] +0,
    dateRange = optionComboB.getDateRange(dataElement);
    // +1, +2
    assertEquals(jan1, dateRange.getStartDate());
    assertEquals(jan6, dateRange.getEndDate());
    // [null, Jan 1-4,
    dateRange = optionComboC.getDateRange(dataElement);
    // Jan 2-5] +0,
    // +1, +2
    assertEquals(jan2, dateRange.getStartDate());
    assertEquals(jan6, dateRange.getEndDate());
  }

  @Test
  void testGetDateRangeProgram() {
    DateRange dateRange;
    dateRange = optionComboA.getDateRange(program);
    assertNull(dateRange.getStartDate());
    assertNull(dateRange.getEndDate());
    dateRange = optionComboB.getDateRange(program);
    assertEquals(jan1, dateRange.getStartDate());
    assertEquals(jan4, dateRange.getEndDate());
    dateRange = optionComboC.getDateRange(program);
    assertEquals(jan2, dateRange.getStartDate());
    assertEquals(jan4, dateRange.getEndDate());
    program.setOpenDaysAfterCoEndDate(2);
    dateRange = optionComboA.getDateRange(program);
    assertNull(dateRange.getStartDate());
    assertNull(dateRange.getEndDate());
    dateRange = optionComboB.getDateRange(program);
    assertEquals(jan1, dateRange.getStartDate());
    assertEquals(jan6, dateRange.getEndDate());
    dateRange = optionComboC.getDateRange(program);
    assertEquals(jan2, dateRange.getStartDate());
    assertEquals(jan6, dateRange.getEndDate());
  }

  @Test
  void testGetLatestStartDate() {
    assertNull(optionComboA.getLatestStartDate());
    assertEquals(jan1, optionComboB.getLatestStartDate());
    assertEquals(jan2, optionComboC.getLatestStartDate());
  }

  @Test
  void testGetEarliestEndDate() {
    assertNull(optionComboA.getEarliestEndDate());
    assertEquals(jan4, optionComboB.getEarliestEndDate());
    assertEquals(jan4, optionComboC.getEarliestEndDate());
  }
}
