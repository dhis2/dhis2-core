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
package org.hisp.dhis.datastore;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObjectStore;

/**
 * @author Stian Sandvold
 */
public interface DatastoreStore extends IdentifiableObjectStore<DatastoreEntry> {
  /**
   * Retrieves a list of all namespaces
   *
   * @return a list of strings representing each existing namespace
   */
  List<String> getNamespaces();

  /**
   * Retrieves a list of keys associated with a given namespace.
   *
   * @param namespace the namespace to retrieve keys from
   * @return a list of strings representing the different keys in the namespace
   */
  List<String> getKeysInNamespace(String namespace);

  /**
   * Retrieves a list of keys associated with a given namespace which are updated after lastUpdated
   * time. A sharing check for Metadata Read access is performed on the User to see what entries
   * they have permission to read.
   *
   * @param namespace the namespace to retrieve keys from
   * @param lastUpdated the lastUpdated time to retrieve keys from
   * @return a list of strings representing the different keys in the namespace
   */
  List<String> getKeysInNamespace(String namespace, Date lastUpdated);

  /**
   * Retrieves a list of KeyJsonValue objects based on a given namespace
   *
   * @param namespace the namespace to retrieve KeyJsonValues from
   * @return a List of KeyJsonValues
   */
  List<DatastoreEntry> getEntriesInNamespace(String namespace);

  /**
   * Stream the matching entries to a transformer or consumer function. A sharing check for Metadata
   * Read access is performed on the User to see what entries they have permission to read.
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
  <T> T getEntries(DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform);

  /**
   * Retrieves a KeyJsonValue based on the associated key and namespace
   *
   * @param namespace the namespace where the key is stored
   * @param key the key referencing the value
   * @return the KeyJsonValue retrieved
   */
  DatastoreEntry getEntry(String namespace, String key);

  /**
   * Deletes all values in the provided namespace.
   *
   * @param namespace the namespace for which to remove all values
   */
  void deleteNamespace(String namespace);

  /**
   * Counts the entries in a given namespace.
   *
   * @param namespace the namespace to count
   * @return number of entries in the given namespace.
   */
  int countKeysInNamespace(String namespace);

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
