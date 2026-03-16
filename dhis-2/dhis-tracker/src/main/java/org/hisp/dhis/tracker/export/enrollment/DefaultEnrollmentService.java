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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.audit.AuditOperationType.READ;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.acl.TrackerProgramService;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service("org.hisp.dhis.tracker.export.enrollment.EnrollmentService")
class DefaultEnrollmentService implements EnrollmentService {
  private final JdbcEnrollmentStore enrollmentStore;

  private final TrackerEventService trackerEventService;

  private final RelationshipService relationshipService;

  private final TrackerOwnershipManager trackerOwnershipManager;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackerAccessManager trackerAccessManager;

  private final EnrollmentOperationParamsMapper paramsMapper;

  private final TrackedEntityAuditService trackedEntityAuditService;

  private final TrackerProgramService trackerProgramService;

  @Nonnull
  @Override
  public Optional<Enrollment> findEnrollment(@Nonnull UID uid) {
    try {
      return Optional.of(getEnrollment(uid));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @Override
  public Enrollment getEnrollment(@Nonnull UID uid) throws NotFoundException {
    return getEnrollment(uid, EnrollmentFields.none());
  }

  @Nonnull
  @Override
  public Enrollment getEnrollment(@Nonnull UID uid, @Nonnull EnrollmentFields fields)
      throws NotFoundException {
    Page<Enrollment> enrollments;
    try {
      EnrollmentOperationParams operationParams =
          EnrollmentOperationParams.builder().enrollments(Set.of(uid)).fields(fields).build();
      enrollments = findEnrollments(operationParams, PageParams.single());
    } catch (BadRequestException | ForbiddenException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EnrollmentOperationParams are built");
    }

    if (enrollments.getItems().isEmpty()) {
      throw new NotFoundException(Enrollment.class, uid);
    }

    return enrollments.getItems().get(0);
  }

  @Nonnull
  @Override
  public List<Enrollment> findEnrollments(@Nonnull Set<UID> uids) throws ForbiddenException {
    if (uids.isEmpty()) {
      return List.of();
    }

    try {
      return findEnrollments(EnrollmentOperationParams.builder().enrollments(uids).build());
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EnrollmentOperationParams are built");
    }
  }

  @Nonnull
  @Override
  public List<Enrollment> findEnrollments(@Nonnull EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());

    List<Enrollment> enrollments =
        mapEnrollment(
            new ArrayList<>(enrollmentStore.getEnrollments(queryParams)),
            params.getFields(),
            params.isIncludeDeleted());

    addTrackedEntityAudit(queryParams.getTrackedEntities(), enrollments);

    return enrollments;
  }

  @Nonnull
  @Override
  public Page<Enrollment> findEnrollments(
      @Nonnull EnrollmentOperationParams params, PageParams pageParams)
      throws ForbiddenException, BadRequestException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());

    Page<Enrollment> enrollmentsPage = enrollmentStore.getEnrollments(queryParams, pageParams);
    List<Enrollment> enrollments =
        mapEnrollment(enrollmentsPage.getItems(), params.getFields(), params.isIncludeDeleted());

    addTrackedEntityAudit(queryParams.getTrackedEntities(), enrollments);

