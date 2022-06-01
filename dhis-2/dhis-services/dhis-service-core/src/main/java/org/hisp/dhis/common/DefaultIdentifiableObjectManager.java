/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;

/**
 * Note that it is required for nameable object stores to have concrete
 * implementation classes, not rely on the HibernateIdentifiableObjectStore
 * class, in order to be injected as nameable object stores.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component( "org.hisp.dhis.common.IdentifiableObjectManager" )
public class DefaultIdentifiableObjectManager
    implements IdentifiableObjectManager
{
    public static final String DEFAULT = "default";

    /**
     * Cache for default category objects. Disabled during test phase.
     */
    private final Cache<IdentifiableObject> defaultObjectCache;

    private final Set<IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStores;

    private final Set<GenericDimensionalObjectStore<? extends DimensionalObject>> dimensionalObjectStores;

    private final SessionFactory sessionFactory;

    private final CurrentUserService currentUserService;

    protected final SchemaService schemaService;

    private Map<Class<? extends IdentifiableObject>, IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStoreMap;

    private Map<Class<? extends DimensionalObject>, GenericDimensionalObjectStore<? extends DimensionalObject>> dimensionalObjectStoreMap;

    public DefaultIdentifiableObjectManager(
        Set<IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStores,
        Set<GenericDimensionalObjectStore<? extends DimensionalObject>> dimensionalObjectStores,
        SessionFactory sessionFactory, CurrentUserService currentUserService, SchemaService schemaService,
        CacheProvider cacheProvider )
    {
        checkNotNull( identifiableObjectStores );
        checkNotNull( dimensionalObjectStores );
        checkNotNull( sessionFactory );
        checkNotNull( currentUserService );
        checkNotNull( schemaService );
        checkNotNull( cacheProvider );

        this.identifiableObjectStores = identifiableObjectStores;
        this.dimensionalObjectStores = dimensionalObjectStores;
        this.sessionFactory = sessionFactory;
        this.currentUserService = currentUserService;
        this.schemaService = schemaService;
        this.defaultObjectCache = cacheProvider.createDefaultObjectCache();

        removeNonSupportedStores();
    }

    /**
     * This method removes identifiable stores that should not be supported
     * anymore. Some objects are still in usage in very specific places, but
     * should no longer be backed by any store. Their stores are still usage
     * directly in a few limited places, but they should not be used broadly or
     * be part of the core set of endpoints exposed.
     */
    private void removeNonSupportedStores()
    {
        identifiableObjectStores
            .removeIf( store -> store.getClazz() == Chart.class || store.getClazz() == ReportTable.class );
    }

    // --------------------------------------------------------------------------
    // IdentifiableObjectManager implementation
    // --------------------------------------------------------------------------

    @Override
    @Transactional
    public void save( IdentifiableObject object )
    {
        save( object, true );
    }

    @Override
    @Transactional
    @SuppressWarnings( "unchecked" )
    public void save( IdentifiableObject object, boolean clearSharing )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore(
            HibernateProxyUtils.getRealClass( object ) );

        if ( store != null )
        {
            store.save( object, clearSharing );
        }
    }

    @Override
    @Transactional
    public void save( List<IdentifiableObject> objects )
    {
        objects.forEach( o -> save( o, true ) );
    }

    @Override
    @Transactional
    public void update( IdentifiableObject object )
    {
        update( object, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional
    @SuppressWarnings( "unchecked" )
    public void update( IdentifiableObject object, User user )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore(
            HibernateProxyUtils.getRealClass( object ) );

        if ( store != null )
        {
            store.update( object, user );
        }
    }

    @Override
    @Transactional
    public void update( List<IdentifiableObject> objects )
    {
        update( objects, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional
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
    @Transactional
    public void updateTranslations( IdentifiableObject persistedObject, Set<Translation> translations )
    {
        Session session = sessionFactory.getCurrentSession();

        BaseIdentifiableObject translatedObject = (BaseIdentifiableObject) persistedObject;

        translatedObject.setTranslations(
            translations.stream()
                .filter( t -> !StringUtils.isEmpty( t.getValue() ) )
                .collect( Collectors.toSet() ) );

        translatedObject.setLastUpdated( new Date() );
        translatedObject.setLastUpdatedBy( currentUserService.getCurrentUser() );

        session.update( translatedObject );
    }

    @Override
    @Transactional
    public void delete( IdentifiableObject object )
    {
        delete( object, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional
    @SuppressWarnings( "unchecked" )
    public void delete( IdentifiableObject object, User user )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore(
            HibernateProxyUtils.getRealClass( object ) );

        if ( store != null )
        {
            store.delete( object, user );
        }
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Class<T> clazz, long id )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.get( id );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> boolean exists( Class<T> clazz, String uid )
    {
        return get( clazz, uid ) != null;
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getNoAcl( Class<T> clazz, Collection<String> uids )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (List<T>) store.getByUidNoAcl( uids );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> T getByUniqueAttributeValue( Class<T> clazz, Attribute attribute,
        String value )
    {
        return getByUniqueAttributeValue( clazz, attribute, value, currentUserService.getCurrentUserInfo() );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> T getByUniqueAttributeValue( Class<T> clazz, Attribute attribute,
        String value, UserInfo userInfo )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (T) store.getByUniqueAttributeValue( attribute, value, userInfo );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> List<AttributeValue> getAllValuesByAttributes( Class<T> klass,
        List<Attribute> attributes )
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

        return store.getAllValuesByAttributes( attributes );
    }

    @Override
    public <T extends IdentifiableObject> long countAllValuesByAttributes( Class<T> klass, List<Attribute> attributes )
    {
        Schema schema = schemaService.getDynamicSchema( klass );

        if ( schema == null || !schema.havePersistedProperty( "attributeValues" ) || attributes.isEmpty() )
        {
            return 0;
        }

        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );

        if ( store == null )
        {
            return 0;
        }

        return store.countAllValuesByAttributes( attributes );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> List<T> getAndValidateByUid( Class<T> type, Collection<String> uids )
        throws IllegalQueryException
    {
        List<T> objects = getByUid( type, uids );

        List<String> identifiers = IdentifiableObjectUtils.getUids( objects );
        List<String> difference = CollectionUtils.difference( uids, identifiers );

        if ( !difference.isEmpty() )
        {
            throw new IllegalQueryException( new ErrorMessage(
                ErrorCode.E1112, type.getSimpleName(), difference ) );
        }

        return objects;
    }

    @Override
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getById( Class<T> clazz, Collection<Long> ids )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return null;
        }

        return (List<T>) store.getById( ids );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> List<T> getOrdered( Class<T> clazz, IdScheme idScheme,
        Collection<String> values )
    {
        if ( values == null )
        {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>();

        for ( String value : values )
        {
            T object = getObject( clazz, idScheme, value );

            if ( object != null )
            {
                list.add( object );
            }
        }

        return list;
    }

    @Override
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getByUidOrdered( Class<T> clazz, List<String> uids )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        if ( store == null || uids == null )
        {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>();

        for ( String uid : uids )
        {
            T object = store.getByUid( uid );

            if ( object != null )
            {
                list.add( object );
            }
        }

        return list;
    }

    @Override
    @Transactional
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getLikeName( Class<T> clazz, String name )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllLikeName( name );
    }

    @Override
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getLikeName( Class<T> clazz, String name, boolean caseSensitive )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllLikeName( name, caseSensitive );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> clazz, Set<String> words, int first,
        int max )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return (List<T>) store.getAllLikeName( words, first, max );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> clazz, IdentifiableProperty property )
    {
        return getIdMap( clazz, IdScheme.from( property ) );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, IdentifiableProperty property )
    {
        return getIdMapNoAcl( clazz, IdScheme.from( property ) );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, IdentifiableProperty property,
        Collection<String> identifiers )
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

            throw new InvalidIdentifierReferenceException(
                "Invalid identifiable property / class combination: " + property );
        }

        return new ArrayList<>();
    }

    @Override
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, Collection<Long> identifiers )
    {
        IdentifiableObjectStore<T> store = (IdentifiableObjectStore<T>) getIdentifiableObjectStore( clazz );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return store.getById( identifiers );
    }

    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> T getObject( Class<T> clazz, IdentifiableProperty property, String value )
    {
        return getObject( clazz, IdScheme.from( property ), value );
    }

    @Override
    @Transactional( readOnly = true )
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

            throw new InvalidIdentifierReferenceException(
                "Invalid identifiable property / class combination: " + idScheme );
        }

        return null;
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public IdentifiableObject getObject( long id, String simpleClassName )
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
    @Transactional
    public void refresh( Object object )
    {
        sessionFactory.getCurrentSession().refresh( object );
    }

    @Override
    @Transactional
    public void flush()
    {
        sessionFactory.getCurrentSession().flush();
    }

    @Override
    @Transactional
    public void clear()
    {
        sessionFactory.getCurrentSession().clear();
    }

    @Override
    @Transactional
    public void evict( Object object )
    {
        sessionFactory.getCurrentSession().evict( object );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> void updateNoAcl( T object )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore(
            HibernateProxyUtils.getRealClass( object ) );

        if ( store != null )
        {
            store.updateNoAcl( object );
        }
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getByAttributeAndValue( Class<T> klass, Attribute attribute,
        String value )
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

        return (List<T>) store.getByAttributeAndValue( attribute, value );
    }

    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> klass,
        T object, AttributeValue attributeValue )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );
        return store != null && store.isAttributeValueUnique( object, attributeValue );
    }

    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> klass,
        T object, Attribute attribute, String value )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );
        return store != null && store.isAttributeValueUnique( object, attribute, value );
    }

    @Override
    @Transactional( readOnly = true )
    public List<? extends IdentifiableObject> getAllByAttributeAndValues( Class<? extends IdentifiableObject> klass,
        Attribute attribute, List<String> values )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );
        return store != null ? store.getAllByAttributeAndValues( attribute, values ) : Lists.newArrayList();
    }

    @Override
    public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults()
    {
        Optional<IdentifiableObject> categoryObjects = defaultObjectCache.get( Category.class.getName(),
            key -> HibernateProxyUtils.unproxy( getByName( Category.class, DEFAULT ) ) );
        Optional<IdentifiableObject> categoryComboObjects = defaultObjectCache.get( CategoryCombo.class.getName(),
            key -> HibernateProxyUtils.unproxy( getByName( CategoryCombo.class, DEFAULT ) ) );
        Optional<IdentifiableObject> categoryOptionObjects = defaultObjectCache.get( CategoryOption.class.getName(),
            key -> HibernateProxyUtils.unproxy( getByName( CategoryOption.class, DEFAULT ) ) );
        Optional<IdentifiableObject> categoryOptionCombo = defaultObjectCache.get(
            CategoryOptionCombo.class.getName(),
            key -> HibernateProxyUtils.unproxy( getByName( CategoryOptionCombo.class, DEFAULT ) ) );

        return new ImmutableMap.Builder<Class<? extends IdentifiableObject>, IdentifiableObject>()
            .put( Category.class, Objects.requireNonNull( categoryObjects.orElse( null ) ) )
            .put( CategoryCombo.class, Objects.requireNonNull( categoryComboObjects.orElse( null ) ) )
            .put( CategoryOption.class, Objects.requireNonNull( categoryOptionObjects.orElse( null ) ) )
            .put( CategoryOptionCombo.class, Objects.requireNonNull( categoryOptionCombo.orElse( null ) ) )
            .build();
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getUidsCreatedBefore( Class<? extends IdentifiableObject> klass, Date date )
    {
        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( klass );

        if ( store == null )
        {
            return new ArrayList<>();
        }

        return store.getUidsCreatedBefore( date );
    }

    // --------------------------------------------------------------------------
    // Supportive methods
    // --------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public boolean isDefault( IdentifiableObject object )
    {
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = getDefaults();

        if ( object == null )
        {
            return false;
        }

        Class<?> realClass = HibernateProxyUtils.getRealClass( object );

        if ( !defaults.containsKey( realClass ) )
        {
            return false;
        }

        IdentifiableObject defaultObject = defaults.get( realClass );

        return defaultObject != null && defaultObject.getUid().equals( object.getUid() );
    }

    @Override
    @Transactional
    public void removeUserGroupFromSharing( String userGroupUid )
    {
        List<Schema> schemas = schemaService.getSchemas().stream().filter( s -> s.isShareable() ).collect(
            Collectors.toList() );

        IdentifiableObjectStore<IdentifiableObject> store = getIdentifiableObjectStore( UserGroup.class );
        schemas.forEach( schema -> store.removeUserGroupFromSharing( userGroupUid, schema.getTableName() ) );
    }

    @SuppressWarnings( "unchecked" )
    private <T extends IdentifiableObject> IdentifiableObjectStore<IdentifiableObject> getIdentifiableObjectStore(
        Class<T> clazz )
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
    private <T extends DimensionalObject> GenericDimensionalObjectStore<DimensionalObject> getDimensionalObjectStore(
        Class<T> clazz )
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
