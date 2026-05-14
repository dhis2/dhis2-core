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
package org.hisp.dhis.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;

/**
 * In-memory {@link BlobStoreService} used by tests and any deployment with {@code
 * filestore.provider=transient}. Replaces the JClouds {@code transient} provider.
 *
 * <p>State is held in a single {@link ConcurrentHashMap} keyed by the full blob key; nothing is
 * persisted across JVM restarts. {@link #signedGetUri} returns {@code null}.
 */
public class TransientBlobStoreService implements BlobStoreService {

  private final BlobContainerName container;
  private final Map<String, byte[]> blobs = new ConcurrentHashMap<>();

  public TransientBlobStoreService(DhisConfigurationProvider configurationProvider) {
    this.container =
        new BlobContainerName(
            configurationProvider.getProperty(ConfigurationKey.FILESTORE_CONTAINER));
  }

  @Override
  public boolean blobExists(BlobKey key) {
    return key != null && blobs.containsKey(key.value());
  }

  @Override
  @CheckForNull
  public InputStream openStream(BlobKey key) {
    byte[] payload = blobs.get(key.value());
    return payload == null ? null : new ByteArrayInputStream(payload);
  }

  @Override
  public long contentLength(BlobKey key) {
    byte[] payload = blobs.get(key.value());
    return payload == null ? 0L : payload.length;
  }

  @Override
  public void putBlob(
      BlobKey key,
      InputStream content,
      long contentLength,
      @CheckForNull String contentType,
      @CheckForNull ContentDisposition contentDisposition,
      @CheckForNull ContentHash contentHash) {
    try {
      blobs.put(key.value(), content.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read blob payload for " + key, e);
    }
  }

  @Override
  public void deleteBlob(BlobKey key) {
    blobs.remove(key.value());
  }

  @Override
  public void deleteDirectory(BlobKeyPrefix prefix) {
    String pfx = prefix.value() + "/";
    blobs.keySet().removeIf(k -> k.startsWith(pfx));
  }

  @Override
  public Iterable<BlobKeyPrefix> listFolders(BlobKeyPrefix prefix) {
    String pfx = prefix.value() + "/";
    Set<String> immediate = new HashSet<>();
    for (String k : blobs.keySet()) {
      if (!k.startsWith(pfx)) continue;
      String remainder = k.substring(pfx.length());
      int slash = remainder.indexOf('/');
      if (slash < 0) continue; // file at this level, not a folder
      immediate.add(pfx + remainder.substring(0, slash));
    }
    List<BlobKeyPrefix> result = new ArrayList<>(immediate.size());
    for (String p : immediate) result.add(BlobKeyPrefix.of(p));
    return result;
  }

  @Override
  public Iterable<BlobKey> listKeys(BlobKeyPrefix prefix) {
    String pfx = prefix.value();
    List<BlobKey> result = new ArrayList<>();
    for (String k : blobs.keySet()) {
      if (k.equals(pfx) || k.startsWith(pfx + "/")) {
        result.add(new BlobKey(k));
      }
    }
    return result;
  }

  @Override
  @CheckForNull
  public URI signedGetUri(BlobKey key, long expirationSeconds) {
    return null;
  }

  @Override
  public BlobContainerName container() {
    return container;
  }

  @Override
  public boolean isFilesystem() {
    return false;
  }
}
