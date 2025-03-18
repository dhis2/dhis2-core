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

import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = Report.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ReportTemplateController {
  @Autowired private ContextUtils contextUtils;

  @OpenApi.Response(String.class)
  @GetMapping(value = "/api/reportTemplate.xml", produces = APPLICATION_XML_VALUE)
  public void getReportDesignJrxml(HttpServletResponse response) throws Exception {
    serveTemplate(response, ContextUtils.CONTENT_TYPE_XML, "jasper-report-template.jrxml");
  }

  @OpenApi.Response(String.class)
  @GetMapping(value = "/api/reportTemplate.html", produces = APPLICATION_XML_VALUE)
  public void getReportDesignHtml(HttpServletResponse response) throws Exception {
    serveTemplate(response, ContextUtils.CONTENT_TYPE_HTML, "html-report-template.html");
  }

  private void serveTemplate(HttpServletResponse response, String contentType, String template)
      throws IOException {
    contextUtils.configureResponse(
        response, contentType, CacheStrategy.CACHE_1_HOUR, template, true);

    String content =
        IOUtils.toString(new ClassPathResource(template).getInputStream(), StandardCharsets.UTF_8);

    IOUtils.write(content, response.getWriter());
  }
}
