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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummaries;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummary;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;
import static org.hisp.dhis.scheduling.JobType.TEI_IMPORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.TrackedEntityCriteriaMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.TrackedEntityInstanceCriteria;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.TrackedEntityInstanceSupportService;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.TrackedEntityInstanceStrategyHandler;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.request.TrackerEntityInstanceRequest;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * The following statements are added not to cause api break. They need to be
 * remove say in 2.26 or so once users are aware of the changes.
 * <p>
 * programEnrollmentStartDate= ObjectUtils.firstNonNull(
 * programEnrollmentStartDate, programStartDate ); programEnrollmentEndDate=
 * ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackedEntityInstanceController
{
    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

    private final ContextUtils contextUtils;

    private final FieldFilterService fieldFilterService;

    private final ContextService contextService;

    private final CurrentUserService currentUserService;

    private final FileResourceService fileResourceService;

    private final TrackerAccessManager trackerAccessManager;

    private final TrackedEntityInstanceSupportService trackedEntityInstanceSupportService;

    private final TrackedEntityCriteriaMapper criteriaMapper;

    private final TrackedEntityInstanceStrategyHandler trackedEntityInstanceStrategyHandler;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @GetMapping( produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_XML,
        ContextUtils.CONTENT_TYPE_TEXT_CSV } )
    public @ResponseBody RootNode getTrackedEntityInstances( TrackedEntityInstanceCriteria criteria,
        HttpServletResponse response )
    {
        List<String> fields = contextService.getFieldsFromRequestOrAll();

        TrackedEntityInstanceQueryParams queryParams = criteriaMapper.map( criteria );

        List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService.getTrackedEntityInstances(
            queryParams,
            getTrackedEntityInstanceParams( fields ), false, false );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( queryParams.isPaging() && queryParams.isTotalPages() )
        {
            int count = trackedEntityInstanceService.getTrackedEntityInstanceCount( queryParams, true, true );
            Pager pager = new Pager( queryParams.getPageWithDefault(), count, queryParams.getPageSizeWithDefault() );
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        if ( !StringUtils.isEmpty( criteria.getAttachment() ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=" + criteria.getAttachment() );
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( TrackedEntityInstance.class,
            new FieldFilterParams( trackedEntityInstances, fields ) ) );

        return rootNode;
    }

    @GetMapping( "/{teiId}/{attributeId}/image" )
    public void getAttributeImage(
        @PathVariable( "teiId" ) String teiId,
        @PathVariable( "attributeId" ) String attributeId,
        @RequestParam( required = false ) Integer width,
        @RequestParam( required = false ) Integer height,
        @RequestParam( required = false ) ImageFileDimension dimension,
        HttpServletResponse response )
        throws WebMessageException
    {
        User user = currentUserService.getCurrentUser();

        org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = instanceService
            .getTrackedEntityInstance( teiId );

        List<String> trackerAccessErrors = trackerAccessManager.canRead( user, trackedEntityInstance );

        List<TrackedEntityAttributeValue> attribute = trackedEntityInstance.getTrackedEntityAttributeValues().stream()
            .filter( val -> val.getAttribute().getUid().equals( attributeId ) )
            .collect( Collectors.toList() );

        if ( !trackerAccessErrors.isEmpty() )
        {
            throw new WebMessageException(
                unauthorized( "You're not authorized to access the TrackedEntityInstance with id: " + teiId ) );
        }

        if ( attribute.size() == 0 )
        {
            throw new WebMessageException( notFound( "Attribute not found for ID " + attributeId ) );
        }

        TrackedEntityAttributeValue value = attribute.get( 0 );

        if ( value == null )
        {
            throw new WebMessageException( notFound( "Value not found for ID " + attributeId ) );
        }

        if ( value.getAttribute().getValueType() != ValueType.IMAGE )
        {
            throw new WebMessageException( conflict( "Attribute must be of type image" ) );
        }

        // ---------------------------------------------------------------------
        // Get file resource
        // ---------------------------------------------------------------------

        FileResource fileResource = fileResourceService.getFileResource( value.getValue() );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException(
                notFound( "A data value file resource with id " + value.getValue() + " does not exist." ) );
        }

        if ( fileResource.getStorageStatus() != FileResourceStorageStatus.STORED )
        {
            // -----------------------------------------------------------------
            // The FileResource exists and is tied to DataValue, however the
            // underlying file content still not stored to external file store
            // -----------------------------------------------------------------

            throw new WebMessageException(
                conflict( "The content is being processed and is not available yet. Try again later.",
                    "The content requested is in transit to the file store and will be available at a later time." ) );
        }

        // ---------------------------------------------------------------------
        // Build response and return
        // ---------------------------------------------------------------------

        FileResourceUtils.setImageFileDimensions( fileResource,
            MoreObjects.firstNonNull( dimension, ImageFileDimension.ORIGINAL ) );

        response.setContentType( fileResource.getContentType() );
        response.setContentLengthLong( fileResource.getContentLength() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );

        try ( InputStream inputStream = fileResourceService.getFileResourceContent( fileResource ) )
        {
            BufferedImage img = ImageIO.read( inputStream );
            height = height == null ? img.getHeight() : height;
            width = width == null ? img.getWidth() : width;
            BufferedImage resizedImg = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );
            Graphics2D canvas = resizedImg.createGraphics();
            canvas.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
            canvas.drawImage( img, 0, 0, width, height, null );
            canvas.dispose();
            ImageIO.write( resizedImg, fileResource.getFormat(), response.getOutputStream() );
        }
        catch ( IOException ex )
        {
            throw new WebMessageException( error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend." ) );
        }
    }

    @GetMapping( value = "/query", produces = { ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_JAVASCRIPT } )
    public @ResponseBody Grid queryTrackedEntityInstancesJson( TrackedEntityInstanceCriteria criteria,
        HttpServletResponse response )
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE );
        return getGridByCriteria( criteria );
    }

    @GetMapping( value = "/query", produces = ContextUtils.CONTENT_TYPE_XML )
    public void queryTrackedEntityInstancesXml( TrackedEntityInstanceCriteria criteria,
        HttpServletResponse response )
        throws Exception
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.NO_CACHE );
        GridUtils.toXml( getGridByCriteria( criteria ), response.getOutputStream() );
    }

    @GetMapping( value = "/query", produces = ContextUtils.CONTENT_TYPE_EXCEL )
    public void queryTrackedEntityInstancesXls( TrackedEntityInstanceCriteria criteria,
        HttpServletResponse response )
        throws Exception
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.NO_CACHE );
        GridUtils.toXls( getGridByCriteria( criteria ), response.getOutputStream() );
    }

    @GetMapping( value = "/query", produces = ContextUtils.CONTENT_TYPE_TEXT_CSV )
    public void queryTrackedEntityInstancesCsv( TrackedEntityInstanceCriteria criteria,
        HttpServletResponse response )
        throws Exception
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_TEXT_CSV, CacheStrategy.NO_CACHE );
        GridUtils.toCsv( getGridByCriteria( criteria ), response.getWriter() );
    }

    @GetMapping( "/count" )
    public @ResponseBody int getTrackedEntityInstanceCount( TrackedEntityInstanceCriteria criteria )
    {
        criteria.setSkipMeta( true );
        criteria.setPage( TrackedEntityInstanceQueryParams.DEFAULT_PAGE );
        criteria.setPageSize( TrackedEntityInstanceQueryParams.DEFAULT_PAGE_SIZE );
        criteria.setTotalPages( true );
        criteria.setSkipPaging( true );
        criteria.setIncludeAllAttributes( false );
        criteria.setOrder( null );
        final TrackedEntityInstanceQueryParams queryParams = criteriaMapper.map( criteria );

        return trackedEntityInstanceService.getTrackedEntityInstanceCount( queryParams, false, false );
    }

    @GetMapping( value = "/{id}" )
    public @ResponseBody RootNode getTrackedEntityInstanceById(
        @PathVariable( "id" ) String pvId,
        @RequestParam( required = false ) String program )
    {
        List<String> fields = contextService.getFieldsFromRequestOrAll();

        CollectionNode collectionNode = fieldFilterService.toCollectionNode( TrackedEntityInstance.class,
            new FieldFilterParams( Lists.newArrayList(
                trackedEntityInstanceSupportService.getTrackedEntityInstance( pvId, program, fields ) ), fields ) );

        RootNode rootNode = new RootNode( collectionNode.getChildren().get( 0 ) );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @PostMapping( value = "", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage postTrackedEntityInstanceJson(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request )
        throws IOException,
        BadRequestException
    {
        return postTrackedEntityInstance( strategy, importOptions, request, APPLICATION_JSON_VALUE );
    }

    @PostMapping( value = "", consumes = APPLICATION_XML_VALUE, produces = APPLICATION_XML_VALUE )
    @ResponseBody
    public WebMessage postTrackedEntityInstanceXml(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request )
        throws IOException,
        BadRequestException
    {
        return postTrackedEntityInstance( strategy, importOptions, request, APPLICATION_XML_VALUE );
    }

    private WebMessage postTrackedEntityInstance( ImportStrategy strategy, ImportOptions importOptions,
        HttpServletRequest request, String mediaType )
        throws IOException,
        BadRequestException
    {
        importOptions.setStrategy( strategy );
        importOptions.setSkipLastUpdated( true );

        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );

        // For in memory Jobs
        JobConfiguration jobId = new JobConfiguration( "inMemoryEventImport",
            TEI_IMPORT, currentUserService.getCurrentUser().getUid(), true );

        TrackerEntityInstanceRequest trackerEntityInstanceRequest = TrackerEntityInstanceRequest.builder()
            .inputStream( inputStream )
            .importOptions( importOptions )
            .mediaType( mediaType )
            .jobConfiguration( jobId ).build();

        ImportSummaries importSummaries = trackedEntityInstanceStrategyHandler
            .mergeOrDeleteTrackedEntityInstances( trackerEntityInstanceRequest );

        if ( !importOptions.isAsync() )
        {
            ImportSummary singleSummary = finalizeTrackedEntityInstancePostRequest( importOptions, request,
                importSummaries );
            return importSummaries( importSummaries )
                .setLocation( singleSummary == null
                    ? null
                    : "/api/" + "trackedEntityInstances" + "/" + singleSummary.getReference() );
        }
        return jobConfigurationReport( jobId )
            .setLocation( "/system/tasks/" + TEI_IMPORT );
    }

    private ImportSummary finalizeTrackedEntityInstancePostRequest( ImportOptions importOptions,
        HttpServletRequest request, ImportSummaries importSummaries )
    {

        importSummaries.setImportOptions( importOptions );
        importSummaries.getImportSummaries().stream()
            .filter(
                importSummary -> !importOptions.isDryRun() &&
                    !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                    !importOptions.getImportStrategy().isDelete() &&
                    (!importOptions.getImportStrategy().isSync()
                        || importSummary.getImportCount().getDeleted() == 0) )
            .forEach( importSummary -> importSummary.setHref(
                ContextUtils.getRootPath( request ) + TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT + "/"
                    + importSummary.getReference() ) );

        if ( importSummaries.getImportSummaries().size() == 1 )
        {
            ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
            importSummary.setImportOptions( importOptions );

            if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
            {
                return importSummary;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @PutMapping( value = "/{id}", consumes = APPLICATION_XML_VALUE )
    @ResponseBody
    public WebMessage updateTrackedEntityInstanceXml(
        @PathVariable String id,
        @RequestParam( required = false ) String program,
        ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstanceXml( id, program,
            inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        return importSummary( importSummary );
    }

    @PutMapping( value = "/{id}", consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage updateTrackedEntityInstanceJson(
        @PathVariable String id,
        @RequestParam( required = false ) String program,
        ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstanceJson( id, program,
            inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        return importSummary( importSummary );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @DeleteMapping( "/{id}" )
    @ResponseBody
    public WebMessage deleteTrackedEntityInstance( @PathVariable String id )
    {
        ImportSummary importSummary = trackedEntityInstanceService.deleteTrackedEntityInstance( id );
        return importSummary( importSummary );
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Grid getGridByCriteria( TrackedEntityInstanceCriteria criteria )
    {
        criteria.setLastUpdatedDuration( null );
        criteria.setIncludeAllAttributes( false );
        final TrackedEntityInstanceQueryParams queryParams = criteriaMapper.map( criteria );

        return instanceService.getTrackedEntityInstancesGrid( queryParams );
    }

    private TrackedEntityInstanceParams getTrackedEntityInstanceParams( List<String> fields )
    {
        String joined = Joiner.on( "" ).join( fields );

        if ( joined.contains( "*" ) )
        {
            return TrackedEntityInstanceParams.TRUE;
        }

        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;

        if ( joined.contains( "relationships" ) )
        {
            params = params.withIncludeRelationships( true );
        }

        if ( joined.contains( "enrollments" ) )
        {
            params = params.withTeiEnrollmentParams(
                params.getTeiEnrollmentParams().withIncludeEnrollments( true ) );
        }

        if ( joined.contains( "events" ) )
        {
            params = params.withTeiEnrollmentParams(
                params.getTeiEnrollmentParams().withIncludeEvents( true ) );
        }

        if ( joined.contains( "programOwners" ) )
        {
            params = params.withIncludeProgramOwners( true );
        }

        return params;
    }
}
