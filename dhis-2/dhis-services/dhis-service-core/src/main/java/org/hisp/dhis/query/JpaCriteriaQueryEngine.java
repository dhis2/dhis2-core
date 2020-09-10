package org.hisp.dhis.query;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

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

        store.getSharingCriteria( query.getUser() );

        DetachedCriteria detachedCriteria = buildCriteria(  builder, query );

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


    private List<Function<Root>, Predicate>> buildPredicates( CriteriaBuilder builder, Query query )
    {

    }

    private DetachedCriteria buildCriteria( CriteriaBuilder builder,  Query query )
    {
        List<Predicate> predicates = new ArrayList<>();

        Predicate junction = getHibernateJunction( builder, query.getRootJunctionType() );

        for ( org.hisp.dhis.query.Criterion criterion : query.getCriterions() )
        {
            addCriterion( junction, criterion );
        }

        query.getAliases().forEach( alias -> detachedCriteria.createAlias( alias, alias ) );

        return detachedCriteria.setProjection(
            Projections.distinct( Projections.id() )
        );
    }

    private Predicate getHibernateJunction( CriteriaBuilder builder, Junction.Type type )
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
                junction = Restrictions.conjunction();
            }

            criteria.add( junction );

            for ( org.hisp.dhis.query.Criterion c : ((Junction) criterion).getCriterions() )
            {
                addJunction( junction, c );
            }
        }
    }

}
