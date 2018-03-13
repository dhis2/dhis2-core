package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.*;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The following statements are added not to cause api break.
 * They need to be remove say in 2.26 or so once users are aware of the changes.
 * <p>
 * programEnrollmentStartDate= ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
 * programEnrollmentEndDate= ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class TrackedEntityInstanceController
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private TrackerAccessManager trackerAccessManager;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.GET )
    public @ResponseBody RootNode getTrackedEntityInstances(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdatedStartDate,
        @RequestParam( required = false ) Date lastUpdatedEndDate,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) String order ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        RootNode rootNode = NodeUtils.createMetadata();

        List<TrackedEntityInstance> trackedEntityInstances;

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        TrackedEntityInstanceQueryParams queryParams = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, lastUpdatedStartDate, lastUpdatedEndDate, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntityType,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging, includeDeleted,
            getOrderParams( order ) );

        if ( trackedEntityInstance == null )
        {
            trackedEntityInstances = trackedEntityInstanceService.getTrackedEntityInstances( queryParams,
                getTrackedEntityInstanceParams( fields ) );
        }
        else
        {
            Set<String> trackedEntityInstanceIds = TextUtils.splitToArray( trackedEntityInstance, TextUtils.SEMICOLON );

            trackedEntityInstances = trackedEntityInstanceIds != null ? trackedEntityInstanceIds.stream()
                .map( id -> trackedEntityInstanceService.getTrackedEntityInstance( id, getTrackedEntityInstanceParams( fields ) ) )
                .collect( Collectors.toList() ) : null;
        }

        if ( queryParams.isPaging() && queryParams.isTotalPages() )
        {
            int count = trackedEntityInstanceService.getTrackedEntityInstanceCount( queryParams, true );
            Pager pager = new Pager( queryParams.getPageWithDefault(), count, queryParams.getPageSizeWithDefault() );
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( TrackedEntityInstance.class,
            new FieldFilterParams( trackedEntityInstances, fields ) ) );

        return rootNode;
    }

    private TrackedEntityInstanceParams getTrackedEntityInstanceParams( List<String> fields )
    {
        String joined = Joiner.on( "" ).join( fields );

        if ( joined.contains( "*" ) )
        {
            return TrackedEntityInstanceParams.TRUE;
        }

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        if ( joined.contains( "relationships" ) )
        {
            params.setIncludeRelationships( true );
        }

        if ( joined.contains( "enrollments" ) )
        {
            params.setIncludeEnrollments( true );
        }

        if ( joined.contains( "events" ) )
        {
            params.setIncludeEvents( true );
        }

        return params;
    }

    @RequestMapping( value = "/{teiId}/{attributeId}/image", method = RequestMethod.GET )
    public void getAttributeImage(
            @PathVariable( "teiId" ) String teiId,
            @PathVariable( "attributeId" ) String attributeId,
            @RequestParam( required = false ) Integer width,
            @RequestParam( required = false ) Integer height,
            HttpServletResponse response,
            HttpServletRequest request )
            throws WebMessageException, NotFoundException
    {
        User user = currentUserService.getCurrentUser();

        org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = instanceService.getTrackedEntityInstance( teiId );

        List<String> trackerAccessErrors = trackerAccessManager.canRead( user, trackedEntityInstance );

        List <TrackedEntityAttributeValue> attribute = trackedEntityInstance.getTrackedEntityAttributeValues().stream()
                .filter( val -> val.getAttribute().getUid().equals( attributeId ) )
                .collect( Collectors.toList() );

        if ( !trackerAccessErrors.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.unathorized( "You're not authorized to access the TrackedEntityInstance with id: " + teiId ) );
        }

        if ( attribute.size() == 0 )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Attribute not found for ID " + attributeId ) );
        }

        TrackedEntityAttributeValue value = attribute.get( 0 );

        if ( value == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Value not found for ID " + attributeId ) );
        }

        if ( value.getAttribute().getValueType() != ValueType.IMAGE )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Attribute must be of type image" ) );
        }

        // ---------------------------------------------------------------------
        // Get file resource
        // ---------------------------------------------------------------------

        FileResource fileResource = fileResourceService.getFileResource( value.getValue() );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "A data value file resource with id " + value.getValue() + " does not exist." ) );
        }

        if ( fileResource.getStorageStatus() != FileResourceStorageStatus.STORED )
        {
            // -----------------------------------------------------------------
            // The FileResource exists and is tied to DataValue, however the
            // underlying file content still not stored to external file store
            // -----------------------------------------------------------------

            throw new WebMessageException( WebMessageUtils.conflict( "The content is being processed and is not available yet. Try again later.",
                    "The content requested is in transit to the file store and will be available at a later time." ) );
        }

        ByteSource content = fileResourceService.getFileResourceContent( fileResource );

        if ( content == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "The referenced file could not be found" ) );
        }

        // ---------------------------------------------------------------------
        // Build response and return
        // ---------------------------------------------------------------------

        response.setContentType( fileResource.getContentType() );
        response.setContentLength( new Long( fileResource.getContentLength() ).intValue() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );

        URI uri = fileResourceService.getSignedGetFileResourceContentUri( value.getValue() );
        InputStream inputStream = null;

        try
        {

            inputStream = uri == null ? content.openStream() : uri.toURL().openStream();
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
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                    "There was an exception when trying to fetch the file from the storage backend. " +
                            "Depending on the provider the root cause could be network or file system related." ) );
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_JAVASCRIPT } )
    public @ResponseBody Grid queryTrackedEntityInstancesJson(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdatedStartDate,
        @RequestParam( required = false ) Date lastUpdatedEndDate,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) String order,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, lastUpdatedStartDate, lastUpdatedEndDate, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntityType,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging, includeDeleted,
            getOrderParams( order ) );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE );
        return instanceService.getTrackedEntityInstancesGrid( params );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_XML )
    public void queryTrackedEntityInstancesXml(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdatedStartDate,
        @RequestParam( required = false ) Date lastUpdatedEndDate,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) String order,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, lastUpdatedStartDate, lastUpdatedEndDate, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntityType,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging, includeDeleted,
            getOrderParams( order ) );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.NO_CACHE );
        Grid grid = instanceService.getTrackedEntityInstancesGrid( params );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_EXCEL )
    public void queryTrackedEntityInstancesXls(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdatedStartDate,
        @RequestParam( required = false ) Date lastUpdatedEndDate,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) String order,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, lastUpdatedStartDate, lastUpdatedEndDate, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntityType,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging, includeDeleted,
            getOrderParams( order ) );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.NO_CACHE );
        Grid grid = instanceService.getTrackedEntityInstancesGrid( params );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_CSV )
    public void queryTrackedEntityInstancesCsv(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdatedStartDate,
        @RequestParam( required = false ) Date lastUpdatedEndDate,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false ) boolean includeDeleted,
        @RequestParam( required = false ) String order,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, lastUpdatedStartDate, lastUpdatedEndDate, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntityType,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging, includeDeleted,
            getOrderParams( order ) );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.NO_CACHE );
        Grid grid = instanceService.getTrackedEntityInstancesGrid( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = "/count", method = RequestMethod.GET )
    public @ResponseBody int getTrackedEntityInstanceCount(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdatedStartDate,
        @RequestParam( required = false ) Date lastUpdatedEndDate,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean includeDeleted ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        TrackedEntityInstanceQueryParams queryParams = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, lastUpdatedStartDate, lastUpdatedEndDate, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntityType,
            eventStatus, eventStartDate, eventEndDate, true, TrackedEntityInstanceQueryParams.DEFAULT_PAGE, Pager.DEFAULT_PAGE_SIZE, true, true, includeDeleted,
            null );

        return trackedEntityInstanceService.getTrackedEntityInstanceCount( queryParams, false );
    }

    @RequestMapping( value = "/{id}", method = RequestMethod.GET )
    public @ResponseBody RootNode getTrackedEntityInstanceById( @PathVariable( "id" ) String pvId )
        throws NotFoundException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.toCollectionNode( TrackedEntityInstance.class,
            new FieldFilterParams( Lists.newArrayList( getTrackedEntityInstance( pvId, fields ) ), fields ) );

        RootNode rootNode = new RootNode( collectionNode.getChildren().get( 0 ) );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void postTrackedEntityInstanceJson( @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = trackedEntityInstanceService.addTrackedEntityInstanceJson( inputStream, importOptions );
        importSummaries.setImportOptions( importOptions );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        importSummaries.getImportSummaries().stream()
            .filter( importSummary -> !importOptions.isDryRun() && !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                !importOptions.getImportStrategy().isDelete() )
            .forEach( importSummary -> importSummary.setHref(
                ContextUtils.getRootPath( request ) + TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT + "/" + importSummary.getReference() ) );

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

    @RequestMapping( value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE )
    public void postTrackedEntityInstanceXml( @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = trackedEntityInstanceService.addTrackedEntityInstanceXml( inputStream, importOptions );
        response.setContentType( MediaType.APPLICATION_XML_VALUE );

        importSummaries.getImportSummaries().stream()
            .filter( importSummary -> !importOptions.isDryRun() && !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                !importOptions.getImportStrategy().isDelete() )
            .forEach( importSummary -> importSummary.setHref(
                ContextUtils.getRootPath( request ) + TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT + "/" + importSummary.getReference() ) );

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

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE )
    public void updateTrackedEntityInstanceXml( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstanceXml( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void updateTrackedEntityInstanceJson( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstanceJson( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{id}", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteTrackedEntityInstance( @PathVariable String id ) throws WebMessageException
    {
        if ( !instanceService.trackedEntityInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Tracked entity instance not found: " + id ) );
        }

        trackedEntityInstanceService.deleteTrackedEntityInstance( id );
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private TrackedEntityInstance getTrackedEntityInstance( String id, List<String> fields )
        throws NotFoundException
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( id,
            getTrackedEntityInstanceParams( fields ) );

        if ( trackedEntityInstance == null )
        {
            throw new NotFoundException( "TrackedEntityInstance", id );
        }

        return trackedEntityInstance;
    }

    private String getResourcePath( HttpServletRequest request, ImportSummary importSummary )
    {
        return ContextUtils.getContextPath( request ) + "/api/" + "trackedEntityInstances" + "/" + importSummary.getReference();
    }

    private List<String> getOrderParams( String order )
    {
        if ( order != null && !StringUtils.isEmpty( order ) )
        {

            return Arrays.asList( order.split( "," ) );
        }

        return null;
    }
}
