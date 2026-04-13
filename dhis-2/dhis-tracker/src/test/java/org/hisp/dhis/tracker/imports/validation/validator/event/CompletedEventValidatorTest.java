/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.security.Authorities.F_UNCOMPLETE_EVENT;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1083;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompletedEventValidatorTest {
  private static final UID EVENT_UID = UID.generate();

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private UserDetails user;

  private CompletedEventValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getUser()).thenReturn(user);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new CompletedEventValidator();
  }

  @Test
  void shouldPassWhenUserHasAuthorityToReopenCompletedEvent() {
    org.hisp.dhis.tracker.model.TrackerEvent databaseEvent =
        mock(org.hisp.dhis.tracker.model.TrackerEvent.class);
    when(databaseEvent.getStatus()).thenReturn(EventStatus.COMPLETED);
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(databaseEvent);
    when(user.isAuthorized(F_UNCOMPLETE_EVENT)).thenReturn(true);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventNotCompleted() {
    org.hisp.dhis.tracker.model.TrackerEvent databaseEvent =
        mock(org.hisp.dhis.tracker.model.TrackerEvent.class);
    when(databaseEvent.getStatus()).thenReturn(EventStatus.ACTIVE);
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(databaseEvent);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenPayloadEventStatusIsNull() {
    org.hisp.dhis.tracker.model.TrackerEvent databaseEvent =
        mock(org.hisp.dhis.tracker.model.TrackerEvent.class);
    when(databaseEvent.getStatus()).thenReturn(EventStatus.COMPLETED);
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(databaseEvent);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .status(null)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenPayloadEventStatusSameAsDatabaseEventStatus() {
    org.hisp.dhis.tracker.model.TrackerEvent databaseEvent =
        mock(org.hisp.dhis.tracker.model.TrackerEvent.class);
    when(databaseEvent.getStatus()).thenReturn(EventStatus.COMPLETED);
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(databaseEvent);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .status(EventStatus.COMPLETED)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenUserNotAllowedToReopenCompletedEvent() {
    org.hisp.dhis.tracker.model.TrackerEvent databaseEvent =
        mock(org.hisp.dhis.tracker.model.TrackerEvent.class);
    when(databaseEvent.getStatus()).thenReturn(EventStatus.COMPLETED);
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(databaseEvent);
    when(user.isAuthorized(F_UNCOMPLETE_EVENT)).thenReturn(false);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1083);
  }

  @Test
  void shouldPassWhenEventIsSingleEvent() {
    org.hisp.dhis.tracker.imports.domain.SingleEvent event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(EVENT_UID)
            .status(EventStatus.ACTIVE)
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }
}
