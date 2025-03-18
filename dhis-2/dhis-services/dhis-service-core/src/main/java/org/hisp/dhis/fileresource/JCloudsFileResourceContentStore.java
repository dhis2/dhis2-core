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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.File;
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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.jclouds.JCloudsStore;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.LocalBlobRequestSigner;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.internal.RequestSigningUnsupported;
import org.jclouds.http.HttpRequest;
import org.joda.time.Minutes;
import org.springframework.stereotype.Service;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.fileresource.FileResourceContentStore")
public class JCloudsFileResourceContentStore implements FileResourceContentStore {
  private static final long FIVE_MINUTES_IN_SECONDS =
      Minutes.minutes(5).toStandardDuration().getStandardSeconds();

  private final JCloudsStore jCloudsStore;

  @Override
  public InputStream getFileResourceContent(String key) {
    final Blob blob = jCloudsStore.getBlob(key);

    if (blob == null) {
      return null;
    }

    try {
      return blob.getPayload().openStream();
    } catch (IOException e) {
      log.warn(
          String.format(
              "Unable to retrieve fileResource with key: %s. Message: %s", key, e.getMessage()));
      return null;
    }
  }

  @Override
  public long getFileResourceContentLength(String key) {
    final Blob blob = jCloudsStore.getBlob(key);

    if (blob == null) {
      return 0;
    }

    return blob.getMetadata().getContentMetadata().getContentLength();
  }

  @Override
  public String saveFileResourceContent(@Nonnull FileResource fr, @Nonnull byte[] bytes) {
    return saveFileResourceContent(fr, createBlob(fr, bytes), null);
  }

  @Override
  public String saveFileResourceContent(@Nonnull FileResource fr, @Nonnull File file) {
    return saveFileResourceContent(
        fr,
        createBlob(fr, StringUtils.EMPTY, file, fr.getContentMd5()),
        () -> {
          try {
            Files.deleteIfExists(file.toPath());
          } catch (IOException ioe) {
            log.warn(
                String.format("Temporary file '%s' could not be deleted.", file.toPath()), ioe);
          }
        });
  }

  @CheckForNull
  private String saveFileResourceContent(
      @Nonnull FileResource fr, @CheckForNull Blob blob, @CheckForNull Runnable postPutCallback) {
    if (blob == null) {
      return null;
    }

    try {
      jCloudsStore.putBlob(blob);
    } catch (Exception e) {
      log.error("File upload failed: ", e);
      return null;
    }

    if (postPutCallback != null) {
      postPutCallback.run();
    }

    log.debug(String.format("File resource saved with key: %s", fr.getStorageKey()));

    return fr.getStorageKey();
  }

  @Override
  public String saveFileResourceContent(
      @Nonnull FileResource fr, @Nonnull Map<ImageFileDimension, File> imageFiles) {
    if (imageFiles.isEmpty()) {
      return null;
    }

    Blob blob;

    for (Map.Entry<ImageFileDimension, File> entry : imageFiles.entrySet()) {
      File file = entry.getValue();

      String contentMd5;

      try {
        HashCode hash = com.google.common.io.Files.asByteSource(file).hash(Hashing.md5());
        contentMd5 = hash.toString();
      } catch (IOException e) {
        log.error("Hashing error", e);
        return null;
      }

      blob = createBlob(fr, entry.getKey().getDimension(), file, contentMd5);

      if (blob != null) {
        try {
          jCloudsStore.putBlob(blob);
          Files.deleteIfExists(file.toPath());
        } catch (ContainerNotFoundException e) {
          log.error("Container not found", e);
          return null;
        } catch (IOException ioe) {
          log.warn(String.format("Temporary file '%s' could not be deleted: ", file.toPath()), ioe);
        }
      } else {
        return null;
      }
    }

    return fr.getStorageKey();
  }

  @Override
  public void deleteFileResourceContent(String key) {
    jCloudsStore.removeBlob(key);
  }

  @Override
  public boolean fileResourceContentExists(String key) {
    return jCloudsStore.blobExists(key);
  }

  @Override
  public URI getSignedGetContentUri(String key) {
    BlobRequestSigner signer = jCloudsStore.getBlobRequestSigner();

    if (!requestSigningSupported(signer)) {
      return null;
    }

    HttpRequest httpRequest;

    try {
      httpRequest =
          signer.signGetBlob(jCloudsStore.getBlobContainer(), key, FIVE_MINUTES_IN_SECONDS);
    } catch (UnsupportedOperationException uoe) {
      return null;
    }

    return httpRequest.getEndpoint();
  }

  @Override
  public void copyContent(String key, OutputStream output)
      throws IOException, NoSuchElementException {
    ensureBlobExists(key);

    try (InputStream in = jCloudsStore.getBlob(key).getPayload().openStream()) {
      IOUtils.copy(in, output);
    }
  }

  @Override
  public byte[] copyContent(String key) throws IOException, NoSuchElementException {
    ensureBlobExists(key);

    try (InputStream in = jCloudsStore.getBlob(key).getPayload().openStream()) {
      return IOUtils.toByteArray(in);
    }
  }

  @Override
  public InputStream openStream(String key) throws IOException, NoSuchElementException {
    ensureBlobExists(key);

    return jCloudsStore.getBlob(key).getPayload().openStream();
  }

  private void ensureBlobExists(String key) {
    if (!jCloudsStore.blobExists(key)) {
      throw new NoSuchElementException("key '" + key + "' not found.");
    }
  }

  private Blob createBlob(@Nonnull FileResource fileResource, @Nonnull byte[] bytes) {
    return jCloudsStore
        .getBlobStore()
        .blobBuilder(fileResource.getStorageKey())
        .payload(bytes)
        .contentLength(bytes.length)
        .contentMD5(HashCode.fromString(fileResource.getContentMd5()))
        .contentType(fileResource.getContentType())
        .contentDisposition("filename=" + fileResource.getName())
        .build();
  }

  private Blob createBlob(
      @Nonnull FileResource fileResource,
      String fileDimension,
      @Nonnull File file,
      @Nonnull String contentMd5) {
    return jCloudsStore
        .getBlobStore()
        .blobBuilder(StringUtils.join(fileResource.getStorageKey(), fileDimension))
        .payload(file)
        .contentLength(file.length())
        .contentMD5(HashCode.fromString(contentMd5))
        .contentType(fileResource.getContentType())
        .contentDisposition("filename=" + fileResource.getName() + fileDimension)
        .build();
  }

  private boolean requestSigningSupported(BlobRequestSigner signer) {
    return !(signer instanceof RequestSigningUnsupported)
        && !(signer instanceof LocalBlobRequestSigner);
  }
}
