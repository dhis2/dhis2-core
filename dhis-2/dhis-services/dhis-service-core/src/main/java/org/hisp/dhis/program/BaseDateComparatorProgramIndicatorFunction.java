package org.hisp.dhis.program;

import java.util.Date;
import java.util.regex.Matcher;

import org.hisp.dhis.jdbc.StatementBuilder;

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

/**
 * Function which evaluates a relation between two given dates.
 *
 * @author Markus Bekken
 */
public abstract class BaseDateComparatorProgramIndicatorFunction
    implements ProgramIndicatorFunction
{
    protected abstract String compare( String startDate, String endDate );

    @Override
    public String evaluate( ProgramIndicator programIndicator, StatementBuilder statementBuilder, Date reportingStartDate, Date reportingEndDate, String... args )
    {
        if ( args == null || args.length != 2 )
        {
            throw new IllegalArgumentException( "Illegal arguments, expected 2 arguments: start-date, end-date" );
        }

        for ( int i = 0; i < args.length; i++ )
        {
            String arg = args[i].replaceAll( "^\"|^'|\"$|'$", "" ).trim();
            
            Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_PROGRAM_STAGE_PATTERN.matcher( arg );
            if ( matcher.find() ) 
            {
                String programStageUid = matcher.group( AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP );
                args[i] =  statementBuilder.getProgramIndicatorEventColumnSql( programStageUid, "executiondate", reportingStartDate, reportingEndDate, programIndicator );
            }
        }
        
        String startDate = args[0];
        String endDate = args[1];

        return compare( startDate, endDate );
    }

    public String getSampleValue()
    {
        return "1";
    }
}
