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

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.job.SideEffectTrigger;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EventPersister
    extends AbstractTrackerPersister<org.hisp.dhis.tracker.imports.domain.Event, Event> {
  private final TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event>
      eventConverter;

  public EventPersister(
      ReservedValueService reservedValueService,
      DhisConfigurationProvider config,
      @Qualifier("aes128StringEncryptor") PBEStringEncryptor encryptor,
      TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event> eventConverter) {
    super(reservedValueService, config, encryptor);
    this.eventConverter = eventConverter;
  }

  @Override
  protected void persistNotes(EntityManager entityManager, TrackerPreheat preheat, Event event) {
    if (!event.getNotes().isEmpty()) {
      for (Note note : event.getNotes()) {
        if (Objects.isNull(preheat.getNote(note.getUid()))) {
          entityManager.persist(note);
        }
      }
    }
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, Event event) {
    preheat.putEvents(Collections.singletonList(event));
  }

  @Override
  protected boolean isNew(TrackerPreheat preheat, String uid) {
    return preheat.getEvent(uid) == null;
  }

  @Override
  protected TrackerSideEffectDataBundle handleSideEffects(
      TrackerBundle bundle, Event event, List<SideEffectTrigger> triggers) {
    boolean hasTemplates = hasMatchingNotificationTemplates(event.getProgramStage(), triggers);
    boolean hasRuleEffects = !bundle.getEventRuleEffects().isEmpty();
    if (!hasTemplates && !hasRuleEffects) {
      return null;
    }

    return TrackerSideEffectDataBundle.builder()
        .klass(Event.class)
        .enrollmentRuleEffects(new HashMap<>())
        .eventRuleEffects(bundle.getEventRuleEffects())
        .object(event.getUid())
        .importStrategy(bundle.getImportStrategy())
        .accessedBy(bundle.getUsername())
        .event(event)
        .program(event.getProgramStage().getProgram())
        .triggers(hasTemplates ? triggers : List.of())
        .build();
  }

  @Override
  protected List<SideEffectTrigger> determineSideEffectTriggers(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Event entity) {
    Event persistedEvent = preheat.getEvent(entity.getUid());
    List<SideEffectTrigger> triggers = new ArrayList<>();
    // If the event is new and has been completed
    if (persistedEvent == null && entity.getStatus() == EventStatus.COMPLETED) {
      triggers.add(SideEffectTrigger.EVENT_COMPLETION);
      return triggers;
    }

    // If the event is existing and its status has changed to completed
    if (persistedEvent != null
        && persistedEvent.getStatus() != entity.getStatus()
        && entity.getStatus() == EventStatus.COMPLETED) {
      triggers.add(SideEffectTrigger.EVENT_COMPLETION);
      return triggers;
    }

    return triggers;
  }

  @Override
  protected Event convert(TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    return eventConverter.from(bundle.getPreheat(), event);
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
      ChangeLogAccumulator changeLogs) {
    // DO NOTHING - EVENT HAVE NO ATTRIBUTES
  }

  @Override
  protected void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.Event event,
      Event hibernateEntity,
      ChangeLogAccumulator changeLogs) {
    handleDataValues(entityManager, preheat, event.getDataValues(), hibernateEntity, changeLogs);
  }

  private void handleDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      Set<DataValue> payloadDataValues,
      Event event,
      ChangeLogAccumulator changeLogs) {
    Map<String, EventDataValue> dataValueDBMap =
        Optional.ofNullable(preheat.getEvent(event.getUid()))
            .map(
                a ->
                    a.getEventDataValues().stream()
                        .collect(
                            Collectors.toMap(EventDataValue::getDataElement, Function.identity())))
            .orElse(new HashMap<>());

    payloadDataValues.forEach(
        dv -> {
          DataElement dataElement = preheat.getDataElement(dv.getDataElement());
          checkNotNull(
              dataElement,
              "Data element should never be NULL here if validation is enforced before commit.");

          // EventDataValue.dataElement contains a UID
          EventDataValue eventDataValue = dataValueDBMap.get(dataElement.getUid());

          ValuesHolder valuesHolder = getAuditAndDateParameters(eventDataValue, dv);
          if (valuesHolder.getChangeLogType() == null) {
            return;
          }

          eventDataValue = valuesHolder.getEventDataValue();
          eventDataValue.setDataElement(dataElement.getUid());
          eventDataValue.setStoredBy(dv.getStoredBy());

          if (StringUtils.isEmpty(dv.getValue())) {
            if (dataElement.isFileType()) {
              unassignFileResource(
                  entityManager, preheat, event.getUid(), eventDataValue.getValue());
            }

            event.getEventDataValues().remove(eventDataValue);
          } else {
            eventDataValue.setValue(dv.getValue());

            if (dataElement.isFileType()) {
              assignFileResource(entityManager, preheat, event.getUid(), eventDataValue.getValue());
            }

            event.getEventDataValues().remove(eventDataValue);
            event.getEventDataValues().add(eventDataValue);
          }

          if (valuesHolder.getChangeLogType() != null) {
            changeLogs.addEventDataValueAudit(
                event,
                dataElement,
                valuesHolder.getValue(),
                valuesHolder.isProvidedElseWhere(),
                valuesHolder.getChangeLogType(),
                preheat.getUsername());
          }
        });
  }

  private Date getFromOrNewDate(DataValue dv, Function<DataValue, Instant> dateGetter) {
    return Optional.of(dv).map(dateGetter).map(DateUtils::fromInstant).orElseGet(Date::new);
  }

  @Override
  protected void persistOwnership(TrackerPreheat preheat, Event entity) {
    // DO NOTHING. Event creation does not create ownership records.
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(Event entity) {
    return Stream.of(entity.getEnrollment())
        .filter(e -> e.getTrackedEntity() != null)
        .map(e -> UID.of(e.getTrackedEntity()))
        .collect(Collectors.toSet());
  }

  private boolean isNewDataValue(EventDataValue eventDataValue, DataValue dv) {
    return StringUtils.isNotBlank(dv.getValue()) && eventDataValue == null;
  }

  private boolean isDeletion(EventDataValue eventDataValue, DataValue dv) {
    return eventDataValue != null
        && StringUtils.isNotBlank(eventDataValue.getValue())
        && StringUtils.isBlank(dv.getValue());
  }

  private boolean isUpdate(EventDataValue eventDataValue, DataValue dv) {
    return eventDataValue != null && !StringUtils.equals(dv.getValue(), eventDataValue.getValue());
  }

  private ValuesHolder getAuditAndDateParameters(EventDataValue eventDataValue, DataValue dv) {
    String persistedValue;

    ChangeLogType changeLogType = null;

    if (isNewDataValue(eventDataValue, dv)) {
      eventDataValue = new EventDataValue();
      eventDataValue.setCreated(new Date());
      eventDataValue.setLastUpdated(getFromOrNewDate(dv, DataValue::getUpdatedAt));
      persistedValue = dv.getValue();
      changeLogType = ChangeLogType.CREATE;
    } else {
      persistedValue = eventDataValue != null ? eventDataValue.getValue() : null;

      if (isUpdate(eventDataValue, dv)) {
        changeLogType = ChangeLogType.UPDATE;
        eventDataValue.setLastUpdated(getFromOrNewDate(dv, DataValue::getUpdatedAt));
      }

      if (isDeletion(eventDataValue, dv)) {
        changeLogType = ChangeLogType.DELETE;
        eventDataValue.setLastUpdated(getFromOrNewDate(dv, DataValue::getUpdatedAt));
      }
    }

    return ValuesHolder.builder()
        .value(persistedValue)
        .providedElseWhere(dv.isProvidedElsewhere())
        .changeLogType(changeLogType)
        .eventDataValue(eventDataValue)
        .build();
  }

  @Data
  @Builder
  static class ValuesHolder {
    private final String value;

    private final boolean providedElseWhere;

    private final ChangeLogType changeLogType;

    private final EventDataValue eventDataValue;
  }
}
