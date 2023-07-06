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
package org.hisp.dhis.version;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mortenoh
 */
@Service("org.hisp.dhis.version.VersionService")
public class DefaultVersionService implements VersionService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final VersionStore versionStore;

  public DefaultVersionService(VersionStore versionStore) {
    checkNotNull(versionStore);

    this.versionStore = versionStore;
  }

  // -------------------------------------------------------------------------
  // VersionService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addVersion(Version version) {
    versionStore.save(version);
    return version.getId();
  }

  @Override
  @Transactional
  public void updateVersion(Version version) {
    versionStore.update(version);
  }

  @Override
  @Transactional
  public void updateVersion(String key) {
    updateVersion(key, UUID.randomUUID().toString());
  }

  @Override
  @Transactional
  public void updateVersion(String key, String value) {
    Version version = getVersionByKey(key);

    if (version == null) {
      version = new Version(key, value);
      addVersion(version);
    } else {
      version.setValue(value);
      updateVersion(version);
    }
  }

  @Override
  @Transactional
  public void deleteVersion(Version version) {
    versionStore.delete(version);
  }

  @Override
  @Transactional(readOnly = true)
  public Version getVersion(long id) {
    return versionStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Version getVersionByKey(String key) {
    return versionStore.getVersionByKey(key);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Version> getAllVersions() {
    return versionStore.getAll();
  }
}
