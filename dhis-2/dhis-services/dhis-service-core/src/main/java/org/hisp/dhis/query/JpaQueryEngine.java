package org.hisp.dhis.query;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.query.operators.Operator;
import org.hisp.dhis.query.planner.QueryPath;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.user.User;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class JpaQueryEngine<T extends IdentifiableObject> implements QueryEngine<T>
{
    private final User currentUser;

    private final QueryPlanner queryPlanner;

    private final InternalHibernateGenericStore<T> store;

    private Class<T> clazz;

    public JpaQueryEngine( QueryPlanner queryPlanner,  InternalHibernateGenericStore<T> store, User currentUser )
    {
        this.queryPlanner = queryPlanner;
        this.store = store;
        this.currentUser = currentUser;
    }

    @Override
    public List<T> query( Query query )
    {
        System.out.println( "JpaQueryEngine.query" );
        if ( store == null )
        {
            return new ArrayList<>();
        }

        if ( query.getUser() == null )
        {
            query.setUser( currentUser );
        }

        if ( !query.isPlannedQuery() )
        {
            QueryPlan queryPlan = queryPlanner.planQuery( query, true );
            query = queryPlan.getPersistedQuery();
        }

        CriteriaBuilder builder = store.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery( store.getClazz() );
        Root<T> root = criteriaQuery.from( store.getClazz() );

        Subquery<Integer> subQuery = criteriaQuery.subquery( Integer.class );
        subQuery.from( store.getClazz() );
        subQuery.select(  root.get( "id" ) ).distinct( true );

        List<Predicate> predicates = new ArrayList<>();

        System.out.println( "query = " + query.getPredicates().size() );


        predicates.addAll( buildPredicates( builder, root, query ) );
        predicates.forEach( p -> System.out.println(p.getExpressions().size()) );

        if ( predicates.isEmpty() )
        {
            return new ArrayList<>();
        }

        predicates.addAll( store.getSharingPredicates( builder, query.getUser() ).stream().map( p -> p.apply( root ) ).collect( Collectors.toList()) );

        subQuery.select( root.get( "id" ) );

        subQuery.where( predicates.toArray( new Predicate[0] ) );

        criteriaQuery.where( builder.in( subQuery ) );

        for ( Order order : query.getOrders() )
        {
            criteriaQuery.orderBy( getQueryOrder( builder, root, order ) );
        }

        TypedQuery<T> typedQuery = store.getExecutableTypedQuery( criteriaQuery );

        typedQuery.setFirstResult( query.getFirstResult() );
        typedQuery.setMaxResults( query.getMaxResults() );

        return typedQuery.getResultList();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public int count( Query query )
    {
        if ( store == null )
        {
            return 0;
        }

        if ( query.getUser() == null )
        {
            query.setUser( currentUser );
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

    private List<Predicate> buildPredicates( CriteriaBuilder builder, Root<T> root, Query query )
    {
        List<Predicate> predicates = new ArrayList<>();
        if ( query.isEmpty() )
        {
            return predicates;
        }


        Predicate junction = getJpaJunctionType( builder, query.getRootJunctionType() );

        junction.getExpressions().addAll( query.getPredicates().stream().map( p -> p.apply( root ) ).collect( Collectors.toList()) );

        predicates.add( junction );

        return predicates;
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


    private org.hibernate.criterion.Junction getHibernateJunction( Junction.Type type )
    {
        switch ( type )
        {
            case AND:
                return org.hibernate.criterion.Restrictions.conjunction();
            case OR:
                return org.hibernate.criterion.Restrictions.disjunction();
        }

        return Restrictions.conjunction();
    }
//
//    private Predicate convertCriterionToPredicate( CriteriaBuilder builder, Root<T> root,  org.hisp.dhis.query.Criterion criterion )
//    {
//        if ( Restriction.class.isInstance( criterion ) )
//        {
//            Restriction restriction = (Restriction) criterion;
//            Operator operator = restriction.getOperator();
//            String path = restriction.getPath();
//            QueryPath queryPath = restriction.getQueryPath();
//            value = QueryUtils.p
//
//
//
//
//            org.hibernate.criterion.Criterion hibernateCriterion = getHibernateCriterion( restriction );
//            hibernateCriterion.getTypedValues(  );
//
//            if ( hibernateCriterion != null )
//            {
//                criteria.add( hibernateCriterion );
//            }
//        }
//        else if ( Junction.class.isInstance( criterion ) )
//        {
//            org.hibernate.criterion.Junction junction = null;
//
//            if ( Disjunction.class.isInstance( criterion ) )
//            {
//                junction = org.hibernate.criterion.Restrictions.disjunction();
//            }
//            else if ( Conjunction.class.isInstance( criterion ) )
//            {
//                junction = org.hibernate.criterion.Restrictions.conjunction();
//            }
//
//            criteria.add( junction );
//
//            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
//            {
//                addJunction( junction, c );
//            }
//        }
//    }

    private void addCriterion( org.hibernate.criterion.Junction criteria, org.hisp.dhis.query.Criterion criterion )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            org.hibernate.criterion.Criterion hibernateCriterion = getHibernateCriterion( restriction );

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
                junction = org.hibernate.criterion.Restrictions.disjunction();
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                junction = org.hibernate.criterion.Restrictions.conjunction();
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
            org.hibernate.criterion.Criterion hibernateCriterion = getHibernateCriterion( restriction );

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
                j = org.hibernate.criterion.Restrictions.disjunction();
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                j = org.hibernate.criterion.Restrictions.conjunction();
            }

            junction.add( j );

            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
            {
                addJunction( junction, c );
            }
        }
    }

    private Predicate getJpaJunctionType( CriteriaBuilder builder, Junction.Type type )
    {
        switch ( type )
        {
            case AND:
                return builder.conjunction();
            case OR:
                return builder.disjunction();
        }

        return builder.conjunction();
    }

    private org.hibernate.criterion.Criterion getHibernateCriterion( Restriction restriction )
    {
        if ( restriction == null || restriction.getOperator() == null )
        {
            return null;
        }

        return restriction.getOperator().getHibernateCriterion( restriction.getQueryPath() );
    }

    public javax.persistence.criteria.Order getQueryOrder( CriteriaBuilder builder, Root<T> root, Order order )
    {
        if ( order == null || order.getProperty() == null || !order.getProperty().isPersisted() || !order.getProperty().isSimple() )
        {
            return null;
        }

        javax.persistence.criteria.Order queryOrder;

        if ( order.isAscending() )
        {
            queryOrder = builder.asc( getOrderExpression( builder, root, order.getProperty().getFieldName(), order.isIgnoreCase() ) );
        }
        else
        {
            queryOrder = builder.desc( getOrderExpression( builder, root, order.getProperty().getFieldName(), order.isIgnoreCase() ) );
        }

        return queryOrder;
    }

    private Expression<String> getOrderExpression( CriteriaBuilder builder, Root<T> root, String fieldName, boolean ignoreCase )
    {
        if ( ignoreCase )
        {
            return root.get( fieldName );
        }
        else
        {
            return builder.lower( root.get( fieldName ) );
        }

    }
}
