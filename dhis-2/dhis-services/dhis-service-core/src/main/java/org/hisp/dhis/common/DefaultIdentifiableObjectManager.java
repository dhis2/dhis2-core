package org.hisp.dhis.common;

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

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import static org.hisp.dhis.system.util.ReflectionUtils.getRealClass;

/**
 * Note that it is required for nameable object stores to have concrete implementation
 * classes, not rely on the HibernateIdentifiableObjectStore class, in order to
 * be injected as nameable object stores.
 *
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultIdentifiableObjectManager
    implements IdentifiableObjectManager
{
    private static final Log log = LogFactory.getLog( DefaultIdentifiableObjectManager.class );

    /**
     * Cache for default category objects. Disabled during test phase.
     */
    private static Cache<IdentifiableObject> DEFAULT_OBJECT_CACHE;

    @Autowired
    private Set<IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStores;

    @Autowired
    private Set<GenericDimensionalObjectStore<? extends DimensionalObject>> dimensionalObjectStores;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    private CacheProvider cacheProvider;
    
    private Map<Class<? extends IdentifiableObject>, IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStoreMap;

    private Map<Class<? extends DimensionalObject>, GenericDimensionalObjectStore<? extends DimensionalObject>> dimensionalObjectStoreMap;

    @PostConstruct
    public void init()
    {
        DEFAULT_OBJECT_CACHE = cacheProvider.newCacheBuilder( IdentifiableObject.class )
            .forRegion( "defaultObjectCache" )
            .expireAfterAccess( 2, TimeUnit.HOURS )
            .withInitialCapacity( 4 )
            .forceInMemory()
            .withMaximumSize( SystemUtils.isTestRun() ? 0 : 10 )
            .build();
    }
    
    //--------------------------------------------------------------------------
    // IdentifiableObjectManager implementation
    //--------------------------------------------------------------------------

    @Override
    public void save( IdentifiableObject object )
    {
        save( object, true );
    }

    @Override
    public void save( IdentifiableObject object, boolean clearSharing )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( object.getClass() );

        if ( store != null )
        {
            store.save( object, clearSharing );
        }
    }

    @Override
    public void update( IdentifiableObject object )
    {
        update( object, currentUserService.getCurrentUser() );
    }

    @Override
    public void update( IdentifiableObject object, User user )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( object.getClass() );

        if ( store != null )
        {
            store.update( object, user );
        }
    }

    @Override
    public void update( List<IdentifiableObject> objects )
    {
        update( objects, currentUserService.getCurrentUser() );
    }

    @Override
    public void update( List<IdentifiableObject> objects, User user )
    {
        if ( objects == null || objects.isEmpty() )
        {
            return;
        }

        for ( IdentifiableObject object : objects )
        {
            update( object, user );
        }
    }

    @Override
    public void updateTranslations( IdentifiableObject persistedObject, Set<Translation> translations )
    {
        Session session = sessionFactory.getCurrentSession();
        persistedObject.getTranslations().clear();
        session.flush();

        translations.forEach( translation ->
        {
            if ( StringUtils.isNotEmpty( translation.getValue() ) )
            {
                persistedObject.getTranslations().add( translation );
            }
        } );

        BaseIdentifiableObject translatedObject = (BaseIdentifiableObject) persistedObject;
        translatedObject.setLastUpdated( new Date() );
        translatedObject.setLastUpdatedBy( currentUserService.getCurrentUser() );

        session.update( translatedObject );
    }

    @Override
    public void delete( IdentifiableObject object )
    {
        delete( object, currentUserService.getCurrentUser() );
    }

    @Override
    public void delete( IdentifiableObject object, User user )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( object.getClass() );

        if ( store != null )
        {
            store.delete( object, user );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( String uid )
    {
        for ( IdentifiableObjectStore<? extends IdentifiableObject> store : identifiableObjectStores )
        {
            T object = (T) store.getByUid( uid );

            if ( object != null )
            {
                return object;
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Class<T> clazz, int id )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.get( id );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Class<T> clazz, String uid )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.getByUid( uid );
    }

    @Override
    public <T extends IdentifiableObject> boolean exists( Class<T> clazz, String uid )
    {
        return get( clazz, uid ) != null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> classes, String uid )
    {
        for ( Class<? extends IdentifiableObject> clazz : classes )
        {
            T object = (T) get( clazz, uid );

            if ( object != null )
            {
                return object;
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> classes,
        IdScheme idScheme, String identifier )
    {
        for ( Class<? extends IdentifiableObject> clazz : classes )
        {
            T object = (T) getObject( clazz, idScheme, identifier );

            if ( object != null )
            {
                return object;
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> get( Class<T> clazz, Collection<String> uids )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (List<T>) store.getByUid( uids );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T getByCode( Class<T> clazz, String code )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.getByCode( code );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T getByName( Class<T> clazz, String name )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.getByName( name );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T getByUniqueAttributeValue( Class<T> clazz, Attribute attribute, String value )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.getByUniqueAttributeValue( attribute, value );
    }

    @Override
    public <T extends IdentifiableObject> T search( Class<T> clazz, String query )
    {
        T object = get( clazz, query );

        if ( object == null )
        {
            object = getByCode( clazz, query );
        }

        if ( object == null )
        {
            object = getByName( clazz, query );
        }

        return object;
    }

    @Override
    public <T extends IdentifiableObject> List<T> filter( Class<T> clazz, String query )
    {
        Set<T> uniqueObjects = new HashSet<>();

        T uidObject = get( clazz, query );

        if ( uidObject != null )
        {
            uniqueObjects.add( uidObject );
        }

        T codeObject = getByCode( clazz, query );

        if ( codeObject != null )
        {
            uniqueObjects.add( codeObject );
        }

        uniqueObjects.addAll( getLikeName( clazz, query, false ) );

        List<T> objects = new ArrayList<>( uniqueObjects );

        Collections.sort( objects );

        return objects;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAll( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAll();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getDataWriteAll( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getDataWriteAll();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getDataReadAll( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getDataReadAll();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAllSorted( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllOrderedName();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAllByAttributes( Class<T> klass, List<Attribute> attributes )
    {
        Schema schema = schemaService.getDynamicSchema( klass );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) || attributes.isEmpty() )
        {
            return new ArrayList<>();
        }

        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllByAttributes( attributes );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getByUid( Class<T> clazz, Collection<String> uids )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getByUid( uids );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getById( Class<T> clazz, Collection<Integer> ids )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (List<T>) store.getById( ids );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getByCode( Class<T> clazz, Collection<String> codes )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getByCode( codes );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getByUidOrdered( Class<T> clazz, List<String> uids )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>();

        if ( uids != null )
        {
            for ( String uid : uids )
            {
                T object = store.getByUid( uid );

                if ( object != null )
                {
                    list.add( object );
                }
            }
        }

        return list;
    }

    @Override
    public <T extends IdentifiableObject> int getCount( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store != null )
        {
            return store.getCount();
        }

        return 0;
    }

    @Override
    public <T extends IdentifiableObject> int getCountByCreated( Class<T> clazz, Date created )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store != null )
        {
            return store.getCountGeCreated( created );
        }

        return 0;
    }

    @Override
    public <T extends IdentifiableObject> int getCountByLastUpdated( Class<T> clazz, Date lastUpdated )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store != null )
        {
            return store.getCountGeLastUpdated( lastUpdated );
        }

        return 0;
    }

    @Override
    public <T extends IdentifiableObject> List<T> getLikeName( Class<T> clazz, String name )
    {
        return getLikeName( clazz, name, true );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getLikeName( Class<T> clazz, String name, boolean caseSensitive )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllLikeName( name );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getBetweenSorted( Class<T> clazz, int first, int max )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllOrderedName( first, max );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> clazz, Set<String> words, int first, int max )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllLikeName( words, first, max );
    }

    @Override
    public <T extends IdentifiableObject> Date getLastUpdated( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return store.getLastUpdated();
    }

    @Override
    public <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> clazz, IdentifiableProperty property )
    {
        return getIdMap( clazz, IdScheme.from( property ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> clazz, IdScheme idScheme )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        Map<String, T> map = new HashMap<>();

        if ( store == null )
        {
            return map;
        }

        List<T> objects = store.getAll();

        return IdentifiableObjectUtils.getIdMap( objects, idScheme );
    }

    @Override
    public <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, IdentifiableProperty property )
    {
        return getIdMapNoAcl( clazz, IdScheme.from( property ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, IdScheme idScheme )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        Map<String, T> map = new HashMap<>();

        if ( store == null )
        {
            return map;
        }

        List<T> objects = store.getAllNoAcl();

        return IdentifiableObjectUtils.getIdMap( objects, idScheme );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, IdentifiableProperty property, Collection<String> identifiers )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        if ( identifiers != null && !identifiers.isEmpty() )
        {
            if ( property == null || IdentifiableProperty.UID.equals( property ) )
            {
                return store.getByUid( identifiers );
            }
            else if ( IdentifiableProperty.CODE.equals( property ) )
            {
                return store.getByCode( identifiers );
            }
            else if ( IdentifiableProperty.NAME.equals( property ) )
            {
                return store.getByName( identifiers );
            }

            throw new InvalidIdentifierReferenceException( "Invalid identifiable property / class combination: " + property );
        }

        return new ArrayList<>();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, Collection<Integer> identifiers )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return store.getById( identifiers );
    }

    @Override
    public <T extends IdentifiableObject> T getObject( Class<T> clazz, IdentifiableProperty property, String value )
    {
        return getObject( clazz, IdScheme.from( property ), value );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T getObject( Class<T> clazz, IdScheme idScheme, String value )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        if ( !StringUtils.isEmpty( value ) )
        {
            if ( idScheme.isNull() || idScheme.is( IdentifiableProperty.UID ) )
            {
                return store.getByUid( value );
            }
            else if ( idScheme.is( IdentifiableProperty.CODE ) )
            {
                return store.getByCode( value );
            }
            else if ( idScheme.is( IdentifiableProperty.NAME ) )
            {
                return store.getByName( value );
            }
            else if ( idScheme.is( IdentifiableProperty.ATTRIBUTE ) )
            {
                Attribute attribute = get( Attribute.class, idScheme.getAttribute() );
                return store.getByUniqueAttributeValue( attribute, value );
            }
            else if ( idScheme.is( IdentifiableProperty.ID ) )
            {
                if ( Integer.valueOf( value ) > 0 )
                {
                    return store.get( Integer.valueOf( value ) );
                }
            }

            throw new InvalidIdentifierReferenceException( "Invalid identifiable property / class combination: " + idScheme );
        }

        return null;
    }

    @Override
    public IdentifiableObject getObject( String uid, String simpleClassName )
    {
        for ( IdentifiableObjectStore<? extends IdentifiableObject> objectStore : identifiableObjectStores )
        {
            if ( simpleClassName.equals( objectStore.getClazz().getSimpleName() ) )
            {
                return objectStore.getByUid( uid );
            }
        }

        return null;
    }

    @Override
    public IdentifiableObject getObject( int id, String simpleClassName )
    {
        for ( IdentifiableObjectStore<? extends IdentifiableObject> objectStore : identifiableObjectStores )
        {
            if ( simpleClassName.equals( objectStore.getClazz().getSimpleName() ) )
            {
                return objectStore.get( id );
            }
        }

        return null;
    }

    @Override
    public void refresh( Object object )
    {
        sessionFactory.getCurrentSession().refresh( object );
    }

    @Override
    public void flush()
    {
        sessionFactory.getCurrentSession().flush();
    }

    @Override
    public void evict( Object object )
    {
        sessionFactory.getCurrentSession().evict( object );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T getNoAcl( Class<T> clazz, String uid )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.getByUidNoAcl( uid );
    }

    @Override
    public <T extends IdentifiableObject> void updateNoAcl( T object )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( object.getClass() );

        if ( store != null )
        {
            store.updateNoAcl( object );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAllNoAcl( Class<T> clazz )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllNoAcl();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends DimensionalObject> List<T> getDataDimensions( Class<T> clazz )
    {
        GenericDimensionalObjectStore<DimensionalObject> store = getDimensionalObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getByDataDimension( true );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends DimensionalObject> List<T> getDataDimensionsNoAcl( Class<T> clazz )
    {
        GenericDimensionalObjectStore<DimensionalObject> store = getDimensionalObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getByDataDimensionNoAcl( true );
    }

    @Override
    public <T extends IdentifiableObject> List<AttributeValue> getAttributeValueByAttribute( Class<T> klass, Attribute attribute )
    {
        Schema schema = schemaService.getDynamicSchema( klass );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return new ArrayList<>();
        }

        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );

        if ( store == null )
        {
            return null;
        }

        return store.getAttributeValueByAttribute( attribute );
    }

    @Override
    public <T extends IdentifiableObject> List<AttributeValue> getAttributeValueByAttributeAndValue( Class<T> klass, Attribute attribute, String value )
    {
        Schema schema = schemaService.getDynamicSchema( klass );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return null;
        }

        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );

        if ( store == null )
        {
            return null;
        }

        return store.getAttributeValueByAttributeAndValue( attribute, value );
    }

    @Override
    public <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> klass, T object, AttributeValue attributeValue )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );
        return store != null && store.isAttributeValueUnique( object, attributeValue );
    }

    @Override
    public <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> klass, T object, Attribute attribute, String value )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );
        return store != null && store.isAttributeValueUnique( object, attribute, value );
    }

    @Override
    public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults()
    {
        return new ImmutableMap.Builder<Class<? extends IdentifiableObject>, IdentifiableObject>()
            .put( Category.class, DEFAULT_OBJECT_CACHE.get( Category.class.getName(), key -> getByName( Category.class, "default" ) ).orElse( null ) )
            .put( CategoryCombo.class, DEFAULT_OBJECT_CACHE.get( CategoryCombo.class.getName(), key -> getByName( CategoryCombo.class, "default" ) ).orElse( null ) )
            .put( CategoryOption.class, DEFAULT_OBJECT_CACHE.get( CategoryOption.class.getName(), key -> getByName( CategoryOption.class, "default" ) ).orElse( null ) )
            .put( CategoryOptionCombo.class, DEFAULT_OBJECT_CACHE.get( CategoryOptionCombo.class.getName(), key -> getByName( CategoryOptionCombo.class, "default" ) ).orElse( null ) )
            .build();
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    @Override
    public boolean isDefault( IdentifiableObject object )
    {
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = getDefaults();
        if ( object == null )
        {
            return false;
        }
        Class<?> realClass = getRealClass( object.getClass() );
        if ( !defaults.containsKey( realClass ) )
        {
            return false;
        }
        IdentifiableObject defaultObject = defaults.get( realClass );
        return defaultObject != null && defaultObject.getUid().equals( object.getUid() );
    }

    @SuppressWarnings( "unchecked" )
    private <T extends IdentifiableObject> IdentifiableObjectStore<IdentifiableObject> getIdentifiableObjectStore( Class<T> clazz )
    {
        initMaps();

        IdentifiableObjectStore<? extends IdentifiableObject> store = identifiableObjectStoreMap.get( clazz );

        if ( store == null )
        {
            store = identifiableObjectStoreMap.get( clazz.getSuperclass() );

            if ( store == null && !UserCredentials.class.isAssignableFrom( clazz ) )
            {
                log.debug( "No IdentifiableObjectStore found for class: " + clazz );
            }
        }

        return (IdentifiableObjectStore<IdentifiableObject>) store;
    }

    @SuppressWarnings( "unchecked" )
    private <T extends DimensionalObject> GenericDimensionalObjectStore<DimensionalObject> getDimensionalObjectStore( Class<T> clazz )
    {
        initMaps();

        GenericDimensionalObjectStore<? extends DimensionalObject> store = dimensionalObjectStoreMap.get( clazz );

        if ( store == null )
        {
            store = dimensionalObjectStoreMap.get( clazz.getSuperclass() );

            if ( store == null )
            {
                log.debug( "No DimensionalObjectStore found for class: " + clazz );
            }
        }

        return (GenericDimensionalObjectStore<DimensionalObject>) store;
    }

    private void initMaps()
    {
        if ( identifiableObjectStoreMap != null )
        {
            return; // Already initialized
        }

        identifiableObjectStoreMap = new HashMap<>();

        for ( IdentifiableObjectStore<? extends IdentifiableObject> store : identifiableObjectStores )
        {
            identifiableObjectStoreMap.put( store.getClazz(), store );
        }

        dimensionalObjectStoreMap = new HashMap<>();

        for ( GenericDimensionalObjectStore<? extends DimensionalObject> store : dimensionalObjectStores )
        {
            dimensionalObjectStoreMap.put( store.getClazz(), store );
        }
    }
}
