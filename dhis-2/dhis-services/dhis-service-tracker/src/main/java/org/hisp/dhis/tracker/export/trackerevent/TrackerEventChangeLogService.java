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
package org.hisp.dhis.tracker.export.trackerevent;

import java.util.Date;
import javax.annotation.Nonnull;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.HibernateEventChangeLogStore;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLogService")
public class TrackerEventChangeLogService
    extends EventChangeLogService<TrackerEventChangeLog, TrackerEvent> {

  protected TrackerEventChangeLogService(
      TrackerEventService trackerEventService,
      HibernateEventChangeLogStore<TrackerEventChangeLog, TrackerEvent>
          hibernateEventChangeLogStore,
      DhisConfigurationProvider config) {
    super(trackerEventService, hibernateEventChangeLogStore, config);
  }

  @Override
  public TrackerEventChangeLog buildEventChangeLog(
      TrackerEvent event,
      DataElement dataElement,
      String eventField,
      String previousValue,
      String value,
      ChangeLogType changeLogType,
      Date created,
      String userName) {
    return new TrackerEventChangeLog(
        event, dataElement, eventField, previousValue, value, changeLogType, created, userName);
  }

  @Override
  public void addEntityFieldChangeLog(
      @Nonnull TrackerEvent currentEvent, @Nonnull TrackerEvent event, @Nonnull String username) {
    logIfChanged(
        "scheduledAt",
        TrackerEvent::getScheduledDate,
        EventChangeLogService::formatDate,
        currentEvent,
        event,
        username);
    logIfChanged(
        "occurredAt",
        TrackerEvent::getOccurredDate,
        EventChangeLogService::formatDate,
        currentEvent,
        event,
        username);
    logIfChanged(
        "geometry",
        TrackerEvent::getGeometry,
        EventChangeLogService::formatGeometry,
        currentEvent,
        event,
        username);
  }
}
