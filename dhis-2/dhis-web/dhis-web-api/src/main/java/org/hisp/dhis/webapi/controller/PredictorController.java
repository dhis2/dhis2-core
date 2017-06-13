package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorService;
import org.hisp.dhis.schema.descriptors.PredictorSchemaDescriptor;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * @author Ken Haase (ken@dhis2.org)
 */
@Controller
@RequestMapping( value = PredictorSchemaDescriptor.API_ENDPOINT )
public class PredictorController
    extends AbstractCrudController<Predictor>
{
    private static final Log log = LogFactory.getLog( PredictorController.class );

    @Autowired
    private PredictorService predictorService;

    @Autowired
    private WebMessageService webMessageService;

    @RequestMapping( value = "/{uid}/run", method = { RequestMethod.POST, RequestMethod.PUT } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PREDICTOR_RUN')" )
    public void runPredictor(
        @PathVariable( "uid" ) String uid,
        @RequestParam Date startDate,
        @RequestParam Date endDate,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Predictor predictor = predictorService.getPredictor( uid );

        try
        {
            int count = predictorService.predict( predictor, startDate, endDate );

            webMessageService.send( WebMessageUtils.ok( "Generated " + count + " predictions" ), response, request );
        }
        catch ( Exception ex )
        {
            log.error( "Unable to predict " + predictor.getName(), ex );

            webMessageService.send( WebMessageUtils.conflict( "Unable to predict " + predictor.getName(), ex.getMessage() ), response, request );
        }
    }

    @RequestMapping( value = "/run", method = { RequestMethod.POST, RequestMethod.PUT } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PREDICTOR_RUN')" )
    public void runPredictors(
        @RequestParam Date startDate,
        @RequestParam Date endDate,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        int count = 0;

        List<Predictor> allPredictors = predictorService.getAllPredictors();

        for ( Predictor predictor : allPredictors )
        {
            try
            {
                count += predictorService.predict( predictor, startDate, endDate );
            }
            catch ( Exception ex )
            {
                log.error( "Unable to predict " + predictor.getName(), ex );

                webMessageService.send( WebMessageUtils.conflict( "Unable to predict " + predictor.getName(), ex.getMessage() ), response, request );

                return;
            }
        }

        webMessageService.send( WebMessageUtils.ok( "Generated " + count + " predictions" ), response, request );
    }
}