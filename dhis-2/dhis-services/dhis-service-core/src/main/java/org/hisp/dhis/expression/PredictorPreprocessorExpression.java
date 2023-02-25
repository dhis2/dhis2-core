/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.expression;

import static java.lang.String.format;

import lombok.Getter;

import org.hisp.dhis.antlr.ParserException;

/**
 * Parser for predictor expression preprocessing.
 *
 * @author Jim Grace
 */
@Getter
public class PredictorPreprocessorExpression
    extends PreprocessorExpression
{
    String variable;

    String taggedDegUid;

    String degUid;

    /**
     * Accepts and parses a predictor expression with a preprocessor prefix:
     * forEach ?variable in :DEG:degUid -->
     *
     * @param expression the expression to parse for preprocessing.
     */
    public PredictorPreprocessorExpression( String expression )
    {
        super( expression );

        if ( parts.size() != 4 )
        {
            throw new ParserException( format( "Predictor preprocessor expression should have four parts: '%s'",
                expression ) );
        }

        // forEach

        if ( !parts.get( 0 ).equals( "forEach" ) )
        {
            throw new ParserException( format( "Predictor preprocessor expression must start with forEach: '%s'",
                expression ) );
        }

        // variable

        variable = parts.get( 1 );
        validateVariable( variable, expression );

        // in

        if ( !parts.get( 2 ).equals( "in" ) )
        {
            throw new ParserException( format( "Keyword 'in' must be the third token in '%s'", expression ) );
        }

        // :DEG:deGroupUid

        taggedDegUid = parts.get( 3 );

        if ( !taggedDegUid.startsWith( ":DEG:" ) )
        {
            throw new ParserException( format( "Tag '%s' must start with ':DEG:' in '%s'", taggedDegUid, expression ) );
        }

        degUid = taggedDegUid.split( ":", 3 )[2];
        validateUid( degUid, expression );
    }
}
