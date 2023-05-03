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
package org.hisp.dhis.artemis.audit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.artemis.AuditProducerConfiguration;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Component
public class AuditManager
{
    private final AuditProducerSupplier auditProducerSupplier;

    private final AuditProducerConfiguration config;

    private final AuditScheduler auditScheduler;

    private final AuditMatrix auditMatrix;

    private final UsernameSupplier usernameSupplier;

    private final AuditObjectFactory objectFactory;

    /**
     * Cache for Fields of {@link org.hisp.dhis.audit.Auditable} classes Key is
     * class name. Value is Map of {@link AuditAttribute} Fields and its getter
     * Method
     */
    private static final Map<String, Map<Field, Method>> cachedAuditAttributeFields = new ConcurrentHashMap<>();

    public AuditManager(
        AuditProducerSupplier auditProducerSupplier,
        AuditScheduler auditScheduler,
        AuditProducerConfiguration config,
        AuditMatrix auditMatrix,
        AuditObjectFactory auditObjectFactory,
        UsernameSupplier usernameSupplier )
    {
        checkNotNull( auditProducerSupplier );
        checkNotNull( config );
        checkNotNull( auditMatrix );
        checkNotNull( auditObjectFactory );
        checkNotNull( usernameSupplier );

        this.auditProducerSupplier = auditProducerSupplier;
        this.config = config;
        this.auditScheduler = auditScheduler;
        this.auditMatrix = auditMatrix;
        this.objectFactory = auditObjectFactory;
        this.usernameSupplier = usernameSupplier;
    }

    public void send( Audit audit )
    {
        if ( !auditMatrix.isEnabled( audit ) || audit.getAuditableEntity() == null )
        {
            log.debug( "Audit message ignored:\n" + audit.toLog() );
            return;
        }

        if ( StringUtils.isEmpty( audit.getCreatedBy() ) )
        {
            audit.setCreatedBy( usernameSupplier.get() );
        }

        if ( audit.getData() == null )
        {
            audit.setData( this.objectFactory.create(
                audit.getAuditScope(),
                audit.getAuditType(),
                audit.getAuditableEntity().getEntity(),
                audit.getCreatedBy() ) );
        }

        if ( config.isUseQueue() )
        {
            auditScheduler.addAuditItem( audit );
        }
        else
        {
            auditProducerSupplier.publish( audit );
        }
    }

    public Map<Field, Method> getAuditAttributeFields( Class<?> auditClass )
    {
        Map<Field, Method> map = cachedAuditAttributeFields.get( auditClass.getName() );

        if ( map == null )
        {
            map = AnnotationUtils.getAnnotatedFields( auditClass, AuditAttribute.class );
            cachedAuditAttributeFields.put( auditClass.getName(), map );
        }

        return map;
    }

    public AuditAttributes collectAuditAttributes( Object entity, Class<?> entityClass )
    {
        AuditAttributes auditAttributes = new AuditAttributes();

        getAuditAttributeFields( entityClass ).forEach( ( field, getterMethod ) -> auditAttributes.put( field.getName(),
            getAttributeValue( entity, field.getName(), getterMethod ) ) );

        return auditAttributes;
    }

    private Object getAttributeValue( Object auditObject, String attributeName, Method getter )
    {
        if ( auditObject instanceof Map )
        {
            return ((Map<?, ?>) auditObject).get( attributeName );
        }

        Object value = ReflectionUtils.invokeMethod( auditObject, getter );

        if ( value instanceof IdentifiableObject )
        {
            return ((IdentifiableObject) value).getUid();
        }

        if ( value instanceof RelationshipItem )
        {
            RelationshipItem ri = (RelationshipItem) value;
            return ObjectUtils.firstNonNull( ri.getTrackedEntityInstance(), ri.getEnrollment(),
                ri.getEvent() ).getUid();
        }

        return value;
    }
}
