package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = EnrollmentController.RESOURCE_PATH )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class EnrollmentController
{
    public static final String RESOURCE_PATH = "/enrollments";

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    protected FieldFilterService fieldFilterService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.GET )
    public @ResponseBody RootNode getEnrollments(
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) String trackedEntity,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) String enrollment,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( "enrollment,created,lastUpdated,trackedEntity,trackedEntityInstance,program,status,orgUnit,orgUnitName,enrollmentDate,incidentDate,followup" );
        }

        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        List<Enrollment> enrollments;

        if ( enrollment == null )
        {
            ProgramInstanceQueryParams params = programInstanceService.getFromUrl( orgUnits, ouMode, lastUpdated, program, programStatus, programStartDate,
                programEndDate, trackedEntity, trackedEntityInstance, followUp, page, pageSize, totalPages, skipPaging );

            enrollments = new ArrayList<>( enrollmentService.getEnrollments(
                programInstanceService.getProgramInstances( params ) ) );
        }
        else
        {
            Set<String> enrollmentIds = TextUtils.splitToArray( enrollment, TextUtils.SEMICOLON );
            enrollments = enrollmentIds != null ?  enrollmentIds.stream().map( enrollmentId -> enrollmentService.getEnrollment( enrollmentId ) ).collect( Collectors.toList() ) : null;
        }

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( fieldFilterService.filter( Enrollment.class, enrollments, fields ) );

        return rootNode;
    }

    @RequestMapping( value = "/{id}", method = RequestMethod.GET )
    public @ResponseBody Enrollment getEnrollment( @PathVariable String id, @RequestParam Map<String, String> parameters, Model model ) throws NotFoundException
    {
        return getEnrollment( id );
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_ENROLLMENT')" )
    public void postEnrollmentXml( @RequestParam( defaultValue = "CREATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = enrollmentService.addEnrollmentsXml( inputStream, importOptions );
        response.setContentType( MediaType.APPLICATION_XML_VALUE );

        if ( importSummaries.getImportSummaries().size() > 1 )
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            renderService.toXml( response.getOutputStream(), importSummaries );
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );

            if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
            {
                response.setHeader( "Location", getResourcePath( request, importSummary ) );
            }

            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
    }

    @RequestMapping( value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_ENROLLMENT')" )
    public void postEnrollmentJson( @RequestParam( defaultValue = "CREATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = enrollmentService.addEnrollmentsJson( inputStream, importOptions );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        if ( importSummaries.getImportSummaries().isEmpty() || importSummaries.getImportSummaries().size() > 1 )
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            renderService.toJson( response.getOutputStream(), importSummaries );
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );

            if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
            {
                response.setHeader( "Location", getResourcePath( request, importSummary ) );
            }

            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
    }

    @RequestMapping( value = "/{id}/note", method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    public void updateEnrollmentForNoteJson( @PathVariable String id, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = enrollmentService.updateEnrollmentForNoteJson( id, inputStream );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    public void updateEnrollmentXml( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = enrollmentService.updateEnrollmentXml( id, inputStream, importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    public void updateEnrollmentJson( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = enrollmentService.updateEnrollmentJson( id, inputStream, importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{id}/cancelled", method = RequestMethod.PUT )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void cancelEnrollment( @PathVariable String id ) throws NotFoundException, WebMessageException
    {
        if ( !programInstanceService.programInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Enrollment not found for ID " + id ) );
        }

        enrollmentService.cancelEnrollment( id );
    }

    @RequestMapping( value = "/{id}/completed", method = RequestMethod.PUT )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void completeEnrollment( @PathVariable String id ) throws NotFoundException, WebMessageException
    {
        if ( !programInstanceService.programInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Enrollment not found for ID " + id ) );
        }

        enrollmentService.completeEnrollment( id );
    }

    @RequestMapping( value = "/{id}/incompleted", method = RequestMethod.PUT )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void incompleteEnrollment( @PathVariable String id ) throws NotFoundException, WebMessageException
    {
        if ( !programInstanceService.programInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Enrollment not found for ID " + id ) );
        }

        enrollmentService.incompleteEnrollment( id );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{id}", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_UNENROLLMENT')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteEnrollment( @PathVariable String id, HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        if ( !programInstanceService.programInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Enrollment not found for ID " + id ) );
        }

        response.setStatus( HttpServletResponse.SC_OK );
        ImportSummary importSummary = enrollmentService.deleteEnrollment( id );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Enrollment getEnrollment( String id ) throws NotFoundException
    {
        Enrollment enrollment = enrollmentService.getEnrollment( id );

        if ( enrollment == null )
        {
            throw new NotFoundException( "Enrollment", id );
        }

        return enrollment;
    }

    private String getResourcePath( HttpServletRequest request, ImportSummary importSummary )
    {
        return ContextUtils.getContextPath( request ) + "/api/" + "enrollments" + "/" + importSummary.getReference();
    }
}
