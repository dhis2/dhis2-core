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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service("org.hisp.dhis.tracker.export.enrollment.EnrollmentService")
class DefaultEnrollmentService implements EnrollmentService {
  private final EnrollmentStore enrollmentStore;

  private final EventService eventService;

  private final TrackerOwnershipManager trackerOwnershipAccessManager;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackerAccessManager trackerAccessManager;

  private final EnrollmentOperationParamsMapper paramsMapper;

  @Override
  public Enrollment getEnrollment(@Nonnull UID uid) throws ForbiddenException, NotFoundException {
    return getEnrollment(uid, EnrollmentParams.FALSE, false);
  }

  @Override
  public Enrollment getEnrollment(
      @Nonnull UID uid, @Nonnull EnrollmentParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    UserDetails currentUser = getCurrentUserDetails();
    Enrollment enrollment = enrollmentStore.getByUid(uid.getValue());

    if (enrollment == null) {
      throw new NotFoundException(Enrollment.class, uid);
    }

    List<String> errors = trackerAccessManager.canRead(currentUser, enrollment, false);

    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    return getEnrollment(enrollment, params, includeDeleted, currentUser);
  }

  private Enrollment getEnrollment(
      @Nonnull Enrollment enrollment,
      @Nonnull EnrollmentParams params,
      boolean includeDeleted,
      @Nonnull UserDetails user) {

    Enrollment result = new Enrollment();
    result.setId(enrollment.getId());
    result.setUid(enrollment.getUid());

    if (enrollment.getTrackedEntity() != null) {
      TrackedEntity trackedEntity = new TrackedEntity();
      trackedEntity.setUid(enrollment.getTrackedEntity().getUid());
      result.setTrackedEntity(trackedEntity);
    }
    result.setOrganisationUnit(enrollment.getOrganisationUnit());
    result.setGeometry(enrollment.getGeometry());
    result.setCreated(enrollment.getCreated());
    result.setCreatedAtClient(enrollment.getCreatedAtClient());
    result.setLastUpdated(enrollment.getLastUpdated());
    result.setLastUpdatedAtClient(enrollment.getLastUpdatedAtClient());
    result.setProgram(enrollment.getProgram());
    result.setStatus(enrollment.getStatus());
    result.setEnrollmentDate(enrollment.getEnrollmentDate());
    result.setOccurredDate(enrollment.getOccurredDate());
    result.setFollowup(enrollment.getFollowup());
    result.setCompletedDate(enrollment.getCompletedDate());
    result.setCompletedBy(enrollment.getCompletedBy());
    result.setStoredBy(enrollment.getStoredBy());
    result.setCreatedByUserInfo(enrollment.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(enrollment.getLastUpdatedByUserInfo());
    result.setDeleted(enrollment.isDeleted());
    result.setNotes(enrollment.getNotes());
    if (params.isIncludeEvents()) {
      result.setEvents(
          getEvents(
              enrollment, params.getEnrollmentEventsParams().getEventParams(), includeDeleted));
    }
    if (params.isIncludeRelationships()) {
      result.setRelationshipItems(getRelationshipItems(user, enrollment, includeDeleted));
    }
    if (params.isIncludeAttributes()) {
      result
          .getTrackedEntity()
          .setTrackedEntityAttributeValues(getTrackedEntityAttributeValues(user, enrollment));
    }

    return result;
  }

  @Override
  public RelationshipItem getEnrollmentInRelationshipItem(@Nonnull UID uid, boolean includeDeleted)
      throws NotFoundException {

    RelationshipItem relationshipItem = new RelationshipItem();
    Enrollment enrollment = enrollmentStore.getByUid(uid.getValue());

    if (enrollment == null) {
      throw new NotFoundException(Enrollment.class, uid);
    }

    UserDetails currentUser = getCurrentUserDetails();
    List<String> errors = trackerAccessManager.canRead(currentUser, enrollment, false);
    if (!errors.isEmpty()) {
      return null;
    }

    relationshipItem.setEnrollment(
        getEnrollment(
            enrollment,
            EnrollmentParams.FALSE.withIncludeAttributes(true),
            includeDeleted,
            currentUser));
    return relationshipItem;
  }

  private Set<Event> getEvents(
      Enrollment enrollment, EventParams eventParams, boolean includeDeleted) {
    EventOperationParams eventOperationParams =
        EventOperationParams.builder()
            .enrollments(Set.of(UID.of(enrollment)))
            .eventParams(eventParams)
            .includeDeleted(includeDeleted)
            .build();
    try {
      return Set.copyOf(eventService.getEvents(eventOperationParams));
    } catch (BadRequestException | ForbiddenException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    }
  }

  private Set<RelationshipItem> getRelationshipItems(
      UserDetails user, Enrollment enrollment, boolean includeDeleted) {
    Set<RelationshipItem> relationshipItems = new HashSet<>();

    for (RelationshipItem relationshipItem : enrollment.getRelationshipItems()) {
      org.hisp.dhis.relationship.Relationship daoRelationship = relationshipItem.getRelationship();
      if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
          && (includeDeleted || !daoRelationship.isDeleted())) {
        relationshipItems.add(relationshipItem);
      }
    }

    return relationshipItems;
  }

