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
package org.hisp.dhis.tracker.imports.validation.validator.relationship;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4021;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4022;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletedLinkValidatorTest {
  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private DeletedLinkValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);
    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
    validator = new DeletedLinkValidator();
  }

  @Test
  void shouldFailWhenLinkingToDeletedTrackedEntity() {
    TrackedEntity trackedEntity = trackedEntity(true);
    UID uid = UID.of(trackedEntity);
    when(preheat.getTrackedEntity(uid)).thenReturn(trackedEntity);
    Relationship relationship = relationship(RelationshipItem.builder().trackedEntity(uid).build());

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4021,
        "Relationship cannot link to trackedEntity: `"
            + uid.getValue()
            + "` because it is deleted.");
  }

  @Test
  void shouldPassWhenLinkingToNotDeletedTrackedEntity() {
    TrackedEntity trackedEntity = trackedEntity(false);
    UID uid = UID.of(trackedEntity);
    when(preheat.getTrackedEntity(uid)).thenReturn(trackedEntity);
    Relationship relationship = relationship(RelationshipItem.builder().trackedEntity(uid).build());

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenLinkingToDeletedEnrollment() {
    Enrollment enrollment = enrollment(true, trackedEntity(false));
    Relationship relationship = enrollmentRelationship(enrollment);

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4021,
        "Relationship cannot link to enrollment: `"
            + enrollment.getUid()
            + "` because it is deleted.");
  }

  @Test
  void shouldFailWhenLinkingToEnrollmentOfDeletedTrackedEntity() {
    TrackedEntity trackedEntity = trackedEntity(true);
    Enrollment enrollment = enrollment(false, trackedEntity);
    Relationship relationship = enrollmentRelationship(enrollment);

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4022,
        "Relationship cannot link to enrollment: `"
            + enrollment.getUid()
            + "` because its trackedEntity: `"
            + trackedEntity.getUid()
            + "` is deleted.");
  }

  @Test
  void shouldPassWhenLinkingToNotDeletedEnrollment() {
    Relationship relationship = enrollmentRelationship(enrollment(false, trackedEntity(false)));

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenLinkingToDeletedTrackerEvent() {
    Event event = trackerEvent(true, enrollment(false, trackedEntity(false)));
    Relationship relationship = trackerEventRelationship(event);

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4021,
        "Relationship cannot link to event: `" + event.getUid() + "` because it is deleted.");
  }

  @Test
  void shouldFailWhenLinkingToDeletedEventWithoutTrackedEntity() {
    Event event = trackerEvent(true, enrollment(false, null));
    Relationship relationship = trackerEventRelationship(event);

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4021,
        "Relationship cannot link to event: `" + event.getUid() + "` because it is deleted.");
  }

  @Test
  void shouldFailWhenLinkingToEventInDeletedEnrollment() {
    Enrollment enrollment = enrollment(true, trackedEntity(false));
    Event event = trackerEvent(false, enrollment);
    Relationship relationship = trackerEventRelationship(event);

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4022,
        "Relationship cannot link to event: `"
            + event.getUid()
            + "` because its enrollment: `"
            + enrollment.getUid()
            + "` is deleted.");
  }

  @Test
  void shouldFailWhenLinkingToEventOfDeletedTrackedEntity() {
    TrackedEntity trackedEntity = trackedEntity(true);
    Event event = trackerEvent(false, enrollment(false, trackedEntity));
    Relationship relationship = trackerEventRelationship(event);

    validator.validate(reporter, bundle, relationship);

    assertHasError(
        reporter,
        relationship,
        E4022,
        "Relationship cannot link to event: `"
            + event.getUid()
            + "` because its trackedEntity: `"
            + trackedEntity.getUid()
            + "` is deleted.");
  }

  @Test
  void shouldPassWhenLinkingToNotDeletedTrackerEvent() {
    Relationship relationship =
        trackerEventRelationship(trackerEvent(false, enrollment(false, trackedEntity(false))));

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenLinkedEntitiesAreOnlyInPayload() {
    Relationship relationship =
        Relationship.builder()
            .relationship(UID.generate())
            .from(RelationshipItem.builder().trackedEntity(UID.generate()).build())
            .to(RelationshipItem.builder().event(UID.generate()).build())
            .relationshipType(MetadataIdentifier.ofUid(UID.generate().getValue()))
            .build();

    validator.validate(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  private Relationship enrollmentRelationship(Enrollment enrollment) {
    UID uid = UID.of(enrollment);
    when(preheat.getEnrollment(uid)).thenReturn(enrollment);
    return relationship(RelationshipItem.builder().enrollment(uid).build());
  }

  private Relationship trackerEventRelationship(Event event) {
    UID uid = UID.of(event);
    when(preheat.getEvent(uid)).thenReturn(event);
    return relationship(RelationshipItem.builder().event(uid).build());
  }

  private static Relationship relationship(RelationshipItem item) {
    return Relationship.builder()
        .relationship(UID.generate())
        .from(item)
        .to(item)
        .relationshipType(MetadataIdentifier.ofUid(UID.generate().getValue()))
        .build();
  }

  private static TrackedEntity trackedEntity(boolean deleted) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(UID.generate().getValue());
    trackedEntity.setDeleted(deleted);
    return trackedEntity;
  }

  private static Enrollment enrollment(boolean deleted, TrackedEntity trackedEntity) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(UID.generate().getValue());
    enrollment.setDeleted(deleted);
    enrollment.setTrackedEntity(trackedEntity);
    return enrollment;
  }

  private static Event trackerEvent(boolean deleted, Enrollment enrollment) {
    Event event = new Event();
    event.setUid(UID.generate().getValue());
    event.setDeleted(deleted);
    event.setEnrollment(enrollment);
    return event;
  }
}
