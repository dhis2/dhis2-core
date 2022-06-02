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
package org.hisp.dhis.artemis.audit.listener;

import java.time.LocalDateTime;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@Component
public class PostInsertAuditListener
    extends AbstractHibernateListener implements PostCommitInsertEventListener
{
    public PostInsertAuditListener(
        AuditManager auditManager,
        AuditObjectFactory auditObjectFactory,
        UsernameSupplier userNameSupplier,
        SchemaService schemaService )
    {
        super( auditManager, auditObjectFactory, userNameSupplier, schemaService );
    }

    @Override
    AuditType getAuditType()
    {
        return AuditType.CREATE;
    }

    @Override
    public void onPostInsert( PostInsertEvent postInsertEvent )
    {
        getAuditable( postInsertEvent.getEntity(), "create" ).ifPresent( auditable -> auditManager.send( Audit.builder()
            .auditType( getAuditType() )
            .auditScope( auditable.scope() )
            .createdAt( LocalDateTime.now() )
            .createdBy( getCreatedBy() )
            .object( postInsertEvent.getEntity() )
            .attributes( auditManager.collectAuditAttributes( postInsertEvent.getEntity(),
                postInsertEvent.getEntity().getClass() ) )
            .auditableEntity(
                new AuditableEntity( postInsertEvent.getEntity().getClass(), createAuditEntry( postInsertEvent ) ) )
            .build() ) );

    }

    @Override
    public boolean requiresPostCommitHanding( EntityPersister entityPersister )
    {
        return true;
    }

    @Override
    public void onPostInsertCommitFailed( PostInsertEvent event )
    {
        log.info( "onPostInsertCommitFailed:event: " + event );
        log.info( "onPostInsertCommitFailed:id: " + event.getId() );
        log.info( "onPostInsertCommitFailed:state: " + Arrays.toString( event.getState() ) );

        try
        {
            if ( event.getEntity() != null )
            {
                log.info( "onPostInsertCommitFailed:entity.class: " + event.getEntity().getClass() );
                log.info( "onPostInsertCommitFailed:entity: "
                    + JacksonObjectMapperConfig.staticJsonMapper().writeValueAsString( event.getEntity() ) );
            }
        }
        catch ( JsonProcessingException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Create Audit entry for insert event
     */
    private Object createAuditEntry( PostInsertEvent postInsertEvent )
    {
        return super.createAuditEntry( postInsertEvent.getEntity(), postInsertEvent.getState(),
            postInsertEvent.getSession(), postInsertEvent.getId(), postInsertEvent.getPersister() );
    }
}