  private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      UserDetails userDetails, Enrollment enrollment) {
    Set<TrackedEntityAttribute> readableAttributes =
        trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes(
            userDetails, List.of(enrollment.getProgram()), null);
    Set<TrackedEntityAttributeValue> attributeValues = new LinkedHashSet<>();

    for (TrackedEntityAttributeValue trackedEntityAttributeValue :
        enrollment.getTrackedEntity().getTrackedEntityAttributeValues()) {
      if (readableAttributes.contains(trackedEntityAttributeValue.getAttribute())) {
        attributeValues.add(trackedEntityAttributeValue);
      }
    }

    return attributeValues;
  }

  @Override
  public List<Enrollment> getEnrollments(@Nonnull Set<UID> uids) throws ForbiddenException {
    List<Enrollment> enrollments = enrollmentStore.getByUid(UID.toValueList(uids));
    UserDetails user = getCurrentUserDetails();
    List<String> errors =
        enrollments.stream()
            .flatMap(e -> trackerAccessManager.canRead(user, e, false).stream())
            .toList();

    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    return enrollments.stream()
        .map(e -> getEnrollment(e, EnrollmentParams.FALSE, false, user))
        .toList();
  }

  @Override
  public List<Enrollment> getEnrollments(@Nonnull EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());

    return getEnrollments(
        new ArrayList<>(enrollmentStore.getEnrollments(queryParams)),
        params.getEnrollmentParams(),
        params.isIncludeDeleted(),
        queryParams.getOrganisationUnitMode());
  }

  @Override
  public Page<Enrollment> getEnrollments(
      @Nonnull EnrollmentOperationParams params, PageParams pageParams)
      throws ForbiddenException, BadRequestException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());

    Page<Enrollment> enrollmentsPage = enrollmentStore.getEnrollments(queryParams, pageParams);
    List<Enrollment> enrollments =
        getEnrollments(
            enrollmentsPage.getItems(),
            params.getEnrollmentParams(),
            params.isIncludeDeleted(),
            queryParams.getOrganisationUnitMode());
    return enrollmentsPage.withItems(enrollments);
  }

  private List<Enrollment> getEnrollments(
      Iterable<Enrollment> enrollments,
      EnrollmentParams params,
      boolean includeDeleted,
      OrganisationUnitSelectionMode orgUnitMode) {
    List<Enrollment> enrollmentList = new ArrayList<>();
    UserDetails currentUser = getCurrentUserDetails();

    for (Enrollment enrollment : enrollments) {
      if (enrollment != null
          && (orgUnitMode == ALL
              || trackerOwnershipAccessManager.hasAccess(
                  currentUser, enrollment.getTrackedEntity(), enrollment.getProgram()))
          && trackerAccessManager.canRead(currentUser, enrollment, orgUnitMode == ALL).isEmpty()) {
        enrollmentList.add(getEnrollment(enrollment, params, includeDeleted, currentUser));
      }
    }

    return enrollmentList;
  }

  @Override
  public Set<String> getOrderableFields() {
    return enrollmentStore.getOrderableFields();
  }
}
