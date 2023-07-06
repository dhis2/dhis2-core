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
package org.hisp.dhis.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;

/**
 * @author Lars Helge Overland
 */
public class MockCurrentUserService implements CurrentUserService {
  private User currentUser;

  private boolean superUserFlag;

  public MockCurrentUserService(User currentUser) {
    this.currentUser = currentUser;
  }

  public MockCurrentUserService(
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      String... auths) {
    this(true, organisationUnits, dataViewOrganisationUnits, auths);
  }

  public MockCurrentUserService(
      boolean superUserFlag,
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      String... auths) {
    UserRole userRole = new UserRole();
    userRole.setName("USER");
    userRole.setAutoFields();
    userRole.getAuthorities().addAll(Arrays.asList(auths));

    this.superUserFlag = superUserFlag;

    User user = new User();
    user.setUsername("currentUser");
    user.getUserRoles().add(userRole);
    user.setFirstName("Current");
    user.setSurname("User");
    user.setOrganisationUnits(organisationUnits);
    user.setDataViewOrganisationUnits(dataViewOrganisationUnits);
    user.setAutoFields();
    user.setCreatedBy(user);

    this.currentUser = user;
  }

  @Override
  public String getCurrentUsername() {
    return currentUser.getUsername();
  }

  @Override
  public User getCurrentUser() {
    return currentUser;
  }

  @Override
  public Set<OrganisationUnit> getCurrentUserOrganisationUnits() {
    return currentUser != null ? currentUser.getOrganisationUnits() : new HashSet<>();
  }

  @Override
  public boolean currentUserIsSuper() {
    return superUserFlag;
  }

  public void setSuperUserFlag(boolean superUserFlag) {
    this.superUserFlag = superUserFlag;
  }

  @Override
  public boolean currentUserIsAuthorized(String auth) {
    return true;
  }

  @Override
  public CurrentUserGroupInfo getCurrentUserGroupsInfo() {
    return new CurrentUserGroupInfo(
        currentUser.getUid(),
        currentUser.getGroups().stream().map(g -> g.getUid()).collect(Collectors.toSet()));
  }

  @Override
  public void invalidateUserGroupCache(String username) {}

  @Override
  public CurrentUserGroupInfo getCurrentUserGroupsInfo(User userInfo) {
    return getCurrentUserGroupsInfo();
  }
}
