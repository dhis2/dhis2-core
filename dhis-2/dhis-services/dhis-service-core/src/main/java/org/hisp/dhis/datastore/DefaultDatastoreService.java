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
package org.hisp.dhis.datastore;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold (initial)
 * @author Jan Bernitt (namespace protection)
 */
@RequiredArgsConstructor
@Service
public class DefaultDatastoreService implements DatastoreService {
  private final Map<String, DatastoreNamespaceProtection> protectionByNamespace =
      new ConcurrentHashMap<>();

  private final DatastoreStore store;

  private final CurrentUserService currentUserService;

  private final AclService aclService;

  @Override
  public void addProtection(DatastoreNamespaceProtection protection) {
    protectionByNamespace.put(protection.getNamespace(), protection);
  }

  @Override
  public void removeProtection(String namespace) {
    protectionByNamespace.remove(namespace);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getNamespaces() {
    return store.getNamespaces().stream().filter(this::isNamespaceVisible).collect(toList());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isUsedNamespace(String namespace) {
    return readProtectedIn(namespace, false, () -> store.countKeysInNamespace(namespace) > 0);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getKeysInNamespace(String namespace, Date lastUpdated) {
    return readProtectedIn(
        namespace, emptyList(), () -> store.getKeysInNamespace(namespace, lastUpdated));
  }

  @Override
  @Transactional(readOnly = true)
  public <T> T getEntries(DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform)
      throws ConflictException {
    DatastoreQueryValidator.validate(query);
    return readProtectedIn(query.getNamespace(), null, () -> store.getEntries(query, transform));
  }

  @Override
  public DatastoreQuery plan(DatastoreQuery query) throws ConflictException {
    DatastoreQueryValidator.validate(query);
    return query;
  }

  @Override
  @Transactional(readOnly = true)
  public DatastoreEntry getEntry(String namespace, String key) {
    return readProtectedIn(namespace, null, () -> store.getEntry(namespace, key));
  }

  @Override
  @Transactional
  public void addEntry(DatastoreEntry entry) throws ConflictException, BadRequestException {
    if (getEntry(entry.getNamespace(), entry.getKey()) != null) {
      throw new ConflictException(
          String.format(
              "Key '%s' already exists in namespace '%s'", entry.getKey(), entry.getNamespace()));
    }
    validateEntry(entry);
    writeProtectedIn(entry.getNamespace(), () -> singletonList(entry), () -> store.save(entry));
  }

  @Override
  @Transactional
  public void updateEntry(DatastoreEntry entry) throws BadRequestException {
    validateEntry(entry);
    Runnable update = () -> store.updateNoAcl(entry);
    writeProtectedIn(entry.getNamespace(), () -> singletonList(entry), update);
  }

  @Override
  @Transactional
  public void saveOrUpdateEntry(DatastoreEntry entry) throws BadRequestException {
    validateEntry(entry);
    DatastoreEntry existing = getEntry(entry.getNamespace(), entry.getKey());
    if (existing != null) {
      existing.setValue(entry.getValue());
      writeProtectedIn(
          entry.getNamespace(), () -> singletonList(existing), () -> store.update(existing));
    } else {
      writeProtectedIn(entry.getNamespace(), () -> singletonList(entry), () -> store.save(entry));
    }
  }

  @Override
  @Transactional
  public void deleteNamespace(String namespace) {
    writeProtectedIn(
        namespace,
        () -> store.getEntriesInNamespace(namespace),
        () -> store.deleteNamespace(namespace));
  }

  @Override
  @Transactional
  public void deleteEntry(DatastoreEntry entry) {
    writeProtectedIn(entry.getNamespace(), () -> singletonList(entry), () -> store.delete(entry));
  }

  /**
   * There are 2 levels of access to be aware of in a Datastore: <br>
   *
   * <ol>
   *   <li>{@link DatastoreNamespaceProtection}
   *       <ul>
   *         <li>this is currently only set programmatically
   *         <li>new namespaces setup through the API will have no {@link
   *             DatastoreNamespaceProtection}
   *       </ul>
   *   <li>standard {@link Sharing}
   * </ol>
   *
   * @param namespace namespace
   * @param whenHidden value to return when namespace is hidden & no access
   * @param read data supplier
   * @return data supplier value or whenHidden value
   * @throws AccessDeniedException if {@link User} has no {@link Sharing} access to {@link
   *     DatastoreEntry} or {@link User} has no {@link Sharing} access for restricted namespace
   *     {@link DatastoreEntry}
   */
  private <T> T readProtectedIn(String namespace, T whenHidden, Supplier<T> read) {
    DatastoreNamespaceProtection protection = protectionByNamespace.get(namespace);
    if (userHasNamespaceReadAccess(protection)) {
      T res = read.get();
      if (res instanceof DatastoreEntry de
          && (!aclService.canRead(currentUserService.getCurrentUser(), de))) {
        throw new AccessDeniedException(
            String.format("Access denied for key '%s' in namespace '%s'", de.getKey(), namespace));
      }
      return res;
    } else if (protection.getReads() == ProtectionType.RESTRICTED) {
      throw accessDeniedTo(namespace);
    }
    return whenHidden;
  }

  private boolean userHasNamespaceReadAccess(DatastoreNamespaceProtection protection) {
    return protection == null
        || protection.getReads() == ProtectionType.NONE
        || currentUserHasAuthority(protection.getAuthorities());
  }

  private void writeProtectedIn(
      String namespace, Supplier<List<DatastoreEntry>> whenSharing, Runnable write) {
    DatastoreNamespaceProtection protection = protectionByNamespace.get(namespace);
    if (protection == null || protection.getWrites() == ProtectionType.NONE) {
      write.run();
    } else if (currentUserHasAuthority(protection.getAuthorities())) {
      for (DatastoreEntry entry : whenSharing.get()) {
        if (!aclService.canWrite(currentUserService.getCurrentUser(), entry)) {
          throw accessDeniedTo(namespace, entry.getKey());
        }
      }
      write.run();
    } else if (protection.getWrites() == ProtectionType.RESTRICTED) {
      throw accessDeniedTo(namespace);
    }
    // HIDDEN: the operation silently just isn't run
  }

  private AccessDeniedException accessDeniedTo(String namespace) {
    return new AccessDeniedException(
        String.format("Namespace '%s' is protected, access denied", namespace));
  }

  private AccessDeniedException accessDeniedTo(String namespace, String key) {
    return new AccessDeniedException(
        String.format("Access denied for key '%s' in namespace '%s'", key, namespace));
  }

  private boolean isNamespaceVisible(String namespace) {
    DatastoreNamespaceProtection protection = protectionByNamespace.get(namespace);
    return protection == null
        || protection.getReads() != ProtectionType.HIDDEN
        || currentUserHasAuthority(protection.getAuthorities());
  }

  private boolean currentUserHasAuthority(Set<String> authorities) {
    User currentUser = currentUserService.getCurrentUser();
    if (currentUser == null) {
      return false;
    }
    return currentUser.isSuper()
        || !authorities.isEmpty() && currentUser.hasAnyAuthority(authorities);
  }

  private void validateEntry(DatastoreEntry entry) throws BadRequestException {
    try {
      JsonNode.of(entry.getValue()).visit(JsonNode::value);
    } catch (RuntimeException e) {
      throw new BadRequestException(
          String.format("Invalid JSON value for key '%s'", entry.getKey()));
    }
  }
}
