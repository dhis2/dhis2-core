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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.TrackedEntityInstanceStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractTrackedEntityInstanceService implements TrackedEntityInstanceService {
  protected TrackedEntityInstanceStore trackedEntityInstanceStore;

  protected TrackedEntityService teiService;

  protected TrackedEntityAttributeService trackedEntityAttributeService;

  protected TrackedEntityTypeService trackedEntityTypeService;

  protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  protected IdentifiableObjectManager manager;

  protected UserService userService;

  protected DbmsManager dbmsManager;

  protected EnrollmentService programInstanceService;

  protected TrackedEntityChangeLogService trackedEntityChangeLogService;

  protected SchemaService schemaService;

  protected QueryService queryService;

  protected ReservedValueService reservedValueService;

  protected TrackerAccessManager trackerAccessManager;

  protected FileResourceService fileResourceService;

  protected TrackerOwnershipManager trackerOwnershipAccessManager;

  protected TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

  protected TrackedEntityAttributeStore trackedEntityAttributeStore;

  protected ObjectMapper jsonMapper;

  protected ObjectMapper xmlMapper;

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityInstance> getTrackedEntityInstances(
      TrackedEntityQueryParams queryParams,
      TrackedEntityInstanceParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    if (queryParams == null) {
      return Collections.emptyList();
    }
    List<TrackedEntityInstance> trackedEntityInstances;

    final List<Long> ids =
        teiService.getTrackedEntityIds(
            queryParams, skipAccessValidation, skipSearchScopeValidation);

    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    trackedEntityInstances = this.trackedEntityInstanceAggregate.find(ids, params, queryParams);

    addSearchAudit(trackedEntityInstances, queryParams.getUser());

    return trackedEntityInstances;
  }

  private void addSearchAudit(List<TrackedEntityInstance> trackedEntityInstances, User user) {
    if (trackedEntityInstances.isEmpty()) {
      return;
    }
    final String accessedBy =
        user != null ? user.getUsername() : CurrentUserUtil.getCurrentUsername();
    Map<String, TrackedEntityType> tetMap =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .collect(Collectors.toMap(TrackedEntityType::getUid, t -> t));

    List<TrackedEntityChangeLog> auditable =
        trackedEntityInstances.stream()
            .filter(Objects::nonNull)
            .filter(tei -> tei.getTrackedEntityType() != null)
            .filter(tei -> tetMap.get(tei.getTrackedEntityType()).isAllowAuditLog())
            .map(
                tei ->
                    new TrackedEntityChangeLog(
                        tei.getTrackedEntityInstance(), accessedBy, ChangeLogType.SEARCH))
            .collect(Collectors.toList());

    if (!auditable.isEmpty()) {
      trackedEntityChangeLogService.addTrackedEntityChangeLog(auditable);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public int getTrackedEntityInstanceCount(
      TrackedEntityQueryParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    return teiService.getTrackedEntityCount(
        params, skipAccessValidation, skipSearchScopeValidation);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityOuInfo> getTrackedEntityOuInfoByUid(List<String> uids) {
    if (uids == null || uids.isEmpty()) {
      return Collections.emptyList();
    }
    return trackedEntityInstanceStore.getTrackedEntityOuInfoByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityProgramOwnerIds> getTrackedEntityProgramOwnersUidsUsingId(
      List<Long> teiIds, Program program) {
    if (teiIds.isEmpty()) {
      return Collections.emptyList();
    }
    return trackedEntityInstanceStore.getTrackedEntityProgramOwnersUids(teiIds, program.getId());
  }

  @Override
  @Transactional
  public void updateTrackedEntityInstancesSyncTimestamp(
      List<String> entityInstanceUIDs, Date lastSynced) {
    teiService.updateTrackedEntitySyncTimestamp(entityInstanceUIDs, lastSynced);
  }
}
