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
import java.util.function.Function;
import java.util.stream.Stream;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.datastore.DatastoreFields;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.user.User;

/**
 * @author Stian Sandvold
 */
public interface UserDatastoreService {

  boolean isUsedNamespace(User user, String namespace);

  /**
   * Retrieves a KeyJsonValue based on a user and key
   *
   * @param user the user where the key is associated
   * @param namespace the namespace associated with the key
   * @param key the key referencing the value @return the UserKeyJsonValue matching the key and
   *     namespace
   */
  UserDatastoreEntry getUserEntry(User user, String namespace, String key);

  /**
   * Adds a new UserKeyJsonValue
   *
   * @param entry the UserKeyJsonValue to be stored
   * @return the id of the UserKeyJsonValue stored
   */
  long addEntry(UserDatastoreEntry entry) throws ConflictException, BadRequestException;

  /**
   * Updates a UserKeyJsonValue
   *
   * @param entry the updated UserKeyJsonValue
   */
  void updateEntry(UserDatastoreEntry entry) throws BadRequestException;

  /**
   * Deletes a UserKeyJsonValue
   *
   * @param entry the UserKeyJsonValue to be deleted.
   */
  void deleteEntry(UserDatastoreEntry entry);

  /**
   * Returns a list of namespaces connected to the given user
   *
   * @param user the user connected to the namespaces
   * @return List of strings representing namespaces or an empty list if no namespaces are found
   */
  List<String> getNamespacesByUser(User user);

  /**
   * Returns a list of keys in the given namespace connected to the given user
   *
   * @param user connected to keys
   * @param namespace to fetch keys from
   * @return a list of keys or an empty list if no keys are found
   */
  List<String> getKeysByUserAndNamespace(User user, String namespace);

  /**
   * Deletes all keys associated with a given user and namespace
   *
   * @param user the user associated with namespace to delete
   * @param namespace the namespace to delete
   */
  void deleteNamespace(User user, String namespace);

  /**
   * Validates and plans a {@link DatastoreQuery}. This might correct or otherwise update the
   * provided query.
   *
   * @param query to validate and plan
   * @throws IllegalQueryException when the query is not valid
   */
  DatastoreQuery plan(DatastoreQuery query) throws ConflictException;

  /**
   * Stream the matching entry fields to a transformer or consumer function.
   *
   * <p>Note that this API cannot return the {@link Stream} since it has to be processed within the
   * transaction bounds of the function call. For the same reason a transformer function has to
   * process the stream in a way that actually will evaluate the stream.
   *
   * @param query query parameters
   * @param transform transformer or consumer for the stream of matches
   * @param <T> type of the transformed stream
   * @return the transformed stream
   */
  <T> T getEntries(User user, DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform)
      throws ConflictException;
}
