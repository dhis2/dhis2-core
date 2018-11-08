package org.hisp.dhis.program;

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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.jdbc.StatementBuilder;

/**
 * @author Markus Bekken
 */
public abstract class BaseCountIfProgramIndicatorFunction
    implements ProgramIndicatorFunction
{
    public static final String KEY = "countIf";

    public static final String PROGRAM_STAGE_REGEX_GROUP = "p";
    public static final String DATA_ELEMENT_REGEX_GROUP = "de";
    public static final String COHORT_HAVING_DATA_ELEMENT_REGEX = "#\\{(?<" + PROGRAM_STAGE_REGEX_GROUP + ">\\w{11}).(?<"+ DATA_ELEMENT_REGEX_GROUP + ">\\w{11})\\}"; 
    public static final Pattern COHORT_HAVING_DATA_ELEMENT_PATTERN = Pattern.compile( COHORT_HAVING_DATA_ELEMENT_REGEX );

    public String countWhereCondition( ProgramIndicator programIndicator, StatementBuilder sb, Date reportingStartDate, Date reportingEndDate, String element, String condition )
    {   
        Matcher matcher = COHORT_HAVING_DATA_ELEMENT_PATTERN.matcher( element );
        
        if ( matcher.find() )
        {
            String ps = matcher.group( PROGRAM_STAGE_REGEX_GROUP );
            String de = matcher.group( DATA_ELEMENT_REGEX_GROUP );
            
            String eventTableName = "analytics_event_" + programIndicator.getProgram().getUid();
            String columnName = "\"" + de + "\"";
            return "(select count(" + columnName + ") from " + eventTableName + " where " + eventTableName +
                ".pi = enrollmenttable.pi and " + columnName + " is not null " +
                " and " + columnName + condition + " " +
                (programIndicator.getEndEventBoundary() != null ? ("and " + 
                    sb.getBoundaryCondition( programIndicator.getEndEventBoundary(), programIndicator, reportingStartDate, reportingEndDate ) + 
                " ") : "") + (programIndicator.getStartEventBoundary() != null ? ("and " + 
                    sb.getBoundaryCondition( programIndicator.getStartEventBoundary(), programIndicator, reportingStartDate, reportingEndDate ) +
                " ") : "") + "and ps = '" + ps + "')";
        }
        else
        {
            throw new IllegalArgumentException( "No data element found in argument 1:" + element + " in " + BaseCountIfProgramIndicatorFunction.KEY 
                + " for program indciator:" + programIndicator.getUid() );
        }
    }
}
