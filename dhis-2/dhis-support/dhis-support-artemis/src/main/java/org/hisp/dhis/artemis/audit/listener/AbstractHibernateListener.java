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

import static com.cronutils.utils.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.hibernate.HibernateUtils;
import org.hisp.dhis.system.util.AnnotationUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractHibernateListener
{
    protected final String[] AUDIT_IGNORE_PROPERTIES = { "userAccesses", "userGroupAccesses", "user", "lastUpdatedBy" };
    final AuditManager auditManager;
    final AuditObjectFactory objectFactory;
    private final UsernameSupplier usernameSupplier;
    private final SessionFactory sessionFactory;

    public AbstractHibernateListener(
        AuditManager auditManager,
        AuditObjectFactory objectFactory,
        UsernameSupplier usernameSupplier,
        SessionFactory sessionFactory )
    {
        checkNotNull( auditManager );
        checkNotNull( objectFactory );
        checkNotNull( usernameSupplier );
        checkNotNull( sessionFactory );

        this.auditManager = auditManager;
        this.objectFactory = objectFactory;
        this.usernameSupplier = usernameSupplier;
        this.sessionFactory = sessionFactory;
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
     * Try to initialize all lazy loaded collections of the given Entity before sending
     * it to {@link AuditObjectFactory} for serializing to JSON
     * @param factory {@link SessionFactoryImplementor}
     * @param entity current Entity
     * @return the Entity with all collections loaded
     */
    protected Object initHibernateProxy( SessionFactoryImplementor factory, Object entity )
    {
        Session session = factory.getCurrentSession();

        try
        {
            if ( !session.contains( entity ) )
            {
                session.refresh( entity );
            }

            HibernateUtils.initializeProxy( entity );
        }
        catch ( LazyInitializationException e )
        {
            // LazyInitializationException could be caused session closed
            // Try again with new Session
            return initHibernateProxyWithNewSession( entity );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
        }

        return entity;
    }

    /**
     * Open new session for initializing lazy loaded collections of given Entity
     * then close the session.
     * @param entity
     * @return
     */
    private Object initHibernateProxyWithNewSession( Object entity )
    {
        Session session = sessionFactory.openSession();

        try
        {
            session.refresh( entity );
            HibernateUtils.initializeProxy( entity );
        }
        catch ( Exception e )
        {
            log.warn( DebugUtils.getStackTrace( e ) );
        }
        finally
        {
            session.close();
        }

        return entity;
    }
}