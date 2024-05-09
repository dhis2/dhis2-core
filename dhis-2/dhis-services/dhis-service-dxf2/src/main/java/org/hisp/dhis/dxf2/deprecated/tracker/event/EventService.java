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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @deprecated this is a class related to "old" (deprecated) tracker which will be removed with
 *     "old" tracker. Make sure to plan migrating to new tracker.
 */
@Deprecated(since = "2.41")
public interface EventService {
  /**
   * Returns the count of anonymous event that are ready for synchronization (lastUpdated >
   * lastSynchronized)
   *
   * @param skipChangedBefore the point in time specifying which events will be synchronized and
   *     which not
   * @return the count of anonymous event that are ready for synchronization (lastUpdated >
   *     lastSynchronized)
   */
  int getAnonymousEventReadyForSynchronizationCount(Date skipChangedBefore);

  /**
   * Returns the anonymous events that are supposed to be synchronized (lastUpdated >
   * lastSynchronized)
   *
   * @param pageSize Specifies the max number for the events returned.
   * @param skipChangedBefore the point in time specifying which events will be synchronized and
   *     which not
   * @param psdesWithSkipSyncTrue Holds information about PSDEs for which the data should not be
   *     synchronized
   * @return the anonymous events that are supposed to be synchronized (lastUpdated >
   *     lastSynchronized)
   */
  Events getAnonymousEventsForSync(
      int pageSize, Date skipChangedBefore, Map<String, Set<String>> psdesWithSkipSyncTrue);

  /**
   * Updates a last sync timestamp on specified Events
   *
   * @param eventsUIDs UIDs of Events where the lastSynchronized flag should be updated
   * @param lastSynchronized The date of last successful sync
   */
  void updateEventsSyncTimestamp(List<String> eventsUIDs, Date lastSynchronized);
}
