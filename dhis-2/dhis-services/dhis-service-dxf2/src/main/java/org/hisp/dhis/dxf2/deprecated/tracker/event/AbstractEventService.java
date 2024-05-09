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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramType;
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

  protected EventService eventService;

  protected EventStore eventStore;

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

  @Transactional
  @Override
  public void updateEventsSyncTimestamp(List<String> eventsUIDs, Date lastSynchronized) {
    eventService.updateEventsSyncTimestamp(eventsUIDs, lastSynchronized);
  }
}
