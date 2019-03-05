/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.data.pipeline;

import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expressionparser.ExpressionParserService;
import org.hisp.dhis.setting.SystemSettingManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A processing pipeline for the Analytics service.
 * It allows to execute multiple Analytics related steps in sequence
 *
 * @author Luciano Fiandesio
 */
public class AnalyticsPipeline
{
    private SystemSettingManager systemSettingManager;

    private ExpressionParserService expressionParserService;

    private QueryValidator queryValidator;

    private QueryPlanner queryPlanner;

    private AnalyticsManager analyticsManager;

    private ConstantService constantService;

    private int maxQueries;

    private DataQueryParams params;

    private Grid grid;

    private List<Step> steps;

    private boolean init = false;

    /**
     * Set all the common dependencies required by the Analytics Pipeline steps
     */
    public AnalyticsPipeline( SystemSettingManager systemSettingManager,
        ExpressionParserService expressionParserService, QueryValidator queryValidator, QueryPlanner queryPlanner,
        AnalyticsManager analyticsManager, ConstantService constantService, int maxQueries )
    {
        this.systemSettingManager = systemSettingManager;
        this.expressionParserService = expressionParserService;
        this.queryValidator = queryValidator;
        this.queryPlanner = queryPlanner;
        this.analyticsManager = analyticsManager;
        this.constantService = constantService;
        this.maxQueries = maxQueries;
        steps = new ArrayList<>();
    }

    /**
     * Initialize the pipeline by passing the objects used by all the steps
     * 
     * @param params a {@link DataQueryParams} object
     * @param grid an empty {@link Grid} object
     * @return AnalyticsPipeline for fluent interface
     */
    public AnalyticsPipeline init( DataQueryParams params, Grid grid )
    {
        if ( this.params != null || this.grid != null )
        {
            throw new IllegalStateException( "'init' can only be invoked once" );
        }
        this.params = params;
        this.grid = grid;

        init = true;
        return this;
    }

    public DataQueryParams getParams()
    {
        return params;
    }

    public Grid getGrid()
    {
        return grid;
    }

    /**
     * Add a {@link Step} to the processing pipeline
     * 
     * @param step a class implementing the a {@link Step} interface
     * @return AnalyticsPipeline for fluent interface
     */
    public AnalyticsPipeline add( Step step )
    {
        if ( !init )
        {
            throw new IllegalStateException( "Call init first" );
        }
        steps.add( step );
        return this;
    }

    /**
     * Process the steps in the pipeline
     * 
     */
    public void process()
    {
        for ( Step step : steps )
        {
            step.setContext( this.systemSettingManager, this.expressionParserService, this.queryValidator,
                this.queryPlanner, this.analyticsManager, this.constantService, this.maxQueries );
            step.execute( this.params, grid );
        }
    }
}
