package org.hisp.dhis.analytics.util;

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

import org.springframework.util.Assert;

/**
 * Utilities for analytics SQL operations, compatible with PostgreSQL
 * and H2 database platforms.
 * 
 * @author Lars Helge Overland
 */
public class AnalyticsSqlUtils
{
    private static final String DOUBLE_QUOTE = "\"";
    private static final String SINGLE_QUOTE = "'";
    private static final String SEPARATOR = ".";
    
    /**
     * Quotes the given relation (typically a column). Quotes part of
     * the given relation are encoded (replaced by double quotes that is).
     * 
     * @param relation the relation (typically a column).
     * @return the quoted relation.
     */
    public static String quote( String relation )
    {
        Assert.notNull( relation, "Relation must be specified" );
        
        String rel = relation.replaceAll( DOUBLE_QUOTE, ( DOUBLE_QUOTE + DOUBLE_QUOTE ) );
        
        return DOUBLE_QUOTE + rel + DOUBLE_QUOTE;
    }

    /**
     * Quotes and qualifies the given relation (typically a column). Quotes part 
     * of the given relation are encoded (replaced by double quotes that is).
     * 
     * @param relation the relation (typically a column).
     * @return the quoted relation.
     */
    public static String quote( String alias, String relation )
    {
        Assert.notNull( alias, "Alias must be specified" );
        
        return alias + SEPARATOR + quote( relation );
    }

    /**
     * Encodes the given value.
     * 
     * @param value the value.
     * @param quote whether to quote the value.
     * @return the encoded value.
     */
    public static String encode( String value, boolean quote )
    {
        if ( value != null )
        {
            value = value.endsWith( "\\" ) ? value.substring( 0, value.length() - 1 ) : value;
            value = value.replaceAll( SINGLE_QUOTE, SINGLE_QUOTE + SINGLE_QUOTE );
        }

        return quote ? ( SINGLE_QUOTE + value + SINGLE_QUOTE ) : value;
    }
}
