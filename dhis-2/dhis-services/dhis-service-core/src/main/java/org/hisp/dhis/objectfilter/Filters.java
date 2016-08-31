package org.hisp.dhis.objectfilter;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.collect.Maps;
import org.hisp.dhis.objectfilter.ops.Op;

import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Filters
{
    private Map<String, Object> filters = Maps.newHashMap();

    public Filters()
    {
    }

    public void addFilter( String path, String operator, String value )
    {
        FilterOps filterOps = createPath( path );

        if ( filterOps == null )
        {
            return;
        }

        if ( OpFactory.canCreate( operator ) )
        {
            Op op = OpFactory.create( operator );

            if ( op == null )
            {
                return;
            }

            if ( op.wantValue() )
            {
                if ( value == null )
                {
                    return;
                }

                op.setValue( value );
            }

            filterOps.getFilters().add( op );
        }
    }

    @SuppressWarnings( "unchecked" )
    private FilterOps createPath( String path )
    {
        if ( !path.contains( "." ) )
        {
            if ( !filters.containsKey( path ) )
            {
                filters.put( path, new FilterOps() );
            }

            return (FilterOps) filters.get( path );
        }

        String[] split = path.split( "\\." );

        Map<String, Object> c = filters;

        for ( int i = 0; i < split.length; i++ )
        {
            boolean last = (i == (split.length - 1));

            if ( c.containsKey( split[i] ) )
            {
                if ( FilterOps.class.isInstance( c.get( split[i] ) ) )
                {
                    if ( last )
                    {
                        return (FilterOps) c.get( split[i] );
                    }
                    else
                    {
                        FilterOps self = (FilterOps) c.get( split[i] );
                        Map<String, Object> map = Maps.newHashMap();
                        map.put( "__self__", self );

                        c.put( split[i], map );
                        c = map;
                    }
                }
                else
                {
                    c = (Map<String, Object>) c.get( split[i] );
                }
            }
            else
            {
                if ( last )
                {
                    FilterOps filterOps = new FilterOps();
                    c.put( split[i], filterOps );
                    return filterOps;
                }
                else
                {
                    Map<String, Object> map = Maps.newHashMap();
                    c.put( split[i], map );
                    c = map;
                }
            }
        }

        return null;
    }

    public Map<String, Object> getFilters()
    {
        return filters;
    }

    public void setFilters( Map<String, Object> filters )
    {
        this.filters = filters;
    }

    @Override
    public String toString()
    {
        return "Filters{" +
            "filters=" + filters +
            '}';
    }
}
