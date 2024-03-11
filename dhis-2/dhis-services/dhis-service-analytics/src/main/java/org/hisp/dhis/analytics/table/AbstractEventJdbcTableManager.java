package org.hisp.dhis.analytics.table;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.ConcurrentUtils;
import org.springframework.scheduling.annotation.Async;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * @author Markus Bekken
 */
public abstract class AbstractEventJdbcTableManager
    extends AbstractJdbcTableManager
{
    @Override
    @Async
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions,
        Collection<String> dataElements, int aggregationLevel )
    {
        return ConcurrentUtils.getImmediateFuture();
    }

    @Override
    @Async
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTablePartition> tables )
    {
        return ConcurrentUtils.getImmediateFuture();
    }

    /**
     * Returns the database column type based on the given value type. For boolean
     * values, 1 means true, 0 means false and null means no value.
     *
     * @param valueType the value type to represent as database column type.
     */
    protected String getColumnType( ValueType valueType )
    {
        if ( valueType.isDecimal() )
        {
            return statementBuilder.getDoubleColumnType();
        }
        else if ( valueType.isInteger() )
        {
            return "bigint";
        }
        else if ( valueType.isBoolean() )
        {
            return "integer";
        }
        else if ( valueType.isDate() )
        {
            return "timestamp";
        }
        else if ( valueType.isGeo() && databaseInfo.isSpatialSupport() )
        {
            return "geometry(Point, 4326)";
        }
        else
        {
            return "text";
        }
    }

    /**
     * Returns the select clause, potentially with a cast statement, based on the
     * given value type.
     *
     * @param valueType the value type to represent as database column type.
     */
    protected String getSelectClause( ValueType valueType )
    {
        if ( valueType.isDecimal() )
        {
            return "cast(value as " + statementBuilder.getDoubleColumnType() + ")";
        }
        else if ( valueType.isInteger() )
        {
            return "cast(value as bigint)";
        }
        else if ( valueType.isBoolean() )
        {
            return "case when value = 'true' then 1 when value = 'false' then 0 else null end";
        }
        else if ( valueType.isDate() )
        {
            return "cast(value as timestamp)";
        }
        else if ( valueType.isGeo() && databaseInfo.isSpatialSupport() )
        {
            return "ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || value || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}')";
        }
        else if ( valueType.isOrganisationUnit() )
        {
            // org unit is persisted like: /abcd/efgh/djkj so we only extract the last segment using substring
            return "ou.name from organisationunit ou where ou.uid = (select substring(value, '[^/]*$') " ;
        }
        else
        {
            return "value";
        }
    }

    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "select dataelementid from trackedentitydatavalue limit 1" ).next();

        if ( !hasData )
        {
            return "No events exist, not updating event analytics tables";
        }

        return null;
    }

    protected String addClosingParentheses( String selectSql )
    {
        int open = 0;

        for ( int i = 0; i < selectSql.length(); i++ )
        {
            if ( selectSql.charAt( i ) == '(' )
            {
                open++;
            }
            else if ( selectSql.charAt( i ) == ')' )
            {
                open--;
            }

        }
        return StringUtils.repeat(")", open);
    }
}
