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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_PREDICTOR_RUN;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.predictor.PredictionService;
import org.hisp.dhis.predictor.PredictionSummary;
import org.hisp.dhis.predictor.PredictorGroup;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.security.RequiresAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Jim Grace
 */
@Controller
@RequestMapping("/api/predictorGroups")
@RequiredArgsConstructor
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class PredictorGroupController
    extends AbstractCrudController<PredictorGroup, GetObjectListParams> {

  private final PredictionService predictionService;

  @RequestMapping(
      value = "/{uid}/run",
      method = {RequestMethod.POST, RequestMethod.PUT})
  @RequiresAuthority(anyOf = F_PREDICTOR_RUN)
  @ResponseBody
  public WebMessage runPredictorGroup(
      @PathVariable("uid") String uid, @RequestParam Date startDate, @RequestParam Date endDate) {
    try {
      PredictionSummary predictionSummary =
          predictionService.predictAll(startDate, endDate, null, List.of(uid), JobProgress.noop());

      return ok("Generated " + predictionSummary.getPredictions() + " predictions");
    } catch (Exception ex) {
      return conflict("Unable to predict from predictor group " + uid, ex.getMessage());
    }
  }
}
