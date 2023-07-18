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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
public class DefaultMetadataDatastoreService implements MetadataDatastoreService {
  private final DatastoreStore store;

  public DefaultMetadataDatastoreService(DatastoreStore store, DatastoreService service) {
    checkNotNull(store);

    this.store = store;
    if (service != null) {
      service.addProtection(
          new DatastoreNamespaceProtection(
              MetadataDatastoreService.METADATA_STORE_NS,
              ProtectionType.HIDDEN,
              false,
              MetadataDatastoreService.METADATA_SYNC_AUTHORITY));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public DatastoreEntry getMetaDataVersion(String key) {
    return store.getEntry(MetadataDatastoreService.METADATA_STORE_NS, key);
  }

  @Override
  @Transactional
  public void deleteMetaEntry(DatastoreEntry entry) {
    validateNamespace(entry);
    store.delete(entry);
  }

  @Override
  @Transactional
  public long addMetaEntry(DatastoreEntry entry) {
    validateNamespace(entry);
    store.save(entry);

    return entry.getId();
  }

  @Override
  public List<String> getAllVersions() {
    return store.getKeysInNamespace(MetadataDatastoreService.METADATA_STORE_NS);
  }

  private void validateNamespace(DatastoreEntry entry) {
    if (!MetadataDatastoreService.METADATA_STORE_NS.equals(entry.getNamespace())) {
      throw new IllegalArgumentException(
          "Entry is not in metadata namespace but: " + entry.getNamespace());
    }
  }
}
