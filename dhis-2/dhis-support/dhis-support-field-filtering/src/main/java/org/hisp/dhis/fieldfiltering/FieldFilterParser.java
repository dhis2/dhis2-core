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
package org.hisp.dhis.fieldfiltering;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Morten Olav Hansen
 */
public class FieldFilterParser
{
    public static List<FieldPath> parse( Set<String> fields )
    {
        return expandField( StringUtils.join( fields, "," ) );
    }

    public static List<FieldPath> parseWithPrefix( Set<String> fields, String prefix )
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

                    if ( (StringUtils.containsAny( token, ":", "~", "|" )) )
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
                fieldPaths.add( getFieldPath( tokenBuilder, path, fieldPathTransformers ) );

                fieldPathTransformers = new ArrayList<>();
                tokenBuilder = new StringBuilder();
            }
            else if ( isBlockStart( token ) )
            {
                fieldPaths.add( getFieldPath( tokenBuilder, path, fieldPathTransformers ) );
                path.push( tokenBuilder.toString() );

                fieldPathTransformers = new ArrayList<>();
                tokenBuilder = new StringBuilder();
            }
            else if ( isBlockEnd( token ) )
            {
                fieldPaths.add( getFieldPath( tokenBuilder, path, fieldPathTransformers ) );
                path.pop();

                fieldPathTransformers = new ArrayList<>();
                tokenBuilder = new StringBuilder();
            }
            else if ( isAlphanumericOrSpecial( token ) )
            {
                tokenBuilder.append( token );
            }
        }

        if ( tokenBuilder.length() > 0 )
        {
            fieldPaths.add( getFieldPath( tokenBuilder, path, fieldPathTransformers ) );
        }

        return fieldPaths;
    }

    private static FieldPath getFieldPath( StringBuilder fieldNameBuilder, Stack<String> path,
        List<FieldPathTransformer> transformers )
    {
        return new FieldPath( fieldNameBuilder.toString(), new ArrayList<>( path ), transformers );
    }

    private static List<FieldPath> expandField( String field )
    {
        return expandField( field, null );
    }

    private static boolean isBlockStart( String token )
    {
        return token != null && StringUtils.containsAny( token, "[", "(" );
    }

    private static boolean isBlockEnd( String token )
    {
        return token != null && StringUtils.containsAny( token, "]", ")" );
    }

    // please be aware that this also could mean both block start, and parameter
    // start depending on context
    private static boolean isParameterStart( String token )
    {
        return StringUtils.containsAny( token, "(" );
    }

    // please be aware that this also could mean both block end, and parameter
    // end depending on context
    private static boolean isParameterEnd( String token )
    {
        return StringUtils.containsAny( token, ")" );
    }

    private static boolean isParameterSeparator( String token )
    {
        return StringUtils.containsAny( token, ";" );
    }

    private static boolean isFieldSeparator( String token )
    {
        return StringUtils.containsAny( token, "," );
    }

    private static boolean isAlphanumericOrSpecial( String token )
    {
        return StringUtils.isAlphanumeric( token )
            || StringUtils.containsAny( token, "*", ":", "{", "}", "~", "!", "|" );
    }

    private static boolean isTransformer( String[] fieldSplit, int idx )
    {
        String token = fieldSplit[idx];

        return StringUtils.containsAny( token, "~", "|" )
            || (fieldSplit.length > 1 && ":".equals( fieldSplit[idx] ) && ":".equals( fieldSplit[idx + 1] ));
    }

    private FieldFilterParser()
    {
    }
}
