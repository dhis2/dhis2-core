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
package org.hisp.dhis.dataset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link SectionStore} additional methods.
 *
 * @author Jan Bernitt
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class SectionStoreTest extends PostgresIntegrationTestBase {
  @Autowired private SectionStore sectionStore;

  @Autowired private DataElementService dataElementService;

  @Autowired private DataSetService dataSetService;

  @Autowired private IdentifiableObjectManager manager;

  private DataElement de;

  private DataSet ds;

  @BeforeEach
  void setUp() {
    de = createDataElement('A');
    dataElementService.addDataElement(de);

    ds = createDataSet('A');
    dataSetService.addDataSet(ds);
  }

  @Test
  void testGetSectionsByDataElement_SectionOfDataElement() {
    Section s = new Section("test", ds, List.of(de), Set.of());
    assertDoesNotThrow(() -> sectionStore.save(s));

    assertEquals(1, sectionStore.getSectionsByDataElement(de.getUid()).size());
  }

  @Test
  void testGetSectionsByDataElement_SectionOfDataElementOperand() {
    DataElementOperand deo = new DataElementOperand(de);
    Section s = new Section("test", ds, List.of(), Set.of(deo));
    assertDoesNotThrow(() -> sectionStore.save(s));

    assertEquals(1, sectionStore.getSectionsByDataElement(de.getUid()).size());
  }

  @Test
  @DisplayName("get sections with indicator references")
  void getSectionsWithIndicators() {
    // given 3 sections exist, each of which has an indicator
    IndicatorType indicatorType = createIndicatorType('t');
    manager.save(indicatorType);

    Indicator indicator1 = createIndicator('a', indicatorType);
    Indicator indicator2 = createIndicator('b', indicatorType);
    Indicator indicator3 = createIndicator('c', indicatorType);

    manager.save(indicator1);
    manager.save(indicator2);
    manager.save(indicator3);

    Section s1 = new Section("s1", ds, List.of(), Set.of());
    s1.setIndicators(List.of(indicator1));
    Section s2 = new Section("s2", ds, List.of(), Set.of());
    s2.setIndicators(List.of(indicator2));
    Section s3 = new Section("s3", ds, List.of(), Set.of());
    s3.setIndicators(List.of(indicator3));

    sectionStore.save(s1);
    sectionStore.save(s2);
    sectionStore.save(s3);

    // when searching for sections with specific indicators
    List<Section> matchedSections =
        sectionStore.getSectionsByIndicators(List.of(indicator1, indicator2));

    // then only the 2 sections with matching indicators are retrieved
    assertTrue(matchedSections.contains(s1));
    assertTrue(matchedSections.contains(s2));

    // and the section with no matching indicator is not retrieved
    assertFalse(matchedSections.contains(s3));
  }
}
