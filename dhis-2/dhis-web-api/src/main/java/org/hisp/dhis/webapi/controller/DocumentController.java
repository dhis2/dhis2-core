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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Lars Helge Overland
 */
@Controller
@Slf4j
@RequestMapping("/api/documents")
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class DocumentController extends AbstractCrudController<Document, GetObjectListParams> {

  @Autowired private DocumentService documentService;

  @Autowired private LocationManager locationManager;

  @Autowired private FileResourceService fileResourceService;

  @Autowired private ContextUtils contextUtils;

  @Autowired private DhisConfigurationProvider dhisConfig;

  @GetMapping("/{uid}/data")
  public void getDocumentContent(@PathVariable("uid") String uid, HttpServletResponse response)
      throws Exception {
    Document document = documentService.getDocument(uid);

    if (document == null) {
      throw new WebMessageException(
          notFound(String.format("Document not found or not accessible: '%s'", uid)));
    }

    if (document.isExternal()) {
      response.sendRedirect(response.encodeRedirectURL(document.getUrl()));
    } else if (document.getFileResource() != null) {
      FileResource fileResource = document.getFileResource();

      response.setContentType(fileResource.getContentType());
      response.setContentLengthLong(fileResource.getContentLength());
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());
      HeaderUtils.setSecurityHeaders(
          response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));

      try {
        fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
      } catch (IOException ex) {
        throw new WebMessageException(
            error(FileResourceStream.EXCEPTION_IO, FileResourceStream.EXCEPTION_IO_DEV));
      }
    } else {
      contextUtils.configureResponse(
          response,
          document.getContentType(),
          CacheStrategy.CACHE_TWO_WEEKS,
          document.getUrl(),
          document.isAttachmentDefaultFalse());

      try (InputStream in =
          locationManager.getInputStream(document.getUrl(), DocumentService.DIR)) {
        IOUtils.copy(in, response.getOutputStream());
      } catch (IOException ex) {
        log.error("Could not retrieve file", ex);
        throw new WebMessageException(
            error(FileResourceStream.EXCEPTION_IO, FileResourceStream.EXCEPTION_IO_DEV));
      }
    }
  }
}
