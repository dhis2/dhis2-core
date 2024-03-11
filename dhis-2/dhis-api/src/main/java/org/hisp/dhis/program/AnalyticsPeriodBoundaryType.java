package org.hisp.dhis.program;

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
 * @author Markus Bekken
 */

public enum AnalyticsPeriodBoundaryType
{
    BEFORE_START_OF_REPORTING_PERIOD( "before_start_of_reporting_period" ), 
    BEFORE_END_OF_REPORTING_PERIOD( "before_end_of_reporting_period" ),
    AFTER_START_OF_REPORTING_PERIOD( "after_start_of_reporting_period" ), 
    AFTER_END_OF_REPORTING_PERIOD( "after_end_of_reporting_period" );
    
    private final String value;

    private AnalyticsPeriodBoundaryType( String value )
    {
        this.value = value;
    }

    public static AnalyticsPeriodBoundaryType fromValue( String value )
    {
        for ( AnalyticsPeriodBoundaryType analyticsPeriodBoundaryType : AnalyticsPeriodBoundaryType.values() )
        {
            if ( analyticsPeriodBoundaryType.getValue().equalsIgnoreCase( value ) )
            {
                return analyticsPeriodBoundaryType;
            }
        }

        return null;
    }
    
    public String getValue()
    {
        return value;
    }
    
    public Boolean isEndBoundary()
    {
        return this == BEFORE_END_OF_REPORTING_PERIOD || this == BEFORE_START_OF_REPORTING_PERIOD;
    }
    
    public Boolean isStartBoundary()
    {
        return this == AFTER_END_OF_REPORTING_PERIOD || this == AFTER_START_OF_REPORTING_PERIOD;
    }
}
