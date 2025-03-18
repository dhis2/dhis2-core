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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ProgramNotificationTemplateServiceTest extends PostgresIntegrationTestBase {

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

  @BeforeAll
  void setUp() {
    organisationUnit = createOrganisationUnit('O');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    program = createProgram('P');
    programService.addProgram(program);
    programStage = createProgramStage('S', program);
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
  void shouldGetProgramNotificationTemplates() {
    ProgramNotificationTemplateOperationParams param =
        ProgramNotificationTemplateOperationParams.builder()
            .program(UID.of(program.getUid()))
            .build();

    List<ProgramNotificationTemplate> templates =
        programNotificationTemplateService.getProgramNotificationTemplates(param);

    assertEquals(List.of(pnt2, pnt1), templates);
  }

  @Test
  void shouldCountProgramNotificationTemplates() {
    ProgramNotificationTemplateOperationParams paramsOnlyWithProgram =
        ProgramNotificationTemplateOperationParams.builder()
            .program(UID.of(program.getUid()))
            .build();
    ProgramNotificationTemplateOperationParams paramsOnlyWithProgramStage =
        ProgramNotificationTemplateOperationParams.builder()
            .programStage(UID.of(programStage.getUid()))
            .build();

    // assert when associated with program
    assertEquals(
        programNotificationTemplateService
            .getProgramNotificationTemplates(paramsOnlyWithProgram)
            .size(),
        programNotificationTemplateService.countProgramNotificationTemplates(
            paramsOnlyWithProgram));

    // assert when associated with programStage
    assertEquals(
        programNotificationTemplateService
            .getProgramNotificationTemplates(paramsOnlyWithProgramStage)
            .size(),
        programNotificationTemplateService.countProgramNotificationTemplates(
            paramsOnlyWithProgramStage));
  }
}
