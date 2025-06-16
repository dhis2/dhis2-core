/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.util.ObjectUtils;

/**
 * FileResourceStream holds a file resource and a supplier to open an input stream to the file
 * resource content if needed. The content is wrapped in a supplier to avoid fetching the file if
 * it's not needed. {@link #ofImage(FileResourceService, FileResource, ImageFileDimension)} will
 * still need to read image variants (small, medium and large) into memory in order to provide the
 * content length. This is because the content length is only stored for the original image.
 */
public record FileResourceStream(
    String uid, String name, String contentType, FileResourceSupplier<Content> contentSupplier) {

  private static final String EXCEPTION_PENDING =
      "Content is being processed and is not available yet, try again later";
  public static final String EXCEPTION_IO = "Failed fetching file resource from storage";
  public static final String EXCEPTION_IO_DEV =
      "Exception occurred while fetching file resource from the storage backend";

  public record Content(long length, InputStream stream) {}

  @Nonnull
  public static FileResourceStream of(
      @Nonnull FileResourceService fileResourceService, @Nonnull FileResource fileResource) {
    return new FileResourceStream(
        fileResource.getUid(),
        fileResource.getName(),
        fileResource.getContentType(),
        () -> {
          try {
            return new Content(
                fileResource.getContentLength(),
                fileResourceService.openContentStream(fileResource));
          } catch (NoSuchElementException e) {
            // Note: we are assuming that the file resource is not available yet. The same approach
            // is taken in other file endpoints or code relying on the storageStatus = PENDING.
            // All we know for sure is the file resource is in the DB but not in the store.
            throw new ConflictException(EXCEPTION_PENDING);
          } catch (IOException e) {
            throw new ConflictException(EXCEPTION_IO, EXCEPTION_IO_DEV);
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
   */
  @Nonnull
  public static FileResourceStream ofImage(
      @Nonnull FileResourceService fileResourceService,
      @Nonnull FileResource fileResource,
      @CheckForNull ImageFileDimension dimension) {
    ImageFileDimension imageDimension =
        ObjectUtils.firstNonNull(dimension, ImageFileDimension.ORIGINAL);
    if (imageDimension == ImageFileDimension.ORIGINAL) {
      return new FileResourceStream(
          fileResource.getUid(),
          fileResource.getName(),
          fileResource.getContentType(),
          () -> {
            try {
              return new Content(
                  fileResource.getContentLength(),
                  fileResourceService.openContentStreamToImage(fileResource, imageDimension));
            } catch (NoSuchElementException e) {
              // Note: we are assuming that the file resource is not available yet. The same
              // approach
              // is taken in other file endpoints or code relying on the storageStatus = PENDING.
              // All we know for sure is the file resource is in the DB but not in the store.
              throw new ConflictException(EXCEPTION_PENDING);
            } catch (IOException e) {
              throw new ConflictException(EXCEPTION_IO, EXCEPTION_IO_DEV);
            }
          });
    }

    return new FileResourceStream(
        fileResource.getUid(),
        fileResource.getName(),
        fileResource.getContentType(),
        () -> {
          try {
            byte[] content = fileResourceService.copyImageContent(fileResource, imageDimension);
            return new Content(content.length, new ByteArrayInputStream(content));
          } catch (NoSuchElementException e) {
            // Note: we are assuming that the file resource is not available yet. The same approach
            // is taken in other file endpoints or code relying on the storageStatus = PENDING.
            // All we know for sure is the file resource is in the DB but not in the store.
            throw new ConflictException(EXCEPTION_PENDING);
          } catch (IOException e) {
            throw new ConflictException(EXCEPTION_IO, EXCEPTION_IO_DEV);
          }
        });
  }
}
