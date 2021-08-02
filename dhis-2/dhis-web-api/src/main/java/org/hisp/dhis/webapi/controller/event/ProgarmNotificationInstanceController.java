package org.hisp.dhis.webapi.controller.event;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.ProgramNotificationInstanceSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Zubair Asghar
 */

@Controller
@RequestMapping( value = ProgramNotificationInstanceSchemaDescriptor.API_ENDPOINT )
@ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ProgarmNotificationInstanceController
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final RenderService renderService;

    private final ProgramNotificationInstanceService programNotificationInstanceService;

    public ProgarmNotificationInstanceController( RenderService renderService, ProgramNotificationInstanceService programNotificationInstanceService )
    {
        this.renderService = renderService;
        this.programNotificationInstanceService = programNotificationInstanceService;
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL')" )
    @GetMapping( produces = { "application/json" } )
    public void getScheduledMessage(
            @RequestParam( required = false ) String programInstance,
            @RequestParam( required = false ) String programStageInstance,
            @RequestParam( required = false ) Date scheduledAt,
            @RequestParam( required = false ) Integer page, @RequestParam( required = false ) Integer pageSize,
            HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        ProgramNotificationInstanceParam params = ProgramNotificationInstanceParam.builder()
                .programInstance( programInstance )
                .programInstance( programStageInstance )
                .pageSize( pageSize )
                .page( page )
                .scheduledAt( scheduledAt ).build();

        programNotificationInstanceService.validateQueryParameters( params );

        List<ProgramNotificationInstance> instances = programNotificationInstanceService.getProgramNotificationInstances( params );

        renderService.toJson( response.getOutputStream(), instances );
    }
}
