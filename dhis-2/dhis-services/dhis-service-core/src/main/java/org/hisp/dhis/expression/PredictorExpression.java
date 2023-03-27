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
import static org.hisp.dhis.common.CodeGenerator.isValidUid;

import java.util.regex.Pattern;

import lombok.Getter;

import org.hisp.dhis.antlr.ParserException;

/**
 * Parser for predictor expressions, including preprocessing.
 *
 * @author Jim Grace
 */
@Getter
public class PredictorExpression
{
    /**
     * Whether the expression is simple (does not need preprocessing).
     */
    final boolean simple;

    /**
     * The entire expression, including the preprocessor prefix if any.
     */
    final String expression;

    /**
     * The prefix to the main expression if any.
     */
    final String prefix;

    /**
     * The main expression (without preprocessor prefix if any).
     */
    final String main;

    /**
     * The variable name for data elements in a data element group.
     */
    final String variable;

    /**
     * The tagged data element group UID.
     */
    final String taggedDegUid;

    /**
     * The untagged data element group UID.
     */
    final String degUid;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile( "^\\?[A-Za-z][A-Za-z0-9]*$" );

    private static final String FOR_EACH = "forEach";

    private static final String IN = "in";

    private static final String PREPROCESSOR_SEPARATOR = " --> ";

    private static final int PREPROCESSOR_SEPARATOR_LENGTH = PREPROCESSOR_SEPARATOR.length();

    /**
     * Accepts and parses a predictor expression with a preprocessor prefix:
     * forEach ?variable in :DEG:degUid -->
     *
     * @param expression the expression to parse for preprocessing.
     */
    public PredictorExpression( String expression )
    {
        this.expression = expression;

        if ( !expression.startsWith( FOR_EACH ) )
        {
            simple = true;
            main = expression;
            prefix = null;
            variable = null;
            taggedDegUid = null;
            degUid = null;
            return;
        }

        simple = false;

        int mainSplit = expression.indexOf( PREPROCESSOR_SEPARATOR );

        if ( mainSplit < 0 )
        {
            throw new ParserException(
                format( "Couldn't find preprocessor termination '%s' in '%s'", PREPROCESSOR_SEPARATOR, expression ) );
        }

        prefix = this.expression.substring( 0, mainSplit );
        main = this.expression.substring( mainSplit + PREPROCESSOR_SEPARATOR_LENGTH );

        String[] parts = prefix.split( " " );

        if ( parts.length != 4 )
        {
            throw new ParserException( format( "Predictor preprocessor expression should have four parts: '%s'",
                expression ) );
        }

        // forEach

        if ( !parts[0].equals( FOR_EACH ) )
        {
            throw new ParserException( format( "Predictor preprocessor expression must start with forEach: '%s'",
                expression ) );
        }

        // variable

        variable = parts[1];
        validateVariable( variable, expression );

        // in

        if ( !parts[2].equals( IN ) )
        {
            throw new ParserException( format( "Keyword 'in' must be the third token in '%s'", expression ) );
        }

        // :DEG:deGroupUid

        taggedDegUid = parts[3];
        if ( !taggedDegUid.startsWith( ":DEG:" ) )
        {
            throw new ParserException( format( "Tag '%s' must start with ':DEG:' in '%s'", taggedDegUid, expression ) );
        }

        degUid = taggedDegUid.split( ":", 3 )[2];
        if ( !isValidUid( degUid ) )
        {
            throw new ParserException( format(
                "UID '%s' must start with a letter and contain 10 more letters and numbers in '%s'", degUid,
                expression ) );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Validates that variable name starts with a '?', followed by a letter, and
     * optionally followed by more letters or numbers.
     *
     * @param variable the variable name to validate.
     * @param expression the full expression.
     */
    private void validateVariable( String variable, String expression )
    {
        if ( !VARIABLE_PATTERN.matcher( variable ).matches() )
        {
            if ( !variable.startsWith( "?" ) )
            {
                throw new ParserException(
                    format( "Variable '%s' must start with '?' in '%s'", variable, expression ) );
            }

            throw new ParserException( format(
                "Variable '%s' must start with a letter and contain only letters and numbers in '%s'", variable,
                expression ) );
        }
    }
}
