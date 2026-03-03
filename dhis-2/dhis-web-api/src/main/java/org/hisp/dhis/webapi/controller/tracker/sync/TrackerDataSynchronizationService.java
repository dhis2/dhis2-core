/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.controller.tracker.sync;

import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.trackedentity.TrackedEntityMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class TrackerDataSynchronizationService
    extends BaseDataSynchronizationWithPaging<
        org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity, TrackedEntity> {
  private static final String PROCESS_NAME = "Tracker Data Synchronization";
  private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER =
      Mappers.getMapper(TrackedEntityMapper.class);

  private final TrackedEntityService trackedEntityService;

  public TrackerDataSynchronizationService(
      TrackedEntityService trackedEntityService,
      SystemSettingsService systemSettingsService,
      RestTemplate restTemplate,
      RenderService renderService) {
    super(renderService, restTemplate, systemSettingsService);
    this.trackedEntityService = trackedEntityService;
  }

  @Override
  public void updateEntitySyncTimeStamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      Date syncTime) {
    List<String> trackedEntityUids =
        trackedEntities.stream().map(te -> te.getTrackedEntity().getValue()).toList();

    trackedEntityService.updateTrackedEntitiesSyncTimestamp(UID.of(trackedEntityUids), syncTime);
  }

  @Override
  public List<TrackedEntity> fetchEntitiesForPage(int page, TrackerSynchronizationContext context)
      throws BadRequestException, ForbiddenException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.buildForDataSync(context.getSkipChangedBefore()).build();
    return trackedEntityService
        .findTrackedEntities(params, PageParams.of(page, context.getPageSize(), false))
        .getItems();
  }

  @Override
  public long countEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.buildForDataSync(skipChangedBefore).build();
    return trackedEntityService.getTrackedEntityCount(params);
  }

  @Override
  public boolean isDeleted(TrackedEntity entity) {
    return entity.isDeleted();
  }

  @Override
  public org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity getMappedEntities(
      TrackedEntity ev, TrackerIdSchemeParams idSchemeParam, MappingErrors errors) {
    return TRACKED_ENTITY_MAPPER.map(idSchemeParam, errors, ev);
  }

  @Override
  public String getJsonRootName() {
    return "trackedEntities";
  }

  @Override
  public String getEntityName() {
    return "Tracked Entities";
  }

  @Override
  public String getProcessName() {
    return PROCESS_NAME;
  }

  @Override
  public org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity toMinimalEntity(
      TrackedEntity trackedEntity) {
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity minimalTrackedEntity =
        new org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity();
    minimalTrackedEntity.setTrackedEntity(UID.of(trackedEntity.getUid()));
    return minimalTrackedEntity;
  }

  @Override
  public TrackerSynchronizationContext createContext(int pageSize, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();

    long trackedEntityCount = countEntitiesForSynchronization(skipChangedBefore);

    if (trackedEntityCount == 0) {
      return TrackerSynchronizationContext.emptyContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);

    return TrackerSynchronizationContext.forTrackedEntities(
        skipChangedBefore, trackedEntityCount, instance, pageSize);
  }
}
