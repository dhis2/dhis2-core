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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.audit.AuditOperationType.READ;
import static org.hisp.dhis.audit.AuditOperationType.SEARCH;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.TrackedEntityAggregate;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService")
@RequiredArgsConstructor
class DefaultTrackedEntityService implements TrackedEntityService {

  private final HibernateTrackedEntityStore trackedEntityStore;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackedEntityTypeStore trackedEntityTypeStore;

  private final TrackedEntityAuditService trackedEntityAuditService;

  private final TrackerAccessManager trackerAccessManager;

  private final TrackedEntityAggregate trackedEntityAggregate;

  private final ProgramService programService;

  private final EnrollmentService enrollmentService;

  private final RelationshipService relationshipService;

  private final FileResourceService fileResourceService;

  private final TrackedEntityOperationParamsMapper mapper;

  @Override
  public FileResourceStream getFileResource(
      @Nonnull UID trackedEntity, @Nonnull UID attribute, @CheckForNull UID program)
      throws NotFoundException {
    FileResource fileResource = getFileResourceMetadata(trackedEntity, attribute, program);
    return FileResourceStream.of(fileResourceService, fileResource);
  }

  @Override
  public FileResourceStream getFileResourceImage(
      @Nonnull UID trackedEntity,
      @Nonnull UID attribute,
      @CheckForNull UID program,
      ImageFileDimension dimension)
      throws NotFoundException {
    FileResource fileResource = getFileResourceMetadata(trackedEntity, attribute, program);
    return FileResourceStream.ofImage(fileResourceService, fileResource, dimension);
  }

  private FileResource getFileResourceMetadata(
      UID trackedEntityUid, UID attributeUid, @CheckForNull UID programUid)
      throws NotFoundException {
    TrackedEntity trackedEntity = trackedEntityStore.getByUid(trackedEntityUid.getValue());
    if (trackedEntity == null || trackedEntity.isDeleted()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntityUid.getValue());
    }

    TrackedEntityAttribute attribute = getAttribute(attributeUid, trackedEntity, programUid);
    if (!attribute.getValueType().isFile()) {
      throw new NotFoundException(
          "Tracked entity attribute " + attributeUid.getValue() + " is not a file (or image).");
    }

    String fileResourceUid = null;
    for (TrackedEntityAttributeValue attributeValue :
        trackedEntity.getTrackedEntityAttributeValues()) {
      if (attributeUid.getValue().equals(attributeValue.getAttribute().getUid())) {
        fileResourceUid = attributeValue.getValue();
        break;
      }
    }

    if (fileResourceUid == null) {
      throw new NotFoundException(
          "Attribute value for tracked entity attribute "
              + attributeUid.getValue()
              + " could not be found.");
    }

