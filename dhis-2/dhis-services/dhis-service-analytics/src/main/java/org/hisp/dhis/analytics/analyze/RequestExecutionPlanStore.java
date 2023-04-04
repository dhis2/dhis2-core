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
package org.hisp.dhis.analytics.analyze;

import static org.hisp.dhis.analytics.analyze.RequestExecutionPlanStore.Execution.EXECUTION_TIME;
import static org.hisp.dhis.analytics.analyze.RequestExecutionPlanStore.Execution.EXPLAIN_QUERY;
import static org.hisp.dhis.analytics.analyze.RequestExecutionPlanStore.Execution.PLAN;
import static org.hisp.dhis.analytics.analyze.RequestExecutionPlanStore.Execution.PLANNING_TIME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.common.ExecutionPlan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dusan Bernat
 */
@Slf4j
@Service
public class RequestExecutionPlanStore implements ExecutionPlanStore
{
    private final Map<String, List<ExecutionPlan>> executionPlanMap = new HashMap<>();

    @Nonnull
    private final JdbcTemplate jdbcTemplate;

    @Nonnull
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Nonnull
    private final ScheduledExecutorService executorService;

    public RequestExecutionPlanStore( @Qualifier( "executionPlanJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
        this.executorService = Executors.newScheduledThreadPool( 10 );
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    @Override
    public void addExecutionPlan( String key, String sql )
    {
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( EXPLAIN_QUERY.value() + sql );

        JsonNode root = getJsonFromRowSet( rowSet );

        ExecutionPlan executionPlan = getExecutionPlan( sql, root );

        storeExecutionPlan( key, executionPlan );
    }

    @Override
    public List<ExecutionPlan> getExecutionPlans( String key )
    {
        if ( executionPlanMap.containsKey( key ) )
        {
            return executionPlanMap.get( key );
        }

        return new ArrayList<>();
    }

    @Override
    public void removeExecutionPlans( String key )
    {
        executorService.schedule( () -> executionPlanMap.remove( key ), 2, TimeUnit.SECONDS );
    }

    /**
     * Creates an {@link ExecutionPlan} object based on the given input
     * parameters.
     *
     * @param sql the SQL statement of the plan.
     * @param jsonNode the root {@JsonNode} of the execution plan.
     * @return the {@link ExecutionPlan}.
     */
    private ExecutionPlan getExecutionPlan( String sql, JsonNode jsonNode )
    {
        ExecutionPlan executionPlan = new ExecutionPlan();
        executionPlan.setQuery( sql );

        if ( jsonNode != null && jsonNode.get( 0 ) != null && jsonNode.get( 0 ).get( PLAN.value() ) != null )
        {
            JsonNode plan = jsonNode.get( 0 ).get( PLAN.value() );
            executionPlan.setPlan( plan );

            double execTime = jsonNode.get( 0 ).get( EXECUTION_TIME.value() ) != null
                ? jsonNode.get( 0 ).get( EXECUTION_TIME.value() ).asDouble()
                : 0.0;

            double planTime = jsonNode.get( 0 ).get( PLANNING_TIME.value() ) != null
                ? jsonNode.get( 0 ).get( PLANNING_TIME.value() ).asDouble()
                : 0.0;

            executionPlan.setExecutionTime( execTime );
            executionPlan.setPlanningTime( planTime );
            executionPlan.setTimeInMillis( Precision.round( execTime + planTime, 3 ) );
        }

        return executionPlan;
    }

    /**
     * It stores, in the current execution plan map, the given
     * {@link ExecutionPlan} object for the associated "key".
     *
     * @param key the unique key associated with the given
     *        {@link ExecutionPlan}.
     * @param executionPlan the {@link ExecutionPlan}.
     */
    private synchronized void storeExecutionPlan( String key, ExecutionPlan executionPlan )
    {
        if ( executionPlanMap.containsKey( key ) )
        {
            List<ExecutionPlan> oldList = executionPlanMap.get( key );

            List<ExecutionPlan> newList = new ArrayList<>( oldList );
            newList.add( executionPlan );

            executionPlanMap.replace( key, newList );
        }
        else
        {
            executionPlanMap.put( key, List.of( executionPlan ) );
        }
    }

    /**
     * This method extracts the root {@link JsonNode} object from the given
     * {@link SqlRowSet}. If something goes wrong it returns null the exception
     * carried on by the JSON object, if any.
     *
     * @param rowSet the {@link SqlRowSet}.
     * @return the root {@link JsonNode} object.
     */
    private JsonNode getJsonFromRowSet( SqlRowSet rowSet )
    {
        ObjectMapper objectMapper = new ObjectMapper();

        try
        {
            if ( rowSet.next() )
            {
                String json = rowSet.getString( 1 );

                return objectMapper.readTree( json );
            }
        }
        catch ( Exception e )
        {
            try
            {
                return objectMapper.readTree( "{ \"error\": " + "\"" + e.getMessage() + "\"}" );
            }
            catch ( JsonProcessingException ex )
            {
                log.error( ex.getMessage(), ex );
            }
        }

        return null;
    }

    enum Execution
    {
        EXECUTION_TIME( "Execution Time" ),
        PLANNING_TIME( "Planning Time" ),
        EXPLAIN_QUERY( "EXPLAIN (ANALYZE true, COSTS true, FORMAT json) " ),
        PLAN( "Plan" );

        private String value;

        Execution( String value )
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }
}
