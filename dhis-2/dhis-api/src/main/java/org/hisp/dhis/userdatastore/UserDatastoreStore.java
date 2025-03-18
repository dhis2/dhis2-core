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
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.datastore.DatastoreFields;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.user.User;

/**
 * @author Stian Sandvold
 */
public interface UserDatastoreStore extends IdentifiableObjectStore<UserDatastoreEntry> {
  /**
   * Retrieves a KeyJsonValue based on the associated key and user
   *
   * @param user the user where the key is stored
   * @param namespace the namespace referencing the value
   * @param key the key referencing the value
   * @return the KeyJsonValue retrieved
   */
  UserDatastoreEntry getEntry(User user, String namespace, String key);

  /**
   * Retrieves a list of namespaces associated with a user
   *
   * @param user to search namespaces for
   * @return a list of strings representing namespaces
   */
  List<String> getNamespaces(User user);

  /**
   * Retrieves a list of keys associated with a given user and namespace.
   *
   * @param user the user to retrieve keys from
   * @param namespace the namespace to search
   * @return a list of strings representing the different keys stored on the user
   */
  List<String> getKeysInNamespace(User user, String namespace);

  /**
   * Retrieves all UserKeyJsonvalues from a given user and namespace
   *
   * @param user to search
   * @param namespace to search
   */
  List<UserDatastoreEntry> getEntriesInNamespace(User user, String namespace);

  /**
   * Counts the entries in a given namespace.
   *
   * @param namespace the namespace to count
   * @return number of entries in the given namespace.
   */
  int countKeysInNamespace(User user, String namespace);

  <T> T getEntries(User user, DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform);

  /**
   * Updates the entry value (path is undefined or empty) or updates the existing value the the
   * provided path with the provided value.
   *
   * <p>If a roll size is provided and the exiting value (at path) is an array the array is not
   * replaced with the value but the value is appended to the array. The head of the array is
   * dropped if the size of the array is equal or larger than the roll size.
   *
   * @param ns namespace to update
   * @param key key to update
   * @param value the new JSON value, null to remove the entry or clear the property at the provided
   *     path
   * @param path to update, null or empty to update the root (the entire value)
   * @param roll when set the value is appended to arrays instead of replacing them while also
   *     rolling (dropping the array head element when its size exceeds the given roll size)
   * @return true, if the update affects an existing row
   */
  boolean updateEntry(
      @Nonnull String ns,
      @Nonnull String key,
      @CheckForNull String value,
      @CheckForNull String path,
      @CheckForNull Integer roll);
}
