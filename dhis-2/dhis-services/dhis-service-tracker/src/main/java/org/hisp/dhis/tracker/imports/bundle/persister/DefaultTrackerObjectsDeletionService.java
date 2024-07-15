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
package org.hisp.dhis.tracker.imports.bundle.persister;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.EnrollmentTrackerConverterService;
import org.hisp.dhis.tracker.imports.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.imports.converter.RelationshipTrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.springframework.stereotype.Service;

/**
 * @author Zubair Asghar
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerObjectsDeletionService implements TrackerObjectDeletionService {
  private final org.hisp.dhis.program.EnrollmentService apiEnrollmentService;

  private final TrackedEntityService teService;

  private final IdentifiableObjectManager manager;

  private final RelationshipService relationshipService;

  private final EnrollmentTrackerConverterService enrollmentTrackerConverterService;

  private final EventTrackerConverterService eventTrackerConverterService;

  private final RelationshipTrackerConverterService relationshipTrackerConverterService;

  @Override
  public List<Entity> deleteEnrollments(TrackerBundle bundle) {
    List<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollments = bundle.getEnrollments();
    List<Entity> deletedEntities = new ArrayList<>();
    for (org.hisp.dhis.tracker.imports.domain.Enrollment value : enrollments) {
      String uid = value.getEnrollment();

      Entity objectReport = new Entity(TrackerType.ENROLLMENT, uid);

      Enrollment enrollment = manager.get(Enrollment.class, uid);
      enrollment.setLastUpdatedByUserInfo(bundle.getUserInfo());

      List<org.hisp.dhis.tracker.imports.domain.Event> events =
          eventTrackerConverterService.to(
              enrollment.getEvents().stream().filter(event -> !event.isDeleted()).toList());

      List<org.hisp.dhis.tracker.imports.domain.Relationship> relationships =
          relationshipTrackerConverterService.to(
              enrollment.getRelationshipItems().stream()
                  .map(RelationshipItem::getRelationship)
                  .filter(r -> !r.isDeleted())
                  .toList());

      TrackerBundle trackerBundle =
          TrackerBundle.builder()
              .events(events)
              .relationships(relationships)
              .user(bundle.getUser())
              .userInfo(bundle.getUserInfo())
              .build();

      List<Entity> deletedEvents = deleteEvents(trackerBundle);
      List<Entity> deletedRelationships = deleteRelationships(trackerBundle);

      TrackedEntity te = enrollment.getTrackedEntity();
      te.setLastUpdatedByUserInfo(bundle.getUserInfo());
      te.getEnrollments().remove(enrollment);

      apiEnrollmentService.deleteEnrollment(enrollment);
      teService.updateTrackedEntity(te);

      deletedEntities.add(objectReport);
      deletedEntities.addAll(deletedEvents);
      deletedEntities.addAll(deletedRelationships);
    }

    return deletedEntities;
  }

  @Override
  public List<Entity> deleteEvents(TrackerBundle bundle) {
    List<Entity> deletedEntities = new ArrayList<>();

    List<org.hisp.dhis.tracker.imports.domain.Event> events = bundle.getEvents();

    for (org.hisp.dhis.tracker.imports.domain.Event value : events) {
      String uid = value.getEvent();

      Entity objectReport = new Entity(TrackerType.EVENT, uid);

      Event event = manager.get(Event.class, uid);
      event.setLastUpdatedByUserInfo(bundle.getUserInfo());

      List<org.hisp.dhis.tracker.imports.domain.Relationship> relationships =
          relationshipTrackerConverterService.to(
              event.getRelationshipItems().stream()
                  .map(RelationshipItem::getRelationship)
                  .filter(r -> !r.isDeleted())
                  .toList());
      TrackerBundle trackerBundle =
          TrackerBundle.builder()
              .events(events)
              .relationships(relationships)
              .user(bundle.getUser())
              .userInfo(bundle.getUserInfo())
              .build();

      List<Entity> deletedRelationships = deleteRelationships(trackerBundle);

      manager.delete(event);

      if (event.getProgramStage().getProgram().isRegistration()) {
        TrackedEntity entity = event.getEnrollment().getTrackedEntity();
        entity.setLastUpdatedByUserInfo(bundle.getUserInfo());

        teService.updateTrackedEntity(entity);

        Enrollment enrollment = event.getEnrollment();
        enrollment.setLastUpdatedByUserInfo(bundle.getUserInfo());

        enrollment.getEvents().remove(event);
        apiEnrollmentService.updateEnrollment(enrollment);
      }

      deletedEntities.add(objectReport);
      deletedEntities.addAll(deletedRelationships);
    }

    return deletedEntities;
  }

  @Override
  public List<Entity> deleteTrackedEntities(TrackerBundle bundle) {
    List<Entity> deletedEntities = new ArrayList<>();

    List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities =
        bundle.getTrackedEntities();

    for (org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity : trackedEntities) {
      String uid = trackedEntity.getTrackedEntity();

      Entity objectReport = new Entity(TrackerType.TRACKED_ENTITY, uid);

      TrackedEntity entity = teService.getTrackedEntity(uid);
      entity.setLastUpdatedByUserInfo(bundle.getUserInfo());

      Set<Enrollment> daoEnrollments = entity.getEnrollments();

      List<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollments =
          enrollmentTrackerConverterService.to(
              daoEnrollments.stream().filter(enrollment -> !enrollment.isDeleted()).toList());

      List<org.hisp.dhis.tracker.imports.domain.Relationship> relationships =
          relationshipTrackerConverterService.to(
              entity.getRelationshipItems().stream()
                  .map(RelationshipItem::getRelationship)
                  .filter(r -> !r.isDeleted())
                  .toList());

      TrackerBundle trackerBundle =
          TrackerBundle.builder()
              .enrollments(enrollments)
              .relationships(relationships)
              .user(bundle.getUser())
              .userInfo(bundle.getUserInfo())
              .build();

      List<Entity> deleteEnrollments = deleteEnrollments(trackerBundle);
      List<Entity> deleteRelationships = deleteRelationships(trackerBundle);

      teService.deleteTrackedEntity(entity);

      deletedEntities.add(objectReport);
      deletedEntities.addAll(deleteEnrollments);
      deletedEntities.addAll(deleteRelationships);
    }

    return deletedEntities;
  }

  @Override
  public List<Entity> deleteRelationships(TrackerBundle bundle) {
    List<Entity> deletedEntities = new ArrayList<>();

    List<Relationship> relationships = bundle.getRelationships();

    for (Relationship value : relationships) {
      String uid = value.getRelationship();

      Entity objectReport = new Entity(TrackerType.RELATIONSHIP, uid);

      org.hisp.dhis.relationship.Relationship relationship =
          relationshipService.getRelationship(uid);

      relationshipService.deleteRelationship(relationship);

      deletedEntities.add(objectReport);
    }

    return deletedEntities;
  }
}
