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

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expressionparser.ExpressionParserService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.util.Timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;

/**
 * @author Luciano Fiandesio
 */
public abstract class BaseStep
    implements
    Step
{

    private static final Log log = LogFactory.getLog( BaseStep.class );

    protected SystemSettingManager systemSettingManager;

    protected ExpressionParserService expressionParserService;

    protected QueryValidator queryValidator;

    protected QueryPlanner queryPlanner;

    protected AnalyticsManager analyticsManager;

    protected ConstantService constantService;

    private int maxQueries;

    @Override
    public void setContext( SystemSettingManager systemSettingManager, ExpressionParserService expressionParserService,
        QueryValidator queryValidator, QueryPlanner queryPlanner, AnalyticsManager analyticsManager,
        ConstantService constantService, int maxQueries )
    {
        this.systemSettingManager = systemSettingManager;
        this.expressionParserService = expressionParserService;
        this.queryValidator = queryValidator;
        this.queryPlanner = queryPlanner;
        this.analyticsManager = analyticsManager;
        this.constantService = constantService;

        this.maxQueries = maxQueries;
    }

    /**
     * Generates a mapping between a dimension key and the aggregated value. The
     * dimension key is a concatenation of the identifiers of the dimension items
     * separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @param tableType the {@link AnalyticsTableType}.
     * @param queryGroupers the list of additional query groupers to use for query
     *        planning, use empty list for none.
     * @return a mapping between a dimension key and aggregated values.
     */
    Map<String, Object> getAggregatedValueMap( DataQueryParams params, AnalyticsTableType tableType,
        List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers )
    {
        queryValidator.validateMaintenanceMode();

        int optimalQueries = MathUtils.getWithin( getProcessNo(), 1, maxQueries );

        int maxLimit = params.isIgnoreLimit() ? 0
            : (Integer) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAX_LIMIT );

        Timer timer = new Timer().start().disablePrint();

        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( optimalQueries )
            .withTableName( tableType.getTableName() ).withQueryGroupers( queryGroupers ).build();

        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );

        timer.getSplitTime(
            "Planned analytics query, got: " + queryGroups.getLargestGroupSize() + " for optimal: " + optimalQueries );

        Map<String, Object> map = new HashMap<>();

        for ( List<DataQueryParams> queries : queryGroups.getSequentialQueries() )
        {
            List<Future<Map<String, Object>>> futures = new ArrayList<>();

            for ( DataQueryParams query : queries )
            {
                futures.add( analyticsManager.getAggregatedDataValues( query, tableType, maxLimit ) );
            }

            for ( Future<Map<String, Object>> future : futures )
            {
                try
                {
                    Map<String, Object> taskValues = future.get();

                    if ( taskValues != null )
                    {
                        map.putAll( taskValues );
                    }
                }
                catch ( Exception ex )
                {
                    log.error( DebugUtils.getStackTrace( ex ) );
                    log.error( DebugUtils.getStackTrace( ex.getCause() ) );

                    if ( ex.getCause() != null && ex.getCause() instanceof RuntimeException )
                    {
                        throw (RuntimeException) ex.getCause(); // Throw the real exception instead of execution
                                                                // exception
                    }
                    else
                    {
                        throw new RuntimeException( "Error during execution of aggregation query task", ex );
                    }
                }
            }
        }

        timer.getTime( "Got analytics values" );

        return map;
    }

    /**
     * Gets the number of available cores. Uses explicit number from system setting
     * if available. Detects number of cores from current server runtime if not.
     *
     * @return the number of available cores.
     */
    private int getProcessNo()
    {
        Integer cores = (Integer) systemSettingManager.getSystemSetting( SettingKey.DATABASE_SERVER_CPUS );

        return (cores == null || cores == 0) ? SystemUtils.getCpuCores() : cores;
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping between a
     * dimension key and the aggregated value. The dimension key is a concatenation
     * of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    Map<String, Object> getAggregatedDataValueMapObjectTyped( DataQueryParams params )
    {
        return getAggregatedValueMap( params, AnalyticsTableType.DATA_VALUE, Lists.newArrayList() );
    }

    /**
     * Fill grid with aggregated data map with key and value
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid
     * @param aggregatedDataMap the aggregated data map
     */
    void fillGridWithAggregatedDataMap( DataQueryParams params, Grid grid, Map<String, Double> aggregatedDataMap )
    {
        for ( Map.Entry<String, Double> entry : aggregatedDataMap.entrySet() )
        {
            Double value = params.isSkipRounding() ? entry.getValue() : MathUtils.getRounded( entry.getValue() );

            grid.addRow().addValues( entry.getKey().split( DIMENSION_SEP ) ).addValue( value );

            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( 3 );
            }
        }
    }
}
