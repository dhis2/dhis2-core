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
package org.hisp.dhis.user;

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.Map;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.JdbcDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@AllArgsConstructor
@Component
public class UserDeletionHandler extends JdbcDeletionHandler {
  private final IdentifiableObjectManager idObjectManager;

  private final UserStore userStore;

  private static final DeletionVeto VETO = new DeletionVeto(User.class);

  @Override
  protected void register() {
    whenDeleting(UserRole.class, this::deleteUserRole);
    whenDeleting(OrganisationUnit.class, this::deleteOrganisationUnit);
    whenDeleting(UserGroup.class, this::deleteUserGroup);
    whenVetoing(UserRole.class, this::allowDeleteUserRole);
    whenVetoing(FileResource.class, this::allowDeleteFileResource);
  }

  private void deleteUserRole(UserRole role) {
    for (User user : role.getMembers()) {
      user.getUserRoles().remove(role);
      idObjectManager.updateNoAcl(user);
    }
  }

  private void deleteOrganisationUnit(OrganisationUnit unit) {
    for (User user : unit.getUsers()) {
      user.getOrganisationUnits().remove(unit);
      idObjectManager.updateNoAcl(user);
    }
    for (User user : userStore.getUsers(new UserQueryParams().addDataViewOrganisationUnit(unit))) {
      user.getDataViewOrganisationUnits().remove(unit);
      idObjectManager.updateNoAcl(user);
    }
    for (User user : userStore.getUsers(new UserQueryParams().addTeiSearchOrganisationUnit(unit))) {
      user.getTeiSearchOrganisationUnits().remove(unit);
      idObjectManager.updateNoAcl(user);
    }
  }

  private void deleteUserGroup(UserGroup group) {
    for (User user : group.getMembers()) {
      user.getGroups().remove(group);
      idObjectManager.updateNoAcl(user);
    }
  }

  private DeletionVeto allowDeleteUserRole(UserRole userRole) {
    for (User credentials : userRole.getMembers()) {
      for (UserRole role : credentials.getUserRoles()) {
        if (role.equals(userRole)) {
          return new DeletionVeto(User.class, credentials.getName());
        }
      }
    }
    return ACCEPT;
  }

  private DeletionVeto allowDeleteFileResource(FileResource fileResource) {
    String sql = "select 1 from userinfo where avatar=:id limit 1";
    return vetoIfExists(VETO, sql, Map.of("id", fileResource.getId()));
  }
}
