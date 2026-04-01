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
package org.hisp.dhis.tracker.imports.bundle.persister;

import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.hisp.dhis.changelog.ChangeLogType.DELETE;
import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;

import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLog;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.notification.EntityNotifications;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class TrackerEventPersister
    extends AbstractTrackerPersister<
        org.hisp.dhis.tracker.imports.domain.TrackerEvent, TrackerEvent> {
  public TrackerEventPersister(ReservedValueService reservedValueService) {
    super(reservedValueService);
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, TrackerEvent event) {
    preheat.putTrackerEvents(Collections.singletonList(event));
  }

  @Override
  protected boolean isBeingCompleted(
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackerEvent entity,
      boolean isNew) {
    if (entity.getStatus() != EventStatus.COMPLETED) {
      return false;
    }
    if (isNew) {
      return true;
    }
    TrackerEvent persisted = preheat.getTrackerEvent(entity.getUID());
    return persisted != null && persisted.getStatus() != EventStatus.COMPLETED;
  }

  @Override
  protected EntityNotifications collectNotifications(
      TrackerBundle bundle, TrackerEvent event, boolean isNew, boolean completedInThisImport) {
    Set<ProgramNotificationTemplate> matchedTemplates =
        completedInThisImport
            ? filterTemplates(
                event.getProgramStage().getNotificationTemplates(),
                EnumSet.of(NotificationTrigger.COMPLETION))
            : Set.of();
    List<Notification> ruleEngineNotifications =
        bundle.getTrackerEventNotifications().getOrDefault(event.getUID(), List.of());

    Set<Notification> notifications = mergeNotifications(matchedTemplates, ruleEngineNotifications);
    return notifications.isEmpty() ? null : new EntityNotifications(event, notifications);
  }

  @Override
  protected TrackerEvent convert(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.TrackerEvent event) {
    return TrackerObjectsMapper.map(bundle.getPreheat(), event, bundle.getUser());
  }

  @Override
  protected TrackerEvent cloneEntityProperties(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.TrackerEvent event) {
    TrackerEvent originalEvent = preheat.getTrackerEvent(event.getUID());

    if (originalEvent == null) {
      return new TrackerEvent();
    }

    TrackerEvent clonedEvent = new TrackerEvent();
    clonedEvent.setUid(originalEvent.getUid());
    clonedEvent.setOccurredDate(originalEvent.getOccurredDate());
    clonedEvent.setScheduledDate(originalEvent.getScheduledDate());
    clonedEvent.setGeometry(originalEvent.getGeometry());

    return clonedEvent;
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.EVENT;
  }

  @Override
  protected List<org.hisp.dhis.tracker.imports.domain.TrackerEvent> getByType(
      TrackerBundle bundle) {
    return bundle.getTrackerEvents();
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackerEvent event,
      TrackerEvent hibernateEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs) {
    // DO NOTHING - EVENT HAVE NO ATTRIBUTES
  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackerEvent event,
      TrackerEvent payloadEntity,
      TrackerEvent currentEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs) {
    handleDataValues(
        entityManager, preheat, event.getDataValues(), payloadEntity, user, changeLogs);
    logFieldChanges(currentEntity, payloadEntity, user.getUsername(), changeLogs);
  }

  private void handleDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      Set<DataValue> payloadDataValues,
      TrackerEvent event,
      UserDetails user,
      ChangeLogAccumulator changeLogs) {
    Program program = event.getProgramStage().getProgram();
    Map<String, EventDataValue> dataValueDBMap =
        event.getEventDataValues().stream()
            .collect(Collectors.toMap(EventDataValue::getDataElement, Function.identity()));

    payloadDataValues.forEach(
        dataValue -> {
          DataElement dataElement = preheat.getDataElement(dataValue.getDataElement());
          EventDataValue dbDataValue = dataValueDBMap.get(dataElement.getUid());

          if (isNewDataValue(dbDataValue, dataValue)) {
            addEventChangeLog(
                changeLogs, event, dataElement, program, null, dataValue.getValue(), CREATE, user);
            saveDataValue(dataValue, event, dataElement, user, entityManager, preheat);
          } else if (isUpdate(dbDataValue, dataValue)) {
            addEventChangeLog(
                changeLogs,
                event,
                dataElement,
                program,
                dbDataValue.getValue(),
                dataValue.getValue(),
                UPDATE,
                user);
            updateDataValue(
                dbDataValue, dataValue, event, dataElement, user, entityManager, preheat);
          } else if (isDeletion(dbDataValue, dataValue)) {
            addEventChangeLog(
                changeLogs,
                event,
                dataElement,
                program,
                dbDataValue.getValue(),
                null,
                DELETE,
                user);
            deleteDataValue(dbDataValue, event, dataElement, entityManager, preheat);
          }
        });
  }

  private static void addEventChangeLog(
      ChangeLogAccumulator changeLogs,
      TrackerEvent event,
      DataElement dataElement,
      Program program,
      String previousValue,
      String currentValue,
      ChangeLogType changeLogType,
      UserDetails user) {
    if (program.isEnableChangeLog()) {
      changeLogs.addTrackerEventChangeLog(
          new TrackerEventChangeLog(
              event,
              dataElement,
              null,
              previousValue,
              currentValue,
              changeLogType,
              new Date(),
              user.getUsername()));
    }
  }

  private static void logFieldChanges(
      TrackerEvent currentEntity,
      TrackerEvent payloadEntity,
      String username,
      ChangeLogAccumulator changeLogs) {
    Program program = payloadEntity.getProgramStage().getProgram();
    if (!program.isEnableChangeLog()) {
      return;
    }

    logFieldChange(
        changeLogs,
        payloadEntity,
        "scheduledAt",
        EventChangeLogService.formatDate(currentEntity.getScheduledDate()),
        EventChangeLogService.formatDate(payloadEntity.getScheduledDate()),
        username);
    logFieldChange(
        changeLogs,
        payloadEntity,
        "occurredAt",
        EventChangeLogService.formatDate(currentEntity.getOccurredDate()),
        EventChangeLogService.formatDate(payloadEntity.getOccurredDate()),
        username);
    logFieldChange(
        changeLogs,
        payloadEntity,
        "geometry",
        EventChangeLogService.formatGeometry(currentEntity.getGeometry()),
        EventChangeLogService.formatGeometry(payloadEntity.getGeometry()),
        username);
  }

  private static void logFieldChange(
      ChangeLogAccumulator changeLogs,
      TrackerEvent event,
      String field,
      String currentValue,
      String newValue,
      String username) {
    if (!Objects.equals(currentValue, newValue)) {
      ChangeLogType changeLogType;
      if (currentValue == null) {
        changeLogType = CREATE;
      } else if (newValue == null) {
        changeLogType = DELETE;
      } else {
        changeLogType = UPDATE;
      }
      changeLogs.addTrackerEventChangeLog(
          new TrackerEventChangeLog(
              event, null, field, currentValue, newValue, changeLogType, new Date(), username));
    }
  }

  private void saveDataValue(
      DataValue dv,
      TrackerEvent event,
      DataElement dataElement,
      UserDetails user,
      EntityManager entityManager,
      TrackerPreheat preheat) {
    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setDataElement(dataElement.getUid());
    eventDataValue.setCreated(new Date());
    eventDataValue.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    eventDataValue.setStoredBy(user.getUsername());

    eventDataValue.setLastUpdated(new Date());
    eventDataValue.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));

    eventDataValue.setValue(dv.getValue());
    eventDataValue.setProvidedElsewhere(dv.isProvidedElsewhere());

    if (dataElement.isFileType()) {
      assignFileResource(entityManager, preheat, event.getUid(), eventDataValue.getValue());
    }

    event.getEventDataValues().add(eventDataValue);
  }

  private void updateDataValue(
      EventDataValue eventDataValue,
      DataValue dv,
      TrackerEvent event,
      DataElement dataElement,
      UserDetails user,
      EntityManager entityManager,
      TrackerPreheat preheat) {
    eventDataValue.setLastUpdated(new Date());
    eventDataValue.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));

    if (dataElement.isFileType()) {
      unassignFileResource(entityManager, preheat, event.getUid(), eventDataValue.getValue());
      assignFileResource(entityManager, preheat, event.getUid(), dv.getValue());
    }

    eventDataValue.setProvidedElsewhere(dv.isProvidedElsewhere());
    eventDataValue.setValue(dv.getValue());
  }

  private void deleteDataValue(
      EventDataValue eventDataValue,
      TrackerEvent event,
      DataElement dataElement,
      EntityManager entityManager,
      TrackerPreheat preheat) {
    if (dataElement.isFileType()) {
      unassignFileResource(entityManager, preheat, event.getUid(), eventDataValue.getValue());
    }

    event.getEventDataValues().remove(eventDataValue);
  }

  @Override
  protected void persistOwnership(
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackerEvent trackerDto,
      TrackerEvent entity) {
    // DO NOTHING. Event creation does not create ownership records.
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(TrackerEvent entity) {
    return Stream.of(entity.getEnrollment())
        .filter(e -> e.getTrackedEntity() != null)
        .map(e -> e.getTrackedEntity().getUID())
        .collect(Collectors.toSet());
  }

  private boolean isNewDataValue(
      @CheckForNull EventDataValue eventDataValue, @Nonnull DataValue dv) {
    return eventDataValue == null && !StringUtils.isBlank(dv.getValue());
  }

  private boolean isDeletion(@CheckForNull EventDataValue eventDataValue, @Nonnull DataValue dv) {
    return eventDataValue != null
        && StringUtils.isNotBlank(eventDataValue.getValue())
        && StringUtils.isBlank(dv.getValue());
  }

  private boolean isUpdate(@CheckForNull EventDataValue eventDataValue, @Nonnull DataValue dv) {
    return eventDataValue != null
        && !StringUtils.isBlank(dv.getValue())
        && !StringUtils.equals(dv.getValue(), eventDataValue.getValue());
  }
}
