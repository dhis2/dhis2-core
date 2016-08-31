package org.hisp.dhis.query;

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

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CriteriaQueryEngine<T> implements QueryEngine<T>
{
    @Autowired
    private final List<HibernateGenericStore<T>> hibernateGenericStores = new ArrayList<>();

    private Map<Class<?>, HibernateGenericStore<T>> stores = new HashMap<>();

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> query( Query query )
    {
        Schema schema = query.getSchema();

        if ( schema == null )
        {
            return new ArrayList<>();
        }

        HibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        Criteria criteria = buildCriteria( store.getSharingCriteria(), query );

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

        HibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

        if ( store == null )
        {
            return 0;
        }

        Criteria criteria = buildCriteria( store.getSharingCriteria(), countQuery );

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
        if ( query.getFirstResult() != null )
        {
            criteria.setFirstResult( query.getFirstResult() );
        }

        if ( query.getMaxResults() != null )
        {
            criteria.setMaxResults( query.getMaxResults() );
        }

        for ( org.hisp.dhis.query.Criterion criterion : query.getCriterions() )
        {
            addCriterion( criteria, criterion, query.getSchema() );
        }

        for ( Order order : query.getOrders() )
        {
            criteria.addOrder( getHibernateOrder( order ) );
        }

        return criteria;
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

    // TODO verify parameters length
    private Criterion getHibernateCriterion( Schema schema, Restriction restriction )
    {
        if ( restriction == null || restriction.getOperator() == null )
        {
            return null;
        }

        Property property = schema.getProperty( restriction.getPath() );

        List<Object> parameters = new ArrayList<>();

        for ( Object parameter : restriction.getParameters() )
        {
            parameters.add( QueryUtils.getValue( property.getKlass(), parameter ) );
        }

        switch ( restriction.getOperator() )
        {
            case EQ:
            {
                return Restrictions.eq( property.getFieldName(), parameters.get( 0 ) );
            }
            case NE:
            {
                return Restrictions.ne( property.getFieldName(), parameters.get( 0 ) );
            }
            case GT:
            {
                return Restrictions.gt( property.getFieldName(), parameters.get( 0 ) );
            }
            case LT:
            {
                return Restrictions.lt( property.getFieldName(), parameters.get( 0 ) );
            }
            case GE:
            {
                return Restrictions.ge( property.getFieldName(), parameters.get( 0 ) );
            }
            case LE:
            {
                return Restrictions.le( property.getFieldName(), parameters.get( 0 ) );
            }
            case BETWEEN:
            {
                return Restrictions.between( property.getFieldName(), parameters.get( 0 ), parameters.get( 1 ) );
            }
            case LIKE:
            {
                return Restrictions.like( property.getFieldName(), parameters.get( 0 ) );
            }
            case ILIKE:
            {
                return Restrictions.ilike( property.getFieldName(), parameters.get( 0 ) );
            }
            case IN:
            {
                if ( !Collection.class.isInstance( parameters.get( 0 ) ) || ((Collection<?>) parameters.get( 0 )).isEmpty() )
                {
                    return null;
                }

                return Restrictions.in( property.getFieldName(), (Collection<?>) parameters.get( 0 ) );
            }
            case NULL:
            {
                return Restrictions.isNull( property.getFieldName() );
            }
        }

        return null;
    }

    public org.hibernate.criterion.Order getHibernateOrder( Order order )
    {
        if ( order == null || order.getProperty() == null || !order.getProperty().isPersisted() || !order.getProperty().isSimple() )
        {
            return null;
        }

        org.hibernate.criterion.Order criteriaOrder = null;

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

        for ( HibernateGenericStore<T> store : hibernateGenericStores )
        {
            stores.put( store.getClazz(), store );
        }
    }

    private HibernateGenericStore<?> getStore( Class<? extends IdentifiableObject> klass )
    {
        initStoreMap();
        return stores.get( klass );
    }
}
