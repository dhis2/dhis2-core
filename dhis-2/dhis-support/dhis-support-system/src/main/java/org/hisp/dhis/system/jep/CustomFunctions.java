package org.hisp.dhis.system.jep;

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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.hisp.dhis.system.jep.ArithmeticMean;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommandI;

import com.google.common.collect.ImmutableMap;

/**
 * @author Kenneth Haase
 */
public class CustomFunctions
{
    public static final Map<String, PostfixMathCommandI> AGGREGATE_FUNCTIONS = 
        ImmutableMap.<String, PostfixMathCommandI>builder().
        put( "AVG", new ArithmeticMean() ).put( "STDDEV", new StandardDeviation() ).
        put( "MEDIAN", new MedianValue() ).put( "MAX", new MaxValue() ).
        put( "MIN", new MinValue() ).put( "COUNT", new Count() ).
        put( "VSUM", new VectorSum() ).build();
    
    public static final Pattern AGGREGATE_PATTERN_PREFIX = Pattern.compile( "(AVG|STDDEV|MEDIAN|MAX|MIN|COUNT|VSUM)\\s*\\(" );

    public static void addFunctions( JEP parser )
    {        
        for ( Entry<String, PostfixMathCommandI> e : AGGREGATE_FUNCTIONS.entrySet() )
        {
            String fname = e.getKey();
            PostfixMathCommandI cmd = e.getValue();
            parser.addFunction( fname, cmd );
        }
    }

    @SuppressWarnings( "unchecked" )
    public static List<Double> checkVector( Object param )
        throws ParseException
    {
        if ( param instanceof List )
        {
            List<?> vals = (List<?>) param;
            
            for ( Object val : vals )
            {
                if ( !(val instanceof Double) )
                {
                    throw new ParseException( "Non numeric vector" );
                }
            }
            
            return (List<Double>) param;
        }
        else
        {
            throw new ParseException( "Invalid vector argument" );
        }
    }
}
