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

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService")
@RequiredArgsConstructor
public class DefaultTrackedEntityChangeLogService implements TrackedEntityChangeLogService {

  private final TrackedEntityService trackedEntityService;

  private final ProgramService programService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackerAccessManager trackerAccessManager;

  private final HibernateTrackedEntityChangeLogStore hibernateTrackedEntityChangeLogStore;

  @Transactional
  @Override
  public void addTrackedEntityChangeLog(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull TrackedEntityAttribute trackedEntityAttribute,
      @Nullable String previousValue,
      @Nullable String currentValue,
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

  @Override
  @Transactional(readOnly = true)
  public Page<TrackedEntityChangeLog> getTrackedEntityChangeLog(
      @Nonnull UID trackedEntityUid,
      @Nullable UID programUid,
      @Nonnull TrackedEntityChangeLogOperationParams operationParams,
      @Nonnull PageParams pageParams)
      throws NotFoundException, ForbiddenException, BadRequestException {
    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(trackedEntityUid);
    if (trackedEntity == null) {
      throw new NotFoundException(TrackedEntity.class, trackedEntityUid.getValue());
    }

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Set<UID> trackedEntityAttributes = Collections.emptySet();
    if (programUid != null) {
      Program program = validateProgram(programUid.getValue());
      validateOwnership(currentUser, trackedEntity, program);
    } else {
      validateTrackedEntity(currentUser, trackedEntity);
      trackedEntityAttributes = validateTrackedEntityAttributes(trackedEntity);
    }

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

  private Program validateProgram(String programUid) throws NotFoundException {
    Program program = programService.getProgram(programUid);
    if (program == null) {
      throw new NotFoundException(Program.class, programUid);
    }

    return program;
  }

  private void validateOwnership(
      UserDetails currentUser, TrackedEntity trackedEntity, Program program)
      throws NotFoundException {
    if (!trackerAccessManager
        .canRead(currentUser, trackedEntity, program, currentUser.isSuper())
        .isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }
  }

  private void validateTrackedEntity(UserDetails currentUser, TrackedEntity trackedEntity)
      throws NotFoundException {
    if (!trackerAccessManager.canRead(currentUser, trackedEntity).isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }
  }

  private Set<UID> validateTrackedEntityAttributes(TrackedEntity trackedEntity)
      throws NotFoundException {
    Set<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getTrackedEntityTypeAttributes(
            trackedEntity.getTrackedEntityType());

    if (attributes.isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }

    return attributes.stream().map(UID::of).collect(Collectors.toSet());
  }
}
