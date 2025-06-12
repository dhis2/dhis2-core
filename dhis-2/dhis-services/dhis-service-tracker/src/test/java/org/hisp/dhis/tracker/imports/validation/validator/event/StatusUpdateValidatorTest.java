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

import static org.hisp.dhis.event.EventStatus.ACTIVE;
import static org.hisp.dhis.event.EventStatus.COMPLETED;
import static org.hisp.dhis.event.EventStatus.OVERDUE;
import static org.hisp.dhis.event.EventStatus.SCHEDULE;
import static org.hisp.dhis.event.EventStatus.SKIPPED;
import static org.hisp.dhis.event.EventStatus.VISITED;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class StatusUpdateValidatorTest {

  private StatusUpdateValidator validator;

  @Mock TrackerPreheat preheat;

  private static final UID EVENT_UID = UID.generate();

  @Mock private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new StatusUpdateValidator();

    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @ParameterizedTest
  @MethodSource("validTransitions")
  void shouldPassValidationWhenGoingFromStatusToStatus(
      EventStatus fromStatus, EventStatus toStatus) {
    org.hisp.dhis.program.Event savedEvent = new org.hisp.dhis.program.Event();
    savedEvent.setUid(EVENT_UID.getValue());
    savedEvent.setStatus(fromStatus);
    when(preheat.getEvent(EVENT_UID)).thenReturn(savedEvent);

    Event event = TrackerEvent.builder().event(EVENT_UID).status(toStatus).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @MethodSource("invalidTransitions")
  void shouldFailValidationWhenGoingFromStatusToStatus(
      EventStatus fromStatus, EventStatus toStatus) {
    org.hisp.dhis.program.Event savedEvent = new org.hisp.dhis.program.Event();
    savedEvent.setUid(EVENT_UID.getValue());
    savedEvent.setStatus(fromStatus);
    when(preheat.getEvent(EVENT_UID)).thenReturn(savedEvent);

    Event event = TrackerEvent.builder().event(EVENT_UID).status(toStatus).build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1316);
  }

  private static Stream<Arguments> validTransitions() {
    return Stream.of(
        Arguments.of(ACTIVE, ACTIVE),
        Arguments.of(ACTIVE, COMPLETED),
        Arguments.of(ACTIVE, VISITED),
        Arguments.of(VISITED, VISITED),
        Arguments.of(VISITED, ACTIVE),
        Arguments.of(VISITED, COMPLETED),
        Arguments.of(COMPLETED, VISITED),
        Arguments.of(COMPLETED, ACTIVE),
        Arguments.of(COMPLETED, COMPLETED),
        Arguments.of(SCHEDULE, ACTIVE),
        Arguments.of(SCHEDULE, COMPLETED),
        Arguments.of(SCHEDULE, VISITED),
        Arguments.of(SCHEDULE, SCHEDULE),
        Arguments.of(SCHEDULE, SKIPPED),
        Arguments.of(SKIPPED, ACTIVE),
        Arguments.of(SKIPPED, COMPLETED),
        Arguments.of(SKIPPED, VISITED),
        Arguments.of(SKIPPED, SCHEDULE),
        Arguments.of(SKIPPED, SKIPPED),
        Arguments.of(OVERDUE, ACTIVE),
        Arguments.of(OVERDUE, COMPLETED),
        Arguments.of(OVERDUE, VISITED),
        Arguments.of(OVERDUE, SCHEDULE),
        Arguments.of(OVERDUE, SKIPPED),
        Arguments.of(SCHEDULE, OVERDUE),
        Arguments.of(SKIPPED, OVERDUE),
        Arguments.of(OVERDUE, OVERDUE));
  }

  private static Stream<Arguments> invalidTransitions() {
    return Stream.of(
        Arguments.of(ACTIVE, OVERDUE),
        Arguments.of(ACTIVE, SKIPPED),
        Arguments.of(ACTIVE, SCHEDULE),
        Arguments.of(VISITED, OVERDUE),
        Arguments.of(VISITED, SKIPPED),
        Arguments.of(VISITED, SCHEDULE),
        Arguments.of(COMPLETED, OVERDUE),
        Arguments.of(COMPLETED, SKIPPED),
        Arguments.of(COMPLETED, SCHEDULE));
  }
}
