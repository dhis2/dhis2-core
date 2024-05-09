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
package org.hisp.dhis.dxf2.deprecated.tracker.enrollment;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.trackedentity.TrackedEntityAttributeService.TEA_VALUE_MAX_LENGTH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.Constants;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.EnrollmentParams;
import org.hisp.dhis.dxf2.deprecated.tracker.NoteHelper;
import org.hisp.dhis.dxf2.deprecated.tracker.RelationshipParams;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.relationship.RelationshipService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Attribute;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.note.NoteService;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractEnrollmentService
    implements org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService {
  private static final String ATTRIBUTE_VALUE = "Attribute.value";

  private static final String ATTRIBUTE_ATTRIBUTE = "Attribute.attribute";

  protected EnrollmentService enrollmentService;

  protected EventService programStageInstanceService;

  protected ProgramService programService;

  protected TrackedEntityInstanceService trackedEntityInstanceService;

  protected TrackerOwnershipManager trackerOwnershipAccessManager;

  protected RelationshipService relationshipService;

  protected TrackedEntityService teiService;

  protected TrackedEntityAttributeService trackedEntityAttributeService;

  protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;
  protected NoteService commentService;

  protected IdentifiableObjectManager manager;

  protected I18nManager i18nManager;

  protected UserService userService;

  protected DbmsManager dbmsManager;

  protected org.hisp.dhis.dxf2.deprecated.tracker.event.EventService eventService;

  protected TrackerAccessManager trackerAccessManager;

  protected SchemaService schemaService;

  protected QueryService queryService;

  protected Notifier notifier;

  protected ApplicationEventPublisher eventPublisher;

  protected ObjectMapper jsonMapper;

  protected ObjectMapper xmlMapper;

  private CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

  private CachingMap<String, Program> programCache = new CachingMap<>();

  private CachingMap<String, TrackedEntityAttribute> trackedEntityAttributeCache =
      new CachingMap<>();

  private CachingMap<String, TrackedEntity> trackedEntityInstanceCache = new CachingMap<>();

  @Override
  public org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment getEnrollment(
      Enrollment enrollment, EnrollmentParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    return getEnrollment(currentUser, enrollment, params, false);
  }

  @Override
  public org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment getEnrollment(
      UserDetails user,
      Enrollment programInstance,
      EnrollmentParams params,
      boolean skipOwnershipCheck) {
    org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment =
        new org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment();
    enrollment.setEnrollment(programInstance.getUid());
    List<String> errors = trackerAccessManager.canRead(user, programInstance, skipOwnershipCheck);

    if (!errors.isEmpty()) {
      throw new IllegalQueryException(errors.toString());
    }

    if (programInstance.getTrackedEntity() != null) {
      enrollment.setTrackedEntityType(
          programInstance.getTrackedEntity().getTrackedEntityType().getUid());
      enrollment.setTrackedEntityInstance(programInstance.getTrackedEntity().getUid());
    }

    if (programInstance.getOrganisationUnit() != null) {
      enrollment.setOrgUnit(programInstance.getOrganisationUnit().getUid());
      enrollment.setOrgUnitName(programInstance.getOrganisationUnit().getName());
    }

    if (programInstance.getGeometry() != null) {
      enrollment.setGeometry(programInstance.getGeometry());
    }

    enrollment.setCreated(DateUtils.toIso8601NoTz(programInstance.getCreated()));
    enrollment.setCreatedAtClient(DateUtils.toIso8601NoTz(programInstance.getCreatedAtClient()));
    enrollment.setLastUpdated(DateUtils.toIso8601NoTz(programInstance.getLastUpdated()));
    enrollment.setLastUpdatedAtClient(
        DateUtils.toIso8601NoTz(programInstance.getLastUpdatedAtClient()));
    enrollment.setProgram(programInstance.getProgram().getUid());
    enrollment.setStatus(EnrollmentStatus.fromProgramStatus(programInstance.getStatus()));
    enrollment.setEnrollmentDate(programInstance.getEnrollmentDate());
    enrollment.setIncidentDate(programInstance.getOccurredDate());
    enrollment.setFollowup(programInstance.getFollowup());
    enrollment.setCompletedDate(programInstance.getCompletedDate());
    enrollment.setCompletedBy(programInstance.getCompletedBy());
    enrollment.setStoredBy(programInstance.getStoredBy());
    enrollment.setCreatedByUserInfo(programInstance.getCreatedByUserInfo());
    enrollment.setLastUpdatedByUserInfo(programInstance.getLastUpdatedByUserInfo());
    enrollment.setDeleted(programInstance.isDeleted());

    enrollment.getNotes().addAll(NoteHelper.convertNotes(programInstance.getNotes()));

    if (params.isIncludeEvents()) {
      for (Event event : programInstance.getEvents()) {
        if ((params.isIncludeDeleted() || !event.isDeleted())
            && trackerAccessManager.canRead(user, event, true).isEmpty()) {
          enrollment
              .getEvents()
              .add(
                  eventService.getEvent(
                      event,
                      params.isDataSynchronizationQuery(),
                      true,
                      params.getEnrollmentEventsParams().getEventParams()));
        }
      }
    }

    if (params.isIncludeRelationships()) {
      for (RelationshipItem relationshipItem : programInstance.getRelationshipItems()) {
        org.hisp.dhis.relationship.Relationship daoRelationship =
            relationshipItem.getRelationship();
        if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
            && (params.isIncludeDeleted() || !daoRelationship.isDeleted())) {
          Optional<Relationship> relationship =
              relationshipService.findRelationship(
                  relationshipItem.getRelationship(), RelationshipParams.FALSE, user);
          relationship.ifPresent(r -> enrollment.getRelationships().add(r));
        }
      }
    }

    if (params.isIncludeAttributes()) {
      Set<TrackedEntityAttribute> readableAttributes =
          trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes(
              user, List.of(programInstance.getProgram()), null);

      for (TrackedEntityAttributeValue trackedEntityAttributeValue :
          programInstance.getTrackedEntity().getTrackedEntityAttributeValues()) {
        if (readableAttributes.contains(trackedEntityAttributeValue.getAttribute())) {
          Attribute attribute = new Attribute();
          attribute.setCreated(DateUtils.toIso8601NoTz(trackedEntityAttributeValue.getCreated()));
          attribute.setLastUpdated(
              DateUtils.toIso8601NoTz(trackedEntityAttributeValue.getLastUpdated()));
          attribute.setDisplayName(trackedEntityAttributeValue.getAttribute().getDisplayName());
          attribute.setAttribute(trackedEntityAttributeValue.getAttribute().getUid());
          attribute.setValueType(trackedEntityAttributeValue.getAttribute().getValueType());
          attribute.setCode(trackedEntityAttributeValue.getAttribute().getCode());
          attribute.setValue(trackedEntityAttributeValue.getValue());
          attribute.setStoredBy(trackedEntityAttributeValue.getStoredBy());

          enrollment.getAttributes().add(attribute);
        }
      }
    }
    return enrollment;
  }

  private ImportSummary validateProgramInstance(
      Program program,
      Enrollment programInstance,
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      ImportSummary importSummary) {
    if (programInstance == null) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.setDescription(
          "Could not enroll tracked entity instance "
              + enrollment.getTrackedEntityInstance()
              + " into program "
              + enrollment.getProgram());
      importSummary.incrementIgnored();

      return importSummary;
    }

    if (program.getDisplayIncidentDate() && programInstance.getOccurredDate() == null) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.setDescription("DisplayIncidentDate is true but IncidentDate is null ");
      importSummary.incrementIgnored();

      return importSummary;
    }

    if (programInstance.getOccurredDate() != null
        && !DateUtils.dateIsValid(DateUtils.toMediumDate(programInstance.getOccurredDate()))) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.setDescription(
          "Invalid enollment incident date:  " + programInstance.getOccurredDate());
      importSummary.incrementIgnored();

      return importSummary;
    }

    if (programInstance.getEnrollmentDate() != null
        && !DateUtils.dateIsValid(DateUtils.toMediumDate(programInstance.getEnrollmentDate()))) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.setDescription(
          "Invalid enollment date:  " + programInstance.getEnrollmentDate());
      importSummary.incrementIgnored();

      return importSummary;
    }

    if (enrollment.getCreatedAtClient() != null
        && !DateUtils.dateIsValid(enrollment.getCreatedAtClient())) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.setDescription(
          "Invalid enrollment created at client date: " + enrollment.getCreatedAtClient());
      importSummary.incrementIgnored();

      return importSummary;
    }

    if (enrollment.getLastUpdatedAtClient() != null
        && !DateUtils.dateIsValid(enrollment.getLastUpdatedAtClient())) {
      importSummary.setStatus(ImportStatus.ERROR);
      importSummary.setDescription(
          "Invalid enrollment last updated at client date: " + enrollment.getCreatedAtClient());
      importSummary.incrementIgnored();

      return importSummary;
    }

    return importSummary;
  }

  private String validateProgramForEnrollment(
      Program program,
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      OrganisationUnit orgUnit,
      ImportOptions importOptions) {
    if (program == null) {
      return "Program can not be null";
    }

    if (orgUnit == null) {
      return "OrganisationUnit can not be null";
    }

    if (!program.isRegistration()) {
      return "Provided program "
          + program.getUid()
          + " is a program without registration. An enrollment cannot be created into program without registration.";
    }

    if (!programService.checkProgramOrganisationUnitsAssociations(
        program.getUid(), orgUnit.getUid())) {
      return "Program is not assigned to this Organisation Unit: " + enrollment.getOrgUnit();
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // UPDATE
  // -------------------------------------------------------------------------

  @Override
  public ImportSummaries updateEnrollments(
      List<org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment> enrollments,
      ImportOptions importOptions,
      boolean clearSession) {
    List<List<org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment>> partitions =
        Lists.partition(enrollments, FLUSH_FREQUENCY);
    importOptions = updateImportOptions(importOptions);
    ImportSummaries importSummaries = new ImportSummaries();
    List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events = new ArrayList<>();

    for (List<org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment> _enrollments :
        partitions) {
      reloadUser(importOptions);
      prepareCaches(_enrollments, importOptions.getUser());

      for (org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment : _enrollments) {
        ImportSummary importSummary = updateEnrollment(enrollment, importOptions, false);
        importSummaries.addImportSummary(importSummary);

        if (importSummary.isStatus(ImportStatus.SUCCESS)) {
          List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> enrollmentEvents =
              enrollment.getEvents();
          enrollmentEvents.forEach(e -> e.setEnrollment(enrollment.getEnrollment()));
          events.addAll(enrollmentEvents);
        }
      }

      if (clearSession && enrollments.size() >= FLUSH_FREQUENCY) {
        clearSession();
      }
    }

    ImportSummaries eventImportSummaries =
        eventService.processEventImport(events, importOptions, null);
    linkEventSummaries(importSummaries, eventImportSummaries, events);

    return importSummaries;
  }

  private ImportSummary updateEnrollment(
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      ImportOptions importOptions,
      boolean handleEvents) {
    importOptions = updateImportOptions(importOptions);

    if (enrollment == null || StringUtils.isEmpty(enrollment.getEnrollment())) {
      return new ImportSummary(ImportStatus.ERROR, "No enrollment or enrollment ID was supplied")
          .incrementIgnored();
    }

    Enrollment programInstance = enrollmentService.getEnrollment(enrollment.getEnrollment());
    UserDetails currentUser = UserDetails.fromUser(importOptions.getUser());
    List<String> errors = trackerAccessManager.canUpdate(currentUser, programInstance, false);

    if (programInstance == null) {
      return new ImportSummary(
              ImportStatus.ERROR,
              "ID " + enrollment.getEnrollment() + " doesn't point to a valid enrollment.")
          .incrementIgnored();
    }

    if (!errors.isEmpty()) {
      return new ImportSummary(ImportStatus.ERROR, errors.toString()).incrementIgnored();
    }

    ImportSummary importSummary = new ImportSummary(enrollment.getEnrollment());
    checkAttributes(programInstance.getTrackedEntity(), enrollment, importOptions, importSummary);

    if (importSummary.hasConflicts()) {
      importSummary.setStatus(ImportStatus.ERROR).incrementIgnored();
      return importSummary;
    }

    Program program = getProgram(importOptions.getIdSchemes(), enrollment.getProgram());

    if (!program.isRegistration()) {
      String descMsg =
          "Provided program "
              + program.getUid()
              + " is a program without registration. An enrollment cannot be created into program without registration.";

      return new ImportSummary(ImportStatus.ERROR, descMsg).incrementIgnored();
    }

    programInstance.setProgram(program);

    if (enrollment.getIncidentDate() != null) {
      programInstance.setOccurredDate(enrollment.getIncidentDate());
    }

    if (enrollment.getEnrollmentDate() != null) {
      programInstance.setEnrollmentDate(enrollment.getEnrollmentDate());
    }

    if (enrollment.getOrgUnit() != null) {
      OrganisationUnit organisationUnit =
          getOrganisationUnit(importOptions.getIdSchemes(), enrollment.getOrgUnit());
      programInstance.setOrganisationUnit(organisationUnit);
    }

    programInstance.setFollowup(enrollment.getFollowup());

    if (program.getDisplayIncidentDate() && programInstance.getOccurredDate() == null) {
      return new ImportSummary(
              ImportStatus.ERROR, "DisplayIncidentDate is true but IncidentDate is null")
          .incrementIgnored();
    }

    updateFeatureType(program, enrollment, programInstance);

    if (EnrollmentStatus.fromProgramStatus(programInstance.getStatus()) != enrollment.getStatus()) {
      Date endDate = enrollment.getCompletedDate();

      String user = enrollment.getCompletedBy();

      if (enrollment.getCompletedDate() == null) {
        endDate = new Date();
      }

      if (user == null) {
        user = currentUser.getUsername();
      }

      if (EnrollmentStatus.CANCELLED == enrollment.getStatus()) {
        programInstance.setCompletedDate(endDate);

        enrollmentService.cancelEnrollmentStatus(programInstance);
      } else if (EnrollmentStatus.COMPLETED == enrollment.getStatus()) {
        programInstance.setCompletedDate(endDate);
        programInstance.setCompletedBy(user);

        enrollmentService.completeEnrollmentStatus(programInstance);
      } else if (EnrollmentStatus.ACTIVE == enrollment.getStatus()) {
        enrollmentService.incompleteEnrollmentStatus(programInstance);
      }
    }

    validateProgramInstance(program, programInstance, enrollment, importSummary);

    if (importSummary.getStatus() != ImportStatus.SUCCESS) {
      return importSummary;
    }

    updateAttributeValues(enrollment, importOptions);
    updateDateFields(enrollment, programInstance);

    programInstance.setLastUpdatedByUserInfo(UserInfoSnapshot.from(currentUser));

    enrollmentService.updateEnrollment(programInstance, currentUser);
    teiService.updateTrackedEntity(programInstance.getTrackedEntity(), currentUser);

    saveTrackedEntityComment(programInstance, enrollment, currentUser);

    importSummary = new ImportSummary(enrollment.getEnrollment()).incrementUpdated();
    importSummary.setReference(enrollment.getEnrollment());

    if (handleEvents) {
      importSummary.setEvents(handleEvents(enrollment, programInstance, importOptions));
    } else {
      for (org.hisp.dhis.dxf2.deprecated.tracker.event.Event event : enrollment.getEvents()) {
        event.setEnrollment(enrollment.getEnrollment());
        event.setProgram(programInstance.getProgram().getUid());
        event.setTrackedEntityInstance(enrollment.getTrackedEntityInstance());
      }
    }

    return importSummary;
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------

  private ImportSummary deleteEnrollment(
      String uid,
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      ImportOptions importOptions) {
    ImportSummary importSummary = new ImportSummary();
    importOptions = updateImportOptions(importOptions);

    UserDetails user = UserDetails.fromUser(importOptions.getUser());

    boolean existsEnrollment = enrollmentService.enrollmentExists(uid);

    if (existsEnrollment) {
      Enrollment programInstance = enrollmentService.getEnrollment(uid);

      if (enrollment != null) {
        importSummary.setReference(uid);
        importSummary.setEvents(handleEvents(enrollment, programInstance, importOptions));
      }

      if (user != null) {
        isAllowedToDelete(user, programInstance, importSummary);

        if (importSummary.hasConflicts()) {
          importSummary.setStatus(ImportStatus.ERROR);
          importSummary.setReference(programInstance.getUid());
          importSummary.incrementIgnored();
          return importSummary;
        }
      }

      programInstance.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
      enrollmentService.deleteEnrollment(programInstance);

      TrackedEntity entity = programInstance.getTrackedEntity();
      entity.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));

      teiService.updateTrackedEntity(entity);

      importSummary.setReference(uid);
      importSummary.setStatus(ImportStatus.SUCCESS);
      importSummary.setDescription("Deletion of enrollment " + uid + " was successful");

      return importSummary.incrementDeleted();
    } else {
      // If I am here, it means that the item is either already deleted or
      // it is not present in the system at all.
      importSummary.setStatus(ImportStatus.SUCCESS);
      importSummary.setDescription(
          "Enrollment " + uid + " cannot be deleted as it is not present in the system");
      return importSummary.incrementIgnored();
    }
  }

  @Override
  public ImportSummaries deleteEnrollments(
      List<org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment> enrollments,
      ImportOptions importOptions,
      boolean clearSession) {
    ImportSummaries importSummaries = new ImportSummaries();
    importOptions = updateImportOptions(importOptions);
    int counter = 0;

    for (org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment : enrollments) {
      importSummaries.addImportSummary(
          deleteEnrollment(enrollment.getEnrollment(), enrollment, importOptions));

      if (clearSession && counter % FLUSH_FREQUENCY == 0) {
        clearSession();
      }

      counter++;
    }

    return importSummaries;
  }

  // -------------------------------------------------------------------------
  // HELPERS
  // -------------------------------------------------------------------------

  private void linkEventSummaries(
      ImportSummaries importSummaries,
      ImportSummaries eventImportSummaries,
      List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events) {
    importSummaries.getImportSummaries().forEach(is -> is.setEvents(new ImportSummaries()));

    Map<String, List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event>> eventsGroupedByEnrollment =
        events.stream()
            .filter(ev -> !StringUtils.isEmpty(ev.getEnrollment()))
            .collect(
                Collectors.groupingBy(
                    org.hisp.dhis.dxf2.deprecated.tracker.event.Event::getEnrollment));

    Map<String, List<ImportSummary>> summariesGroupedByReference =
        importSummaries.getImportSummaries().stream()
            .filter(ev -> !StringUtils.isEmpty(ev.getReference()))
            .collect(Collectors.groupingBy(ImportSummary::getReference));

    Map<String, List<ImportSummary>> eventSummariesGroupedByReference =
        eventImportSummaries.getImportSummaries().stream()
            .filter(ev -> !StringUtils.isEmpty(ev.getReference()))
            .collect(Collectors.groupingBy(ImportSummary::getReference));

    for (Map.Entry<String, List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event>> set :
        eventsGroupedByEnrollment.entrySet()) {
      if (!summariesGroupedByReference.containsKey(set.getKey())) {
        continue;
      }

      ImportSummary importSummary = summariesGroupedByReference.get(set.getKey()).get(0);
      ImportSummaries eventSummaries = new ImportSummaries();

      for (org.hisp.dhis.dxf2.deprecated.tracker.event.Event event : set.getValue()) {
        if (!eventSummariesGroupedByReference.containsKey(event.getEvent())) {
          continue;
        }

        ImportSummary enrollmentSummary =
            eventSummariesGroupedByReference.get(event.getEvent()).get(0);
        eventSummaries.addImportSummary(enrollmentSummary);
      }

      if (eventImportSummaries.getImportSummaries().isEmpty()) {
        continue;
      }

      importSummary.setEvents(eventSummaries);
    }
  }

  private ImportSummaries handleEvents(
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      Enrollment programInstance,
      ImportOptions importOptions) {
    List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> create = new ArrayList<>();
    List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> update = new ArrayList<>();
    List<String> delete = new ArrayList<>();

    for (org.hisp.dhis.dxf2.deprecated.tracker.event.Event event : enrollment.getEvents()) {
      event.setEnrollment(enrollment.getEnrollment());
      event.setProgram(programInstance.getProgram().getUid());
      event.setTrackedEntityInstance(enrollment.getTrackedEntityInstance());

      if (importOptions.getImportStrategy().isSync() && event.isDeleted()) {
        delete.add(event.getEvent());
      } else if (!programStageInstanceService.eventExists(event.getEvent())) {
        create.add(event);
      } else {
        update.add(event);
      }
    }

    ImportSummaries importSummaries = new ImportSummaries();
    importSummaries.addImportSummaries(eventService.deleteEvents(delete, false));
    importSummaries.addImportSummaries(
        eventService.updateEvents(update, importOptions, false, false));
    importSummaries.addImportSummaries(eventService.addEvents(create, importOptions, false));

    return importSummaries;
  }

  private void prepareCaches(
      List<org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment> enrollments, User user) {
    Collection<String> orgUnits =
        enrollments.stream()
            .map(org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment::getOrgUnit)
            .collect(Collectors.toSet());

    if (!orgUnits.isEmpty()) {
      Query query = Query.from(schemaService.getDynamicSchema(OrganisationUnit.class));
      query.setCurrentUserDetails(UserDetails.fromUser(user));
      query.add(Restrictions.in("id", orgUnits));
      queryService
          .query(query)
          .forEach(ou -> organisationUnitCache.put(ou.getUid(), (OrganisationUnit) ou));
    }

    Collection<String> programs =
        enrollments.stream()
            .map(org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment::getProgram)
            .collect(Collectors.toSet());

    if (!programs.isEmpty()) {
      Query query = Query.from(schemaService.getDynamicSchema(Program.class));
      query.setCurrentUserDetails(UserDetails.fromUser(user));
      query.add(Restrictions.in("id", programs));
      queryService.query(query).forEach(pr -> programCache.put(pr.getUid(), (Program) pr));
    }

    Collection<String> trackedEntityAttributes = new HashSet<>();
    enrollments.forEach(
        e -> e.getAttributes().forEach(at -> trackedEntityAttributes.add(at.getAttribute())));

    if (!trackedEntityAttributes.isEmpty()) {
      Query query = Query.from(schemaService.getDynamicSchema(TrackedEntityAttribute.class));
      query.setCurrentUserDetails(UserDetails.fromUser(user));
      query.add(Restrictions.in("id", trackedEntityAttributes));
      queryService
          .query(query)
          .forEach(
              tea -> trackedEntityAttributeCache.put(tea.getUid(), (TrackedEntityAttribute) tea));
    }

    Collection<String> trackedEntityInstances =
        enrollments.stream()
            .map(
                org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment
                    ::getTrackedEntityInstance)
            .collect(toList());

    if (!trackedEntityInstances.isEmpty()) {
      Query query = Query.from(schemaService.getDynamicSchema(TrackedEntity.class));
      query.setCurrentUserDetails(UserDetails.fromUser(user));
      query.add(Restrictions.in("id", trackedEntityInstances));
      queryService
          .query(query)
          .forEach(te -> trackedEntityInstanceCache.put(te.getUid(), (TrackedEntity) te));
    }
  }

  private void updateFeatureType(
      Program program,
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      Enrollment programInstance) {
    if (program.getFeatureType() != null) {
      if (enrollment.getGeometry() != null && !program.getFeatureType().equals(FeatureType.NONE)) {
        programInstance.setGeometry(enrollment.getGeometry());
      } else {
        programInstance.setGeometry(null);
      }
    }

    if (programInstance.getGeometry() != null) {
      programInstance.getGeometry().setSRID(GeoUtils.SRID);
    }
  }

  private boolean doValidationOfMandatoryAttributes(User user) {
    return user == null
        || !user.isAuthorized(Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.name());
  }

  private void checkAttributes(
      TrackedEntity trackedEntity,
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      ImportOptions importOptions,
      ImportConflicts importConflicts) {
    Map<TrackedEntityAttribute, Boolean> mandatoryMap = Maps.newHashMap();
    Map<String, String> attributeValueMap = Maps.newHashMap();

    Program program = getProgram(importOptions.getIdSchemes(), enrollment.getProgram());

    for (ProgramTrackedEntityAttribute programTrackedEntityAttribute :
        program.getProgramAttributes()) {
      mandatoryMap.put(
          programTrackedEntityAttribute.getAttribute(),
          programTrackedEntityAttribute.isMandatory());
    }

    // ignore attributes which do not belong to this program
    trackedEntity.getTrackedEntityAttributeValues().stream()
        .filter(value -> mandatoryMap.containsKey(value.getAttribute()))
        .forEach(value -> attributeValueMap.put(value.getAttribute().getUid(), value.getValue()));

    for (Attribute attribute : enrollment.getAttributes()) {
      attributeValueMap.put(attribute.getAttribute(), attribute.getValue());
      validateAttributeType(attribute, importOptions, importConflicts);
    }

    UserDetails user = UserDetails.fromUser(importOptions.getUser());
    List<String> errors = trackerAccessManager.canRead(user, trackedEntity);

    if (!errors.isEmpty()) {
      throw new IllegalQueryException(errors.toString());
    }

    checkAttributeForMandatoryMaxLengthAndUniqueness(
        trackedEntity, importOptions, importConflicts, mandatoryMap, attributeValueMap);

    if (!attributeValueMap.isEmpty()) {
      importConflicts.addConflict(
          ATTRIBUTE_ATTRIBUTE,
          "Only program attributes is allowed for enrollment " + attributeValueMap);
    }

    if (!program.getSelectEnrollmentDatesInFuture()) {
      if (Objects.nonNull(enrollment.getEnrollmentDate())
          && enrollment.getEnrollmentDate().after(new Date())) {
        importConflicts.addConflict(
            "Enrollment.date",
            "Enrollment Date can't be future date :" + enrollment.getEnrollmentDate());
      }
    }

    if (!program.getSelectIncidentDatesInFuture()) {
      if (Objects.nonNull(enrollment.getIncidentDate())
          && enrollment.getIncidentDate().after(new Date())) {
        importConflicts.addConflict(
            "Enrollment.incidentDate",
            "Incident Date can't be future date :" + enrollment.getIncidentDate());
      }
    }
  }

  private void checkAttributeForMandatoryMaxLengthAndUniqueness(
      TrackedEntity trackedEntity,
      ImportOptions importOptions,
      ImportConflicts importConflicts,
      Map<TrackedEntityAttribute, Boolean> mandatoryMap,
      Map<String, String> attributeValueMap) {
    for (TrackedEntityAttribute trackedEntityAttribute : mandatoryMap.keySet()) {
      Boolean mandatory = mandatoryMap.get(trackedEntityAttribute);

      if (mandatory
          && doValidationOfMandatoryAttributes(importOptions.getUser())
          && !attributeValueMap.containsKey(trackedEntityAttribute.getUid())) {
        importConflicts.addConflict(
            ATTRIBUTE_ATTRIBUTE, "Missing mandatory attribute " + trackedEntityAttribute.getUid());
        continue;
      }

      String attributeValue = attributeValueMap.get(trackedEntityAttribute.getUid());

      if (attributeValue != null && attributeValue.length() > TEA_VALUE_MAX_LENGTH) {
        // We shorten the value to first 25 characters, since we dont
        // want to post a 1200+ string back.
        importConflicts.addConflict(
            ATTRIBUTE_VALUE,
            String.format(
                "Value exceeds the character limit of %s characters: '%s...'",
                TEA_VALUE_MAX_LENGTH,
                attributeValueMap.get(trackedEntityAttribute.getUid()).substring(0, 25)));
      }

      if (trackedEntityAttribute.isUnique()) {
        checkAttributeUniquenessWithinScope(
            trackedEntity,
            trackedEntityAttribute,
            attributeValueMap.get(trackedEntityAttribute.getUid()),
            trackedEntity.getOrganisationUnit(),
            importConflicts);
      }

      attributeValueMap.remove(trackedEntityAttribute.getUid());
    }
  }

  private void checkAttributeUniquenessWithinScope(
      TrackedEntity trackedEntity,
      TrackedEntityAttribute trackedEntityAttribute,
      String value,
      OrganisationUnit organisationUnit,
      ImportConflicts importConflicts) {
    if (value == null) {
      return;
    }

    String errorMessage =
        trackedEntityAttributeService.validateAttributeUniquenessWithinScope(
            trackedEntityAttribute, value, trackedEntity, organisationUnit);

    if (errorMessage != null) {
      importConflicts.addConflict(ATTRIBUTE_VALUE, errorMessage);
    }
  }

  private void updateAttributeValues(
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      ImportOptions importOptions) {
    TrackedEntity trackedEntity =
        teiService.getTrackedEntity(enrollment.getTrackedEntityInstance());
    Map<String, Attribute> attributeValueMap = Maps.newHashMap();

    for (Attribute attribute : enrollment.getAttributes()) {
      attributeValueMap.put(attribute.getAttribute(), attribute);
    }

    UserDetails user = UserDetails.fromUser(importOptions.getUser());
    trackedEntity.getTrackedEntityAttributeValues().stream()
        .filter(value -> attributeValueMap.containsKey(value.getAttribute().getUid()))
        .forEach(
            value -> {
              Attribute enrollmentAttribute = attributeValueMap.get(value.getAttribute().getUid());

              String newValue = enrollmentAttribute.getValue();
              value.setValue(newValue);
              value.setStoredBy(getStoredBy(enrollmentAttribute, user));

              trackedEntityAttributeValueService.updateTrackedEntityAttributeValue(value, user);

              attributeValueMap.remove(value.getAttribute().getUid());
            });

    for (String key : attributeValueMap.keySet()) {
      TrackedEntityAttribute attribute =
          getTrackedEntityAttribute(importOptions.getIdSchemes(), key);

      if (attribute != null) {
        TrackedEntityAttributeValue value = new TrackedEntityAttributeValue();
        Attribute enrollmentAttribute = attributeValueMap.get(key);

        value.setValue(enrollmentAttribute.getValue());
        value.setAttribute(attribute);
        value.setStoredBy(getStoredBy(enrollmentAttribute, user));

        trackedEntityAttributeValueService.addTrackedEntityAttributeValue(value);
        trackedEntity.addAttributeValue(value);
      }
    }
  }

  private String getStoredBy(Attribute attribute, UserDetails user) {
    if (!StringUtils.isEmpty(attribute.getStoredBy())) {
      return attribute.getStoredBy();
    }

    return User.username(user, Constants.UNKNOWN);
  }

  private TrackedEntity getTrackedEntityInstance(String teiUID, UserDetails user) {
    TrackedEntity entityInstance = teiService.getTrackedEntity(teiUID, user);

    if (entityInstance == null) {
      throw new InvalidIdentifierReferenceException("TrackedEntity does not exist.");
    }

    return entityInstance;
  }

  private void validateAttributeType(
      Attribute attribute, ImportOptions importOptions, ImportConflicts importConflicts) {
    // Cache is populated if it is a batch operation. Otherwise, it is not.
    TrackedEntityAttribute teAttribute =
        getTrackedEntityAttribute(importOptions.getIdSchemes(), attribute.getAttribute());

    if (teAttribute == null) {
      importConflicts.addConflict(ATTRIBUTE_ATTRIBUTE, "Does not point to a valid attribute.");
    }

    String errorMessage =
        trackedEntityAttributeService.validateValueType(teAttribute, attribute.getValue());

    if (errorMessage != null) {
      importConflicts.addConflict(ATTRIBUTE_VALUE, errorMessage);
    }
  }

  private void saveTrackedEntityComment(
      Enrollment programInstance,
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      UserDetails user) {
    for (Note note : enrollment.getNotes()) {
      String noteUid =
          CodeGenerator.isValidUid(note.getNote()) ? note.getNote() : CodeGenerator.generateUid();

      if (!commentService.noteExists(noteUid) && !StringUtils.isEmpty(note.getValue())) {
        org.hisp.dhis.note.Note comment = new org.hisp.dhis.note.Note();
        comment.setUid(noteUid);
        comment.setNoteText(note.getValue());
        comment.setCreator(
            StringUtils.isEmpty(note.getStoredBy()) ? user.getUsername() : note.getStoredBy());

        Date created = DateUtils.parseDate(note.getStoredDate());

        if (created == null) {
          created = new Date();
        }

        comment.setCreated(created);

        // TODO is this needed here?
        comment.setLastUpdatedBy(userService.getUser(user.getUid()));
        comment.setLastUpdated(new Date());

        commentService.addNote(comment);

        programInstance.getNotes().add(comment);

        enrollmentService.updateEnrollment(programInstance, user);
        teiService.updateTrackedEntity(programInstance.getTrackedEntity(), user);
      }
    }
  }

  private OrganisationUnit getOrganisationUnit(IdSchemes idSchemes, String id) {
    return organisationUnitCache.get(
        id,
        new IdentifiableObjectCallable<>(
            manager, OrganisationUnit.class, idSchemes.getOrgUnitIdScheme(), id));
  }

  private Program getProgram(IdSchemes idSchemes, String id) {
    return programCache.get(
        id,
        new IdentifiableObjectCallable<>(
            manager, Program.class, idSchemes.getProgramIdScheme(), id));
  }

  private TrackedEntityAttribute getTrackedEntityAttribute(IdSchemes idSchemes, String id) {
    return trackedEntityAttributeCache.get(
        id,
        new IdentifiableObjectCallable<>(
            manager,
            TrackedEntityAttribute.class,
            idSchemes.getTrackedEntityAttributeIdScheme(),
            id));
  }

  private void clearSession() {
    organisationUnitCache.clear();
    programCache.clear();
    trackedEntityAttributeCache.clear();

    dbmsManager.flushSession();
  }

  private void updateDateFields(
      org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment,
      Enrollment programInstance) {
    programInstance.setAutoFields();

    Date createdAtClient = DateUtils.parseDate(enrollment.getCreatedAtClient());

    if (createdAtClient == null) {
      createdAtClient = new Date();
    }

    programInstance.setCreatedAtClient(createdAtClient);

    Date lastUpdatedAtClient = DateUtils.parseDate(enrollment.getLastUpdatedAtClient());

    if (lastUpdatedAtClient == null) {
      lastUpdatedAtClient = new Date();
    }

    programInstance.setLastUpdatedAtClient(lastUpdatedAtClient);
  }

  protected ImportOptions updateImportOptions(ImportOptions importOptions) {
    if (importOptions == null) {
      importOptions = new ImportOptions();
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    if (importOptions.getUser() == null) {
      importOptions.setUser(currentUser);
    }

    return importOptions;
  }

  protected void reloadUser(ImportOptions importOptions) {
    if (importOptions == null || importOptions.getUser() == null) {
      return;
    }

    importOptions.setUser(userService.getUser(importOptions.getUser().getUid()));
  }

  private void isAllowedToDelete(UserDetails user, Enrollment pi, ImportConflicts importConflicts) {

    Set<Event> notDeletedEvents =
        pi.getEvents().stream().filter(psi -> !psi.isDeleted()).collect(Collectors.toSet());

    if (!notDeletedEvents.isEmpty()
        && !user.isAuthorized(Authorities.F_ENROLLMENT_CASCADE_DELETE.name())) {
      importConflicts.addConflict(
          pi.getUid(),
          "Enrollment "
              + pi.getUid()
              + " cannot be deleted as it has associated events and user does not have authority: "
              + Authorities.F_ENROLLMENT_CASCADE_DELETE.name());
    }

    List<String> errors = trackerAccessManager.canDelete(user, pi, false);

    if (!errors.isEmpty()) {
      errors.forEach(error -> importConflicts.addConflict(pi.getUid(), error));
    }
  }
}
