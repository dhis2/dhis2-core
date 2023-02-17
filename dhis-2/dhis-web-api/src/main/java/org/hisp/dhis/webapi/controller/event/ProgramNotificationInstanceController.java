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
package org.hisp.dhis.webapi.controller.event;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationInstanceSchemaDescriptor;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Zubair Asghar
 */
@OpenApi.Tags( "tracker" )
@Controller
@RequestMapping( value = ProgramNotificationInstanceSchemaDescriptor.API_ENDPOINT )
@ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ProgramNotificationInstanceController
{
    private final ProgramNotificationInstanceService programNotificationInstanceService;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageInstanceService programStageInstanceService;

    public ProgramNotificationInstanceController(
        ProgramNotificationInstanceService programNotificationInstanceService,
        ProgramInstanceService programInstanceService,
        ProgramStageInstanceService programStageInstanceService )
    {
        this.programNotificationInstanceService = programNotificationInstanceService;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL')" )
    @GetMapping( produces = { "application/json" } )
    public @ResponseBody PagingWrapper<ProgramNotificationInstance> getScheduledMessage(
        @RequestParam( required = false ) String programInstance,
        @RequestParam( required = false ) String programStageInstance,
        @RequestParam( required = false ) Date scheduledAt,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false, defaultValue = "0" ) int page,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize )
    {
        ProgramNotificationInstanceParam params = ProgramNotificationInstanceParam.builder()
            .programInstance( programInstanceService.getProgramInstance( programInstance ) )
            .programStageInstance( programStageInstanceService.getProgramStageInstance( programStageInstance ) )
            .skipPaging( skipPaging )
            .page( page )
            .pageSize( pageSize )
            .scheduledAt( scheduledAt ).build();

        PagingWrapper<ProgramNotificationInstance> instancePagingWrapper = new PagingWrapper<>();

        if ( !skipPaging )
        {
            long total = programNotificationInstanceService.countProgramNotificationInstances( params );

            instancePagingWrapper = instancePagingWrapper.withPager(
                PagingWrapper.Pager.builder().page( page ).pageSize( pageSize )
                    .total( total ).build() );
        }

        programNotificationInstanceService.validateQueryParameters( params );

        List<ProgramNotificationInstance> instances = programNotificationInstanceService
            .getProgramNotificationInstances( params );

        return instancePagingWrapper.withInstances( instances );
    }
}
