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
package org.hisp.dhis.helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class QueryParamsBuilder
{
    List<MutablePair<String, String>> queryParams;

    public QueryParamsBuilder()
    {
        queryParams = new ArrayList<>();
    }

    public QueryParamsBuilder add( String param, String value )
    {
        return this.add( param + "=" + value );
    }

    /**
     * Adds or updates the query param. Format: key=value
     *
     * @param param
     * @return
     */
    public QueryParamsBuilder add( String param )
    {
        String[] split = param.split( "=" );
        MutablePair pair = getByKey( split[0] );

        if ( pair != null && !pair.getKey().equals( "filter" ) )
        {
            pair.setRight( split[1] );
            return this;
        }

        queryParams.add( MutablePair.of( split[0], split[1] ) );

        return this;
    }

    public QueryParamsBuilder addAll( String... params )
    {
        for ( String param : params )
        {
            this.add( param );
        }

        return this;
    }

    private MutablePair getByKey( String key )
    {
        return queryParams.stream()
            .filter( p -> p.getLeft().equals( key ) )
            .findFirst()
            .orElse( null );
    }

    public String build()
    {
        if ( queryParams.size() == 0 )
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append( "?" );

        for ( int i = 0; i < queryParams.size(); i++ )
        {
            builder.append( String.format( "%s=%s", queryParams.get( i ).getLeft(), queryParams.get( i ).getRight() ) );

            if ( i != queryParams.size() - 1 )
            {
                builder.append( "&" );
            }
        }

        return builder.toString();
    }
}
