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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.security.Authorities.F_EXPORT_DATA;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.sync.AvailabilityStatus;
import org.hisp.dhis.dxf2.sync.SynchronizationManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = Server.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequestMapping("/api/synchronization")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class SynchronizationController {

  private final SynchronizationManager synchronizationManager;
  private final DhisConfigurationProvider configProvider;
  private final RestTemplate restTemplate;

  @RequiresAuthority(anyOf = F_EXPORT_DATA)
  @PostMapping(value = "/dataPush", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ImportConflicts execute() throws IOException {
    return synchronizationManager.executeDataValuePush();
  }

  /**
   * This endpoint is used to perform a metadata pull from a remote url. It accepts a user-supplied
   * string parameter. The url is trimmed to remove any {@linkplain Character#isWhitespace(int)
   * white space}. This is recommended when the parameter could be logged (which occurs later in the
   * call chain).
   *
   * @param url to retrieve metadata from
   * @return import report
   */
  @RequiresAuthority(anyOf = ALL)
  @PostMapping(value = "/metadataPull", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ImportReport importMetaData(@RequestBody @Nonnull String url) throws ConflictException {
    String urlTrimmed = url.trim();
    if (configProvider.isMetaDataSyncRemoteServerAllowed(urlTrimmed)) {
      return synchronizationManager.executeMetadataPull(urlTrimmed);
    }
    throw new ConflictException("Provided URL is not in the remote servers allowed list");
  }

  @GetMapping(value = "/availability", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody AvailabilityStatus remoteServerAvailable() {
    return synchronizationManager.isRemoteServerAvailable();
  }

  @GetMapping(value = "/metadataRepo", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody String getMetadataRepoIndex() {
    return restTemplate.getForObject(
        "https://raw.githubusercontent.com/dhis2/dhis2-metadata-repo/master/repo/221/index.json",
        String.class);
  }
}
