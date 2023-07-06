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
package org.hisp.dhis.dxf2.events.relationship;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.EnrollmentParams;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.RelationshipParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractRelationshipService implements RelationshipService {
  protected DbmsManager dbmsManager;

  protected CurrentUserService currentUserService;

  protected SchemaService schemaService;

  protected QueryService queryService;

  protected TrackerAccessManager trackerAccessManager;

  protected org.hisp.dhis.relationship.RelationshipService relationshipService;

  protected TrackedEntityInstanceService trackedEntityInstanceService;

  protected EnrollmentService enrollmentService;

  protected EventService eventService;

  protected org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiDaoService;

  protected UserService userService;

  protected ObjectMapper jsonMapper;

  protected ObjectMapper xmlMapper;

  private HashMap<String, RelationshipType> relationshipTypeCache = new HashMap<>();

  private HashMap<String, TrackedEntityInstance> trackedEntityInstanceCache = new HashMap<>();

  private HashMap<String, ProgramInstance> programInstanceCache = new HashMap<>();

  private HashMap<String, ProgramStageInstance> programStageInstanceCache = new HashMap<>();

  @Override
  @Transactional(readOnly = true)
  public List<Relationship> getRelationshipsByTrackedEntityInstance(
      TrackedEntityInstance tei,
      PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
      boolean skipAccessValidation) {
    User user = currentUserService.getCurrentUser();

    return relationshipService
        .getRelationshipsByTrackedEntityInstance(
            tei, pagingAndSortingCriteriaAdapter, skipAccessValidation)
        .stream()
        .filter((r) -> !skipAccessValidation && trackerAccessManager.canRead(user, r).isEmpty())
        .map(r -> getRelationship(r, user))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Relationship> getRelationshipsByProgramInstance(
      ProgramInstance pi,
      PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
      boolean skipAccessValidation) {
    User user = currentUserService.getCurrentUser();

    return relationshipService
        .getRelationshipsByProgramInstance(
            pi, pagingAndSortingCriteriaAdapter, skipAccessValidation)
        .stream()
        .filter((r) -> !skipAccessValidation && trackerAccessManager.canRead(user, r).isEmpty())
        .map(r -> getRelationship(r, user))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Relationship> getRelationshipsByProgramStageInstance(
      ProgramStageInstance psi,
      PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
      boolean skipAccessValidation) {
    User user = currentUserService.getCurrentUser();

    return relationshipService
        .getRelationshipsByProgramStageInstance(
            psi, pagingAndSortingCriteriaAdapter, skipAccessValidation)
        .stream()
        .filter((r) -> !skipAccessValidation && trackerAccessManager.canRead(user, r).isEmpty())
        .map(r -> getRelationship(r, user))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public ImportSummaries processRelationshipList(
      List<Relationship> relationships, ImportOptions importOptions) {
    ImportSummaries importSummaries = new ImportSummaries();
    importOptions = updateImportOptions(importOptions);

    List<Relationship> create = new ArrayList<>();
    List<Relationship> update = new ArrayList<>();
    List<Relationship> delete = new ArrayList<>();

    // TODO: Logic "delete relationships missing in the payload" is missing.
    // Has to be implemented later.

    if (importOptions.getImportStrategy().isCreate()) {
      create.addAll(relationships);
    } else if (importOptions.getImportStrategy().isCreateAndUpdate()) {
      for (Relationship relationship : relationships) {
        sortCreatesAndUpdates(relationship, create, update);
      }
    } else if (importOptions.getImportStrategy().isUpdate()) {
      update.addAll(relationships);
    } else if (importOptions.getImportStrategy().isDelete()) {
      delete.addAll(relationships);
    } else if (importOptions.getImportStrategy().isSync()) {
      for (Relationship relationship : relationships) {
        sortCreatesAndUpdates(relationship, create, update);
      }
    }

    importSummaries.addImportSummaries(addRelationships(create, importOptions));
    importSummaries.addImportSummaries(updateRelationships(update, importOptions));
    importSummaries.addImportSummaries(deleteRelationships(delete, importOptions));

    if (ImportReportMode.ERRORS == importOptions.getReportMode()) {
      importSummaries.getImportSummaries().removeIf(is -> !is.hasConflicts());
    }

    return importSummaries;
  }

  @Override
  @Transactional
  public ImportSummaries addRelationships(
      List<Relationship> relationships, ImportOptions importOptions) {
    List<List<Relationship>> partitions = Lists.partition(relationships, FLUSH_FREQUENCY);
    importOptions = updateImportOptions(importOptions);

    ImportSummaries importSummaries = new ImportSummaries();

    for (List<Relationship> _relationships : partitions) {
      reloadUser(importOptions);
      prepareCaches(_relationships, importOptions.getUser());

      for (Relationship relationship : _relationships) {
        importSummaries.addImportSummary(addRelationship(relationship, importOptions));
      }

      clearSession();
    }

    return importSummaries;
  }

  @Override
  @Transactional
  public ImportSummary addRelationship(Relationship relationship, ImportOptions importOptions) {
    importOptions = updateImportOptions(importOptions);

    // Set up cache if not set already
    if (!cacheExists()) {
      prepareCaches(Lists.newArrayList(relationship), importOptions.getUser());
    }

    ImportSummary importSummary = new ImportSummary(relationship.getRelationship());
    checkRelationship(relationship, importSummary);

    if (importSummary.hasConflicts()) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.getImportCount().incrementIgnored();
      return importSummary;
    }

    org.hisp.dhis.relationship.Relationship daoRelationship = createDAORelationship(relationship);

    Optional<org.hisp.dhis.relationship.Relationship> existing =
        relationshipService.getRelationshipByRelationship(daoRelationship);

    if (existing.isPresent()) {
      String message = "Relationship " + existing.get().getUid() + " already exists";
      return new ImportSummary(ImportStatus.ERROR, message)
          .setReference(existing.get().getUid())
          .incrementIgnored();
    }

    // Check access for both sides
    List<String> errors = trackerAccessManager.canWrite(importOptions.getUser(), daoRelationship);

    if (!errors.isEmpty()) {
      return new ImportSummary(ImportStatus.ERROR, errors.toString()).incrementIgnored();
    }

    relationshipService.addRelationship(daoRelationship);

    importSummary.setReference(daoRelationship.getUid());
    importSummary.getImportCount().incrementImported();

    return importSummary;
  }

  @Override
  @Transactional
  public ImportSummaries updateRelationships(
      List<Relationship> relationships, ImportOptions importOptions) {
    List<List<Relationship>> partitions = Lists.partition(relationships, FLUSH_FREQUENCY);
    importOptions = updateImportOptions(importOptions);
    ImportSummaries importSummaries = new ImportSummaries();

    for (List<Relationship> _relationships : partitions) {
      reloadUser(importOptions);
      prepareCaches(_relationships, importOptions.getUser());

      for (Relationship relationship : _relationships) {
        importSummaries.addImportSummary(updateRelationship(relationship, importOptions));
      }

      clearSession();
    }

    return importSummaries;
  }

  @Override
  @Transactional
  public ImportSummary updateRelationship(Relationship relationship, ImportOptions importOptions) {
    ImportSummary importSummary = new ImportSummary(relationship.getRelationship());
    importOptions = updateImportOptions(importOptions);

    // Set up cache if not set already
    if (!cacheExists()) {
      prepareCaches(Lists.newArrayList(relationship), importOptions.getUser());
    }

    org.hisp.dhis.relationship.Relationship daoRelationship =
        relationshipService.getRelationship(relationship.getRelationship());

    checkRelationship(relationship, importSummary);

    if (daoRelationship == null) {
      String message = "Relationship '" + relationship.getRelationship() + "' does not exist";
      importSummary.addConflict("Relationship", message);

      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.getImportCount().incrementIgnored();

      return importSummary;
    }

    List<String> errors = trackerAccessManager.canWrite(importOptions.getUser(), daoRelationship);

    if (!errors.isEmpty() || importSummary.hasConflicts()) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.getImportCount().incrementIgnored();

      if (!errors.isEmpty()) {
        importSummary.setDescription(errors.toString());
      }
      return importSummary;
    }

    relationshipService.updateRelationship(updatedDAORelationship(daoRelationship, relationship));

    importSummary.setReference(daoRelationship.getUid());
    importSummary.getImportCount().incrementUpdated();

    return importSummary;
  }

  /**
   * Update the relationship object fetched from the db only setting relationshipItem instance type.
   * Using the same relationship and relationshipItem objects maintains hibernate session reference
   *
   * @param relationshipDb relationship fetched from db
   * @param relationshipInput relationship in the payload
   * @return relationshipDb with updated relationshipItems
   */
  private org.hisp.dhis.relationship.Relationship updatedDAORelationship(
      org.hisp.dhis.relationship.Relationship relationshipDb, Relationship relationshipInput) {
    RelationshipType relationshipType =
        relationshipTypeCache.get(relationshipInput.getRelationshipType());
    relationshipDb.setRelationshipType(relationshipType);

    RelationshipItem fromItem = relationshipDb.getFrom();

    // FROM
    updateRelationshipItem(
        relationshipType.getFromConstraint(), fromItem, relationshipInput.getFrom());

    RelationshipItem toItem = relationshipDb.getTo();

    // TO
    updateRelationshipItem(relationshipType.getToConstraint(), toItem, relationshipInput.getTo());

    return relationshipDb;
  }

  private void updateRelationshipItem(
      RelationshipConstraint relationshipConstraint,
      RelationshipItem relationshipItem,
      org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem relationshipInput) {
    relationshipItem.setTrackedEntityInstance(null);
    relationshipItem.setProgramStageInstance(null);
    relationshipItem.setProgramInstance(null);

    if (relationshipConstraint.getRelationshipEntity().equals(TRACKED_ENTITY_INSTANCE)) {
      relationshipItem.setTrackedEntityInstance(
          trackedEntityInstanceCache.get(getUidOfRelationshipItem(relationshipInput)));
    } else if (relationshipConstraint.getRelationshipEntity().equals(PROGRAM_INSTANCE)) {
      relationshipItem.setProgramInstance(
          programInstanceCache.get(getUidOfRelationshipItem(relationshipInput)));
    } else if (relationshipConstraint.getRelationshipEntity().equals(PROGRAM_STAGE_INSTANCE)) {
      relationshipItem.setProgramStageInstance(
          programStageInstanceCache.get(getUidOfRelationshipItem(relationshipInput)));
    }
  }

  @Override
  @Transactional
  public ImportSummary deleteRelationship(String uid) {
    return deleteRelationship(uid, null);
  }

  @Override
  @Transactional
  public ImportSummaries deleteRelationships(
      List<Relationship> relationships, ImportOptions importOptions) {
    ImportSummaries importSummaries = new ImportSummaries();
    importOptions = updateImportOptions(importOptions);

    int counter = 0;

    for (Relationship relationship : relationships) {
      importSummaries.addImportSummary(
          deleteRelationship(relationship.getRelationship(), importOptions));

      if (counter % FLUSH_FREQUENCY == 0) {
        clearSession();
      }

      counter++;
    }

    return importSummaries;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Relationship> findRelationshipByUid(String id) {
    org.hisp.dhis.relationship.Relationship relationship = relationshipService.getRelationship(id);

    if (relationship == null) {
      return Optional.empty();
    }

    return getRelationship(relationship, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Relationship> findRelationship(
      org.hisp.dhis.relationship.Relationship dao, RelationshipParams params, User user) {
    List<String> errors = trackerAccessManager.canRead(user, dao);

    if (!errors.isEmpty()) {
      // Dont include relationship
      return Optional.empty();
    }

    Relationship relationship = new Relationship();

    relationship.setRelationship(dao.getUid());
    relationship.setRelationshipType(dao.getRelationshipType().getUid());
    relationship.setRelationshipName(dao.getRelationshipType().getName());

    relationship.setFrom(includeRelationshipItem(dao.getFrom(), !params.isIncludeFrom()));
    relationship.setTo(includeRelationshipItem(dao.getTo(), !params.isIncludeTo()));

    relationship.setBidirectional(dao.getRelationshipType().isBidirectional());

    relationship.setCreated(DateUtils.getIso8601NoTz(dao.getCreated()));
    relationship.setLastUpdated(DateUtils.getIso8601NoTz(dao.getLastUpdated()));

    return Optional.of(relationship);
  }

  private Optional<Relationship> getRelationship(
      org.hisp.dhis.relationship.Relationship dao, User user) {
    return findRelationship(dao, RelationshipParams.TRUE, user);
  }

  private org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem includeRelationshipItem(
      RelationshipItem dao, boolean uidOnly) {
    org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem relationshipItem =
        new org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem();

    if (dao.getTrackedEntityInstance() != null) {
      org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei =
          new org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance();
      String uid = dao.getTrackedEntityInstance().getUid();

      if (uidOnly) {
        tei.clear();
        tei.setTrackedEntityInstance(uid);
      } else {
        tei =
            trackedEntityInstanceService.getTrackedEntityInstance(
                dao.getTrackedEntityInstance(), TrackedEntityInstanceParams.TRUE);
      }

      relationshipItem.setTrackedEntityInstance(tei);
    } else if (dao.getProgramInstance() != null) {
      Enrollment enrollment = new Enrollment();
      String uid = dao.getProgramInstance().getUid();

      if (uidOnly) {

        enrollment.clear();
        enrollment.setEnrollment(uid);
      } else {
        enrollment =
            enrollmentService.getEnrollment(dao.getProgramInstance(), EnrollmentParams.TRUE);
      }

      relationshipItem.setEnrollment(enrollment);
    } else if (dao.getProgramStageInstance() != null) {
      Event event = new Event();
      String uid = dao.getProgramStageInstance().getUid();

      if (uidOnly) {
        event.clear();
        event.setEvent(uid);
      } else {
        event = eventService.getEvent(dao.getProgramStageInstance(), EventParams.FALSE);
      }

      relationshipItem.setEvent(event);
    }

    return relationshipItem;
  }

  private ImportSummary deleteRelationship(String uid, ImportOptions importOptions) {
    ImportSummary importSummary = new ImportSummary();
    importOptions = updateImportOptions(importOptions);

    if (uid.isEmpty()) {
      importSummary.setStatus(ImportStatus.WARNING);
      importSummary.setDescription("Missing required property 'relationship'");
      return importSummary.incrementIgnored();
    }

    org.hisp.dhis.relationship.Relationship daoRelationship =
        relationshipService.getRelationship(uid);

    if (daoRelationship != null) {
      importSummary.setReference(uid);

      List<String> errors = trackerAccessManager.canWrite(importOptions.getUser(), daoRelationship);

      if (!errors.isEmpty()) {
        importSummary.setDescription(errors.toString());
        importSummary.setStatus(ImportStatus.ERROR);
        importSummary.getImportCount().incrementIgnored();
        return importSummary;
      }

      relationshipService.deleteRelationship(daoRelationship);

      importSummary.setStatus(ImportStatus.SUCCESS);
      importSummary.setDescription("Deletion of relationship " + uid + " was successful");
      return importSummary.incrementDeleted();
    } else {
      importSummary.setStatus(ImportStatus.WARNING);
      importSummary.setDescription(
          "Relationship " + uid + " cannot be deleted as it is not present in the system");
      return importSummary.incrementIgnored();
    }
  }

  /** Checks the relationship for any conflicts, like missing or invalid references. */
  private void checkRelationship(Relationship relationship, ImportConflicts importConflicts) {
    RelationshipType relationshipType = null;

    if (StringUtils.isEmpty(relationship.getRelationshipType())) {
      importConflicts.addConflict(
          relationship.getRelationship(), "Missing property 'relationshipType'");
    } else {
      relationshipType = relationshipTypeCache.get(relationship.getRelationshipType());
    }

    if (relationship.getFrom() == null
        || getUidOfRelationshipItem(relationship.getFrom()).isEmpty()) {
      importConflicts.addConflict(relationship.getRelationship(), "Missing property 'from'");
    }

    if (relationship.getTo() == null || getUidOfRelationshipItem(relationship.getTo()).isEmpty()) {
      importConflicts.addConflict(relationship.getRelationship(), "Missing property 'to'");
    }

    if (relationship.getFrom().equals(relationship.getTo())) {
      importConflicts.addConflict(
          relationship.getRelationship(), "Self-referencing relationships are not allowed.");
    }

    if (importConflicts.hasConflicts()) {
      return;
    }

    if (relationshipType == null) {
      importConflicts.addConflict(
          relationship.getRelationship(),
          "relationshipType '" + relationship.getRelationshipType() + "' not found.");
      return;
    }

    addRelationshipConstraintConflicts(
        relationshipType.getFromConstraint(),
        relationship.getFrom(),
        relationship.getRelationship(),
        importConflicts);
    addRelationshipConstraintConflicts(
        relationshipType.getToConstraint(),
        relationship.getTo(),
        relationship.getRelationship(),
        importConflicts);
  }

  /**
   * Finds and adds any conflicts between relationship and relationship type
   *
   * @param constraint the constraint to check
   * @param relationshipItem the relationshipItem to check
   * @param relationshipUid the uid of the relationship
   */
  private void addRelationshipConstraintConflicts(
      RelationshipConstraint constraint,
      org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem relationshipItem,
      String relationshipUid,
      ImportConflicts importConflicts) {
    RelationshipEntity entity = constraint.getRelationshipEntity();
    String itemUid = getUidOfRelationshipItem(relationshipItem);

    if (TRACKED_ENTITY_INSTANCE.equals(entity)) {
      TrackedEntityInstance tei = trackedEntityInstanceCache.get(itemUid);

      if (tei == null) {
        importConflicts.addConflict(
            relationshipUid, "TrackedEntityInstance '" + itemUid + "' not found.");
      } else if (!tei.getTrackedEntityType().equals(constraint.getTrackedEntityType())) {
        importConflicts.addConflict(
            relationshipUid,
            "TrackedEntityInstance '" + itemUid + "' has invalid TrackedEntityType.");
      }
    } else if (PROGRAM_INSTANCE.equals(entity)) {
      ProgramInstance pi = programInstanceCache.get(itemUid);

      if (pi == null) {
        importConflicts.addConflict(
            relationshipUid, "ProgramInstance '" + itemUid + "' not found.");
      } else if (!pi.getProgram().equals(constraint.getProgram())) {
        importConflicts.addConflict(
            relationshipUid, "ProgramInstance '" + itemUid + "' has invalid Program.");
      }
    } else if (PROGRAM_STAGE_INSTANCE.equals(entity)) {
      ProgramStageInstance psi = programStageInstanceCache.get(itemUid);

      if (psi == null) {
        importConflicts.addConflict(
            relationshipUid, "ProgramStageInstance '" + itemUid + "' not found.");
      } else {
        if (constraint.getProgram() != null
            && !psi.getProgramStage().getProgram().equals(constraint.getProgram())) {
          importConflicts.addConflict(
              relationshipUid, "ProgramStageInstance '" + itemUid + "' has invalid Program.");
        } else if (constraint.getProgramStage() != null
            && !psi.getProgramStage().equals(constraint.getProgramStage())) {
          importConflicts.addConflict(
              relationshipUid, "ProgramStageInstance '" + itemUid + "' has invalid ProgramStage.");
        }
      }
    }
  }

  private String getUidOfRelationshipItem(
      org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem relationshipItem) {
    if (relationshipItem.getTrackedEntityInstance() != null) {
      return relationshipItem.getTrackedEntityInstance().getTrackedEntityInstance();
    } else if (relationshipItem.getEnrollment() != null) {
      return relationshipItem.getEnrollment().getEnrollment();
    } else if (relationshipItem.getEvent() != null) {
      return relationshipItem.getEvent().getEvent();
    }

    return "";
  }

  private org.hisp.dhis.relationship.Relationship createDAORelationship(Relationship relationship) {
    RelationshipType relationshipType =
        relationshipTypeCache.get(relationship.getRelationshipType());
    org.hisp.dhis.relationship.Relationship daoRelationship =
        new org.hisp.dhis.relationship.Relationship();
    RelationshipItem fromItem = null;
    RelationshipItem toItem = null;

    daoRelationship.setRelationshipType(relationshipType);

    if (relationship.getRelationship() != null) {
      daoRelationship.setUid(relationship.getRelationship());
    }

    // FROM
    if (relationshipType
        .getFromConstraint()
        .getRelationshipEntity()
        .equals(TRACKED_ENTITY_INSTANCE)) {
      fromItem = new RelationshipItem();
      fromItem.setTrackedEntityInstance(
          trackedEntityInstanceCache.get(getUidOfRelationshipItem(relationship.getFrom())));
    } else if (relationshipType
        .getFromConstraint()
        .getRelationshipEntity()
        .equals(PROGRAM_INSTANCE)) {
      fromItem = new RelationshipItem();
      fromItem.setProgramInstance(
          programInstanceCache.get(getUidOfRelationshipItem(relationship.getFrom())));
    } else if (relationshipType
        .getFromConstraint()
        .getRelationshipEntity()
        .equals(PROGRAM_STAGE_INSTANCE)) {
      fromItem = new RelationshipItem();
      fromItem.setProgramStageInstance(
          programStageInstanceCache.get(getUidOfRelationshipItem(relationship.getFrom())));
    }

    // TO
    if (relationshipType
        .getToConstraint()
        .getRelationshipEntity()
        .equals(TRACKED_ENTITY_INSTANCE)) {
      toItem = new RelationshipItem();
      toItem.setTrackedEntityInstance(
          trackedEntityInstanceCache.get(getUidOfRelationshipItem(relationship.getTo())));
    } else if (relationshipType
        .getToConstraint()
        .getRelationshipEntity()
        .equals(PROGRAM_INSTANCE)) {
      toItem = new RelationshipItem();
      toItem.setProgramInstance(
          programInstanceCache.get(getUidOfRelationshipItem(relationship.getTo())));
    } else if (relationshipType
        .getToConstraint()
        .getRelationshipEntity()
        .equals(PROGRAM_STAGE_INSTANCE)) {
      toItem = new RelationshipItem();
      toItem.setProgramStageInstance(
          programStageInstanceCache.get(getUidOfRelationshipItem(relationship.getTo())));
    }

    daoRelationship.setFrom(fromItem);
    daoRelationship.setTo(toItem);
    daoRelationship.setKey(RelationshipUtils.generateRelationshipKey(daoRelationship));
    daoRelationship.setInvertedKey(
        RelationshipUtils.generateRelationshipInvertedKey(daoRelationship));

    return daoRelationship;
  }

  private boolean cacheExists() {
    return !relationshipTypeCache.isEmpty();
  }

  private void prepareCaches(List<Relationship> relationships, User user) {
    Map<RelationshipEntity, List<String>> relationshipEntities = new HashMap<>();
    Map<String, List<Relationship>> relationshipTypeMap =
        relationships.stream().collect(Collectors.groupingBy(Relationship::getRelationshipType));

    // Find all the RelationshipTypes first, so we know what the uids refer
    // to
    Query query = Query.from(schemaService.getDynamicSchema(RelationshipType.class));
    query.setUser(user);
    query.add(Restrictions.in("id", relationshipTypeMap.keySet()));
    queryService
        .query(query)
        .forEach(rt -> relationshipTypeCache.put(rt.getUid(), (RelationshipType) rt));

    // Group all uids into their respective RelationshipEntities
    relationshipTypeCache
        .values()
        .forEach(
            relationshipType -> {
              List<String> fromUids =
                  relationshipTypeMap.get(relationshipType.getUid()).stream()
                      .map((r) -> getUidOfRelationshipItem(r.getFrom()))
                      .collect(Collectors.toList());

              List<String> toUids =
                  relationshipTypeMap.get(relationshipType.getUid()).stream()
                      .map((r) -> getUidOfRelationshipItem(r.getTo()))
                      .collect(Collectors.toList());

              // Merge existing results with newly found ones.

              relationshipEntities.merge(
                  relationshipType.getFromConstraint().getRelationshipEntity(),
                  fromUids,
                  (old, _new) -> ListUtils.union(old, _new));

              relationshipEntities.merge(
                  relationshipType.getToConstraint().getRelationshipEntity(),
                  toUids,
                  (old, _new) -> ListUtils.union(old, _new));
            });

    // Find and put all Relationship members in their respective cache
    if (relationshipEntities.get(TRACKED_ENTITY_INSTANCE) != null) {
      teiDaoService
          .getTrackedEntityInstancesByUid(relationshipEntities.get(TRACKED_ENTITY_INSTANCE), user)
          .forEach(tei -> trackedEntityInstanceCache.put(tei.getUid(), tei));
    }

    if (relationshipEntities.get(PROGRAM_INSTANCE) != null) {
      Query piQuery = Query.from(schemaService.getDynamicSchema(ProgramInstance.class));
      piQuery.setUser(user);
      piQuery.add(Restrictions.in("id", relationshipEntities.get(PROGRAM_INSTANCE)));
      queryService
          .query(piQuery)
          .forEach(pi -> programInstanceCache.put(pi.getUid(), (ProgramInstance) pi));
    }

    if (relationshipEntities.get(PROGRAM_STAGE_INSTANCE) != null) {
      Query psiQuery = Query.from(schemaService.getDynamicSchema(ProgramStageInstance.class));
      psiQuery.setUser(user);
      psiQuery.add(Restrictions.in("id", relationshipEntities.get(PROGRAM_STAGE_INSTANCE)));
      queryService
          .query(psiQuery)
          .forEach(psi -> programStageInstanceCache.put(psi.getUid(), (ProgramStageInstance) psi));
    }
  }

  private void clearSession() {
    relationshipTypeCache.clear();
    trackedEntityInstanceCache.clear();
    programInstanceCache.clear();
    programStageInstanceCache.clear();

    dbmsManager.flushSession();
  }

  protected ImportOptions updateImportOptions(ImportOptions importOptions) {
    if (importOptions == null) {
      importOptions = new ImportOptions();
    }

    if (importOptions.getUser() == null) {
      importOptions.setUser(currentUserService.getCurrentUser());
    }

    return importOptions;
  }

  private void reloadUser(ImportOptions importOptions) {
    if (importOptions == null || importOptions.getUser() == null) {
      return;
    }

    importOptions.setUser(userService.getUser(importOptions.getUser().getId()));
  }

  private void sortCreatesAndUpdates(
      Relationship relationship, List<Relationship> create, List<Relationship> update) {
    if (StringUtils.isEmpty(relationship.getRelationship())) {
      create.add(relationship);
    } else {
      if (!relationshipService.relationshipExists(relationship.getRelationship())) {
        create.add(relationship);
      } else {
        update.add(relationship);
      }
    }
  }
}
