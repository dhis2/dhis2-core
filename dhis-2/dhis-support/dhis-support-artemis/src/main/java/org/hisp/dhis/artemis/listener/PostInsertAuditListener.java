package org.hisp.dhis.artemis.listener;

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

import java.util.Date;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.legacy.AuditLegacyObjectFactory;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class PostInsertAuditListener
    extends
    AbstractHibernateListener
    implements
    PostInsertEventListener
{

    public PostInsertAuditListener( AuditManager auditManager, AuditLegacyObjectFactory auditLegacyObjectFactory,
        CurrentUserService currentUserService )
    {
        super( auditManager, auditLegacyObjectFactory, currentUserService );
    }

    @Override
    public void onPostInsert( PostInsertEvent postInsertEvent )
    {
        Object entity = postInsertEvent.getEntity();
        
        if ( isAuditable( entity ) )
        {
            IdentifiableObject io = (IdentifiableObject) entity;
            
            auditManager.send(Audit.builder().withAuditType(AuditType.CREATE)
                .withAuditScope( getScope( entity ) )
                .withCreatedAt( new Date() )
                .withCreatedBy( currentUserService.getCurrentUsername() )
                .withObject( entity )
                .withData( this.legacyObjectFactory.create( getScope( entity ), AuditType.CREATE, io, currentUserService.getCurrentUsername()))
                    .build());
        }
    }

    @Override
    public boolean requiresPostCommitHanding( EntityPersister entityPersister )
    {
        return false;
    }
}
