/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.imports;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ParamsConverterTest extends TestBase {
  private static final UID TRACKER_EVENT_UID = UID.generate();

  private static final UID SINGLE_EVENT_UID = UID.generate();

  private Program programWithRegistration;

  private Program programWithoutRegistration;

  private ProgramStage programStageWithRegistration;

  private ProgramStage programStageWithoutRegistration;

  private UserDetails user;

  private TrackerPreheat trackerPreheat;

  @BeforeEach
  void setup() {
    user = UserDetails.fromUser(new User());

    programWithRegistration = createProgram('A');
    programWithRegistration.setProgramType(ProgramType.WITH_REGISTRATION);
    programWithoutRegistration = createProgram('B');
    programWithoutRegistration.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programStageWithRegistration = createProgramStage('C', programWithRegistration);
    programStageWithoutRegistration = createProgramStage('D', programWithoutRegistration);
    trackerPreheat = new TrackerPreheat();
    trackerPreheat.put(programWithRegistration);
    trackerPreheat.put(programWithoutRegistration);
    trackerPreheat.put(programStageWithRegistration);
    trackerPreheat.put(programStageWithoutRegistration);
  }

  @Test
  void shouldSuccessToConvertWhenNoTrackerObjectsArePresent() {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = TrackerObjects.builder().build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertEquals(params.getImportStrategy(), trackerBundle.getImportStrategy());
    assertEquals(params.getImportMode(), trackerBundle.getImportMode());
    assertEquals(params.getImportStrategy(), trackerBundle.getImportStrategy());
    assertEquals(params.isSkipPatternValidation(), trackerBundle.isSkipRuleEngine());
    assertEquals(params.isSkipSideEffects(), trackerBundle.isSkipSideEffects());
    assertEquals(params.isSkipRuleEngine(), trackerBundle.isSkipRuleEngine());
    assertEquals(params.getFlushMode(), trackerBundle.getFlushMode());
    assertEquals(params.getValidationMode(), trackerBundle.getValidationMode());
    assertIsEmpty(trackerBundle.getTrackedEntities());
    assertIsEmpty(trackerBundle.getEnrollments());
    assertIsEmpty(trackerBundle.getEvents());
    assertIsEmpty(trackerBundle.getRelationships());
    assertEquals(user, trackerBundle.getUser());
  }

  @Test
  void shouldSuccessToConvertTrackedEntities() {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .trackedEntities(List.of(TrackedEntity.builder().trackedEntity(UID.generate()).build()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertEquals(trackerObjects.getTrackedEntities(), trackerBundle.getTrackedEntities());
    assertIsEmpty(trackerBundle.getEnrollments());
    assertIsEmpty(trackerBundle.getEvents());
    assertIsEmpty(trackerBundle.getRelationships());
  }

  @Test
  void shouldSuccessToConvertEnrollments() {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .enrollments(List.of(Enrollment.builder().enrollment(UID.generate()).build()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertIsEmpty(trackerBundle.getTrackedEntities());
    assertEquals(trackerObjects.getEnrollments(), trackerBundle.getEnrollments());
    assertIsEmpty(trackerBundle.getEvents());
    assertIsEmpty(trackerBundle.getRelationships());
  }

  @Test
  void shouldSuccessToConvertRelationships() {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .relationships(List.of(Relationship.builder().relationship(UID.generate()).build()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertIsEmpty(trackerBundle.getTrackedEntities());
    assertIsEmpty(trackerBundle.getEnrollments());
    assertIsEmpty(trackerBundle.getEvents());
    assertEquals(trackerObjects.getRelationships(), trackerBundle.getRelationships());
  }

  @Test
  void shouldSuccessToConvertEventsBasedOnProgramWhenStrategyIsCreate() {
    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build();

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(List.of(trackerEventWithProgram(), singleEventWithProgram()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertContainsOnly(
        trackerBundle.getTrackerEvents().stream().map(TrackerDto::getUid).toList(),
        List.of(TRACKER_EVENT_UID));
    assertContainsOnly(
        trackerBundle.getSingleEvents().stream().map(TrackerDto::getUid).toList(),
        List.of(SINGLE_EVENT_UID));
  }

  @Test
  void shouldSuccessToConvertEventsBasedOnProgramStageWhenStrategyIsCreate() {
    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(List.of(trackerEventWithProgramStage(), singleEventWithProgramStage()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertContainsOnly(
        trackerBundle.getTrackerEvents().stream().map(TrackerDto::getUid).toList(),
        List.of(TRACKER_EVENT_UID));
    assertContainsOnly(
        trackerBundle.getSingleEvents().stream().map(TrackerDto::getUid).toList(),
        List.of(SINGLE_EVENT_UID));
  }

  @Test
  void
      shouldSuccessToConvertToTrackerEventsWhenNoProgramOrProgramStageIsPresentWhenStrategyIsCreate() {
    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder().events(List.of(trackerEvent(), singleEvent())).build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertContainsOnly(
        trackerBundle.getTrackerEvents().stream().map(TrackerDto::getUid).toList(),
        List.of(TRACKER_EVENT_UID, SINGLE_EVENT_UID));
    assertIsEmpty(trackerBundle.getSingleEvents());
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      names = {"UPDATE", "DELETE"})
  void shouldSuccessToConvertEventsWithDeleteOrUpdateStrategyWhenEventsArePresentInPreheat(
      TrackerImportStrategy importStrategy) {
    trackerPreheat.putTrackerEvent(trackerEventFromDB());
    trackerPreheat.putSingleEvent(singleEventFromDB());

    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(importStrategy).build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(List.of(trackerEventWithProgramStage(), singleEventWithProgramStage()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertContainsOnly(
        List.of(TRACKER_EVENT_UID),
        trackerBundle.getTrackerEvents().stream().map(TrackerDto::getUid).toList());
    assertContainsOnly(
        List.of(SINGLE_EVENT_UID),
        trackerBundle.getSingleEvents().stream().map(TrackerDto::getUid).toList());
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      names = {"UPDATE", "DELETE"})
  void
      shouldSuccessToConvertEventsToTrackerEventsWithDeleteOrUpdateStrategyWhenEventsAreNotPresentInPreheat(
          TrackerImportStrategy importStrategy) {
    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(importStrategy).build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(List.of(trackerEventWithProgramStage(), singleEventWithProgramStage()))
            .build();
    TrackerBundle trackerBundle =
        ParamsConverter.convert(params, trackerObjects, user, trackerPreheat);

    assertContainsOnly(
        trackerBundle.getTrackerEvents().stream().map(TrackerDto::getUid).toList(),
        List.of(TRACKER_EVENT_UID, SINGLE_EVENT_UID));
    assertIsEmpty(trackerBundle.getSingleEvents());
  }

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent trackerEvent() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(TRACKER_EVENT_UID)
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent singleEvent() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(SINGLE_EVENT_UID)
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent trackerEventWithProgram() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(TRACKER_EVENT_UID)
        .program(MetadataIdentifier.ofUid(programWithRegistration))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent singleEventWithProgram() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(SINGLE_EVENT_UID)
        .program(MetadataIdentifier.ofUid(programWithoutRegistration))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent trackerEventWithProgramStage() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(TRACKER_EVENT_UID)
        .programStage(MetadataIdentifier.ofUid(programStageWithRegistration))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.TrackerEvent singleEventWithProgramStage() {
    return org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
        .event(SINGLE_EVENT_UID)
        .programStage(MetadataIdentifier.ofUid(programStageWithoutRegistration))
        .build();
  }

  private TrackerEvent trackerEventFromDB() {
    TrackerEvent event = new TrackerEvent();
    event.setUid(TRACKER_EVENT_UID.getValue());
    event.setProgramStage(programStageWithRegistration);
    return event;
  }

  private SingleEvent singleEventFromDB() {
    SingleEvent event = new SingleEvent();
    event.setUid(SINGLE_EVENT_UID.getValue());
    event.setProgramStage(programStageWithoutRegistration);
    return event;
  }
}
