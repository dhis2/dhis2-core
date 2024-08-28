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
package org.hisp.dhis.tracker.deprecated.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAudit;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.trackedentity.TrackedEntityChangeLogService")
public class DefaultTrackedEntityAuditService implements TrackedEntityAuditService {
  private final TrackedEntityAuditStore trackedEntityAuditStore;

  private final IdentifiableObjectManager manager;

  private final TrackerAccessManager trackerAccessManager;

  @Override
  @Async
  @Transactional
  public void addTrackedEntityAudit(
      TrackedEntity trackedEntity, String username, ChangeLogType changeLogType) {
    if (username != null
        && trackedEntity != null
        && trackedEntity.getTrackedEntityType() != null
        && trackedEntity.getTrackedEntityType().isAllowAuditLog()) {
      TrackedEntityAudit trackedEntityAudit =
          new TrackedEntityAudit(trackedEntity.getUid(), username, changeLogType);
      trackedEntityAuditStore.addTrackedEntityAudit(trackedEntityAudit);
    }
  }

  @Override
  @Async
  @Transactional
  public void addTrackedEntityAudit(List<TrackedEntityAudit> trackedEntityAudits) {
    trackedEntityAuditStore.addTrackedEntityAudit(trackedEntityAudits);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAudit> getTrackedEntityAudits(TrackedEntityAuditQueryParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    return trackedEntityAuditStore.getTrackedEntityAudit(params).stream()
        .filter(
            a ->
                trackerAccessManager
                    .canRead(currentUser, manager.get(TrackedEntity.class, a.getTrackedEntity()))
                    .isEmpty())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public int getTrackedEntityAuditsCount(TrackedEntityAuditQueryParams params) {
    return trackedEntityAuditStore.getTrackedEntityAuditCount(params);
  }
}
