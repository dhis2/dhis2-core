/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.FileResourceStream;

public interface TrackedEntityService {

  /** Get a file for a tracked entities' attribute. */
  FileResourceStream getFileResource(UID trackedEntity, UID attribute, UID program)
      throws NotFoundException, ForbiddenException;

  /** Get an image for a tracked entities' attribute in the given dimension. */
  FileResourceStream getFileResourceImage(
      UID trackedEntity, UID attribute, UID program, ImageFileDimension dimension)
      throws NotFoundException, ForbiddenException;

  /**
   * Finds the tracked entity that matches the given {@code UID} based on the privileges of the
   * currently authenticated user. Returns an {@link Optional} indicating whether the tracked entity
   * was found.
   *
   * @return an {@link Optional} containing the tracked entity if found, or an empty {@link
   *     Optional} if not
   */
  @Nonnull
  Optional<TrackedEntity> findTrackedEntity(@Nonnull UID uid);

  /**
   * Retrieves the tracked entity that matches the given {@code UID} based on the privileges of the
   * currently authenticated user. This method only includes TETAs (Tracked Entity Type Attributes)
   * and excludes program attributes, enrollments, and relationships. To include enrollments,
   * relationships, and program attributes, use {@link #getTrackedEntity(UID, UID,
   * TrackedEntityParams)}.
   *
   * @return the tracked entity associated with the specified {@code UID}
   * @throws NotFoundException if the tracked entity cannot be found
   * @throws ForbiddenException if the user does not have permission to access the tracked entity
   */
  @Nonnull
  TrackedEntity getTrackedEntity(@Nonnull UID uid) throws NotFoundException, ForbiddenException;

  /**
   * Retrieves the tracked entity that matches the given {@code UID} based on the privileges of the
   * currently authenticated user. If the {@code program} is provided, the program attributes for
   * the specified program are included; otherwise, only TETAs (Tracked Entity Type Attributes) are
   * included. This method also includes any enrollments, relationships, attributes, and ownerships
   * as defined by the provided {@code params}.
   *
   * @return the tracked entity with additional details based on the provided parameters
   * @throws NotFoundException if the tracked entity cannot be found
   * @throws ForbiddenException if the user does not have permission to access the tracked entity
   */
  @Nonnull
  TrackedEntity getTrackedEntity(@Nonnull UID uid, UID program, @Nonnull TrackedEntityParams params)
      throws NotFoundException, ForbiddenException;

  /** Find all tracked entities matching given params. */
  @Nonnull
  List<TrackedEntity> findTrackedEntities(TrackedEntityOperationParams operationParams)
      throws BadRequestException, ForbiddenException, NotFoundException;

  /** Get a page of tracked entities matching given params. */
  @Nonnull
  Page<TrackedEntity> findTrackedEntities(
      TrackedEntityOperationParams params, PageParams pageParams)
      throws BadRequestException, ForbiddenException, NotFoundException;

  /**
   * Fields the {@link #findTrackedEntities(TrackedEntityOperationParams)} can order tracked
   * entities by. Ordering by fields other than these is considered a programmer error. Validation
   * of user provided field names should occur before calling {@link
   * #findTrackedEntities(TrackedEntityOperationParams)}.
   */
  Set<String> getOrderableFields();
}
