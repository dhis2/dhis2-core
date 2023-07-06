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
package org.hisp.dhis.dxf2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
class SectionUtilsTest extends DhisSpringTest {

  @Autowired private DataSetService dataSetService;

  @Autowired private DataElementService dataElementService;

  @Autowired private SectionUtils sectionUtils;

  @Test
  void testDataSetSection() {
    CategoryOption categoryOptionA = new CategoryOption("OptionA");
    CategoryOption categoryOptionB = new CategoryOption("OptionB");
    CategoryOption categoryOptionC = new CategoryOption("OptionC");
    CategoryOption categoryOptionD = new CategoryOption("OptionD");
    CategoryOption categoryOptionE = new CategoryOption("OptionE");
    CategoryOption categoryOptionF = new CategoryOption("OptionF");
    CategoryOption categoryOptionG = new CategoryOption("OptionG");
    categoryService.addCategoryOption(categoryOptionA);
    categoryService.addCategoryOption(categoryOptionB);
    categoryService.addCategoryOption(categoryOptionC);
    categoryService.addCategoryOption(categoryOptionD);
    categoryService.addCategoryOption(categoryOptionE);
    categoryService.addCategoryOption(categoryOptionF);
    categoryService.addCategoryOption(categoryOptionG);
    Category categoryA = createCategory('A', categoryOptionA, categoryOptionB);
    Category categoryB = createCategory('B', categoryOptionC, categoryOptionD);
    Category categoryC = createCategory('C', categoryOptionE, categoryOptionF);
    categoryService.addCategory(categoryA);
    categoryService.addCategory(categoryB);
    categoryService.addCategory(categoryC);
    List<Category> categoriesAB = new ArrayList<>();
    List<Category> categoriesABC = new ArrayList<>();
    categoriesAB.add(categoryA);
    categoriesAB.add(categoryB);
    categoriesABC.add(categoryA);
    categoriesABC.add(categoryB);
    categoriesABC.add(categoryC);
    CategoryCombo categoryComboAB =
        new CategoryCombo("CategoryComboA", DataDimensionType.DISAGGREGATION, categoriesAB);
    CategoryCombo categoryComboABC =
        new CategoryCombo("CategoryComboB", DataDimensionType.DISAGGREGATION, categoriesABC);
    long catAB = categoryService.addCategoryCombo(categoryComboAB);
    long catABC = categoryService.addCategoryCombo(categoryComboABC);
    long catDefault = categoryService.getDefaultCategoryCombo().getId();
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C', categoryComboAB);
    DataElement dataElementD = createDataElement('D', categoryComboAB);
    DataElement dataElementE = createDataElement('E');
    DataElement dataElementF = createDataElement('F', categoryComboABC);
    DataElement dataElementG = createDataElement('G', categoryComboABC);
    DataElement dataElementH = createDataElement('H', categoryComboAB);
    DataElement dataElementI = createDataElement('I', categoryComboABC);
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    dataElementService.addDataElement(dataElementD);
    dataElementService.addDataElement(dataElementE);
    dataElementService.addDataElement(dataElementF);
    dataElementService.addDataElement(dataElementG);
    dataElementService.addDataElement(dataElementH);
    dataElementService.addDataElement(dataElementI);
    PeriodType periodType = new MonthlyPeriodType();
    DataSet dataSetA = createDataSet('A', periodType);
    dataSetA.addDataSetElement(dataElementA);
    dataSetA.addDataSetElement(dataElementB);
    dataSetA.addDataSetElement(dataElementC);
    dataSetA.addDataSetElement(dataElementD);
    dataSetA.addDataSetElement(dataElementE);
    dataSetA.addDataSetElement(dataElementF);
    dataSetA.addDataSetElement(dataElementG);
    dataSetA.addDataSetElement(dataElementH);
    dataSetA.addDataSetElement(dataElementI);
    Section section = new Section();
    List<DataElement> dataElements = new ArrayList<>();
    dataElements.add(dataElementA);
    dataElements.add(dataElementB);
    dataElements.add(dataElementC);
    dataElements.add(dataElementD);
    dataElements.add(dataElementE);
    dataElements.add(dataElementF);
    dataElements.add(dataElementG);
    dataElements.add(dataElementH);
    dataElements.add(dataElementI);
    section.setDataElements(dataElements);
    dataSetA.getSections().add(section);
    long idA = dataSetService.addDataSet(dataSetA);
    DataSet ds = dataSetService.getDataSet(idA);
    assertEquals(idA, ds.getId());
    assertEquals(9, ds.getDataSetElements().size());
    assertEquals(1, ds.getSections().size());
    Section sec = ds.getSections().iterator().next();
    assertEquals(9, sec.getDataElements().size());
    Map<String, Collection<DataElement>> orderedDataElements =
        sectionUtils.getOrderedDataElementsMap(sec);
    List<String> keys = new ArrayList<>(orderedDataElements.keySet());
    assertEquals(6, orderedDataElements.keySet().size());
    String key1 = sec.getId() + "-" + catDefault + "-" + "0";
    String key2 = sec.getId() + "-" + catAB + "-" + "1";
    String key3 = sec.getId() + "-" + catDefault + "-" + "2";
    String key4 = sec.getId() + "-" + catABC + "-" + "3";
    String key5 = sec.getId() + "-" + catAB + "-" + "4";
    String key6 = sec.getId() + "-" + catABC + "-" + "5";
    assertEquals(key1, keys.get(0));
    assertEquals(key2, keys.get(1));
    assertEquals(key3, keys.get(2));
    assertEquals(key4, keys.get(3));
    assertEquals(key5, keys.get(4));
    assertEquals(key6, keys.get(5));
    assertEquals(2, orderedDataElements.get(key1).size());
    assertEquals(2, orderedDataElements.get(key2).size());
    assertEquals(1, orderedDataElements.get(key3).size());
    assertEquals(2, orderedDataElements.get(key4).size());
    assertEquals(1, orderedDataElements.get(key5).size());
    assertEquals(1, orderedDataElements.get(key6).size());
  }
}
