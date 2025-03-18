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
package org.hisp.dhis.userdatastore;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.datastore.DatastoreFields;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.DatastoreQueryValidator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold
 */
@Service
@RequiredArgsConstructor
public class DefaultUserDatastoreService implements UserDatastoreService {

  private final UserDatastoreStore store;

  @Override
  @Transactional(readOnly = true)
  public boolean isUsedNamespace(User user, String namespace) {
    return store.countKeysInNamespace(user, namespace) > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDatastoreEntry getUserEntry(User user, String namespace, String key) {
    return store.getEntry(user, namespace, key);
  }

  @Override
  @Transactional
  public void addEntry(UserDatastoreEntry entry) throws ConflictException, BadRequestException {
    if (getUserEntry(entry.getUser(), entry.getNamespace(), entry.getKey()) != null) {
      throw new ConflictException(
          String.format(
              "Key '%s' already exists in namespace '%s'", entry.getKey(), entry.getNamespace()));
    }
    validateEntry(entry);
    store.save(entry);
  }

  @Override
  @Transactional
  public void updateEntry(
      @Nonnull String ns,
      @Nonnull String key,
      @CheckForNull String value,
      @CheckForNull String path,
      @CheckForNull Integer roll)
      throws BadRequestException {
    validateEntry(key, value);
    store.updateEntry(ns, key, value, path, roll);
  }

  @Override
  @Transactional
  public void deleteEntry(UserDatastoreEntry entry) {
    store.delete(entry);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getNamespacesByUser(User user) {
    return store.getNamespaces(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getKeysByUserAndNamespace(User user, String namespace) {
    return store.getKeysInNamespace(user, namespace);
  }

  @Override
  @Transactional
  public void deleteNamespace(User user, String namespace) {
    store.getEntriesInNamespace(user, namespace).forEach(store::delete);
  }

  @Override
  public DatastoreQuery plan(DatastoreQuery query) throws ConflictException {
    DatastoreQueryValidator.validate(query);
    return query;
  }

  @Override
  @Transactional(readOnly = true)
  public <T> T getEntries(
      User user, DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform)
      throws ConflictException {
    DatastoreQueryValidator.validate(query);
    return store.getEntries(user, query, transform);
  }

  private void validateEntry(UserDatastoreEntry entry) throws BadRequestException {
    validateEntry(entry.getKey(), entry.getValue());
  }

  private static void validateEntry(String key, String value) throws BadRequestException {
    if (value == null) return;
    try {
      JsonNode.of(value).visit(JsonNode::value);
    } catch (RuntimeException e) {
      throw new BadRequestException(String.format("Invalid JSON value for key '%s'", key));
    }
  }
}
