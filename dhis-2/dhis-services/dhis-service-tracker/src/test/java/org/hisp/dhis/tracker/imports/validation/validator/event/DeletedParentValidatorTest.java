/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1132;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletedParentValidatorTest {
  private static final String EVENT_UID = CodeGenerator.generateUid();

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private DeletedParentValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
    validator = new DeletedParentValidator();
  }

  @Test
  void shouldFailWhenCreatingEventInDeletedEnrollment() {
    Enrollment enrollment = enrollment(true, trackedEntity(false));
    Event event = newEvent(enrollment);

    validator.validate(reporter, bundle, event);

    assertHasError(
        reporter,
        event,
        E1132,
        "event: `"
            + EVENT_UID
            + "` cannot be created or updated because its enrollment: `"
            + enrollment.getUid()
            + "` is deleted.");
  }

  @Test
  void shouldFailWhenCreatingEventInEnrollmentOfDeletedTrackedEntity() {
    TrackedEntity trackedEntity = trackedEntity(true);
    Event event = newEvent(enrollment(false, trackedEntity));

    validator.validate(reporter, bundle, event);

    assertHasError(
        reporter,
        event,
        E1132,
        "event: `"
            + EVENT_UID
            + "` cannot be created or updated because its trackedEntity: `"
            + trackedEntity.getUid()
            + "` is deleted.");
  }

  @Test
  void shouldFailWhenUpdatingEventInDeletedEnrollment() {
    Event event = existingEvent(enrollment(true, trackedEntity(false)));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1132);
  }

  @Test
  void shouldFailWhenUpdatingEventInEnrollmentOfDeletedTrackedEntity() {
    Event event = existingEvent(enrollment(false, trackedEntity(true)));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1132);
  }

  @Test
  void shouldPassWhenCreatingEventInNotDeletedEnrollment() {
    Event event = newEvent(enrollment(false, trackedEntity(false)));

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEnrollmentIsOnlyInPayload() {
    when(bundle.getPreheat()).thenReturn(preheat);
    Event event = Event.builder().event(EVENT_UID).enrollment(CodeGenerator.generateUid()).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEventHasNoEnrollment() {
    when(bundle.getPreheat()).thenReturn(preheat);
    Event event = Event.builder().event(EVENT_UID).build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  private Event newEvent(Enrollment enrollment) {
    when(bundle.getPreheat()).thenReturn(preheat);
    String enrollmentUid = enrollment.getUid();
    when(preheat.getEnrollment(enrollmentUid)).thenReturn(enrollment);
    return Event.builder().event(EVENT_UID).enrollment(enrollmentUid).build();
  }

  private Event existingEvent(Enrollment enrollment) {
    when(bundle.getPreheat()).thenReturn(preheat);
    org.hisp.dhis.program.Event existingEvent = new org.hisp.dhis.program.Event();
    existingEvent.setUid(EVENT_UID);
    existingEvent.setEnrollment(enrollment);
    when(preheat.getEvent(EVENT_UID)).thenReturn(existingEvent);
    return Event.builder().event(EVENT_UID).build();
  }

  private static Enrollment enrollment(boolean deleted, TrackedEntity trackedEntity) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(CodeGenerator.generateUid());
    enrollment.setDeleted(deleted);
    enrollment.setTrackedEntity(trackedEntity);
    return enrollment;
  }

  private static TrackedEntity trackedEntity(boolean deleted) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(CodeGenerator.generateUid());
    trackedEntity.setDeleted(deleted);
    return trackedEntity;
  }
}
