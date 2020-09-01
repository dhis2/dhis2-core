package org.hisp.dhis.query;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Subqueries;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.query.planner.QueryPlan;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
        InternalHibernateGenericStore<?> store = getStore( (Class<? extends IdentifiableObject>) schema.getKlass() );

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






        store.getSharingCriteria();

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

}
