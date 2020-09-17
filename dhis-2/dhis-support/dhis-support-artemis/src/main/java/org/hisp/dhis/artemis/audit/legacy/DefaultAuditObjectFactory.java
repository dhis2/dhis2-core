package org.hisp.dhis.artemis.audit.legacy;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory for constructing {@see org.hisp.dhis.audit.Audit} data payloads.
 * This can be the object itself (as is the case for metadata), or it can be a
 * wrapper object collecting the parts wanted.
 *
 * @author Luciano Fiandesio
 */
@Slf4j
@Component
public class DefaultAuditObjectFactory implements AuditObjectFactory
{
    private final ObjectMapper objectMapper;

    /**
     * Cache for Fields of {@link org.hisp.dhis.audit.Auditable} classes Key is
     * class name. Value is Map of {@link AuditAttribute} Fields and its getter
     * Method
     */
    private final Map<String, Map<Field, Method>> cachedAuditAttributeFields = new ConcurrentHashMap<>();

    public DefaultAuditObjectFactory( @Qualifier("jsonMapper") ObjectMapper objectMapper )
    {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object create( AuditScope auditScope, AuditType auditType, Object object, String user )
    {
        switch ( auditScope )
        {
        case METADATA:
            return handleMetadataAudit( object );
        case TRACKER:
            return handleTracker( object );
        case AGGREGATE:
            return handleAggregate( object );
        }
        return null;
    }

    @Override
    public AuditAttributes collectAuditAttributes( Object auditObject )
    {
        AuditAttributes auditAttributes = new AuditAttributes();

        getAuditAttributeFields( auditObject.getClass() ).forEach( ( key, value ) -> {

            Object attributeObject = ReflectionUtils.invokeMethod( auditObject, value );

            if ( attributeObject instanceof IdentifiableObject )
            {
                auditAttributes.put( key.getName(), ((IdentifiableObject) attributeObject).getUid() );
            }
            else
            {
                auditAttributes.put( key.getName(), attributeObject );
            }
        } );

        return auditAttributes;
    }

    private Map<Field, Method> getAuditAttributeFields( Class<?> auditClass )
    {
        Map<Field, Method> map = cachedAuditAttributeFields.get( auditClass.getName() );

        if ( map == null )
        {
            map = AnnotationUtils.getAnnotatedFields( auditClass, AuditAttribute.class );
            cachedAuditAttributeFields.put( auditClass.getName(), map );
        }

        return map;
    }

    private String handleTracker( Object object )
    {
        return toJson( object );
    }

    private String handleAggregate( Object object )
    {
        return toJson( object );
    }

    private String handleMetadataAudit( Object object )
    {
        return toJson( object );
    }

    private String toJson( Object object )
    {
        try
        {
            return objectMapper.writeValueAsString( object );
        }
        catch ( JsonProcessingException e )
        {
            log.debug( DebugUtils.getStackTrace( e ) );
        }

        return null;
    }
}