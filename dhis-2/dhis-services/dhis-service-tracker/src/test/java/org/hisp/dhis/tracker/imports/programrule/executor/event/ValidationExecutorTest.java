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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.warning;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1300;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ValidationExecutorTest extends TestBase {
  private static final UID RULE_UID = UID.of("TvctPPhpD8u");

  private static final String CONTENT = "SHOW ERROR DATA";

  private static final String EVALUATED_DATA = "4.0";

  private static final UID ACTIVE_EVENT_UID = UID.generate();

  private static final UID COMPLETED_EVENT_UID = UID.generate();

  private static final String PROGRAM_STAGE_ID = "ProgramStageId";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

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

  private ProgramStage programStage;

  private ProgramStage anotherProgramStage;

  @BeforeEach
  void setUpTest() {
    programStage = createProgramStage('A', 0);
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    DataElement dataElementA = createDataElement('A');
    dataElementA.setUid(DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(programStage, dataElementA, 0);
    programStage.setProgramStageDataElements(Set.of(programStageDataElementA));
    anotherProgramStage = createProgramStage('B', 0);
    anotherProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    DataElement dataElementB = createDataElement('B');
    dataElementB.setUid(ANOTHER_DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementB =
        createProgramStageDataElement(anotherProgramStage, dataElementB, 0);
    anotherProgramStage.setProgramStageDataElements(Set.of(programStageDataElementB));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID)))
        .thenReturn(programStage);

    bundle = TrackerBundle.builder().build();
    bundle.setEvents(getEvents());
    bundle.setPreheat(preheat);
  }

  @Test
  void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForActiveEvent() {
    Optional<ProgramRuleIssue> error = showErrorExecutor.executeRuleAction(bundle, activeEvent());

    assertTrue(error.isPresent());
    assertEquals(error(RULE_UID, E1300, validationMessage(ERROR)), error.get());
  }

  @Test
  void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForCompletedEvent() {
    Optional<ProgramRuleIssue> error =
        showErrorExecutor.executeRuleAction(bundle, completedEvent());

    assertTrue(error.isPresent());
    assertEquals(error(RULE_UID, E1300, validationMessage(ERROR)), error.get());
  }

  @Test
  void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForActiveEvent() {
    Optional<ProgramRuleIssue> warning =
        showWarningExecutor.executeRuleAction(bundle, activeEvent());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, validationMessage(WARNING)), warning.get());
  }

  @Test
  void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForCompletedEvent() {
    Optional<ProgramRuleIssue> warning =
        showWarningExecutor.executeRuleAction(bundle, completedEvent());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, validationMessage(WARNING)), warning.get());
  }

  @Test
  void shouldNotReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForActiveEvent() {
    Optional<ProgramRuleIssue> error =
        errorOnCompleteExecutor.executeRuleAction(bundle, activeEvent());

    assertFalse(error.isPresent());
  }

  @Test
  void shouldReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEvent() {
    Optional<ProgramRuleIssue> error =
        errorOnCompleteExecutor.executeRuleAction(bundle, completedEvent());

    assertTrue(error.isPresent());
    assertEquals(error(RULE_UID, E1300, validationMessage(ERROR)), error.get());
  }

  @Test
  void shouldNotReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForActiveEvent() {
    Optional<ProgramRuleIssue> warning =
        warningOnCompleteExecutor.executeRuleAction(bundle, activeEvent());

    assertFalse(warning.isPresent());
  }

  @Test
  void shouldReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEvent() {
    Optional<ProgramRuleIssue> warning =
        warningOnCompleteExecutor.executeRuleAction(bundle, completedEvent());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, validationMessage(WARNING)), warning.get());
  }

  private ValidationEffect getValidationRuleAction(
      IssueType issueType, ValidationAction actionType) {
    return new ValidationEffect(
        actionType, RULE_UID, EVALUATED_DATA, null, issueType.name() + CONTENT);
  }

  private List<Event> getEvents() {
    return List.of(activeEvent(), completedEvent());
  }

  private Event activeEvent() {
    return TrackerEvent.builder()
        .event(ACTIVE_EVENT_UID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
        .build();
  }

  private Event completedEvent() {
    return TrackerEvent.builder()
        .event(COMPLETED_EVENT_UID)
        .status(EventStatus.COMPLETED)
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_ID))
        .build();
  }

  private String validationMessage(IssueType issueType) {
    return issueType.name() + CONTENT + " " + EVALUATED_DATA;
  }
}
