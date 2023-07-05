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
package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Combined integrity test for program rules. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/program_rules/}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityProgramRulesControllerTest extends AbstractDataIntegrityIntegrationTest {

  @Autowired private ProgramService programService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramStageService programStageService;

  private ProgramRule programRuleA;

  private ProgramStage programStageA;

  private static final String detailsIdType = "programRules";

  @Test
  void testProgramRuleNoAction() {

    setUpTest();

    programStageService.saveProgramStage(programStageA);
    programRuleService.addProgramRule(programRuleA);

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        detailsIdType,
        "program_rules_no_action",
        100,
        programRuleA.getUid(),
        programRuleA.getName(),
        null,
        true);
    assertHasDataIntegrityIssues(
        detailsIdType,
        "program_rules_no_expression",
        100,
        programRuleA.getUid(),
        programRuleA.getName(),
        null,
        true);
  }

  @Test
  void testProgramRuleChecksRun() {
    assertHasNoDataIntegrityIssues(detailsIdType, "program_rules_no_action", false);
    assertHasNoDataIntegrityIssues(detailsIdType, "program_rules_no_expression", false);
    assertHasNoDataIntegrityIssues(detailsIdType, "program_rules_message_no_template", false);
  }

  @Test
  void testProgramRuleMessageTemplate() {

    setUpTest();

    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setAutoFields();
    programRuleAction.setName("Rule Action A");
    programRuleAction.setProgramRuleActionType(ProgramRuleActionType.SENDMESSAGE);
    programRuleA.getProgramRuleActions().add(programRuleAction);
    programStageService.saveProgramStage(programStageA);
    programRuleService.addProgramRule(programRuleA);

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        detailsIdType,
        "program_rules_message_no_template",
        100,
        programRuleA.getUid(),
        programRuleA.getName(),
        null,
        true);
  }

  public void setUpTest() {

    Program programA = new Program();
    programA.setAutoFields();
    programA.setName("Program A");
    programA.setShortName("Program A");
    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programA.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programA);

    programStageA = new ProgramStage();
    programStageA.setAutoFields();
    programStageA.setName("programStageA");
    programStageA.setProgram(programA);

    programRuleA = new ProgramRule();
    programRuleA.setAutoFields();
    programRuleA.setName("ProgramRuleA");
    programRuleA.setProgram(programA);
    programRuleA.setProgramStage(programStageA);
    programA.getProgramStages().add(programStageA);
  }
}
