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
package org.hisp.dhis.dxf2.deprecated.tracker.event;

import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ATTRIBUTE_OPTION_COMBO_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_COMPLETED_BY_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_COMPLETED_DATE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_CREATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_CREATED_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_DELETED;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_DUE_DATE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ENROLLMENT_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_EXECUTION_DATE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_GEOMETRY;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_LAST_UPDATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_LAST_UPDATED_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ORG_UNIT_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ORG_UNIT_NAME;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_PROGRAM_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_PROGRAM_STAGE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_STATUS_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_STORED_BY_ID;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.EventParams;
import org.hisp.dhis.dxf2.deprecated.tracker.NoteHelper;
import org.hisp.dhis.dxf2.deprecated.tracker.RelationshipParams;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.EventImporter;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.EventManager;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContextLoader;
import org.hisp.dhis.dxf2.deprecated.tracker.relationship.RelationshipService;
import org.hisp.dhis.dxf2.deprecated.tracker.report.EventRow;
import org.hisp.dhis.dxf2.deprecated.tracker.report.EventRows;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityOuInfo;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.note.NoteService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractEventService
    implements org.hisp.dhis.dxf2.deprecated.tracker.event.EventService {
  public static final List<String> STATIC_EVENT_COLUMNS =
      Arrays.asList(
          EVENT_ID,
          EVENT_ENROLLMENT_ID,
          EVENT_CREATED_ID,
          EVENT_CREATED_BY_USER_INFO_ID,
          EVENT_LAST_UPDATED_ID,
          EVENT_LAST_UPDATED_BY_USER_INFO_ID,
          EVENT_STORED_BY_ID,
          EVENT_COMPLETED_BY_ID,
          EVENT_COMPLETED_DATE_ID,
          EVENT_EXECUTION_DATE_ID,
          EVENT_DUE_DATE_ID,
          EVENT_ORG_UNIT_ID,
          EVENT_ORG_UNIT_NAME,
          EVENT_STATUS_ID,
          EVENT_PROGRAM_STAGE_ID,
          EVENT_PROGRAM_ID,
          EVENT_ATTRIBUTE_OPTION_COMBO_ID,
          EVENT_DELETED,
          EVENT_GEOMETRY);

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  protected EventImporter eventImporter;

  protected EventManager eventManager;

  protected WorkContextLoader workContextLoader;

  protected ProgramService programService;

  protected EnrollmentService enrollmentService;

  protected EventService eventService;

  protected OrganisationUnitService organisationUnitService;

  protected TrackedEntityService entityInstanceService;

  protected NoteService commentService;

  protected EventStore eventStore;

  protected Notifier notifier;

  protected DbmsManager dbmsManager;

  protected IdentifiableObjectManager manager;

  protected CategoryService categoryService;

  protected FileResourceService fileResourceService;

  protected SchemaService schemaService;

  protected QueryService queryService;

  protected TrackerAccessManager trackerAccessManager;

  protected TrackerOwnershipManager trackerOwnershipAccessManager;

  protected RelationshipService relationshipService;

  protected UserService userService;

  protected EventServiceContextBuilder eventServiceContextBuilder;

  protected Cache<Boolean> dataElementCache;

  private static final int FLUSH_FREQUENCY = 100;

  // -------------------------------------------------------------------------
  // Caches
  // -------------------------------------------------------------------------

  private final Set<TrackedEntity> trackedEntityInstancesToUpdate = new HashSet<>();

  // -------------------------------------------------------------------------
  // CREATE
  // -------------------------------------------------------------------------

  @Override
  public ImportSummaries processEventImport(
      List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events,
      ImportOptions importOptions,
      JobConfiguration jobConfiguration) {
    return eventImporter.importAll(events, importOptions, jobConfiguration);
  }

  @Transactional
  @Override
  public ImportSummaries addEvents(
      List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events,
      ImportOptions importOptions,
      boolean clearSession) {
    final WorkContext workContext = workContextLoader.load(importOptions, events);
    return eventManager.addEvents(events, workContext);
  }

  // -------------------------------------------------------------------------
  // READ
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  @Override
  public int getAnonymousEventReadyForSynchronizationCount(Date skipChangedBefore) {
    EventSearchParams params =
        new EventSearchParams()
            .setProgramType(ProgramType.WITHOUT_REGISTRATION)
            .setIncludeDeleted(true)
            .setSynchronizationQuery(true)
            .setSkipChangedBefore(skipChangedBefore);

    return eventStore.getEventCount(params);
  }

  @Override
  public Events getAnonymousEventsForSync(
      int pageSize, Date skipChangedBefore, Map<String, Set<String>> psdesWithSkipSyncTrue) {
    // A page is not specified here as it would lead to SQLGrammarException
    // after a successful sync of few pages, as total count will change
    // and offset won't be valid.

    EventSearchParams params =
        new EventSearchParams()
            .setProgramType(ProgramType.WITHOUT_REGISTRATION)
            .setIncludeDeleted(true)
            .setSynchronizationQuery(true)
            .setPageSize(pageSize)
            .setSkipChangedBefore(skipChangedBefore);

    Events anonymousEvents = new Events();
    List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events =
        eventStore.getEvents(params, psdesWithSkipSyncTrue);
    anonymousEvents.setEvents(events);
    return anonymousEvents;
  }

  @Transactional(readOnly = true)
  @Override
  public EventRows getEventRows(EventSearchParams params) {
    EventRows eventRows = new EventRows();

    List<EventRow> eventRowList = eventStore.getEventRows(params);

    EventContext eventContext = eventServiceContextBuilder.build(eventRowList);

    for (EventRow eventRow : eventRowList) {
      Program program = eventContext.getProgramsByUid().get(eventRow.getProgram());
      TrackedEntityOuInfo trackedEntityOuInfo =
          eventContext.getTrackedEntityOuInfoByUid().get(eventRow.getTrackedEntityInstance());

      OrganisationUnit ou =
          Optional.ofNullable(
                  eventContext
                      .getOrgUnitByTeiUidAndProgramUidPairs()
                      .get(Pair.of(eventRow.getTrackedEntityInstance(), eventRow.getProgram())))
              .map(organisationUnitUid -> eventContext.getOrgUnitsByUid().get(organisationUnitUid))
              .orElseGet(
                  () ->
                      organisationUnitService.getOrganisationUnit(trackedEntityOuInfo.orgUnitId()));

      UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
      if (trackerOwnershipAccessManager.hasAccess(
          currentUser, trackedEntityOuInfo.trackedEntityUid(), ou, program)) {
        eventRows.getEventRows().add(eventRow);
      }
    }

    return eventRows;
  }

  @Transactional(readOnly = true)
  @Override
  public org.hisp.dhis.dxf2.deprecated.tracker.event.Event getEvent(
      org.hisp.dhis.program.Event event, EventParams eventParams) {
    return getEvent(event, false, false, eventParams);
  }

  @Transactional(readOnly = true)
  @Override
  public org.hisp.dhis.dxf2.deprecated.tracker.event.Event getEvent(
      org.hisp.dhis.program.Event programStageInstance,
      boolean isSynchronizationQuery,
      boolean skipOwnershipCheck,
      EventParams eventParams) {
    if (programStageInstance == null) {
      return null;
    }

    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
    event.setEvent(programStageInstance.getUid());

    if (programStageInstance.getEnrollment().getTrackedEntity() != null) {
      event.setTrackedEntityInstance(
          programStageInstance.getEnrollment().getTrackedEntity().getUid());
    }

    event.setFollowup(programStageInstance.getEnrollment().getFollowup());
    event.setEnrollmentStatus(
        EnrollmentStatus.fromProgramStatus(programStageInstance.getEnrollment().getStatus()));
    event.setStatus(programStageInstance.getStatus());
    event.setEventDate(DateUtils.toIso8601NoTz(programStageInstance.getOccurredDate()));
    event.setDueDate(DateUtils.toIso8601NoTz(programStageInstance.getScheduledDate()));
    event.setStoredBy(programStageInstance.getStoredBy());
    event.setCompletedBy(programStageInstance.getCompletedBy());
    event.setCompletedDate(DateUtils.toIso8601NoTz(programStageInstance.getCompletedDate()));
    event.setCreated(DateUtils.toIso8601NoTz(programStageInstance.getCreated()));
    event.setCreatedByUserInfo(programStageInstance.getCreatedByUserInfo());
    event.setLastUpdatedByUserInfo(programStageInstance.getLastUpdatedByUserInfo());
    event.setCreatedAtClient(DateUtils.toIso8601NoTz(programStageInstance.getCreatedAtClient()));
    event.setLastUpdated(DateUtils.toIso8601NoTz(programStageInstance.getLastUpdated()));
    event.setLastUpdatedAtClient(
        DateUtils.toIso8601NoTz(programStageInstance.getLastUpdatedAtClient()));
    event.setGeometry(programStageInstance.getGeometry());
    event.setDeleted(programStageInstance.isDeleted());

    if (programStageInstance.getAssignedUser() != null) {
      event.setAssignedUser(programStageInstance.getAssignedUser().getUid());
      event.setAssignedUserUsername(programStageInstance.getAssignedUser().getUsername());
      event.setAssignedUserDisplayName(programStageInstance.getAssignedUser().getName());
      event.setAssignedUserFirstName(programStageInstance.getAssignedUser().getFirstName());
      event.setAssignedUserSurname(programStageInstance.getAssignedUser().getSurname());
    }

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    OrganisationUnit ou = programStageInstance.getOrganisationUnit();

    List<String> errors =
        trackerAccessManager.canRead(currentUser, programStageInstance, skipOwnershipCheck);

    if (!errors.isEmpty()) {
      throw new IllegalQueryException(errors.toString());
    }

    if (ou != null) {
      event.setOrgUnit(ou.getUid());
      event.setOrgUnitName(ou.getName());
    }

    Program program = programStageInstance.getEnrollment().getProgram();

    event.setProgram(program.getUid());
    event.setEnrollment(programStageInstance.getEnrollment().getUid());
    event.setProgramStage(programStageInstance.getProgramStage().getUid());
    CategoryOptionCombo attributeOptionCombo = programStageInstance.getAttributeOptionCombo();
    if (attributeOptionCombo != null) {
      event.setAttributeOptionCombo(attributeOptionCombo.getUid());
      event.setAttributeCategoryOptions(
          String.join(
              ";",
              attributeOptionCombo.getCategoryOptions().stream()
                  .map(CategoryOption::getUid)
                  .collect(Collectors.toList())));
    }
    if (programStageInstance.getEnrollment().getTrackedEntity() != null) {
      event.setTrackedEntityInstance(
          programStageInstance.getEnrollment().getTrackedEntity().getUid());
    }

    Collection<EventDataValue> dataValues;
    if (!isSynchronizationQuery) {
      dataValues = programStageInstance.getEventDataValues();
    } else {
      Set<String> dataElementsToSync =
          programStageInstance.getProgramStage().getProgramStageDataElements().stream()
              .filter(psde -> !psde.getSkipSynchronization())
              .map(psde -> psde.getDataElement().getUid())
              .collect(Collectors.toSet());

      dataValues =
          programStageInstance.getEventDataValues().stream()
              .filter(dv -> dataElementsToSync.contains(dv.getDataElement()))
              .collect(Collectors.toSet());
    }

    for (EventDataValue dataValue : dataValues) {
      if (getDataElement(
          CurrentUserUtil.getCurrentUserDetails().getUid(), dataValue.getDataElement())) {
        DataValue value = new DataValue();
        value.setCreated(DateUtils.toIso8601NoTz(dataValue.getCreated()));
        value.setCreatedByUserInfo(dataValue.getCreatedByUserInfo());
        value.setLastUpdated(DateUtils.toIso8601NoTz(dataValue.getLastUpdated()));
        value.setLastUpdatedByUserInfo(dataValue.getLastUpdatedByUserInfo());
        value.setDataElement(dataValue.getDataElement());
        value.setValue(dataValue.getValue());
        value.setProvidedElsewhere(dataValue.getProvidedElsewhere());
        value.setStoredBy(dataValue.getStoredBy());

        event.getDataValues().add(value);
      } else {
        log.info("Can not find a Data Element having UID [" + dataValue.getDataElement() + "]");
      }
    }

    event.getNotes().addAll(NoteHelper.convertNotes(programStageInstance.getNotes()));

    if (eventParams.isIncludeRelationships()) {
      event.setRelationships(
          programStageInstance.getRelationshipItems().stream()
              .filter(Objects::nonNull)
              .map(
                  r ->
                      relationshipService.findRelationship(
                          r.getRelationship(), RelationshipParams.FALSE, currentUser))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet()));
    }

    return event;
  }

  // -------------------------------------------------------------------------
  // UPDATE
  // -------------------------------------------------------------------------

  @Transactional
  @Override
  public ImportSummaries updateEvents(
      List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events,
      ImportOptions importOptions,
      boolean singleValue,
      boolean clearSession) {
    ImportSummaries importSummaries = new ImportSummaries();
    importOptions = updateImportOptions(importOptions);
    List<List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event>> partitions =
        Lists.partition(events, FLUSH_FREQUENCY);

    for (List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> _events : partitions) {
      reloadUser(importOptions);
      // prepareCaches( importOptions.getUser(), _events );

      for (org.hisp.dhis.dxf2.deprecated.tracker.event.Event event : _events) {
        importSummaries.addImportSummary(updateEvent(event, singleValue, importOptions, true));
      }

      if (clearSession && events.size() >= FLUSH_FREQUENCY) {
        // clearSession( importOptions.getUser() );
      }
    }

    updateEntities(importOptions.getUser());

    return importSummaries;
  }

  @Transactional
  @Override
  public ImportSummary updateEvent(
      org.hisp.dhis.dxf2.deprecated.tracker.event.Event event,
      boolean singleValue,
      ImportOptions importOptions,
      boolean bulkUpdate) {
    ImportOptions localImportOptions = importOptions;

    // API allows null import options
    if (localImportOptions == null) {
      localImportOptions = ImportOptions.getDefaultImportOptions();
    }
    // TODO this doesn't make a lot of sense, but I didn't want to change
    // the EventService interface and preserve the "singleValue" flag
    localImportOptions.setMergeDataValues(singleValue);

    return eventManager.updateEvent(
        event, workContextLoader.load(localImportOptions, Collections.singletonList(event)));
  }

  @Transactional
  @Override
  public void updateEventsSyncTimestamp(List<String> eventsUIDs, Date lastSynchronized) {
    eventService.updateEventsSyncTimestamp(eventsUIDs, lastSynchronized);
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------

  @Transactional
  @Override
  public ImportSummary deleteEvent(String uid) {
    boolean existsEvent = eventService.eventExists(uid);

    if (existsEvent) {
      Event event = eventService.getEvent(uid);

      UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
      List<String> errors = trackerAccessManager.canDelete(currentUser, event, false);

      if (!errors.isEmpty()) {
        return new ImportSummary(ImportStatus.ERROR, errors.toString()).incrementIgnored();
      }

      event.setAutoFields();
      event.setLastUpdatedByUserInfo(UserInfoSnapshot.from(currentUser));
      eventService.deleteEvent(event);

      if (event.getProgramStage().getProgram().isRegistration()) {
        TrackedEntity entity = event.getEnrollment().getTrackedEntity();
        entity.setLastUpdatedByUserInfo(UserInfoSnapshot.from(currentUser));
        entityInstanceService.updateTrackedEntity(entity);
      }

      ImportSummary importSummary =
          new ImportSummary(ImportStatus.SUCCESS, "Deletion of event " + uid + " was successful")
              .incrementDeleted();
      importSummary.setReference(uid);
      return importSummary;
    } else {
      return new ImportSummary(
              ImportStatus.SUCCESS,
              "Event " + uid + " cannot be deleted as it is not present in the system")
          .incrementIgnored();
    }
  }

  @Transactional
  @Override
  public ImportSummaries deleteEvents(List<String> uids, boolean clearSession) {
    ImportSummaries importSummaries = new ImportSummaries();
    for (String uid : uids) {
      importSummaries.addImportSummary(deleteEvent(uid));
    }

    return importSummaries;
  }

  /**
   * Get DataElement by given uid
   *
   * @return FALSE if currentUser doesn't have READ access to given DataElement OR no DataElement
   *     with given uid exist TRUE if DataElement exist and currentUser has READ access
   */
  private boolean getDataElement(String userUid, String dataElementUid) {
    String key = userUid + "-" + dataElementUid;
    return dataElementCache.get(key, k -> manager.get(DataElement.class, dataElementUid) != null);
  }

  private void updateEntities(User user) {
    UserDetails currentUserDetails = UserDetails.fromUser(user);
    trackedEntityInstancesToUpdate.forEach(tei -> manager.update(tei, currentUserDetails));
    trackedEntityInstancesToUpdate.clear();
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

  private void reloadUser(ImportOptions importOptions) {
    if (importOptions == null || importOptions.getUser() == null) {
      return;
    }

    importOptions.setUser(userService.getUser(importOptions.getUser().getId()));
  }
}
