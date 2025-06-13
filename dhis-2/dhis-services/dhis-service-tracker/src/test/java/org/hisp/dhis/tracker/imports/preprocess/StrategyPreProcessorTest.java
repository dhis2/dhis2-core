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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.test.TestBase;
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
class StrategyPreProcessorTest extends TestBase {

  private static final UID TE_UID = UID.generate();

  private static final UID NEW_TE_UID = UID.generate();

  private static final UID ENROLLMENT_UID = UID.generate();

  private static final UID NEW_ENROLLMENT_UID = UID.generate();

  private static final UID EVENT_UID = UID.generate();

  private static final UID NEW_EVENT_UID = UID.generate();

  private static final UID RELATIONSHIP_UID = UID.generate();

  private static final UID NEW_RELATIONSHIP_UID = UID.generate();

  private Event dbEvent;

  private Enrollment preheatEnrollment;

  private TrackedEntity te;

  private Relationship relationship;

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent event;

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent newEvent;

  private org.hisp.dhis.tracker.imports.domain.Enrollment enrollment;

  private org.hisp.dhis.tracker.imports.domain.Enrollment newEnrollment;

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity;

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity newTrackedEntity;

  private org.hisp.dhis.tracker.imports.domain.Relationship payloadRelationship;

  private org.hisp.dhis.tracker.imports.domain.Relationship newPayloadRelationship;

  private final StrategyPreProcessor preProcessorToTest = new StrategyPreProcessor();

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUp() {
    te = new TrackedEntity();
    te.setUid(TE_UID.getValue());
    trackedEntity = new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
    trackedEntity.setTrackedEntity(TE_UID);
    newTrackedEntity = new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
    newTrackedEntity.setTrackedEntity(NEW_TE_UID);
    preheatEnrollment = new Enrollment();
    preheatEnrollment.setUid(ENROLLMENT_UID.getValue());
    enrollment = new org.hisp.dhis.tracker.imports.domain.Enrollment();
    enrollment.setEnrollment(ENROLLMENT_UID);
    newEnrollment = new org.hisp.dhis.tracker.imports.domain.Enrollment();
    newEnrollment.setEnrollment(NEW_ENROLLMENT_UID);
    dbEvent = new Event();
    dbEvent.setUid(EVENT_UID.getValue());
    event = org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder().event(EVENT_UID).build();
    newEvent =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder().event(NEW_EVENT_UID).build();
    relationship = new Relationship();
    relationship.setUid(RELATIONSHIP_UID.getValue());
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
            .trackedEntities(List.of(trackedEntity, newTrackedEntity))
            .enrollments(List.of(enrollment, newEnrollment))
            .trackerEvents(List.of(event, newEvent))
            .relationships(List.of(payloadRelationship, newPayloadRelationship))
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .preheat(preheat)
            .build();
    preProcessorToTest.process(bundle);

    assertEquals(UPDATE, getStrategy(bundle, TRACKED_ENTITY, TE_UID));
    assertEquals(CREATE, getStrategy(bundle, TRACKED_ENTITY, NEW_TE_UID));
    assertEquals(UPDATE, getStrategy(bundle, ENROLLMENT, ENROLLMENT_UID));
    assertEquals(CREATE, getStrategy(bundle, ENROLLMENT, NEW_ENROLLMENT_UID));
    assertEquals(UPDATE, getStrategy(bundle, EVENT, EVENT_UID));
    assertEquals(CREATE, getStrategy(bundle, EVENT, NEW_EVENT_UID));
    assertEquals(UPDATE, getStrategy(bundle, RELATIONSHIP, RELATIONSHIP_UID));
    assertEquals(CREATE, getStrategy(bundle, RELATIONSHIP, NEW_RELATIONSHIP_UID));
  }

  @Test
  void testStrategyPreprocessForDelete() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackedEntities(List.of(trackedEntity, newTrackedEntity))
            .enrollments(List.of(enrollment, newEnrollment))
            .trackerEvents(List.of(event, newEvent))
            .relationships(List.of(payloadRelationship, newPayloadRelationship))
            .importStrategy(DELETE)
            .preheat(preheat)
            .build();
    preProcessorToTest.process(bundle);

    assertEquals(DELETE, getStrategy(bundle, TRACKED_ENTITY, TE_UID));
    assertEquals(DELETE, getStrategy(bundle, TRACKED_ENTITY, NEW_TE_UID));
    assertEquals(DELETE, getStrategy(bundle, ENROLLMENT, ENROLLMENT_UID));
    assertEquals(DELETE, getStrategy(bundle, ENROLLMENT, NEW_ENROLLMENT_UID));
    assertEquals(DELETE, getStrategy(bundle, EVENT, EVENT_UID));
    assertEquals(DELETE, getStrategy(bundle, EVENT, NEW_EVENT_UID));
    assertEquals(DELETE, getStrategy(bundle, RELATIONSHIP, RELATIONSHIP_UID));
    assertEquals(DELETE, getStrategy(bundle, RELATIONSHIP, NEW_RELATIONSHIP_UID));
  }

  private TrackerImportStrategy getStrategy(
      TrackerBundle bundle, TrackerType trackerType, UID uid) {
    return bundle.getResolvedStrategyMap().get(trackerType).get(uid);
  }
}
