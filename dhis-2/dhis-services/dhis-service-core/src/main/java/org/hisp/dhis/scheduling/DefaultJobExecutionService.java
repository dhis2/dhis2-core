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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJobExecutionService implements JobExecutionService {

  private final JobConfigurationService jobConfigurationService;
  private final JobSchedulerService jobSchedulerService;

  @Override
  @IndirectTransactional
  public void executeOnceNow(
      @Nonnull JobConfiguration config, @Nonnull MimeType contentType, @Nonnull InputStream content)
      throws ConflictException {
    validateIsNewRunOnce(config);
    executeOnceNow(jobConfigurationService.create(config, contentType, content));
  }

  @Override
  @IndirectTransactional
  public void executeOnceNow(@Nonnull JobConfiguration config) throws ConflictException {
    validateIsNewRunOnce(config);
    executeOnceNow(jobConfigurationService.create(config));
  }

  private void executeOnceNow(String jobId) throws ConflictException {
    try {
      jobSchedulerService.executeNow(UID.ofNullable(jobId));
    } catch (NotFoundException ex) {
      log.error("Ad-hoc job creation failed", ex);
      ConflictException error = new ConflictException("Ad-hoc job creation failed");
      error.initCause(ex);
      throw error;
    }
  }

  private void validateIsNewRunOnce(JobConfiguration config) throws ConflictException {
    if (config.getId() != 0 || config.getSchedulingType() != SchedulingType.ONCE_ASAP)
      throw new ConflictException(
          "Job %s must be a run once type but was: %s"
              .formatted(config.getName(), config.getSchedulingType()));
  }
}
