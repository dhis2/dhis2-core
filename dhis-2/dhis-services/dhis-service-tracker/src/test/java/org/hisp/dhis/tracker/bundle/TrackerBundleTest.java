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
package org.hisp.dhis.tracker.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerBundleTest {

  @Test
  void testBasicSetup1() {
    TrackerBundle trackerBundle =
        TrackerBundle.builder()
            .atomicMode(AtomicMode.ALL)
            .validationMode(ValidationMode.SKIP)
            .trackedEntities(Collections.singletonList(new TrackedEntity()))
            .enrollments(Collections.singletonList(new Enrollment()))
            .events(Collections.singletonList(new Event()))
            .build();
    assertEquals(AtomicMode.ALL, trackerBundle.getAtomicMode());
    assertSame(trackerBundle.getValidationMode(), ValidationMode.SKIP);
    assertFalse(trackerBundle.getTrackedEntities().isEmpty());
    assertFalse(trackerBundle.getEnrollments().isEmpty());
    assertFalse(trackerBundle.getEvents().isEmpty());
  }

  @Test
  void testBasicSetup2() {
    TrackerBundle trackerBundle =
        TrackerBundle.builder()
            .atomicMode(AtomicMode.ALL)
            .validationMode(ValidationMode.SKIP)
            .trackedEntities(Arrays.asList(new TrackedEntity(), new TrackedEntity()))
            .enrollments(Arrays.asList(new Enrollment(), new Enrollment()))
            .events(Arrays.asList(new Event(), new Event()))
            .build();
    assertEquals(AtomicMode.ALL, trackerBundle.getAtomicMode());
    assertSame(trackerBundle.getValidationMode(), ValidationMode.SKIP);
    assertEquals(2, trackerBundle.getTrackedEntities().size());
    assertEquals(2, trackerBundle.getEnrollments().size());
    assertEquals(2, trackerBundle.getEvents().size());
  }

  @Test
  void testGetTrackedEntityGivenNull() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackedEntities(List.of(TrackedEntity.builder().trackedEntity("uid").build()))
            .build();

    assertTrue(bundle.findTrackedEntityByUid(null).isEmpty());
  }

  @Test
  void testExistsTrackedEntity() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackedEntities(List.of(TrackedEntity.builder().trackedEntity("uid").build()))
            .build();

    assertFalse(bundle.exists(TrackerType.TRACKED_ENTITY, "missing"));
    assertTrue(bundle.exists(TrackedEntity.builder().trackedEntity("uid").build()));
  }

  @Test
  void testGetEnrollmentGivenNull() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .enrollments(List.of(Enrollment.builder().enrollment("uid").build()))
            .build();

    assertTrue(bundle.findEnrollmentByUid(null).isEmpty());
  }

  @Test
  void testExistsEnrollment() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .enrollments(List.of(Enrollment.builder().enrollment("uid").build()))
            .build();

    assertFalse(bundle.exists(TrackerType.ENROLLMENT, "missing"));
    assertTrue(bundle.exists(Enrollment.builder().enrollment("uid").build()));
  }

  @Test
  void testGetEventGivenNull() {
    TrackerBundle bundle =
        TrackerBundle.builder().events(List.of(Event.builder().event("uid").build())).build();

    assertTrue(bundle.findEventByUid(null).isEmpty());
  }

  @Test
  void testExistsEvent() {
    TrackerBundle bundle =
        TrackerBundle.builder().events(List.of(Event.builder().event("uid").build())).build();

    assertFalse(bundle.exists(TrackerType.EVENT, "missing"));
    assertTrue(bundle.exists(Event.builder().event("uid").build()));
  }

  @Test
  void testGetRelationshipGivenNull() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .relationships(List.of(Relationship.builder().relationship("uid").build()))
            .build();

    assertTrue(bundle.findRelationshipByUid(null).isEmpty());
  }

  @Test
  void testGetRelationshipInBundleContainingNullUids() {
    TrackerBundle bundle =
        TrackerBundle.builder().relationships(List.of(Relationship.builder().build())).build();

    assertTrue(bundle.findRelationshipByUid("uid").isEmpty());
  }

  @Test
  void testExistsRelationship() {
    TrackerBundle bundle =
        TrackerBundle.builder()
            .relationships(List.of(Relationship.builder().relationship("uid").build()))
            .build();

    assertFalse(bundle.exists(TrackerType.RELATIONSHIP, "missing"));
    assertTrue(bundle.exists(Relationship.builder().relationship("uid").build()));
  }

  @Test
  void testExistsFailsOnNullType() {
    TrackerBundle bundle = TrackerBundle.builder().build();

    assertThrows(NullPointerException.class, () -> bundle.exists(null, "uid"));
  }
}
