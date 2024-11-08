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
package org.hisp.dhis.tracker.imports.bundle.persister;

import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.hisp.dhis.changelog.ChangeLogType.DELETE;
import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.job.NotificationTrigger;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EventPersister
    extends AbstractTrackerPersister<org.hisp.dhis.tracker.imports.domain.Event, Event> {
  private final EventChangeLogService eventChangeLogService;

  public EventPersister(
      ReservedValueService reservedValueService,
      TrackedEntityChangeLogService trackedEntityChangeLogService,
      EventChangeLogService eventChangeLogService) {
    super(reservedValueService, trackedEntityChangeLogService);
    this.eventChangeLogService = eventChangeLogService;
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, Event event) {
    preheat.putEvents(Collections.singletonList(event));
  }

  @Override
  protected TrackerNotificationDataBundle handleNotifications(
      TrackerBundle bundle, Event event, List<NotificationTrigger> triggers) {

    return TrackerNotificationDataBundle.builder()
        .klass(Event.class)
        .eventNotifications(bundle.getEventNotifications().get(UID.of(event)))
        .object(event.getUid())
        .importStrategy(bundle.getImportStrategy())
        .accessedBy(bundle.getUser().getUsername())
        .event(event)
        .program(event.getProgramStage().getProgram())
        .triggers(triggers)
        .build();
  }

  @Override
  protected List<NotificationTrigger> determineNotificationTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Event entity) {
    Event persistedEvent = preheat.getEvent(entity.getUid());
    List<NotificationTrigger> triggers = new ArrayList<>();
    // If the event is new and has been completed
    if (persistedEvent == null && entity.getStatus() == EventStatus.COMPLETED) {
      triggers.add(NotificationTrigger.EVENT_COMPLETION);
      return triggers;
    }

    // If the event is existing and its status has changed to completed
    if (persistedEvent != null
        && persistedEvent.getStatus() != entity.getStatus()
        && entity.getStatus() == EventStatus.COMPLETED) {
      triggers.add(NotificationTrigger.EVENT_COMPLETION);
      return triggers;
    }

    return triggers;
  }

  @Override
  protected Event convert(TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    return TrackerObjectsMapper.map(bundle.getPreheat(), event, bundle.getUser());
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.EVENT;
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Event event,
      Event hibernateEntity,
      UserDetails user) {
    // DO NOTHING - EVENT HAVE NO ATTRIBUTES
  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Event event,
      Event hibernateEntity,
      UserDetails user) {
    handleDataValues(entityManager, preheat, event.getDataValues(), hibernateEntity, user);
  }

  private void handleDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      Set<DataValue> payloadDataValues,
      Event event,
      UserDetails user) {
    Map<String, EventDataValue> dataValueDBMap =
        Optional.ofNullable(event)
            .map(
                a ->
                    a.getEventDataValues().stream()
                        .collect(
                            Collectors.toMap(EventDataValue::getDataElement, Function.identity())))
            .orElse(new HashMap<>());

    payloadDataValues.forEach(
        dataValue -> {
          DataElement dataElement = preheat.getDataElement(dataValue.getDataElement());
          EventDataValue dbDataValue = dataValueDBMap.get(dataElement.getUid());

          if (isNewDataValue(dbDataValue, dataValue)) {
            eventChangeLogService.addDataValueChangeLog(
                event, dataElement, dataValue.getValue(), null, CREATE, user.getUsername());
            saveDataValue(dataValue, event, dataElement, user, entityManager, preheat);
          } else if (isUpdate(dbDataValue, dataValue)) {
            eventChangeLogService.addDataValueChangeLog(
                event,
                dataElement,
                dataValue.getValue(),
                dbDataValue.getValue(),
                UPDATE,
                user.getUsername());
            updateDataValue(
                dbDataValue, dataValue, event, dataElement, user, entityManager, preheat);
          } else if (isDeletion(dbDataValue, dataValue)) {
            eventChangeLogService.addDataValueChangeLog(
                event, dataElement, null, dbDataValue.getValue(), DELETE, user.getUsername());
            deleteDataValue(dbDataValue, event, dataElement, entityManager, preheat);
          }
        });
  }

  private void saveDataValue(
      DataValue dv,
      Event event,
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
      Event event,
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
      Event event,
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
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event trackerDto, Event entity) {
    // DO NOTHING. Event creation does not create ownership records.
  }

  @Override
  protected String getUpdatedTrackedEntity(Event entity) {
    return Optional.ofNullable(entity.getEnrollment())
        .filter(e -> e.getTrackedEntity() != null)
        .map(e -> e.getTrackedEntity().getUid())
        .orElse(null);
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
