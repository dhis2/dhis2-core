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

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import javax.annotation.CheckForNull;

/**
 * Provider-agnostic abstraction over a blob/object store. Implementations exist for JClouds
 * (current) and will be added for other backends (e.g. MinIO SDK + NIO filesystem) when JClouds is
 * replaced.
 *
 * <p>All methods operate against a single container/bucket configured at startup. Keys are
 * path-like strings (e.g. {@code apps/my-app/index.html}).
 */
public interface BlobStoreService {

  /** Returns {@code true} if a blob with the given key exists in the container. */
  boolean blobExists(String key);

  /**
   * Opens a stream for the blob content. Returns {@code null} if no blob exists for the key.
   * Callers are responsible for closing the returned stream.
   */
  @CheckForNull
  InputStream openStream(String key);

  /** Returns the content length in bytes of the blob, or {@code 0} if the blob does not exist. */
  long contentLength(String key);

  /**
   * Stores a byte-array payload. {@code contentMd5} is the hex-encoded MD5 hash and may be {@code
   * null} if not known.
   */
  void putBlob(
      String key,
      byte[] content,
      String contentType,
      String contentDisposition,
      @CheckForNull String contentMd5);

  /**
   * Stores a file payload. {@code contentMd5} is the hex-encoded MD5 hash and may be {@code null}
   * if not known.
   */
  void putBlob(
      String key,
      File content,
      String contentType,
      String contentDisposition,
      @CheckForNull String contentMd5);

  /**
   * Stores a streaming payload. {@code contentType} may be {@code null} when unknown (e.g. entries
   * extracted from a zip archive).
   */
  void putBlob(
      String key, InputStream content, long contentLength, @CheckForNull String contentType);

  /** Deletes the blob with the given key. A no-op if the blob does not exist. */
  void deleteBlob(String key);

  /**
   * Recursively deletes all blobs whose key starts with {@code prefix}. On filesystem backends this
   * maps to a directory delete; on object-store backends it performs per-key deletion.
   */
  void deleteDirectory(String prefix);

  /**
   * Lists the names of immediate child "folders" under {@code prefix} (non-recursive, equivalent to
   * a delimiter-{@code /} list). Returned names include the trailing {@code /}.
   */
  Iterable<String> listFolders(String prefix);

  /**
   * Lists all blob keys whose key starts with {@code prefix} (recursive). May return an empty
   * iterable if no matching blobs exist.
   */
  Iterable<String> listKeys(String prefix);

  /**
   * Returns a pre-signed GET URI valid for {@code expirationSeconds} seconds, or {@code null} if
   * the backend does not support request signing (e.g. local filesystem).
   */
  @CheckForNull
  URI signedGetUri(String key, long expirationSeconds);

  /** Returns the name of the container/bucket all blobs are stored in. */
  String container();

  /** Returns {@code true} if this service is backed by the local filesystem. */
  boolean isFilesystem();
}
