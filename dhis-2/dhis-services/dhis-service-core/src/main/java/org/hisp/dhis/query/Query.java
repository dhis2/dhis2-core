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
package org.hisp.dhis.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;

import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Setter
@Accessors( chain = true )
public class Query extends Criteria
{
    private User user;

    private String locale;

    private final List<Order> orders = new ArrayList<>();

    private boolean skipPaging;

    private Integer firstResult = 0;

    private Integer maxResults = Integer.MAX_VALUE;

    private final Junction.Type rootJunctionType;

    private boolean plannedQuery;

    private Defaults defaults = Defaults.EXCLUDE;

    private boolean cacheable = true;

    private List<? extends IdentifiableObject> objects;

    public static Query from( Schema schema )
    {
        return new Query( schema );
    }

    public static Query from( Schema schema, Junction.Type rootJunction )
    {
        return new Query( schema, rootJunction );
    }

    public static Query from( Query query )
    {
        Query clone = Query.from( query.getSchema(), query.getRootJunctionType() );
        clone.setUser( query.getUser() );
        clone.setLocale( query.getLocale() );
        clone.addOrders( query.getOrders() );
        clone.setFirstResult( query.getFirstResult() );
        clone.setMaxResults( query.getMaxResults() );
        clone.add( query.getCriterions() );
        clone.setObjects( query.getObjects() );

        return clone;
    }

    private Query( Schema schema )
    {
        this( schema, Junction.Type.AND );
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

    public boolean ordersPersisted()
    {
        return orders.stream().noneMatch( Order::isNonPersisted );
    }

    public void clearOrders()
    {
        orders.clear();
    }

    public Integer getFirstResult()
    {
        return skipPaging ? 0 : firstResult;
    }

    public Integer getMaxResults()
    {
        return skipPaging ? Integer.MAX_VALUE : maxResults;
    }

    public Query addOrder( Order... orders )
    {
        Stream.of( orders ).filter( Objects::nonNull ).forEach( this.orders::add );
        return this;
    }

    public Query addOrders( Collection<Order> orders )
    {
        this.orders.addAll( orders );
        return this;
    }

    @Override
    public Query add( Criterion criterion )
    {
        super.add( criterion );
        return this;
    }

    @Override
    public Query add( Criterion... criterions )
    {
        super.add( criterions );
        return this;
    }

    @Override
    public Query add( Collection<Criterion> criterions )
    {
        super.add( criterions );
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

        if ( schema.havePersistedProperty( "id" ) )
        {
            addOrder( Order.asc( schema.getPersistedProperty( "id" ) ) );
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
