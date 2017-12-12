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
public enum AnayticsEvaluationBoundaryType
{
    START_OF_REPORTING_PERIOD( "start_of_reporting_period" ),
    END_OF_REPORTING_PERIOD( "end_of_reporting_period" ),
    EARLIER_RELATIVE_TO_START_OF_REPORTING_PERIOD( "earlier_relative_to_start_of_reporting_period" ),
    LATER_RELATIVE_TO_START_OF_REPORTING_PERIOD( "later_relative_to_start_of_reporting_period" ),
    EARLIER_RELATIVE_TO_END_OF_REPORTING_PERIOD( "earlier_relative_to_end_of_reporting_period" ),
    LATER_RELATIVE_TO_END_OF_REPORTING_PERIOD( "later_relative_to_end_of_reporting_period" ),
    UNBOUNDED( "unbounded" );

    private final String value;

    AnayticsEvaluationBoundaryType( String value )
    {
        this.value = value;
    }

    public static AnayticsEvaluationBoundaryType fromValue( String value )
    {
        for ( AnayticsEvaluationBoundaryType type : AnayticsEvaluationBoundaryType.values() )
        {
            if ( type.value.equalsIgnoreCase( value ) )
            {
                return type;
            }
        }

        return null;
    }
}
