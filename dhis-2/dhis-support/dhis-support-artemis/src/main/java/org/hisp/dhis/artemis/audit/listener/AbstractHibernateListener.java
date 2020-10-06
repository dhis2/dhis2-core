package org.hisp.dhis.artemis.audit.listener;

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

import com.sun.tools.rngom.parse.host.Base;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.adapter.UidJsonSerializer;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.AnnotationUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.cronutils.utils.Preconditions.checkNotNull;

/**
 * @author Luciano Fiandesio
 */
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
        if ( AnnotationUtils.isAnnotationPresent( object.getClass(), Auditable.class ) )
        {
            Auditable auditable = AnnotationUtils.getAnnotation( object.getClass(), Auditable.class );

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
     * Create Audit entry for update event
     */
    protected Object createAuditEntry( PostUpdateEvent postUpdateEvent )
    {
        return createAuditEntry( postUpdateEvent.getEntity(), postUpdateEvent.getState(), postUpdateEvent.getSession(), postUpdateEvent.getId(), postUpdateEvent.getPersister() );
    }

    /**
     * Create Audit entry for insert event
     */
    protected Object createAuditEntry( PostInsertEvent postInsertEvent )
    {
        return createAuditEntry( postInsertEvent.getEntity(), postInsertEvent.getState(), postInsertEvent.getSession(), postInsertEvent.getId(), postInsertEvent.getPersister() );
    }

    /**
     * Create Audit entry for delete event
     * Because the entity has already been deleted and transaction is committed
     * all lazy collection or properties that hasn't been loaded will be ignored.
     */
    protected Object createAuditEntry( PostDeleteEvent event )
    {
        Map<String,Object> objectMap = new HashMap<>();
        Schema schema = schemaService.getDynamicSchema( event.getEntity().getClass() );
        Map<String, Property> properties = schema.getFieldNameMapProperties();

        for ( int i = 0; i< event.getDeletedState().length; i++ )
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

            if ( Hibernate.isInitialized( value )  )
            {
                if ( property.isCollection() && BaseIdentifiableObject.class.isAssignableFrom( property.getItemKlass() ) )
                {
                    objectMap.put( pName, IdentifiableObjectUtils.getUids( ( Collection ) value ) );
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
     * Create Audit Entry base on given Entity
     * Only include lazy collections and properties with owner = true
     * For properties that are BaseIdentifiableObject, only include the uid of the object.
     */
    private Object createAuditEntry( Object entity, Object[] state, EventSource session, Serializable id, EntityPersister persister )
    {
        Map<String, Object> objectMap = new HashMap<>();
        Schema schema = schemaService.getDynamicSchema( entity.getClass() );
        Map<String, Property> properties = schema.getFieldNameMapProperties();

        HibernateProxy entityProxy = null;

        for ( int i = 0; i< state.length; i++ )
        {
            if ( state[i] == null )
            {
                continue;
            }

            Object value = state[i];

            String pName = persister.getPropertyNames()[i];
            Property property = properties.get( pName );

            if ( property == null || !property.isOwner() )
            {
                continue;
            }

            if ( !Hibernate.isInitialized( value ) )
            {
                if ( entityProxy == null )
                {
                    entityProxy = ( HibernateProxy ) persister.createProxy( id, session );
                }

                value =  persister.getPropertyValue( entityProxy, pName );
            }

            if ( value != null )
            {
                if ( property.isCollection() && BaseIdentifiableObject.class.isAssignableFrom( property.getItemKlass() ) &&
                    !EmbeddedObject.class.isAssignableFrom( property.getItemKlass() ) )
                {
                    objectMap.put( pName, IdentifiableObjectUtils.getUids( ( Collection ) value ) );
                }
                else
                {
                    objectMap.put( pName, getId( value ) );
                }
            }
        }

        return objectMap;
    }

    private Object getId( Object object )
    {
        if ( IdentifiableObject.class.isAssignableFrom( object.getClass() ) )
        {
            return ( (IdentifiableObject) object).getUid();
        }

        return object;
    }
}
