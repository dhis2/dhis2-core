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
package org.hisp.dhis.dxf2.events.importer.context;

import java.util.function.Supplier;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.dxf2.events.importer.EventImporterUserService;
import org.hisp.dhis.dxf2.events.importer.ServiceDelegator;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextServiceDelegatorSupplier" )
@RequiredArgsConstructor
public class ServiceDelegatorSupplier implements Supplier<ServiceDelegator>
{
    @NonNull
    private final ProgramInstanceStore programInstanceStore;

    @NonNull
    private final TrackerAccessManager trackerAccessManager;

    @NonNull
    private final ApplicationEventPublisher applicationEventPublisher;

    @NonNull
    private final ProgramRuleVariableService programRuleVariableService;

    @NonNull
    private final EventImporterUserService eventImporterUserService;

    @NonNull
    private final ObjectMapper jsonMapper;

    @NonNull
    @Qualifier( "readOnlyJdbcTemplate" )
    private final JdbcTemplate jdbcTemplate;

    @NonNull
    private final AuditManager auditManager;

    @Override
    public ServiceDelegator get()
    {
        return ServiceDelegator.builder()
            .programInstanceStore( programInstanceStore )
            .trackerAccessManager( trackerAccessManager )
            .applicationEventPublisher( applicationEventPublisher )
            .programRuleVariableService( programRuleVariableService )
            .eventImporterUserService( eventImporterUserService )
            .jsonMapper( jsonMapper )
            .jdbcTemplate( jdbcTemplate )
            .auditManager( auditManager )
            .build();
    }
}
