package org.hisp.dhis.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public class HibernateGenericStore<T>
    implements GenericStore<T>
{
    private static final Log log = LogFactory.getLog( HibernateGenericStore.class );

    public static final String HIBERNATE_CACHEABLE_HINT = "org.hibernate.cacheable";

    protected SessionFactory sessionFactory;

    @Required
    public void setSessionFactory( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    protected JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected Class<T> clazz;

    /**
     * Could be overridden programmatically.
     */
    @Override
    public Class<T> getClazz()
    {
        return clazz;
    }

    /**
     * Could be injected through container.
     */
    @Required
    public void setClazz( Class<T> clazz )
    {
        this.clazz = clazz;
    }

    protected boolean cacheable = false;

    /**
     * Could be overridden programmatically.
     */
    protected boolean isCacheable()
    {
        return cacheable;
    }

    /**
     * Could be injected through container.
     */
    public void setCacheable( boolean cacheable )
    {
        this.cacheable = cacheable;
    }

    // -------------------------------------------------------------------------
    // Convenience methods
    // -------------------------------------------------------------------------

    /**
     * Returns the current session.
     *
     * @return the current session.
     */
    protected final Session getSession()
    {
        return sessionFactory.getCurrentSession();
    }

    /**
     * Creates a Query with given hql String
     * Return type is auto cast to generic type T of the Store class
     * @param hql the hql query.
     * @return a Query instance with return type is the object type T of the store class
     */
    protected final Query<T> getQuery( String hql )
    {
        Query<T> query = getSession().createQuery( hql );
        return query.setCacheable( cacheable );
    }

    /**
     * Creates a Query with given hql String
     * Must specify the return type of the Query variable.
     * @param hql the hql query.
     * @return a Query instance with return type specified in the Query<Y>
     */
    protected final <Y> Query<Y> getTypedQuery( String hql )
    {
        Query<Y> query = getSession().createQuery( hql );
        return query.setCacheable( cacheable );
    }

    /**
     * Creates a Criteria for the implementation Class type.
     * <p>
     * Please note that sharing is not considered.
     *
     * @return a Criteria instance.
     */
    public final Criteria getCriteria()
    {
        DetachedCriteria criteria = DetachedCriteria.forClass( getClazz() );

        preProcessDetachedCriteria( criteria );

        return getExecutableCriteria( criteria );
    }

    /**
     * Override to add additional restrictions to criteria before
     * it is invoked.
     */
    protected void preProcessDetachedCriteria( DetachedCriteria detachedCriteria )
    {
    }

    public final Criteria getExecutableCriteria( DetachedCriteria detachedCriteria )
    {
        return detachedCriteria.getExecutableCriteria( getSession() ).setCacheable( cacheable );
    }

    @Deprecated
    protected Criteria getClazzCriteria()
    {
        return getSession().createCriteria( getClazz() );
    }

    public CriteriaBuilder getCriteriaBuilder()
    {
        return sessionFactory.getCriteriaBuilder();
    }

    /**
     * Creates a Criteria for the implementation Class type restricted by the
     * given Criterions.
     *
     * @param expressions the Criterions for the Criteria.
     * @return a Criteria instance.
     */
    protected final Criteria getCriteria( Criterion... expressions )
    {
        Criteria criteria = getCriteria();

        for ( Criterion expression : expressions )
        {
            criteria.add( expression );
        }

        criteria.setCacheable( cacheable );
        return criteria;
    }

    //------------------------------------------------------------------------------------------
    // JPA Methods
    //------------------------------------------------------------------------------------------

    /**
     * Get executable Typed Query from Criteria Query.
     * Apply cache if needed.
     * @param criteriaQuery
     * @return  executable TypedQuery
     */
    public final TypedQuery<T> getExecutableTypedQuery( CriteriaQuery<T> criteriaQuery )
    {
        return getSession()
            .createQuery( criteriaQuery )
            .setHint( HIBERNATE_CACHEABLE_HINT, cacheable );
    }

    /**
     * Method for adding additional Predicates into where clause
     * @param builder
     * @param predicates
     */
    protected void preProcessPredicates( CriteriaBuilder builder, List<Function<Root<T>, Predicate>> predicates )
    {
    }

    /**
     * Get single result from executable typedQuery
     * @param Executable TypedQuery
     * @return single object
     */
   protected <V> V getSingleResult( TypedQuery<V> typedQuery )
    {
        List<V> list = typedQuery.getResultList();

        if ( list != null && list.size() > 1 )
        {
            throw new NonUniqueResultException( "More than one entity found for query" );
        }

        return list != null && !list.isEmpty() ? list.get( 0 ) : null;
    }

    /**
     * Get List objects returned by JPA CriteriaQuery
     * @param criteriaQuery
     * @return list objects
     */
    protected List<T> getList( CriteriaQuery<T> criteriaQuery )
    {
        return getSession().createQuery( criteriaQuery ).getResultList();
    }

    /**
     * Get List objects returned by executable TypedQuery
     * @param typedQuery
     * @return list result
     */
    protected final List<T> getList( TypedQuery<T> typedQuery )
    {
        return typedQuery.getResultList();
    }

    /**
     * Get List objects return by querying given JpaQueryParameters
     * @param builder
     * @param parameters JpaQueryParameters
     * @return list objects
     */
    protected final List<T> getList( CriteriaBuilder builder, JpaQueryParameters<T> parameters )
    {
        return getTypedQuery( builder, parameters ).getResultList();
    }

    /**
     * Get executable TypedQuery from JpaQueryParameter.
     * @param builder
     * @param parameters
     * @return executable TypedQuery
     */
    protected final TypedQuery<T> getTypedQuery( CriteriaBuilder builder, JpaQueryParameters<T> parameters )
    {
        List<Function<Root<T>, Predicate>> predicateProviders = parameters.getPredicates();
        List<Function<Root<T>, Order>> orderProviders = parameters.getOrders();
        preProcessPredicates( builder, predicateProviders );

        CriteriaQuery<T> query = builder.createQuery( getClazz() );
        Root<T> root = query.from( getClazz() );
        query.select( root );

        if ( !predicateProviders.isEmpty() )
        {
            List<Predicate> predicates = predicateProviders.stream().map( t -> t.apply( root ) ).collect( Collectors.toList() );
            query.where( predicates.toArray( new Predicate[0] ) );
        }

        if ( !orderProviders.isEmpty() )
        {
            List<Order> orders = orderProviders.stream().map( o -> o.apply( root ) ).collect( Collectors.toList() );
            query.orderBy( orders );
        }

        TypedQuery<T> typedQuery = getExecutableTypedQuery( query );

        if ( parameters.hasFirstResult() )
        {
            typedQuery.setFirstResult( parameters.getFirstResult() );
        }

        if ( parameters.hasMaxResult() )
        {
            typedQuery.setMaxResults( parameters.getMaxResults() );
        }

        typedQuery.setHint( HIBERNATE_CACHEABLE_HINT, parameters.isCachable() != null ? parameters.isCachable() : cacheable );

        return typedQuery;
    }

    /**
     * Count number of objects based on given parameters
     * @param builder
     * @param parameters JpaQueryParameters
     * @return number of objects
     */
    protected  final Long getCount( CriteriaBuilder builder, JpaQueryParameters<T> parameters  )
    {
        CriteriaQuery<Long> query = builder.createQuery( Long.class );

        Root<T> root = query.from( getClazz() );

        List<Function<Root<T>, Predicate>> predicateProviders = parameters.getPredicates();

        List<Function<Root<T>, Expression<Long>>> countExpressions = parameters.getCountExpressions();

        if ( !countExpressions.isEmpty() )
        {
            if ( countExpressions.size() > 1 )
            {
                query.multiselect( countExpressions.stream().map( c -> c.apply( root ) ).collect( Collectors.toList()) ) ;
            }
            else
            {
                query.select( countExpressions.get( 0 ).apply( root ) );
            }
        }
        else
        {
            query.select(  parameters.isUseDistinct() ?  builder.countDistinct( root ) :  builder.count( root )  );
        }

        if ( !predicateProviders.isEmpty() )
        {
            List<Predicate> predicates = predicateProviders.stream().map( t -> t.apply( root ) ).collect( Collectors.toList() );
            query.where( predicates.toArray( new Predicate[0] ) );
        }

        return getSession().createQuery( query ).getSingleResult();
    }

    /**
     * Retrieves an object based on the given Jpa Predicates.
     *
     * @param parameters
     * @return an object of the implementation Class type.
     */
    protected T getSingleResult( CriteriaBuilder builder,  JpaQueryParameters<T> parameters )
    {
        return getSingleResult( getTypedQuery( builder, parameters ) );
    }

    //------------------------------------------------------------------------------------------
    // End JPA Methods
    //------------------------------------------------------------------------------------------

    /**
     * Creates a SqlQuery.
     *
     * @param sql the sql query String.
     * @return a NativeQuery<T> instance.
     */
    protected final NativeQuery<T> getSqlQuery( String sql )
    {
        NativeQuery<T> query = getSession().createNativeQuery( sql );
        query.setHint( HIBERNATE_CACHEABLE_HINT, cacheable );
        return query;
    }

    // -------------------------------------------------------------------------
    // GenericIdentifiableObjectStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void save( T object )
    {
        AuditLogUtil.infoWrapper( log, object, AuditLogUtil.ACTION_CREATE );
        
        getSession().save( object );
    }
    
    @Override
    public void update( T object )
    {
        getSession().update( object );
    }
    
    @Override
    public void delete( T object )
    {
        getSession().delete( object );
    }
    
    @Override
    public T get( int id )
    {
        T object = (T) getSession().get( getClazz(), id );
        
        return postProcessObject( object );
    }
    
    /**
     * Override for further processing of a retrieved object.
     * 
     * @param object the object.
     * @return the processed object.
     */
    protected T postProcessObject( T object )
    {
        return object;
    }

    @Override
    public List<T> getAll()
    {
        return getList( getCriteriaBuilder(), new JpaQueryParameters<T>() );
    }
    
    @Override
    public List<T> getAllByAttributes( List<Attribute> attributes )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> parameters = new JpaQueryParameters<T>()
            .addPredicate( root ->  root.join( "attributeValues", JoinType.INNER ).get( "attribute" ).in( attributes )  );

        return getList( builder, parameters );
    }

    @Override
    public int getCount()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getCount( builder, newJpaParameters().count( root -> builder.countDistinct( root.get( "id" ) ) ) ).intValue();
    }

    @Override
    public List<AttributeValue> getAttributeValueByAttribute( Attribute attribute )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<AttributeValue> query = builder.createQuery( AttributeValue.class );

        Root root = query.from( getClazz() );
        Join joinAttributeValue = root.join( ( "attributeValues" ), JoinType.INNER );
        query.select( root.get( "attributeValues" ) );
        query.where( builder.equal( joinAttributeValue.get( "attribute" ), attribute ) );

        return getSession().createQuery( query ).list();
    }

    @Override
    public List<AttributeValue> getAttributeValueByAttributeAndValue( Attribute attribute, String value )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<AttributeValue> query = builder.createQuery( AttributeValue.class );

        Root<T> root = query.from( getClazz() );
        Join joinAttributeValue = root.join( ( "attributeValues" ), JoinType.INNER );
        query.select( root.get( "attributeValues" ) );
        query.where(
            builder.and(
                builder.equal( joinAttributeValue.get( "attribute" ), attribute ),
                builder.equal( joinAttributeValue.get( "value" ), value ) ) );

        return getSession().createQuery( query ).list();
    }

    @Override
    public <P extends IdentifiableObject> boolean isAttributeValueUnique( P object, AttributeValue attributeValue )
    {
        List<AttributeValue> values = getAttributeValueByAttribute( attributeValue.getAttribute() );
        return values.isEmpty() || (object != null && values.size() == 1 && object.getAttributeValues().contains( values.get( 0 ) ));
    }

    @Override
    public <P extends IdentifiableObject> boolean isAttributeValueUnique( P object, Attribute attribute, String value )
    {
        List<AttributeValue> values = getAttributeValueByAttributeAndValue( attribute, value );
        return values.isEmpty() || (object != null && values.size() == 1 && object.getAttributeValues().contains( values.get( 0 ) ));
    }

    /**
     * Create new instance of JpaQueryParameters
     * @return JpaQueryParameters<T>
     */
    protected JpaQueryParameters<T> newJpaParameters()
    {
        return new JpaQueryParameters<>();
    }
}
