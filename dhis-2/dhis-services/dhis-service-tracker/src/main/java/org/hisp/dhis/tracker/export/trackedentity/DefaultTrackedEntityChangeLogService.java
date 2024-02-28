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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultTrackedEntityChangeLogService implements TrackedEntityChangeLogService {

  private final TrackedEntityService trackedEntityService;

  private final ProgramService programService;

  private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackerAccessManager trackerAccessManager;

  private final UserService userService;

  private final JdbcTrackedEntityChangeLogStore jdbcTrackedEntityChangeLogStore;

  @Override
  public List<TrackedEntityChangeLog> getTrackedEntityChangeLog(
      UID trackedEntityUid, UID programUid) throws NotFoundException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(trackedEntityUid.getValue());
    if (trackedEntity == null) {
      throw new NotFoundException(TrackedEntity.class, trackedEntityUid.getValue());
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    Set<String> trackedEntityAttributes = Collections.emptySet();
    if (programUid != null) {
      Program program = validateProgram(programUid.getValue());
      validateOwnership(currentUser, trackedEntity, program);
    } else {
      validateTrackedEntity(trackedEntityUid, currentUser, trackedEntity);
      trackedEntityAttributes = validateTrackedEntityAttributes(trackedEntity);
    }

    return jdbcTrackedEntityChangeLogStore.getTrackedEntityChangeLog(
        trackedEntityUid, trackedEntityAttributes);
  }

  private Program validateProgram(String programUid) throws NotFoundException {
    Program program = programService.getProgram(programUid);
    if (program == null) {
      throw new NotFoundException(Program.class, programUid);
    }

    return program;
  }

  private void validateOwnership(User currentUser, TrackedEntity trackedEntity, Program program)
      throws NotFoundException {
    if (!trackerAccessManager
        .canRead(currentUser, trackedEntity, program, currentUser.isSuper())
        .isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }
  }

  private void validateTrackedEntity(
      UID trackedEntityUid, User currentUser, TrackedEntity trackedEntity)
      throws NotFoundException {
    if (!trackerAccessManager.canRead(currentUser, trackedEntity).isEmpty()) {
      // TODO Do we want to show this error if the user has no access to the org unit? Won't it make
      // it harder for the end user to understand why the tracked entity is not accessible? What's
      // the advantage of doing this? Are we giving away any detail by saying the user has no access
      // to an org unit?
      throw new NotFoundException(TrackedEntity.class, trackedEntityUid.getValue());
    }
  }

  private Set<String> validateTrackedEntityAttributes(TrackedEntity trackedEntity)
      throws NotFoundException {
    Set<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getTrackedEntityTypeAttributes(
            trackedEntity.getTrackedEntityType());

    if (attributes.isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }

    return attributes.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet());
  }
}
