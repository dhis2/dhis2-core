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
package org.hisp.dhis.trackedentitydatavalue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityDataValueAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service("org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService")
public class DefaultTrackedEntityDataValueAuditService
    implements TrackedEntityDataValueAuditService {
  private final TrackedEntityDataValueAuditStore trackedEntityDataValueAuditStore;

  private final Predicate<TrackedEntityDataValueAudit> aclFilter;

  public DefaultTrackedEntityDataValueAuditService(
      TrackedEntityDataValueAuditStore trackedEntityDataValueAuditStore,
      TrackerAccessManager trackerAccessManager,
      CurrentUserService currentUserService) {
    checkNotNull(trackedEntityDataValueAuditStore);
    checkNotNull(trackerAccessManager);
    checkNotNull(currentUserService);

    this.trackedEntityDataValueAuditStore = trackedEntityDataValueAuditStore;

    aclFilter =
        (audit) ->
            trackerAccessManager
                .canRead(
                    currentUserService.getCurrentUser(),
                    audit.getProgramStageInstance(),
                    audit.getDataElement(),
                    false)
                .isEmpty();
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void addTrackedEntityDataValueAudit(
      TrackedEntityDataValueAudit trackedEntityDataValueAudit) {
    trackedEntityDataValueAuditStore.addTrackedEntityDataValueAudit(trackedEntityDataValueAudit);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits(
      TrackedEntityDataValueAuditQueryParams params) {
    return trackedEntityDataValueAuditStore.getTrackedEntityDataValueAudits(params).stream()
        .filter(aclFilter)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public int countTrackedEntityDataValueAudits(TrackedEntityDataValueAuditQueryParams params) {
    return trackedEntityDataValueAuditStore.countTrackedEntityDataValueAudits(params);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityDataValueAudit(DataElement dataElement) {
    trackedEntityDataValueAuditStore.deleteTrackedEntityDataValueAudit(dataElement);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityDataValueAudit(ProgramStageInstance programStageInstance) {
    trackedEntityDataValueAuditStore.deleteTrackedEntityDataValueAudit(programStageInstance);
  }
}
