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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.pushanalysis.PushAnalysisService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
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
@RequestMapping("/api/pushAnalysis")
@Slf4j
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
@OpenApi.Document(classifiers = {"team:platform", "purpose:support"})
public class PushAnalysisController
    extends AbstractCrudController<PushAnalysis, GetObjectListParams> {

  private final PushAnalysisService pushAnalysisService;
  private final ContextUtils contextUtils;
  private final JobExecutionService jobExecutionService;

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
        "User '" + CurrentUserUtil.getCurrentUsername() + "' started PushAnalysis for 'rendering'");

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    String result = pushAnalysisService.generateHtmlReport(pushAnalysis, currentUser);
    response.getWriter().write(result);
    response.getWriter().close();
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/{uid}/run")
  public void sendPushAnalysis(@PathVariable() String uid)
      throws NotFoundException, ConflictException {
    PushAnalysis pushAnalysis = pushAnalysisService.getByUid(uid);

    if (pushAnalysis == null) {
      throw new NotFoundException(PushAnalysis.class, uid);
    }

    JobConfiguration config = new JobConfiguration(JobType.PUSH_ANALYSIS);
    config.setJobParameters(new PushAnalysisJobParameters(uid));
    config.setExecutedBy(CurrentUserUtil.getCurrentUserDetails().getUid());

    jobExecutionService.executeOnceNow(config);
  }
}
