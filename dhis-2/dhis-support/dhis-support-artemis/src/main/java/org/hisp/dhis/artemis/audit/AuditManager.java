package org.hisp.dhis.artemis.audit;

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



import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.artemis.AuditProducerConfiguration;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

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

    private final AuditObjectFactory objectFactory;

    private final SessionFactory sessionFactory;

    public AuditManager(
        AuditProducerSupplier auditProducerSupplier,
        AuditScheduler auditScheduler,
        AuditProducerConfiguration config,
        AuditMatrix auditMatrix,
        AuditObjectFactory auditObjectFactory,
        SessionFactory sessionFactory )
    {
        checkNotNull( auditProducerSupplier );
        checkNotNull( config );
        checkNotNull( auditMatrix );
        checkNotNull( auditObjectFactory );
        checkNotNull( sessionFactory );

        this.auditProducerSupplier = auditProducerSupplier;
        this.config = config;
        this.auditScheduler = auditScheduler;
        this.auditMatrix = auditMatrix;
        this.objectFactory = auditObjectFactory;
        this.sessionFactory = sessionFactory;
    }

    public void send( Audit audit )
    {
        if ( !auditMatrix.isEnabled( audit ) )
        {
            log.debug( "Audit message ignored:\n" + audit.toLog() );
            return;
        }

        Object entity = audit.getAuditableEntity().getEntity();

        if ( entity instanceof String )
        {
            audit.setData( entity );
        }
        else
        {

            Session session = sessionFactory.getCurrentSession();

            if ( !session.contains( entity ) &&  entity instanceof IdentifiableObject )
            {
                session.load( entity, ( ( IdentifiableObject ) entity ).getId() );
            }

            audit.setData( this.objectFactory.create(
                audit.getAuditScope(),
                audit.getAuditType(),
                audit.getAuditableEntity().getEntity(),
                audit.getCreatedBy() ) );
        }

        audit.setAttributes( this.objectFactory.collectAuditAttributes( audit.getAuditableEntity().getEntity() ) );

        if ( config.isUseQueue() )
        {
            auditScheduler.addAuditItem( audit );
        }
        else
        {
            auditProducerSupplier.publish( audit );
        }
    }
}
