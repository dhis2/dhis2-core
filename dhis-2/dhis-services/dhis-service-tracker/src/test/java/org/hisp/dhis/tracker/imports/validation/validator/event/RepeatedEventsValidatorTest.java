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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1039;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E9999;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Error;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class RepeatedEventsValidatorTest extends TestBase {

  private static final String NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION =
      "NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION";

  private static final String REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION =
      "REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION";

  private static final String NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION =
      "NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION";

  private static final UID ENROLLMENT_A = UID.generate();

  private static final UID ENROLLMENT_B = UID.generate();

  private RepeatedEventsValidator validator;

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new RepeatedEventsValidator();

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void testSingleEventIsPassingValidation() {
    when(preheat.getProgramStage(
            MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(notRepeatebleProgramStageWithRegistration());
    List<Event> events = List.of(notRepeatableEvent());
    bundle.setEvents(events);
    events.forEach(e -> bundle.setStrategy(e, TrackerImportStrategy.CREATE_AND_UPDATE));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testOneEventInNotRepeatableProgramStageAndOneAlreadyOnDBAreNotPassingValidation() {
    when(preheat.getProgramStage(
            MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(notRepeatebleProgramStageWithRegistration());
    // given
    Event event = notRepeatableEvent();
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(event.getEnrollment().getValue());

    // when
    bundle.setStrategy(event, TrackerImportStrategy.CREATE);

    when(preheat.hasProgramStageWithEvents(
            event.getProgramStage(), event.getEnrollment().getValue()))
        .thenReturn(true);
    bundle.setEvents(List.of(event));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    // then
    assertHasError(
        reporter,
        event,
        E1039,
        "ProgramStage: `"
            + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION
            + "` is not repeatable and an Event already exists.");
  }

  @Test
  void testTwoEventInNotRepeatableProgramStageAreNotPassingValidation() {
    when(preheat.getProgramStage(
            MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(notRepeatebleProgramStageWithRegistration());
    List<Event> events = List.of(notRepeatableEvent(), notRepeatableEvent());
    bundle.setEvents(events);
    events.forEach(e -> bundle.setStrategy(e, TrackerImportStrategy.CREATE_AND_UPDATE));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    assertAll(
        () ->
            assertHasError(
                reporter,
                events.get(0),
                E1039,
                "ProgramStage: `"
                    + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION
                    + "` is not repeatable and an Event already exists."),
        () ->
            assertHasError(
                reporter,
                events.get(1),
                E1039,
                "ProgramStage: `"
                    + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION
                    + "` is not repeatable and an Event already exists."));
  }

  @Test
  void testTwoEventInRepeatableProgramStageArePassingValidation() {
    when(preheat.getProgramStage(
            MetadataIdentifier.ofUid(REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(repeatebleProgramStageWithRegistration());
    List<Event> events = List.of(repeatableEvent(), repeatableEvent());
    bundle.setEvents(events);
    events.forEach(e -> bundle.setStrategy(e, TrackerImportStrategy.CREATE_AND_UPDATE));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testTwoEventsInNotRepeatableProgramStageWhenOneIsInvalidArePassingValidation() {
    when(preheat.getProgramStage(
            MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(notRepeatebleProgramStageWithRegistration());
    Event invalidEvent = notRepeatableEvent();
    List<Event> events = List.of(invalidEvent, notRepeatableEvent());
    bundle.setEvents(events);
    events.forEach(e -> bundle.setStrategy(e, TrackerImportStrategy.CREATE_AND_UPDATE));
    reporter.addError(
        new Error("", E9999, invalidEvent.getTrackerType(), invalidEvent.getUid(), List.of()));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    assertFalse(reporter.hasErrorReport(e -> E1039 == e.getErrorCode()));
  }

  @Test
  void testTwoEventsInNotRepeatableProgramStageButInDifferentEnrollmentsArePassingValidation() {
    when(preheat.getProgramStage(
            MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION)))
        .thenReturn(notRepeatebleProgramStageWithRegistration());
    Event eventEnrollmentA = notRepeatableEvent();
    Event eventEnrollmentB = notRepeatableEvent();
    eventEnrollmentB.setEnrollment(ENROLLMENT_B);
    List<Event> events = List.of(eventEnrollmentA, eventEnrollmentB);
    bundle.setEvents(events);
    events.forEach(e -> bundle.setStrategy(e, TrackerImportStrategy.CREATE_AND_UPDATE));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testTwoSingleEventsInSameProgramStageArePassingValidation() {
    Event eventProgramA = singleEvent();
    Event eventProgramB = singleEvent();
    List<Event> events = List.of(eventProgramA, eventProgramB);
    bundle.setEvents(events);
    events.forEach(e -> bundle.setStrategy(e, TrackerImportStrategy.CREATE_AND_UPDATE));

    validator.validate(reporter, bundle, bundle.getTrackerEvents());

    assertIsEmpty(reporter.getErrors());
  }

  private ProgramStage notRepeatebleProgramStageWithRegistration() {
    ProgramStage programStage = createProgramStage('A', 1, false);
    programStage.setUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION);
    programStage.setProgram(programWithRegistration());
    return programStage;
  }

  private ProgramStage repeatebleProgramStageWithRegistration() {
    ProgramStage programStage = createProgramStage('A', 1, true);
    programStage.setUid(REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION);
    programStage.setProgram(programWithRegistration());
    return programStage;
  }

  private Program programWithRegistration() {
    Program program = createProgram('A');
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    return program;
  }

  private SingleEvent singleEvent() {
    return SingleEvent.builder()
        .event(UID.generate())
        .enrollment(ENROLLMENT_B)
        .programStage(MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION))
        .build();
  }

  private Event notRepeatableEvent() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .enrollment(ENROLLMENT_A)
        .programStage(MetadataIdentifier.ofUid(NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION))
        .build();
  }

  private Event repeatableEvent() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .enrollment(ENROLLMENT_A)
        .programStage(MetadataIdentifier.ofUid(REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION))
        .build();
  }
}
