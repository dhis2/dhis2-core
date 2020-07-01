package org.hisp.dhis.dxf2.events.importer.context;

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

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.dxf2.events.importer.ServiceDelegator;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextServiceDelegatorSupplier" )
public class ServiceDelegatorSupplier implements Supplier<ServiceDelegator>
{
    private final ProgramInstanceStore programInstanceStore;

    private final TrackerAccessManager trackerAccessManager;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ProgramRuleVariableService programRuleVariableService;

    private final CurrentUserService currentUserService;

    private final ObjectMapper jsonMapper;

    private final JdbcTemplate jdbcTemplate;

    public ServiceDelegatorSupplier( ProgramInstanceStore programInstanceStore,
        TrackerAccessManager trackerAccessManager, ApplicationEventPublisher applicationEventPublisher,
        ProgramRuleVariableService programRuleVariableService, CurrentUserService currentUserService,
        ObjectMapper jsonMapper,
        @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate)
    {
        this.programInstanceStore = programInstanceStore;
        this.trackerAccessManager = trackerAccessManager;
        this.applicationEventPublisher = applicationEventPublisher;
        this.programRuleVariableService = programRuleVariableService;
        this.currentUserService = currentUserService;
        this.jsonMapper =jsonMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ServiceDelegator get()
    {
        return ServiceDelegator.builder()
            .programInstanceStore( this.programInstanceStore )
            .trackerAccessManager( this.trackerAccessManager )
            .applicationEventPublisher( this.applicationEventPublisher )
            .programRuleVariableService( this.programRuleVariableService )
            .currentUserService( this.currentUserService )
            .jsonMapper( jsonMapper )
            .jdbcTemplate( this.jdbcTemplate )
            .build();
    }
}
