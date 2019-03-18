package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.predictor.PredictionService;
import org.hisp.dhis.predictor.PredictionSummary;
import org.hisp.dhis.predictor.PredictionTask;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.PREDICTOR;

/**
 * @author Jim Grace
 */

@Controller
@RequestMapping( value = PredictionController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class PredictionController
{
    public static final String RESOURCE_PATH = "/predictions";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PREDICTOR_RUN')" )
    public void runPredictors(
        @RequestParam Date startDate,
        @RequestParam Date endDate,
        @RequestParam( value = "predictor", required = false ) List<String> predictors,
        @RequestParam( value = "predictorGroup", required = false ) List<String> predictorGroups,
        @RequestParam( defaultValue = "false", required = false ) boolean async,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        if ( async )
        {
            JobConfiguration jobId = new JobConfiguration( "inMemoryPrediction", PREDICTOR, currentUserService.getCurrentUser().getUid(), true );

            schedulingManager.executeJob( new PredictionTask( startDate, endDate, predictors, predictorGroups, predictionService, jobId ) );

            response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + PREDICTOR );

            webMessageService.send( jobConfigurationReport( jobId ), response, request );
        }
        else
        {
            PredictionSummary predictionSummary = predictionService.predictTask( startDate, endDate, predictors, predictorGroups, null );

            renderService.toJson( response.getOutputStream(), predictionSummary );
        }
    }
}
