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
package org.hisp.dhis.indicator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IndicatorDeletionHandlerTest extends DhisSpringTest {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private PeriodService periodService;

  @Autowired private UserService _userService;

  private Indicator indicator;

  @Override
  public void setUpTest() {
    userService = _userService;

    IndicatorType type = new IndicatorType("Type", 1, true);
    manager.save(type);

    indicator = createIndicator('A', type);
    manager.save(indicator);
  }

  @Test
  void testAllowDeleteIndicatorType() {
    IndicatorType typeA = new IndicatorType("TypeA", 1, true);
    IndicatorType typeB = new IndicatorType("TypeB", 1, true);

    manager.save(typeA);
    manager.save(typeB);

    indicator.setIndicatorType(typeB);
    manager.update(indicator);

    manager.delete(typeA);
    assertThrows(DeleteNotAllowedException.class, () -> manager.delete(typeB));
  }

  @Test
  void testDeleteIndicatorGroup() {
    IndicatorGroup groupA = createIndicatorGroup('A');
    groupA.addIndicator(indicator);
    manager.save(groupA);

    assertTrue(indicator.getGroups().contains(groupA));

    manager.delete(groupA);

    assertFalse(indicator.getGroups().contains(groupA));
  }

  @Test
  void testDeleteDataSet() {
    PeriodType monthly = PeriodType.getPeriodTypeByName("Monthly");
    monthly = periodService.reloadPeriodType(monthly);

    DataSet setA = createDataSet('A', monthly);
    setA.addIndicator(indicator);
    manager.save(setA);

    assertTrue(indicator.getDataSets().contains(setA));

    manager.delete(setA);

    assertFalse(indicator.getDataSets().contains(setA));
  }

  @Test
  void testDeleteLegendSet() {
    LegendSet setA = createLegendSet('A');
    manager.save(setA);

    indicator.getLegendSets().add(setA);
    manager.update(indicator);

    assertTrue(indicator.getLegendSets().contains(setA));

    manager.delete(setA);

    assertFalse(indicator.getLegendSets().contains(setA));
  }

  @Test
  void testAllowDeleteDataElement() {
    DataElement elementA = createDataElement('A');
    DataElement elementB = createDataElement('B');
    DataElement elementC = createDataElement('C');

    manager.save(elementA);
    manager.save(elementB);
    manager.save(elementC);

    indicator.setNumerator("#{" + elementB.getUid() + "}");
    indicator.setDenominator("#{" + elementC.getUid() + "}");
    manager.update(indicator);

    manager.delete(elementA);
    assertThrows(DeleteNotAllowedException.class, () -> manager.delete(elementB));
    assertThrows(DeleteNotAllowedException.class, () -> manager.delete(elementC));
  }

  @Test
  void testAllowDeleteCategoryCombo() {
    CategoryCombo comboA = createCategoryCombo('A');
    CategoryCombo comboB = createCategoryCombo('B');
    CategoryCombo comboC = createCategoryCombo('C');

    manager.save(comboA);
    manager.save(comboB);
    manager.save(comboC);

    CategoryOptionCombo cocA = createCategoryOptionCombo('A');
    CategoryOptionCombo cocB = createCategoryOptionCombo('B');
    CategoryOptionCombo cocC = createCategoryOptionCombo('C');

    cocA.setCategoryCombo(comboA);
    cocB.setCategoryCombo(comboB);
    cocC.setCategoryCombo(comboC);

    manager.save(cocA);
    manager.save(cocB);
    manager.save(cocC);

    comboA.getOptionCombos().add(cocA);
    comboB.getOptionCombos().add(cocB);
    comboC.getOptionCombos().add(cocC);

    manager.update(comboA);
    manager.update(comboB);
    manager.update(comboC);

    indicator.setNumerator("#{abcdefghijk." + cocB.getUid() + "}");
    indicator.setDenominator("#{abcdefghijk." + cocC.getUid() + "}");
    manager.update(indicator);

    manager.delete(comboA);
    assertThrows(DeleteNotAllowedException.class, () -> manager.delete(comboB));
    assertThrows(DeleteNotAllowedException.class, () -> manager.delete(comboC));
  }
}
