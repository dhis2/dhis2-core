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

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.audit.payloads.MetadataAuditPayload;
import org.hisp.dhis.audit.payloads.TrackedEntityAuditPayload;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for constructing @{@link org.hisp.dhis.audit.Audit} data payloads. This can be the object itself
 * (as is the case for metadata), or it can be a wrapper object collecting the parts wanted.
 *
 * @author Luciano Fiandesio
 */
@Component
public class DefaultAuditObjectFactory implements AuditObjectFactory
{
    private final RenderService renderService;

    private final CacheProvider cacheProvider;

    /**
     * Cache for Fields of {@link org.hisp.dhis.audit.Auditable} classes
     * Key is class name. Value is Map of {@link AuditAttribute} Fields and its getter Method
     */
    private Cache<Map<Field, Method>> cachedAuditAttributeFields;

    public DefaultAuditObjectFactory( RenderService renderService, CacheProvider cacheProvider )
    {
        this.renderService = renderService;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    public void initCache()
    {
        cachedAuditAttributeFields = cacheProvider.newCacheBuilder( Field.class, Method.class )
            .forRegion( "auditAttributeFields" )
            .withInitialCapacity( 500 )
            .withMaximumSize( 1000 )
            .build();
    }

    @Override
    public Object create( AuditScope auditScope, AuditType auditType, Object object, String user )
    {
        switch ( auditScope )
        {
            case METADATA:
                return handleMetadataAudit( auditType, object, user );
            case TRACKER:
                return handleTracker( auditType, object, user );
            case AGGREGATE:
                return handleAggregate( auditType, object, user );
        }

        return null;
    }

    @Override
    public AuditAttributes collectAuditAttributes( Object auditObject )
    {
        AuditAttributes auditAttributes = new AuditAttributes();

        getAuditAttributeFields( auditObject.getClass() ).entrySet().forEach( entry -> {

            Object attributeObject = ReflectionUtils.invokeMethod( auditObject, entry.getValue() );

            if ( !ReflectionUtils.isCollection( attributeObject ) )
            {
                if ( attributeObject instanceof IdentifiableObject )
                {
                    auditAttributes.put( entry.getKey().getName(), ( ( IdentifiableObject ) attributeObject).getUid() );
                }
                else
                {
                    auditAttributes.put( entry.getKey().getName(), attributeObject );
                }
            }
        } );

        return auditAttributes;
    }

    private Map<Field, Method> getAuditAttributeFields( Class<?> auditClass )
    {
        return cachedAuditAttributeFields.get( auditClass.getName(), a -> AnnotationUtils.getAnnotatedFields( auditClass, AuditAttribute.class ) ).orElse( new HashMap<>() );
    }

    private Object handleTracker( AuditType auditType, Object object, String user )
    {
        if ( !(object instanceof IdentifiableObject) )
        {
            return null;
        }

        return renderService.toJsonAsString( TrackedEntityAuditPayload.builder()
            .trackedEntityInstance( ( TrackedEntityInstance ) object )
            .build() );
    }

    private Object handleAggregate( AuditType auditType, Object object, String user )
    {
        return null;
    }

    private Object handleMetadataAudit( AuditType auditType, Object object, String user )
    {
        if ( !(object instanceof IdentifiableObject) )
        {
            return null;
        }

        return renderService.toJsonAsString( MetadataAuditPayload.builder()
            .identifiableObject( (IdentifiableObject) object )
            .build() );
    }
}
