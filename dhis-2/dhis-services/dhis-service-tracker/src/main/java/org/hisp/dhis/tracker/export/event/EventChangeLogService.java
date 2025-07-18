/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.hisp.dhis.changelog.ChangeLogType.DELETE;
import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.locationtech.jts.geom.Geometry;
import org.springframework.transaction.annotation.Transactional;

public abstract class EventChangeLogService<T, S extends SoftDeletableObject> {

  private final EventService eventService;
  private final HibernateEventChangeLogStore<T, S> hibernateEventChangeLogStore;
  private final DhisConfigurationProvider config;

  protected EventChangeLogService(
      EventService eventService,
      HibernateEventChangeLogStore<T, S> hibernateEventChangeLogStore,
      DhisConfigurationProvider config) {
    this.eventService = eventService;
    this.hibernateEventChangeLogStore = hibernateEventChangeLogStore;
    this.config = config;
  }

  public abstract T buildEventChangeLog(
      S event,
      DataElement dataElement,
      String eventField,
      String previousValue,
      String value,
      ChangeLogType changeLogType,
      Date created,
      String userName);

  @Nonnull
  @Transactional(readOnly = true)
  public Page<EventChangeLog> getEventChangeLog(
      UID event, EventChangeLogOperationParams operationParams, PageParams pageParams)
      throws NotFoundException {
    if (!eventService.exists(event)) {
      throw new NotFoundException(Event.class, event);
    }

    return hibernateEventChangeLogStore.getEventChangeLogs(event, operationParams, pageParams);
  }

  @Transactional
  public void deleteEventChangeLog(S event) {
    hibernateEventChangeLogStore.deleteEventChangeLog(event);
  }

  @Transactional
  public void deleteEventChangeLog(DataElement dataElement) {
    hibernateEventChangeLogStore.deleteEventChangeLog(dataElement);
  }

  @Transactional
  public void addEventChangeLog(
      S event,
      DataElement dataElement,
      String previousValue,
      String value,
      ChangeLogType changeLogType,
      String userName) {
    if (config.isDisabled(CHANGELOG_TRACKER)) {
      return;
    }

    T eventChangeLog =
        buildEventChangeLog(
            event, dataElement, null, previousValue, value, changeLogType, new Date(), userName);

    hibernateEventChangeLogStore.addEventChangeLog(eventChangeLog);
  }

  @Transactional
  public void addFieldChangeLog(
      @Nonnull S currentEvent, @Nonnull S event, @Nonnull String username) {
    if (config.isDisabled(CHANGELOG_TRACKER)) {
      return;
    }
    addEntityFieldChangeLog(currentEvent, event, username);
  }

  public abstract void addEntityFieldChangeLog(
      @Nonnull S currentEvent, @Nonnull S event, @Nonnull String username);

  @Transactional(readOnly = true)
  public Set<String> getOrderableFields() {
    return hibernateEventChangeLogStore.getOrderableFields();
  }

  public Set<Pair<String, Class<?>>> getFilterableFields() {
    return hibernateEventChangeLogStore.getFilterableFields();
  }

  protected <V> void logIfChanged(
      String field,
      Function<S, V> valueExtractor,
      Function<V, String> formatter,
      S currentEvent,
      S event,
      String userName) {

    String currentValue = formatter.apply(valueExtractor.apply(currentEvent));
    String newValue = formatter.apply(valueExtractor.apply(event));

    if (!Objects.equals(currentValue, newValue)) {
      ChangeLogType changeLogType = getChangeLogType(currentValue, newValue);

      T eventChangeLog =
          buildEventChangeLog(
              event, null, field, currentValue, newValue, changeLogType, new Date(), userName);

      hibernateEventChangeLogStore.addEventChangeLog(eventChangeLog);
    }
  }

  private static ChangeLogType getChangeLogType(String oldValue, String newValue) {
    if (isFieldCreated(oldValue, newValue)) {
      return CREATE;
    } else if (isFieldUpdated(oldValue, newValue)) {
      return UPDATE;
    } else {
      return DELETE;
    }
  }

  private static boolean isFieldCreated(String originalValue, String payloadValue) {
    return originalValue == null && payloadValue != null;
  }

  private static boolean isFieldUpdated(String originalValue, String payloadValue) {
    return originalValue != null && payloadValue != null;
  }

  public static String formatDate(Date date) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    return date != null ? formatter.format(date) : null;
  }

  public static String formatGeometry(Geometry geometry) {
    if (geometry == null) {
      return null;
    }

    return Stream.of(geometry.getCoordinates())
        .map(c -> String.format("(%f, %f)", c.x, c.y))
        .collect(Collectors.joining(", "));
  }
}
