package org.hisp.dhis.antlr.operator;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.antlr.InternalParserException;

import java.util.List;

import static org.hisp.dhis.antlr.AntlrParserUtils.castBoolean;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;

/**
 * Abstract class for compare operators
 *
 * @author Jim Grace
 */
public abstract class AntlrOperatorCompare
    extends AntlrComputeFunction
{
    /**
     * Compares two Doubles, Strings or Booleans.
     *
     * @param values the values to compare
     * @return the results of the comparision.
     */
    protected int compare( List<Object> values )
    {
        Object o1 = values.get( 0 );
        Object o2 = values.get( 1 );

        if ( o1 == null || o2 == null )
        {
            throw new InternalParserException( "found null when comparing '" + o1 + "' with '" + o2 + "'" );
        }
        else if ( o1 instanceof Double )
        {
            return ((Double) o1).compareTo( castDouble( o2 ) );
        }
        else if ( o2 instanceof Double )
        {
            return ((Double) o2).compareTo( castDouble( o1 ) );
        }
        else if ( o1 instanceof String )
        {
            return ((String) o1).compareTo( castString( o2 ) );
        }
        else if ( o1 instanceof Boolean )
        {
            return ((Boolean) o1).compareTo( castBoolean( o2 ) );
        }
        else
        {
            throw new InternalParserException( "trying to compare class " + o1.getClass().getName() );
        }
    }
}
