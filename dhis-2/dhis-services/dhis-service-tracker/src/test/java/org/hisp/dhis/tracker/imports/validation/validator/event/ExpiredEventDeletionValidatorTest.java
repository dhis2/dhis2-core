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
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1043;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1047;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpiredEventDeletionValidatorTest extends TestBase {

  private ExpiredEventDeletionValidator validator;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    validator = new ExpiredEventDeletionValidator();

    bundle =
        TrackerBundle.builder().user(UserDetails.fromUser(makeUser("A"))).preheat(preheat).build();

    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
  }

  @Test
  void shouldFailWhenEventCompletionHasExpired() {
    UID uid = UID.generate();
    when(preheat.getEvent(uid))
        .thenReturn(completed(persistedEvent(programExpiringCompletedEventsAfter(5))));
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1043);
  }

  @Test
  void shouldPassWhenEventCompletionHasNotExpired() {
    UID uid = UID.generate();
    org.hisp.dhis.program.Event persisted =
        completed(persistedEvent(programExpiringCompletedEventsAfter(5)));
    persisted.setCompletedDate(Date.from(twoDaysAgo()));
    when(preheat.getEvent(uid)).thenReturn(persisted);
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventIsNotCompleted() {
    UID uid = UID.generate();
    when(preheat.getEvent(uid)).thenReturn(persistedEvent(programExpiringCompletedEventsAfter(5)));
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenProgramDoesNotExpireCompletedEvents() {
    UID uid = UID.generate();
    when(preheat.getEvent(uid))
        .thenReturn(completed(persistedEvent(programExpiringCompletedEventsAfter(0))));
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenCompletionHasExpiredButUserIsAuthorized() {
    bundle.setUser(userAuthorizedToEditExpired());
    Event event = Event.builder().event(UID.generate()).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenEventOccurredDateBelongsToExpiredPeriod() {
    UID uid = UID.generate();
    when(preheat.getEvent(uid)).thenReturn(persistedEvent(programExpiringAfter(1)));
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1047);
  }

  @Test
  void shouldFailWhenEventScheduledDateBelongsToExpiredPeriodAndOccurredDateIsNotSet() {
    UID uid = UID.generate();
    org.hisp.dhis.program.Event persisted = persistedEvent(programExpiringAfter(1));
    persisted.setOccurredDate(null);
    persisted.setScheduledDate(Date.from(sevenDaysAgo()));
    when(preheat.getEvent(uid)).thenReturn(persisted);
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1047);
  }

  @Test
  void shouldPassWhenEventDateBelongsToPeriodWithinExpiryDays() {
    UID uid = UID.generate();
    when(preheat.getEvent(uid)).thenReturn(persistedEvent(programExpiringAfter(14)));
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventHasNeitherOccurredNorScheduledDateEvenIfProgramExpiresItsPeriods() {
    UID uid = UID.generate();
    org.hisp.dhis.program.Event persisted = persistedEvent(programExpiringAfter(1));
    persisted.setOccurredDate(null);
    persisted.setScheduledDate(null);
    when(preheat.getEvent(uid)).thenReturn(persisted);
    Event event = Event.builder().event(uid).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @EnumSource(TrackerImportStrategy.class)
  void shouldOnlyRunOnDelete(TrackerImportStrategy strategy) {
    assertEquals(TrackerImportStrategy.DELETE == strategy, validator.needsToRun(strategy));
  }

  private UserDetails userAuthorizedToEditExpired() {
    UserDetails user = mock(UserDetails.class);
    when(user.isAuthorized(Authorities.F_EDIT_EXPIRED)).thenReturn(true);
    return user;
  }

  /** Returns an event which occurred seven days ago and is not completed. */
  private org.hisp.dhis.program.Event persistedEvent(Program program) {
    org.hisp.dhis.program.Event event = new org.hisp.dhis.program.Event();
    event.setProgramStage(createProgramStage('A', program));
    event.setStatus(EventStatus.ACTIVE);
    event.setOccurredDate(Date.from(sevenDaysAgo()));
    return event;
  }

  private org.hisp.dhis.program.Event completed(org.hisp.dhis.program.Event event) {
    event.setStatus(EventStatus.COMPLETED);
    event.setCompletedDate(Date.from(sevenDaysAgo()));
    return event;
  }

  /**
   * Returns a program locking its events the given number of days after their completion. It does
   * not expire the periods its events belong to.
   */
  private Program programExpiringCompletedEventsAfter(int completeEventsExpiryDays) {
    Program program = createProgram('A');
    program.setCompleteEventsExpiryDays(completeEventsExpiryDays);
    return program;
  }

  /**
   * Returns a program locking its events the given number of days after the end of the day they
   * belong to. It does not expire completed events.
   */
  private Program programExpiringAfter(int expiryDays) {
    Program program = createProgram('A');
    program.setExpiryDays(expiryDays);
    program.setExpiryPeriodType(new DailyPeriodType());
    return program;
  }

  private static Instant sevenDaysAgo() {
    return LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC);
  }

  private static Instant twoDaysAgo() {
    return LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC);
  }
}
