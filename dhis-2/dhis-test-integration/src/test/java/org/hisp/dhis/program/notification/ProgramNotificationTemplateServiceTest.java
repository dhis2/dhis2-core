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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class ProgramNotificationTemplateServiceTest extends SingleSetupIntegrationTestBase {

  private Program program;

  private ProgramStage programStage;

  private ProgramNotificationTemplate pnt1;

  private ProgramNotificationTemplate pnt2;

  private ProgramNotificationTemplate pnt3;

  private OrganisationUnit organisationUnit;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private ProgramNotificationTemplateService programNotificationTemplateService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Override
  protected void setUpTest() throws Exception {
    organisationUnit = createOrganisationUnit('O');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    program = createProgram('P');
    program.setAutoFields();
    program.setUid("P_UID_1");
    programService.addProgram(program);
    programStage = createProgramStage('S', program);
    programStage.setAutoFields();
    programStage.setUid("PS_UID_1");
    programStageService.saveProgramStage(programStage);
    pnt1 =
        createProgramNotificationTemplate(
            "test1", 1, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.USER_GROUP);
    pnt1.setAutoFields();
    pnt1.setUid("PNT_UID_1");
    pnt2 =
        createProgramNotificationTemplate(
            "test2", 1, NotificationTrigger.COMPLETION, ProgramNotificationRecipient.DATA_ELEMENT);
    pnt2.setAutoFields();
    pnt2.setUid("PNT_UID_2");
    pnt3 =
        createProgramNotificationTemplate(
            "test3",
            1,
            NotificationTrigger.PROGRAM_RULE,
            ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE);
    pnt3.setAutoFields();
    pnt3.setUid("PNT_UID_3");
    programNotificationTemplateService.save(pnt1);
    programNotificationTemplateService.save(pnt2);
    programNotificationTemplateService.save(pnt3);
    program.getNotificationTemplates().add(pnt1);
    program.getNotificationTemplates().add(pnt2);
  }

  @Test
  void testGetProgramNotificationTemplates() {
    ProgramNotificationTemplateParam param =
        ProgramNotificationTemplateParam.builder().program(program).build();
    List<ProgramNotificationTemplate> templates =
        programNotificationTemplateService.getProgramNotificationTemplates(param);
    assertFalse(templates.isEmpty());
    assertEquals(2, templates.size());
    assertTrue(templates.contains(pnt1));
    assertTrue(templates.contains(pnt2));
    assertFalse(templates.contains(pnt3));
  }

  @Test
  void testCountProgramNotificationTemplates() {
    ProgramNotificationTemplateParam param =
        ProgramNotificationTemplateParam.builder().program(program).build();
    ProgramNotificationTemplateParam param2 =
        ProgramNotificationTemplateParam.builder().programStage(programStage).build();
    assertEquals(
        programNotificationTemplateService.getProgramNotificationTemplates(param).size(),
        programNotificationTemplateService.countProgramNotificationTemplates(param));
    assertEquals(
        programNotificationTemplateService.getProgramNotificationTemplates(param2).size(),
        programNotificationTemplateService.countProgramNotificationTemplates(param2));
  }
}
