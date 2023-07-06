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
package org.hisp.dhis.userdatastore;

import java.util.List;
import lombok.AllArgsConstructor;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold
 */
@Service
@AllArgsConstructor
public class DefaultUserDatastoreService implements UserDatastoreService {
  private final UserDatastoreStore userDatastoreStore;

  @Override
  @Transactional(readOnly = true)
  public UserDatastoreEntry getUserEntry(User user, String namespace, String key) {
    return userDatastoreStore.getUserKeyJsonValue(user, namespace, key);
  }

  @Override
  @Transactional
  public long addUserEntry(UserDatastoreEntry entry) {
    userDatastoreStore.save(entry);
    return entry.getId();
  }

  @Override
  @Transactional
  public void updateUserEntry(UserDatastoreEntry entry) {
    userDatastoreStore.update(entry);
  }

  @Override
  @Transactional
  public void deleteUserEntry(UserDatastoreEntry entry) {
    userDatastoreStore.delete(entry);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getNamespacesByUser(User user) {
    return userDatastoreStore.getNamespacesByUser(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getKeysByUserAndNamespace(User user, String namespace) {
    return userDatastoreStore.getKeysByUserAndNamespace(user, namespace);
  }

  @Override
  @Transactional
  public void deleteNamespaceFromUser(User user, String namespace) {
    userDatastoreStore
        .getUserKeyJsonValueByUserAndNamespace(user, namespace)
        .forEach(userDatastoreStore::delete);
  }
}
