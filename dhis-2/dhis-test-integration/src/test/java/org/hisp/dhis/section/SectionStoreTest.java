/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SectionStoreTest extends PostgresIntegrationTestBase {

  @Autowired private SectionStore sectionStore;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("retrieving Sections by DataElement returns expected entries")
  void sectionsByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    createSectionAndSave(deW, 'a');
    createSectionAndSave(deX, 'b');
    createSectionAndSave(deY, 'c');
    createSectionAndSave(deZ, 'd');

    // when
    List<Section> sections = sectionStore.getSectionsByDataElement(List.of(deW, deX, deY));

    // then
    assertEquals(3, sections.size());
    assertTrue(
        sections.stream()
            .flatMap(s -> s.getDataElements().stream())
            .toList()
            .containsAll(List.of(deW, deX, deY)));
  }

  @Test
  @DisplayName("section names can be the same across different data sets")
  void sectionNameCanBeSameAcrossDataSets() {
    DataSet dataSetA = createDataSet('A', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet dataSetB = createDataSet('B', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    manager.save(dataSetA);
    manager.save(dataSetB);

    Section sectionInA = new Section();
    sectionInA.setName("First Section");
    sectionInA.setDataSet(dataSetA);
    manager.save(sectionInA);

    Section sectionInB = new Section();
    sectionInB.setName("First Section");
    sectionInB.setDataSet(dataSetB);
    manager.save(sectionInB);
    entityManager.flush();

    assertNotNull(sectionInA.getId());
    assertNotNull(sectionInB.getId());
  }

  @Test
  @DisplayName("section names must be unique within a data set")
  void sectionNameMustBeUniqueWithinDataSet() {
    DataSet dataSet = createDataSet('C', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    manager.save(dataSet);

    Section section1 = new Section();
    section1.setName("First Section");
    section1.setDataSet(dataSet);
    manager.save(section1);

    Section section2 = new Section();
    section2.setName("First Section");
    section2.setDataSet(dataSet);
    manager.save(section2);

    assertThrows(Exception.class, () -> entityManager.flush());
  }

  private DataElement createDataElementAndSave(char c) {
    CategoryCombo cc = createCategoryCombo(c);
    manager.save(cc);

    DataElement de = createDataElement(c, cc);
    manager.save(de);
    return de;
  }

  private void createSectionAndSave(DataElement de, char c) {
    DataSet ds = createDataSet(c, PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    manager.save(ds);

    Section section = new Section();
    section.setName("section " + c);
    section.getDataElements().add(de);
    section.setDataSet(ds);
    manager.save(section);
  }
}
