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
package org.hisp.dhis.sharing;

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.util.SharingUtils;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultSharingService implements SharingService {
  @Nonnull private final AclService aclService;

  @Nonnull private final IdentifiableObjectManager manager;

  @Nonnull private final UserGroupService userGroupService;

  @Nonnull private final UserService userService;

  @Nonnull private final SchemaService schemaService;

  @Override
  public <T extends IdentifiableObject> ObjectReport saveSharing(
      @Nonnull Class<T> entityClass, @Nonnull T entity, @Nonnull Sharing sharing) {
    ObjectReport objectReport = new ObjectReport(Sharing.class, 0);

    if (entity instanceof SystemDefaultMetadataObject sdmo && sdmo.isDefault()) {
      objectReport.addErrorReport(
          new ErrorReport(Sharing.class, ErrorCode.E3013, entityClass.getSimpleName())
              .setErrorKlass(entityClass));
    }

    if (!aclService.canManage(CurrentUserUtil.getCurrentUserDetails(), entity)) {
      objectReport.addErrorReport(
          new ErrorReport(Sharing.class, ErrorCode.E3014).setErrorKlass(entityClass));
    }

    if (!AccessStringHelper.isValid(sharing.getPublicAccess())) {
      objectReport.addErrorReport(
          new ErrorReport(Sharing.class, ErrorCode.E3015, sharing.getPublicAccess())
              .setErrorKlass(entityClass));
    }

    // ---------------------------------------------------------------------
    // Ignore externalAccess if user is not allowed to make objects external
    // ---------------------------------------------------------------------

    if (aclService.canMakeClassExternal(CurrentUserUtil.getCurrentUserDetails(), entityClass)) {
      entity.getSharing().setExternal(sharing.isExternal());
    }

    // ---------------------------------------------------------------------
    // Ignore publicAccess if user is not allowed to make objects public
    // ---------------------------------------------------------------------

    Schema schema = schemaService.getDynamicSchema(entityClass);

    if (aclService.canMakePublic(CurrentUserUtil.getCurrentUserDetails(), entity)) {
      entity.getSharing().setPublicAccess(sharing.getPublicAccess());
    }

    if (!schema.isDataShareable()) {
      if (AccessStringHelper.hasDataSharing(entity.getSharing().getPublicAccess())) {
        objectReport.addErrorReport(
            new ErrorReport(Sharing.class, ErrorCode.E3016).setErrorKlass(entityClass));
      }
    }

    entity.getSharing().setOwner(sharing.getOwner());

    // --------------------------------------
    // Handle UserGroupAccesses
    // --------------------------------------

    entity.getSharing().getUserGroups().clear();

    if (sharing.hasUserGroupAccesses()) {
      for (UserGroupAccess sharingUserGroupAccess : sharing.getUserGroups().values()) {
        if (!AccessStringHelper.isValid(sharingUserGroupAccess.getAccess())) {
          objectReport.addErrorReport(
              new ErrorReport(Sharing.class, ErrorCode.E3017, sharingUserGroupAccess.getAccess())
                  .setErrorKlass(entityClass));
        }

        if (!schema.isDataShareable()) {
          if (AccessStringHelper.hasDataSharing(sharingUserGroupAccess.getAccess())) {
            objectReport.addErrorReport(
                new ErrorReport(Sharing.class, ErrorCode.E3016).setErrorKlass(entityClass));
          }
        }

        UserGroup userGroup = userGroupService.getUserGroup(sharingUserGroupAccess.getId());

        if (userGroup != null) {
          entity.getSharing().addUserGroupAccess(sharingUserGroupAccess);
        }
      }
    }

    // --------------------------------------
    // Handle UserAccesses
    // --------------------------------------

    entity.getSharing().getUsers().clear();

    if (sharing.hasUserAccesses()) {
      for (UserAccess sharingUserAccess : sharing.getUsers().values()) {
        if (!AccessStringHelper.isValid(sharingUserAccess.getAccess())) {
          objectReport.addErrorReport(
              new ErrorReport(Sharing.class, ErrorCode.E3018, sharingUserAccess.getAccess())
                  .setErrorKlass(entityClass));
        }

        if (!schema.isDataShareable()) {
          if (AccessStringHelper.hasDataSharing(sharingUserAccess.getAccess())) {
            objectReport.addErrorReport(
                new ErrorReport(Sharing.class, ErrorCode.E3016).setErrorKlass(entityClass));
          }
        }

        User sharingUser = userService.getUser(sharingUserAccess.getId());

        if (sharingUser != null) {
          entity.getSharing().addUserAccess(sharingUserAccess);
        }
      }
    }

    manager.updateNoAcl(entity);

    if (entity instanceof Program program) {
      syncSharingForEventProgram(program);
    }

    log.info(SharingUtils.sharingToString(entity, CurrentUserUtil.getCurrentUsername()));

    return objectReport;
  }

  private void syncSharingForEventProgram(Program program) {
    if (ProgramType.WITH_REGISTRATION == program.getProgramType()
        || program.getProgramStages().isEmpty()) {
      return;
    }

    ProgramStage programStage = program.getProgramStages().iterator().next();
    AccessStringHelper.copySharing(program, programStage);

    programStage.setCreatedBy(program.getCreatedBy());
    manager.update(programStage);
  }
}
