package org.hisp.dhis.hibernate;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class HibernateGenericStore<T>
    implements InternalHibernateGenericStore<T>
{
    private static final Log log = LogFactory.getLog( HibernateGenericStore.class );

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

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected DeletedObjectService deletedObjectService;

    /**
     * Allows injection (e.g. by a unit test)
     */
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Autowired
    protected AclService aclService;

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
     * Creates a Query.
     *
     * @param hql the hql query.
     * @return a Query instance.
     */
    protected final Query getQuery( String hql )
    {
        return getSession().createQuery( hql ).setCacheable( cacheable );
    }

    /**
     * Creates a SqlQuery.
     *
     * @param sql the sql query.
     * @return a SqlQuery instance.
     */
    protected final SQLQuery getSqlQuery( String sql )
    {
        SQLQuery query = getSession().createSQLQuery( sql );
        query.setCacheable( cacheable );
        return query;
    }

    /**
     * Creates a Criteria for the implementation Class type.
     * <p>
     * Please note that sharing is not considered.
     *
     * @return a Criteria instance.
     */
    @Override
    public final Criteria getCriteria()
    {
        DetachedCriteria criteria = DetachedCriteria.forClass( getClazz() );

        preProcessDetachedCriteria( criteria );

        return getExecutableCriteria( criteria );
    }

    @Override
    public final Criteria getSharingCriteria()
    {
        return getExecutableCriteria( getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), "r%" ) );
    }

    @Override
    public final Criteria getSharingCriteria( String access )
    {
        return getExecutableCriteria( getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access ) );
    }

    @Override
    public final Criteria getSharingCriteria( User user )
    {
        return getExecutableCriteria( getSharingDetachedCriteria( UserInfo.fromUser( user ), "r%" ) );
    }

    @Override
    public final DetachedCriteria getSharingDetachedCriteria()
    {
        return getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), "r%" );
    }

    @Override
    public final DetachedCriteria getSharingDetachedCriteria( String access )
    {
        return getSharingDetachedCriteria( currentUserService.getCurrentUserInfo(), access );
    }

    @Override
    public final DetachedCriteria getSharingDetachedCriteria( User user )
    {
        return getSharingDetachedCriteria( UserInfo.fromUser( user ), "r%" );
    }

    @Override
    public final Criteria getExecutableCriteria( DetachedCriteria detachedCriteria )
    {
        return detachedCriteria.getExecutableCriteria( getSession() ).setCacheable( cacheable );
    }

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

    /**
     * Override to add additional restrictions to criteria before
     * it is invoked.
     */
    protected void preProcessDetachedCriteria( DetachedCriteria detachedCriteria )
    {
    }

    protected Criteria getClazzCriteria()
    {
        return getSession().createCriteria( getClazz() );
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
     * Retrieves an object based on the given Criterions.
     *
     * @param expressions the Criterions for the Criteria.
     * @return an object of the implementation Class type.
     */
    @SuppressWarnings( "unchecked" )
    protected final T getObject( Criterion... expressions )
    {
        return (T) getCriteria( expressions ).uniqueResult();
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
     * Retrieves a List based on the given Criterions.
     *
     * @param expressions the Criterions for the Criteria.
     * @return a List with objects of the implementation Class type.
     */
    @SuppressWarnings( "unchecked" )
    protected final List<T> getList( Criterion... expressions )
    {
        return getCriteria( expressions ).list();
    }

    // -------------------------------------------------------------------------
    // GenericIdentifiableObjectStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void save( T object )
    {
        save( object, currentUserService.getCurrentUser(), true );
    }

    @Override
    public void save( T object, User user )
    {
        save( object, user, true );
    }

    @Override
    public void save( T object, boolean clearSharing )
    {
        save( object, currentUserService.getCurrentUser(), clearSharing );
    }

    @Override
    public void save( T object, User user, boolean clearSharing )
    {
        String username = user != null ? user.getUsername() : "system-process";

        if ( IdentifiableObject.class.isAssignableFrom( object.getClass() ) )
        {
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

    private boolean checkPublicAccess( User user, IdentifiableObject identifiableObject )
    {
        return aclService.canMakePublic( user, identifiableObject.getClass() ) ||
            (aclService.canMakePrivate( user, identifiableObject.getClass() ) &&
                !AccessStringHelper.canReadOrWrite( identifiableObject.getPublicAccess() ));
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

        if ( !isReadAllowed( object ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( object.toString() );
        }

        return postProcessObject( object );
    }

    /**
     * Override to inspect, or alter object before it is returned.
     */
    protected T postProcessObject( T object )
    {
        return object;
    }


    @Override
    public final T getNoAcl( int id )
    {
        return (T) getSession().get( getClazz(), id );
    }

    @Override
    public final T load( int id )
    {
        T object = (T) getSession().load( getClazz(), id );

        if ( !isReadAllowed( object ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( object.toString() );
        }

        return object;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getAll()
    {
        return getSharingCriteria().list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getAll( int first, int max )
    {
        return getSharingCriteria()
            .setFirstResult( first )
            .setMaxResults( max )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllByAttributes( List<Attribute> attributes )
    {
        Schema schema = schemaService.getDynamicSchema( getClazz() );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) || attributes.isEmpty() )
        {
            return new ArrayList<>();
        }

        String hql = "select e from " + getClazz().getSimpleName() + "  as e " +
            "inner join e.attributeValues av inner join av.attribute at where at in (:attributes) )";

        return getQuery( hql ).setParameterList( "attributes", attributes ).list();
    }

    @Override
    public int getCount()
    {
        return ((Number) getSharingCriteria()
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getAllNoAcl()
    {
        return getCriteria().list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final List<T> getAllNoAcl( int first, int max )
    {
        return getCriteria()
            .setFirstResult( first )
            .setMaxResults( max )
            .list();
    }

    @Override
    public int getCountNoAcl()
    {
        return ((Number) getCriteria()
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public T getByAttribute( Attribute attribute )
    {
        Schema schema = schemaService.getDynamicSchema( getClazz() );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return null;
        }

        Criteria criteria = getCriteria();
        criteria.createAlias( "attributeValues", "av" );
        criteria.add( Restrictions.eq( "av.attribute", attribute ) );

        return (T) criteria.uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<AttributeValue> getAttributeValueByAttribute( Attribute attribute )
    {
        Schema schema = schemaService.getDynamicSchema( getClazz() );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return new ArrayList<>();
        }

        String hql = "select av from " + getClazz().getSimpleName() + "  as e " +
            "inner join e.attributeValues av inner join av.attribute at where at = :attribute )";

        return getQuery( hql ).setEntity( "attribute", attribute ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<AttributeValue> getAttributeValueByAttributes( List<Attribute> attributes )
    {
        Schema schema = schemaService.getDynamicSchema( getClazz() );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return new ArrayList<>();
        }

        String hql = "select av from " + getClazz().getSimpleName() + "  as e " +
            "inner join e.attributeValues av inner join av.attribute at where at in (:attributes) )";

        return getQuery( hql ).setParameterList( "attributes", attributes ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<AttributeValue> getAttributeValueByAttributeAndValue( Attribute attribute, String value )
    {
        Schema schema = schemaService.getDynamicSchema( getClazz() );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return null;
        }

        String hql = "select av from " + getClazz().getSimpleName() + "  as e " +
            "inner join e.attributeValues av inner join av.attribute at where at = :attribute and av.value = :value)";

        return getQuery( hql ).setEntity( "attribute", attribute ).setString( "value", value ).list();
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

    //----------------------------------------------------------------------------------------------------------------
    // Helpers
    //----------------------------------------------------------------------------------------------------------------

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

    protected boolean isReadAllowed( T object )
    {
        return isReadAllowed( object, currentUserService.getCurrentUser() );
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
}
