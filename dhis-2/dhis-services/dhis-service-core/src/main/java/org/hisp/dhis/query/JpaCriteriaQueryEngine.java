package org.hisp.dhis.query;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
@Component
public class JpaCriteriaQueryEngine<T extends IdentifiableObject>
    implements QueryEngine<T>
{
    private final CurrentUserService currentUserService;

    private final QueryPlanner queryPlanner;

    private final List<InternalHibernateGenericStore<T>> hibernateGenericStores;

    private final SessionFactory sessionFactory;

    private Map<Class<?>, InternalHibernateGenericStore<T>> stores = new HashMap<>();

    @Autowired
    public JpaCriteriaQueryEngine( CurrentUserService currentUserService, QueryPlanner queryPlanner,
        List<InternalHibernateGenericStore<T>> hibernateGenericStores, SessionFactory sessionFactory )
    {
        checkNotNull( currentUserService );
        checkNotNull( queryPlanner );
        checkNotNull( hibernateGenericStores );
        checkNotNull( sessionFactory );

        this.currentUserService = currentUserService;
        this.queryPlanner = queryPlanner;
        this.hibernateGenericStores = hibernateGenericStores;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<T> query( Query query )
    {
        Schema schema = query.getSchema();
        InternalHibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject> ) schema.getKlass() );

        Class<T> klass = (Class<T>) schema.getKlass();

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

        CriteriaBuilder builder = sessionFactory.getCriteriaBuilder();

        CriteriaQuery<T> criteriaQuery = builder.createQuery( klass );
        Root<T> root = criteriaQuery.from( klass );

        if ( query.isEmpty() )
        {
            return sessionFactory.getCurrentSession().createQuery( criteriaQuery.select( root.get( "id" ) ).distinct( true ) ).getResultList();
        }

        Predicate predicate = buildPredicates( builder, root, query );

        criteriaQuery.where( predicate );

        if ( !query.getOrders().isEmpty() )
        {
            criteriaQuery.orderBy( query.getOrders().stream()
                .map( o -> o.isAscending() ? builder.asc( root.get( o.getProperty().getName() ) )
                    : builder.desc( root.get( o.getProperty().getName() ) ) ).collect( Collectors.toList() ) );
        }

        TypedQuery<T> typedQuery = sessionFactory.getCurrentSession().createQuery( criteriaQuery );

        typedQuery.setFirstResult( query.getFirstResult() );
        typedQuery.setMaxResults( query.getMaxResults() );

        return typedQuery.getResultList();
    }

    @Override
    public int count( Query query )
    {
        return 0;
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

    private <Y> Predicate buildPredicates( CriteriaBuilder builder, Root<Y> root, Query query )
    {
        Predicate junction = getJpaJunction( builder, query.getRootJunctionType() );

        for ( org.hisp.dhis.query.Criterion criterion : query.getCriterions() )
        {
            addPredicate( builder, root, junction, criterion );
        }

        query.getAliases().forEach( alias -> root.get( alias ).alias( alias ) );

        return junction;
    }

    private Predicate getJpaJunction( CriteriaBuilder builder, Junction.Type type )
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



    private <Y> Predicate getPredicate( CriteriaBuilder builder, Root<Y> root, Restriction restriction )
    {
        if ( restriction == null || restriction.getOperator() == null )
        {
            return null;
        }

        return restriction.getOperator().getPredicate( builder, root, restriction.getQueryPath() );
    }


    private <Y> void addPredicate( CriteriaBuilder builder, Root<Y> root, Predicate predicateJunction, org.hisp.dhis.query.Criterion criterion )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            Predicate predicate = getPredicate( builder, root, restriction );

            if ( predicate != null )
            {
                predicateJunction.getExpressions().add( predicate );
            }
        }
        else if ( Junction.class.isInstance( criterion ) )
        {
            Predicate junction = null;

            if ( Disjunction.class.isInstance( criterion ) )
            {
                junction = builder.disjunction();
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                junction = builder.conjunction();
            }

            predicateJunction.getExpressions().add( junction );

            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
            {
                addJunction( builder, root, junction, c );
            }
        }
    }

    private <Y> void addJunction( CriteriaBuilder builder, Root<Y> root, Predicate junction, org.hisp.dhis.query.Criterion criterion )
    {
        if ( Restriction.class.isInstance( criterion ) )
        {
            Restriction restriction = (Restriction) criterion;
            Predicate predicate = getPredicate( builder, root, restriction );

            if ( predicate != null )
            {
                junction.getExpressions().add( predicate );
            }
        }
        else if ( Junction.class.isInstance( criterion ) )
        {
            Predicate j = null;

            if ( Disjunction.class.isInstance( criterion ) )
            {
                j = builder.disjunction();
            }
            else if ( Conjunction.class.isInstance( criterion ) )
            {
                j = builder.conjunction();
            }

            junction.getExpressions().add( j );

            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
            {
                addJunction( builder, root, junction, c );
            }
        }
    }
}
