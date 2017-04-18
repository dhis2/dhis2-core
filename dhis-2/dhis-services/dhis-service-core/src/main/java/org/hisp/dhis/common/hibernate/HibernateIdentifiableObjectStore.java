package org.hisp.dhis.common.hibernate;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author bobj
 */
public class HibernateIdentifiableObjectStore<T extends BaseIdentifiableObject>
    extends HibernateGenericStore<T> implements GenericDimensionalObjectStore<T>
{
    private static final Log log = LogFactory.getLog( HibernateIdentifiableObjectStore.class );

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
    // IdentifiableObjectStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void save( T object )
    {
        object.setAutoFields();
        super.save( object );
    }

    @Override
    public void update( T object )
    {
        object.setAutoFields();
        super.update( object );
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

        if ( !isReadAllowed( object ) )
        {
            AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED );
            throw new ReadAccessDeniedException( object.toString() );
        }

        return object;
    }

    /**
     * Uses query since name property might not be unique.
     */
    @Override
    public final T getByShortName( String shortName )
    {
        List<T> list = getList( Restrictions.eq( "shortName", shortName ) );

        T object = list != null && !list.isEmpty() ? list.get( 0 ) : null;

        if ( !isReadAllowed( object ) )
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
    public List<T> getAllEqNameIgnoreCase( String name )
    {
        return getSharingCriteria()
            .add( Restrictions.eq( "name", name ).ignoreCase() )
            .addOrder( Order.asc( "name" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllEqShortName( String shortName )
    {
        return getSharingCriteria()
            .add( Restrictions.eq( "shortName", shortName ) )
            .addOrder( Order.asc( "shortName" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllEqShortNameIgnoreCase( String shortName )
    {
        return getSharingCriteria()
            .add( Restrictions.eq( "shortName", shortName ).ignoreCase() )
            .addOrder( Order.asc( "shortName" ) )
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
    public List<T> getAllLikeShortName( String shortName )
    {
        if ( NameableObject.class.isAssignableFrom( clazz ) )
        {
            return getSharingCriteria()
                .add( Restrictions.like( "shortName", "%" + shortName + "%" ).ignoreCase() )
                .addOrder( Order.asc( "shortName" ) )
                .list();
        }

        return getAllLikeName( shortName ); // Fallback to name
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
    public List<T> getAllOrderedLastUpdated()
    {
        return getSharingCriteria()
            .addOrder( Order.desc( "lastUpdated" ) )
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
    public int getCountEqName( String name )
    {
        return ((Number) getSharingCriteria()
            .add( Restrictions.eq( "name", name ).ignoreCase() )
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
    }

    @Override
    public int getCountEqShortName( String shortName )
    {
        return ((Number) getSharingCriteria()
            .add( Restrictions.eq( "shortName", shortName ).ignoreCase() )
            .setProjection( Projections.countDistinct( "id" ) )
            .uniqueResult()).intValue();
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
    public int getCountLikeShortName( String shortName )
    {
        return ((Number) getSharingCriteria()
            .add( Restrictions.like( "shortName", "%" + shortName + "%" ).ignoreCase() )
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
    @SuppressWarnings( "unchecked" )
    public List<T> getAllGeCreatedOrderedName( Date created )
    {
        return getSharingCriteria()
            .add( Restrictions.ge( "created", created ) )
            .addOrder( Order.asc( "name" ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<T> getAllGeLastUpdatedOrderedName( Date lastUpdated )
    {
        return getSharingCriteria()
            .add( Restrictions.ge( "lastUpdated", lastUpdated ) )
            .addOrder( Order.asc( "name" ) )
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
    // No ACL (unfiltered methods)
    //----------------------------------------------------------------------------------------------------------------

    @Override
    public int getCountEqNameNoAcl( String name )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where c.name = :name" );
        query.setParameter( "name", name );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int getCountEqShortNameNoAcl( String shortName )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where c.shortName = :shortName" );
        query.setParameter( "shortName", shortName );

        return ((Long) query.uniqueResult()).intValue();
    }
}
