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

import java.util.List;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateParam;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland
 */
@OpenApi.Tags( "tracker" )
@Controller
@RequestMapping( value = ProgramNotificationTemplateSchemaDescriptor.API_ENDPOINT )
@ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ProgramNotificationTemplateController
    extends AbstractCrudController<ProgramNotificationTemplate>
{
    private final ProgramService programService;

    private final ProgramStageService programStageService;

    private final ProgramNotificationTemplateService programNotificationTemplateService;

    public ProgramNotificationTemplateController( ProgramService programService,
        ProgramStageService programStageService,
        ProgramNotificationTemplateService programNotificationTemplateService )
    {
        this.programService = programService;
        this.programStageService = programStageService;
        this.programNotificationTemplateService = programNotificationTemplateService;
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL')" )
    @GetMapping( produces = { "application/json" }, value = "/filter" )
    public @ResponseBody PagingWrapper<ProgramNotificationTemplate> getProgramNotificationTemplates(
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) String programStage,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false, defaultValue = "0" ) int page,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize )
    {
        ProgramNotificationTemplateParam params = ProgramNotificationTemplateParam.builder()
            .program( programService.getProgram( program ) )
            .programStage( programStageService.getProgramStage( programStage ) )
            .skipPaging( skipPaging )
            .page( page )
            .pageSize( pageSize )
            .build();

        PagingWrapper<ProgramNotificationTemplate> templatePagingWrapper = new PagingWrapper<>();

        if ( !skipPaging )
        {
            long total = programNotificationTemplateService.countProgramNotificationTemplates( params );

            templatePagingWrapper = templatePagingWrapper.withPager(
                PagingWrapper.Pager.builder().page( page ).pageSize( pageSize )
                    .total( total ).build() );
        }

        List<ProgramNotificationTemplate> instances = programNotificationTemplateService
            .getProgramNotificationTemplates( params );

        return templatePagingWrapper.withInstances( instances );
    }
}
