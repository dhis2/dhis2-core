/*
 * Copyright (c) 2004-2025, University of Oslo
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

import java.io.InputStream;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.ConflictException;
import org.springframework.util.MimeType;

public interface JobExecutionService {

  /**
   * Creates and runs a new job for one-off operations executed via the scheduler.
   *
   * @param config a new job that does not exist yet
   * @param contentType of the provided content data
   * @param content the data that should be processed by the job which is stored as file
   * @throws ConflictException in case the config belongs to an existing job or when the job isn't
   *     configured correctly
   */
  void executeOnceNow(
      @Nonnull JobConfiguration config, @Nonnull MimeType contentType, @Nonnull InputStream content)
      throws ConflictException;

  /**
   * Creates and runs a new job for one-off operations executed via the scheduler.
   *
   * @param config a new job that does not exist yet
   * @throws ConflictException in case the config belongs to an existing job or when the job isn't
   *     configured correctly
   */
  void executeOnceNow(@Nonnull JobConfiguration config) throws ConflictException;
}
