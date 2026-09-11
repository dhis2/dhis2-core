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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentFields;
import org.hisp.dhis.tracker.export.timeout.Deadline;
import org.hisp.dhis.tracker.export.timeout.DeadlineExceededException;
import org.hisp.dhis.tracker.export.timeout.DeadlineHolder;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityFields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityIdentifiers;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Locks in the gating and merge semantics of {@link TrackedEntityAggregate#find(List,
 * TrackedEntityFields, TrackedEntityQueryParams)}. The four fetches run in series on the request
 * thread; nothing here asserts anything about timing, ordering or threading.
 */
@ExtendWith(MockitoExtension.class)
class TrackedEntityAggregateTest {

  private static final String TE_UID_1 = "OBzmpRP6YUh";
  private static final String TE_UID_2 = "KSd4PejqBf9";
  private static final long TE_ID_1 = 1L;
  private static final long TE_ID_2 = 2L;
  private static final long PROGRAM_ID = 42L;

  private static final List<TrackedEntityIdentifiers> IDENTIFIERS =
      List.of(
          new TrackedEntityIdentifiers(TE_ID_1, TE_UID_1),
          new TrackedEntityIdentifiers(TE_ID_2, TE_UID_2));
  private static final List<Long> IDS = List.of(TE_ID_1, TE_ID_2);

  @Mock private TrackedEntityStore trackedEntityStore;

  @Mock private EnrollmentAggregate enrollmentAggregate;

  @InjectMocks private TrackedEntityAggregate aggregate;

  @AfterEach
  void tearDown() {
    DeadlineHolder.clear();
  }

  @Test
  void shouldReturnEmptyListAndNotCallAnyStoreGivenNoIdentifiers() {
    List<TrackedEntity> trackedEntities =
        aggregate.find(List.of(), TrackedEntityFields.all(), new TrackedEntityQueryParams());

    assertTrue(trackedEntities.isEmpty());
    verifyNoInteractions(trackedEntityStore, enrollmentAggregate);
  }

  @Test
  void shouldOnlyFetchTrackedEntitiesGivenNoFields() {
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());

    List<TrackedEntity> trackedEntities =
        aggregate.find(IDENTIFIERS, TrackedEntityFields.none(), new TrackedEntityQueryParams());

