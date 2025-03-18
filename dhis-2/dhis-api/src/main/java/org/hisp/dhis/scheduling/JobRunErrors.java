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
package org.hisp.dhis.scheduling;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.user.User;

/**
 * @author Jan Bernitt
 */
public interface JobRunErrors extends JsonObject {

  @Nonnull
  @OpenApi.Property({UID.class, JobConfiguration.class})
  default String id() {
    return getString("id").string();
  }

  @Nonnull
  default JobType _type() {
    return getString("type").parsed(JobType::valueOf);
  }

  @Nonnull
  @OpenApi.Property({UID.class, User.class})
  default String user() {
    return getString("user").string();
  }

  @Nonnull
  default JsonDate created() {
    return get("created", JsonDate.class);
  }

  @Nonnull
  default JsonDate executed() {
    return get("executed", JsonDate.class);
  }

  @Nonnull
  default JsonDate finished() {
    return get("finished", JsonDate.class);
  }

  @OpenApi.Description(
      """
    The file resource used to store the job's input
    """)
  @OpenApi.Property({UID.class, FileResource.class})
  default String file() {
    return getString("file").string();
  }

  default Long filesize() {
    return getNumber("filesize").longValue();
  }

  default String filetype() {
    return getString("filetype").string();
  }

  @Nonnull
  default String codes() {
    return getString("codes").string();
  }

  @Nonnull
  default JsonList<JobRunError> errors() {
    return getList("errors", JobRunError.class);
  }

  interface JobRunError extends JsonObject {

    @Nonnull
    default String code() {
      return getString("code").string();
    }

    @OpenApi.Property(UID.class)
    default String id() {
      return getString("id").string();
    }

    @Nonnull
    default String _type() {
      return getString("type").string();
    }

    default List<String> args() {
      return getArray("args").stringValues();
    }

    @Nonnull
    default String message() {
      return getString("message").string();
    }
  }
}
