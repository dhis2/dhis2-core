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
package org.hisp.dhis.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.QueryHints;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.hibernate.jsonb.type.JsonAttributeValueBinaryType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class HibernateGenericStore<T>
    implements GenericStore<T>
{
    public static final String FUNCTION_JSONB_EXTRACT_PATH = "jsonb_extract_path";

    public static final String FUNCTION_JSONB_EXTRACT_PATH_TEXT = "jsonb_extract_path_text";

    protected static final int OBJECT_FETCH_SIZE = 2000;

    protected SessionFactory sessionFactory;

    protected JdbcTemplate jdbcTemplate;

    protected ApplicationEventPublisher publisher;

    protected Class<T> clazz;

    protected boolean cacheable;

    public HibernateGenericStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, Class<T> clazz, boolean cacheable )
    {
        checkNotNull( sessionFactory );
        checkNotNull( jdbcTemplate );
        checkNotNull( publisher );
        checkNotNull( clazz );

        this.sessionFactory = sessionFactory;
        this.jdbcTemplate = jdbcTemplate;
        this.publisher = publisher;
        this.clazz = clazz;
        this.cacheable = cacheable;
    }

    /**
     * Could be overridden programmatically.
     */
    @Override
    public Class<T> getClazz()
    {
        return clazz;
    }

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

    protected final StatelessSession getStatelessSession()
    {
        return sessionFactory.openStatelessSession();
    }

    /**
     * Creates a Query for given HQL query string. Return type is casted to
     * generic type T of the Store class.
     *
     * @param hql the HQL query.
     * @return a Query instance with return type is the object type T of the
     *         store class
     */
    @SuppressWarnings( "unchecked" )
    protected final Query<T> getQuery( String hql )
    {
        return getSession()
            .createQuery( hql )
            .setCacheable( cacheable ).setHint( QueryHints.CACHEABLE, cacheable );
    }

    protected final <C> Query<C> getQuery( String hql, Class<C> customClass )
    {
        return getSession().createQuery( hql, customClass )
            .setCacheable( cacheable ).setHint( QueryHints.CACHEABLE, cacheable );
    }

    /**
     * Creates a Query for given HQL query string. Must specify the return type
     * of the Query variable.
     *
     * @param hql the HQL query.
     * @return a Query instance with return type specified in the Query<Y>
     */
    @SuppressWarnings( "unchecked" )
    protected final <V> Query<V> getTypedQuery( String hql )
    {
        return getSession()
            .createQuery( hql )
            .setCacheable( cacheable ).setHint( QueryHints.CACHEABLE, cacheable );
    }

    /**
     * Creates a Criteria for the implementation Class type.
     * <p>
     * Please note that sharing is not considered.
     *
     * @return a Criteria instance.
     */
    @Deprecated
    public final Criteria getCriteria()
    {
        DetachedCriteria criteria = DetachedCriteria.forClass( getClazz() );

        preProcessDetachedCriteria( criteria );

        return getExecutableCriteria( criteria );
    }

    /**
     * Override to add additional restrictions to criteria before it is invoked.
     */
    protected void preProcessDetachedCriteria( DetachedCriteria detachedCriteria )
    {
    }

    public final Criteria getExecutableCriteria( DetachedCriteria detachedCriteria )
    {
        return detachedCriteria.getExecutableCriteria( getSession() )
            .setCacheable( cacheable );
    }

    public CriteriaBuilder getCriteriaBuilder()
    {
        return sessionFactory.getCriteriaBuilder();
    }

    // ------------------------------------------------------------------------------------------
    // JPA Methods
    // ------------------------------------------------------------------------------------------

    /**
     * Get executable Typed Query from Criteria Query. Apply cache if needed.
     *
     * @return executable TypedQuery
     */
    private TypedQuery<T> getExecutableTypedQuery( CriteriaQuery<T> criteriaQuery )
    {
        return getSession()
            .createQuery( criteriaQuery )
            .setCacheable( cacheable ).setHint( QueryHints.CACHEABLE, cacheable );
    }

    /**
     * Method for adding additional Predicates into where clause
     *
     */
    protected void preProcessPredicates( CriteriaBuilder builder, List<Function<Root<T>, Predicate>> predicates )
    {
    }

    /**
     * Get single result from executable typedQuery
     *
     * @param typedQuery TypedQuery
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
     * Get List objects returned by executable TypedQuery
     *
     * @return list result
     */
    protected final List<T> getList( TypedQuery<T> typedQuery )
    {
        return typedQuery.getResultList();
    }

    /**
     * Get List objects return by querying given JpaQueryParameters with
     * Pagination
     *
     * @param parameters JpaQueryParameters
     * @return list objects
     */
    protected final List<T> getList( CriteriaBuilder builder, JpaQueryParameters<T> parameters )
    {
        return getTypedQuery( builder, parameters ).getResultList();
    }

    protected final <V> List<T> getListFromPartitions( CriteriaBuilder builder, Collection<V> values, int partitionSize,
        Function<Collection<V>, JpaQueryParameters<T>> createPartitionParams )
    {
        if ( values == null || values.isEmpty() )
        {
            return new ArrayList<>( 0 );
        }
        if ( values.size() <= partitionSize )
        {
            // fast path: avoid aggregation collection
            return getList( builder, createPartitionParams.apply( values ) );
        }

        List<List<V>> partitionedValues = Lists.partition( new ArrayList<>( values ), partitionSize );
        List<T> aggregate = new ArrayList<>();
        for ( List<V> valuesPartition : partitionedValues )
        {
            aggregate.addAll( getList( builder, createPartitionParams.apply( valuesPartition ) ) );
        }
        return aggregate;
    }

    /**
     * Get executable TypedQuery from JpaQueryParameter.
     *
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
            List<Predicate> predicates = predicateProviders.stream().map( t -> t.apply( root ) )
                .collect( Collectors.toList() );
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

        return typedQuery
            .setHint( QueryHints.CACHEABLE, parameters.isCacheable( cacheable ) );
    }

    /**
     * Count number of objects based on given parameters
     *
     * @param parameters JpaQueryParameters
     * @return number of objects
     */
    protected final Long getCount( CriteriaBuilder builder, JpaQueryParameters<T> parameters )
    {
        CriteriaQuery<Long> query = builder.createQuery( Long.class );

        Root<T> root = query.from( getClazz() );

        List<Function<Root<T>, Predicate>> predicateProviders = parameters.getPredicates();

        List<Function<Root<T>, Expression<Long>>> countExpressions = parameters.getCountExpressions();

        if ( !countExpressions.isEmpty() )
        {
            if ( countExpressions.size() > 1 )
            {
                query.multiselect(
                    countExpressions.stream().map( c -> c.apply( root ) ).collect( Collectors.toList() ) );
            }
            else
            {
                query.select( countExpressions.get( 0 ).apply( root ) );
            }
        }
        else
        {
            query.select( parameters.isUseDistinct() ? builder.countDistinct( root ) : builder.count( root ) );
        }

        if ( !predicateProviders.isEmpty() )
        {
            List<Predicate> predicates = predicateProviders.stream().map( t -> t.apply( root ) )
                .collect( Collectors.toList() );
            query.where( predicates.toArray( new Predicate[0] ) );
        }

        return getSession().createQuery( query )
            .setHint( QueryHints.CACHEABLE, parameters.isCacheable( cacheable ) )
            .getSingleResult();
    }

    /**
     * Retrieves an object based on the given Jpa Predicates.
     *
     * @return an object of the implementation Class type.
     */
    protected T getSingleResult( CriteriaBuilder builder, JpaQueryParameters<T> parameters )
    {
        return getSingleResult( getTypedQuery( builder, parameters ) );
    }

    // ------------------------------------------------------------------------------------------
    // End JPA Methods
    // ------------------------------------------------------------------------------------------

    /**
     * Creates a SqlQuery.
     *
     * @param sql the SQL query String.
     * @return a NativeQuery<T> instance.
     */
    @SuppressWarnings( "unchecked" )
    protected final NativeQuery<T> getSqlQuery( String sql )
    {
        return getSession().createNativeQuery( sql )
            .setCacheable( cacheable ).setHint( QueryHints.CACHEABLE, cacheable );
    }

    /**
     * Creates a untyped SqlQuery.
     *
     * @param sql the SQL query String.
     * @return a NativeQuery<T> instance.
     */
    protected final NativeQuery<?> getUntypedSqlQuery( String sql )
    {
        return getSession().createNativeQuery( sql )
            .setCacheable( cacheable ).setHint( QueryHints.CACHEABLE, cacheable );
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
        if ( !ObjectDeletionRequestedEvent.shouldSkip( HibernateProxyUtils.getRealClass( object ) ) )
        {
            publisher.publishEvent( new ObjectDeletionRequestedEvent( object ) );
        }

        getSession().delete( object );
    }

    @Override
    public T get( long id )
    {
        T object = getSession().get( getClazz(), id );

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
        return getList( getCriteriaBuilder(), new JpaQueryParameters<>() );
    }

    @Override
    public List<T> getAllByAttributes( List<Attribute> attributes )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<T> query = builder.createQuery( getClazz() );
        Root<T> root = query.from( getClazz() );
        query.select( root ).distinct( true );

        List<Predicate> predicates = attributes.stream()
            .map( attribute -> builder.isNotNull(
                builder.function( FUNCTION_JSONB_EXTRACT_PATH, String.class, root.get( "attributeValues" ),
                    builder.literal( attribute.getUid() ) ) ) )
            .collect( Collectors.toList() );

        query.where( builder.or( predicates.toArray( new Predicate[predicates.size()] ) ) );

        return getSession().createQuery( query ).list();
    }

    @Override
    public List<AttributeValue> getAllValuesByAttributes( List<Attribute> attributes )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<String> query = builder.createQuery( String.class );
        Root<T> root = query.from( getClazz() );

        CriteriaBuilder.Coalesce<String> coalesce = builder.coalesce();
        attributes.stream().forEach( attribute -> coalesce.value(
            builder.function( FUNCTION_JSONB_EXTRACT_PATH, String.class, root.get( "attributeValues" ),
                builder.literal( attribute.getUid() ) ) ) );

        query.select( coalesce );

        List<Predicate> predicates = attributes.stream()
            .map( attribute -> builder.isNotNull(
                builder.function( FUNCTION_JSONB_EXTRACT_PATH, String.class, root.get( "attributeValues" ),
                    builder.literal( attribute.getUid() ) ) ) )
            .collect( Collectors.toList() );

        query.where( builder.or( predicates.toArray( new Predicate[predicates.size()] ) ) );

        List<String> result = getSession().createQuery( query ).list();

        return convertListJsonToListObject( JsonAttributeValueBinaryType.MAPPER, result, AttributeValue.class );
    }

    @Override
    public long countAllValuesByAttributes( List<Attribute> attributes )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<Long> query = builder.createQuery( Long.class );
        Root<T> root = query.from( getClazz() );
        query.select( builder.countDistinct( root ) );

        List<Predicate> predicates = attributes.stream()
            .map( attribute -> builder.isNotNull(
                builder.function( FUNCTION_JSONB_EXTRACT_PATH, String.class, root.get( "attributeValues" ),
                    builder.literal( attribute.getUid() ) ) ) )
            .collect( Collectors.toList() );

        query.where( builder.or( predicates.toArray( new Predicate[predicates.size()] ) ) );

        return getSession().createQuery( query )
            .getSingleResult();
    }

    @Override
    public List<AttributeValue> getAttributeValueByAttribute( Attribute attribute )
    {
        return getAllValuesByAttributes( Lists.newArrayList( attribute ) );
    }

    @Override
    public int getCount()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getCount( builder, newJpaParameters().count( root -> builder.countDistinct( root.get( "id" ) ) ) )
            .intValue();
    }

    @Override
    public List<T> getByAttribute( Attribute attribute )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery( getClazz() );

        Root<T> root = query.from( getClazz() );

        query.select( root );
        query.where( builder.function( FUNCTION_JSONB_EXTRACT_PATH, String.class, root.get( "attributeValues" ),
            builder.literal( attribute.getUid() ) ).isNotNull() );

        return getSession().createQuery( query ).list();
    }

    @Override
    public List<T> getByAttributeAndValue( Attribute attribute, String value )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<T> query = builder.createQuery( getClazz() );
        Root<T> root = query.from( getClazz() );
        query.select( root );
        query.where( builder.equal(
            builder.function( FUNCTION_JSONB_EXTRACT_PATH_TEXT, String.class, root.get( "attributeValues" ),
                builder.literal( attribute.getUid() ), builder.literal( "value" ) ),
            value ) );
        return getSession().createQuery( query ).list();
    }

    @Override
    public List<AttributeValue> getAttributeValueByAttributeAndValue( Attribute attribute, String value )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<String> query = builder.createQuery( String.class );
        Root<T> root = query.from( getClazz() );

        query.select( builder.function( FUNCTION_JSONB_EXTRACT_PATH, String.class, root.get( "attributeValues" ),
            builder.literal( attribute.getUid() ) ) );

        query.where( builder.equal(
            builder.function( FUNCTION_JSONB_EXTRACT_PATH_TEXT, String.class, root.get( "attributeValues" ),
                builder.literal( attribute.getUid() ), builder.literal( "value" ) ),
            value ) );

        List<String> result = getSession().createQuery( query ).list();

        return convertListJsonToListObject( JsonAttributeValueBinaryType.MAPPER, result, AttributeValue.class );
    }

    @Override
    public List<T> getByAttributeValue( AttributeValue attributeValue )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<T> query = builder.createQuery( getClazz() );
        Root<T> root = query.from( getClazz() );
        query.select( root );
        query.where( builder.equal(
            builder.function( FUNCTION_JSONB_EXTRACT_PATH_TEXT, String.class, root.get( "attributeValues" ),
                builder.literal( attributeValue.getAttribute().getUid() ), builder.literal( "value" ) ),
            attributeValue.getValue() ) );
        return getSession().createQuery( query ).list();
    }

    @Override
    public <P extends IdentifiableObject> boolean isAttributeValueUnique( P object, AttributeValue attributeValue )
    {
        List<T> objects = getByAttributeValue( attributeValue );
        return objects.isEmpty() || (object != null && objects.size() == 1 && object.equals( objects.get( 0 ) ));
    }

    @Override
    public <P extends IdentifiableObject> boolean isAttributeValueUnique( P object, Attribute attribute, String value )
    {
        List<T> objects = getByAttributeAndValue( attribute, value );
        return objects.isEmpty() || (object != null && objects.size() == 1 && object.equals( objects.get( 0 ) ));
    }

    @Override
    public List<T> getAllByAttributeAndValues( Attribute attribute, List<String> values )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<T> query = builder.createQuery( getClazz() );
        Root<T> root = query.from( getClazz() );
        query.select( root );
        query.where( builder.function( FUNCTION_JSONB_EXTRACT_PATH_TEXT, String.class, root.get( "attributeValues" ),
            builder.literal( attribute.getUid() ), builder.literal( "value" ) ).in( values ) );

        return getSession().createQuery( query ).list();
    }

    /**
     * Create new instance of JpaQueryParameters
     *
     * @return JpaQueryParameters<T>
     */
    protected JpaQueryParameters<T> newJpaParameters()
    {
        return new JpaQueryParameters<>();
    }

    /**
     * Convert List of Json String object into List of given klass
     *
     * @param mapper Object mapper that is configured for given klass
     * @param content List of Json String
     * @param klass Class for converting to
     * @param <T>
     * @return List of converted Object
     */
    public static <T> List<T> convertListJsonToListObject( ObjectMapper mapper, List<String> content, Class<T> klass )
    {
        return content.stream().map( json -> {
            try
            {
                return mapper.readValue( json, klass );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        } ).collect( Collectors.toList() );
    }
}
