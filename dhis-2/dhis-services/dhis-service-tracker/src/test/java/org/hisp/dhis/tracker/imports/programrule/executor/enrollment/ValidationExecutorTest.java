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
package org.hisp.dhis.tracker.imports.programrule.executor.enrollment;

import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.program.EnrollmentStatus.COMPLETED;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.warning;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1300;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.IssueType;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.engine.ValidationAction;
import org.hisp.dhis.tracker.imports.programrule.engine.ValidationEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationExecutorTest extends TestBase {
  private static final UID RULE_UID = UID.of("TvctPPhpD8u");

  private static final String CONTENT = "SHOW ERROR DATA";

  private static final String EVALUATED_DATA = "4.0";

  private static final UID ACTIVE_ENROLLMENT_ID = UID.generate();

  private static final UID COMPLETED_ENROLLMENT_ID = UID.generate();

  private final ShowWarningOnCompleteExecutor warningOnCompleteExecutor =
      new ShowWarningOnCompleteExecutor(
          getValidationRuleAction(WARNING, ValidationAction.SHOW_WARNING_ON_COMPLETE));

  private final ShowErrorOnCompleteExecutor errorOnCompleteExecutor =
      new ShowErrorOnCompleteExecutor(
          getValidationRuleAction(ERROR, ValidationAction.SHOW_ERROR_ON_COMPLETE));

  private final ShowErrorExecutor showErrorExecutor =
      new ShowErrorExecutor(getValidationRuleAction(ERROR, ValidationAction.SHOW_ERROR));

  private final ShowWarningExecutor showWarningExecutor =
      new ShowWarningExecutor(getValidationRuleAction(WARNING, ValidationAction.SHOW_WARNING));

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUpTest() {
    bundle = TrackerBundle.builder().build();
    bundle.setEnrollments(getEnrollments());
    bundle.setPreheat(preheat);
  }

  @Test
  void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForActiveEnrollment() {
    Optional<ProgramRuleIssue> error =
        showErrorExecutor.executeRuleAction(bundle, activeEnrollment());

    assertTrue(error.isPresent());
    assertEquals(error(RULE_UID, E1300, validationMessage(ERROR)), error.get());
  }

  @Test
  void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForCompletedEnrollment() {
    Optional<ProgramRuleIssue> error =
        showErrorExecutor.executeRuleAction(bundle, completedEnrollment());

    assertTrue(error.isPresent());
    assertEquals(error(RULE_UID, E1300, validationMessage(ERROR)), error.get());
  }

  @Test
  void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForActiveEnrollment() {
    Optional<ProgramRuleIssue> warning =
        showWarningExecutor.executeRuleAction(bundle, activeEnrollment());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, validationMessage(WARNING)), warning.get());
  }

  @Test
  void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForCompletedEnrollment() {
    Optional<ProgramRuleIssue> warning =
        showWarningExecutor.executeRuleAction(bundle, completedEnrollment());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, validationMessage(WARNING)), warning.get());
  }

  @Test
  void shouldNotReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForActiveEnrollment() {
    Optional<ProgramRuleIssue> error =
        errorOnCompleteExecutor.executeRuleAction(bundle, activeEnrollment());

    assertFalse(error.isPresent());
  }

  @Test
  void shouldReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEnrollment() {
    Optional<ProgramRuleIssue> error =
        errorOnCompleteExecutor.executeRuleAction(bundle, completedEnrollment());

    assertTrue(error.isPresent());
    assertEquals(error(RULE_UID, E1300, validationMessage(ERROR)), error.get());
  }

  @Test
  void shouldNotReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForActiveEnrollment() {
    Optional<ProgramRuleIssue> warning =
        warningOnCompleteExecutor.executeRuleAction(bundle, activeEnrollment());

    assertFalse(warning.isPresent());
  }

  @Test
  void shouldReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEnrollment() {
    Optional<ProgramRuleIssue> warning =
        warningOnCompleteExecutor.executeRuleAction(bundle, completedEnrollment());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, validationMessage(WARNING)), warning.get());
  }

  private List<Enrollment> getEnrollments() {
    return List.of(activeEnrollment(), completedEnrollment());
  }

  private Enrollment activeEnrollment() {
    return Enrollment.builder().enrollment(ACTIVE_ENROLLMENT_ID).status(ACTIVE).build();
  }

  private Enrollment completedEnrollment() {
    return Enrollment.builder().enrollment(COMPLETED_ENROLLMENT_ID).status(COMPLETED).build();
  }

  private ValidationEffect getValidationRuleAction(
      IssueType issueType, ValidationAction actionType) {
    return new ValidationEffect(
        actionType, RULE_UID, EVALUATED_DATA, null, issueType.name() + CONTENT);
  }

  private String validationMessage(IssueType issueType) {
    return issueType.name() + CONTENT + " " + EVALUATED_DATA;
  }
}
