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
package org.hisp.dhis.program;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntity;

/**
 * @author Abyot Asalefew
 */
public interface EventStore extends IdentifiableObjectStore<Event> {
  /**
   * Retrieve an event list on enrollment list with a certain status
   *
   * @param enrollments Enrollment list
   * @param status EventStatus
   * @return Event list
   */
  List<Event> get(Collection<Enrollment> enrollments, EventStatus status);

  /**
   * Get all events by TrackedEntity, optionally filtering by completed.
   *
   * @param entityInstance TrackedEntity
   * @param status EventStatus
   * @return Event list
   */
  List<Event> get(TrackedEntity entityInstance, EventStatus status);

  /**
   * Get the number of events updates since the given Date.
   *
   * @param time the time.
   * @return the number of events.
   */
  long getEventCountLastUpdatedAfter(Date time);

  /**
   * Checks for the existence of an event by UID. The deleted events are not taken into account.
   *
   * @param uid event UID to check for
   * @return true/false depending on result
   */
  boolean exists(String uid);

  /**
   * Checks for the existence of an event by UID. It takes into account also the deleted events.
   *
   * @param uid event UID to check for
   * @return true/false depending on result
   */
  boolean existsIncludingDeleted(String uid);

  /**
   * Returns UIDs of existing events (including deleted) from the provided UIDs.
   *
   * @param uids event UIDs to check
   * @return List containing UIDs of existing events (including deleted)
   */
  List<String> getUidsIncludingDeleted(List<String> uids);

  /**
   * Fetches Event matching the given list of UIDs
   *
   * @param uids a List of UID
   * @return a List containing the Event matching the given parameters list
   */
  List<Event> getIncludingDeleted(List<String> uids);

  /**
   * Get all events which have notifications with the given. ProgramNotificationTemplate scheduled
   * on the given date.
   *
   * @param template the template.
   * @param notificationDate the Date for which the notification is scheduled.
   * @return a list of Event.
   */
  List<Event> getWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate);

  /**
   * Set lastSynchronized timestamp to provided timestamp for provided events.
   *
   * @param eventUids UIDs of events where the lastSynchronized flag should be updated
   * @param lastSynchronized The date of last successful sync
   */
  void updateEventsSyncTimestamp(List<String> eventUids, Date lastSynchronized);
}
