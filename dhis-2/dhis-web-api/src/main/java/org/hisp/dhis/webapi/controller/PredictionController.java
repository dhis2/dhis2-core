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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.PREDICTOR;
import static org.hisp.dhis.security.Authorities.F_PREDICTOR_RUN;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.predictor.PredictionService;
import org.hisp.dhis.predictor.PredictionSummary;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jim Grace
 */
@OpenApi.Document(
    entity = Predictor.class,
    classifiers = {"team:platform", "purpose:metadata"})
@Controller
@RequestMapping("/api/predictions")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class PredictionController {

  private final PredictionService predictionService;
  private final JobExecutionService jobExecutionService;

  @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
  @RequiresAuthority(anyOf = F_PREDICTOR_RUN)
  @ResponseBody
  public WebMessage runPredictors(
      @RequestParam Date startDate,
      @RequestParam Date endDate,
      @RequestParam(value = "predictor", required = false) List<String> predictors,
      @RequestParam(value = "predictorGroup", required = false) List<String> predictorGroups,
      @RequestParam(defaultValue = "false", required = false) boolean async,
      @CurrentUser UserDetails currentUser)
      throws ConflictException {

    if (async) {
      JobConfiguration config = new JobConfiguration(PREDICTOR);
      PredictorJobParameters params =
          PredictorJobParameters.builder()
              .startDate(startDate)
              .endDate(endDate)
              .predictors(predictors)
              .predictorGroups(predictorGroups)
              .build();
      config.setJobParameters(params);
      config.setExecutedBy(currentUser.getUid());

      jobExecutionService.executeOnceNow(config);

      return jobConfigurationReport(config);
    }
    PredictionSummary predictionSummary =
        predictionService.predictTask(
            startDate, endDate, predictors, predictorGroups, JobProgress.noop());

    return new WebMessage(Status.OK, HttpStatus.OK).setResponse(predictionSummary);
  }
}
