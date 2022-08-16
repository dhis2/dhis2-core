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
package org.hisp.dhis.cacheinvalidation.debezium;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;

/**
 * Startup routine responsible for pre-populating the table name to entity
 * lookup table {@link TableNameToEntityMapping} This class is executed before
 * the {@link StartupDebeziumServiceRoutine} which starts the Debezium engine
 * itself.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Profile( { "!test", "!test-h2" } )
@Conditional( value = DebeziumCacheInvalidationEnabledCondition.class )
public class DebeziumPreStartupRoutine extends AbstractStartupRoutine
{
    @PersistenceUnit
    private EntityManagerFactory emf;

    @Autowired
    private HibernateFlushListener hibernateFlushListener;

    @Autowired
    private TableNameToEntityMapping tableNameToEntityMapping;

    @Override

    public void execute()
        throws Exception
    {
        tableNameToEntityMapping.init();

        SessionFactoryImpl sessionFactory = emf.unwrap( SessionFactoryImpl.class );
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService( EventListenerRegistry.class );
        registry.getEventListenerGroup( EventType.FLUSH ).appendListener( hibernateFlushListener );
    }
}
