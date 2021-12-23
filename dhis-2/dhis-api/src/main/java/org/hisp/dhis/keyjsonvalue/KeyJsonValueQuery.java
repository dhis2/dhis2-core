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
package org.hisp.dhis.keyjsonvalue;

import static java.lang.Character.isLetterOrDigit;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import org.hisp.dhis.common.NamedParams;

/**
 * Details of a query as it can be performed to fetch {@link KeyJsonValue}s.
 *
 * @author Jan Bernitt
 */
@ToString
@Getter
@Builder( toBuilder = true )
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public final class KeyJsonValueQuery
{

    private final String namespace;

    /**
     * By default, only entries which have at least one non-null value for the
     * extracted fields are returned. If all are included even matches with only
     * null values are included in the result list.
     */
    private final boolean includeAll;

    @Builder.Default
    private final List<Field> fields = emptyList();

    @ToString
    @Getter
    public static final class Field
    {
        private final String path;

        private final String alias;

        public Field( String path )
        {
            this( path, null );
        }

        public Field( String path, String alias )
        {
            if ( path == null || !path.matches( "^[-_a-zA-Z0-9]{1,32}(?:\\.[-_a-zA-Z0-9]{1,32}){0,5}$" ) )
            {
                throw new IllegalArgumentException( "Not a valid path: " + path );
            }
            this.path = path;
            this.alias = alias != null ? alias : path;
        }

        public String getPathSegments()
        {
            return Arrays.stream( path.split( "\\." ) )
                .map( name -> "'" + name + "'" )
                .collect( Collectors.joining( ", " ) );
        }
    }

    public KeyJsonValueQuery with( NamedParams params )
    {
        String fieldsParam = params.getString( "fields", null );
        String namespaceParam = params.getString( "namespace", null );
        KeyJsonValueQueryBuilder builder = toBuilder();
        if ( fieldsParam != null )
        {
            builder = builder.fields( parseFields( fieldsParam ) );
        }
        if ( namespaceParam != null )
        {
            builder = builder.namespace( namespaceParam );
        }
        return builder.build();
    }

    public static List<Field> parseFields( String fields )
    {
        final List<Field> flat = new ArrayList<>();
        final int len = fields.length();
        String parentPath = "";
        int start = 0;
        while ( start < len )
        {
            int end = findNameEnd( fields, start );
            String field = fields.substring( start, end );
            start = end + 1;
            if ( end >= len )
            {
                addNonEmptyTo( flat, parentPath, field );
                return flat;
            }
            char next = fields.charAt( end );
            if ( next == ',' )
            {
                addNonEmptyTo( flat, parentPath, field );
            }
            else if ( next == '[' )
            {
                parentPath += field + ".";
            }
            else if ( next == ']' )
            {
                addNonEmptyTo( flat, parentPath, field );
                parentPath = parentPath.substring( 0, parentPath.lastIndexOf( '.', parentPath.length() - 2 ) + 1 );
            }
            else
            {
                throw new IllegalArgumentException(
                    String.format( "Illegal fields expression. Expected `,`, `[` or `]` at position %d but found `%s`",
                        end, next ) );
            }
        }
        return flat;
    }

    private static void addNonEmptyTo( List<Field> fields, String parent, String field )
    {
        if ( !field.isEmpty() )
        {
            int aliasStart = field.indexOf( '(' );
            String name = aliasStart > 0 ? field.substring( 0, aliasStart ) : field;
            String alias = aliasStart > 0 ? field.substring( aliasStart + 1, field.length() - 1 ) : null;
            fields.add( new Field( parent + name, alias ) );
        }
    }

    private static int findNameEnd( String fields, int start )
    {
        int pos = start;
        while ( pos < fields.length() && isNameCharacter( fields.charAt( pos ) ) )
        {
            pos++;
        }
        return findAliasEnd( fields, pos );
    }

    private static int findAliasEnd( String fields, int start )
    {
        if ( start >= fields.length() || fields.charAt( start ) != '(' )
        {
            return start;
        }
        return fields.indexOf( ')', start ) + 1;
    }

    private static boolean isNameCharacter( char c )
    {
        return isLetterOrDigit( c ) || c == '.';
    }
}
