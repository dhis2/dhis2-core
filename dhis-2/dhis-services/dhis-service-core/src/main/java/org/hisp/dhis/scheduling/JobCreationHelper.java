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
package org.hisp.dhis.scheduling;

import java.io.InputStream;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.springframework.util.MimeType;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface JobCreationHelper {

  String create(JobConfiguration config) throws ConflictException;

  String create(JobConfiguration config, MimeType contentType, InputStream content)
      throws ConflictException;

  default String createFromConfig(JobConfiguration config, JobConfigurationStore store) {
    config.setAutoFields();
    store.save(config);
    return config.getUid();
  }

  default String createFromConfigAndInputStream(
      JobConfiguration config,
      MimeType contentType,
      InputStream content,
      JobConfigurationStore store,
      FileResourceService fileResourceService)
      throws ConflictException {
    if (config.getSchedulingType() != SchedulingType.ONCE_ASAP)
      throw new ConflictException(
          "Job must be of type %s to allow content data".formatted(SchedulingType.ONCE_ASAP));
    config.setAutoFields(); // ensure UID is set
    FileResource fr = FileResource.ofKey(FileResourceDomain.JOB_DATA, config.getUid(), contentType);
    fr.setUid(config.getUid());
    fr.setAssigned(true);
    fileResourceService.syncSaveFileResource(fr, content);
    store.save(config);
    return config.getUid();
  }
}
