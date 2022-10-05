/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.common;

import static org.hisp.dhis.analytics.QueryKey.NV;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class QueryFilter
{
    public static final String OPTION_SEP = ";";

    public static final ImmutableMap<QueryOperator, Function<Boolean, String>> OPERATOR_MAP = ImmutableMap
        .<QueryOperator, Function<Boolean, String>> builder()
        .put( QueryOperator.EQ, isValueNull -> isValueNull ? "is" : "=" )
        .put( QueryOperator.GT, unused -> ">" )
        .put( QueryOperator.GE, unused -> ">=" )
        .put( QueryOperator.LT, unused -> "<" )
        .put( QueryOperator.LE, unused -> "<=" )
        .put( QueryOperator.NE, isValueNull -> isValueNull ? "is not" : "!=" )
        .put( QueryOperator.LIKE, unused -> "like" )
        .put( QueryOperator.IN, unused -> "in" ).build();

    protected QueryOperator operator;

    protected String filter;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public QueryFilter()
    {
    }

    public QueryFilter( QueryOperator operator, String filter )
    {
        this.operator = operator;
        this.filter = filter;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isFilter()
    {
        return operator != null && filter != null && !filter.isEmpty();
    }

    public boolean isOperator( QueryOperator op )
    {
        return operator != null && operator.equals( op );
    }

    public String getSqlOperator()
    {
        return getSqlOperator( false );
    }

    /**
     *
     * @param isOperatorSubstitutionAllowed whether the operator should be
     *        replaced to support null values or not
     * @return the sql operator found in the operator map
     */
    public String getSqlOperator( boolean isOperatorSubstitutionAllowed )
    {
        if ( operator == null )
        {
            return null;
        }

        return safelyGetOperator( isOperatorSubstitutionAllowed );
    }

    private String safelyGetOperator( boolean isOperatorSubstitutionAllowed )
    {
        Function<Boolean, String> operatorFunction = OPERATOR_MAP.get( operator );
        if ( operatorFunction != null )
        {
            return operatorFunction
                .apply( StringUtils.trimToEmpty( filter ).equalsIgnoreCase( NV ) && isOperatorSubstitutionAllowed );
        }

        return null;
    }

    public String getSqlFilter( final String encodedFilter )
    {
        return getSqlFilter( encodedFilter, false );
    }

    public String getSqlFilter( String encodedFilter, boolean isNullValueSubstitutionAllowed )
    {
        if ( operator == null || encodedFilter == null )
        {
            return null;
        }

        if ( QueryOperator.LIKE.equals( operator ) )
        {
            return "'%" + encodedFilter + "%'";
        }
        else if ( QueryOperator.EQ.equals( operator ) || QueryOperator.NE.equals( operator ) )
        {
            if ( encodedFilter.equals( NV ) && isNullValueSubstitutionAllowed )
            {
                return "null";
            }
        }
        else if ( QueryOperator.IN.equals( operator ) )
        {
            return getFilterItems( encodedFilter ).stream()
                .map( this::quote )
                .collect( Collectors.joining( ",", "(", ")" ) );
        }
        return "'" + encodedFilter + "'";
    }

    protected String quote( String filterItem )
    {
        return "'" + filterItem + "'";
    }

    /**
     * Returns the items of the filter.
     *
     * @param encodedFilter the encoded filter.
     */
    public static List<String> getFilterItems( String encodedFilter )
    {
        return Lists.newArrayList( encodedFilter.split( OPTION_SEP ) );
    }

    /**
     * Returns a string representation of the query operator and filter.
     */
    public String getFilterAsString()
    {
        return operator.getValue() + " " + filter;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "[Operator: " + operator + ", filter: " + filter + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        QueryFilter other = (QueryFilter) object;

        if ( filter == null )
        {
            if ( other.filter != null )
            {
                return false;
            }
        }
        else if ( !filter.equals( other.filter ) )
        {
            return false;
        }

        if ( operator == null )
        {
            return other.operator == null;
        }
        else
            return operator.equals( other.operator );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public QueryOperator getOperator()
    {
        return operator;
    }

    public void setOperator( QueryOperator operator )
    {
        this.operator = operator;
    }

    public String getFilter()
    {
        return filter;
    }

    public void setFilter( String filter )
    {
        this.filter = filter;
    }
}
