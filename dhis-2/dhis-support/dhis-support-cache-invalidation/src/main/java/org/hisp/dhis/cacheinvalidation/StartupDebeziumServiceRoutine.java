/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.cacheinvalidation;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Profile( { "!test", "!test-h2" } )
@Slf4j
public class StartupDebeziumServiceRoutine extends AbstractStartupRoutine implements ApplicationContextAware
{
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @PersistenceUnit
    private EntityManagerFactory emf;

    @Autowired
    private HibernateFlushListener hibernateFlushListener;

    @Autowired
    private DhisConfigurationProvider config;

    @Autowired
    private DebeziumService debeziumService;

    @Autowired
    private DbChangeEventHandler dbChangeEventHandler;

    @Override

    public void execute()
        throws Exception
    {
        debeziumService.startDebeziumEngine();

        // final ScheduledExecutorService scheduler =
        // Executors.newScheduledThreadPool( 1 );
        // scheduler.schedule( this::start, 11, TimeUnit.SECONDS );
    }

    private void start()
    {
        try
        {
            debeziumService.startDebeziumEngine();
        }
        catch ( Exception e )
        {
            log.error( "was an error", e );
        }
        log.info( String.format( "DEBEZIUM STARTED!" ) );
    }
}
