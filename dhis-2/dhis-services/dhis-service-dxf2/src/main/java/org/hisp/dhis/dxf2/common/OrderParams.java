package org.hisp.dhis.dxf2.common;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.base.MoreObjects;

import org.hisp.dhis.query.Order;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class OrderParams
{
    private Set<String> order = new HashSet<>();

    public OrderParams()
    {
    }

    public OrderParams( Set<String> order )
    {
        this.order = order;
    }

    public void setOrder( Set<String> order )
    {
        this.order = order;
    }

    public List<Order> getOrders( Schema schema )
    {
        Map<String, Order> orders = new LinkedHashMap<>();

        for ( String o : order )
        {
            String[] split = o.split( ":" );

            //Using ascending as default direction. 
            String direction = "asc";
            
            if ( split.length < 1 )
            {
                continue;
            }
            else if ( split.length == 2 )
            {
                direction = split[1].toLowerCase();
            }

            String propertyName = split[0];
            Property property = schema.getProperty( propertyName );
            

            if ( orders.containsKey( propertyName ) || !schema.haveProperty( propertyName )
                || !validProperty( property ) || !validDirection( direction ) )
            {
                continue;
            }

            orders.put( propertyName, Order.from( direction, property ) );
        }

        return new ArrayList<>( orders.values() );
    }

    private boolean validProperty( Property property )
    {
        return property.isSimple();
    }

    private boolean validDirection( String direction )
    {
        return "asc".equals( direction ) || "desc".equals( direction )
            || "iasc".equals( direction ) || "idesc".equals( direction );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( order );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        final OrderParams other = (OrderParams) obj;

        return Objects.equals( this.order, other.order );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "order", order )
            .toString();
    }
}
