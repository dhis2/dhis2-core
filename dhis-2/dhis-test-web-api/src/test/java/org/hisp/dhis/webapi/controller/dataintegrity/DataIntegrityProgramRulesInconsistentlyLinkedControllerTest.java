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
package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.program.*;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Combined integrity test for program rules. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/program_rules/}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityProgramRulesInconsistentlyLinkedControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  @Autowired private ProgramService programService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramStageService programStageService;

  private ProgramStage programStageA;

  private Program programA;

  private Program programB;

  private static final String DETAILS_ID_TYPE = "programRules";

  private static final String CHECK = "program_rules_inconsistent_program_program_stage";

  @Test
  void testProgramRuleConsistentlyLinked() {

    setUpTest();
    dbmsManager.clearSession();
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void testProgramRuleInconsistentlyLinked() {
    setUpTest();

    ProgramStage programStageB = new ProgramStage();
    programStageB.setAutoFields();
    programStageB.setName("programStageB");
    programStageA.setProgram(programB);

    ProgramRule programRuleB = new ProgramRule();
    programRuleB.setAutoFields();
    programRuleB.setName("ProgramRuleB");
    programRuleB.setProgram(programA); // Purposefully set to program A
    programRuleB.setProgramStage(programStageB);
    programB.getProgramStages().add(programStageB);

    programStageService.saveProgramStage(programStageB);
    programRuleService.addProgramRule(programRuleB);

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        detailsIdType, CHECK, 50, programRuleB.getUid(), programRuleB.getName(), null, true);
  }

  public void setUpTest() {

    programA = new Program();
    programA.setAutoFields();
    programA.setName("Program A");
    programA.setShortName("Program A");
    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programA.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programA);

    programB = new Program();
    programB.setAutoFields();
    programB.setName("Program B");
    programB.setShortName("Program B");
    programB.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programB.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programB);

    programStageA = new ProgramStage();
    programStageA.setAutoFields();
    programStageA.setName("programStageA");
    programStageA.setProgram(programA);

    ProgramRule programRuleA = new ProgramRule();
    programRuleA.setAutoFields();
    programRuleA.setName("ProgramRuleA");
    programRuleA.setProgram(programA);
    programRuleA.setProgramStage(programStageA);
    programA.getProgramStages().add(programStageA);

    programStageService.saveProgramStage(programStageA);
    programRuleService.addProgramRule(programRuleA);
  }
}
