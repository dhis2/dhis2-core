/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.commons.jackson.config.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Morten Olav Hansen
 */
public class FieldFilterParser
{
    public static Set<String> parse( Set<String> fields )
    {
        return new HashSet<>( expandField( StringUtils.join( fields, "," ) ) );
    }

    private static Set<String> expandField( String field )
    {
        Set<String> output = new HashSet<>();

        Stack<String> path = new Stack<>();
        StringBuilder builder = new StringBuilder();

        for ( String token : field.split( "" ) )
        {
            if ( isFieldSeparator( token ) )
            {
                output.add( toFullPath( builder.toString(), path ) );
                builder = new StringBuilder();
            }
            else if ( isBlockStart( token ) )
            {
                path.push( builder.toString() );
                builder = new StringBuilder();
            }
            else if ( isBlockEnd( token ) )
            {
                output.add( toFullPath( builder.toString(), path ) );
                output.add( path.pop() );

                builder = new StringBuilder();
            }
            else if ( isAlphanumeric( token ) )
            {
                builder.append( token );
            }
        }

        if ( builder.length() > 0 )
        {
            output.add( builder.toString() );
        }

        return output;
    }

    private static boolean isBlockStart( String token )
    {
        return token != null && StringUtils.containsAny( token, "[", "(" );
    }

    private static boolean isBlockEnd( String token )
    {
        return token != null && StringUtils.containsAny( token, "]", ")" );
    }

    private static boolean isFieldSeparator( String token )
    {
        return token != null && StringUtils.containsAny( token, "," );
    }

    private static boolean isAlphanumeric( String token )
    {
        return StringUtils.isAlphanumeric( token ) || StringUtils.containsAny( token, "*" );
    }

    private static String toFullPath( String field, Stack<String> path )
    {
        return path.size() == 0 ? field : StringUtils.join( path, "." ) + "." + field;
    }

    private FieldFilterParser()
    {
    }
}
