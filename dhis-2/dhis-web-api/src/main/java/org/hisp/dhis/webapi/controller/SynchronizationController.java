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
package org.hisp.dhis.webapi.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
@OpenApi.Tags("data")
@Controller
@RequestMapping(value = SynchronizationController.RESOURCE_PATH)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class SynchronizationController {
  public static final String RESOURCE_PATH = "/synchronization";

  @Autowired private SynchronizationManager synchronizationManager;

  @Autowired private RestTemplate restTemplate;

  @PreAuthorize("hasRole('ALL') or hasRole('F_EXPORT_DATA')")
  @PostMapping(value = "/dataPush", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ImportConflicts execute() throws IOException {
    return synchronizationManager.executeDataValuePush();
  }

  @PreAuthorize("hasRole('ALL')")
  @PostMapping(value = "/metadataPull", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ImportReport importMetaData(@RequestBody String url) {
    return synchronizationManager.executeMetadataPull(url);
  }

  @GetMapping(value = "/availability", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody AvailabilityStatus remoteServerAvailable() {
    return synchronizationManager.isRemoteServerAvailable();
  }

  @GetMapping(value = "/metadataRepo", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody String getMetadataRepoIndex() {
    return restTemplate.getForObject(
        SettingKey.METADATA_REPO_URL.getDefaultValue().toString(), String.class);
  }
}
