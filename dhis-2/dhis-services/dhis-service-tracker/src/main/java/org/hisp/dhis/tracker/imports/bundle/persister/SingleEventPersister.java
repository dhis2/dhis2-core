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
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLogService;
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
public class SingleEventPersister
    extends AbstractTrackerPersister<
        org.hisp.dhis.tracker.imports.domain.SingleEvent, org.hisp.dhis.program.SingleEvent> {
  private final SingleEventChangeLogService singleEventChangeLogService;

  public SingleEventPersister(
      ReservedValueService reservedValueService,
      TrackedEntityChangeLogService trackedEntityChangeLogService,
      SingleEventChangeLogService eventChangeLogService) {
    super(reservedValueService, trackedEntityChangeLogService);
    this.singleEventChangeLogService = eventChangeLogService;
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, SingleEvent event) {
    preheat.putSingleEvents(Collections.singletonList(event));
  }

  @Override
  protected TrackerNotificationDataBundle handleNotifications(
      TrackerBundle bundle, SingleEvent event, List<NotificationTrigger> triggers) {

    return TrackerNotificationDataBundle.builder()
        .klass(SingleEvent.class)
        .singleEventNotifications(bundle.getSingleEventNotifications().get(UID.of(event)))
        .object(event.getUid())
        .importStrategy(bundle.getImportStrategy())
        .accessedBy(bundle.getUser().getUsername())
        .singleEvent(event)
        .program(event.getProgramStage().getProgram())
        .triggers(triggers)
        .build();
  }

  @Override
  protected List<NotificationTrigger> determineNotificationTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.SingleEvent entity) {
    SingleEvent persistedEvent = preheat.getSingleEvent(entity.getUid());
    List<NotificationTrigger> triggers = new ArrayList<>();
    // If the event is new and has been completed
    if (persistedEvent == null && entity.getStatus() == EventStatus.COMPLETED) {
      triggers.add(NotificationTrigger.SINGLE_EVENT_COMPLETION);
      return triggers;
    }

    // If the event is existing and its status has changed to completed
    if (persistedEvent != null
        && persistedEvent.getStatus() != entity.getStatus()
        && entity.getStatus() == EventStatus.COMPLETED) {
      triggers.add(NotificationTrigger.SINGLE_EVENT_COMPLETION);
      return triggers;
    }

    return triggers;
  }

  @Override
  protected SingleEvent convert(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.SingleEvent event) {
    return TrackerObjectsMapper.mapSingleEvent(bundle.getPreheat(), event, bundle.getUser());
  }

  @Override
  protected SingleEvent cloneEntityProperties(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.SingleEvent event) {
    SingleEvent originalEvent = preheat.getSingleEvent(event.getUid());

    if (originalEvent == null) {
      return new SingleEvent();
    }

    SingleEvent clonedEvent = new SingleEvent();
    clonedEvent.setUid(originalEvent.getUid());
    clonedEvent.setOccurredDate(originalEvent.getOccurredDate());
    clonedEvent.setGeometry(originalEvent.getGeometry());

    return clonedEvent;
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.EVENT;
  }

  @Override
  protected List<org.hisp.dhis.tracker.imports.domain.SingleEvent> getByType(TrackerBundle bundle) {
    return bundle.getSingleEvents();
  }

  @Override
  protected void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.SingleEvent event,
      SingleEvent hibernateEntity,
      UserDetails user) {
    // DO NOTHING - EVENT HAVE NO ATTRIBUTES
  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.SingleEvent event,
      SingleEvent payloadEntity,
      SingleEvent currentEntity,
      UserDetails user) {
    handleDataValues(entityManager, preheat, event.getDataValues(), payloadEntity, user);
    singleEventChangeLogService.addFieldChangeLog(currentEntity, payloadEntity, user.getUsername());
  }

  private void handleDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      Set<DataValue> payloadDataValues,
      SingleEvent event,
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
            singleEventChangeLogService.addEventChangeLog(
                event, dataElement, null, dataValue.getValue(), CREATE, user.getUsername());
            saveDataValue(dataValue, event, dataElement, user, entityManager, preheat);
          } else if (isUpdate(dbDataValue, dataValue)) {
            singleEventChangeLogService.addEventChangeLog(
                event,
                dataElement,
                dbDataValue.getValue(),
                dataValue.getValue(),
                UPDATE,
                user.getUsername());
            updateDataValue(
                dbDataValue, dataValue, event, dataElement, user, entityManager, preheat);
          } else if (isDeletion(dbDataValue, dataValue)) {
            singleEventChangeLogService.addEventChangeLog(
                event, dataElement, dbDataValue.getValue(), null, DELETE, user.getUsername());
            deleteDataValue(dbDataValue, event, dataElement, entityManager, preheat);
          }
        });
  }

  private void saveDataValue(
      DataValue dv,
      SingleEvent event,
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
      SingleEvent event,
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
      SingleEvent event,
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
      org.hisp.dhis.tracker.imports.domain.SingleEvent trackerDto,
      SingleEvent entity) {
    // DO NOTHING. Event creation does not create ownership records.
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(SingleEvent entity) {
    return Stream.of(entity.getEnrollment())
        .filter(e -> e.getTrackedEntity() != null)
        .map(e -> UID.of(e.getTrackedEntity()))
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
