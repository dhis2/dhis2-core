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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.program.Program;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CategoryOption}.
 *
 * @author Volker Schmidt
 */
class CategoryOptionTest {

  @Test
  void hasDefault() {
    Assertions.assertTrue(SystemDefaultMetadataObject.class.isAssignableFrom(CategoryOption.class));
  }

  @Test
  void isDefault() {
    CategoryOption categoryOption = new CategoryOption();
    categoryOption.setName(CategoryOption.DEFAULT_NAME);
    Assertions.assertTrue(categoryOption.isDefault());
  }

  @Test
  void isNotDefault() {
    CategoryOption categoryOption = new CategoryOption();
    categoryOption.setName(CategoryOption.DEFAULT_NAME + "x");
    Assertions.assertFalse(categoryOption.isDefault());
  }

  @Test
  void getAdjustedDate_DataSet() {
    CategoryOption categoryOption = new CategoryOption();
    DataSet dataSet = new DataSet("dataSet", new DailyPeriodType());
    assertNull(categoryOption.getAdjustedEndDate(dataSet));
    categoryOption.setEndDate(new DateTime(2020, 1, 1, 0, 0).toDate());
    assertEquals(
        new DateTime(2020, 1, 1, 0, 0).toDate(), categoryOption.getAdjustedEndDate(dataSet));
    dataSet.setOpenPeriodsAfterCoEndDate(3);
    assertEquals(
        new DateTime(2020, 1, 4, 0, 0).toDate(), categoryOption.getAdjustedEndDate(dataSet));
  }

  @Test
  void getAdjustedDate_DataElement() {
    CategoryOption categoryOption = new CategoryOption();
    DataElement dataElement = new DataElement();
    DataSet dataSetA = new DataSet("dataSetA", new MonthlyPeriodType());
    DataSet dataSetB = new DataSet("dataSetB", new MonthlyPeriodType());
    dataSetA.addDataSetElement(dataElement);
    dataSetB.addDataSetElement(dataElement);
    dataElement.getDataSetElements().addAll(dataSetA.getDataSetElements());
    dataElement.getDataSetElements().addAll(dataSetB.getDataSetElements());
    assertNull(categoryOption.getAdjustedEndDate(dataElement));
    categoryOption.setEndDate(new DateTime(2020, 1, 1, 0, 0).toDate());
    assertEquals(
        new DateTime(2020, 1, 1, 0, 0).toDate(), categoryOption.getAdjustedEndDate(dataElement));
    dataSetA.setOpenPeriodsAfterCoEndDate(2);
    assertEquals(
        new DateTime(2020, 3, 1, 0, 0).toDate(), categoryOption.getAdjustedEndDate(dataElement));
    dataSetB.setOpenPeriodsAfterCoEndDate(4);
    assertEquals(
        new DateTime(2020, 5, 1, 0, 0).toDate(), categoryOption.getAdjustedEndDate(dataElement));
  }

  @Test
  void getAdjustedDate_Program() {
    CategoryOption categoryOption = new CategoryOption();
    Program program = new Program("program");
    assertNull(categoryOption.getAdjustedEndDate(program));
    categoryOption.setEndDate(new DateTime(2020, 1, 1, 0, 0).toDate());
    assertEquals(
        new DateTime(2020, 1, 1, 0, 0).toDate(), categoryOption.getAdjustedEndDate(program));
    program.setOpenDaysAfterCoEndDate(3);
    assertEquals(
        new DateTime(2020, 1, 4, 0, 0).toDate(), categoryOption.getAdjustedEndDate(program));
  }
}
