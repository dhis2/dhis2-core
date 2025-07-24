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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DataElementTest {

  private PeriodType periodType = new MonthlyPeriodType();

  @Test
  void testGetCategoryCombos() {
    CategoryCombo ccA = new CategoryCombo("CategoryComboA", DataDimensionType.DISAGGREGATION);
    CategoryCombo ccB = new CategoryCombo("CategoryComboB", DataDimensionType.DISAGGREGATION);
    DataSet dsA = new DataSet("DataSetA");
    DataSet dsB = new DataSet("DataSetB");
    DataElement deA = new DataElement("DataElementA");
    DataElement deB = new DataElement("DataElementB");
    deA.setCategoryCombo(ccA);
    dsA.addDataSetElement(deA);
    dsB.addDataSetElement(deA, ccB);
    assertEquals(2, deA.getCategoryCombos().size());
    assertEquals(Sets.newHashSet(ccA, ccB), deA.getCategoryCombos());
    deB.setCategoryCombo(ccA);
    dsB.addDataSetElement(deB);
    assertEquals(1, deB.getCategoryCombos().size());
    assertEquals(Sets.newHashSet(ccA), deB.getCategoryCombos());
  }

  @Test
  void testGetCategoryComboDataSet() {
    CategoryCombo ccA = new CategoryCombo("CategoryComboA", DataDimensionType.DISAGGREGATION);
    CategoryCombo ccB = new CategoryCombo("CategoryComboB", DataDimensionType.DISAGGREGATION);
    DataSet dsA = new DataSet("DataSetA");
    DataSet dsB = new DataSet("DataSetB");
    DataElement deA = new DataElement("DataElementA");
    DataElement deB = new DataElement("DataElementB");
    deA.setCategoryCombo(ccA);
    dsA.addDataSetElement(deA);
    dsB.addDataSetElement(deA, ccB);
    assertEquals(ccA, deA.getDataElementCategoryCombo(dsA));
    assertEquals(ccB, deA.getDataElementCategoryCombo(dsB));
    deB.setCategoryCombo(ccA);
    dsA.addDataSetElement(deB);
    assertEquals(ccA, deB.getDataElementCategoryCombo(dsA));
    assertEquals(ccA, deB.getDataElementCategoryCombo(dsB));
  }

  @Test
  void testGetPeriodType() {
    DataElement de = new DataElement();
    DataSet dsA = new DataSet("A", periodType);
    DataSet dsB = new DataSet("B", periodType);
    DataSet dsC = new DataSet("C", periodType);
    dsA.addDataSetElement(de);
    dsB.addDataSetElement(de);
    dsC.addDataSetElement(de);
    assertEquals(periodType, de.getPeriodType());
  }
}
