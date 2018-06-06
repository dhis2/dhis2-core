package org.hisp.dhis.common.hibernate;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author bobj
 */
public class HibernateIdentifiableObjectStore<T extends BaseIdentifiableObject>
    extends HibernateGenericStore<T> implements GenericDimensionalObjectStore<T>, InternalHibernateGenericStore<T>
{
    private static final Log log = LogFactory.getLog( HibernateIdentifiableObjectStore.class );

    @Autowired
    protected CurrentUserService currentUserService;

    /**
     * Allows injection (e.g. by a unit test)
     */
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Autowired
    protected DeletedObjectService deletedObjectService;

    @Autowired
    protected AclService aclService;

    private boolean transientIdentifiableProperties = false;

    /**
     * Indicates whether the object represented by the implementation does not
     * have persisted identifiable object properties.
     */
    public boolean isTransientIdentifiableProperties()
    {
        return transientIdentifiableProperties;
    }

    /**
     * Can be overridden programmatically or injected through container.
     */
    public void setTransientIdentifiableProperties( boolean transientIdentifiableProperties )
    {
        this.transientIdentifiableProperties = transientIdentifiableProperties;
    }

    // -------------------------------------------------------------------------
    // InternalHibernateGenericStore implementation
    // -------------------------------------------------------------------------

    public final Criteria getDataSharingCriteria()
    {
        return getExecutableCriteria( getDataSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_DATA ) );
    }

    public final Criteria getDataSharingCriteria( String access )
    {
        return getExecutableCriteria( getDataSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access ) );
    }

    public final Criteria getDataSharingCriteria( User user, String access )
    {
        return getExecutableCriteria( getDataSharingDetachedCriteria( UserInfo.fromUser( user ), access ) );
    }

    public final Criteria getSharingCriteria( String access )
    {
        return getExecutableCriteria( getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access ) );
    }

    public final Criteria getSharingCriteria( User user )
    {
        return getExecutableCriteria( getSharingDetachedCriteria( UserInfo.fromUser( user ), AclService.LIKE_READ_METADATA ) );
    }

    public final DetachedCriteria getSharingDetachedCriteria()
    {
        return getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), AclService.LIKE_READ_METADATA );
    }

    public final DetachedCriteria getSharingDetachedCriteria( String access )
    {
        return getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access );
    }

    public final DetachedCriteria getDataSharingDetachedCriteria( String access )
    {
        return getDataSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access );
    }

    public final DetachedCriteria getSharingDetachedCriteria( User user )
    {
        return getSharingDetachedCriteria( UserInfo.fromUser( user ), AclService.LIKE_READ_METADATA );
    }

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
    public void save( T object, boolean clearSharing )
    {
        User user = currentUserService.getCurrentUser();
        
        String username = user != null ? user.getUsername() : "system-process";

        if ( IdentifiableObject.class.isAssignableFrom( object.getClass() ) )
        {
            object.setAutoFields();
            
            BaseIdentifiableObject identifiableObject = (BaseIdentifiableObject) object;
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
            BaseIdentifiableObject identifiableObject = (BaseIdentifiableObject) object;

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

        if ( MetadataObject.class.isInstance( object ) )
        {
            deletedObjectService.deleteDeletedObjects( new DeletedObjectQuery( (IdentifiableObject) object ) );
        }
    }

    @Override
    public void update( T object )
    {
        update( object, currentUserService.getCurrentUser() );
    }

    @Override
    public void update( T object, User user )
    {
        String username = user != null ? user.getUsername() : "system-process";

        if ( IdentifiableObject.class.isInstance( object ) )
        {
            object.setAutoFields();
            
            BaseIdentifiableObject identifiableObject = (BaseIdentifiableObject) object;
            identifiableObject.setAutoFields();
            identifiableObject.setLastUpdatedBy( user );

            if ( identifiableObject.getUser() == null )
            {
                identifiableObject.setUser( user );
            }
        }

        if ( !isUpdateAllowed( object, user ) )
        {
            AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_UPDATE_DENIED );
            throw new UpdateAccessDeniedException( object.toString() );
        }

        AuditLogUtil.infoWrapper( log, username, object, AuditLogUtil.ACTION_UPDATE );

        if ( object != null )
        {
            getSession().update( object );
        }

        if ( MetadataObject.class.isInstance( object ) )
        {
            deletedObjectService.deleteDeletedObjects( new DeletedObjectQuery( (IdentifiableObject) object ) );
        }
    }

    @Override
    public void delete( T object )
    {
        delete( object, currentUserService.getCurrentUser() );
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
            getSession().delete( object );
        }
    }

    @Override
    public final T get( int id )
    {
        T object = (T) getSession().get( getClazz(), id );
        
        if ( !isReadAllowed( object, currentUserService.getCurrentUser() ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( object.toString() );
        }

        return postProcessObject( object );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getAll()
    {
        return getSharingCriteria().list();
    }

    @Override
    public int getCount()
    {
        return ((Number) getSharingCriteria()
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    public final T getByUid( String uid )
    {
        if ( isTransientIdentifiableProperties() )
        {
            return null;
        }

        return getSharingObject( Restrictions.eq( "uid", uid ) );
    }

    @Override
    public final T getByUidNoAcl( String uid )
    {
        if ( isTransientIdentifiableProperties() )
        {
            return null;
        }

        return getObject( Restrictions.eq( "uid", uid ) );
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
        List<T> list = getList( Restrictions.eq( "name", name ) );

        T object = list != null && !list.isEmpty() ? list.get( 0 ) : null;

        if ( !isReadAllowed( object, currentUserService.getCurrentUser() ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( object.toString() );
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

        return getSharingObject( Restrictions.eq( "code", code ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public T getByUniqueAttributeValue( Attribute attribute, String value )
    {
        if ( attribute == null || StringUtils.isEmpty( value ) || !attribute.isUnique() )
        {
            return null;
        }

        Criteria criteria = getSharingCriteria();
        criteria.createAlias( "attributeValues", "av" );
        criteria.add( Restrictions.eq( "av.value", value ) );
        criteria.createAlias( "av.attribute", "att" );
        criteria.add( Restrictions.eq( "att.id", attribute.getId() ) );

        return (T) criteria.uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllEqName( String name )
    {
        return getSharingCriteria()
            .add( Restrictions.eq( "name", name ) )
            .addOrder( Order.asc( "name" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllLikeName( String name )
    {
        return getSharingCriteria()
            .add( Restrictions.like( "name", "%" + name + "%" ).ignoreCase() )
            .addOrder( Order.asc( "name" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllLikeName( String name, int first, int max )
    {
        return getSharingCriteria()
            .add( Restrictions.like( "name", "%" + name + "%" ).ignoreCase() )
            .addOrder( Order.asc( "name" ) )
            .setFirstResult( first )
            .setMaxResults( max )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllLikeName( Set<String> nameWords, int first, int max )
    {
        Conjunction conjunction = Restrictions.conjunction();

        for ( String word : nameWords )
        {
            conjunction.add( Restrictions.like( "name", "%" + word + "%" ).ignoreCase() );
        }

        return getSharingCriteria()
            .add( conjunction )
            .addOrder( Order.asc( "name" ) )
            .setFirstResult( first )
            .setMaxResults( max )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllOrderedName()
    {
        return getSharingCriteria()
            .addOrder( Order.asc( "name" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllOrderedName( int first, int max )
    {
        return getSharingCriteria()
            .addOrder( Order.asc( "name" ) )
            .setFirstResult( first ).setMaxResults( max )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllOrderedLastUpdated( int first, int max )
    {
        return getSharingCriteria()
            .addOrder( Order.desc( "lastUpdated" ) )
            .setFirstResult( first ).setMaxResults( max )
            .list();
    }

    @Override
    public int getCountLikeName( String name )
    {
        return ((Number) getSharingCriteria()
            .add( Restrictions.like( "name", "%" + name + "%" ).ignoreCase() )
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    public int getCountGeLastUpdated( Date lastUpdated )
    {
        return ((Number) getSharingCriteria()
            .add( Restrictions.ge( "lastUpdated", lastUpdated ) )
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllGeLastUpdated( Date lastUpdated )
    {
        return getSharingCriteria()
            .add( Restrictions.ge( "lastUpdated", lastUpdated ) )
            .addOrder( Order.desc( "lastUpdated" ) )
            .list();
    }

    @Override
    public int getCountGeCreated( Date created )
    {
        return ((Number) getSharingCriteria()
            .add( Restrictions.ge( "created", created ) )
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllGeCreated( Date created )
    {
        return getSharingCriteria()
            .add( Restrictions.ge( "created", created ) )
            .addOrder( Order.desc( "created" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllLeCreated( Date created )
    {
        return getSharingCriteria()
            .add( Restrictions.le( "created", created ) )
            .addOrder( Order.desc( "created" ) )
            .list();
    }

    @Override
    public Date getLastUpdated()
    {
        return (Date) getClazzCriteria().setProjection( Projections.property( "lastUpdated" ) )
            .addOrder( Order.desc( "lastUpdated" ) )
            .setMaxResults( 1 )
            .setCacheable( true ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getByDataDimension( boolean dataDimension )
    {
        return getSharingCriteria()
            .add( Restrictions.eq( "dataDimension", dataDimension ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getByDataDimensionNoAcl( boolean dataDimension )
    {
        return getCriteria()
            .add( Restrictions.eq( "dataDimension", dataDimension ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getById( Collection<Integer> ids )
    {
        if ( ids == null || ids.isEmpty() )
        {
            return new ArrayList<>();
        }

        return getSharingCriteria().add( Restrictions.in( "id", ids ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getByUid( Collection<String> uids )
    {
        if ( uids == null || uids.isEmpty() )
        {
            return new ArrayList<>();
        }

        return getSharingCriteria().add( Restrictions.in( "uid", uids ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getByCode( Collection<String> codes )
    {
        if ( codes == null || codes.isEmpty() )
        {
            return new ArrayList<>();
        }

        return getSharingCriteria().add( Restrictions.in( "code", codes ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getByName( Collection<String> names )
    {
        if ( names == null || names.isEmpty() )
        {
            return new ArrayList<>();
        }

        return getSharingCriteria().add( Restrictions.in( "name", names ) ).list();
    }

    @Override
    public List<T> getAllNoAcl()
    {
        return super.getAll();
    }

    @Override
    public List<T> getByUidNoAcl( Collection<String> uids )
    {
        List<T> list = new ArrayList<>();

        if ( uids != null )
        {
            for ( String uid : uids )
            {
                T object = getByUidNoAcl( uid );

                if ( object != null )
                {
                    list.add( object );
                }
            }
        }

        return list;
    }

    //----------------------------------------------------------------------------------------------------------------
    // Data sharing
    //----------------------------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getDataReadAll()
    {
        return getDataSharingCriteria( AclService.LIKE_READ_DATA ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getDataReadAll( User user )
    {
        return getDataSharingCriteria( user, AclService.LIKE_READ_DATA ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getDataWriteAll()
    {
        return getDataSharingCriteria( AclService.LIKE_WRITE_DATA ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getDataWriteAll( User user )
    {
        return getDataSharingCriteria( user, AclService.LIKE_WRITE_DATA ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getDataReadAll( int first, int max )
    {
        return getDataSharingCriteria()
            .setFirstResult( first )
            .setMaxResults( max )
            .list();
    }

    //----------------------------------------------------------------------------------------------------------------
    // Supportive methods
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a criteria with sharing restrictions relative to the given 
     * user and access string.
     */
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
    protected DetachedCriteria getDataSharingDetachedCriteria( UserInfo user, String access )
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
     * @param user the user.
     * @param access the access string.
     * @return a DetachedCriteria.
     */
    protected DetachedCriteria getSharingDetachedCriteria( UserInfo user, String access )
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

    /**
     * Creates a sharing Criteria for the implementation Class type restricted by the
     * given Criterions.
     *
     * @param expressions the Criterions for the Criteria.
     * @return a Criteria instance.
     */
    protected final Criteria getSharingDetachedCriteria( Criterion... expressions )
    {
        Criteria criteria = getSharingCriteria();

        for ( Criterion expression : expressions )
        {
            criteria.add( expression );
        }

        criteria.setCacheable( cacheable );
        return criteria;
    }

    /**
     * Retrieves an object based on the given Criterions using a sharing Criteria.
     *
     * @param expressions the Criterions for the Criteria.
     * @return an object of the implementation Class type.
     */
    @SuppressWarnings( "unchecked" )
    protected final T getSharingObject( Criterion... expressions )
    {
        return (T) getSharingDetachedCriteria( expressions ).uniqueResult();
    }

    /**
     * Checks whether the given user has public access to the given identifiable object.
     * 
     * @param user the user.
     * @param identifiableObject the identifiable object.
     * @return true or false.
     */
    protected boolean checkPublicAccess( User user, IdentifiableObject identifiableObject )
    {
        return aclService.canMakePublic( user, identifiableObject.getClass() ) ||
            (aclService.canMakePrivate( user, identifiableObject.getClass() ) &&
                !AccessStringHelper.canReadOrWrite( identifiableObject.getPublicAccess() ));
    }

    protected boolean forceAcl()
    {
        return Dashboard.class.isAssignableFrom( clazz );
    }

    protected boolean sharingEnabled( User user )
    {
        return forceAcl() || (aclService.isShareable( clazz ) && !(user == null || user.isSuper()));
    }

    protected boolean sharingEnabled( UserInfo userInfo )
    {
        return forceAcl() || (aclService.isShareable( clazz ) && !(userInfo == null || userInfo.isSuper()));
    }

    protected boolean dataSharingEnabled( UserInfo userInfo )
    {
        return aclService.isDataShareable( clazz ) && !userInfo.isSuper();
    }

    protected boolean isReadAllowed( T object, User user )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject idObject = (IdentifiableObject) object;

            if ( sharingEnabled( user ) )
            {
                return aclService.canRead( user, idObject );
            }
        }

        return true;
    }

    protected boolean isUpdateAllowed( T object, User user )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject idObject = (IdentifiableObject) object;

            if ( aclService.isShareable( clazz ) )
            {
                return aclService.canUpdate( user, idObject );
            }
        }

        return true;
    }

    protected boolean isDeleteAllowed( T object, User user )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject idObject = (IdentifiableObject) object;

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
}
