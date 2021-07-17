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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.ENROLLMENT_IMPORT;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.enrollment.Enrollments;
import org.hisp.dhis.dxf2.events.enrollment.ImportEnrollmentsTask;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.event.mapper.EnrollmentCriteriaMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.EnrollmentCriteria;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = EnrollmentController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class EnrollmentController
{
    public static final String RESOURCE_PATH = "/enrollments";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    protected FieldFilterService fieldFilterService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private EnrollmentCriteriaMapper enrollmentCriteriaMapper;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @GetMapping
    public @ResponseBody RootNode getEnrollments(
        EnrollmentCriteria enrollmentCriteria )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add(
                "enrollment,created,lastUpdated,trackedEntityType,trackedEntityInstance,program,status,orgUnit,orgUnitName,enrollmentDate,incidentDate,followup" );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        List<Enrollment> listEnrollments;

        if ( enrollmentCriteria.getEnrollment() == null )
        {
            ProgramInstanceQueryParams params = enrollmentCriteriaMapper.getFromUrl(
                TextUtils.splitToArray( enrollmentCriteria.getOu(), TextUtils.SEMICOLON ),
                enrollmentCriteria.getOuMode(),
                enrollmentCriteria.getLastUpdated(),
                enrollmentCriteria.getLastUpdatedDuration(),
                enrollmentCriteria.getProgram(),
                enrollmentCriteria.getProgramStatus(),
                enrollmentCriteria.getProgramStartDate(),
                enrollmentCriteria.getProgramEndDate(),
                enrollmentCriteria.getTrackedEntityType(),
                enrollmentCriteria.getTrackedEntityInstance(),
                enrollmentCriteria.getFollowUp(),
                enrollmentCriteria.getPage(),
                enrollmentCriteria.getPageSize(),
                enrollmentCriteria.isTotalPages(),
                PagerUtils.isSkipPaging( enrollmentCriteria.getSkipPaging(), enrollmentCriteria.getPaging() ),
                enrollmentCriteria.isIncludeDeleted(),
                enrollmentCriteria.getOrder() );

            Enrollments enrollments = enrollmentService.getEnrollments( params );

            if ( enrollments.getPager() != null )
            {
                rootNode.addChild( NodeUtils.createPager( enrollments.getPager() ) );
            }

            listEnrollments = enrollments.getEnrollments();
        }
        else
        {
            Set<String> enrollmentIds = TextUtils.splitToArray( enrollmentCriteria.getEnrollment(),
                TextUtils.SEMICOLON );
            listEnrollments = enrollmentIds != null ? enrollmentIds.stream()
                .map( enrollmentId -> enrollmentService.getEnrollment( enrollmentId ) ).collect( Collectors.toList() )
                : null;
        }

        rootNode.addChild(
            fieldFilterService.toCollectionNode( Enrollment.class, new FieldFilterParams( listEnrollments, fields ) ) );

        return rootNode;
    }

    @GetMapping( "/{id}" )
    public @ResponseBody Enrollment getEnrollment( @PathVariable String id,
        @RequestParam Map<String, String> parameters, Model model )
        throws NotFoundException
    {
        return getEnrollment( id );
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @PostMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE )
    public void postEnrollmentJson( @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );

        if ( !importOptions.isAsync() )
        {
            ImportSummaries importSummaries = enrollmentService.addEnrollmentsJson( inputStream, importOptions );
            importSummaries.setImportOptions( importOptions );
            response.setContentType( MediaType.APPLICATION_JSON_VALUE );

            importSummaries.getImportSummaries().stream()
                .filter(
                    importSummary -> !importOptions.isDryRun() &&
                        !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                        !importOptions.getImportStrategy().isDelete() &&
                        (!importOptions.getImportStrategy().isSync()
                            || importSummary.getImportCount().getDeleted() == 0) )
                .forEach( importSummary -> importSummary.setHref(
                    ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() ) );

            if ( importSummaries.getImportSummaries().size() == 1 )
            {
                ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
                importSummary.setImportOptions( importOptions );

                if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                {
                    response.setHeader( "Location", getResourcePath( request, importSummary ) );
                }
            }

            response.setStatus( HttpServletResponse.SC_CREATED );
            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
        else
        {
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsJson( inputStream );
            startAsyncImport( importOptions, enrollments, request, response );
        }
    }

    @PostMapping( value = "", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE )
    public void postEnrollmentXml( @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );

        if ( !importOptions.isAsync() )
        {
            ImportSummaries importSummaries = enrollmentService.addEnrollmentsXml( inputStream, importOptions );
            importSummaries.setImportOptions( importOptions );
            response.setContentType( MediaType.APPLICATION_XML_VALUE );

            importSummaries.getImportSummaries().stream()
                .filter(
                    importSummary -> !importOptions.isDryRun() &&
                        !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                        !importOptions.getImportStrategy().isDelete() &&
                        (!importOptions.getImportStrategy().isSync()
                            || importSummary.getImportCount().getDeleted() == 0) )
                .forEach( importSummary -> importSummary.setHref(
                    ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() ) );

            if ( importSummaries.getImportSummaries().size() == 1 )
            {
                ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
                importSummary.setImportOptions( importOptions );

                if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                {
                    response.setHeader( "Location", getResourcePath( request, importSummary ) );
                }
            }

            response.setStatus( HttpServletResponse.SC_CREATED );
            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
        else
        {
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsXml( inputStream );
            startAsyncImport( importOptions, enrollments, request, response );
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @PostMapping( value = "/{id}/note", consumes = "application/json" )
    public void updateEnrollmentForNoteJson( @PathVariable String id, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = enrollmentService.updateEnrollmentForNoteJson( id, inputStream );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @PutMapping( value = "/{id}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE )
    public void updateEnrollmentXml( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = enrollmentService.updateEnrollmentXml( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @PutMapping( value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE )
    public void updateEnrollmentJson( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = enrollmentService.updateEnrollmentJson( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @PutMapping( "/{id}/cancelled" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void cancelEnrollment( @PathVariable String id )
        throws WebMessageException
    {
        if ( !programInstanceService.programInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Enrollment not found for ID " + id ) );
        }

        enrollmentService.cancelEnrollment( id );
    }

    @PutMapping( "/{id}/completed" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void completeEnrollment( @PathVariable String id )
        throws WebMessageException
    {
        if ( !programInstanceService.programInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Enrollment not found for ID " + id ) );
        }

        enrollmentService.completeEnrollment( id );
    }

    @PutMapping( "/{id}/incompleted" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void incompleteEnrollment( @PathVariable String id )
        throws WebMessageException
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

    @DeleteMapping( "/{id}" )
    public void deleteEnrollment( @PathVariable String id, HttpServletRequest request, HttpServletResponse response )
    {
        ImportSummary importSummary = enrollmentService.deleteEnrollment( id );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    /**
     * Starts an asynchronous enrollment task.
     *
     * @param importOptions the ImportOptions.
     * @param enrollments the enrollments to import.
     * @param request the HttpRequest.
     * @param response the HttpResponse.
     */
    private void startAsyncImport( ImportOptions importOptions, List<Enrollment> enrollments,
        HttpServletRequest request, HttpServletResponse response )
    {
        JobConfiguration jobId = new JobConfiguration( "inMemoryEventImport",
            ENROLLMENT_IMPORT, currentUserService.getCurrentUser().getUid(), true );
        taskExecutor
            .executeTask( new ImportEnrollmentsTask( enrollments, enrollmentService, importOptions, jobId ) );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + ENROLLMENT_IMPORT );
        webMessageService.send( jobConfigurationReport( jobId ), response, request );
    }

    private Enrollment getEnrollment( String id )
        throws NotFoundException
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
