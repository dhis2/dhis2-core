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

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of QueryEngine that uses Hibernate Criteria and
 * supports idObjects only.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CriteriaQueryEngine<T extends IdentifiableObject>
    implements QueryEngine<T>
{
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private final List<InternalHibernateGenericStore<T>> hibernateGenericStores = new ArrayList<>();

    private Map<Class<?>, InternalHibernateGenericStore<T>> stores = new HashMap<>();

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> query( Query query )
    {
        Schema schema = query.getSchema();

        if ( schema == null )
        {
            return new ArrayList<>();
        }

        InternalHibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        if ( query.getUser() == null )
        {
            query.setUser( currentUserService.getCurrentUser() );
        }

        Criteria criteria = buildCriteria( store.getSharingCriteria( query.getUser() ), query );

        if ( criteria == null )
        {
            return new ArrayList<>();
        }

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public int count( Query query )
    {
        Schema schema = query.getSchema();

        // create a copy of this query using only the restrictions
        Query countQuery = Query.from( query.getSchema() );
        countQuery.add( query.getCriterions() );

        if ( schema == null )
        {
            return 0;
        }

        InternalHibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

        if ( store == null )
        {
            return 0;
        }

        if ( query.getUser() == null )
        {
            query.setUser( currentUserService.getCurrentUser() );
        }

        Criteria criteria = buildCriteria( store.getSharingCriteria( query.getUser() ), countQuery );

        if ( criteria == null )
        {
            return 0;
        }

        return ((Number) criteria
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    private Criteria buildCriteria( Criteria criteria, Query query )
    {
        Query criteriaQuery = getCriteriaQuery( query );

        for ( org.hisp.dhis.query.Criterion criterion : criteriaQuery.getCriterions() )
        {
            addCriterion( criteria, criterion, query.getSchema() );
        }

        // no more criterions available, so we can do our own paging
        if ( query.isEmpty() )
        {
            if ( query.getFirstResult() != null )
            {
                criteria.setFirstResult( query.getFirstResult() );
            }

            if ( query.getMaxResults() != null )
            {
                criteria.setMaxResults( query.getMaxResults() );
            }
        }

        for ( Order order : criteriaQuery.getOrders() )
        {
            criteria.addOrder( getHibernateOrder( order ) );
        }

        return criteria;
    }

    /**
     * Remove criterions that can be applied by criteria engine, and return those. The rest of
     * the criterions will be passed on to the next query engine.
     *
     * @param query Query
     * @return Query instance
     */
    private Query getCriteriaQuery( Query query )
    {
        Query criteriaQuery = Query.from( query.getSchema() );
        Iterator<org.hisp.dhis.query.Criterion> criterionIterator = query.getCriterions().iterator();

        while ( criterionIterator.hasNext() )
        {
            org.hisp.dhis.query.Criterion criterion = criterionIterator.next();

            if ( Restriction.class.isInstance( criterion ) )
            {
                Restriction restriction = (Restriction) criterion;

                if ( !restriction.getPath().contains( "\\." ) )
                {
                    if ( criteriaQuery.getSchema().haveProperty( restriction.getPath() ) )
                    {
                        Property property = query.getSchema().getProperty( restriction.getPath() );

                        if ( property.isSimple() && property.isPersisted() )
                        {
                            criteriaQuery.getCriterions().add( criterion );
                            criterionIterator.remove();
                        }
                    }
                }
            }
        }

        if ( query.ordersPersisted() )
        {
            criteriaQuery.addOrders( query.getOrders() );
            query.clearOrders();
        }

        return criteriaQuery;
    }

    private void addJunction( org.hibernate.criterion.Junction junction, org.hisp.dhis.query.Criterion criterion, Schema schema )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            Criterion hibernateCriterion = getHibernateCriterion( schema, restriction );

            if ( hibernateCriterion != null )
            {
                junction.add( hibernateCriterion );
            }
        }
        else if ( Junction.class.isInstance( criterion ) )
        {
            org.hibernate.criterion.Junction j = null;

            if ( Disjunction.class.isInstance( criterion ) )
            {
                j = Restrictions.disjunction();
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                j = Restrictions.conjunction();
            }

            junction.add( j );

            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
            {
                addJunction( junction, c, schema );
            }
        }
    }

    private void addCriterion( Criteria criteria, org.hisp.dhis.query.Criterion criterion, Schema schema )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            Criterion hibernateCriterion = getHibernateCriterion( schema, restriction );

            if ( hibernateCriterion != null )
            {
                criteria.add( hibernateCriterion );
            }
        }
        else if ( Junction.class.isInstance( criterion ) )
        {
            org.hibernate.criterion.Junction junction = null;

            if ( Disjunction.class.isInstance( criterion ) )
            {
                junction = Restrictions.disjunction();
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                junction = Restrictions.conjunction();
            }

            criteria.add( junction );

            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
            {
                addJunction( junction, c, schema );
            }
        }
    }

    private Criterion getHibernateCriterion( Schema schema, Restriction restriction )
    {
        if ( restriction == null || restriction.getOperator() == null )
        {
            return null;
        }

        Property property = schema.getProperty( restriction.getPath() );

        return restriction.getOperator().getHibernateCriterion( property );
    }

    public org.hibernate.criterion.Order getHibernateOrder( Order order )
    {
        if ( order == null || order.getProperty() == null || !order.getProperty().isPersisted() || !order.getProperty().isSimple() )
        {
            return null;
        }

        org.hibernate.criterion.Order criteriaOrder;

        if ( order.isAscending() )
        {
            criteriaOrder = org.hibernate.criterion.Order.asc( order.getProperty().getFieldName() );
        }
        else
        {
            criteriaOrder = org.hibernate.criterion.Order.desc( order.getProperty().getFieldName() );
        }

        if ( order.isIgnoreCase() )
        {
            criteriaOrder.ignoreCase();
        }

        return criteriaOrder;
    }

    private void initStoreMap()
    {
        if ( !stores.isEmpty() )
        {
            return;
        }

        for ( InternalHibernateGenericStore<T> store : hibernateGenericStores )
        {
            stores.put( store.getClazz(), store );
        }
    }

    private InternalHibernateGenericStore<?> getStore( Class<? extends IdentifiableObject> klass )
    {
        initStoreMap();
        return stores.get( klass );
    }
}
