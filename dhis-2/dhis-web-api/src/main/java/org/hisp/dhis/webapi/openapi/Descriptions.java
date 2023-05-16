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
package org.hisp.dhis.webapi.openapi;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Reads CommonMark markdown files into key-value pairs.
 * <p>
 * The splitting into key-value pairs is done one number of conventions:
 * <ol>
 * <li>Everything between a {@code #} header and the next header is ignored
 * (including the header)</li>
 * <li>A {@code ##} header starts a new key prefix</li>
 * <li>A {@code ###} header adds a sub-prefix to the current root-prefix</li>
 * <li>A key header either contains its key prefix in backticks or the entire
 * header line is stripped of space and converted to lower case to compute the
 * key-prefix</li>
 * <li>A header can be split in segments using a colon {@code :}, each segment
 * handled as described in 4.</li>
 * <li>If the first line of a paragraph start with a URL this becomes the
 * {@code <key>.url} key-value pair</li>
 * <li>Any other line is added to the body of the current value until the next
 * key header or the end of the document is found</li>
 * </ol>
 * <p>
 * Examples
 *
 * <pre>
 * ## Tag `metadata`            => metadata
 * ## Metadata                  => metadata
 * ## Metadata : `externalDocs` => metadata.externalDocs
 * </pre>
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
final class Descriptions
{
    /**
     * Note: This needs to use {@link ConcurrentSkipListMap} instead of the
     * usual {@link ConcurrentHashMap} map that does not support inserting a key
     * while in the function to compute the value for another insert.
     */
    private static final Map<Class<?>, Descriptions> CACHE = new ConcurrentSkipListMap<>(
        comparing( Class::getCanonicalName ) );

    private final Class<?> target;

    private final Map<String, String> entries = new HashMap<>();

    static Descriptions of( Class<?> target )
    {
        return CACHE.computeIfAbsent( target, Descriptions::ofUncached );
    }

    static Descriptions ofUncached( Class<?> target )
    {
        Descriptions res = new Descriptions( target );
        // most abstract are "global" descriptions
        if ( target != Descriptions.class )
        {
            res.entries.putAll( of( Descriptions.class ).entries );
        }
        // next are all super-classes starting with the base class by doing parent first
        Class<?> parent = target.getSuperclass();
        if ( parent != null && parent != Object.class )
        {
            // inherit texts from parent
            res.entries.putAll( of( parent ).entries );
        }
        // finally adding all specific to only the target type
        res.read( target.getSimpleName() );
        return res;
    }

    private void read( String filename )
    {
        String file = "/openapi/" + filename + ".openapi.md";
        InputStream is = target.getResourceAsStream( file );
        if ( is == null )
        {
            return;
        }
        try ( Scanner in = new Scanner( is ) )
        {
            String rootKey = null;
            String key = null;
            StringBuilder value = new StringBuilder();
            Consumer<String> addText = prefix -> {
                if ( prefix != null )
                {
                    entries.put( prefix + ".description", trimText( value.toString() ) );
                    value.setLength( 0 );
                }
            };
            while ( in.hasNextLine() )
            {
                String line = in.nextLine();
                if ( line.startsWith( "# " ) )
                {
                    rootKey = null;
                    key = null;
                }
                else if ( line.startsWith( "## " ) )
                {
                    addText.accept( key );
                    rootKey = toKey( line.substring( 3 ) );
                    key = rootKey;
                }
                else if ( line.startsWith( "### " ) )
                {
                    addText.accept( key );
                    key = rootKey + "." + toKey( line.substring( 4 ) );
                }
                else
                {
                    if ( key != null && value.length() == 0 && (line.startsWith( "http://" ) || line.startsWith(
                        "https://" )) )
                    {
                        entries.put( key + ".url", line.trim() );
                    }
                    else
                    {
                        // in CommonMark 2 spaces at the end mean new line
                        value.append( line ).append( "  \n" );
                    }
                }
            }
            addText.accept( key );
        }
    }

    private String trimText( String value )
    {
        String text = value;
        while ( text.startsWith( "  \n" ) )
        {
            text = text.substring( 3 );
        }
        while ( text.endsWith( "  \n" ) )
        {
            text = text.substring( 0, text.length() - 3 );
        }
        return text.trim();
    }

    private static String toKey( String line )
    {
        return stream( line.split( ":" ) )
            .map( String::trim )
            .map( Descriptions::toKeySegment )
            .collect( Collectors.joining( "." ) );
    }

    private static String toKeySegment( String str )
    {
        int start = str.indexOf( '`' );
        return start < 0
            ? str.replaceAll( "\\s+", "" ).toLowerCase()
            : str.substring( start + 1, str.indexOf( '`', start + 1 ) );
    }

    boolean exists( String key )
    {
        return entries.get( key ) != null;
    }

    String get( String key )
    {
        return entries.get( key );
    }

    String get( String key, UnaryOperator<String> transformer )
    {
        String value = get( key );
        return value == null ? null : transformer.apply( value );
    }
}
