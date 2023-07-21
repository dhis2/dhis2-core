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
package org.hisp.dhis.tracker.export.relationship;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// @MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class RelationshipOperationParamsMapperTest extends DhisConvenienceTest {

  private static final String TE_UID = "OBzmpRP6YUh";

  private static final String EN_UID = "KSd4PejqBf9";

  private static final String EV_UID = "TvjwTPToKHO";

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private EnrollmentService enrollmentService;

  @Mock private EventService eventService;

  @InjectMocks private RelationshipOperationParamsMapper mapper;

  private TrackedEntity trackedEntity;

  private Enrollment enrollment;

  private Event event;

  @BeforeEach
  public void setUp() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    Program program = createProgram('A');
    ProgramStage programStage = createProgramStage('A', program);

    trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.setUid(TE_UID);
    enrollment = createEnrollment(program, trackedEntity, organisationUnit);
    enrollment.setUid(EN_UID);
    event = createEvent(programStage, enrollment, organisationUnit);
    event.setUid(EV_UID);
  }

  @Test
  void shouldMapTrackedEntityWhenATrackedEntityIsPassed()
      throws NotFoundException, ForbiddenException {
    when(trackedEntityService.getTrackedEntity(TE_UID, TrackedEntityParams.TRUE, true))
        .thenReturn(trackedEntity);
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(TRACKED_ENTITY).identifier(TE_UID).build();

    RelationshipQueryParams queryParams = mapper.map(params);

    assertInstanceOf(TrackedEntity.class, queryParams.getEntity());
    assertEquals(TE_UID, queryParams.getEntity().getUid());
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTrackedEntityIsNotFound()
      throws NotFoundException, ForbiddenException {
    when(trackedEntityService.getTrackedEntity(TE_UID, TrackedEntityParams.TRUE, true))
        .thenThrow(new NotFoundException(TrackedEntity.class, TE_UID));
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(TRACKED_ENTITY).identifier(TE_UID).build();

    assertThrows(NotFoundException.class, () -> mapper.map(params));
  }

  @Test
  void shouldMapToNullWhenUserHasNoAccessToTrackedEntity()
      throws NotFoundException, ForbiddenException {
    when(trackedEntityService.getTrackedEntity(TE_UID, TrackedEntityParams.TRUE, true))
        .thenThrow(new ForbiddenException("User has no access"));
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(TRACKED_ENTITY).identifier(TE_UID).build();

    RelationshipQueryParams queryParams = mapper.map(params);

    assertNull(queryParams.getEntity());
  }

  @Test
  void shouldMapEnrollmentWhenAEnrollmentIsPassed() throws NotFoundException, ForbiddenException {
    when(enrollmentService.getEnrollment(EN_UID, EnrollmentParams.TRUE, true))
        .thenReturn(enrollment);
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(ENROLLMENT).identifier(EN_UID).build();

    RelationshipQueryParams queryParams = mapper.map(params);

    assertInstanceOf(Enrollment.class, queryParams.getEntity());
    assertEquals(EN_UID, queryParams.getEntity().getUid());
  }

  @Test
  void shouldThrowNotFoundExceptionWhenEnrollmentIsNotFound()
      throws NotFoundException, ForbiddenException {
    when(enrollmentService.getEnrollment(EN_UID, EnrollmentParams.TRUE, true))
        .thenThrow(new NotFoundException(Enrollment.class, EN_UID));
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(ENROLLMENT).identifier(EN_UID).build();

    assertThrows(NotFoundException.class, () -> mapper.map(params));
  }

  @Test
  void shouldMapToNullWhenUserHasNoAccessToEnrollment()
      throws NotFoundException, ForbiddenException {
    when(enrollmentService.getEnrollment(EN_UID, EnrollmentParams.TRUE, true))
        .thenThrow(new ForbiddenException("User has no access"));
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(ENROLLMENT).identifier(EN_UID).build();

    RelationshipQueryParams queryParams = mapper.map(params);

    assertNull(queryParams.getEntity());
  }

  @Test
  void shouldMapEventWhenAEventIsPassed() throws NotFoundException, ForbiddenException {
    when(eventService.getEvent(EV_UID, EventParams.TRUE)).thenReturn(event);
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(EVENT).identifier(EV_UID).build();

    RelationshipQueryParams queryParams = mapper.map(params);

    assertInstanceOf(Event.class, queryParams.getEntity());
    assertEquals(EV_UID, queryParams.getEntity().getUid());
  }

  @Test
  void shouldThrowNotFoundExceptionWhenEventIsNotFound()
      throws NotFoundException, ForbiddenException {
    when(eventService.getEvent(EV_UID, EventParams.TRUE))
        .thenThrow(new NotFoundException(Event.class, EV_UID));
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(EVENT).identifier(EV_UID).build();

    assertThrows(NotFoundException.class, () -> mapper.map(params));
  }

  @Test
  void shouldMapToNullWhenUserHasNoAccessToEvent() throws NotFoundException, ForbiddenException {
    when(eventService.getEvent(EV_UID, EventParams.TRUE))
        .thenThrow(new ForbiddenException("User has no access"));
    RelationshipOperationParams params =
        RelationshipOperationParams.builder().type(EVENT).identifier(EV_UID).build();

    RelationshipQueryParams queryParams = mapper.map(params);

    assertNull(queryParams.getEntity());
  }
}
