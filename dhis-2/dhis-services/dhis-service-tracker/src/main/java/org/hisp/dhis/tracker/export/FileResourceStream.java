/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.util.ObjectUtils;

/**
 * FileResourceStream holds a file resource and a supplier to open an input stream to the file
 * resource content if needed.
 */
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class FileResourceStream {
  private final FileResource fileResource;
  private FileResourceSupplier<InputStream> inputStreamSupplier;

  @Nonnull
  public static FileResourceStream of(
      FileResourceService fileResourceService, FileResource fileResource) {
    return new FileResourceStream(
        fileResource,
        () -> {
          try {
            return fileResourceService.openContentStream(fileResource);
          } catch (NoSuchElementException e) {
            // Note: we are assuming that the file resource is not available yet. The same approach
            // is taken in other file endpoints or code relying on the storageStatus = PENDING.
            // All we know for sure is the file resource is in the DB but not in the store.
            throw new ConflictException(
                "The content is being processed and is not available yet. Try again later.");
          } catch (IOException e) {
            throw new ConflictException(
                "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend. "
                    + "Depending on the provider the root cause could be network or file system related.");
          }
        });
  }

  /**
   * Creates a {@code FileResourceStream} of an image of the given dimension. Dimension defaults to
   * the original dimensions if the given {@code dimension} is {@code null}.
   *
   * <p>Images have to be fetched and buffered in memory for image dimension other than the
   * original. This is due to us currently only storing the originals content length and md5 hash in
   * the DB. We thus need to read the image and compute its content length and md5 hash for them to
   * be available for cache validation.
   *
   * @param dimension the dimension of the image to create a stream for
   * @throws BadRequestException when the file resource is not an image, does not support multiple
   *     dimensions or does not have multiple dimension files stored
   */
  @Nonnull
  public static FileResourceStream ofImage(
      FileResourceService fileResourceService,
      FileResource fileResource,
      @CheckForNull ImageFileDimension dimension)
      throws BadRequestException, ConflictException {
    // The FileResource only stores the storageKey, contentLength and md5Hash of the original image.
    // At least for now we are losing the benefit of not fetching the file from storage if the
    // client already has an up-to-date version of the image in the given dimension other than the
    // original. We have to fetch and compute the length and hash of the image again.
    ImageFileDimension imageDimension =
        ObjectUtils.firstNonNull(dimension, ImageFileDimension.ORIGINAL);
    FileResourceStream fileResourceStream = new FileResourceStream(fileResource);
    if (imageDimension != ImageFileDimension.ORIGINAL) {
      byte[] content;
      try {
        content = fileResourceService.copyImageContent(fileResource, imageDimension);
        fileResourceStream.inputStreamSupplier = () -> new ByteArrayInputStream(content);
      } catch (NoSuchElementException e) {
        // Note: we are assuming that the file resource is not available yet. The same approach
        // is taken in other file endpoints or code relying on the storageStatus = PENDING.
        // All we know for sure is the file resource is in the DB but not in the store.
        throw new ConflictException(
            "The content is being processed and is not available yet. Try again later.");
      } catch (IOException e) {
        throw new ConflictException(
            "Failed fetching the file from storage",
            "There was an exception when trying to fetch the file from the storage backend. "
                + "Depending on the provider the root cause could be network or file system related.");
      }
      fileResource.setContentLength(content.length);
      fileResource.setContentMd5(Hashing.md5().hashBytes(content).toString());
    } else {
      fileResourceStream.inputStreamSupplier =
          () -> {
            try {
              return fileResourceService.openContentStreamToImage(fileResource, dimension);
            } catch (NoSuchElementException e) {
              // Note: we are assuming that the file resource is not available yet. The same
              // approach
              // is taken in other file endpoints or code relying on the storageStatus = PENDING.
              // All we know for sure is the file resource is in the DB but not in the store.
              throw new ConflictException(
                  "The content is being processed and is not available yet. Try again later.");
            } catch (IOException e) {
              throw new ConflictException(
                  "Failed fetching the file from storage",
                  "There was an exception when trying to fetch the file from the storage backend. "
                      + "Depending on the provider the root cause could be network or file system related.");
            }
          };
    }
    return fileResourceStream;
  }
}
