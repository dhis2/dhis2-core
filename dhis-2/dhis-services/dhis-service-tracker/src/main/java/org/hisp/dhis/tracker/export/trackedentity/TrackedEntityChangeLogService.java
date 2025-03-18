/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;

public interface TrackedEntityChangeLogService {

  void addTrackedEntityChangeLog(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull TrackedEntityAttribute trackedEntityAttribute,
      @CheckForNull String previousValue,
      @CheckForNull String currentValue,
      @Nonnull ChangeLogType changeLogType,
      @Nonnull String username);

  void deleteTrackedEntityChangeLogs(TrackedEntity trackedEntity);

  /**
   * Retrieves the change log data for a particular tracked entity.
   *
   * @return the paged change logs of the supplied tracked entity, if any
   */
  @Nonnull
  Page<TrackedEntityChangeLog> getTrackedEntityChangeLog(
      @Nonnull UID trackedEntityUid,
      @CheckForNull UID programUid,
      @Nonnull TrackedEntityChangeLogOperationParams operationParams,
      @Nonnull PageParams pageParams)
      throws NotFoundException, ForbiddenException, BadRequestException;

  /**
   * Fields the {@link #getTrackedEntityChangeLog(UID, UID, TrackedEntityChangeLogOperationParams,
   * PageParams)} can order tracked entities change logs by. Ordering by fields other than these are
   * considered a programmer error. Validation of user provided field names should occur before
   * calling {@link #getTrackedEntityChangeLog(UID, UID, TrackedEntityChangeLogOperationParams,
   * PageParams)}.
   */
  Set<String> getOrderableFields();

  /**
   * Fields the {@link #getTrackedEntityChangeLog(UID, UID, TrackedEntityChangeLogOperationParams,
   * PageParams)} can filter attribute change logs by. Filtering by fields other than these, is
   * considered a programmer error. Validation of user provided field names should occur before
   * calling {@link #getTrackedEntityChangeLog(UID, UID, TrackedEntityChangeLogOperationParams,
   * PageParams)}.
   */
  Set<Pair<String, Class<?>>> getFilterableFields();
}
