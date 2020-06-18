package org.hisp.dhis.common.hibernate;

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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * @author bobj
 */
@Slf4j
public class HibernateIdentifiableObjectStore<T extends BaseIdentifiableObject>
    extends HibernateGenericStore<T> implements GenericDimensionalObjectStore<T>, InternalHibernateGenericStore<T>
{
    protected CurrentUserService currentUserService;

    protected AclService aclService;

    protected boolean transientIdentifiableProperties = false;

    public HibernateIdentifiableObjectStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, Class<T> clazz, CurrentUserService currentUserService,
        AclService aclService, boolean cacheable )
    {
        super( sessionFactory, jdbcTemplate, publisher, clazz, cacheable );

        checkNotNull( currentUserService );
        checkNotNull( aclService );

        this.currentUserService = currentUserService;
        this.aclService = aclService;
        this.cacheable = cacheable;
    }

    /**
     * Only used by tests, remove after fixing the tests
     */
    @Deprecated
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    /**
     * Indicates whether the object represented by the implementation does not
     * have persisted identifiable object properties.
     */
    private boolean isTransientIdentifiableProperties()
    {
        return transientIdentifiableProperties;
    }

    // -------------------------------------------------------------------------
    // InternalHibernateGenericStore implementation
    // -------------------------------------------------------------------------

    public final Criteria getDataSharingCriteria()
    {
        return getExecutableCriteria( getDataSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_DATA ) );
    }

    @Override
    public final Criteria getSharingCriteria( User user )
    {
        return getExecutableCriteria( getSharingDetachedCriteria( UserInfo.fromUser( user ), AclService.LIKE_READ_METADATA ) );
    }

    @Override
    public final DetachedCriteria getSharingDetachedCriteria()
    {
        return getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_METADATA );
    }

    @Override
    public final DetachedCriteria getSharingDetachedCriteria( String access )
    {
        return getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access );
    }

    @Override
    public final DetachedCriteria getDataSharingDetachedCriteria( String access )
    {
        return getDataSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access );
    }

    @Override
    public final DetachedCriteria getSharingDetachedCriteria( User user )
    {
        return getSharingDetachedCriteria( UserInfo.fromUser( user ), AclService.LIKE_READ_METADATA );
    }

    @Override
    public final DetachedCriteria getDataSharingDetachedCriteria( User user )
    {
        return getDataSharingDetachedCriteria( UserInfo.fromUser( user ), AclService.LIKE_READ_DATA );
    }

    // -------------------------------------------------------------------------
    // IdentifiableObjectStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void save( T object )
    {
        save( object, true );
    }

    @Override
    public void save( T object, User user )
    {
        save( object, user, true );
    }

    @Override
    public void save( T object, boolean clearSharing )
    {
        save( object, getCurrentUser(), clearSharing );
    }

    private void save( T object, User user, boolean clearSharing )
    {
        String username = user != null ? user.getUsername() : "system-process";

        if ( IdentifiableObject.class.isAssignableFrom( object.getClass() ) )
        {
            object.setAutoFields();

            BaseIdentifiableObject identifiableObject = object;
            identifiableObject.setAutoFields();
            identifiableObject.setLastUpdatedBy( user );

            if ( clearSharing )
            {
                identifiableObject.setPublicAccess( AccessStringHelper.DEFAULT );

                if ( identifiableObject.getUserGroupAccesses() != null )
                {
                    identifiableObject.getUserGroupAccesses().clear();
                }

                if ( identifiableObject.getUserAccesses() != null )
                {
                    identifiableObject.getUserAccesses().clear();
                }
            }

            if ( identifiableObject.getUser() == null )
            {
                identifiableObject.setUser( user );
            }
        }

        if ( user != null && aclService.isShareable( clazz ) )
        {
            BaseIdentifiableObject identifiableObject = object;

            if ( clearSharing )
            {
                if ( aclService.canMakePublic( user, identifiableObject.getClass() ) )
                {
                    if ( aclService.defaultPublic( identifiableObject.getClass() ) )
                    {
                        identifiableObject.setPublicAccess( AccessStringHelper.READ_WRITE );
                    }
                }
                else if ( aclService.canMakePrivate( user, identifiableObject.getClass() ) )
                {
                    identifiableObject.setPublicAccess( AccessStringHelper.newInstance().build() );
                }
            }

            if ( !checkPublicAccess( user, identifiableObject ) )
            {
                AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_CREATE_DENIED );
                throw new CreateAccessDeniedException( object.toString() );
            }
        }

        AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_CREATE );

        getSession().save( object );
    }

    @Override
    public void update( T object )
    {
        update( object, getCurrentUser() );
    }

    @Override
    public void update( T object, User user )
    {
        String username = user != null ? user.getUsername() : "system-process";

        if ( object != null )
        {
            object.setAutoFields();

            object.setAutoFields();
            object.setLastUpdatedBy( user );

            if ( object.getUser() == null )
            {
                object.setUser( user );
            }
        }

        if ( !isUpdateAllowed( object, user ) )
        {
            AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_UPDATE_DENIED );
            throw new UpdateAccessDeniedException( String.valueOf( object ) );
        }

        AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_UPDATE );

        if ( object != null )
        {
            getSession().update( object );
        }
    }

    @Override
    public void delete( T object )
    {
        this.delete( object, getCurrentUser() );
    }

    @Override
    public final void delete( T object, User user )
    {
        String username = user != null ? user.getUsername() : "system-process";

        if ( !isDeleteAllowed( object, user ) )
        {
            AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_DELETE_DENIED );
            throw new DeleteAccessDeniedException( object.toString() );
        }

        AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_DELETE );

        if ( object != null )
        {
            super.delete( object );
        }
    }

    @Override
    public final T get( long id )
    {
        T object = getSession().get( getClazz(), id );

        if ( !isReadAllowed( object, getCurrentUser() ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( object.toString() );
        }

        return postProcessObject( object );
    }

    @Override
    public final List<T> getAll()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, new JpaQueryParameters<T>().addPredicates( getSharingPredicates( builder ) ) );
    }

    @Override
    public int getCount()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) );

        return getCount( builder, param ).intValue();
    }

    @Override
    public final T getByUid( String uid )
    {
        if ( isTransientIdentifiableProperties() )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.equal( root.get( "uid" ), uid ) );

        return getSingleResult( builder, param );
    }

    @Override
    public final T getByUidNoAcl( String uid )
    {
        if ( isTransientIdentifiableProperties() )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicate( root -> builder.equal( root.get( "uid" ), uid ) );

        return getSingleResult( builder, param );
    }

    @Override
    public final void updateNoAcl( T object )
    {
        object.setAutoFields();
        getSession().update( object );
    }

    /**
     * Uses query since name property might not be unique.
     */
    @Override
    public final T getByName( String name )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.equal( root.get( "name" ), name ) );

        List<T> list = getList( builder, param );

        T object = list != null && !list.isEmpty() ? list.get( 0 ) : null;

        if ( !isReadAllowed( object, getCurrentUser() ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( String.valueOf( object ) );
        }

        return object;
    }

    @Override
    public final T getByCode( String code )
    {
        if ( isTransientIdentifiableProperties() )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.equal( root.get( "code" ), code ) );

        return getSingleResult( builder, param );
    }


    @Override
    public T getByUniqueAttributeValue( Attribute attribute, String value )
    {
        if ( attribute == null || StringUtils.isEmpty( value ) || !attribute.isUnique() )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root ->
                  builder.equal(
                    builder.function( FUNCTION_JSONB_EXTRACT_PATH_TEXT, String.class, root.get( "attributeValues" ), builder.literal( attribute.getUid() ),  builder.literal( "value" ) ) , value ) );

        return getSingleResult( builder, param );
    }

    @Override
    public T getByUniqueAttributeValue( Attribute attribute, String value, UserInfo userInfo )
    {
        if ( attribute == null || StringUtils.isEmpty( value ) || !attribute.isUnique() )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder, userInfo ) )
            .addPredicate( root ->
                builder.equal(
                    builder.function( FUNCTION_JSONB_EXTRACT_PATH_TEXT, String.class, root.get( "attributeValues" ), builder.literal( attribute.getUid() ),  builder.literal( "value" ) ) , value ) );

        return getSingleResult( builder, param );
    }

    @Override
    public List<T> getAllEqName( String name )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.equal( root.get( "name" ), name ) )
            .addOrder( root -> builder.asc( root.get( "name" ) ) );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllLikeName( String name )
    {
        return getAllLikeName( name, true );
    }

    @Override
    public List<T> getAllLikeName( String name, boolean caseSensitive )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        Function<Root<T>, Predicate> likePredicate;

        if ( caseSensitive )
        {
            likePredicate = root -> builder.like( root.get( "name" ), "%" + name + "%" );
        }
        else
        {
            likePredicate = root -> builder.like( builder.lower( root.get( "name" ) ), "%" + name.toLowerCase() + "%" );
        }

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( likePredicate )
            .addOrder( root -> builder.asc( root.get( "name" ) ) );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllLikeName( String name, int first, int max )
    {
        return getAllLikeName( name, first, max, true );
    }

    @Override
    public List<T> getAllLikeName( String name, int first, int max, boolean caseSensitive )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        Function<Root<T>, Predicate> likePredicate;

        if ( caseSensitive )
        {
            likePredicate = root -> builder.like( root.get( "name" ), "%" + name + "%" );
        }
        else
        {
            likePredicate = root -> builder.like( builder.lower( root.get( "name" ) ), "%" + name.toLowerCase() + "%" );
        }

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( likePredicate )
            .addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first )
            .setMaxResults( max );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllLikeName( Set<String> nameWords, int first, int max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first )
            .setMaxResults( max );

        if ( nameWords.isEmpty() )
        {
            return getList( builder, param );
        }

        List<Function<Root<T>, Predicate>> conjunction = new ArrayList<>();

        for ( String word : nameWords )
        {
            conjunction.add( root -> builder.like( builder.lower( root.get( "name" ) ), "%" + word.toLowerCase() + "%" ) );
        }

        param.addPredicate( root -> builder.and( conjunction.stream().map( p -> p.apply( root ) ).collect( Collectors.toList() ).toArray( new Predicate[0] ) ) );

        return getList( builder, param );
    }

    public List<T> getAllLikeNameAndEqualsAttribute( Set<String> nameWords, String attribute, String attributeValue, int first, int max )
    {
        if ( StringUtils.isEmpty( attribute ) || StringUtils.isEmpty( attributeValue ) )
        {
            return new ArrayList<>();
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first )
            .setMaxResults( max );

        if ( nameWords.isEmpty() )
        {
            return getList( builder, param );
        }

        List<Function<Root<T>, Predicate>> conjunction = new ArrayList<>();

        for ( String word : nameWords )
        {
            conjunction.add( root -> builder.like( builder.lower( root.get( "name" ) ), "%" + word.toLowerCase() + "%" ) );
        }

        conjunction.add( root -> builder.equal( builder.lower( root.get( attribute ) ), attributeValue ) );

        param.addPredicate( root -> builder.and( conjunction.stream().map( p -> p.apply( root ) ).collect( Collectors.toList() ).toArray( new Predicate[0] ) ) );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllOrderedName()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addOrder( root -> builder.asc( root.get( "name" ) ) );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllOrderedName( int first, int max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first )
            .setMaxResults( max );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllOrderedLastUpdated( int first, int max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addOrder( root -> builder.asc( root.get( "lastUpdated" ) ) );

        return getList( builder, param );
    }

    @Override
    public int getCountLikeName( String name )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.like( builder.lower( root.get( "name" ) ), "%" + name.toLowerCase() + "%" ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) );

        return getCount( builder, param ).intValue();
    }

    @Override
    public int getCountGeLastUpdated( Date lastUpdated )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "lastUpdated" ), lastUpdated ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) );

        return getCount( builder, param ).intValue();
    }

    @Override
    public List<T> getAllGeLastUpdated( Date lastUpdated )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "lastUpdated" ), lastUpdated ) )
            .addOrder( root -> builder.desc( root.get( "lastUpdated" ) ) );

        return getList( builder, param );
    }

    @Override
    public int getCountGeCreated( Date created )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "created" ), created ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) );

        return getCount( builder, param ).intValue();
    }

    @Override
    public List<T> getAllGeCreated( Date created )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "created" ), created ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        return getList( builder, param );
    }

    @Override
    public List<T> getAllLeCreated( Date created )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> param = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.lessThanOrEqualTo( root.get( "created" ), created ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        return getList( builder, param );
    }

    @Override
    public Date getLastUpdated()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<Date> query = builder.createQuery( Date.class );

        Root<T> root = query.from( getClazz() );

        query.select( root.get( "lastUpdated" ) );

        query.orderBy( builder.desc( root.get( "lastUpdated" ) ) );

        TypedQuery<Date> typedQuery = getSession().createQuery( query );

        typedQuery.setMaxResults( 1 );

        typedQuery.setHint( JpaQueryUtils.HIBERNATE_CACHEABLE_HINT, true );

        return getSingleResult( typedQuery );
    }

    @Override
    public List<T> getByDataDimension( boolean dataDimension )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicate( root -> builder.equal( root.get( "dataDimension" ), dataDimension ) )
            .addPredicates( getSharingPredicates( builder ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByDataDimensionNoAcl( boolean dataDimension )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicate( root -> builder.equal( root.get( "dataDimension" ), dataDimension ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getById( Collection<Long> ids )
    {
        if ( ids == null || ids.isEmpty() )
        {
            return new ArrayList<>();
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> root.get( "id" ).in( ids ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByUid( Collection<String> uids )
    {
        if ( uids == null || uids.isEmpty() )
        {
            return new ArrayList<>();
        }

        //TODO Include paging to avoid exceeding max query length

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> root.get( "uid" ).in( uids ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByUid( Collection<String> uids, User user )
    {
        if ( uids == null || uids.isEmpty() )
        {
            return new ArrayList<>();
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder, user ) )
            .addPredicate( root -> root.get( "uid" ).in( uids ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByUidNoAcl( Collection<String> uids )
    {
        if ( uids == null || uids.isEmpty() )
        {
            return new ArrayList<>();
        }

        List<T> objects = Lists.newArrayList();

        List<List<String>> partitions = Lists.partition( Lists.newArrayList( uids ), OBJECT_FETCH_SIZE );

        for ( List<String> partition : partitions )
        {
            objects.addAll( getByUidNoAclInternal( partition ) );
        }

        return objects;
    }

    private List<T> getByUidNoAclInternal( Collection<String> uids )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicate( root -> root.get( "uid" ).in( uids ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByCode( Collection<String> codes )
    {
        if ( codes == null || codes.isEmpty() )
        {
            return new ArrayList<>();
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> root.get( "code" ).in( codes ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByCode( Collection<String> codes, User user )
    {
        if ( codes == null || codes.isEmpty() )
        {
            return new ArrayList<>();
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder, user ) )
            .addPredicate( root -> root.get( "code" ).in( codes ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getByName( Collection<String> names )
    {
        if ( names == null || names.isEmpty() )
        {
            return new ArrayList<>();
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> jpaQueryParameters = new JpaQueryParameters<T>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> root.get( "name" ).in( names ) );

        return getList( builder, jpaQueryParameters );
    }

    @Override
    public List<T> getAllNoAcl()
    {
        return super.getAll();
    }

    @Override
    public List<String> getUidsCreatedBefore( Date date )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<String> query = builder.createQuery( String.class );

        Root<T> root = query.from( getClazz() );

        query.select( root.get( "uid" ) );
        query.where( builder.lessThan( root.get( "created" ), date ) );

        TypedQuery<String> typedQuery = getSession().createQuery( query );
        typedQuery.setHint( JpaQueryUtils.HIBERNATE_CACHEABLE_HINT, true );

        return typedQuery.getResultList();
    }

    //----------------------------------------------------------------------------------------------------------------
    // Data sharing
    //----------------------------------------------------------------------------------------------------------------

    @Override
    public final List<T> getDataReadAll()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> parameters = new JpaQueryParameters<T>()
            .addPredicates( getDataSharingPredicates( builder ) );

        return getList( builder, parameters );
    }

    @Override
    public final List<T> getDataReadAll( User user )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> parameters = new JpaQueryParameters<T>()
            .addPredicates( getDataSharingPredicates( builder, user ) );

        return getList( builder, parameters );
    }

    @Override
    public final List<T> getDataWriteAll()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> parameters = new JpaQueryParameters<T>()
            .addPredicates( getDataSharingPredicates( builder, AclService.LIKE_WRITE_DATA ) );

        return getList( builder, parameters );
    }

    @Override
    public final List<T> getDataWriteAll( User user )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> parameters = new JpaQueryParameters<T>()
            .addPredicates( getDataSharingPredicates( builder, user, AclService.LIKE_WRITE_DATA ) );

        return getList( builder, parameters );
    }

    @Override
    public final List<T> getDataReadAll( int first, int max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<T> parameters = new JpaQueryParameters<T>()
            .addPredicates( getDataSharingPredicates( builder ) )
            .setFirstResult( first )
            .setMaxResults( max );

        return getList( builder, parameters );
    }

    //----------------------------------------------------------------------------------------------------------------
    // Supportive methods
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a criteria with sharing restrictions relative to the given
     * user and access string.
     */
    @Override
    public final Criteria getSharingCriteria()
    {
        return getExecutableCriteria( getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_METADATA ) );
    }

    /**
     * Creates a detached criteria with data sharing restrictions relative to the
     * given user and access string.
     *
     * @param user the user.
     * @param access the access string.
     * @return a DetachedCriteria.
     */
    private DetachedCriteria getDataSharingDetachedCriteria( UserInfo user, String access )
    {
        DetachedCriteria criteria = DetachedCriteria.forClass( getClazz(), "c" );

        if ( user == null || !dataSharingEnabled( user ) )
        {
            return criteria;
        }

        Assert.notNull( user, "User argument can't be null." );

        Disjunction disjunction = Restrictions.disjunction();

        disjunction.add( Restrictions.like( "c.publicAccess", access ) );
        disjunction.add( Restrictions.isNull( "c.publicAccess" ) );

        DetachedCriteria userGroupDetachedCriteria = DetachedCriteria.forClass( getClazz(), "ugdc" );
        userGroupDetachedCriteria.createCriteria( "ugdc.userGroupAccesses", "uga" );
        userGroupDetachedCriteria.createCriteria( "uga.userGroup", "ug" );
        userGroupDetachedCriteria.createCriteria( "ug.members", "ugm" );

        userGroupDetachedCriteria.add( Restrictions.eqProperty( "ugdc.id", "c.id" ) );
        userGroupDetachedCriteria.add( Restrictions.eq( "ugm.id", user.getId() ) );
        userGroupDetachedCriteria.add( Restrictions.like( "uga.access", access ) );

        userGroupDetachedCriteria.setProjection( Property.forName( "uga.id" ) );

        disjunction.add( Subqueries.exists( userGroupDetachedCriteria ) );

        DetachedCriteria userDetachedCriteria = DetachedCriteria.forClass( getClazz(), "udc" );
        userDetachedCriteria.createCriteria( "udc.userAccesses", "ua" );
        userDetachedCriteria.createCriteria( "ua.user", "u" );

        userDetachedCriteria.add( Restrictions.eqProperty( "udc.id", "c.id" ) );
        userDetachedCriteria.add( Restrictions.eq( "u.id", user.getId() ) );
        userDetachedCriteria.add( Restrictions.like( "ua.access", access ) );

        userDetachedCriteria.setProjection( Property.forName( "ua.id" ) );

        disjunction.add( Subqueries.exists( userDetachedCriteria ) );

        criteria.add( disjunction );

        return criteria;
    }

    /**
     * Creates a detached criteria with sharing restrictions relative to the given
     * user and access string.
     *
     * @param user   the user.
     * @param access the access string.
     * @return a DetachedCriteria.
     */
    private DetachedCriteria getSharingDetachedCriteria( UserInfo user, String access )
    {
        DetachedCriteria criteria = DetachedCriteria.forClass( getClazz(), "c" );

        preProcessDetachedCriteria( criteria );

        if ( !sharingEnabled( user ) || user == null )
        {
            return criteria;
        }

        Assert.notNull( user, "User argument can't be null." );

        Disjunction disjunction = Restrictions.disjunction();

        disjunction.add( Restrictions.like( "c.publicAccess", access ) );
        disjunction.add( Restrictions.isNull( "c.publicAccess" ) );
        disjunction.add( Restrictions.isNull( "c.user.id" ) );
        disjunction.add( Restrictions.eq( "c.user.id", user.getId() ) );

        DetachedCriteria userGroupDetachedCriteria = DetachedCriteria.forClass( getClazz(), "ugdc" );
        userGroupDetachedCriteria.createCriteria( "ugdc.userGroupAccesses", "uga" );
        userGroupDetachedCriteria.createCriteria( "uga.userGroup", "ug" );
        userGroupDetachedCriteria.createCriteria( "ug.members", "ugm" );

        userGroupDetachedCriteria.add( Restrictions.eqProperty( "ugdc.id", "c.id" ) );
        userGroupDetachedCriteria.add( Restrictions.eq( "ugm.id", user.getId() ) );
        userGroupDetachedCriteria.add( Restrictions.like( "uga.access", access ) );

        userGroupDetachedCriteria.setProjection( Property.forName( "uga.id" ) );

        disjunction.add( Subqueries.exists( userGroupDetachedCriteria ) );

        DetachedCriteria userDetachedCriteria = DetachedCriteria.forClass( getClazz(), "udc" );
        userDetachedCriteria.createCriteria( "udc.userAccesses", "ua" );
        userDetachedCriteria.createCriteria( "ua.user", "u" );

        userDetachedCriteria.add( Restrictions.eqProperty( "udc.id", "c.id" ) );
        userDetachedCriteria.add( Restrictions.eq( "u.id", user.getId() ) );
        userDetachedCriteria.add( Restrictions.like( "ua.access", access ) );

        userDetachedCriteria.setProjection( Property.forName( "ua.id" ) );

        disjunction.add( Subqueries.exists( userDetachedCriteria ) );

        criteria.add( disjunction );

        return criteria;
    }

    // ----------------------------------------------------------------------
    // JPA support methods
    // ----------------------------------------------------------------------

    @Override
    public final List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder )
    {
        return getDataSharingPredicates( builder, currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_DATA );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, UserInfo user )
    {
        return getDataSharingPredicates( builder, user, AclService.LIKE_READ_DATA );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User user )
    {
        return getDataSharingPredicates( builder, UserInfo.fromUser( user ), AclService.LIKE_READ_DATA );
    }

    @Override
    public final List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, String access )
    {
        return getDataSharingPredicates( builder, currentUserService.getCurrentUserInfo(), access );
    }

    @Override
    public final List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User user, String access )
    {
        return getDataSharingPredicates( builder, UserInfo.fromUser( user ), access );
    }

    @Override
    public final List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder )
    {
        return getSharingPredicates( builder, currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_METADATA );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, UserInfo user )
    {
        return getSharingPredicates( builder, user, AclService.LIKE_READ_METADATA );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User user, String access )
    {
        return getSharingPredicates( builder, UserInfo.fromUser( user ), access );
    }

    /**
     * Get sharing predicates based on given user and AclService.LIKE_READ_METADATA
     *
     * @param builder CriteriaBuilder
     * @param user    User
     * @return List of Function<Root<T>, Predicate>
     */
    @Override
    public List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User user )
    {
        return getSharingPredicates( builder, user, AclService.LIKE_READ_METADATA );
    }

    /**
     * Get sharing predicates based on Access string and current user
     *
     * @param builder CriteriaBuilder
     * @param access  Access String
     * @return List of Function<Root<T>, Predicate>
     */
    @Override
    public final List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, String access )
    {
        return getSharingPredicates( builder, currentUserService.getCurrentUserInfo(), access );
    }

    /**
     * Get sharing predicates based on given UserInfo and Access String
     *
     * @param builder CriteriaBuilder
     * @param user    UserInfo
     * @param access  Access String
     * @return List of Function<Root<T>, Predicate>
     */
    @Override
    public List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, UserInfo user, String access )
    {
        List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

        CriteriaQuery<T> criteria = builder.createQuery( getClazz() );

        preProcessPredicates( builder, predicates );

        if ( !sharingEnabled( user ) || user == null )
        {
            return predicates;
        }

        Function<Root<T>, Subquery<Integer>> userGroupPredicate = (( Root<T> root ) -> {
            Subquery<Integer> userGroupSubQuery = criteria.subquery( Integer.class );
            Root<T> ugdc = userGroupSubQuery.from( getClazz() );
            Join<T, UserGroupAccess> uga = ugdc.join( "userGroupAccesses" );
            userGroupSubQuery.select( uga.get( "id" ) );

            return userGroupSubQuery.where(
                builder.and(
                    builder.equal( root.get( "id" ), ugdc.get( "id" ) ),
                    builder.equal( uga.join( "userGroup" ).join( "members" ).get( "id" ), user.getId() ),
                    builder.like( uga.get( "access" ), access ) ) );
        });

        Function<Root<T>, Subquery<Integer>> userPredicate = (root -> {
            Subquery<Integer> userSubQuery = criteria.subquery( Integer.class );
            Root<T> udc = userSubQuery.from( getClazz() );
            Join<T, UserAccess> ua = udc.join( "userAccesses" );
            userSubQuery.select( ua.get( "id" ) );

            return userSubQuery.where(
                builder.and(
                    builder.equal( root.get( "id" ), udc.get( "id" ) ),
                    builder.equal( ua.get( "user" ).get( "id" ), user.getId() ),
                    builder.like( ua.get( "access" ), access ) ) );
        });

        predicates.add( root -> builder.or(
            builder.like( root.get( "publicAccess" ), access ),
            builder.isNull( root.get( "publicAccess" ) ),
            builder.isNull( root.get( "user" ) ),
            builder.equal( root.get( "user" ).get( "id" ), user.getId() ),
            builder.exists( userGroupPredicate.apply( root ) ),
            builder.exists( userPredicate.apply( root ) ) ) );

        return predicates;
    }

    @Override
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, UserInfo user, String access )
    {
        List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

        CriteriaQuery<T> criteria = builder.createQuery( getClazz() );

        preProcessPredicates( builder, predicates );

        if ( !dataSharingEnabled( user ) || user == null )
        {
            return predicates;
        }

        Function<Root<T>, Subquery<Integer>> userGroupPredicate = (( Root<T> root ) -> {
            Subquery<Integer> userGroupSubQuery = criteria.subquery( Integer.class );
            Root<T> ugdc = userGroupSubQuery.from( getClazz() );
            Join<T, UserGroupAccess> uga = ugdc.join( "userGroupAccesses" );
            userGroupSubQuery.select( uga.get( "id" ) );

            return userGroupSubQuery.where(
                builder.and(
                    builder.equal( root.get( "id" ), ugdc.get( "id" ) ),
                    builder.equal( uga.join( "userGroup" ).join( "members" ).get( "id" ), user.getId() ),
                    builder.like( uga.get( "access" ), access ) ) );
        });

        Function<Root<T>, Subquery<Integer>> userPredicate = (root -> {
            Subquery<Integer> userSubQuery = criteria.subquery( Integer.class );
            Root<T> udc = userSubQuery.from( getClazz() );
            Join<T, UserAccess> ua = udc.join( "userAccesses" );
            userSubQuery.select( ua.get( "id" ) );

            return userSubQuery.where(
                builder.and(
                    builder.equal( root.get( "id" ), udc.get( "id" ) ),
                    builder.equal( ua.get( "user" ).get( "id" ), user.getId() ),
                    builder.like( ua.get( "access" ), access ) ) );
        });

        predicates.add( root -> builder.or(
            builder.like( root.get( "publicAccess" ), access ),
            builder.isNull( root.get( "publicAccess" ) ),
            builder.exists( userGroupPredicate.apply( root ) ),
            builder.exists( userPredicate.apply( root ) ) ) );

        return predicates;
    }

    // ----------------------------------------------------------------------
    // JPA Implementations
    // ----------------------------------------------------------------------

    /**
     * Checks whether the given user has public access to the given identifiable object.
     *
     * @param user the user.
     * @param identifiableObject the identifiable object.
     * @return true or false.
     */
    private boolean checkPublicAccess( User user, IdentifiableObject identifiableObject )
    {
        return aclService.canMakePublic( user, identifiableObject.getClass() ) ||
            (aclService.canMakePrivate( user, identifiableObject.getClass() ) &&
                !AccessStringHelper.canReadOrWrite( identifiableObject.getPublicAccess() ));
    }

    private boolean forceAcl()
    {
        return Dashboard.class.isAssignableFrom( clazz );
    }

    private boolean sharingEnabled( User user )
    {
        return forceAcl() || (aclService.isShareable( clazz ) && !(user == null || user.isSuper()));
    }

    private boolean sharingEnabled( UserInfo userInfo )
    {
        return forceAcl() || (aclService.isShareable( clazz ) && !(userInfo == null || userInfo.isSuper()));
    }

    private boolean dataSharingEnabled( UserInfo userInfo )
    {
        return aclService.isDataShareable( clazz ) && !userInfo.isSuper();
    }

    private boolean isReadAllowed( T object, User user )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject idObject = object;

            if ( sharingEnabled( user ) )
            {
                return aclService.canRead( user, idObject );
            }
        }

        return true;
    }

    private boolean isUpdateAllowed( T object, User user )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject idObject = object;

            if ( aclService.isShareable( clazz ) )
            {
                return aclService.canUpdate( user, idObject );
            }
        }

        return true;
    }

    private boolean isDeleteAllowed( T object, User user )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject idObject = object;

            if ( aclService.isShareable( clazz ) )
            {
                return aclService.canDelete( user, idObject );
            }
        }

        return true;
    }

    public void flush()
    {
        getSession().flush();
    }

    private User getCurrentUser()
    {
        UserCredentials userCredentials = currentUserService.getCurrentUserCredentials();

        if ( userCredentials != null )
        {
            return userCredentials.getUserInfo();
        }

        return null;
    }
}
