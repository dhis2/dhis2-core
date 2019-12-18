package org.hisp.dhis.artemis.audit.legacy;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * A factory for constructing "legacy" Auditing objects. For legacy, it is intended objects that contains foreign keys
 * to other objects and are eventually going to be replaced by a key-less Audit object.
 *
 * @author Luciano Fiandesio
 */
@Component
public class DefaultAuditObjectFactory implements AuditObjectFactory
{
    private final RenderService renderService;

    public DefaultAuditObjectFactory( RenderService renderService )
    {
        this.renderService = renderService;
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

    private Object handleTracker( AuditType auditType, Object object, String user )
    {
        return null;
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

        IdentifiableObject identifiableObject = (IdentifiableObject) object;

        return new MetadataAudit()
            .setType( mapAuditType( auditType ) )
            .setCreatedAt( new Date() )
            .setCreatedBy( user ) // TODO was bundle.getUsername()
            .setKlass( identifiableObject.getClass().getName() )
            .setUid( identifiableObject.getUid() )
            .setCode( identifiableObject.getCode() )
            .setValue( renderService.toJsonAsString( identifiableObject ) );
    }

    private org.hisp.dhis.common.AuditType mapAuditType( AuditType auditType )
    {
        switch ( auditType )
        {
            case READ:
                return org.hisp.dhis.common.AuditType.READ;
            case CREATE:
                return org.hisp.dhis.common.AuditType.CREATE;
            case UPDATE:
                return org.hisp.dhis.common.AuditType.UPDATE;
            case SEARCH:
                return org.hisp.dhis.common.AuditType.SEARCH;
            case DELETE:
                return org.hisp.dhis.common.AuditType.DELETE;
            default:
                throw new IllegalArgumentException( "Invalid Audit Type" );
        }
    }
}
