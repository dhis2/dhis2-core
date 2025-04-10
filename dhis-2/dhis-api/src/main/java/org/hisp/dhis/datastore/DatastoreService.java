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
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.user.UserDetails;
import org.springframework.security.access.AccessDeniedException;

/**
 * Datastore is a key-value store with namespaces to isolate different collections of key-value
 * pairs.
 *
 * <p>Each namespace can have a specific {@link DatastoreNamespaceProtection} policy guarding
 * read/write access to it.
 *
 * @author Stian Sandvold
 * @author Jan Bernitt (advanced namespace protection)
 */
public interface DatastoreService {

  /**
   * @param namespace to check
   * @return the protection used or {@code null} when unprotected
   */
  @CheckForNull
  DatastoreNamespaceProtection getProtection(@Nonnull String namespace);

  /**
   * @return a list of the active namespace protections sorted by namespace name
   */
  @Nonnull
  List<DatastoreNamespaceProtection> getProtections();

  /**
   * Applies the configuration for the provided protection so it is considered by this service in
   * future requests.
   *
   * @param protection configuration for protection
   */
  void addProtection(DatastoreNamespaceProtection protection);

  /**
   * Removes any {@link DatastoreNamespaceProtection} configuration for the given namespace (if
   * exists).
   *
   * @param namespace the namespace for which to remove configuration
   */
  void removeProtection(String namespace);

  /**
   * True, if there is at least a single value for the provided namespace.
   *
   * @param namespace the namespace to check
   * @return true, if the namespace exists, else false
   */
  boolean isUsedNamespace(String namespace) throws ForbiddenException;

  /**
   * Retrieves a list of existing namespaces.
   *
   * <p>This does not include {@link ProtectionType#HIDDEN} namespaces that the current user cannot
   * see.
   *
   * @return a list of strings representing the existing namespaces.
   */
  List<String> getNamespaces();

  /**
   * Retrieves a list of keys from a namespace which are updated after lastUpdated time.
   *
   * @param namespace the namespace to retrieve keys from.
   * @param lastUpdated the lastUpdated time to retrieve keys from.
   * @return a list of strings representing the keys from the namespace.
   * @throws AccessDeniedException when user lacks authority for namespace
   */
  List<String> getKeysInNamespace(String namespace, Date lastUpdated) throws ForbiddenException;

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
  <T> T getEntries(DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform)
      throws ConflictException, ForbiddenException;

  /**
   * Validates and plans a {@link DatastoreQuery}. This might correct or otherwise update the
   * provided query.
   *
   * @param query to validate and plan
   * @throws IllegalQueryException when the query is not valid
   */
  DatastoreQuery plan(DatastoreQuery query) throws ConflictException;

  /**
   * Retrieves a KeyJsonValue based on a namespace and key.
   *
   * @param namespace the namespace where the key is associated.
   * @param key the key referencing the value.
   * @return the KeyJsonValue matching the key and namespace.
   * @throws AccessDeniedException when user lacks authority for namespace
   */
  @CheckForNull
  DatastoreEntry getEntry(String namespace, String key) throws ForbiddenException;

  @CheckForNull
  DatastoreEntry getEntry(String namespace, String key, UserDetails user) throws ForbiddenException;

  /**
   * Adds a new entry.
   *
   * @param entry the KeyJsonValue to be stored.
   * @throws IllegalStateException when an entry with same namespace and key already exists
   * @throws IllegalArgumentException when the entry value is not valid JSON
   * @throws AccessDeniedException when user lacks authority for namespace or entry
   */
  void addEntry(DatastoreEntry entry)
      throws ConflictException, BadRequestException, ForbiddenException;

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
   */
  void updateEntry(
      @Nonnull String ns,
      @Nonnull String key,
      @CheckForNull String value,
      @CheckForNull String path,
      @CheckForNull Integer roll)
      throws BadRequestException;

  /**
   * Deletes an entry.
   *
   * @param entry the KeyJsonValue to be deleted.
   * @throws AccessDeniedException when user lacks authority for namespace or entry
   */
  void deleteEntry(DatastoreEntry entry);

  void deleteEntry(DatastoreEntry entry, UserDetails user);

  /**
   * Adds a new KeyJsonValue entry or updates the entry if the namespace and key already exists.
   *
   * @param entry to be saved or updated
   * @throws IllegalArgumentException when the entry value is not valid JSON
   */
  void saveOrUpdateEntry(DatastoreEntry entry) throws BadRequestException, ForbiddenException;

  void saveOrUpdateEntry(DatastoreEntry entry, UserDetails user)
      throws BadRequestException, ForbiddenException;

  /**
   * Deletes all entries associated with a given namespace.
   *
   * @param namespace the namespace to delete
   * @throws AccessDeniedException when user lacks authority for namespace or any of the entries
   */
  void deleteNamespace(String namespace);
}
