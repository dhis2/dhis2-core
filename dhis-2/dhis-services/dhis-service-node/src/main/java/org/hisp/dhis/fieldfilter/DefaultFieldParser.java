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
package org.hisp.dhis.fieldfilter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component( "org.hisp.dhis.fieldfilter.FieldParser" )
public class DefaultFieldParser implements FieldParser
{
    @Override
    public FieldMap parse( String fields )
    {
        List<String> prefixList = Lists.newArrayList();
        FieldMap fieldMap = new FieldMap();

        StringBuilder builder = new StringBuilder();

        String[] fieldSplit = fields.split( "" );

        for ( int i = 0; i < fieldSplit.length; i++ )
        {
            String c = fieldSplit[i];

            // if we reach a field transformer, parse it out here (necessary to
            // allow for () to be used to handle transformer parameters)
            if ( (c.equals( ":" ) && fieldSplit[i + 1].equals( ":" )) || c.equals( "~" ) || c.equals( "|" ) )
            {
                boolean insideParameters = false;

                for ( ; i < fieldSplit.length; i++ )
                {
                    c = fieldSplit[i];

                    if ( StringUtils.isAlphanumeric( c ) || c.equals( ":" ) || c.equals( "~" ) || c.equals( "|" ) )
                    {
                        builder.append( c );
                    }
                    else if ( c.equals( "(" ) ) // start parameter
                    {
                        insideParameters = true;
                        builder.append( c );
                    }
                    else if ( insideParameters && c.equals( ";" ) ) // allow
                                                                   // parameter
                                                                   // separator
                    {
                        builder.append( c );
                    }
                    else if ( (insideParameters && c.equals( ")" )) ) // end
                    {
                        builder.append( c );
                        break;
                    }
                    else if ( c.equals( "," ) || (c.equals( "[" ) && !insideParameters) ) // rewind
                                                                                         // and
                                                                                         // break
                    {
                        i--;
                        break;
                    }
                }
            }
            else if ( c.equals( "," ) )
            {
                putInMap( fieldMap, joinedWithPrefix( builder, prefixList ) );
                builder = new StringBuilder();
            }
            else if ( c.equals( "[" ) || c.equals( "(" ) )
            {
                prefixList.add( builder.toString() );
                builder = new StringBuilder();
            }
            else if ( c.equals( "]" ) || c.equals( ")" ) )
            {
                if ( !builder.toString().isEmpty() )
                {
                    putInMap( fieldMap, joinedWithPrefix( builder, prefixList ) );
                }

                prefixList.remove( prefixList.size() - 1 );
                builder = new StringBuilder();
            }
            else if ( StringUtils.isAlphanumeric( c ) || c.equals( "*" ) || c.equals( ":" ) || c.equals( ";" )
                || c.equals( "~" ) || c.equals( "!" )
                || c.equals( "|" ) || c.equals( "{" ) || c.equals( "}" ) )
            {
                builder.append( c );
            }
        }

        if ( !builder.toString().isEmpty() )
        {
            putInMap( fieldMap, joinedWithPrefix( builder, prefixList ) );
        }

        return fieldMap;
    }

    @Override
    public List<String> modifyFilter( Collection<String> fields, Collection<String> excludeFields )
    {
        if ( fields == null )
        {
            fields = new LinkedList<>();
        }

        return fields.stream()
            .map( s -> s.replaceAll( "]",
                String.format( ",%s]", excludeFields.toString().replaceAll( "\\[|\\]", "" ) ) ) )
            .map( s -> s.replaceAll( "\\)",
                String.format( ",%s)", excludeFields.toString().replaceAll( "\\(|\\)", "" ) ) ) )
            .collect( Collectors.toList() );
    }

    private String joinedWithPrefix( StringBuilder builder, List<String> prefixList )
    {
        String prefixes = StringUtils.join( prefixList, "." );
        prefixes = prefixes.isEmpty() ? builder.toString() : (prefixes + "." + builder.toString());
        return prefixes;
    }

    private void putInMap( FieldMap fieldMap, String path )
    {
        if ( StringUtils.isEmpty( path ) )
        {
            return;
        }

        for ( String p : path.split( "\\." ) )
        {
            fieldMap = fieldMap.computeIfAbsent( p, key -> new FieldMap() );
        }
    }
}
