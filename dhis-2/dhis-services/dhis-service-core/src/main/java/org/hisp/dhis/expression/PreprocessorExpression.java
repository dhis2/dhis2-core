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

import java.util.List;
import java.util.regex.Pattern;

import lombok.Getter;

import org.hisp.dhis.antlr.ParserException;

/**
 * Parser for expression preprocessing.
 *
 * @author Jim Grace
 */
@Getter
public abstract class PreprocessorExpression
{
    /**
     * The entire expression including the preprocessor prefix.
     */
    protected final String expression;

    /**
     * The prefix to the main expression.
     */
    protected final String prefix;

    /**
     * The parts (separated by spaces) of the prefix.
     */
    protected final List<String> parts;

    /**
     * The main expression.
     */
    protected final String main;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile( "^\\?[A-Za-z][A-Za-z0-9]*$" );

    public static final Pattern UID_PATTERN = Pattern.compile( "^[A-Za-z][A-Za-z0-9]{10}$" );

    public static final String PREPROCESSOR_SEPARATOR = " --> ";

    public static final int PREPROCESSOR_SEPARATOR_LENGTH = PREPROCESSOR_SEPARATOR.length();

    /**
     * Accepts an expression with a preprocessor prefix. Parses it into the
     * prefix (also broken down into parts) and the main expression.
     *
     * @param expression the expression to parse for preprocessing.
     */
    protected PreprocessorExpression( String expression )
    {
        this.expression = expression;

        int mainSplit = expression.indexOf( PREPROCESSOR_SEPARATOR );

        if ( mainSplit < 0 )
        {
            throw new ParserException(
                format( "Couldn't find preprocessor termination '%s' in '%s'", PREPROCESSOR_SEPARATOR, expression ) );
        }

        prefix = expression.substring( 0, mainSplit );
        parts = List.of( prefix.split( " " ) );
        main = expression.substring( mainSplit + PREPROCESSOR_SEPARATOR_LENGTH );
    }

    /**
     * Validates that variable name starts with a '?', followed by a letter, and
     * optionally followed by more letters or numbers.
     *
     * @param variable the variable name to validate.
     * @param expression the full expression.
     */
    protected void validateVariable( String variable, String expression )
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

    /**
     * Validates a UID.
     *
     * @param uid the UID to validate.
     * @param expression the full expression.
     */
    protected void validateUid( String uid, String expression )
    {
        if ( !UID_PATTERN.matcher( uid ).matches() )
        {
            throw new ParserException( format(
                "UID '%s' must start with a letter and contain 10 more letters and numbers in '%s'", uid,
                expression ) );
        }
    }
}
