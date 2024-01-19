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
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Combined integrity test for program rules. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/program_rules/}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityProgramIndicatorsControllerTest extends AbstractDataIntegrityIntegrationTest {

  @Autowired private ProgramService programService;

  @Autowired private ProgramIndicatorService programIndicatorService;

  private ProgramIndicator programIndicatorA;

  private static final String detailsIdType = "programIndicators";

  private static final String checkName = "program_indicators_without_expression";

  @Test
  void testProgramIndicatorNoExpression() {

    setUpTest();

    programIndicatorService.addProgramIndicator(programIndicatorA);

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        detailsIdType,
        checkName,
        100,
        programIndicatorA.getUid(),
        programIndicatorA.getName(),
        null,
        true);
  }

  @Test
  void testProgramIndicatorHasExpression() {

    setUpTest();

    programIndicatorA.setExpression("true");
    programIndicatorService.addProgramIndicator(programIndicatorA);

    dbmsManager.clearSession();

    assertHasNoDataIntegrityIssues(detailsIdType, checkName, true);
  }

  @Test
  void testProgramIndicatorChecksRun() {
    assertHasNoDataIntegrityIssues(detailsIdType, checkName, false);
  }

  public void setUpTest() {

    Program programA = new Program();
    programA.setAutoFields();
    programA.setName("Program A");
    programA.setShortName("Program A");
    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programA.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programA);

    programIndicatorA = new ProgramIndicator();
    programIndicatorA.setAutoFields();
    programIndicatorA.setName("ProgramRuleA");
    programIndicatorA.setShortName("ProgramRuleA");
    programIndicatorA.setProgram(programA);
  }
}
