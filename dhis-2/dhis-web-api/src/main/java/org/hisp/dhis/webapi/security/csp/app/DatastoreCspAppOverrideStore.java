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
package org.hisp.dhis.webapi.security.csp.app;

import static org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType.RESTRICTED;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.SystemUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prototype storage of CSP overrides in the protected datastore namespace {@value #NAMESPACE}, one
 * key per app. Reads through the datastore API need {@code F_SYSTEM_SETTING} or {@code
 * M_dhis-web-app-management}; writes are locked to {@code ALL} (superusers). This service reads and
 * writes with system privileges, after {@link CspAppOverrideService} has checked authority and
 * validated the document. The authority sets must not be empty: an empty set silently turns the
 * protection into {@code NONE}.
 *
 * @author Morten Svanæs
 */
@Component
@RequiredArgsConstructor
public class DatastoreCspAppOverrideStore implements CspAppOverrideStore {
  public static final String NAMESPACE = "csp-app-overrides";

  private final DatastoreService datastore;

  @PostConstruct
  void registerProtection() {
    datastore.addProtection(
        new DatastoreNamespaceProtection(
            NAMESPACE,
            RESTRICTED,
            Set.of(
                Authorities.F_SYSTEM_SETTING.name(),
                Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString()),
            RESTRICTED,
            Set.of(Authorities.ALL.name())));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> get(String appKey) {
    return Optional.ofNullable(entry(appKey)).map(DatastoreEntry::getValue);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, String> getAll() {
    Map<String, String> result = new LinkedHashMap<>();
    try {
      for (String key : datastore.getKeysInNamespace(NAMESPACE, null)) {
        DatastoreEntry entry = entry(key);
        if (entry != null) {
          result.put(key, entry.getValue());
        }
      }
    } catch (ForbiddenException e) {
      throw new IllegalStateException("No read access to " + NAMESPACE, e);
    }
    return result;
  }

  @Override
  @Transactional
  public void put(String appKey, String json) {
    DatastoreEntry entry = new DatastoreEntry(NAMESPACE, appKey, json, false);
    try {
      datastore.saveOrUpdateEntry(entry, new SystemUser());
    } catch (BadRequestException | ForbiddenException e) {
      throw new IllegalStateException("Failed to store CSP override of " + appKey, e);
    }
  }

  @Override
  @Transactional
  public boolean delete(String appKey) {
    DatastoreEntry entry = entry(appKey);
    if (entry == null) {
      return false;
    }
    datastore.deleteEntry(entry, new SystemUser());
    return true;
  }

  private DatastoreEntry entry(String appKey) {
    try {
      return datastore.getEntry(NAMESPACE, appKey, new SystemUser());
    } catch (ForbiddenException e) {
      throw new IllegalStateException("System user cannot read " + NAMESPACE, e);
    }
  }
}
