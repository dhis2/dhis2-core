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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.event.ProgramRuleActionController} using
 * (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramRuleActionControllerTest extends H2ControllerIntegrationTestBase {
  private Program trackerProgramA;
  private ProgramStage trackerProgramStageA, trackerProgramStageB, eventProgramStage;
  private DataElement dataElement;
  private ProgramRule programRuleA, programRuleB;

  @BeforeAll
  public void setUp() {
    createProgramRuleActions();
  }

  @Test
  void testGetDataExpressionDescription() {
    String pId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'P1', 'shortName':'P1', 'programType':'WITHOUT_REGISTRATION'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Valid",
        POST("/programRuleActions/data/expression/description?programId=" + pId, "70")
            .content(HttpStatus.OK));
  }

  @Test
  void testGetDataExpressionDescription_NoSuchProgram() {
    assertEquals(
        "Program is specified but does not exist: abcdefghijk",
        POST("/programRuleActions/data/expression/description?programId=abcdefghijk", "70")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void testGetDataExpressionDescription_WithInvalidExpression() {
    String pId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'P1', 'shortName':'P1', 'programType':'WITHOUT_REGISTRATION'}"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Expression is not valid",
        POST("/programRuleActions/data/expression/description?programId=" + pId, "1 +")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testSaveActionWithIrrelevantReferenceObjects() {
    String programRuleAction =
        "{ 'programRule':{'id':'"
            + programRuleA.getUid()
            + "'}, "
            + "'programRuleActionType': 'HIDEPROGRAMSTAGE', "
            + "'dataElement':{'id':'"
            + dataElement.getUid()
            + "'}, "
            + "'programStage': {'id':'"
            + trackerProgramStageA.getUid()
            + "'} }";

    JsonErrorReport error =
        POST("/programRuleActions", programRuleAction)
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4058);
    assertNotNull(error);
    assertEquals(
        "Program Rule `ProgramRuleA` with Action Type `HIDEPROGRAMSTAGE` has irrelevant reference objects",
        error.getMessage());
  }

  @Test
  void testSaveScheduleEventRuleActionWithEventProgram() {
    String programRuleAction =
        "{ 'programRule':{'id':'"
            + programRuleB.getUid()
            + "'}, "
            + "'programRuleActionType': 'SCHEDULEEVENT', "
            + "'dataElement':{'id':'"
            + dataElement.getUid()
            + "'}, "
            + "'programStage': {'id':'"
            + eventProgramStage.getUid()
            + "'} }";

    JsonErrorReport error =
        POST("/programRuleActions", programRuleAction)
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4081);
    assertNotNull(error);
    assertEquals(
        "ProgramRule `%s` must be associated with a Tracker Program (a program with registration)"
            .formatted(programRuleB.getUid()),
        error.getMessage());
  }

  @Test
  void testSaveScheduleEventRuleActionWithProgramStageNotPartOfProgram() {
    String programRuleAction =
        "{ 'programRule':{'id':'"
            + programRuleA.getUid()
            + "'}, "
            + "'programRuleActionType': 'SCHEDULEEVENT', "
            + "'dataElement':{'id':'"
            + dataElement.getUid()
            + "'}, "
            + "'programStage': {'id':'"
            + trackerProgramStageB.getUid()
            + "'} }";

    JsonErrorReport error =
        POST("/programRuleActions", programRuleAction)
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4082);
    assertNotNull(error);
    assertEquals(
        "ProgramStage `%s` is not part of Program `%s`"
            .formatted(trackerProgramStageB.getUid(), trackerProgramA.getUid()),
        error.getMessage());
  }

  @Test
  void testSaveScheduleEventRuleActionWithNoProgramStage() {
    String programRuleAction =
        "{ 'programRule':{'id':'"
            + programRuleA.getUid()
            + "'}, "
            + "'programRuleActionType': 'SCHEDULEEVENT', "
            + "'dataElement':{'id':'"
            + dataElement.getUid()
            + "'}, "
            + "'programStage': {'id':'"
            + ""
            + "'} }";

    JsonErrorReport error =
        POST("/programRuleActions", programRuleAction)
            .content(HttpStatus.CONFLICT)
            .find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4038);
    assertNotNull(error);
    assertEquals(
        "ProgramStage cannot be null for program rule `%s`".formatted(programRuleA.getUid()),
        error.getMessage());
  }

  @Test
  void testSaveScheduleEventRuleActionAllRelevantParameters() {
    String programRuleAction =
        "{ 'programRule':{'id':'"
            + programRuleA.getUid()
            + "'}, "
            + "'programRuleActionType': 'SCHEDULEEVENT', "
            + "'dataElement':{'id':'"
            + dataElement.getUid()
            + "'}, "
            + "'programStage': {'id':'"
            + trackerProgramStageA.getUid()
            + "'} }";

    assertStatus(HttpStatus.CREATED, POST("/programRuleActions", programRuleAction));
  }

  private void createProgramRuleActions() {
    trackerProgramA = createProgram('A');
    manager.save(trackerProgramA);
    Program trackerProgramB = createProgram('B');
    manager.save(trackerProgramB);

    Program eventProgram = createProgram('C');
    eventProgram.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    manager.save(eventProgram);

    dataElement = createDataElement('A');
    manager.save(dataElement);

    trackerProgramStageA = createProgramStage('A', trackerProgramA);
    trackerProgramStageA.addDataElement(dataElement, 0);
    trackerProgramStageA.setProgram(trackerProgramA);
    manager.save(trackerProgramStageA);

    trackerProgramStageB = createProgramStage('B', trackerProgramB);
    trackerProgramStageB.addDataElement(dataElement, 0);
    trackerProgramStageB.setProgram(trackerProgramB);
    manager.save(trackerProgramStageB);

    eventProgramStage = createProgramStage('C', eventProgram);
    eventProgramStage.addDataElement(dataElement, 0);
    eventProgramStage.setProgram(trackerProgramB);
    manager.save(eventProgramStage);

    programRuleA = createProgramRule('A', trackerProgramA);
    manager.save(programRuleA);

    programRuleB = createProgramRule('B', eventProgram);
    manager.save(programRuleB);
  }
}
