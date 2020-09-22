package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JpaQuery
    extends Criteria
{
    private User user;

    private String locale;

    private List<Order> orders = new ArrayList<>();

    private boolean skipPaging;

    private Integer firstResult = 0;

    private Integer maxResults = Integer.MAX_VALUE;

    private Junction.Type rootJunctionType = Junction.Type.AND;

    private boolean plannedQuery;

    private Defaults defaults = Defaults.EXCLUDE;

    private List<? extends IdentifiableObject> objects;

    public static JpaQuery from( Schema schema )
    {
        return new JpaQuery( schema );
    }

    public static JpaQuery from( Schema schema, Junction.Type rootJunction )
    {
        return new JpaQuery( schema, rootJunction );
    }

    public static JpaQuery from( CriteriaBuilder builder, JpaQuery query )
    {
        JpaQuery clone = JpaQuery.from( query.getSchema(), query.getRootJunctionType() );
        clone.setUser( query.getUser() );
        clone.setLocale( query.getLocale() );
        clone.addOrders( query.getOrders() );
        clone.setFirstResult( query.getFirstResult() );
        clone.setMaxResults( query.getMaxResults() );
        clone.add( query.getCriterions() );
        clone.setObjects( query.getObjects() );

        return clone;
    }

    private JpaQuery( Schema schema )
    {
        super( schema );
    }

    private JpaQuery( Schema schema, Junction.Type rootJunctionType )
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

    public JpaQuery setUser( User user )
    {
        this.user = user;
        return this;
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

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public JpaQuery setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
        return this;
    }

    public Integer getFirstResult()
    {
        return skipPaging ? 0 : firstResult;
    }

    public JpaQuery setFirstResult( Integer firstResult )
    {
        this.firstResult = firstResult;
        return this;
    }

    public Integer getMaxResults()
    {
        return skipPaging ? Integer.MAX_VALUE : maxResults;
    }

    public JpaQuery setMaxResults( Integer maxResults )
    {
        this.maxResults = maxResults;
        return this;
    }

    public Junction.Type getRootJunctionType()
    {
        return rootJunctionType;
    }

    public boolean isPlannedQuery()
    {
        return plannedQuery;
    }

    public JpaQuery setPlannedQuery( boolean plannedQuery )
    {
        this.plannedQuery = plannedQuery;
        return this;
    }

    public Defaults getDefaults()
    {
        return defaults;
    }

    public JpaQuery setDefaults( Defaults defaults )
    {
        this.defaults = defaults;
        return this;
    }

    public List<? extends IdentifiableObject> getObjects()
    {
        return objects;
    }

    public JpaQuery setObjects( List<? extends IdentifiableObject> objects )
    {
        this.objects = objects;
        return this;
    }

    public JpaQuery addOrder( Order... orders )
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

    public JpaQuery addOrders( Collection<Order> orders )
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

    public JpaQuery forceDefaultOrder()
    {
        orders.clear();
        return setDefaultOrder();
    }

    public JpaQuery setDefaultOrder()
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
