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
package org.hisp.dhis.trackedentity;

import java.util.List;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
public interface TrackedEntityChangeLogStore {
  String ID = TrackedEntityChangeLogStore.class.getName();

  /**
   * Adds the given tracked entity audit.
   *
   * @param trackedEntityChangeLog the {@link TrackedEntityChangeLog} to add.
   */
  void addTrackedEntityChangeLog(TrackedEntityChangeLog trackedEntityChangeLog);

  /**
   * Adds the given {@link TrackedEntityChangeLog}s.
   *
   * @param trackedEntityChangeLog the list of {@link TrackedEntityChangeLog}.
   */
  void addTrackedEntityChangeLog(List<TrackedEntityChangeLog> trackedEntityChangeLog);

  /**
   * Returns tracked entity audits matching query params
   *
   * @param params tracked entity audit query params
   * @return a list of {@link TrackedEntityChangeLog}.
   */
  List<TrackedEntityChangeLog> getTrackedEntityChangeLogs(TrackedEntityChangeLogQueryParams params);

  /**
   * Returns count of tracked entity audits matching query params
   *
   * @param params tracked entity audit query params
   * @return count of audits.
   */
  int getTrackedEntityChangeLogsCount(TrackedEntityChangeLogQueryParams params);
}
