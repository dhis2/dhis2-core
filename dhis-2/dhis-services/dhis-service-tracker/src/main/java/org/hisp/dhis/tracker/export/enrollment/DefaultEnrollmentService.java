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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service("org.hisp.dhis.tracker.export.enrollment.EnrollmentService")
class DefaultEnrollmentService
    implements org.hisp.dhis.tracker.export.enrollment.EnrollmentService {
  private final EnrollmentStore enrollmentStore;

  private final AclService aclService;

  private final TrackerOwnershipManager trackerOwnershipAccessManager;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final UserService userService;

  private final TrackerAccessManager trackerAccessManager;

  private final EnrollmentOperationParamsMapper paramsMapper;

  @Override
  public Enrollment getEnrollment(String uid, EnrollmentParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    Enrollment enrollment = enrollmentStore.getByUid(uid);

    if (enrollment == null) {
      throw new NotFoundException(Enrollment.class, uid);
    }

    return getEnrollment(enrollment, params, includeDeleted);
  }

  @Override
  public Enrollment getEnrollment(
      @Nonnull Enrollment enrollment, EnrollmentParams params, boolean includeDeleted)
      throws ForbiddenException {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    List<String> errors = trackerAccessManager.canRead(currentUser, enrollment, false);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

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
      result.setEvents(getEvents(currentUser, enrollment, includeDeleted));
    }
    if (params.isIncludeRelationships()) {
      result.setRelationshipItems(getRelationshipItems(currentUser, enrollment, includeDeleted));
    }
    if (params.isIncludeAttributes()) {
      result
          .getTrackedEntity()
          .setTrackedEntityAttributeValues(
              getTrackedEntityAttributeValues(currentUser, enrollment));
    }

    return result;
  }

  private Set<Event> getEvents(User user, Enrollment enrollment, boolean includeDeleted) {
    Set<Event> events = new HashSet<>();

    for (Event event : enrollment.getEvents()) {
      if ((includeDeleted || !event.isDeleted())
          && trackerAccessManager.canRead(user, event, true).isEmpty()) {
        events.add(event);
      }
    }
    return events;
  }

  private Set<RelationshipItem> getRelationshipItems(
      User user, Enrollment enrollment, boolean includeDeleted) {
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
      User user, Enrollment enrollment) {
    Set<TrackedEntityAttribute> readableAttributes =
        trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes(
            user.getUsername(), List.of(enrollment.getProgram()), null);
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
  public List<Enrollment> getEnrollments(EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params);

    decideAccess(queryParams);
    validate(queryParams);

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (currentUser != null
        && queryParams.isOrganisationUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)) {
      queryParams.setOrganisationUnits(currentUser.getTeiSearchOrganisationUnitsWithFallback());
      queryParams.setOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    } else if (currentUser != null && queryParams.isOrganisationUnitMode(CAPTURE)) {
      queryParams.setOrganisationUnits(currentUser.getOrganisationUnits());
      queryParams.setOrganisationUnitMode(DESCENDANTS);
    }

    return getEnrollments(
        new ArrayList<>(enrollmentStore.getEnrollments(queryParams)),
        params.getEnrollmentParams(),
        params.isIncludeDeleted());
  }

  @Override
  public Page<Enrollment> getEnrollments(EnrollmentOperationParams params, PageParams pageParams)
      throws ForbiddenException, BadRequestException, NotFoundException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params);

    decideAccess(queryParams);
    validate(queryParams);

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (currentUser != null
        && queryParams.isOrganisationUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)) {
      queryParams.setOrganisationUnits(currentUser.getTeiSearchOrganisationUnitsWithFallback());
      queryParams.setOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    } else if (currentUser != null && queryParams.isOrganisationUnitMode(CAPTURE)) {
      queryParams.setOrganisationUnits(currentUser.getOrganisationUnits());
      queryParams.setOrganisationUnitMode(DESCENDANTS);
    } else if (queryParams.isOrganisationUnitMode(CHILDREN)) {
      Set<OrganisationUnit> organisationUnits = new HashSet<>(queryParams.getOrganisationUnits());

      for (OrganisationUnit organisationUnit : queryParams.getOrganisationUnits()) {
        organisationUnits.addAll(organisationUnit.getChildren());
      }

      queryParams.setOrganisationUnits(organisationUnits);
    }

    Page<Enrollment> enrollmentsPage = enrollmentStore.getEnrollments(queryParams, pageParams);
    List<Enrollment> enrollments =
        getEnrollments(
            enrollmentsPage.getItems(), params.getEnrollmentParams(), params.isIncludeDeleted());

    return Page.of(enrollments, enrollmentsPage.getPager());
  }

  public void decideAccess(EnrollmentQueryParams params) {
    if (params.hasProgram()) {
      if (!aclService.canDataRead(params.getUser().getUsername(), params.getProgram())) {
        throw new IllegalQueryException(
            "Current user is not authorized to read data from selected program:  "
                + params.getProgram().getUid());
      }

      if (params.getProgram().getTrackedEntityType() != null
          && !aclService.canDataRead(
              params.getUser().getUsername(), params.getProgram().getTrackedEntityType())) {
        throw new IllegalQueryException(
            "Current user is not authorized to read data from selected program's tracked entity type:  "
                + params.getProgram().getTrackedEntityType().getUid());
      }
    }

    if (params.hasTrackedEntityType()
        && !aclService.canDataRead(params.getUser().getUsername(), params.getTrackedEntityType())) {
      throw new IllegalQueryException(
          "Current user is not authorized to read data from selected tracked entity type:  "
              + params.getTrackedEntityType().getUid());
    }
  }

  public void validate(EnrollmentQueryParams params) throws IllegalQueryException {
    String violation = null;

    if (params == null) {
      throw new IllegalQueryException("Params cannot be null");
    }

    if (params.hasProgram() && params.hasTrackedEntityType()) {
      violation = "Program and tracked entity cannot be specified simultaneously";
    }

    if (params.hasProgramStatus() && !params.hasProgram()) {
      violation = "Program must be defined when program status is defined";
    }

    if (params.hasFollowUp() && !params.hasProgram()) {
      violation = "Program must be defined when follow up status is defined";
    }

    if (params.hasProgramStartDate() && !params.hasProgram()) {
      violation = "Program must be defined when program start date is specified";
    }

    if (params.hasProgramEndDate() && !params.hasProgram()) {
      violation = "Program must be defined when program end date is specified";
    }

    if (params.hasLastUpdated() && params.hasLastUpdatedDuration()) {
      violation = "Last updated and last updated duration cannot be specified simultaneously";
    }

    if (params.hasLastUpdatedDuration()
        && DateUtils.getDuration(params.getLastUpdatedDuration()) == null) {
      violation = "Duration is not valid: " + params.getLastUpdatedDuration();
    }

    if (violation != null) {
      log.warn("Validation failed: " + violation);

      throw new IllegalQueryException(violation);
    }
  }

  private List<Enrollment> getEnrollments(
      Iterable<Enrollment> enrollments, EnrollmentParams params, boolean includeDeleted)
      throws ForbiddenException {
    List<Enrollment> enrollmentList = new ArrayList<>();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    for (Enrollment enrollment : enrollments) {
      if (enrollment != null
          && trackerOwnershipAccessManager.hasAccess(
              currentUser, enrollment.getTrackedEntity(), enrollment.getProgram())) {
        enrollmentList.add(getEnrollment(enrollment, params, includeDeleted));
      }
    }

    return enrollmentList;
  }

  @Override
  public Set<String> getOrderableFields() {
    return enrollmentStore.getOrderableFields();
  }
}
