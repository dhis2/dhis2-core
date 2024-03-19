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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.IOException;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.ExternalFileResource;
import org.hisp.dhis.fileresource.ExternalFileResourceService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.schema.descriptors.ExternalFileResourceSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Stian Sandvold
 */
@OpenApi.Tags("system")
@Controller
@RequestMapping(ExternalFileResourceSchemaDescriptor.API_ENDPOINT)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ExternalFileResourceController {
  @Autowired private ExternalFileResourceService externalFileResourceService;

  @Autowired private FileResourceService fileResourceService;

  @Autowired private DhisConfigurationProvider dhisConfig;

  /**
   * Returns a file associated with the externalFileResource resolved from the accessToken.
   *
   * <p>Only files contained in externalFileResources with a valid accessToken, expiration date null
   * or in the future are files allowed to be served trough this endpoint.
   *
   * @param accessToken a unique string that resolves to a given externalFileResource
   * @param response
   * @throws WebMessageException
   */
  @OpenApi.Response(FileResource.class)
  @GetMapping("/{accessToken}")
  public void getExternalFileResource(
      @PathVariable String accessToken, HttpServletResponse response) throws WebMessageException {
    ExternalFileResource externalFileResource =
        externalFileResourceService.getExternalFileResourceByAccessToken(accessToken);

    if (externalFileResource == null) {
      throw new WebMessageException(notFound("No file found with key '" + accessToken + "'"));
    }

    if (externalFileResource.getExpires() != null
        && externalFileResource.getExpires().before(new Date())) {
      throw new WebMessageException(
          WebMessageUtils.createWebMessage(
              "The key you requested has expired", Status.WARNING, HttpStatus.GONE));
    }

    FileResource fileResource = externalFileResource.getFileResource();

    response.setContentType(fileResource.getContentType());
    response.setContentLengthLong(fileResource.getContentLength());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());

    HeaderUtils.setSecurityHeaders(
        response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));
    setNoStore(response);

    try {
      fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
    } catch (IOException e) {
      throw new WebMessageException(
          error(
              "Failed fetching the file from storage",
              "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related"));
    }
  }
}
