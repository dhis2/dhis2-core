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
package org.hisp.dhis.predictor;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;

/**
 * @author Jim Grace
 */
public interface PredictionService
{
    /**
     * Executes a predictor job run
     *
     * @param predictorJobParameters parameters for the predictor job run
     * @param progress track processing progress
     * @return a summary of what was predicted
     */
    PredictionSummary predictJob( PredictorJobParameters predictorJobParameters, JobProgress progress );

    /**
     * Executes predictors and/or predictor groups for a date range in a job
     *
     * @param startDate the start date of the predictor run
     * @param endDate the end date of the predictor run
     * @param predictors predictor(s) to run
     * @param predictorGroups predictor group(s) to run
     * @param progress track processing progress
     * @return a summary of what was predicted
     */
    PredictionSummary predictTask( Date startDate, Date endDate,
        List<String> predictors, List<String> predictorGroups, JobProgress progress );

    /**
     * Executes predictors and/or predictor groups for a date range
     *
     * @param startDate the start date of the predictor run
     * @param endDate the end date of the predictor run
     * @param predictors predictor(s) to run
     * @param predictorGroups predictor group(s) to run
     * @param progress track processing progress
     * @return a summary of what was predicted
     */
    PredictionSummary predictAll( Date startDate, Date endDate,
        List<String> predictors, List<String> predictorGroups, JobProgress progress );

    /**
     * Executes a single predictor for a date range
     *
     * @param predictor the predictor to run
     * @param startDate the start date of the predictor run
     * @param endDate the end date of the predictor run
     * @param predictionSummary the prediction summary to add to
     */
    void predict( Predictor predictor, Date startDate, Date endDate, PredictionSummary predictionSummary );

    /**
     * Executes a simple predictor (no preprocessor) for a date range
     *
     * @param predictor the predictor to run
     * @param startDate the start date of the predictor run
     * @param endDate the end date of the predictor run
     * @param predictionSummary the prediction summary to add to
     */
    void predictSimple( Predictor predictor, Date startDate, Date endDate, PredictionSummary predictionSummary );

    /**
     * Tests whether a predictor expression is valid.
     *
     * @param expression the predictor expression string.
     * @return the ExpressionValidationOutcome of the validation.
     */
    ExpressionValidationOutcome expressionIsValid( String expression );

    /**
     * Creates a predictor expression description containing the names of the
     * DimensionalItemObjects.
     *
     * @param expression the expression string.
     * @return a description containing DimensionalItemObjects names.
     */
    String getExpressionDescription( String expression );
}
