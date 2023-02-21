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
package org.hisp.dhis.fieldfiltering;

import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

/**
 * FieldFilterParser parses <a href=
 * "https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter">metadata
 * field filters</a>. For example <code>"dataSets[id,name]"</code> will result
 * in three {@link FieldPath}'s for <code>id</code> and <code>name</code> with
 * path <code>dataSet</code> and one for <code>dataSets</code>.
 *
 * @author Morten Olav Hansen
 */
public class FieldFilterParser
{
    public static List<FieldPath> parse( String fields )
    {
        return expandField( fields );
    }

    public static List<FieldPath> parseWithPrefix( String fields, String prefix )
    {
        return expandField( StringUtils.join( fields, "," ), prefix );
    }

    private static List<FieldPath> expandField( String field, String prefix )
    {
        List<FieldPath> fieldPaths = new ArrayList<>();
        Stack<String> path = new Stack<>();
        StringBuilder tokenBuilder = new StringBuilder();
        List<FieldPathTransformer> fieldPathTransformers = new ArrayList<>();

        if ( prefix != null )
        {
            path.push( prefix );
        }

        String[] fieldSplit = field.split( "" );

        boolean isExclude = false; // token is !, which means do not include
        boolean isPreset = false; // token is :, which means it's a preset

        // simple token parser that both tokenizes and builds up the internal
        // fields paths.
        for ( int idx = 0; idx < fieldSplit.length; idx++ )
        {
            String token = fieldSplit[idx];

            if ( isTransformer( fieldSplit, idx ) )
            {
                boolean insideParameters = false;

                StringBuilder transformerNameBuilder = new StringBuilder();

                String transformerName = null;
                List<String> transformerParameters = new ArrayList<>();

                for ( ; idx < fieldSplit.length; idx++ )
                {
                    token = fieldSplit[idx];

                    if ( (containsAny( token, ":", "~", "|" )) )
                    {
                        if ( token.equals( ":" ) )
                        {
                            idx++;
                        }

                        if ( transformerNameBuilder.length() > 0 && transformerParameters.isEmpty() )
                        {
                            transformerName = transformerNameBuilder.toString();
                        }

                        if ( transformerName != null )
                        {
                            fieldPathTransformers.add( new FieldPathTransformer( transformerName,
                                transformerParameters ) );
                        }

                        transformerName = null;
                        transformerNameBuilder = new StringBuilder();
                        transformerParameters = new ArrayList<>();

                        continue;
                    }

                    if ( isAlphanumericOrSpecial( token ) )
                    {
                        transformerNameBuilder.append( token );
                    }
                    else if ( isParameterStart( token ) )
                    {
                        insideParameters = true;
                        transformerName = transformerNameBuilder.toString();
                        transformerNameBuilder = new StringBuilder();
                    }
                    else if ( insideParameters && isParameterSeparator( token ) )
                    {
                        transformerParameters.add( transformerNameBuilder.toString() );
                        transformerNameBuilder = new StringBuilder();
                    }
                    else if ( (insideParameters && isParameterEnd( token )) )
                    {
                        transformerParameters.add( transformerNameBuilder.toString() );
                        break;
                    }
                    else if ( isFieldSeparator( token ) || (token.equals( "[" ) && !insideParameters) )
                    {
                        idx--;
                        break;
                    }
                }

                if ( transformerNameBuilder.length() > 0 && transformerParameters.isEmpty() )
                {
                    transformerName = transformerNameBuilder.toString();
                }

                fieldPathTransformers.add( new FieldPathTransformer( transformerName,
                    transformerParameters ) );
            }
            else if ( isFieldSeparator( token ) )
            {
                addFieldPath( tokenBuilder, path, isExclude, isPreset, fieldPathTransformers, fieldPaths );

                fieldPathTransformers = new ArrayList<>();
                tokenBuilder = new StringBuilder();
                isExclude = false;
                isPreset = false;
            }
            else if ( isBlockStart( token ) )
            {
                addFieldPath( tokenBuilder, path, isExclude, isPreset, fieldPathTransformers, fieldPaths );
                path.push( tokenBuilder.toString() );

                fieldPathTransformers = new ArrayList<>();
                tokenBuilder = new StringBuilder();
                isExclude = false;
                isPreset = false;
            }
            else if ( isBlockEnd( token ) )
            {
                addFieldPath( tokenBuilder, path, isExclude, isPreset, fieldPathTransformers, fieldPaths );
                path.pop();

                fieldPathTransformers = new ArrayList<>();
                tokenBuilder = new StringBuilder();
                isExclude = false;
                isPreset = false;
            }
            else if ( isExclude( token ) )
            {
                isExclude = true;
            }
            else if ( isPreset( token ) )
            {
                isPreset = true;
            }
            else if ( isAllAlias( token ) )
            {
                tokenBuilder.append( "all" );
                isPreset = true;
            }
            else if ( isAlphanumericOrSpecial( token ) )
            {
                tokenBuilder.append( token );
            }
        }

        if ( tokenBuilder.length() > 0 )
        {
            addFieldPath( tokenBuilder, path, isExclude, isPreset, fieldPathTransformers, fieldPaths );
        }

        // OBS! this is a work-around way to deduplicate the fields paths list
        return new ArrayList<>( new LinkedHashSet<>( fieldPaths ) );
    }

    private static void addFieldPath( StringBuilder fieldNameBuilder, Stack<String> path,
        boolean isExclude, boolean isPreset, List<FieldPathTransformer> transformers, List<FieldPath> fieldPaths )
    {
        String name = fieldNameBuilder.toString();

        if ( !isEmpty( name ) )
        {
            FieldPath fieldPath = new FieldPath( name, new ArrayList<>( path ), isExclude, isPreset, transformers );
            fieldPaths.add( fieldPath );
        }
    }

    private static List<FieldPath> expandField( String field )
    {
        return expandField( field, null );
    }

    private static boolean isBlockStart( String token )
    {
        return token != null && containsAny( token, "[", "(" );
    }

    private static boolean isBlockEnd( String token )
    {
        return token != null && containsAny( token, "]", ")" );
    }

    // please be aware that this also could mean both block start, and parameter
    // start depending on context
    private static boolean isParameterStart( String token )
    {
        return containsAny( token, "(" );
    }

    // please be aware that this also could mean both block end, and parameter
    // end depending on context
    private static boolean isParameterEnd( String token )
    {
        return containsAny( token, ")" );
    }

    private static boolean isParameterSeparator( String token )
    {
        return containsAny( token, ";" );
    }

    private static boolean isFieldSeparator( String token )
    {
        return containsAny( token, "," );
    }

    private static boolean isAlphanumericOrSpecial( String token )
    {
        return isAlphanumeric( token ) || containsAny( token, "*", ":", "{", "}", "~", "!", "|" );
    }

    private static boolean isExclude( String token )
    {
        return containsAny( token, "!" );
    }

    private static boolean isPreset( String token )
    {
        return containsAny( token, ":" );
    }

    private static boolean isAllAlias( String token )
    {
        // special case, convert * to all (preset=true)
        return containsAny( token, "*" );
    }

    private static boolean isTransformer( String[] fieldSplit, int idx )
    {
        String token = fieldSplit[idx];

        return containsAny( token, "~", "|" )
            || (fieldSplit.length > 1 && ":".equals( fieldSplit[idx] ) && ":".equals( fieldSplit[idx + 1] ));
    }

    private FieldFilterParser()
    {
    }
}
