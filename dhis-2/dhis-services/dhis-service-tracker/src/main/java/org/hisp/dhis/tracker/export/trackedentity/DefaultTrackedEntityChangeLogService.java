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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService")
@RequiredArgsConstructor
public class DefaultTrackedEntityChangeLogService implements TrackedEntityChangeLogService {

  private final TrackedEntityService trackedEntityService;

  private final HibernateTrackedEntityChangeLogStore hibernateTrackedEntityChangeLogStore;

  @Transactional
  @Override
  public void addTrackedEntityChangeLog(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull TrackedEntityAttribute trackedEntityAttribute,
      @CheckForNull String previousValue,
      @CheckForNull String currentValue,
      @Nonnull ChangeLogType changeLogType,
      @Nonnull String username) {

    TrackedEntityChangeLog trackedEntityChangeLog =
        new TrackedEntityChangeLog(
            trackedEntity,
            trackedEntityAttribute,
            previousValue,
            currentValue,
            changeLogType,
            new Date(),
            username);

    hibernateTrackedEntityChangeLogStore.addTrackedEntityChangeLog(trackedEntityChangeLog);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityChangeLogs(TrackedEntity trackedEntity) {
    hibernateTrackedEntityChangeLogStore.deleteTrackedEntityChangeLogs(trackedEntity);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Page<TrackedEntityChangeLog> getTrackedEntityChangeLog(
      @Nonnull UID trackedEntityUid,
      @CheckForNull UID programUid,
      @Nonnull TrackedEntityChangeLogOperationParams operationParams,
      @Nonnull PageParams pageParams)
      throws NotFoundException, ForbiddenException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            trackedEntityUid, programUid, TrackedEntityParams.FALSE.withIncludeAttributes(true));

    Set<UID> trackedEntityAttributes =
        trackedEntity.getTrackedEntityAttributeValues().stream()
            .map(teav -> UID.of(teav.getAttribute().getUid()))
            .collect(Collectors.toSet());

    return hibernateTrackedEntityChangeLogStore.getTrackedEntityChangeLogs(
        trackedEntityUid, programUid, trackedEntityAttributes, operationParams, pageParams);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getOrderableFields() {
    return hibernateTrackedEntityChangeLogStore.getOrderableFields();
  }

  @Override
  public Set<Pair<String, Class<?>>> getFilterableFields() {
    return hibernateTrackedEntityChangeLogStore.getFilterableFields();
  }
}
