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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.deprecated.tracker.RelationshipParams;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.TrackedEntityInstanceStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractTrackedEntityInstanceService implements TrackedEntityInstanceService {
  protected TrackedEntityInstanceStore trackedEntityInstanceStore;

  protected TrackedEntityService teiService;

  protected TrackedEntityAttributeService trackedEntityAttributeService;

  protected TrackedEntityTypeService trackedEntityTypeService;

  protected RelationshipService _relationshipService;

  protected org.hisp.dhis.dxf2.deprecated.tracker.relationship.RelationshipService
      relationshipService;

  protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  protected IdentifiableObjectManager manager;

  protected UserService userService;

  protected DbmsManager dbmsManager;

  protected org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService enrollmentService;

  protected EnrollmentService programInstanceService;

  protected TrackedEntityChangeLogService trackedEntityChangeLogService;

  protected SchemaService schemaService;

  protected QueryService queryService;

  protected ReservedValueService reservedValueService;

  protected TrackerAccessManager trackerAccessManager;

  protected FileResourceService fileResourceService;

  protected TrackerOwnershipManager trackerOwnershipAccessManager;

  protected Notifier notifier;

  protected TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

  protected TrackedEntityAttributeStore trackedEntityAttributeStore;

  protected ObjectMapper jsonMapper;

  protected ObjectMapper xmlMapper;

  private final CachingMap<String, Program> programCache = new CachingMap<>();

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityInstance> getTrackedEntityInstances(
      TrackedEntityQueryParams queryParams,
      TrackedEntityInstanceParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    if (queryParams == null) {
      return Collections.emptyList();
    }
    List<TrackedEntityInstance> trackedEntityInstances;

    final List<Long> ids =
        teiService.getTrackedEntityIds(
            queryParams, skipAccessValidation, skipSearchScopeValidation);

    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    trackedEntityInstances = this.trackedEntityInstanceAggregate.find(ids, params, queryParams);

    addSearchAudit(trackedEntityInstances, queryParams.getUser());

    return trackedEntityInstances;
  }

  private void addSearchAudit(List<TrackedEntityInstance> trackedEntityInstances, User user) {
    if (trackedEntityInstances.isEmpty()) {
      return;
    }
    final String accessedBy =
        user != null ? user.getUsername() : CurrentUserUtil.getCurrentUsername();
    Map<String, TrackedEntityType> tetMap =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .collect(Collectors.toMap(TrackedEntityType::getUid, t -> t));

    List<TrackedEntityChangeLog> auditable =
        trackedEntityInstances.stream()
            .filter(Objects::nonNull)
            .filter(tei -> tei.getTrackedEntityType() != null)
            .filter(tei -> tetMap.get(tei.getTrackedEntityType()).isAllowAuditLog())
            .map(
                tei ->
                    new TrackedEntityChangeLog(
                        tei.getTrackedEntityInstance(), accessedBy, ChangeLogType.SEARCH))
            .collect(Collectors.toList());

    if (!auditable.isEmpty()) {
      trackedEntityChangeLogService.addTrackedEntityChangeLog(auditable);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public int getTrackedEntityInstanceCount(
      TrackedEntityQueryParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    return teiService.getTrackedEntityCount(
        params, skipAccessValidation, skipSearchScopeValidation);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityInstance getTrackedEntityInstance(
      TrackedEntity daoTrackedEntity, TrackedEntityInstanceParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    return getTrackedEntityInstance(daoTrackedEntity, params, currentUser);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityInstance getTrackedEntityInstance(
      TrackedEntity daoTrackedEntity, TrackedEntityInstanceParams params, UserDetails user) {
    if (daoTrackedEntity == null) {
      return null;
    }

    List<String> errors = trackerAccessManager.canRead(user, daoTrackedEntity);

    if (!errors.isEmpty()) {
      throw new IllegalQueryException(errors.toString());
    }

    Set<TrackedEntityAttribute> readableAttributes =
        trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes(user);

    return getTei(daoTrackedEntity, readableAttributes, params, user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityOuInfo> getTrackedEntityOuInfoByUid(List<String> uids) {
    if (uids == null || uids.isEmpty()) {
      return Collections.emptyList();
    }
    return trackedEntityInstanceStore.getTrackedEntityOuInfoByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityProgramOwnerIds> getTrackedEntityProgramOwnersUidsUsingId(
      List<Long> teiIds, Program program) {
    if (teiIds.isEmpty()) {
      return Collections.emptyList();
    }
    return trackedEntityInstanceStore.getTrackedEntityProgramOwnersUids(teiIds, program.getId());
  }

  @Override
  @Transactional
  public void updateTrackedEntityInstancesSyncTimestamp(
      List<String> entityInstanceUIDs, Date lastSynced) {
    teiService.updateTrackedEntitySyncTimestamp(entityInstanceUIDs, lastSynced);
  }

  private Program getProgram(IdSchemes idSchemes, String id) {
    if (id == null) {
      return null;
    }

    return programCache.get(
        id, () -> manager.getObject(Program.class, idSchemes.getProgramIdScheme(), id));
  }

  private TrackedEntityInstance getTei(
      TrackedEntity daoTrackedEntity,
      Set<TrackedEntityAttribute> readableAttributes,
      TrackedEntityInstanceParams params,
      UserDetails user) {
    if (daoTrackedEntity == null) {
      return null;
    }

    TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
    trackedEntityInstance.setTrackedEntityInstance(daoTrackedEntity.getUid());
    trackedEntityInstance.setOrgUnit(daoTrackedEntity.getOrganisationUnit().getUid());
    trackedEntityInstance.setTrackedEntityType(daoTrackedEntity.getTrackedEntityType().getUid());
    trackedEntityInstance.setCreated(DateUtils.toIso8601NoTz(daoTrackedEntity.getCreated()));
    trackedEntityInstance.setCreatedAtClient(
        DateUtils.toIso8601NoTz(daoTrackedEntity.getCreatedAtClient()));
    trackedEntityInstance.setLastUpdated(
        DateUtils.toIso8601NoTz(daoTrackedEntity.getLastUpdated()));
    trackedEntityInstance.setLastUpdatedAtClient(
        DateUtils.toIso8601NoTz(daoTrackedEntity.getLastUpdatedAtClient()));
    trackedEntityInstance.setInactive(
        Optional.ofNullable(daoTrackedEntity.isInactive()).orElse(false));
    trackedEntityInstance.setGeometry(daoTrackedEntity.getGeometry());
    trackedEntityInstance.setDeleted(daoTrackedEntity.isDeleted());
    trackedEntityInstance.setPotentialDuplicate(daoTrackedEntity.isPotentialDuplicate());
    trackedEntityInstance.setStoredBy(daoTrackedEntity.getStoredBy());
    trackedEntityInstance.setCreatedByUserInfo(daoTrackedEntity.getCreatedByUserInfo());
    trackedEntityInstance.setLastUpdatedByUserInfo(daoTrackedEntity.getLastUpdatedByUserInfo());

    if (daoTrackedEntity.getGeometry() != null) {
      Geometry geometry = daoTrackedEntity.getGeometry();
      FeatureType featureType = FeatureType.getTypeFromName(geometry.getGeometryType());
      trackedEntityInstance.setFeatureType(featureType);
      trackedEntityInstance.setCoordinates(GeoUtils.getCoordinatesFromGeometry(geometry));
    }

    if (params.isIncludeRelationships()) {
      for (RelationshipItem relationshipItem : daoTrackedEntity.getRelationshipItems()) {
        org.hisp.dhis.relationship.Relationship daoRelationship =
            relationshipItem.getRelationship();

        if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
            && (params.isIncludeDeleted() || !daoRelationship.isDeleted())) {
          Optional<Relationship> relationship =
              relationshipService.findRelationship(
                  relationshipItem.getRelationship(), RelationshipParams.FALSE, user);
          relationship.ifPresent(r -> trackedEntityInstance.getRelationships().add(r));
        }
      }
    }

    if (params.isIncludeEnrollments()) {
      for (Enrollment enrollment : daoTrackedEntity.getEnrollments()) {
        if (trackerAccessManager.canRead(user, enrollment, false).isEmpty()
            && (params.isIncludeDeleted() || !enrollment.isDeleted())) {
          trackedEntityInstance
              .getEnrollments()
              .add(
                  enrollmentService.getEnrollment(
                      user, enrollment, params.getEnrollmentParams(), true));
        }
      }
    }

    if (params.isIncludeProgramOwners()) {
      for (TrackedEntityProgramOwner programOwner : daoTrackedEntity.getProgramOwners()) {
        trackedEntityInstance.getProgramOwners().add(new ProgramOwner(programOwner));
      }
    }

    Set<TrackedEntityAttribute> readableAttributesCopy =
        filterOutSkipSyncAttributesIfApplies(params, trackedEntityInstance, readableAttributes);

    for (TrackedEntityAttributeValue attributeValue :
        daoTrackedEntity.getTrackedEntityAttributeValues()) {
      if (readableAttributesCopy.contains(attributeValue.getAttribute())) {
        Attribute attribute = new Attribute();

        attribute.setCreated(DateUtils.toIso8601NoTz(attributeValue.getCreated()));
        attribute.setLastUpdated(DateUtils.toIso8601NoTz(attributeValue.getLastUpdated()));
        attribute.setDisplayName(attributeValue.getAttribute().getDisplayName());
        attribute.setAttribute(attributeValue.getAttribute().getUid());
        attribute.setValueType(attributeValue.getAttribute().getValueType());
        attribute.setCode(attributeValue.getAttribute().getCode());
        attribute.setValue(attributeValue.getValue());
        attribute.setStoredBy(attributeValue.getStoredBy());
        attribute.setSkipSynchronization(attributeValue.getAttribute().getSkipSynchronization());

        trackedEntityInstance.getAttributes().add(attribute);
      }
    }

    return trackedEntityInstance;
  }

  private Set<TrackedEntityAttribute> filterOutSkipSyncAttributesIfApplies(
      TrackedEntityInstanceParams params,
      TrackedEntityInstance trackedEntityInstance,
      Set<TrackedEntityAttribute> readableAttributes) {
    Set<TrackedEntityAttribute> readableAttributesCopy;

    if (params.isDataSynchronizationQuery()) {
      List<String> programs =
          trackedEntityInstance.getEnrollments().stream()
              .map(org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment::getProgram)
              .collect(Collectors.toList());

      readableAttributesCopy =
          readableAttributes.stream()
              .filter(att -> !att.getSkipSynchronization())
              .collect(Collectors.toSet());

      IdSchemes idSchemes = new IdSchemes();
      for (String programUid : programs) {
        Program program = getProgram(idSchemes, programUid);
        if (program != null) {
          readableAttributesCopy.addAll(
              program.getTrackedEntityAttributes().stream()
                  .filter(att -> !att.getSkipSynchronization())
                  .collect(Collectors.toSet()));
        }
      }
    } else {
      readableAttributesCopy = new HashSet<>(readableAttributes);
    }

    return readableAttributesCopy;
  }
}
