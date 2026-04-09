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
package org.hisp.dhis.fileresource;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hisp.dhis.storage.BlobKey;
import org.hisp.dhis.storage.BlobStoreService;
import org.hisp.dhis.storage.BlobStoreService.ContentDisposition;
import org.hisp.dhis.storage.ContentHash;
import org.springframework.stereotype.Service;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.fileresource.FileResourceContentStore")
public class JCloudsFileResourceContentStore implements FileResourceContentStore {
  private static final long FIVE_MINUTES_IN_SECONDS = 300L;

  private final BlobStoreService blobStore;

  @Override
  public InputStream getFileResourceContent(BlobKey key) {
    return blobStore.openStream(key);
  }

  @Override
  public long getFileResourceContentLength(BlobKey key) {
    return blobStore.contentLength(key);
  }

  @Override
  @CheckForNull
  public String saveFileResourceContent(@Nonnull FileResource fr, @Nonnull byte[] bytes) {
    try (InputStream is = new ByteArrayInputStream(bytes)) {
      blobStore.putBlob(
          fr.asBlobKey(),
          is,
          bytes.length,
          fr.getContentType(),
          ContentDisposition.filename(fr.getName()),
          ContentHash.ofNullable(fr.getContentMd5()));
    } catch (Exception e) {
      log.error("File upload failed: ", e);
      return null;
    }

    log.debug(String.format("File resource saved with key: %s", fr.getStorageKey()));
    return fr.getStorageKey();
  }

  @Override
  @CheckForNull
  public String saveFileResourceContent(@Nonnull FileResource fr, @Nonnull File file) {
    try (InputStream is = new FileInputStream(file)) {
      blobStore.putBlob(
          fr.asBlobKey(),
          is,
          file.length(),
          fr.getContentType(),
          ContentDisposition.filename(fr.getName()),
          ContentHash.ofNullable(fr.getContentMd5()));
    } catch (Exception e) {
      log.error("File upload failed: ", e);
      return null;
    }

    try {
      Files.deleteIfExists(file.toPath());
    } catch (IOException ioe) {
      log.warn(String.format("Temporary file '%s' could not be deleted.", file.toPath()), ioe);
    }

    log.debug(String.format("File resource saved with key: %s", fr.getStorageKey()));
    return fr.getStorageKey();
  }

  @Override
  @CheckForNull
  public String saveFileResourceContent(
      @Nonnull FileResource fr, @Nonnull Map<ImageFileDimension, File> imageFiles) {
    if (imageFiles.isEmpty()) {
      return null;
    }

    for (Map.Entry<ImageFileDimension, File> entry : imageFiles.entrySet()) {
      File file = entry.getValue();
      String dimension = entry.getKey().getDimension();

      ContentHash contentHash;
      try {
        contentHash =
            ContentHash.of(com.google.common.io.Files.asByteSource(file).hash(Hashing.md5()));
      } catch (IOException e) {
        log.error("Hashing error", e);
        return null;
      }

      try (InputStream is = new FileInputStream(file)) {
        blobStore.putBlob(
            new BlobKey(fr.getStorageKey() + dimension),
            is,
            file.length(),
            fr.getContentType(),
            ContentDisposition.filename(fr.getName() + dimension),
            contentHash);
      } catch (Exception e) {
        log.error("Image file upload failed: ", e);
        return null;
      }

      try {
        Files.deleteIfExists(file.toPath());
      } catch (IOException ioe) {
        log.warn(String.format("Temporary file '%s' could not be deleted: ", file.toPath()), ioe);
      }
    }

    return fr.getStorageKey();
  }

  @Override
  public void deleteFileResourceContent(BlobKey key) {
    blobStore.deleteBlob(key);
  }

  @Override
  public boolean fileResourceContentExists(BlobKey key) {
    return blobStore.blobExists(key);
  }

  @Override
  @CheckForNull
  public URI getSignedGetContentUri(BlobKey key) {
    return blobStore.signedGetUri(key, FIVE_MINUTES_IN_SECONDS);
  }

  @Override
  public void copyContent(BlobKey key, OutputStream output)
      throws IOException, NoSuchElementException {
    ensureBlobExists(key);

    try (InputStream in = blobStore.openStream(key)) {
      IOUtils.copy(in, output);
    }
  }

  @Override
  public byte[] copyContent(BlobKey key) throws IOException, NoSuchElementException {
    ensureBlobExists(key);

    try (InputStream in = blobStore.openStream(key)) {
      return IOUtils.toByteArray(in);
    }
  }

  @Override
  public InputStream openStream(BlobKey key) throws IOException, NoSuchElementException {
    ensureBlobExists(key);
    return blobStore.openStream(key);
  }

  private void ensureBlobExists(BlobKey key) {
    if (!blobStore.blobExists(key)) {
      throw new NoSuchElementException("key '" + key + "' not found.");
    }
  }
}
