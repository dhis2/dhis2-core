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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1031;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1042;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1043;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1046;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1047;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1050;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EventDateValidationHookTest extends DhisConvenienceTest {

  private static final String PROGRAM_WITH_REGISTRATION_ID = "ProgramWithRegistration";

  private static final String PROGRAM_WITHOUT_REGISTRATION_ID = "ProgramWithoutRegistration";

  private EventDateValidationHook hookToTest;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private ValidationErrorReporter reporter;

  @BeforeEach
  public void setUp() {
    hookToTest = new EventDateValidationHook();

    User user = makeUser("A");

    bundle = TrackerBundle.builder().user(user).preheat(preheat).build();

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID)))
        .thenReturn(getProgramWithRegistration());
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID)))
        .thenReturn(getProgramWithoutRegistration());

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new ValidationErrorReporter(idSchemes);
  }

  @Test
  void testEventIsValid() {
    // given
    Event event = new Event();
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID));
    event.setOccurredAt(now());
    event.setStatus(EventStatus.ACTIVE);

    TrackerBundle bundle =
        TrackerBundle.builder().user(getEditExpiredUser()).preheat(preheat).build();

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    assertFalse(reporter.hasErrors());
  }

  @Test
  void testEventIsNotValidWhenOccurredDateIsNotPresentAndProgramIsWithoutRegistration() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITHOUT_REGISTRATION_ID));

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1031, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsActive() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setStatus(EventStatus.ACTIVE);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1031, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsCompleted() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setStatus(EventStatus.COMPLETED);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1031, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenScheduledDateIsNotPresentAndEventIsSchedule() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setOccurredAt(Instant.now());
    event.setStatus(EventStatus.SCHEDULE);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1050, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenCompletedAtIsNotPresentAndEventIsCompleted() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setOccurredAt(now());
    event.setStatus(EventStatus.COMPLETED);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1042, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenCompletedAtIsTooSoonAndEventIsCompleted() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setOccurredAt(now());
    event.setCompletedAt(sevenDaysAgo());
    event.setStatus(EventStatus.COMPLETED);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1043, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenOccurredAtAndScheduledAtAreNotPresent() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setOccurredAt(null);
    event.setScheduledAt(null);
    event.setStatus(EventStatus.SKIPPED);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1046, EVENT, event.getUid());
  }

  @Test
  void testEventIsNotValidWhenDateBelongsToExpiredPeriod() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgram(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION_ID));
    event.setOccurredAt(sevenDaysAgo());
    event.setStatus(EventStatus.ACTIVE);

    // when
    this.hookToTest.validateEvent(reporter, bundle, event);

    // then
    hasTrackerError(reporter, E1047, EVENT, event.getUid());
  }

  private Program getProgramWithRegistration() {
    Program program = createProgram('A');
    program.setUid(PROGRAM_WITH_REGISTRATION_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setCompleteEventsExpiryDays(5);
    program.setExpiryDays(5);
    program.setExpiryPeriodType(new DailyPeriodType());
    return program;
  }

  private Program getProgramWithoutRegistration() {
    Program program = createProgram('A');
    program.setUid(PROGRAM_WITHOUT_REGISTRATION_ID);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    return program;
  }

  private User getEditExpiredUser() {
    User user = makeUser("A");
    UserRole userRole = createUserRole('A');
    userRole.setAuthorities(Sets.newHashSet(Authorities.F_EDIT_EXPIRED.getAuthority()));

    user.setUserRoles(Sets.newHashSet(userRole));

    return user;
  }

  private Instant now() {
    return Instant.now();
  }

  private Instant sevenDaysAgo() {
    return LocalDateTime.now().minus(7, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC);
  }
}
