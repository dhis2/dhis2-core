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
package org.hisp.dhis.webapi.controller.user;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/userRoles")
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class UserRoleController
    extends AbstractCrudController<UserRole, UserRoleController.GetUserRoleObjectListParams> {

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class GetUserRoleObjectListParams extends GetObjectListParams {
    @OpenApi.Description(
        """
      Limits results to those roles that the current user can issue/grant to other users.
      Can be combined with further `filter`s.
      """)
    boolean canIssue;
  }

  @Override
  protected List<UID> getPreQueryMatches(GetUserRoleObjectListParams params) {
    if (!params.isCanIssue()) return null;
    return userService.getRolesCurrentUserCanIssue();
  }

  @RequestMapping(
      value = "/{id}/users/{userId}",
      method = {RequestMethod.POST, RequestMethod.PUT})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addUserToRole(
      @PathVariable(value = "id") String pvId,
      @PathVariable("userId") String pvUserId,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws NotFoundException, ForbiddenException {
    UserRole userRole = userService.getUserRole(pvId);

    if (userRole == null) {
      throw new NotFoundException(getEntityClass(), pvId);
    }

    User user = userService.getUser(pvUserId);

    if (user == null) {
      throw new NotFoundException("User does not exist: " + pvUserId);
    }

    if (!aclService.canUpdate(currentUser, userRole)) {
      throw new ForbiddenException("You don't have the proper permissions to update this object.");
    }

    if (!user.getUserRoles().contains(userRole)) {
      user.getUserRoles().add(userRole);
      userService.updateUser(user);
    }
  }

  @DeleteMapping("/{id}/users/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeUserFromRole(
      @PathVariable(value = "id") String pvId,
      @PathVariable("userId") String pvUserId,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws NotFoundException, ForbiddenException {
    UserRole userRole = userService.getUserRole(pvId);

    if (userRole == null) {
      throw new NotFoundException(getEntityClass(), pvId);
    }

    User user = userService.getUser(pvUserId);

    if (user == null) {
      throw new NotFoundException("User does not exist: " + pvUserId);
    }

    if (!aclService.canUpdate(currentUser, userRole)) {
      throw new ForbiddenException("You don't have the proper permissions to delete this object.");
    }

    if (user.getUserRoles().contains(userRole)) {
      user.getUserRoles().remove(userRole);
      userService.updateUser(user);
    }
  }
}