    return enrollmentsPage.withFilteredItems(enrollments);
  }

  /**
   * Adds audit entry for tracked entity read. Only audits when a single tracked entity is requested
   * to avoid duplicate audits when called from /trackedEntities (which audits its own results).
   */
  private void addTrackedEntityAudit(Set<UID> trackedEntities, List<Enrollment> enrollments) {
    if (trackedEntities.size() == 1 && !enrollments.isEmpty()) {
      trackedEntityAuditService.addTrackedEntityAudit(
          READ, getCurrentUserDetails().getUsername(), enrollments.get(0).getTrackedEntity());
    }
  }

  private Enrollment addRequestedFields(
      @Nonnull Enrollment enrollment,
      @Nonnull EnrollmentFields fields,
      boolean includeDeleted,
      Map<Program, Set<String>> readableAttributesByProgram,
      Map<String, Set<TrackerEvent>> eventsByEnrollment) {

    if (fields.isIncludesEvents()) {
      enrollment.setEvents(eventsByEnrollment.getOrDefault(enrollment.getUid(), Set.of()));
    }
    if (fields.isIncludesRelationships()) {
      enrollment.setRelationshipItems(
          relationshipService.findRelationshipItems(
              TrackerType.ENROLLMENT,
              enrollment.getUID(),
              fields.getRelationshipFields(),
              includeDeleted));
    }
    if (fields.isIncludesAttributes()) {
      Set<String> readableAttributes =
          readableAttributesByProgram.getOrDefault(enrollment.getProgram(), Set.of());
      enrollment
          .getTrackedEntity()
          .setTrackedEntityAttributeValues(filterAttributeValues(enrollment, readableAttributes));
    }

    return enrollment;
  }

  private Set<TrackedEntityAttributeValue> filterAttributeValues(
      Enrollment enrollment, Set<String> readableAttributes) {
    Set<TrackedEntityAttributeValue> attributeValues = new LinkedHashSet<>();
    for (TrackedEntityAttributeValue trackedEntityAttributeValue :
        enrollment.getTrackedEntity().getTrackedEntityAttributeValues()) {
      if (readableAttributes.contains(trackedEntityAttributeValue.getAttribute().getUid())) {
        attributeValues.add(trackedEntityAttributeValue);
      }
    }
    return attributeValues;
  }

  private List<Enrollment> mapEnrollment(
      List<Enrollment> enrollments, EnrollmentFields fields, boolean includeDeleted) {
    // Prefetch readable attributes per program to avoid N+1 queries
    Map<Program, Set<String>> readableAttributesByProgram =
        getReadableAttributesByProgram(enrollments, fields);

    // Prefetch events for all enrollments to avoid N+1 queries
    Map<String, Set<TrackerEvent>> eventsByEnrollment =
        getEventsByEnrollment(enrollments, fields, includeDeleted);

    return enrollments.stream()
        .map(
            e ->
                addRequestedFields(
                    e, fields, includeDeleted, readableAttributesByProgram, eventsByEnrollment))
        .toList();
  }

  /**
   * Fetches readable attribute UIDs per program. Each program defines its own set of attributes, so
   * we must query per program (not in batch) to filter correctly per enrollment.
   */
  private Map<Program, Set<String>> getReadableAttributesByProgram(
      List<Enrollment> enrollments, EnrollmentFields fields) {
    if (!fields.isIncludesAttributes()) {
      return Map.of();
    }

    Map<Program, Set<String>> result = new HashMap<>();
    for (Enrollment enrollment : enrollments) {
      result.computeIfAbsent(
          enrollment.getProgram(),
          p ->
              trackedEntityAttributeService
                  .getAllUserReadableTrackedEntityAttributes(List.of(p), null)
                  .stream()
                  .map(BaseIdentifiableObject::getUid)
                  .collect(Collectors.toSet()));
    }
    return result;
  }

  private Map<String, Set<TrackerEvent>> getEventsByEnrollment(
      List<Enrollment> enrollments, EnrollmentFields fields, boolean includeDeleted) {
    if (!fields.isIncludesEvents() || enrollments.isEmpty()) {
      return Map.of();
    }

    Set<UID> enrollmentUids =
        enrollments.stream().map(Enrollment::getUID).collect(Collectors.toSet());

    TrackerEventOperationParams eventOperationParams =
        TrackerEventOperationParams.builderForEnrollments(enrollmentUids)
            .fields(fields.getEventFields())
            .includeDeleted(includeDeleted)
            .build();

    List<TrackerEvent> allEvents;
    try {
      allEvents = trackerEventService.findEvents(eventOperationParams);
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    } catch (ForbiddenException e) {
      // User doesn't have access to events - return empty map
      return Map.of();
    }

    // Group events by enrollment UID
    Map<String, Set<TrackerEvent>> result = new HashMap<>(enrollments.size());
    for (TrackerEvent event : allEvents) {
      result.computeIfAbsent(event.getEnrollment().getUid(), k -> new HashSet<>()).add(event);
    }
    return result;
  }

  @Override
  public Set<String> getOrderableFields() {
    return enrollmentStore.getOrderableFields();
  }
}
