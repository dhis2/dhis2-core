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
package org.hisp.dhis.fileresource;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Value;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.joda.time.DateTime;

public interface FileResourceStore extends IdentifiableObjectStore<FileResource> {
  List<FileResource> getExpiredFileResources(DateTime expires);

  List<FileResource> getAllUnProcessedImages();

  /** UIDs of documents that own the given file resource. */
  List<String> findDocumentsByFileResource(@Nonnull String uid);

  /** UIDs of users whose avatar is the given file resource. */
  List<String> findUsersByAvatarFileResource(@Nonnull String uid);

  /** UIDs of participants in conversations containing the given attachment. */
  List<String> findMessageAttachmentParticipants(@Nonnull String uid);

  /**
   * Lookup of the aggregate data values of a file value type that hold the given file resource as
   * their current value or held it as a former value, as recorded in their audit.
   *
   * @param uid of a file resource
   * @return where each such data value belongs, once per distinct combination
   */
  List<DataValueFileReference> findDataValueReferencesByFileResourceValue(@Nonnull String uid);

  /** Where an aggregate data value that holds, or held, a file resource as value belongs. */
  @Value
  class DataValueFileReference {
    /** Path of the organisation unit of the data value. */
    @Nonnull String orgUnitPath;

    /** Uid of the category option combo of the data value. */
    @Nonnull String categoryOptionCombo;

    /** Uid of the attribute option combo of the data value. */
    @Nonnull String attributeOptionCombo;
  }
}