    return fileResourceService.getExistingFileResource(fileResourceUid);
  }

  /**
   * Tracked entity attributes are fetched from the program if supplied, otherwise from the tracked
   * entities type. Access is determined through the program sharing and ownership if present. If no
   * program is supplied, we fall back to tracked entity type ownership and the registering org unit
   * of the tracked entity.
   */
  private TrackedEntityAttribute getAttribute(
      UID attributeUid, TrackedEntity trackedEntity, @CheckForNull UID programUid)
      throws NotFoundException {
    UserDetails currentUser = getCurrentUserDetails();

    if (programUid != null) {
      Program program = programService.getProgram(programUid.getValue());
      if (program == null) {
        throw new NotFoundException(Program.class, programUid.getValue());
      }

      if (!trackerAccessManager.canRead(currentUser, trackedEntity, program, false).isEmpty()) {
        throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
      }
      return getAttribute(attributeUid, program);
    }

    if (!trackerAccessManager.canRead(currentUser, trackedEntity).isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }
    return getAttribute(attributeUid, trackedEntity.getTrackedEntityType());
  }

  private TrackedEntityAttribute getAttribute(UID attribute, Program program)
      throws NotFoundException {
    Set<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getProgramAttributes(program);
    return getAttribute(attributes, attribute);
  }

  private TrackedEntityAttribute getAttribute(UID attribute, TrackedEntityType trackedEntityType)
      throws NotFoundException {
    Set<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getTrackedEntityTypeAttributes(trackedEntityType);
    return getAttribute(attributes, attribute);
  }

  private static TrackedEntityAttribute getAttribute(
      Set<TrackedEntityAttribute> attributes, UID attribute) throws NotFoundException {
    return attributes.stream()
        .filter(att -> attribute.getValue().equals(att.getUid()))
        .findFirst()
        .orElseThrow(
            () -> new NotFoundException(TrackedEntityAttribute.class, attribute.getValue()));
  }

  @Nonnull
  @Override
  public TrackedEntity getTrackedEntity(@Nonnull UID uid)
      throws NotFoundException, ForbiddenException, BadRequestException {
    return getTrackedEntity(uid, null, TrackedEntityParams.FALSE);
  }

  @Nonnull
  @Override
  public TrackedEntity getTrackedEntity(
      @Nonnull UID trackedEntityUid,
      @CheckForNull UID programIdentifier,
      @Nonnull TrackedEntityParams params)
      throws NotFoundException, ForbiddenException, BadRequestException {
    Program program = null;
    if (programIdentifier != null) {
      program = programService.getProgram(programIdentifier.getValue());
      if (program == null) {
        throw new NotFoundException(Program.class, programIdentifier);
      }
    }

    return getTrackedEntity(trackedEntityUid, program, params, getCurrentUserDetails());
  }

  /**
   * Gets a tracked entity based on the program and org unit ownership.
   *
   * @return the TE object if found and accessible by the current user
   * @throws NotFoundException if uid does not exist
   * @throws ForbiddenException if TE owner is not in user's scope or not enough sharing access
   */
  private TrackedEntity getTrackedEntity(
      UID uid, Program program, TrackedEntityParams params, UserDetails user)
      throws NotFoundException, ForbiddenException, BadRequestException {
    TrackedEntity trackedEntity = trackedEntityStore.getByUid(uid.getValue());
    if (trackedEntity == null || trackedEntity.isDeleted()) {
      throw new NotFoundException(TrackedEntity.class, uid);
    }

    trackedEntityAuditService.addTrackedEntityAudit(READ, user.getUsername(), trackedEntity);

    if (program != null) {
      List<String> errors =
          trackerAccessManager.canReadProgramAndTrackedEntityType(user, trackedEntity, program);
      if (!errors.isEmpty()) {
        throw new ForbiddenException(errors.toString());
      }

      String error =
          trackerAccessManager.canAccessProgramOwner(user, trackedEntity, program, false);
      if (error != null) {
        throw new ForbiddenException(error);
      }
    } else {
      if (!trackerAccessManager.canRead(user, trackedEntity).isEmpty()) {
        throw new ForbiddenException(TrackedEntity.class, uid);
      }
    }

    if (params.isIncludeEnrollments()) {
      EnrollmentOperationParams enrollmentOperationParams =
          mapToEnrollmentParams(uid, program, params);
      List<Enrollment> enrollments = enrollmentService.getEnrollments(enrollmentOperationParams);
      trackedEntity.setEnrollments(new HashSet<>(enrollments));
    }
    if (params.isIncludeRelationships()) {
      trackedEntity.setRelationshipItems(
          relationshipService.getRelationshipItems(
              TrackerType.TRACKED_ENTITY, UID.of(trackedEntity), false));
    }
    if (params.isIncludeProgramOwners()) {
      trackedEntity.setProgramOwners(getTrackedEntityProgramOwners(trackedEntity, program));
    }
    trackedEntity.setTrackedEntityAttributeValues(
        getTrackedEntityAttributeValues(trackedEntity, program));
    return trackedEntity;
  }

  private EnrollmentOperationParams mapToEnrollmentParams(
      UID trackedEntity, Program program, TrackedEntityParams params) {
    return EnrollmentOperationParams.builder()
        .trackedEntity(trackedEntity)
        .program(program)
        .enrollmentParams(params.getEnrollmentParams())
        .build();
  }

  private static Set<TrackedEntityProgramOwner> getTrackedEntityProgramOwners(
      TrackedEntity trackedEntity, Program program) {
    if (program == null) {
      return trackedEntity.getProgramOwners();
    }

    return trackedEntity.getProgramOwners().stream()
        .filter(te -> te.getProgram().getUid().equals(program.getUid()))
        .collect(Collectors.toSet());
  }

  private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      TrackedEntity trackedEntity, Program program) {
    TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
    if (CollectionUtils.isEmpty(trackedEntityType.getTrackedEntityTypeAttributes())) {
      // the TrackedEntityAggregate does not fetch the TrackedEntityTypeAttributes at the moment
      // TODO(DHIS2-18541) bypass ACL as our controller test as the user must have access to the TET
      // if it has access to the TE.
      trackedEntityType =
          trackedEntityTypeStore.getByUidNoAcl(trackedEntity.getTrackedEntityType().getUid());
    }

    Set<String> teas = // tracked entity type attributes
        trackedEntityType.getTrackedEntityAttributes().stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());
    if (program != null) { // add program tracked entity attributes
      teas.addAll(
          program.getTrackedEntityAttributes().stream()
              .map(IdentifiableObject::getUid)
              .collect(Collectors.toSet()));
    }
    return trackedEntity.getTrackedEntityAttributeValues().stream()
        .filter(av -> teas.contains(av.getAttribute().getUid()))
        .collect(Collectors.toSet());
  }

  @Nonnull
  @Override
  public List<TrackedEntity> getTrackedEntities(
      @Nonnull TrackedEntityOperationParams operationParams)
      throws ForbiddenException, NotFoundException, BadRequestException {
    UserDetails user = getCurrentUserDetails();
    TrackedEntityQueryParams queryParams = mapper.map(operationParams, user);
    final List<TrackedEntityIdentifiers> ids = trackedEntityStore.getTrackedEntityIds(queryParams);

    return getTrackedEntities(ids, operationParams, queryParams, user);
  }

  @Override
  public @Nonnull Page<TrackedEntity> getTrackedEntities(
      @Nonnull TrackedEntityOperationParams operationParams, @Nonnull PageParams pageParams)
      throws BadRequestException, ForbiddenException, NotFoundException {
    UserDetails user = getCurrentUserDetails();
    TrackedEntityQueryParams queryParams = mapper.map(operationParams, user);
    final Page<TrackedEntityIdentifiers> ids =
        trackedEntityStore.getTrackedEntityIds(queryParams, pageParams);

    List<TrackedEntity> trackedEntities =
        getTrackedEntities(ids.getItems(), operationParams, queryParams, user);
    return ids.withItems(trackedEntities);
  }

  private List<TrackedEntity> getTrackedEntities(
      List<TrackedEntityIdentifiers> ids,
      TrackedEntityOperationParams operationParams,
      TrackedEntityQueryParams queryParams,
      UserDetails user) {

    List<TrackedEntity> trackedEntities =
        this.trackedEntityAggregate.find(
            ids,
            operationParams.getTrackedEntityParams(),
            queryParams,
            queryParams.getOrgUnitMode());
    for (TrackedEntity trackedEntity : trackedEntities) {
      if (operationParams.getTrackedEntityParams().isIncludeRelationships()) {
        trackedEntity.setRelationshipItems(
            relationshipService.getRelationshipItems(
                TrackerType.TRACKED_ENTITY, UID.of(trackedEntity), queryParams.isIncludeDeleted()));
      }
    }
    for (TrackedEntity trackedEntity : trackedEntities) {
      if (operationParams.getTrackedEntityParams().isIncludeProgramOwners()) {
        trackedEntity.setProgramOwners(
            getTrackedEntityProgramOwners(
                trackedEntity, queryParams.getEnrolledInTrackerProgram()));
      }
      trackedEntity.setTrackedEntityAttributeValues(
          getTrackedEntityAttributeValues(
              trackedEntity, queryParams.getEnrolledInTrackerProgram()));
    }
    trackedEntityAuditService.addTrackedEntityAudit(SEARCH, user.getUsername(), trackedEntities);
    return trackedEntities;
  }

  @Override
  public Set<String> getOrderableFields() {
    return trackedEntityStore.getOrderableFields();
  }
}