    assertEquals(List.of(TE_UID_1, TE_UID_2), uids(trackedEntities));
    verify(trackedEntityStore).getTrackedEntities(IDS);
    verify(trackedEntityStore, never()).getAttributes(anyList(), any());
    verify(trackedEntityStore, never()).getProgramOwners(anyList());
    verify(enrollmentAggregate, never()).findByTrackedEntityIds(anyList(), any());
  }

  @Test
  void shouldFetchAttributesOnlyGivenAttributesAreIncluded() {
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());
    when(trackedEntityStore.getAttributes(eq(IDS), isNull()))
        .thenReturn(ArrayListMultimap.create());

    aggregate.find(
        IDENTIFIERS,
        TrackedEntityFields.builder().includeAttributes().build(),
        new TrackedEntityQueryParams());

    verify(trackedEntityStore).getTrackedEntities(IDS);
    verify(trackedEntityStore).getAttributes(eq(IDS), isNull());
    verify(trackedEntityStore, never()).getProgramOwners(anyList());
    verify(enrollmentAggregate, never()).findByTrackedEntityIds(anyList(), any());
  }

  @Test
  void shouldFetchEnrollmentsOnlyGivenEnrollmentsAreIncluded() {
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());
    when(enrollmentAggregate.findByTrackedEntityIds(eq(IDENTIFIERS), any()))
        .thenReturn(ArrayListMultimap.create());

    aggregate.find(
        IDENTIFIERS,
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.none()).build(),
        new TrackedEntityQueryParams());

    verify(trackedEntityStore).getTrackedEntities(IDS);
    verify(enrollmentAggregate).findByTrackedEntityIds(eq(IDENTIFIERS), any());
    verify(trackedEntityStore, never()).getAttributes(anyList(), any());
    verify(trackedEntityStore, never()).getProgramOwners(anyList());
  }

  @Test
  void shouldFetchProgramOwnersOnlyGivenProgramOwnersAreIncluded() {
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());
    when(trackedEntityStore.getProgramOwners(IDS)).thenReturn(ArrayListMultimap.create());

    aggregate.find(
        IDENTIFIERS,
        TrackedEntityFields.builder().includeProgramOwners().build(),
        new TrackedEntityQueryParams());

    verify(trackedEntityStore).getTrackedEntities(IDS);
    verify(trackedEntityStore).getProgramOwners(IDS);
    verify(trackedEntityStore, never()).getAttributes(anyList(), any());
    verify(enrollmentAggregate, never()).findByTrackedEntityIds(anyList(), any());
  }

  @Test
  void shouldMergeAttributesEnrollmentsAndProgramOwnersIntoTheirTrackedEntity() {
    TrackedEntityAttributeValue attribute1 = attributeValue("w75KJ2mc4zz", "Bob");
    TrackedEntityAttributeValue attribute2 = attributeValue("zDhUuAYrxNC", "Dylan");
    Enrollment enrollment1 = enrollment("Lg8Ub2R3EWO");
    Enrollment enrollment2 = enrollment("qJLLmxUgqB0");
    TrackedEntityProgramOwner programOwner1 = programOwner("IpHINAT79UW");
    TrackedEntityProgramOwner programOwner2 = programOwner("ur1Edk5Oe2n");

    Multimap<String, TrackedEntityAttributeValue> attributes = ArrayListMultimap.create();
    attributes.put(TE_UID_1, attribute1);
    attributes.put(TE_UID_2, attribute2);
    Multimap<String, Enrollment> enrollments = ArrayListMultimap.create();
    enrollments.put(TE_UID_1, enrollment1);
    enrollments.put(TE_UID_2, enrollment2);
    Multimap<String, TrackedEntityProgramOwner> programOwners = ArrayListMultimap.create();
    programOwners.put(TE_UID_1, programOwner1);
    programOwners.put(TE_UID_2, programOwner2);

    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());
    when(trackedEntityStore.getAttributes(eq(IDS), isNull())).thenReturn(attributes);
    when(enrollmentAggregate.findByTrackedEntityIds(eq(IDENTIFIERS), any()))
        .thenReturn(enrollments);
    when(trackedEntityStore.getProgramOwners(IDS)).thenReturn(programOwners);

    List<TrackedEntity> trackedEntities =
        aggregate.find(IDENTIFIERS, TrackedEntityFields.all(), new TrackedEntityQueryParams());

    assertEquals(List.of(TE_UID_1, TE_UID_2), uids(trackedEntities));
    TrackedEntity trackedEntity1 = trackedEntities.get(0);
    assertEquals(Set.of(attribute1), trackedEntity1.getTrackedEntityAttributeValues());
    assertEquals(Set.of(enrollment1), trackedEntity1.getEnrollments());
    assertEquals(Set.of(programOwner1), trackedEntity1.getProgramOwners());
    TrackedEntity trackedEntity2 = trackedEntities.get(1);
    assertEquals(Set.of(attribute2), trackedEntity2.getTrackedEntityAttributeValues());
    assertEquals(Set.of(enrollment2), trackedEntity2.getEnrollments());
    assertEquals(Set.of(programOwner2), trackedEntity2.getProgramOwners());
  }

  @Test
  void shouldSetEmptyCollectionsOnTrackedEntitiesGivenNoFields() {
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());

    List<TrackedEntity> trackedEntities =
        aggregate.find(IDENTIFIERS, TrackedEntityFields.none(), new TrackedEntityQueryParams());

    for (TrackedEntity trackedEntity : trackedEntities) {
      assertTrue(trackedEntity.getTrackedEntityAttributeValues().isEmpty());
      assertTrue(trackedEntity.getEnrollments().isEmpty());
      assertTrue(trackedEntity.getProgramOwners().isEmpty());
    }
  }

  @Test
  void shouldPassProgramIdToGetAttributesGivenEnrolledInTrackerProgram() {
    Program program = new Program();
    program.setId(PROGRAM_ID);
    TrackedEntityQueryParams queryParams =
        new TrackedEntityQueryParams().setEnrolledInTrackerProgram(program);
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());
    when(trackedEntityStore.getAttributes(IDS, PROGRAM_ID)).thenReturn(ArrayListMultimap.create());

    aggregate.find(
        IDENTIFIERS, TrackedEntityFields.builder().includeAttributes().build(), queryParams);

    verify(trackedEntityStore).getAttributes(IDS, PROGRAM_ID);
  }

  @Test
  void shouldPassNullProgramIdToGetAttributesGivenNoEnrolledInTrackerProgram() {
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());
    when(trackedEntityStore.getAttributes(eq(IDS), isNull()))
        .thenReturn(ArrayListMultimap.create());

    aggregate.find(
        IDENTIFIERS,
        TrackedEntityFields.builder().includeAttributes().build(),
        new TrackedEntityQueryParams());

    verify(trackedEntityStore).getAttributes(eq(IDS), isNull());
  }

  @Test
  void shouldThrowAndNotFetchAnythingGivenExpiredDeadline() {
    DeadlineHolder.set(Deadline.in(Duration.ofSeconds(1), expiredNanoTime()));
    TrackedEntityFields fields = TrackedEntityFields.all();
    TrackedEntityQueryParams queryParams = new TrackedEntityQueryParams();

    assertThrows(
        DeadlineExceededException.class, () -> aggregate.find(IDENTIFIERS, fields, queryParams));

    verifyNoInteractions(trackedEntityStore, enrollmentAggregate);
  }

  @Test
  void shouldFetchGivenDeadlineThatHasNotExpired() {
    DeadlineHolder.set(Deadline.in(Duration.ofSeconds(10)));
    when(trackedEntityStore.getTrackedEntities(IDS)).thenReturn(trackedEntities());

    List<TrackedEntity> trackedEntities =
        aggregate.find(IDENTIFIERS, TrackedEntityFields.none(), new TrackedEntityQueryParams());

    assertEquals(List.of(TE_UID_1, TE_UID_2), uids(trackedEntities));
  }

  /**
   * A nanoTime that jumps one hour forward after the deadline is created, so the deadline is
   * expired without the test having to sleep.
   */
  private static java.util.function.LongSupplier expiredNanoTime() {
    long start = System.nanoTime();
    return new java.util.function.LongSupplier() {
      private boolean created;

      @Override
      public long getAsLong() {
        if (!created) {
          created = true;
          return start;
        }
        return start + Duration.ofHours(1).toNanos();
      }
    };
  }

  private static Map<String, TrackedEntity> trackedEntities() {
    Map<String, TrackedEntity> trackedEntities = new LinkedHashMap<>();
    trackedEntities.put(TE_UID_1, trackedEntity(TE_UID_1));
    trackedEntities.put(TE_UID_2, trackedEntity(TE_UID_2));
    return trackedEntities;
  }

  private static TrackedEntity trackedEntity(String uid) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(uid);
    return trackedEntity;
  }

  private static List<String> uids(List<TrackedEntity> trackedEntities) {
    return trackedEntities.stream().map(TrackedEntity::getUid).toList();
  }

  private static TrackedEntityAttributeValue attributeValue(String attributeUid, String value) {
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid(attributeUid);
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setAttribute(attribute);
    attributeValue.setValue(value);
    return attributeValue;
  }

  private static Enrollment enrollment(String uid) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(uid);
    return enrollment;
  }

  private static TrackedEntityProgramOwner programOwner(String programUid) {
    // equality of TrackedEntityProgramOwner only includes its tracked entity and program, so the
    // program is what makes the two owners in a test distinguishable
    Program program = new Program();
    program.setUid(programUid);
    TrackedEntityProgramOwner programOwner = new TrackedEntityProgramOwner();
    programOwner.setProgram(program);
    return programOwner;
  }
}
