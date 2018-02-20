package org.hisp.dhis.query;

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

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final CurrentUserService currentUserService;

    private final QueryPlanner queryPlanner;

    private final List<InternalHibernateGenericStore<T>> hibernateGenericStores;

    private Map<Class<?>, InternalHibernateGenericStore<T>> stores = new HashMap<>();

    @Autowired
    public CriteriaQueryEngine( CurrentUserService currentUserService, QueryPlanner queryPlanner,
        List<InternalHibernateGenericStore<T>> hibernateGenericStores )
    {
        this.currentUserService = currentUserService;
        this.queryPlanner = queryPlanner;
        this.hibernateGenericStores = hibernateGenericStores;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> query( Query query )
    {
        Schema schema = query.getSchema();
        InternalHibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        if ( query.getUser() == null )
        {
            query.setUser( currentUserService.getCurrentUser() );
        }

        if ( !query.isPlannedQuery() )
        {
            QueryPlan queryPlan = queryPlanner.planQuery( query, true );
            query = queryPlan.getPersistedQuery();
        }

        DetachedCriteria detachedCriteria = buildCriteria( store.getSharingDetachedCriteria( query.getUser() ), query );
        Criteria criteria = store.getCriteria();

        if ( criteria == null )
        {
            return new ArrayList<>();
        }

        criteria.setFirstResult( query.getFirstResult() );
        criteria.setMaxResults( query.getMaxResults() );

        for ( Order order : query.getOrders() )
        {
            criteria.addOrder( getHibernateOrder( order ) );
        }

        return criteria.add( Subqueries.propertyIn( "id", detachedCriteria ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public int count( Query query )
    {
        Schema schema = query.getSchema();
        InternalHibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

        if ( store == null )
        {
            return 0;
        }

        if ( query.getUser() == null )
        {
            query.setUser( currentUserService.getCurrentUser() );
        }

        if ( !query.isPlannedQuery() )
        {
            QueryPlan queryPlan = queryPlanner.planQuery( query, true );
            query = queryPlan.getPersistedQuery();
        }

        DetachedCriteria detachedCriteria = buildCriteria( store.getSharingDetachedCriteria( query.getUser() ), query );
        Criteria criteria = store.getCriteria();

        if ( criteria == null )
        {
            return 0;
        }

        return ((Number) criteria.add( Subqueries.propertyIn( "id", detachedCriteria ) )
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    private DetachedCriteria buildCriteria( DetachedCriteria detachedCriteria, Query query )
    {
        if ( query.isEmpty() )
        {
            return detachedCriteria.setProjection(
                Projections.distinct( Projections.id() )
            );
        }

        org.hibernate.criterion.Junction junction = getHibernateJunction( query.getRootJunctionType() );
        detachedCriteria.add( junction );

        for ( org.hisp.dhis.query.Criterion criterion : query.getCriterions() )
        {
            addCriterion( junction, criterion );
        }

        query.getAliases().forEach( alias -> detachedCriteria.createAlias( alias, alias ) );

        return detachedCriteria.setProjection(
            Projections.distinct( Projections.id() )
        );
    }

    private void addCriterion( org.hibernate.criterion.Junction criteria, org.hisp.dhis.query.Criterion criterion )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            Criterion hibernateCriterion = getHibernateCriterion( restriction );

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
                addJunction( junction, c );
            }
        }
    }

    private void addJunction( org.hibernate.criterion.Junction junction, org.hisp.dhis.query.Criterion criterion )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            Criterion hibernateCriterion = getHibernateCriterion( restriction );

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
                addJunction( junction, c );
            }
        }
    }

    private org.hibernate.criterion.Junction getHibernateJunction( Junction.Type type )
    {
        switch ( type )
        {
            case AND:
                return Restrictions.conjunction();
            case OR:
                return Restrictions.disjunction();
        }

        return Restrictions.conjunction();
    }

    private Criterion getHibernateCriterion( Restriction restriction )
    {
        if ( restriction == null || restriction.getOperator() == null )
        {
            return null;
        }

        return restriction.getOperator().getHibernateCriterion( restriction.getQueryPath() );
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
