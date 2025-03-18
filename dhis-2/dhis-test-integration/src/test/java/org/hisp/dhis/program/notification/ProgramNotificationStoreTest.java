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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ProgramNotificationStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementService dataElementService;
  @Autowired private ProgramNotificationTemplateStore programNotificationTemplateStore;

  @Test
  @DisplayName("retrieving Program Notification Templates by data element returns expected entries")
  void getProgramNotificationTemplatesByDataElementTest() {
    // given
    DataElement deA = createDataElementAndSave('A');
    DataElement deB = createDataElementAndSave('B');
    DataElement deC = createDataElementAndSave('C');

    ProgramNotificationTemplate pnt1 = createProgramNotificationTemplate("temp 1", 2, null, null);
    pnt1.setRecipientDataElement(deA);
    ProgramNotificationTemplate pnt2 = createProgramNotificationTemplate("temp 2", 2, null, null);
    pnt2.setRecipientDataElement(deB);
    ProgramNotificationTemplate pnt3 = createProgramNotificationTemplate("temp 3", 2, null, null);
    pnt3.setRecipientDataElement(deC);
    ProgramNotificationTemplate pnt4 = createProgramNotificationTemplate("temp 4", 2, null, null);
    programNotificationTemplateStore.save(pnt1);
    programNotificationTemplateStore.save(pnt2);
    programNotificationTemplateStore.save(pnt3);
    programNotificationTemplateStore.save(pnt4);

    // when
    List<ProgramNotificationTemplate> programNotificationTemplates =
        programNotificationTemplateStore.getByDataElement(List.of(deA, deB, deC));

    // then
    assertEquals(3, programNotificationTemplates.size());
    assertTrue(
        programNotificationTemplates.stream()
            .map(ProgramNotificationTemplate::getRecipientDataElement)
            .toList()
            .containsAll(List.of(deA, deB, deC)));
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    dataElementService.addDataElement(de);
    return de;
  }
}
