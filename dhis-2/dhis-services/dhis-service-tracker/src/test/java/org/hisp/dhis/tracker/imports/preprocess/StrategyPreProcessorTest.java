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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StrategyPreProcessorTest extends DhisConvenienceTest {

  private static final String TE_UID = "TeiUid";

  private static final String NEW_TE_UID = "NewTeiUid";

  private static final String ENROLLMENT_UID = "EnrollmentUid";

  private static final String NEW_ENROLLMENT_UID = "NewEnrollmentUid";

  private static final String EVENT_UID = "EventUid";

  private static final String NEW_EVENT_UID = "NewEventUid";

  private static final String RELATIONSHIP_UID = "RelationshipUid";

  private static final String NEW_RELATIONSHIP_UID = "NewRelationshipUid";

  private Event dbEvent;

  private Enrollment preheatEnrollment;

  private TrackedEntity te;

  private Relationship relationship;

  private org.hisp.dhis.tracker.imports.domain.Event event;

  private org.hisp.dhis.tracker.imports.domain.Event newEvent;

  private org.hisp.dhis.tracker.imports.domain.Enrollment enrollment;

  private org.hisp.dhis.tracker.imports.domain.Enrollment newEnrollment;

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity;

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity newTrackedEntity;

  private org.hisp.dhis.tracker.imports.domain.Relationship payloadRelationship;

  private org.hisp.dhis.tracker.imports.domain.Relationship newPayloadRelationship;

  private StrategyPreProcessor preProcessorToTest = new StrategyPreProcessor();

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUp() {
    te = new TrackedEntity();
    te.setUid(TE_UID);
    trackedEntity = new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
    trackedEntity.setTrackedEntity(TE_UID);
    newTrackedEntity = new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
    newTrackedEntity.setTrackedEntity(NEW_TE_UID);
    preheatEnrollment = new Enrollment();
    preheatEnrollment.setUid(ENROLLMENT_UID);
    enrollment = new org.hisp.dhis.tracker.imports.domain.Enrollment();
    enrollment.setEnrollment(ENROLLMENT_UID);
    newEnrollment = new org.hisp.dhis.tracker.imports.domain.Enrollment();
    newEnrollment.setEnrollment(NEW_ENROLLMENT_UID);
    dbEvent = new Event();
    dbEvent.setUid(EVENT_UID);
    event = new org.hisp.dhis.tracker.imports.domain.Event();
    event.setEvent(EVENT_UID);
    newEvent = new org.hisp.dhis.tracker.imports.domain.Event();
    newEvent.setEvent(NEW_EVENT_UID);
    relationship = new Relationship();
    relationship.setUid(RELATIONSHIP_UID);
    payloadRelationship = new org.hisp.dhis.tracker.imports.domain.Relationship();
    payloadRelationship.setRelationship(RELATIONSHIP_UID);
    newPayloadRelationship = new org.hisp.dhis.tracker.imports.domain.Relationship();
    newPayloadRelationship.setRelationship(NEW_RELATIONSHIP_UID);
    Mockito.when(preheat.getTrackedEntity(TE_UID)).thenReturn(te);
    Mockito.when(preheat.getEnrollment(ENROLLMENT_UID)).thenReturn(preheatEnrollment);
    Mockito.when(preheat.getEvent(EVENT_UID)).thenReturn(dbEvent);
    Mockito.when(preheat.getRelationship(RELATIONSHIP_UID)).thenReturn(relationship);
  }

  @Test
  void testStrategyPreprocessForCreateAndUpdate() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackedEntities(Lists.newArrayList(trackedEntity, newTrackedEntity))
            .enrollments(Lists.newArrayList(enrollment, newEnrollment))
            .events(Lists.newArrayList(event, newEvent))
            .relationships(Lists.newArrayList(payloadRelationship, newPayloadRelationship))
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .preheat(preheat)
            .build();
    preProcessorToTest.process(bundle);
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.TRACKED_ENTITY).get(TE_UID),
        Matchers.is(TrackerImportStrategy.UPDATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.TRACKED_ENTITY).get(NEW_TE_UID),
        Matchers.is(TrackerImportStrategy.CREATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.ENROLLMENT).get(ENROLLMENT_UID),
        Matchers.is(TrackerImportStrategy.UPDATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.ENROLLMENT).get(NEW_ENROLLMENT_UID),
        Matchers.is(TrackerImportStrategy.CREATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.EVENT).get(EVENT_UID),
        Matchers.is(TrackerImportStrategy.UPDATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.EVENT).get(NEW_EVENT_UID),
        Matchers.is(TrackerImportStrategy.CREATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.RELATIONSHIP).get(RELATIONSHIP_UID),
        Matchers.is(TrackerImportStrategy.UPDATE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.RELATIONSHIP).get(NEW_RELATIONSHIP_UID),
        Matchers.is(TrackerImportStrategy.CREATE));
  }

  @Test
  void testStrategyPreprocessForDelete() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackedEntities(Lists.newArrayList(trackedEntity, newTrackedEntity))
            .enrollments(Lists.newArrayList(enrollment, newEnrollment))
            .events(Lists.newArrayList(event, newEvent))
            .relationships(Lists.newArrayList(payloadRelationship, newPayloadRelationship))
            .importStrategy(TrackerImportStrategy.DELETE)
            .preheat(preheat)
            .build();
    preProcessorToTest.process(bundle);
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.TRACKED_ENTITY).get(TE_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.TRACKED_ENTITY).get(NEW_TE_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.ENROLLMENT).get(ENROLLMENT_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.ENROLLMENT).get(NEW_ENROLLMENT_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.EVENT).get(EVENT_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.EVENT).get(NEW_EVENT_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.RELATIONSHIP).get(RELATIONSHIP_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
    assertThat(
        bundle.getResolvedStrategyMap().get(TrackerType.RELATIONSHIP).get(NEW_RELATIONSHIP_UID),
        Matchers.is(TrackerImportStrategy.DELETE));
  }
}
