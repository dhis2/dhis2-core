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

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.event.EventChangeLogService")
@RequiredArgsConstructor
public class DefaultEventChangeLogService implements EventChangeLogService {

  private final EventService eventService;
  private final JdbcEventChangeLogStore jdbcEventChangeLogStore;
  private final HibernateTrackedEntityDataValueChangeLogStore trackedEntityDataValueChangeLogStore;
  private final TrackerAccessManager trackerAccessManager;

  @Transactional
  @Override
  public void addEventChangeLog(
      Event event,
      DataElement dataElement,
      String eventProperty,
      String currentValue,
      String previousValue,
      ChangeLogType changeLogType,
      String userName) {
    jdbcEventChangeLogStore.addEventChangeLog(
        event, dataElement, eventProperty, currentValue, previousValue, changeLogType, userName);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<EventChangeLog> getEventChangeLog(
      UID event, EventChangeLogOperationParams operationParams, PageParams pageParams)
      throws NotFoundException, ForbiddenException {
    // check existence and access
    eventService.getEvent(event);

    return jdbcEventChangeLogStore.getEventChangeLog(event, operationParams.getOrder(), pageParams);
  }

  @Transactional
  @Override
  public void deleteEventChangeLog(Event event) {
    jdbcEventChangeLogStore.deleteEventChangeLog(event);
  }

  @Transactional
  @Override
  public void deleteEventChangeLog(DataElement dataElement) {
    jdbcEventChangeLogStore.deleteEventChangeLog(dataElement);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityDataValueChangeLog> getTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params) {

    return trackedEntityDataValueChangeLogStore.getTrackedEntityDataValueChangeLogs(params).stream()
        .filter(
            changeLog ->
                trackerAccessManager
                    .canRead(
                        getCurrentUserDetails(),
                        changeLog.getEvent(),
                        changeLog.getDataElement(),
                        false)
                    .isEmpty())
        .toList();
  }

  @Override
  @Transactional
  public void addTrackedEntityDataValueChangeLog(
      TrackedEntityDataValueChangeLog trackedEntityDataValueChangeLog) {
    trackedEntityDataValueChangeLogStore.addTrackedEntityDataValueChangeLog(
        trackedEntityDataValueChangeLog);
  }

  @Override
  @Transactional(readOnly = true)
  public int countTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params) {
    return trackedEntityDataValueChangeLogStore.countTrackedEntityDataValueChangeLogs(params);
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
    return jdbcEventChangeLogStore.getOrderableFields();
  }
}
