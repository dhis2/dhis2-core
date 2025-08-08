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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.audit.AuditOperationType.SEARCH;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.TrackedEntityAggregate;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService")
@RequiredArgsConstructor
class DefaultTrackedEntityService implements TrackedEntityService {

  private final HibernateTrackedEntityStore trackedEntityStore;

  private final TrackedEntityAuditService trackedEntityAuditService;

  private final TrackedEntityAggregate trackedEntityAggregate;

  private final RelationshipService relationshipService;

  private final FileResourceService fileResourceService;

  private final OperationsParamsValidator operationsParamsValidator;

  private final TrackedEntityOperationParamsMapper mapper;

  @Override
  public FileResourceStream getFileResource(
      @Nonnull UID trackedEntity, @Nonnull UID attribute, @CheckForNull UID program)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = getFileResourceMetadata(trackedEntity, attribute, program);
    return FileResourceStream.of(fileResourceService, fileResource);
  }

  @Override
  public FileResourceStream getFileResourceImage(
      @Nonnull UID trackedEntity,
      @Nonnull UID attribute,
      @CheckForNull UID program,
      ImageFileDimension dimension)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = getFileResourceMetadata(trackedEntity, attribute, program);
    return FileResourceStream.ofImage(fileResourceService, fileResource, dimension);
  }

  private FileResource getFileResourceMetadata(
      UID trackedEntityUid, UID attributeUid, @CheckForNull UID programUid)
      throws NotFoundException, ForbiddenException {
    TrackedEntity trackedEntity =
        getTrackedEntity(
            trackedEntityUid,
            programUid,
            TrackedEntityFields.builder().includeAttributes().build());

    TrackedEntityAttribute attribute =
        trackedEntity.getTrackedEntityAttributeValues().stream()
            .map(TrackedEntityAttributeValue::getAttribute)
            .filter(att -> att.getUid().equals(attributeUid.getValue()))
            .findFirst()
            .orElseThrow(
                () -> new NotFoundException(TrackedEntityAttribute.class, attributeUid.getValue()));
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

  @Nonnull
  @Override
  public TrackedEntity getTrackedEntity(@Nonnull UID uid)
      throws NotFoundException, ForbiddenException {
    return getTrackedEntity(uid, (Program) null, TrackedEntityFields.none());
  }

  @Nonnull
  @Override
  public Optional<TrackedEntity> findTrackedEntity(@Nonnull UID uid) {
    try {
      return Optional.of(getTrackedEntity(uid, (Program) null, TrackedEntityFields.none()));
    } catch (NotFoundException | ForbiddenException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @Override
  public TrackedEntity getTrackedEntity(
      @Nonnull UID trackedEntityUid,
      @CheckForNull UID programIdentifier,
      @Nonnull TrackedEntityFields fields)
      throws NotFoundException, ForbiddenException {
    Program program = null;
    if (programIdentifier != null) {
      try {
        program =
            operationsParamsValidator.validateTrackerProgram(
                programIdentifier, CurrentUserUtil.getCurrentUserDetails());
      } catch (BadRequestException e) {
        throw new NotFoundException(Program.class, programIdentifier.getValue());
      }
    }

    return getTrackedEntity(trackedEntityUid, program, fields);
  }

  private TrackedEntity getTrackedEntity(UID uid, Program program, TrackedEntityFields fields)
      throws NotFoundException, ForbiddenException {
    Page<TrackedEntity> trackedEntities;
    try {
      TrackedEntityOperationParams operationParams =
          TrackedEntityOperationParams.builder()
              .trackedEntities(Set.of(uid))
              .fields(fields)
              .program(program)
              .build();
      trackedEntities = findTrackedEntities(operationParams, PageParams.single());
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the TrackedEntityOperationParams are built");
    }

    if (trackedEntities.getItems().isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, uid);
    }

    return trackedEntities.getItems().get(0);
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

  @Nonnull
  @Override
  public List<TrackedEntity> findTrackedEntities(
      @Nonnull TrackedEntityOperationParams operationParams)
      throws ForbiddenException, BadRequestException {
    UserDetails user = getCurrentUserDetails();
    TrackedEntityQueryParams queryParams = mapper.map(operationParams, user);
    final List<TrackedEntityIdentifiers> ids = trackedEntityStore.getTrackedEntityIds(queryParams);

    return findTrackedEntities(ids, operationParams, queryParams, user);
  }

  @Nonnull
  @Override
  public Page<TrackedEntity> findTrackedEntities(
      @Nonnull TrackedEntityOperationParams operationParams, @Nonnull PageParams pageParams)
      throws BadRequestException, ForbiddenException {
    UserDetails user = getCurrentUserDetails();
    TrackedEntityQueryParams queryParams = mapper.map(operationParams, user, pageParams);
    final Page<TrackedEntityIdentifiers> ids =
        trackedEntityStore.getTrackedEntityIds(queryParams, pageParams);

    List<TrackedEntity> trackedEntities =
        findTrackedEntities(ids.getItems(), operationParams, queryParams, user);

    return ids.withFilteredItems(trackedEntities);
  }

  private List<TrackedEntity> findTrackedEntities(
      List<TrackedEntityIdentifiers> ids,
      TrackedEntityOperationParams operationParams,
      TrackedEntityQueryParams queryParams,
      UserDetails user) {

    List<TrackedEntity> trackedEntities =
        this.trackedEntityAggregate.find(ids, operationParams.getFields(), queryParams);
    for (TrackedEntity trackedEntity : trackedEntities) {
      if (operationParams.getFields().isIncludesRelationships()) {
        trackedEntity.setRelationshipItems(
            relationshipService.findRelationshipItems(
                TrackerType.TRACKED_ENTITY,
                UID.of(trackedEntity),
                operationParams.getFields().getRelationshipFields(),
                queryParams.isIncludeDeleted()));
      }
    }
    for (TrackedEntity trackedEntity : trackedEntities) {
      if (operationParams.getFields().isIncludesProgramOwners()) {
        trackedEntity.setProgramOwners(
            getTrackedEntityProgramOwners(
                trackedEntity, queryParams.getEnrolledInTrackerProgram()));
      }
    }
    trackedEntityAuditService.addTrackedEntityAudit(SEARCH, user.getUsername(), trackedEntities);

    return trackedEntities;
  }

  @Override
  public Set<String> getOrderableFields() {
    return trackedEntityStore.getOrderableFields();
  }
}
