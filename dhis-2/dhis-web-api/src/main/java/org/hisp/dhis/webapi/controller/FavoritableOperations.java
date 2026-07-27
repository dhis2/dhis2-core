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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import org.hisp.dhis.common.FavoritableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Endpoints that can be added for types support {@link FavoritableObject} API */
public interface FavoritableOperations<T extends IdentifiableObject & FavoritableObject>
    extends CrudOperationsSupport<T> {

  @PostMapping(value = "/{uid}/favorite")
  @ResponseBody
  default WebMessage postSetAsFavorite(
      @PathVariable("uid") UID uid, @CurrentUser UserDetails currentUser)
      throws ConflictException, NotFoundException {
    if (setAsFavorite(uid, currentUser))
      return ok(
          String.format(
              "Object '%s' set as favorite for user '%s'", uid, currentUser.getUsername()));
    throw new ConflictException("Objects of this class cannot be set as favorite");
  }

  @DeleteMapping(value = "/{uid}/favorite")
  @ResponseBody
  default WebMessage deleteRemoveAsFavorite(
      @PathVariable("uid") UID uid, @CurrentUser UserDetails currentUser)
      throws NotFoundException, ConflictException {
    if (removeAsFavorite(uid, currentUser))
      return ok(
          String.format(
              "Object '%s' removed as favorite for user '%s'", uid, currentUser.getUsername()));
    throw new ConflictException("Objects of this class cannot be set as favorite");
  }
}
