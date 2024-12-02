/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.event.EventChangeLogService")
@RequiredArgsConstructor
public class DefaultEventChangeLogService implements EventChangeLogService {

  private final EventService eventService;
  private final HibernateEventChangeLogStore hibernateEventChangeLogStore;
  private final HibernateTrackedEntityDataValueChangeLogStore trackedEntityDataValueChangeLogStore;

  @Override
  @Transactional(readOnly = true)
  public Page<EventChangeLog> getEventChangeLog(
      UID event, EventChangeLogOperationParams operationParams, PageParams pageParams)
      throws NotFoundException, ForbiddenException {
    // check existence and access
    eventService.getEvent(event);

    return hibernateEventChangeLogStore.getEventChangeLogs(event, operationParams, pageParams);
  }

  @Transactional
  @Override
  public void deleteEventChangeLog(Event event) {
    hibernateEventChangeLogStore.deleteEventChangeLog(event);
  }

  @Transactional
  @Override
  public void deleteEventChangeLog(DataElement dataElement) {
    hibernateEventChangeLogStore.deleteEventChangeLog(dataElement);
  }

  @Override
  @Transactional
  public void addDataValueChangeLog(
      Event event,
      DataElement dataElement,
      String previousValue,
      String value,
      ChangeLogType changeLogType,
      String userName) {

    EventChangeLog eventChangeLog =
        new EventChangeLog(
            event, dataElement, null, previousValue, value, changeLogType, new Date(), userName);

    hibernateEventChangeLogStore.addEventChangeLog(eventChangeLog);
  }

  @Override
  @Transactional
  public void addPropertyChangeLog(
      @Nonnull Event currentEvent, @Nonnull Event event, @Nonnull String username) {
    logIfChanged(
        "occurredAt", Event::getOccurredDate, this::formatDate, currentEvent, event, username);
    logIfChanged(
        "scheduledAt", Event::getScheduledDate, this::formatDate, currentEvent, event, username);
    logIfChanged(
        "geometry", Event::getGeometry, this::formatGeometry, currentEvent, event, username);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityDataValueChangeLog(Event event) {
    trackedEntityDataValueChangeLogStore.deleteTrackedEntityDataValueChangeLog(event);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityDataValueChangeLog(DataElement dataElement) {
    trackedEntityDataValueChangeLogStore.deleteTrackedEntityDataValueChangeLog(dataElement);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getOrderableFields() {
    return hibernateEventChangeLogStore.getOrderableFields();
  }

  @Override
  public Set<Pair<String, Class<?>>> getFilterableFields() {
    return hibernateEventChangeLogStore.getFilterableFields();
  }

  private <T> void logIfChanged(
      String propertyName,
      Function<Event, T> valueExtractor,
      Function<T, String> formatter,
      Event currentEvent,
      Event event,
      String userName) {

    String currentValue = formatter.apply(valueExtractor.apply(currentEvent));
    String newValue = formatter.apply(valueExtractor.apply(event));

    if (!Objects.equals(currentValue, newValue)) {
      ChangeLogType changeLogType = getChangeLogType(currentValue, newValue);

      EventChangeLog eventChangeLog =
          new EventChangeLog(
              event,
              null,
              propertyName,
              currentValue,
              newValue,
              changeLogType,
              new Date(),
              userName);

      hibernateEventChangeLogStore.addEventChangeLog(eventChangeLog);
    }
  }

  private ChangeLogType getChangeLogType(String oldValue, String newValue) {
    if (isNewProperty(oldValue, newValue)) {
      return ChangeLogType.CREATE;
    } else if (isUpdateProperty(oldValue, newValue)) {
      return ChangeLogType.UPDATE;
    } else {
      return ChangeLogType.DELETE;
    }
  }

  private boolean isNewProperty(String originalValue, String payloadValue) {
    return originalValue == null && payloadValue != null;
  }

  private boolean isUpdateProperty(String originalValue, String payloadValue) {
    return originalValue != null && payloadValue != null;
  }

  private String formatDate(Date date) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    return date != null ? formatter.format(date) : null;
  }

  private String formatGeometry(Geometry geometry) {
    if (geometry == null) {
      return null;
    }

    return Stream.of(geometry.getCoordinates())
        .map(c -> String.format("(%f, %f)", c.x, c.y))
        .collect(Collectors.joining(", "));
  }
}
