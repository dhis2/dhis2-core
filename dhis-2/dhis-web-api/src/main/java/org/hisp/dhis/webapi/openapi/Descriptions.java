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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

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
        String file = "/openapi/" + filename + ".md";
        InputStream is = target.getResourceAsStream( file );
        if ( is == null )
        {
            return;
        }
        try ( Scanner in = new Scanner( is ) )
        {
            String key = null;
            StringBuilder value = new StringBuilder();
            while ( in.hasNextLine() )
            {
                String line = in.nextLine();
                if ( line.startsWith( "#" ) )
                {
                    if ( key != null && value.length() > 0 )
                    {
                        entries.put( key + ".description", trimText( value.toString() ) );
                    }
                    key = null;
                    value.setLength( 0 );
                }
                if ( line.startsWith( "### " ) )
                {
                    key = toKey( line.substring( 4 ) );
                }
                else
                {
                    if ( key != null && value.length() == 0
                        && (line.startsWith( "http://" ) || line.startsWith( "https://" )) )
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
            if ( key != null && value.length() > 0 )
            {
                entries.put( key + ".description", trimText( value.toString() ) );
            }
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
        return stream( line.split( " " ) )
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

    /**
     * Lookup a description text by key.
     * <p>
     * {@link Descriptions} are per controller.
     * <p>
     * All text relevant for a specific controller are obtained by using
     * {@link #of(Class)} with the controller class as target.
     * <p>
     * The entries will include (in order or least to highest precedence):
     * <ol>
     * <li>All entries in <code>Descriptions.md</code> ("global" texts)</li>
     * <li>All entries in any superclass of the provided target class found in
     * <code><i>{SimpleClassName}.md</i></code> starting from the base type's
     * simple name</li>
     * <li>All entries in target class found in
     * <code><i>{SimpleClassName}.md</i></code></li>
     * </ol>
     *
     * Within a file entries are using the endpoint method name as a namespace.
     * <p>
     * Key patterns are:
     * <ul>
     * <li><code><i>{method-name}</i>.parameter.<i>{name}</i>.description</code></li>
     * <li><code><i>{method-name}</i>.request.description</code></li>
     * <li><code><i>{method-name}</i>.response.<i>{http-status-code}</i>.description</code></li>
     * </ul>
     * OBS! The <code>.description</code> suffix is not stated in the markdown
     * file!
     * <p>
     * Fallbacks to provide descriptions applying to any endpoint methods use
     * the patterns:
     * <ul>
     * <li><code>*.parameter.<i>{name}</i>.description</code></li>
     * <li><code>*.response.<i>{http-status-code}</i>.description</code></li>
     * </ul>
     *
     * Shared parameters add the simple class name of the parameters class and
     * are used as:
     * <ul>
     * <li><code>*.parameter.<i>{simple-class-name}</i>.</i>{name}</i>.description</code></li>
     * </ul>
     *
     * @param key the complete key
     * @return the value for the provided key
     */
    @CheckForNull
    String get( String key )
    {
        return entries.get( key );
    }

    @CheckForNull
    String get( List<String> keys )
    {
        return get( UnaryOperator.identity(), keys );
    }

    @CheckForNull
    String get( UnaryOperator<String> transformer, String... keys )
    {
        return get( transformer, List.of( keys ) );
    }

    /**
     * Returns the first non-null value transformed by the provided transformer.
     * Keys are tried in the order given.
     *
     * @param transformer transformer for a non-null value
     * @param keys keys to try
     * @return the first non-null value transformed
     */
    @CheckForNull
    String get( UnaryOperator<String> transformer, List<String> keys )
    {
        for ( String key : keys )
        {
            String value = get( key );
            if ( value != null )
            {
                return transformer.apply( value );
            }
        }
        return null;
    }

}
