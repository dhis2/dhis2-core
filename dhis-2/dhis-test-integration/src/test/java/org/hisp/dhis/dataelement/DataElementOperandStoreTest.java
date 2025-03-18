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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataElementOperandStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementOperandStore dataElementOperandStore;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("retrieving DataElementOperands by DataElement returns expected entries")
  void dataElementOperandByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    createDataElementOperandAndSave(deW);
    createDataElementOperandAndSave(deX);
    createDataElementOperandAndSave(deY);
    createDataElementOperandAndSave(deZ);

    // when
    List<DataElementOperand> dataElementOperands =
        dataElementOperandStore.getByDataElement(List.of(deW, deX));

    // then
    assertEquals(2, dataElementOperands.size());
    assertTrue(
        dataElementOperands.stream()
            .map(deo -> deo.getDataElement().getUid())
            .toList()
            .containsAll(List.of(deW.getUid(), deX.getUid())));
  }

  @Test
  @DisplayName("retrieving DataElementOperands by CategoryOptionCombo returns expected entries")
  void dataElementOperandByCatOptComboTest() {
    // given
    CategoryCombo cc = createCategoryCombo("1", "CatComUid01");
    manager.save(cc);

    CategoryOptionCombo coc1 = createCategoryOptionCombo(cc);
    CategoryOptionCombo coc2 = createCategoryOptionCombo(cc);
    CategoryOptionCombo coc3 = createCategoryOptionCombo(cc);
    CategoryOptionCombo coc4 = createCategoryOptionCombo(cc);
    manager.save(List.of(coc1, coc2, coc3, coc4));

    createDataElementOperandAndSave(coc1);
    createDataElementOperandAndSave(coc2);
    createDataElementOperandAndSave(coc3);
    createDataElementOperandAndSave(coc4);

    // when
    List<DataElementOperand> dataElementOperands =
        dataElementOperandStore.getByCategoryOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));

    // then
    assertEquals(2, dataElementOperands.size());
    assertTrue(
        dataElementOperands.stream()
            .map(deo -> deo.getCategoryOptionCombo().getUid())
            .toList()
            .containsAll(List.of(coc1.getUid(), coc2.getUid())));
  }

  private void createDataElementOperandAndSave(DataElement de) {
    DataElementOperand deo = new DataElementOperand();
    deo.setDataElement(de);
    manager.save(deo);
  }

  private void createDataElementOperandAndSave(CategoryOptionCombo coc) {
    DataElementOperand deo = new DataElementOperand();
    deo.setCategoryOptionCombo(coc);
    manager.save(deo);
  }

  private DataElement createDataElementAndSave(char c) {
    CategoryCombo cc = createCategoryCombo(c);
    manager.save(cc);

    DataElement de = createDataElement(c, cc);
    manager.save(de);
    return de;
  }
}
