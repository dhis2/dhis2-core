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
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1031;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1043;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1046;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1047;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1050;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1051;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class DateValidatorTest extends TestBase {

  private static final String PROGRAM_WITH_REGISTRATION_ID = "ProgramWithRegistration";

  private static final String PROGRAM_WITHOUT_REGISTRATION_ID = "ProgramWithoutRegistration";

  private DateValidator validator;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new DateValidator();

    bundle =
        TrackerBundle.builder().user(UserDetails.fromUser(makeUser("A"))).preheat(preheat).build();

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void testEventIsValid() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID)))
        .thenReturn(getProgramWithoutRegistration());
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID))
            .occurredAt(now())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testEventIsNotValidWhenOccurredDateIsNotPresentAndProgramIsWithoutRegistration() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID)))
        .thenReturn(getProgramWithoutRegistration());
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1031);
  }

  @Test
  void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsActive() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1031);
  }

  @Test
  void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsCompleted() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1031);
  }

  @Test
  void testEventIsNotValidWhenScheduledDateIsNotPresentAndEventIsSchedule() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(Instant.now())
            .status(EventStatus.SCHEDULE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1050);
  }

  @Test
  void shouldFailWhenCompletedAtIsPresentAndStatusIsNotCompleted() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(now())
            .completedAt(now())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1051);
  }

  @Test
  void testEventIsNotValidWhenCompletedAtIsTooSoonAndEventIsCompleted() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(now())
            .completedAt(sevenDaysAgo())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1043);
  }

  @Test
  void testEventIsNotValidWhenOccurredAtAndScheduledAtAreNotPresent() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(null)
            .scheduledAt(null)
            .status(EventStatus.SKIPPED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1046);
  }

  @Test
  void shouldFailValidationForEventWhenDateBelongsToExpiredPeriod() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(sevenDaysAgo())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1047);
  }

  @Test
  void shouldPassValidationForEventWhenDateBelongsToPastPeriodWithZeroExpiryDays() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(0));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(sevenDaysAgo())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassValidationForEventWhenDateBelongsPastEventPeriodButWithinExpiryDays() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(7));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .occurredAt(sevenDaysAgo())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassValidationForEventWhenScheduledDateBelongsToFuturePeriod() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration(5));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID))
            .scheduledAt(sevenDaysLater())
            .status(EventStatus.SCHEDULE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  private Program getProgramWithRegistration(int expiryDays) {
    Program program = createProgram('A');
    program.setUid(PROGRAM_WITH_REGISTRATION_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setCompleteEventsExpiryDays(5);
    program.setExpiryDays(expiryDays);
    program.setExpiryPeriodType(new DailyPeriodType());
    return program;
  }

  private Program getProgramWithoutRegistration() {
    Program program = createProgram('A');
    program.setUid(PROGRAM_WITHOUT_REGISTRATION_ID);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    return program;
  }

  private Instant now() {
    return Instant.now();
  }

  private Instant sevenDaysAgo() {
    return LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC);
  }

  private Instant sevenDaysLater() {
    return LocalDateTime.now().plusDays(7).toInstant(ZoneOffset.UTC);
  }
}
