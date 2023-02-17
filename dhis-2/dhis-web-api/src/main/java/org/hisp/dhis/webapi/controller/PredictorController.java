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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_SKIP_TEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.predictor.PredictionService;
import org.hisp.dhis.predictor.PredictionSummary;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorService;
import org.hisp.dhis.schema.descriptors.PredictorSchemaDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Ken Haase (ken@dhis2.org)
 */
@OpenApi.Tags( "metadata" )
@Controller
@Slf4j
@RequestMapping( value = PredictorSchemaDescriptor.API_ENDPOINT )
public class PredictorController extends AbstractCrudController<Predictor>
{
    @Autowired
    private PredictorService predictorService;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private I18nManager i18nManager;

    @RequestMapping( value = "/{uid}/run", method = { RequestMethod.POST, RequestMethod.PUT } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PREDICTOR_RUN')" )
    @ResponseBody
    public WebMessage runPredictor(
        @PathVariable( "uid" ) String uid,
        @RequestParam Date startDate,
        @RequestParam Date endDate,
        TranslateParams translateParams )
    {
        Predictor predictor = predictorService.getPredictor( uid );

        try
        {
            PredictionSummary predictionSummary = new PredictionSummary();

            predictionService.predict( predictor, startDate, endDate, predictionSummary );

            return ok( "Generated " + predictionSummary.getPredictions() + " predictions" );
        }
        catch ( Exception ex )
        {
            log.error( "Unable to predict " + predictor.getName(), ex );

            return conflict( "Unable to predict " + predictor.getName(), ex.getMessage() );
        }
    }

    @RequestMapping( value = "/run", method = { RequestMethod.POST, RequestMethod.PUT } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PREDICTOR_RUN')" )
    @ResponseBody
    public WebMessage runPredictors(
        @RequestParam Date startDate,
        @RequestParam Date endDate,
        TranslateParams translateParams )
    {
        int count = 0;

        List<Predictor> allPredictors = predictorService.getAllPredictors();

        for ( Predictor predictor : allPredictors )
        {
            try
            {
                PredictionSummary predictionSummary = new PredictionSummary();

                predictionService.predict( predictor, startDate, endDate, predictionSummary );

                count += predictionSummary.getPredictions();
            }
            catch ( Exception ex )
            {
                log.error( "Unable to predict " + predictor.getName(), ex );

                return conflict( "Unable to predict " + predictor.getName(), ex.getMessage() );
            }
        }

        return ok( "Generated " + count + " predictions" );
    }

    @PostMapping( value = "/expression/description", produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage getExpressionDescription( @RequestBody String expression )
    {
        ExpressionValidationOutcome result = expressionService.expressionIsValid( expression, PREDICTOR_EXPRESSION );

        return new DescriptiveWebMessage( result.isValid() ? Status.OK : Status.ERROR, HttpStatus.OK )
            .setDescription( result::isValid,
                () -> expressionService.getExpressionDescription( expression, PREDICTOR_EXPRESSION ) )
            .setMessage( i18nManager.getI18n().getString( result.getKey() ) );
    }

    @PostMapping( value = "/skipTest/description", produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage getSkipTestDescription( @RequestBody String expression )
    {
        ExpressionValidationOutcome result = expressionService.expressionIsValid( expression, PREDICTOR_SKIP_TEST );

        return new DescriptiveWebMessage( result.isValid() ? Status.OK : Status.ERROR, HttpStatus.OK )
            .setDescription( result::isValid,
                () -> expressionService.getExpressionDescription( expression, PREDICTOR_SKIP_TEST ) )
            .setMessage( i18nManager.getI18n().getString( result.getKey() ) );
    }
}
