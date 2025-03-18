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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
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
class SMSCommandStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementService dataElementService;
  @Autowired private SMSCommandStore smsCommandStore;
  @Autowired private CategoryOptionComboStore categoryOptionComboStore;

  @Test
  @DisplayName("retrieving SMS Codes by data element returns expected entries")
  void getSMSCodesByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    createSMSCodeAndSave(deW, "code 1");
    createSMSCodeAndSave(deX, "code 2");
    createSMSCodeAndSave(deY, "code 3");
    createSMSCodeAndSave(deZ, "code 4");

    // when
    List<SMSCode> allByDataElement = smsCommandStore.getCodesByDataElement(List.of(deW, deX, deY));

    // then
    assertEquals(3, allByDataElement.size());
    assertTrue(
        allByDataElement.stream()
            .map(code -> code.getDataElement().getUid())
            .toList()
            .containsAll(List.of(deW.getUid(), deX.getUid(), deY.getUid())));
  }

  @Test
  @DisplayName("retrieving SMS Codes by cat option combos returns expected entries")
  void getByCocTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    SMSCode code1 = createSMSCodeAndSave(deW, "code 1");
    CategoryOptionCombo coc1 = createCategoryOptionCombo('A');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc1);
    code1.setOptionId(coc1);

    SMSCode code2 = createSMSCodeAndSave(deX, "code 2");
    CategoryOptionCombo coc2 = createCategoryOptionCombo('B');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc2);
    code2.setOptionId(coc2);

    SMSCode code3 = createSMSCodeAndSave(deY, "code 3");
    CategoryOptionCombo coc3 = createCategoryOptionCombo('C');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc3);
    code3.setOptionId(coc3);

    SMSCode code4 = createSMSCodeAndSave(deZ, "code 4");
    CategoryOptionCombo coc4 = createCategoryOptionCombo('D');
    coc4.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc4);
    code4.setOptionId(coc4);

    // when
    List<SMSCode> allByCoc = smsCommandStore.getCodesByCategoryOptionCombo(UID.of(coc1, coc2));

    // then
    assertEquals(2, allByCoc.size());
    assertTrue(
        allByCoc.stream()
            .map(code -> code.getOptionId().getUid())
            .toList()
            .containsAll(List.of(coc1.getUid(), coc2.getUid())),
        "Codes should contain correct COC UIDs");
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    dataElementService.addDataElement(de);
    return de;
  }

  private SMSCode createSMSCodeAndSave(DataElement de, String code) {
    SMSCode smsCode = new SMSCode();
    smsCode.setCode("Code " + code);
    smsCode.setDataElement(de);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setCodes(Set.of(smsCode));
    smsCommand.setName("CMD " + code);
    smsCommandStore.save(smsCommand);
    return smsCode;
  }
}
