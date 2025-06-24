/*
 * Copyright (c) 2004-2025, University of Oslo
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

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataIntegrityProgramStagesNoPrograms extends AbstractDataIntegrityIntegrationTest {

  @Autowired private ProgramService programService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramStageService programStageService;

  private static final String CHECK = "program_stages_no_programs";

  private static final String DETAILS_ID_TYPE = "programStages";

  @Test
  void testProgramStagesWithProgram() {
    setUpTest();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  public void setUpTest() {

    Program programA = new Program();
    programA.setAutoFields();
    programA.setName("Program A");
    programA.setShortName("Program A");
    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programA.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programA);

    ProgramStage programStageA = new ProgramStage();
    programStageA.setAutoFields();
    programStageA.setName("programStageA");
    programStageA.setProgram(programA);
    programStageService.saveProgramStage(programStageA);

    dbmsManager.clearSession();
  }
}
