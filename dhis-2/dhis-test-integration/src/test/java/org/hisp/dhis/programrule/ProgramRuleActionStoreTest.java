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
package org.hisp.dhis.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ProgramRuleActionStoreTest extends PostgresIntegrationTestBase {

  private ProgramRule programRuleA;

  private DataElement dataElementA;

  private Program programA;

  @Autowired private ProgramRuleStore programRuleStore;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramRuleActionStore actionStore;

  @Autowired private ProgramService programService;

  @Autowired private ProgramNotificationTemplateService programNotificationTemplateService;

  @BeforeAll
  void setUp() {
    programA = createProgram('A', null, null);
    programRuleA = createProgramRule('A', programA);
    dataElementA = createDataElement('A');
    programService.addProgram(programA);
    programRuleStore.save(programRuleA);
    dataElementService.addDataElement(dataElementA);
  }

  @Test
  void testWhenFilerIsEmptySpace() {
    ProgramRuleAction actionA =
        new ProgramRuleAction(
            "ActionA",
            programRuleA,
            ProgramRuleActionType.HIDEFIELD,
            dataElementA,
            null,
            null,
            null,
            null,
            null,
            "$myvar",
            "true",
            null,
            null);
    ProgramRuleAction actionB =
        new ProgramRuleAction(
            "ActionB",
            programRuleA,
            ProgramRuleActionType.SHOWERROR,
            null,
            null,
            null,
            null,
            null,
            "con",
            "Hello",
            "$placeofliving",
            null,
            null);
    ProgramRuleAction actionC =
        new ProgramRuleAction(
            "ActionC",
            programRuleA,
            ProgramRuleActionType.HIDESECTION,
            null,
            null,
            null,
            null,
            null,
            "con",
            "Hello",
            "$placeofliving",
            null,
            null);
    actionStore.save(actionA);
    actionStore.save(actionB);
    actionStore.save(actionC);
    assertTrue(
        actionStore.getMalFormedRuleActionsByType(ProgramRuleActionType.SHOWERROR).isEmpty());
    assertTrue(
        actionStore
            .getMalFormedRuleActionsByType(ProgramRuleActionType.HIDESECTION)
            .contains(actionC));
  }

  @Test
  void testGetProgramActionsWithNoNotification() {
    ProgramRuleAction actionA =
        new ProgramRuleAction(
            "ActionA",
            programRuleA,
            ProgramRuleActionType.SENDMESSAGE,
            null,
            null,
            null,
            null,
            null,
            null,
            "$myvar",
            "true",
            null,
            null);
    ProgramRuleAction actionB =
        new ProgramRuleAction(
            "ActionB",
            programRuleA,
            ProgramRuleActionType.SENDMESSAGE,
            null,
            null,
            null,
            null,
            null,
            "con",
            "Hello",
            "$placeofliving",
            null,
            null);

    ProgramNotificationTemplate pnt =
        createProgramNotificationTemplate(
            "test123",
            3,
            NotificationTrigger.PROGRAM_RULE,
            ProgramNotificationRecipient.USER_GROUP);

    programNotificationTemplateService.save(pnt);

    actionA.setNotificationTemplate(pnt);
    actionStore.save(actionA);
    actionStore.save(actionB);
    assertEquals(1, actionStore.getProgramActionsWithNoNotification().size());
    assertTrue(actionStore.getProgramActionsWithNoNotification().contains(actionB));
  }

  @Test
  void testGetProgramActionsWithNoDataObject() {
    ProgramRuleAction actionA =
        new ProgramRuleAction(
            "ActionA",
            programRuleA,
            ProgramRuleActionType.HIDEFIELD,
            dataElementA,
            null,
            null,
            null,
            null,
            null,
            "$myvar",
            "true",
            null,
            null);
    ProgramRuleAction actionB =
        new ProgramRuleAction(
            "ActionB",
            programRuleA,
            ProgramRuleActionType.HIDEFIELD,
            null,
            null,
            null,
            null,
            null,
            "con",
            "Hello",
            "$placeofliving",
            null,
            null);
    actionStore.save(actionA);
    actionStore.save(actionB);
    assertEquals(1, actionStore.getProgramActionsWithNoDataObject().size());
    assertTrue(actionStore.getProgramActionsWithNoDataObject().contains(actionB));
  }

  @Test
  @DisplayName("retrieving Program Rule Actions by data element returns expected entries")
  void getProgramRuleVariablesByDataElementTest() {
    // given
    DataElement deX = createDataElementAndSave('x');
    DataElement deY = createDataElementAndSave('y');
    DataElement deZ = createDataElementAndSave('z');

    ProgramRuleAction pra1 = createProgramRuleAction('a');
    pra1.setDataElement(deX);
    ProgramRuleAction pra2 = createProgramRuleAction('b');
    pra2.setDataElement(deY);
    ProgramRuleAction pra3 = createProgramRuleAction('c');
    pra3.setDataElement(deZ);
    ProgramRuleAction pra4 = createProgramRuleAction('d');

    actionStore.save(pra1);
    actionStore.save(pra2);
    actionStore.save(pra3);
    actionStore.save(pra4);

    // when
    List<ProgramRuleAction> programRuleActions =
        actionStore.getByDataElement(List.of(deX, deY, deZ));

    // then
    assertEquals(3, programRuleActions.size());
    assertTrue(
        programRuleActions.stream()
            .map(ProgramRuleAction::getDataElement)
            .toList()
            .containsAll(List.of(deX, deY, deZ)));
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    dataElementService.addDataElement(de);
    return de;
  }
}
