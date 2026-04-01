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

import java.util.UUID;
import javax.annotation.Nonnull;
import org.hisp.dhis.storage.BlobKey;

/**
 * Factory methods for constructing typed {@link BlobKey} values for {@link FileResource} blobs.
 *
 * <p>Keys follow the pattern {@code <domainPrefix>/<identifier>}, where {@code domainPrefix} comes
 * from {@link FileResourceDomain#getContainerName()} (e.g. {@code "dataValue"}, {@code "icon"}).
 *
 * <p>Use {@link #makeKey(FileResourceDomain, String)} when the identifier is already known (e.g.
 * when creating an icon or a job-data resource with a fixed key). Use {@link
 * #makeKeyWithRandomUUID(FileResourceDomain)} when a new unique key is needed, such as when
 * uploading a new data-value file.
 */
public class FileResourceKeyUtil {
  private FileResourceKeyUtil() {}

  /**
   * Returns a {@link BlobKey} of the form {@code <domainPrefix>/<key>} for the given domain and
   * known identifier.
   */
  public static BlobKey makeKey(@Nonnull FileResourceDomain domain, @Nonnull String key) {
    return new BlobKey(domain.getContainerName() + "/" + key);
  }

  /**
   * Returns a {@link BlobKey} of the form {@code <domainPrefix>/<uuid>} using a freshly generated
   * random UUID as the identifier. Use this when no external identifier exists for the resource.
   */
  public static BlobKey makeKeyWithRandomUUID(@Nonnull FileResourceDomain domain) {
    return new BlobKey(domain.getContainerName() + "/" + UUID.randomUUID());
  }
}
