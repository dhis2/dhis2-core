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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.PREDICTOR;

import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.predictor.PredictionService;
import org.hisp.dhis.predictor.PredictionSummary;
import org.hisp.dhis.predictor.PredictionTask;
import org.hisp.dhis.scheduling.ControlledJobProgress;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NotifierJobProgress;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jim Grace
 */
@OpenApi.Tags("analytics")
@Controller
@RequestMapping(value = PredictionController.RESOURCE_PATH)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class PredictionController {
  public static final String RESOURCE_PATH = "/predictions";

  private final CurrentUserService currentUserService;

  private final AsyncTaskExecutor taskExecutor;

  private final PredictionService predictionService;

  private final Notifier notifier;

  private final MessageService messageService;

  @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PREDICTOR_RUN')")
  @ResponseBody
  public WebMessage runPredictors(
      @RequestParam Date startDate,
      @RequestParam Date endDate,
      @RequestParam(value = "predictor", required = false) List<String> predictors,
      @RequestParam(value = "predictorGroup", required = false) List<String> predictorGroups,
      @RequestParam(defaultValue = "false", required = false) boolean async,
      HttpServletRequest request) {
    JobConfiguration jobId =
        new JobConfiguration(
            "inMemoryPrediction", PREDICTOR, currentUserService.getCurrentUser().getUid(), true);

    JobProgress progress =
        new ControlledJobProgress(
            messageService, jobId, new NotifierJobProgress(notifier, jobId), true);
    if (async) {
      taskExecutor.executeTask(
          new PredictionTask(
              startDate, endDate, predictors, predictorGroups, predictionService, progress));

      return jobConfigurationReport(jobId).setLocation("/system/tasks/" + PREDICTOR);
    }
    PredictionSummary predictionSummary =
        predictionService.predictTask(startDate, endDate, predictors, predictorGroups, progress);

    return new WebMessage(Status.OK, HttpStatus.OK)
        .setResponse(predictionSummary)
        .withPlainResponseBefore(DhisApiVersion.V38);
  }
}
