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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueChangeLogService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew Gizaw
 */
@Slf4j
@Service("org.hisp.dhis.trackedentity.TrackedEntityService")
public class DefaultTrackedEntityService implements TrackedEntityService {
  private final TrackedEntityStore trackedEntityStore;

  private final TrackedEntityChangeLogService trackedEntityChangeLogService;

  public DefaultTrackedEntityService(
      TrackedEntityStore trackedEntityStore,
      TrackedEntityAttributeValueService attributeValueService,
      TrackedEntityAttributeService attributeService,
      TrackedEntityTypeService trackedEntityTypeService,
      OrganisationUnitService organisationUnitService,
      AclService aclService,
      TrackedEntityChangeLogService trackedEntityChangeLogService,
      TrackedEntityAttributeValueChangeLogService attributeValueAuditService) {
    checkNotNull(trackedEntityStore);
    checkNotNull(attributeValueService);
    checkNotNull(attributeService);
    checkNotNull(trackedEntityTypeService);
    checkNotNull(organisationUnitService);
    checkNotNull(aclService);
    checkNotNull(trackedEntityChangeLogService);
    checkNotNull(attributeValueAuditService);

    this.trackedEntityStore = trackedEntityStore;
    this.trackedEntityChangeLogService = trackedEntityChangeLogService;
  }

  @Override
  @Transactional
  public void updateTrackedEntityLastUpdated(
      Set<String> trackedEntityUIDs, Date lastUpdated, String userInfoSnapshot) {
    trackedEntityStore.updateTrackedEntityLastUpdated(
        trackedEntityUIDs, lastUpdated, userInfoSnapshot);
  }

  @Override
  @Transactional
  public TrackedEntity getTrackedEntity(String uid) {
    TrackedEntity te = trackedEntityStore.getByUid(uid);
    addTrackedEntityAudit(te, CurrentUserUtil.getCurrentUsername());

    return te;
  }

  private void addTrackedEntityAudit(TrackedEntity trackedEntity, String username) {
    if (username != null
        && trackedEntity != null
        && trackedEntity.getTrackedEntityType() != null
        && trackedEntity.getTrackedEntityType().isAllowAuditLog()) {
      TrackedEntityChangeLog trackedEntityChangeLog =
          new TrackedEntityChangeLog(trackedEntity.getUid(), username, ChangeLogType.READ);
      trackedEntityChangeLogService.addTrackedEntityChangeLog(trackedEntityChangeLog);
    }
  }
}
