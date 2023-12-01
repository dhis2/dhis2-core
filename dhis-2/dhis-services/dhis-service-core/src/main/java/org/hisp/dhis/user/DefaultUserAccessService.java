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
package org.hisp.dhis.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.user.sharing.UserAccess;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.user.UserAccessService")
public class DefaultUserAccessService implements UserAccessService {
  @Qualifier("org.hisp.dhis.user.UserAccessStore")
  private final GenericStore<UserAccess> userAccessStore;

  // -------------------------------------------------------------------------
  // UserGroupAccess
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void addUserAccess(UserAccess userAccess) {
    userAccessStore.save(userAccess);
  }

  @Override
  @Transactional
  public void updateUserAccess(UserAccess userAccess) {
    userAccessStore.update(userAccess);
  }

  @Override
  @Transactional
  public void deleteUserAccess(UserAccess userAccess) {
    userAccessStore.delete(userAccess);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserAccess> getAllUserAccesses() {
    return userAccessStore.getAll();
  }
}
