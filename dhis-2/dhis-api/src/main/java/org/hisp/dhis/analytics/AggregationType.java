package org.hisp.dhis.analytics;

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

/**
 * @author Lars Helge Overland
 */
public enum AggregationType
{
    SUM( "sum", false, true ),
    AVERAGE( "avg", false, true ),
    AVERAGE_SUM_ORG_UNIT( "avg_sum_org_unit", false, true ),
    COUNT( "count", false, true ),
    STDDEV( "stddev", false, true ),
    VARIANCE( "variance", false, true ),
    MIN( "min", false, true ),
    MAX( "max", false, true ),
    NONE( "none", false, true ), // Text only
    CUSTOM( "custom", false, false ),
    DEFAULT( "default", true, false ),
    AVERAGE_SUM_INT( "avg_sum_int", true, true ), // Sum in organisation unit hierarchy
    AVERAGE_SUM_INT_DISAGGREGATION( "avg_sum_int_disaggregation", true, true ), // Sum in organisation unit hierarchy
    AVERAGE_INT( "avg_int", true, true ),
    AVERAGE_INT_DISAGGREGATION( "avg_int_disaggregation", true, true ),
    AVERAGE_BOOL( "avg_bool", true, true );

    private final String value;
    
    private boolean internal;

    private boolean aggregateable;
    
    AggregationType( String value )
    {
        this.value = value;
    }

    AggregationType( String value, boolean internal, boolean aggregateable )
    {
        this.value = value;
        this.internal = internal;
        this.aggregateable = aggregateable;
    }

    public String getValue()
    {
        return value;
    }

    public boolean isAverage()
    {
        return this == AVERAGE_SUM_ORG_UNIT || this == AVERAGE;
    }

    public boolean isInternal()
    {
        return internal;
    }
    
    public boolean isAggregateable()
    {
        return aggregateable;
    }

    public static AggregationType fromValue( String value )
    {
        for ( AggregationType type : AggregationType.values() )
        {
            if ( type.value.equalsIgnoreCase( value ) )
            {
                return type;
            }
        }

        return null;
    }
}
