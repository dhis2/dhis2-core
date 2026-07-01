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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1031;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1043;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1046;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1047;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1050;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DateValidatorTest extends DhisConvenienceTest {

  private static final String PROGRAM_ID = "ProgramId";

  private DateValidator validator;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new DateValidator();

    bundle = TrackerBundle.builder().user(makeUser("A")).preheat(preheat).build();

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(getProgram(5));

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void shouldPassWhenEventIsValid() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
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
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(eventStatus)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1031);
  }

  @Test
  void shouldFailWhenScheduledDateIsNotPresentAndEventIsSchedule() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .status(EventStatus.SCHEDULE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1050);
  }

  @Test
  void shouldFailWhenCompletedAtIsTooSoonAndEventIsCompleted() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
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
    String uid = CodeGenerator.generateUid();
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
    String uid = CodeGenerator.generateUid();
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
    String uid = CodeGenerator.generateUid();
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
    String uid = CodeGenerator.generateUid();
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
    TrackerBundle bundle =
        TrackerBundle.builder().user(getEditExpiredUser()).preheat(preheat).build();
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .occurredAt(now())
            .completedAt(sevenDaysAgo())
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenOccurredAtAndScheduledAtAreNotPresent() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
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
    Event event = expiredEvent();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1047);
  }

  @Test
  void shouldPassWhenEventDateBelongsToExpiredPeriodIfUserIsAuthorized() {
    TrackerBundle bundle =
        TrackerBundle.builder().user(getEditExpiredUser()).preheat(preheat).build();

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
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .scheduledAt(sevenDaysLater())
            .status(EventStatus.SCHEDULE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  private Event expiredEvent() {
    return Event.builder()
        .event(CodeGenerator.generateUid())
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
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setCompleteEventsExpiryDays(5);
    program.setExpiryDays(expiryDays);
    program.setExpiryPeriodType(new DailyPeriodType());
    return program;
  }

  private User getEditExpiredUser() {
    User user = makeUser("A");
    UserRole userRole = createUserRole('A');
    userRole.setAuthorities(Sets.newHashSet(Authorities.F_EDIT_EXPIRED.name()));
    user.setUserRoles(Sets.newHashSet(userRole));
    return user;
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
