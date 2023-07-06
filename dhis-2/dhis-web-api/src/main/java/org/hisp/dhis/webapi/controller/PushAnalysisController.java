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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.pushanalysis.PushAnalysisService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.schema.descriptors.PushAnalysisSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping(PushAnalysisSchemaDescriptor.API_ENDPOINT)
@Slf4j
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class PushAnalysisController extends AbstractCrudController<PushAnalysis> {
  @Autowired private PushAnalysisService pushAnalysisService;

  @Autowired private ContextUtils contextUtils;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private SchedulingManager schedulingManager;

  @GetMapping("/{uid}/render")
  public void renderPushAnalytics(@PathVariable() String uid, HttpServletResponse response)
      throws WebMessageException, IOException {
    PushAnalysis pushAnalysis = pushAnalysisService.getByUid(uid);

    if (pushAnalysis == null) {
      throw new WebMessageException(notFound("Push analysis with uid " + uid + " was not found"));
    }

    contextUtils.configureResponse(
        response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.NO_CACHE);

    log.info(
        "User '"
            + currentUserService.getCurrentUser().getUsername()
            + "' started PushAnalysis for 'rendering'");

    String result =
        pushAnalysisService.generateHtmlReport(pushAnalysis, currentUserService.getCurrentUser());
    response.getWriter().write(result);
    response.getWriter().close();
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/{uid}/run")
  public void sendPushAnalysis(@PathVariable() String uid) throws WebMessageException {
    PushAnalysis pushAnalysis = pushAnalysisService.getByUid(uid);

    if (pushAnalysis == null) {
      throw new WebMessageException(notFound("Push analysis with uid " + uid + " was not found"));
    }

    JobConfiguration config =
        new JobConfiguration(
            "pushAnalysisJob from controller",
            JobType.PUSH_ANALYSIS,
            "",
            new PushAnalysisJobParameters(uid),
            true,
            true);
    schedulingManager.executeNow(config);
  }
}
