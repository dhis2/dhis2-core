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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.controller.tracker.JsonImportReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerScheduleEventControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String INVALID_DATE = "2025-22-22";
  private static final String DATE_TODAY = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

  private User importUser;
  private User readOnlyUser;
  private Enrollment enrollment;

  private DataElement dataElementA;
  private Program program;
  private ProgramStage programStageA;

  @BeforeAll
  void setUp() {
    importUser = makeUser("o", List.of("ALL"));
    readOnlyUser = makeUser("p");
    manager.save(importUser, false);
    manager.save(readOnlyUser, false);

    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);

    dataElementA = createDataElement('A');
    manager.save(dataElementA);

    importUser.addOrganisationUnit(orgUnit);
    readOnlyUser.addOrganisationUnit(orgUnit);
    manager.update(importUser);
    manager.save(readOnlyUser);

    program = createProgram('A', new HashSet<>(), orgUnit);
    manager.save(program, false);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);

    programStageA = createProgramStage('A', program);
    manager.save(programStageA);
    programStageA
        .getSharing()
        .addUserAccess(new UserAccess(readOnlyUser, AccessStringHelper.DATA_WRITE));
    manager.update(programStageA);

    ProgramStage programStageB = createProgramStage('B', program);
    manager.save(programStageB);

    ProgramStage programStageReadOnly = createProgramStage('C', program);
    manager.save(programStageReadOnly, false);

    programStageReadOnly
        .getSharing()
        .addUserAccess(new UserAccess(readOnlyUser, AccessStringHelper.READ_ONLY));

    manager.update(programStageReadOnly);

    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
    te.setTrackedEntityType(trackedEntityType);
    manager.save(te);

    enrollment = createEnrollment(te, program, orgUnit);
    manager.update(enrollment);

    createProgramRuleWithAction(
        'R', "V{current_date}!=V{event_date}", "V{current_date}", programStageB);
    createProgramRuleWithAction('S', "V{current_date}==V{event_date}", INVALID_DATE, programStageB);
    createProgramRuleWithAction(
        'S', "'2025-12-12'==V{event_date}", "V{current_date}", programStageReadOnly);
  }

  @Test
  void shouldSuccessfullyScheduleEvent() {
    injectSecurityContextUser(importUser);

    JsonImportReport importReport =
        POST("/tracker?async=false&reportMode=FULL", buildEventJson("2025-11-11"))
            .content(HttpStatus.OK)
            .as(JsonImportReport.class);

    assertEquals(
        "E1320",
        importReport
            .getObject("validationReport.warningReports[0]")
            .getString("warningCode")
            .string());
    assertEquals(2, importReport.getStats().getCreated());
    assertEquals(0, importReport.getStats().getIgnored());
  }

  @Test
  void shouldReturnWarningScheduleDateIsInValid() {
    injectSecurityContextUser(importUser);

    JsonImportReport importReport =
        POST("/tracker?async=false&reportMode=FULL", buildEventJson(DATE_TODAY))
            .content(HttpStatus.OK)
            .as(JsonImportReport.class);

    assertEquals(
        "E1319",
        importReport
            .getObject("validationReport.warningReports[0]")
            .getString("warningCode")
            .string());
    assertEquals(1, importReport.getStats().getCreated());
    assertEquals(0, importReport.getStats().getIgnored());
  }

  @Test
  void shouldReturnWarningUserHasNoWriteAccess() {
    injectSecurityContextUser(readOnlyUser);

    JsonImportReport importReport =
        POST("/tracker?async=false&reportMode=FULL", buildEventJson("2025-12-12"))
            .content(HttpStatus.OK)
            .as(JsonImportReport.class);

    assertEquals(
        "E1321",
        importReport
            .getObject("validationReport.warningReports[0]")
            .getString("warningCode")
            .string());
    assertEquals(1, importReport.getStats().getCreated());
    assertEquals(0, importReport.getStats().getIgnored());
  }

  private Enrollment createEnrollment(TrackedEntity te, Program program, OrganisationUnit orgUnit) {
    Enrollment enrollmentA = new Enrollment(program, te, orgUnit);
    enrollmentA.setAutoFields();
    enrollmentA.setEnrollmentDate(new Date());
    enrollmentA.setOccurredDate(new Date());
    enrollmentA.setStatus(EnrollmentStatus.COMPLETED);
    enrollmentA.setFollowup(true);
    manager.save(enrollmentA, false);
    te.setEnrollments(Set.of(enrollmentA));
    manager.save(te, false);
    return enrollmentA;
  }

  private void createProgramRuleWithAction(
      char ch, String ruleCondition, String actionData, ProgramStage programStage) {
    ProgramRule programRule = createProgramRule(ch, program);
    programRule.setCondition(ruleCondition);
    programRule.setProgramStage(programStageA);
    manager.save(programRule, false);

    ProgramRuleAction action = createProgramRuleAction(ch, programRule);
    action.setProgramRule(programRule);
    action.setProgramStage(programStage);
    action.setProgramRuleActionType(ProgramRuleActionType.SCHEDULEEVENT);
    action.setDataElement(dataElementA);
    action.setData(actionData);
    manager.save(action, false);

    programRule.getProgramRuleActions().add(action);
    manager.update(programRule);
  }

  private String buildEventJson(String occurredAt) {
    return """
            {"events": [
                {
                    "enrollment": "%s",
                    "occurredAt": "%s",
                    "orgUnit": "%s",
                    "program": "%s",
                    "programStage": "%s",
                    "status": "COMPLETED",
                    "trackedEntity": "%s"
                }
            ]}
            """
        .formatted(
            enrollment.getUid(),
            occurredAt,
            enrollment.getOrganisationUnit().getUid(),
            enrollment.getProgram().getUid(),
            programStageA.getUid(),
            enrollment.getTrackedEntity().getUid());
  }
}
