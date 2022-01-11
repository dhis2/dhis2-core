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
package org.hisp.dhis.webapi.utils;

import java.util.List;

import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.analyze.ExecutionPlanCache;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.PerformanceMetrics;

/**
 * @author Dusan Bernat
 */
public class PerformanceMetricsUtils
{
    public static void addPerformanceMetrics( final ExecutionPlanCache executionPlanCache, final DataQueryParams params,
        final Grid grid )
    {
        if ( params.analyzeOnly() )
        {
            String key = params.getAnalyzeOrderId();

            List<ExecutionPlan> plans = executionPlanCache.getExecutionPlans( key );

            PerformanceMetrics performanceMetrics = new PerformanceMetrics();

            double total = plans.stream().map( ExecutionPlan::getTimeEstimation ).reduce( 0.0, Double::sum );

            performanceMetrics.setTotalTimeEstimation( Precision.round( total, 3 ) );

            performanceMetrics.setExecutionPlans( plans );

            grid.setPerformanceMetrics( performanceMetrics );

            executionPlanCache.removeExecutionPlans( key );
        }
    }
}
