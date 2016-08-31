package org.hisp.dhis.dxf2.metadata;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.deletion.DeletionManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultObjectBridge
    implements ObjectBridge
{
    private static final Log log = LogFactory.getLog( DefaultObjectBridge.class );

    //-------------------------------------------------------------------------------------------------------
    // Dependencies
    //-------------------------------------------------------------------------------------------------------

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private PeriodStore periodStore;

    @Autowired
    private UserService userService;

    @Autowired
    private DeletionManager deletionManager;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    //-------------------------------------------------------------------------------------------------------
    // Internal and Semi-Public maps
    //-------------------------------------------------------------------------------------------------------

    private Set<Class<?>> supportedTypes = new HashSet<>();

    private HashMap<Class<?>, Set<?>> masterMap;

    private Map<String, PeriodType> periodTypeMap;

    private Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> uidMap;

    private Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> uuidMap;

    private Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> codeMap;

    private Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> nameMap;

    private Map<String, UserCredentials> usernameMap;

    private boolean writeEnabled = true;

    private boolean preheatCache = true;

    //-------------------------------------------------------------------------------------------------------
    // Build maps
    //-------------------------------------------------------------------------------------------------------

    @PostConstruct
    public void postConstruct()
    {
        supportedTypes.add( PeriodType.class );
        supportedTypes.add( UserCredentials.class );
        supportedTypes.addAll( schemaService.getMetadataSchemas().stream().map( Schema::getKlass ).collect( Collectors.toList() ) );
    }

    @Override
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void init( Set<Class> preheatClasses )
    {
        if ( preheatClasses.isEmpty() )
        {
            preheatClasses.addAll( supportedTypes );
        }

        log.info( "Building object-bridge maps (preheatCache: " + preheatCache + ", " + preheatClasses.size() + " classes)." );
        Timer timer = new SystemTimer().start();

        masterMap = new HashMap<>();
        periodTypeMap = new HashMap<>();
        uidMap = new HashMap<>();
        uuidMap = new HashMap<>();
        codeMap = new HashMap<>();
        nameMap = new HashMap<>();
        usernameMap = new HashMap<>();

        populatePeriodTypeMap( PeriodType.class );
        populateUsernameMap( UserCredentials.class );

        for ( Class<?> type : supportedTypes )
        {
            masterMap.put( type, new HashSet<IdentifiableObject>() );
            uidMap.put( (Class<? extends IdentifiableObject>) type, new HashMap<>() );
            uuidMap.put( (Class<? extends IdentifiableObject>) type, new HashMap<>() );
            codeMap.put( (Class<? extends IdentifiableObject>) type, new HashMap<>() );
            nameMap.put( (Class<? extends IdentifiableObject>) type, new HashMap<>() );

            if ( preheatClasses.contains( type ) )
            {
                populateIdentifiableObjectMap( type );
                populateIdentifiableObjectMap( type, IdentifiableProperty.UID );
                populateIdentifiableObjectMap( type, IdentifiableProperty.UUID );
                populateIdentifiableObjectMap( type, IdentifiableProperty.CODE );
                populateIdentifiableObjectMap( type, IdentifiableProperty.NAME );
            }
        }

        timer.stop();
        log.info( "Building object-bridge maps took " + timer.toString() + "." );
    }

    @Override
    public void destroy()
    {
        masterMap = null;
        uidMap = null;
        codeMap = null;
        nameMap = null;
        periodTypeMap = null;

        writeEnabled = true;
        preheatCache = true;
    }

    //-------------------------------------------------------------------------------------------------------
    // Populate Helpers
    //-------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private void populateIdentifiableObjectMap( Class<?> clazz )
    {
        if ( preheatCache && IdentifiableObject.class.isAssignableFrom( clazz ) )
        {
            Set<IdentifiableObject> map = (Set<IdentifiableObject>) masterMap.get( clazz );
            map.addAll( manager.getAllNoAcl( (Class<IdentifiableObject>) clazz ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void populateIdentifiableObjectMap( Class<?> clazz, IdentifiableProperty property )
    {
        Map<String, IdentifiableObject> map = new HashMap<>();
        IdentifiableObject identifiableObject;

        try
        {
            identifiableObject = (IdentifiableObject) clazz.newInstance();
        }
        catch ( InstantiationException | IllegalAccessException ignored )
        {
            return;
        }

        if ( preheatCache && IdentifiableObject.class.isAssignableFrom( clazz ) )
        {
            map = (Map<String, IdentifiableObject>) manager.getIdMapNoAcl( (Class<? extends IdentifiableObject>) clazz, property );
        }

        if ( !preheatCache || map != null )
        {
            if ( property == IdentifiableProperty.UID )
            {
                uidMap.put( (Class<? extends IdentifiableObject>) clazz, map );
            }
            else if ( property == IdentifiableProperty.UUID && OrganisationUnit.class.isAssignableFrom( clazz ) )
            {
                uuidMap.put( (Class<? extends IdentifiableObject>) clazz, map );
            }
            else if ( property == IdentifiableProperty.CODE && identifiableObject.haveUniqueCode() )
            {
                codeMap.put( (Class<? extends IdentifiableObject>) clazz, map );
            }
            else if ( property == IdentifiableProperty.NAME && identifiableObject.haveUniqueNames() )
            {
                if ( !preheatCache )
                {
                    nameMap.put( (Class<? extends IdentifiableObject>) clazz, map );
                }
                else
                {
                    if ( identifiableObject.haveUniqueNames() )
                    {
                        nameMap.put( (Class<? extends IdentifiableObject>) clazz, map );
                    }
                    else
                    {
                        // add an empty map here, since we could still have some auto-generated properties
                        nameMap.put( (Class<? extends IdentifiableObject>) clazz, new HashMap<>() );

                        // find all auto-generated props and add them
                        for ( Map.Entry<String, IdentifiableObject> entry : map.entrySet() )
                        {
                            if ( entry.getValue().isAutoGenerated() )
                            {
                                nameMap.get( clazz ).put( entry.getKey(), entry.getValue() );
                            }
                        }
                    }
                }
            }
        }
    }

    private void populatePeriodTypeMap( Class<?> clazz )
    {
        Collection<Object> periodTypes = new ArrayList<>();

        if ( PeriodType.class.isAssignableFrom( clazz ) )
        {
            for ( PeriodType periodType : periodStore.getAllPeriodTypes() )
            {
                periodType = periodStore.reloadPeriodType( periodType );
                periodTypes.add( periodType );
                periodTypeMap.put( periodType.getName(), periodType );
            }
        }

        masterMap.put( clazz, new HashSet<>( periodTypes ) );
    }

    private void populateUsernameMap( Class<?> clazz )
    {
        if ( UserCredentials.class.isAssignableFrom( clazz ) )
        {
            Collection<UserCredentials> allUserCredentials = userService.getAllUserCredentials();
            allUserCredentials.stream().filter( userCredentials -> userCredentials.getUsername() != null )
                .forEach( userCredentials -> usernameMap.put( userCredentials.getUsername(), userCredentials ) );
        }
    }

    //-------------------------------------------------------------------------------------------------------
    // ObjectBridge Implementation
    //-------------------------------------------------------------------------------------------------------

    @Override
    public void saveObject( Object object )
    {
        saveObject( object, true );
    }

    @Override
    public void saveObject( Object object, boolean clearSharing )
    {
        if ( _typeSupported( object.getClass() ) && IdentifiableObject.class.isInstance( object ) )
        {
            if ( writeEnabled )
            {
                manager.save( (IdentifiableObject) object, clearSharing );
            }

            _updateInternalMaps( object, false );
        }
        else
        {
            log.warn( "Trying to save unsupported type + " + object.getClass() + " with object " + object + " object discarded." );
        }
    }

    @Override
    public void updateObject( Object object )
    {
        if ( _typeSupported( object.getClass() ) && IdentifiableObject.class.isInstance( object ) )
        {
            if ( writeEnabled )
            {
                AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_UPDATE );

                if ( IdentifiableObject.class.isInstance( object ) )
                {
                    ((BaseIdentifiableObject) object).setAutoFields();
                }

                sessionFactory.getCurrentSession().update( object );
            }

            _updateInternalMaps( object, false );
        }
        else
        {
            log.warn( "Trying to update unsupported type + " + object.getClass() + " with object " + object + " object discarded." );
        }
    }

    @Override
    public void deleteObject( Object object )
    {
        if ( _typeSupported( object.getClass() ) && IdentifiableObject.class.isInstance( object ) )
        {
            if ( writeEnabled )
            {
                deletionManager.execute( object );
                AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_DELETE );
                sessionFactory.getCurrentSession().delete( object );
            }

            _updateInternalMaps( object, true );
        }
        else
        {
            log.warn( "Trying to delete unsupported type + " + object.getClass() + " with object " + object + " object discarded." );
        }
    }

    @Override
    public <T> T getObject( T object )
    {
        Set<T> objects = findMatches( object );

        if ( objects.size() == 1 )
        {
            return objects.iterator().next();
        }
        else if ( !objects.isEmpty() )
        {
            String objectName;

            try
            {
                // several of our domain objects build toString based on several properties, which is not checked for
                // null, which means that a NPE is very likely.
                objectName = object.toString();
            }
            catch ( NullPointerException ignored )
            {
                objectName = "UNKNOWN_NAME (" + object.getClass().getName() + ")";
            }

            if ( objects.size() > 1 )
            {
                log.debug( "Multiple objects found for " + objectName + ", object discarded, returning null." );
            }
            else
            {
                log.debug( "No object found for " + objectName + ", returning null." );
            }
        }

        return null;
    }

    @Override
    public <T> Set<T> getObjects( T object )
    {
        return findMatches( object );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T> Set<T> getAllObjects( Class<T> clazz )
    {
        return (Set<T>) masterMap.get( clazz );
    }

    @Override
    public void setWriteEnabled( boolean enabled )
    {
        this.writeEnabled = enabled;
    }

    @Override
    public boolean isWriteEnabled()
    {
        return writeEnabled;
    }

    @Override
    public void setPreheatCache( boolean enabled )
    {
        this.preheatCache = enabled;
    }

    @Override
    public boolean isPreheatCache()
    {
        return preheatCache;
    }

    //-------------------------------------------------------------------------------------------------------
    // Internal Methods
    //-------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private <T> Set<T> findMatches( T object )
    {
        Set<T> objects = new HashSet<>();

        if ( DataElementCategoryOption.class.isInstance( object ) && ((DataElementCategoryOption) object).isDefault() )
        {
            objects.add( (T) dataElementCategoryService.getDefaultDataElementCategoryOption() );
            return objects;
        }

        if ( DataElementCategory.class.isInstance( object ) && ((DataElementCategory) object).isDefault() )
        {
            objects.add( (T) dataElementCategoryService.getDefaultDataElementCategory() );
            return objects;
        }

        if ( DataElementCategoryCombo.class.isInstance( object ) && ((DataElementCategoryCombo) object).isDefault() )
        {
            objects.add( (T) dataElementCategoryService.getDefaultDataElementCategoryCombo() );
            return objects;
        }

        if ( DataElementCategoryOptionCombo.class.isInstance( object ) && ((DataElementCategoryOptionCombo) object).isDefault() )
        {
            objects.add( (T) dataElementCategoryService.getDefaultDataElementCategoryOptionCombo() );
            return objects;
        }

        if ( PeriodType.class.isInstance( object ) )
        {
            PeriodType periodType = (PeriodType) object;
            periodType = periodTypeMap.get( periodType.getName() );

            if ( periodType != null )
            {
                objects.add( (T) periodType );
            }
        }

        if ( User.class.isInstance( object ) )
        {
            User user = (User) object;
            UserCredentials userCredentials = usernameMap.get( user.getUsername() );

            if ( userCredentials != null && userCredentials.getUserInfo() != null )
            {
                objects.add( (T) userCredentials.getUserInfo() );
            }
        }

        if ( OrganisationUnit.class.isInstance( object ) )
        {
            OrganisationUnit organisationUnit = (OrganisationUnit) getUuidMatch( (OrganisationUnit) object );

            if ( organisationUnit != null )
            {
                objects.add( (T) organisationUnit );
            }
        }

        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject identifiableObject = (IdentifiableObject) object;

            if ( identifiableObject.getUid() != null )
            {
                IdentifiableObject match = getUidMatch( identifiableObject );

                if ( match != null )
                {
                    objects.add( (T) match );
                }
            }

            if ( identifiableObject.getCode() != null && identifiableObject.haveUniqueCode() )
            {
                IdentifiableObject match = getCodeMatch( identifiableObject );

                if ( match != null )
                {
                    objects.add( (T) match );
                }
            }

            if ( (identifiableObject.haveUniqueNames() || identifiableObject.isAutoGenerated()) && identifiableObject.getName() != null )
            {
                IdentifiableObject match = getNameMatch( identifiableObject );

                if ( match != null )
                {
                    objects.add( (T) match );
                }
            }
        }

        return objects;
    }

    private <T> void _updateInternalMaps( T object, boolean delete )
    {
        if ( OrganisationUnit.class.isInstance( object ) )
        {
            OrganisationUnit organisationUnit = (OrganisationUnit) object;

            if ( !StringUtils.isEmpty( organisationUnit.getUuid() ) )
            {
                Map<String, IdentifiableObject> map = uuidMap.get( OrganisationUnit.class );

                if ( map == null )
                {
                    // might be dynamically sub-classed by javassist or cglib, fetch superclass and try again
                    map = uuidMap.get( OrganisationUnit.class.getSuperclass() );
                }

                if ( !delete )
                {
                    map.put( organisationUnit.getUuid(), organisationUnit );
                }
                else
                {
                    try
                    {
                        map.remove( organisationUnit.getUuid() );
                    }
                    catch ( NullPointerException ignored )
                    {
                    }
                }
            }
        }

        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject identifiableObject = (IdentifiableObject) object;

            if ( identifiableObject.getUid() != null )
            {
                Map<String, IdentifiableObject> map = uidMap.get( identifiableObject.getClass() );

                if ( map == null )
                {
                    // might be dynamically sub-classed by javassist or cglib, fetch superclass and try again
                    map = uidMap.get( identifiableObject.getClass().getSuperclass() );
                }

                if ( !delete )
                {
                    map.put( identifiableObject.getUid(), identifiableObject );
                }
                else
                {
                    try
                    {
                        map.remove( identifiableObject.getUid() );
                    }
                    catch ( NullPointerException ignored )
                    {
                    }
                }
            }

            if ( identifiableObject.getCode() != null && identifiableObject.haveUniqueCode() )
            {
                Map<String, IdentifiableObject> map = codeMap.get( identifiableObject.getClass() );

                if ( map == null )
                {
                    // might be dynamically sub-classed by javassist or cglib, fetch superclass and try again
                    map = codeMap.get( identifiableObject.getClass().getSuperclass() );
                }

                if ( !delete )
                {
                    map.put( identifiableObject.getCode(), identifiableObject );
                }
                else
                {
                    try
                    {
                        map.remove( identifiableObject.getCode() );
                    }
                    catch ( NullPointerException ignored )
                    {
                    }
                }
            }

            if ( (identifiableObject.haveUniqueNames() || identifiableObject.isAutoGenerated()) && identifiableObject.getName() != null )
            {
                Map<String, IdentifiableObject> map = nameMap.get( identifiableObject.getClass() );

                if ( map == null )
                {
                    // might be dynamically sub-classed by javassist or cglib, fetch superclass and try again
                    map = nameMap.get( identifiableObject.getClass().getSuperclass() );
                }

                if ( !delete )
                {
                    map.put( identifiableObject.getName(), identifiableObject );
                }
                else
                {
                    try
                    {
                        map.remove( identifiableObject.getName() );
                    }
                    catch ( NullPointerException ignored )
                    {
                    }
                }

            }
        }
    }

    private IdentifiableObject getUuidMatch( OrganisationUnit organisationUnit )
    {
        Class<? extends OrganisationUnit> klass = organisationUnit.getClass();
        Map<String, IdentifiableObject> map = uuidMap.get( klass );
        OrganisationUnit entity = null;

        if ( map != null )
        {
            entity = (OrganisationUnit) map.get( organisationUnit.getUuid() );
        }
        else
        {
            uuidMap.put( klass, new HashMap<>() );
            map = uuidMap.get( klass );
        }

        if ( entity == null )
        {
            entity = organisationUnitService.getOrganisationUnitByUuid( organisationUnit.getUuid() );

            if ( entity != null )
            {
                map.put( entity.getUuid(), entity );
            }
        }

        return entity;
    }

    private IdentifiableObject getUidMatch( IdentifiableObject identifiableObject )
    {
        Map<String, IdentifiableObject> map = uidMap.get( identifiableObject.getClass() );
        IdentifiableObject entity = null;

        if ( map != null )
        {
            entity = map.get( identifiableObject.getUid() );
        }
        else
        {
            uidMap.put( identifiableObject.getClass(), new HashMap<>() );
            map = uidMap.get( identifiableObject.getClass() );
        }

        if ( entity == null )
        {
            entity = manager.get( identifiableObject.getClass(), identifiableObject.getUid() );

            if ( entity != null )
            {
                map.put( entity.getUid(), entity );
            }
        }

        return entity;
    }

    private IdentifiableObject getCodeMatch( IdentifiableObject identifiableObject )
    {
        Map<String, IdentifiableObject> map = codeMap.get( identifiableObject.getClass() );
        IdentifiableObject entity = null;

        if ( map != null )
        {
            entity = map.get( identifiableObject.getCode() );
        }
        else
        {
            codeMap.put( identifiableObject.getClass(), new HashMap<>() );
            map = codeMap.get( identifiableObject.getClass() );
        }

        if ( entity != null && !entity.haveUniqueCode() )
        {
            return null;
        }

        if ( entity == null && identifiableObject.haveUniqueCode() )
        {
            entity = manager.getByCode( identifiableObject.getClass(), identifiableObject.getCode() );

            if ( entity != null )
            {
                map.put( entity.getCode(), entity );
            }
        }

        return entity;
    }

    private IdentifiableObject getNameMatch( IdentifiableObject identifiableObject )
    {
        Map<String, IdentifiableObject> map = nameMap.get( identifiableObject.getClass() );
        IdentifiableObject entity = null;

        if ( map != null )
        {
            entity = map.get( identifiableObject.getName() );
        }
        else
        {
            nameMap.put( identifiableObject.getClass(), new HashMap<>() );
            map = nameMap.get( identifiableObject.getClass() );
        }

        if ( entity != null && !entity.haveUniqueNames() )
        {
            return null;
        }

        if ( entity == null && identifiableObject.haveUniqueNames() )
        {
            entity = manager.getByName( identifiableObject.getClass(), identifiableObject.getName() );

            if ( entity != null )
            {
                map.put( entity.getName(), entity );
            }
        }

        return entity;
    }

    private boolean _typeSupported( Class<?> clazz )
    {
        for ( Class<?> c : supportedTypes )
        {
            if ( c.isAssignableFrom( clazz ) )
            {
                return true;
            }
        }

        return false;
    }
}
