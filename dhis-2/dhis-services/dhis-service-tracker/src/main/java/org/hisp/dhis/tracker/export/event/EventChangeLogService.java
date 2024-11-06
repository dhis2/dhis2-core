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

import java.util.List;
import java.util.Set;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;

public interface EventChangeLogService {

  /**
   * Retrieves the change log data for a particular event.
   *
   * @return event change logs page
   */
  Page<EventChangeLog> getEventChangeLog(
      UID event, EventChangeLogOperationParams operationParams, PageParams pageParams)
      throws NotFoundException, ForbiddenException;

  /**
   * @deprecated use {@link EventChangeLogService#getEventChangeLog} instead
   */
  @Deprecated(since = "2.41")
  List<TrackedEntityDataValueChangeLog> getTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params);

  @Deprecated(since = "2.42")
  void addTrackedEntityDataValueChangeLog(
      TrackedEntityDataValueChangeLog trackedEntityDataValueChangeLog);

  void addDataValueChangeLog(
      Event event,
      DataElement dataElement,
      String currentValue,
      String previousValue,
      ChangeLogType changeLogType,
      String userName);

  @Deprecated(since = "2.42")
  int countTrackedEntityDataValueChangeLogs(TrackedEntityDataValueChangeLogQueryParams params);

  void deleteTrackedEntityDataValueChangeLog(Event event);

  void deleteEventChangeLog(Event event);

  void deleteTrackedEntityDataValueChangeLog(DataElement dataElement);

  void deleteEventChangeLog(DataElement dataElement);

  /**
   * Fields the {@link #getEventChangeLog(UID, EventChangeLogOperationParams, PageParams)} can order
   * event change logs by. Ordering by fields other than these is considered a programmer error.
   * Validation of user provided field names should occur before calling {@link
   * #getEventChangeLog(UID, EventChangeLogOperationParams, PageParams)}.
   */
  Set<String> getOrderableFields();
}
