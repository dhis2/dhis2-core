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
package org.hisp.dhis.artemis.audit.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.hisp.dhis.system.util.ReflectionUtils;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractHibernateListener
{
    protected final AuditManager auditManager;

    protected final AuditObjectFactory objectFactory;

    private final UsernameSupplier usernameSupplier;

    private final SchemaService schemaService;

    public AbstractHibernateListener(
        AuditManager auditManager,
        AuditObjectFactory objectFactory,
        UsernameSupplier usernameSupplier,
        SchemaService schemaService )
    {
        checkNotNull( auditManager );
        checkNotNull( objectFactory );
        checkNotNull( usernameSupplier );
        checkNotNull( schemaService );

        this.auditManager = auditManager;
        this.objectFactory = objectFactory;
        this.usernameSupplier = usernameSupplier;
        this.schemaService = schemaService;
    }

    Optional<Auditable> getAuditable( Object object, String type )
    {
        if ( AnnotationUtils.isAnnotationPresent( HibernateProxyUtils.getRealClass( object ), Auditable.class ) )
        {
            Auditable auditable = AnnotationUtils.getAnnotation( HibernateProxyUtils.getRealClass( object ),
                Auditable.class );

            boolean shouldAudit = Arrays.stream( auditable.eventType() )
                .anyMatch( s -> s.contains( "all" ) || s.contains( type ) );

            if ( shouldAudit )
            {
                return Optional.of( auditable );
            }
        }

        return Optional.empty();
    }

    public String getCreatedBy()
    {
        return usernameSupplier.get();
    }

    abstract AuditType getAuditType();

    /**
     * Create serializable Map<String, Object> for delete event Because the
     * entity has already been deleted and transaction is committed all lazy
     * collections or properties that haven't been loaded will be ignored.
     *
     * @return Map<String, Object> with key is property name and value is
     *         property value.
     */
    protected Object createAuditEntry( PostDeleteEvent event )
    {
        Map<String, Object> objectMap = new HashMap<>();
        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( event.getEntity() ) );
        Map<String, Property> properties = schema.getFieldNameMapProperties();

        for ( int i = 0; i < event.getDeletedState().length; i++ )
        {
            if ( event.getDeletedState()[i] == null )
            {
                continue;
            }

            Object value = event.getDeletedState()[i];
            String pName = event.getPersister().getPropertyNames()[i];
            Property property = properties.get( pName );

            if ( property == null || !property.isOwner() )
            {
                continue;
            }

            if ( Hibernate.isInitialized( value ) )
            {
                if ( property.isCollection()
                    && BaseIdentifiableObject.class.isAssignableFrom( property.getItemKlass() ) )
                {
                    objectMap.put( pName, IdentifiableObjectUtils.getUids( (Collection) value ) );
                }
                else
                {
                    objectMap.put( pName, getId( value ) );
                }
            }
        }
        return objectMap;
    }

    /**
     * Create serializable Map<String, Object> based on given Audit Entity and
     * related objects that are produced by {@link PostUpdateEvent} or
     * {@link PostInsertEvent} The returned object must comply with below rules:
     * 1. Only includes referenced properties that are owned by the current
     * Audit Entity. Means that the property's schema has attribute "owner =
     * true" 2. Do not include any lazy HibernateProxy or PersistentCollection
     * that is not loaded. 3. All referenced properties that extend
     * BaseIdentifiableObject should be mapped to only UID string
     *
     * @return Map<String, Object> with key is property name and value is
     *         property value.
     */
    protected Object createAuditEntry( Object entity, Object[] state, EventSource session, Serializable id,
        EntityPersister persister )
    {
        Map<String, Object> objectMap = new HashMap<>();
        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( entity ) );
        Map<String, Property> properties = schema.getFieldNameMapProperties();

        HibernateProxy entityProxy = null;

        for ( int i = 0; i < state.length; i++ )
        {
            if ( state[i] == null )
                continue;

            Object value = state[i];

            String pName = persister.getPropertyNames()[i];
            Property property = properties.get( pName );

            if ( shouldIgnoreProperty( property ) )
            {
                continue;
            }

            if ( shouldInitializeProxy( value ) || property.isEmbeddedObject() )
            {
                if ( entityProxy == null )
                {
                    entityProxy = createProxy( id, session, persister );
                }

                value = getPropertyValue( entityProxy, persister, pName );
            }

            if ( value == null )
            {
                continue;
            }

            putValueToMap( property, objectMap, value );
        }

        return objectMap;
    }

    private HibernateProxy createProxy( Serializable id, EventSource session, EntityPersister persister )
    {
        try
        {
            return (HibernateProxy) persister.createProxy( id, session );
        }
        catch ( Exception ex )
        {
            log.debug( "Couldn't create proxy " + DebugUtils.getStackTrace( ex ) );
        }

        return null;
    }

    private void handleNonIdentifiableCollection( Property property, Object value, Map<String, Object> objectMap )
    {
        if ( value == null )
            return;

        Schema schema = schemaService.getSchema( property.getItemKlass() );

        if ( schema == null )
        {
            objectMap.put( property.getFieldName(), value );
            return;
        }

        List<Map<String, Object>> listProperties = new ArrayList<>();

        List<Property> properties = schema.getProperties();
        Collection collection = (Collection) value;
        collection.forEach( item -> {
            Map<String, Object> propertyMap = new HashMap<>();
            properties.forEach( prop -> putValueToMap( prop, propertyMap,
                ReflectionUtils.invokeGetterMethod( prop.getFieldName(), item ) ) );
            listProperties.add( propertyMap );
        } );

        objectMap.put( property.getFieldName(), listProperties );
    }

    private void putValueToMap( Property property, Map<String, Object> objectMap, Object value )
    {
        if ( value == null )
            return;

        if ( property.isCollection() )
        {
            Collection collection = (Collection) value;

            if ( collection.isEmpty() )
                return;

            if ( BaseIdentifiableObject.class.isAssignableFrom( property.getItemKlass() ) )
            {
                List<String> uids = IdentifiableObjectUtils.getUids( collection );

                if ( uids != null && !uids.isEmpty() )
                {
                    objectMap.put( property.getFieldName(), uids );
                }
            }
            else
            {
                handleNonIdentifiableCollection( property, value, objectMap );
            }
        }
        else
        {
            objectMap.put( property.getFieldName(), getId( value ) );
        }
    }

    private Object getPropertyValue( HibernateProxy entityProxy, EntityPersister persister, String pName )
    {
        try
        {
            return persister.getPropertyValue( entityProxy, pName );
        }
        catch ( Exception ex )
        {
            // Ignore if couldn't find property reference object, maybe it was
            // deleted.
            log.debug( "Couldn't value of property: " + pName, DebugUtils.getStackTrace( ex ) );
        }

        return null;
    }

    private boolean shouldInitializeProxy( Object value )
    {
        if ( value == null || Hibernate.isInitialized( value ) )
        {
            return false;
        }

        return true;
    }

    private Object getId( Object object )
    {
        if ( BaseIdentifiableObject.class.isAssignableFrom( object.getClass() ) )
        {
            return ((BaseIdentifiableObject) object).getUid();
        }

        return object;
    }

    private boolean shouldIgnoreProperty( Property property )
    {
        return property == null || (!property.isOwner() && !property.isEmbeddedObject()) || !property.isReadable();
    }
}
