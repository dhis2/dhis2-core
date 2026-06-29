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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class DateValidatorTest extends TestBase {

  private static final String PROGRAM_ID = "ProgramId";

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
  void shouldPassWhenEventIsValid() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(0));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @EnumSource(
      value = EventStatus.class,
      mode = Mode.INCLUDE,
      names = {"ACTIVE", "COMPLETED"})
  void shouldFailWhenMandatoryOccurredAtIsNotPresent(EventStatus eventStatus) {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(0));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(eventStatus)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1031);
  }

  @Test
  void shouldFailWhenScheduledDateIsNotPresentAndEventIsSchedule() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .status(EventStatus.SCHEDULE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1050);
  }

  @Test
  void shouldFailWhenCompletedAtIsPresentAndStatusIsNotCompleted() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .completedAt(now())
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1051);
  }

  @Test
  void shouldFailWhenCompletedAtIsTooSoonAndEventIsCompleted() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .completedAt(sevenDaysAgo())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1043);
  }

  @Test
  void shouldFailWhenCompletionHasExpiredEvenIfPayloadHasNoCompletedAt() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    UID uid = UID.generate();
    when(preheat.getEvent(uid)).thenReturn(completedEvent(sevenDaysAgo()));
    Event event =
        Event.builder()
            .event(uid)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1043);
  }

  @Test
  void shouldFailWhenCompletionHasExpiredEvenIfPayloadHasNoStatusNorCompletedAt() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    UID uid = UID.generate();
    when(preheat.getEvent(uid)).thenReturn(completedEvent(sevenDaysAgo()));
    Event event =
        Event.builder()
            .event(uid)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1043);
  }

  @Test
  void shouldUsePersistedCompletionDateAndIgnorePayloadCompletedAtWhenAlreadyCompleted() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    UID uid = UID.generate();
    // event was completed in the database long ago (expired)...
    when(preheat.getEvent(uid)).thenReturn(completedEvent(sevenDaysAgo()));
    Event event =
        Event.builder()
            .event(uid)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            // ...so a fresh completedAt in the payload must not reset the expiry clock
            .completedAt(now())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1043);
  }

  @Test
  void shouldPassWhenPersistedCompletionIsWithinExpiryDays() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    UID uid = UID.generate();
    when(preheat.getEvent(uid)).thenReturn(completedEvent(twoDaysAgo()));
    Event event =
        Event.builder()
            .event(uid)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenPersistedCompletionHasExpiredButUserIsAuthorized() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    UID uid = UID.generate();
    UserDetails user = mock(UserDetails.class);
    when(user.isAuthorized(Authorities.F_EDIT_EXPIRED.name())).thenReturn(true);
    bundle.setUser(user);
    Event event =
        Event.builder()
            .event(uid)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenOccurredAtAndScheduledAtAreNotPresent() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(null)
            .scheduledAt(null)
            .status(EventStatus.SKIPPED)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1046);
  }

  @Test
  void shouldFailWhenEventDateBelongsToExpiredPeriod() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    Event event = expiredEvent();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1047);
  }

  @Test
  void shouldPassWhenEventDateBelongsToExpiredPeriodIfUserIsAuthorized() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    UserDetails user = mock(UserDetails.class);
    when(user.isAuthorized(Authorities.F_EDIT_EXPIRED.name())).thenReturn(true);
    bundle.setUser(user);

    validator.validate(reporter, bundle, expiredEvent());

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventDateBelongsToPastPeriodWithZeroExpiryDays() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(0));

    validator.validate(reporter, bundle, expiredEvent());

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventDateBelongsPastEventPeriodButWithinExpiryDays() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(14));

    validator.validate(reporter, bundle, expiredEvent());

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenScheduledDateBelongsToFuturePeriod() {
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));
    Event event =
        Event.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .scheduledAt(sevenDaysLater())
            .status(EventStatus.SCHEDULE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  private Event expiredEvent() {
    return Event.builder()
        .event(UID.generate())
        .program(MetadataIdentifier.ofUid(PROGRAM_ID))
        .occurredAt(sevenDaysAgo())
        .status(EventStatus.ACTIVE)
        .build();
  }

  private static org.hisp.dhis.program.Event completedEvent(Instant completedDate) {
    org.hisp.dhis.program.Event event = new org.hisp.dhis.program.Event();
    event.setStatus(EventStatus.COMPLETED);
    event.setCompletedDate(Date.from(completedDate));
    return event;
  }

  private Program getProgram(int expiryDays) {
    Program program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setCompleteEventsExpiryDays(5);
    program.setExpiryDays(expiryDays);
    program.setExpiryPeriodType(new DailyPeriodType());
    return program;
  }

  private Instant now() {
    return Instant.now();
  }

  private static Instant sevenDaysAgo() {
    return LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC);
  }

  private static Instant twoDaysAgo() {
    return LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC);
  }

  private Instant sevenDaysLater() {
    return LocalDateTime.now().plusDays(7).toInstant(ZoneOffset.UTC);
  }
}
