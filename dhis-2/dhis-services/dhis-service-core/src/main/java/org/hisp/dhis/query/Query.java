package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Query extends Criteria
{
    private User user;

    private String locale;

    private List<Order> orders = new ArrayList<>();

    private Integer firstResult = 0;

    private Integer maxResults = Integer.MAX_VALUE;

    private Junction.Type rootJunctionType = Junction.Type.AND;

    private List<? extends IdentifiableObject> objects;

    public static Query from( Schema schema )
    {
        return new Query( schema );
    }

    public static Query from( Schema schema, Junction.Type rootJunction )
    {
        return new Query( schema, rootJunction );
    }

    private Query( Schema schema )
    {
        super( schema );
    }

    private Query( Schema schema, Junction.Type rootJunctionType )
    {
        super( schema );
        this.rootJunctionType = rootJunctionType;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public boolean isEmpty()
    {
        return criterions.isEmpty() && orders.isEmpty();
    }

    public List<Order> getOrders()
    {
        return orders;
    }

    public boolean ordersPersisted()
    {
        for ( Order order : orders )
        {
            if ( order.isNonPersisted() )
            {
                return false;
            }
        }

        return true;
    }

    public void clearOrders()
    {
        orders.clear();
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public String getLocale()
    {
        return locale;
    }

    public boolean hasLocale()
    {
        return !StringUtils.isEmpty( locale );
    }

    public void setLocale( String locale )
    {
        this.locale = locale;
    }

    public Integer getFirstResult()
    {
        return firstResult;
    }

    public Query setFirstResult( Integer firstResult )
    {
        this.firstResult = firstResult;
        return this;
    }

    public Integer getMaxResults()
    {
        return maxResults;
    }

    public Query setMaxResults( Integer maxResults )
    {
        this.maxResults = maxResults;
        return this;
    }

    public Junction getRootJunction()
    {
        switch ( rootJunctionType )
        {
            case AND:
                return addConjunction();
            case OR:
                return addDisjunction();
        }

        throw new QueryException( "Unhandled junction type: " + rootJunctionType );
    }

    public List<? extends IdentifiableObject> getObjects()
    {
        return objects;
    }

    public void setObjects( List<? extends IdentifiableObject> objects )
    {
        this.objects = objects;
    }

    public Query addOrder( Order... orders )
    {
        for ( Order order : orders )
        {
            if ( order != null )
            {
                this.orders.add( order );
            }
        }

        return this;
    }

    public Query addOrders( Collection<Order> orders )
    {
        this.orders.addAll( orders );
        return this;
    }

    public Disjunction addDisjunction()
    {
        Disjunction disjunction = new Disjunction( schema );
        add( disjunction );

        return disjunction;
    }

    public Disjunction disjunction()
    {
        return new Disjunction( schema );
    }

    public Conjunction addConjunction()
    {
        Conjunction conjunction = new Conjunction( schema );
        add( conjunction );

        return conjunction;
    }

    public Conjunction conjunction()
    {
        return new Conjunction( schema );
    }

    public Query forceDefaultOrder()
    {
        orders.clear();
        return setDefaultOrder();
    }

    public Query setDefaultOrder()
    {
        if ( !orders.isEmpty() )
        {
            return this;
        }

        if ( schema.havePersistedProperty( "name" ) )
        {
            addOrder( Order.iasc( schema.getPersistedProperty( "name" ) ) );
        }
        if ( schema.havePersistedProperty( "created" ) )
        {
            addOrder( Order.idesc( schema.getPersistedProperty( "created" ) ) );
        }

        return this;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "firstResult", firstResult )
            .add( "maxResults", maxResults )
            .add( "orders", orders )
            .add( "criterions", criterions )
            .toString();
    }
}
