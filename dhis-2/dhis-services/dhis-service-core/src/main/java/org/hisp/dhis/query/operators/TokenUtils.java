package org.hisp.dhis.query.operators;

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

import org.hibernate.criterion.MatchMode;
import org.hisp.dhis.query.Type;

import java.util.Arrays;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class TokenUtils
{
    public static List<String> getTokens( String value )
    {
        return Arrays.asList( value.replaceAll( "[^a-zA-Z0-9]", " " ).split( "[\\s@&.?$+-]+" ) );
    }

    public static StringBuilder createRegex( String value )
    {
        StringBuilder regex = new StringBuilder();

        TokenUtils.getTokens( value ).forEach( token -> regex.append( "(?=.*" ).append( token ).append( ")" ) );

        return regex;
    }

    public static boolean test( List<Object> args, Object testValue, String targetValue, boolean caseSensitive,
        MatchMode matchMode )
    {
        if ( args.isEmpty() || testValue == null )
        {
            return false;
        }

        Type type = new Type( testValue );

        if ( type.isString() )
        {
            String s2 = caseSensitive ? (String) testValue : ((String) testValue).toLowerCase();

            List<String> s1_tokens = getTokens( targetValue );
            List<String> s2_tokens = Arrays.asList( s2.replaceAll( "[^a-zA-Z0-9]", " " ).split( "[\\s@&.?$+-]+" ) );

            if ( s1_tokens.size() == 1 )
            {
                return s2.contains( targetValue );
            }
            else
            {
                for ( String s : s1_tokens )
                {
                    boolean found = false;
                    for ( String s3 : s2_tokens )
                    {
                        switch ( matchMode )
                        {
                        case EXACT:
                            found = s3.equals( s );
                            break;
                        case START:
                            found = s3.startsWith( s );
                            break;
                        case END:
                            found = s3.endsWith( s );
                            break;
                        case ANYWHERE:
                            found = s3.contains( s );
                        }

                        if ( found )
                        {
                            break;
                        }
                    }

                    if ( !found )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
