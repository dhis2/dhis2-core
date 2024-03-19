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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.trackedentity.TrackedEntityChangeLogService")
public class DefaultTrackedEntityChangeLogService implements TrackedEntityChangeLogService {
  private final TrackedEntityChangeLogStore trackedEntityChangeLogStore;

  private final TrackedEntityStore trackedEntityStore;

  private final TrackerAccessManager trackerAccessManager;

  private final UserService userService;

  // -------------------------------------------------------------------------
  // TrackedEntityAuditService implementation
  // -------------------------------------------------------------------------

  @Override
  @Async
  @Transactional
  public void addTrackedEntityChangeLog(TrackedEntityChangeLog trackedEntityChangeLog) {
    trackedEntityChangeLogStore.addTrackedEntityChangeLog(trackedEntityChangeLog);
  }

  @Override
  @Async
  @Transactional
  public void addTrackedEntityChangeLog(List<TrackedEntityChangeLog> trackedEntityChangeLogs) {
    trackedEntityChangeLogStore.addTrackedEntityChangeLog(trackedEntityChangeLogs);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityChangeLog> getTrackedEntityChangeLogs(
      TrackedEntityChangeLogQueryParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    return trackedEntityChangeLogStore.getTrackedEntityChangeLogs(params).stream()
        .filter(
            a ->
                trackerAccessManager
                    .canRead(currentUser, trackedEntityStore.getByUid(a.getTrackedEntity()))
                    .isEmpty())
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public int getTrackedEntityChangeLogsCount(TrackedEntityChangeLogQueryParams params) {
    return trackedEntityChangeLogStore.getTrackedEntityChangeLogsCount(params);
  }
}
