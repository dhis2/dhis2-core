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
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1030;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1032;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1082;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
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
class ExistenceValidatorTest {
  private static final UID SOFT_DELETED_EVENT_UID = UID.generate();

  private static final UID EVENT_UID = UID.generate();

  private static final UID NOT_PRESENT_EVENT_UID = UID.generate();

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private ExistenceValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new ExistenceValidator();
  }

  @Test
  void verifyEventValidationSuccessWhenIsCreateAndEventIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(NOT_PRESENT_EVENT_UID)
            .build();
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEventValidationSuccessWhenEventIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(NOT_PRESENT_EVENT_UID)
            .build();
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Event.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEventValidationSuccessWhenIsUpdate() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder().event(EVENT_UID).build();
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(getEvent());
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Event.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEventValidationFailsWhenIsSoftDeleted() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(SOFT_DELETED_EVENT_UID)
            .build();
    when(preheat.getTrackerEvent(SOFT_DELETED_EVENT_UID)).thenReturn(getSoftDeletedEvent());
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.Event.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1082);
  }

  @Test
  void verifyEventValidationFailsWhenIsCreateAndEventIsAlreadyPresent() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder().event(EVENT_UID).build();
    when(preheat.getTrackerEvent(EVENT_UID)).thenReturn(getEvent());
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1030);
  }

  @Test
  void verifyEventValidationFailsWhenIsUpdateAndEventIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(NOT_PRESENT_EVENT_UID)
            .build();
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1032);
  }

  private TrackerEvent getSoftDeletedEvent() {
    TrackerEvent event = new TrackerEvent();
    event.setUid(SOFT_DELETED_EVENT_UID.getValue());
    event.setDeleted(true);
    return event;
  }

  private TrackerEvent getEvent() {
    TrackerEvent event = new TrackerEvent();
    event.setUid(EVENT_UID.getValue());
    event.setDeleted(false);
    return event;
  }
}
