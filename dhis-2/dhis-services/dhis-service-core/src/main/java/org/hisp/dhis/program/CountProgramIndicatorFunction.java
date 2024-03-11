package org.hisp.dhis.program;

import java.util.Arrays;
import java.util.Date;

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
 * Function which counts the number of occurrences of a data value within an enrollment.
 * 
 * @author Markus Bekken
 */
public class CountProgramIndicatorFunction
    extends BaseCountIfProgramIndicatorFunction
{
 
    public static final String KEY = "count";
    
    @Override
    public String evaluate( ProgramIndicator programIndicator, StatementBuilder sb, Date reportingStartDate, Date reportingEndDate, String... args )
    {
        if ( args == null || args.length != 1 )
        {
            throw new IllegalArgumentException( "Illegal arguments, expected 1 arguments: source data element to count."
                + " Arguments passed: " + Arrays.toString( args ) );
        }
        
        String condition = " is not null";
        
        return this.countWhereCondition( programIndicator, sb, reportingStartDate, reportingEndDate, args[0], condition );
    }
}
