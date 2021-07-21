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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.visualization.ConversionHelper.convertToChartList;
import static org.hisp.dhis.visualization.ConversionHelper.convertToVisualization;
import static org.hisp.dhis.visualization.VisualizationType.PIVOT_TABLE;
import static org.hisp.dhis.webapi.utils.PaginationUtils.getPaginationData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SubscribableObject;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.collection.CollectionService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.config.InclusionStrategy;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.patch.Patch;
import org.hisp.dhis.patch.PatchParams;
import org.hisp.dhis.patch.PatchService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Enums;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Temporary class, in deprecation process. Avoid making changes here. This
 * class is just supporting the deprecation process of the ChartController.
 * <p>
 * It's just a Façade to keep it compatible with the new Visualization model.
 */
@Deprecated
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@SuppressWarnings( "unchecked" )
public abstract class ChartFacadeController
{
    protected static final WebOptions NO_WEB_OPTIONS = new WebOptions( new HashMap<>() );

    protected static final String DEFAULTS = "INCLUDE";

    // --------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------

    private Cache<String, Long> paginationCountCache = new Cache2kBuilder<String, Long>()
    {
    }
        .expireAfterWrite( 1, TimeUnit.MINUTES )
        .build();

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected FieldFilterService fieldFilterService;

    @Autowired
    protected AclService aclService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected LinkService linkService;

    @Autowired
    protected RenderService renderService;

    @Autowired
    protected MetadataImportService importService;

    @Autowired
    protected MetadataExportService exportService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected QueryService queryService;

    @Autowired
    protected WebMessageService webMessageService;

    @Autowired
    protected HibernateCacheManager hibernateCacheManager;

    @Autowired
    protected UserSettingService userSettingService;

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected MergeService mergeService;

    @Autowired
    protected PatchService patchService;

    @Autowired
    protected AttributeService attributeService;

    @Autowired
    protected ObjectMapper jsonMapper;

    @Autowired
    @Qualifier( "xmlMapper" )
    protected ObjectMapper xmlMapper;

    // --------------------------------------------------------------------------
    // GET
    // --------------------------------------------------------------------------

    @GetMapping
    public @ResponseBody RootNode getObjectList(
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, HttpServletRequest request, User currentUser )
        throws QueryParserException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<Order> orders = orderParams.getOrders( getSchema() );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        WebOptions options = new WebOptions( rpParameters );
        WebMetadata metadata = new WebMetadata();

        if ( !aclService.canRead( currentUser, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException(
                "You don't have the proper permissions to read objects of this type." );
        }

        // Force the load of Visualizations of Chart types only.
        // The only non-chart type, currently, is PIVOT_TABLE. So we only
        // exclude this
        // type.
        filters.add( "type:!eq:" + PIVOT_TABLE );

        List<Visualization> entities = getEntityList( metadata, options, filters, orders );

        // Conversion point
        List<Chart> charts = convertToChartList( entities );

        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            long count = paginationCountCache.computeIfAbsent(
                composePaginationCountKey( currentUser, filters, options ),
                () -> countTotal( options, filters, orders ) );
            pager = new Pager( options.getPage(), count, options.getPageSize() );
        }

        postProcessResponseEntities( charts, options, rpParameters );

        handleLinksAndAccess( charts, fields, false, currentUser );

        handleAttributeValues( charts, fields );

        linkService.generatePagerLinks( pager, Chart.class );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.getConfig().setInclusionStrategy( getInclusionStrategy( rpParameters.get( "inclusionStrategy" ) ) );

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( Chart.class,
            new FieldFilterParams( charts, fields, Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) ) ) );

        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );

        return rootNode;
    }

    @GetMapping( "/{uid}" )
    public @ResponseBody RootNode getObject(
        @PathVariable( "uid" ) String pvUid,
        @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canRead( user, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException(
                "You don't have the proper permissions to read objects of this type." );
        }

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );

        return getObjectInternal( pvUid, rpParameters, filters, fields, user );
    }

    @GetMapping( "/{uid}/{property}" )
    public @ResponseBody RootNode getObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !"translations".equals( pvProperty ) )
        {
            setUserContext( user, translateParams );
        }
        else
        {
            setUserContext( null, new TranslateParams( false ) );
        }

        try
        {
            if ( !aclService.canRead( user, getEntityClass() ) )
            {
                throw new ReadAccessDeniedException(
                    "You don't have the proper permissions to read objects of this type." );
            }

            List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

            if ( fields.isEmpty() )
            {
                fields.add( ":all" );
            }

            String fieldFilter = "[" + Joiner.on( ',' ).join( fields ) + "]";

            response.setHeader( ContextUtils.HEADER_CACHE_CONTROL,
                CacheControl.noCache().cachePrivate().getHeaderValue() );

            return getObjectInternal( pvUid, rpParameters, Lists.newArrayList(),
                Lists.newArrayList( pvProperty + fieldFilter ), user );
        }
        finally
        {
            UserContext.reset();
        }
    }

    @PutMapping( "/{uid}/translations" )
    public void replaceTranslations(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );
        List<Visualization> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        Visualization persistedObject = entities.get( 0 );

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        Chart chart = renderService.fromJson( request.getInputStream(), Chart.class );

        Visualization object = convertToVisualization( chart );

        TypeReport typeReport = new TypeReport( Translation.class );

        List<Translation> objectTranslations = Lists.newArrayList( object.getTranslations() );

        for ( int idx = 0; idx < object.getTranslations().size(); idx++ )
        {
            ObjectReport objectReport = new ObjectReport( Translation.class, idx );
            Translation translation = objectTranslations.get( idx );

            if ( translation.getLocale() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "locale" ).setErrorKlass( Chart.class ) );
            }

            if ( translation.getProperty() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "property" ).setErrorKlass( Chart.class ) );
            }

            if ( translation.getValue() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "value" ).setErrorKlass( Chart.class ) );
            }

            typeReport.addObjectReport( objectReport );

            if ( !objectReport.isEmpty() )
            {
                typeReport.getStats().incIgnored();
            }
        }

        if ( typeReport.hasErrorReports() )
        {
            WebMessage webMessage = WebMessageUtils.typeReport( typeReport );
            webMessageService.send( webMessage, response, request );
            return;
        }

        manager.updateTranslations( persistedObject, object.getTranslations() );
        manager.update( persistedObject );

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }

    @PatchMapping( "/{uid}" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void partialUpdateObject(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );
        List<Visualization> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        Visualization persistedObject = entities.get( 0 );

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        Patch patch = null;

        if ( isJson( request ) )
        {
            patch = patchService.diff(
                new PatchParams( jsonMapper.readTree( request.getInputStream() ) ) );
        }
        else if ( isXml( request ) )
        {
            patch = patchService.diff(
                new PatchParams( xmlMapper.readTree( request.getInputStream() ) ) );
        }

        patchService.apply( patch, persistedObject );
        manager.update( persistedObject );
    }

    @RequestMapping( value = "/{uid}/{property}", method = { RequestMethod.PUT, RequestMethod.PATCH } )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void updateObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );

        List<Visualization> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        if ( !getSchema().haveProperty( pvProperty ) )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Property " + pvProperty + " does not exist on " + getEntityName() ) );
        }

        Property property = getSchema().getProperty( pvProperty );
        Visualization persistedObject = entities.get( 0 );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !property.isWritable() )
        {
            throw new UpdateAccessDeniedException( "This property is read-only." );
        }

        Chart object = deserialize( request );

        if ( object == null )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Unknown payload format." ) );
        }

        Object value = property.getGetterMethod().invoke( object );

        property.getSetterMethod().invoke( persistedObject, value );

        manager.update( persistedObject );
    }

    private RootNode getObjectInternal( String uid, Map<String, String> parameters,
        List<String> filters, List<String> fields, User user )
        throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        List<Visualization> entities = getEntity( uid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, uid ) );
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, new ArrayList<>(),
            getPaginationData( options ), options.getRootJunction() );
        query.setUser( user );
        query.setObjects( entities );
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );

        entities = (List<Visualization>) queryService.query( query );

        // Conversion point
        List<Chart> charts = convertToChartList( entities );

        if ( CollectionUtils.isEmpty( charts ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, uid ) );
        }

        handleLinksAndAccess( charts, fields, true, user );

        handleAttributeValues( charts, fields );

        for ( Chart entity : charts )
        {
            postProcessResponseEntity( entity, options, parameters );
        }

        CollectionNode collectionNode = fieldFilterService.toCollectionNode( Chart.class,
            new FieldFilterParams( charts, fields, Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) )
                .setUser( user ) );

        if ( options.isTrue( "useWrapper" ) || entities.size() > 1 )
        {
            RootNode rootNode = NodeUtils.createMetadata( collectionNode );
            rootNode.getConfig().setInclusionStrategy( getInclusionStrategy( parameters.get( "inclusionStrategy" ) ) );

            return rootNode;
        }
        else
        {
            List<Node> children = collectionNode.getChildren();
            RootNode rootNode;

            if ( !children.isEmpty() )
            {
                rootNode = NodeUtils.createRootNode( children.get( 0 ) );
            }
            else
            {
                rootNode = NodeUtils.createRootNode( new ComplexNode( getSchema().getSingular() ) );
            }

            rootNode.getConfig().setInclusionStrategy( getInclusionStrategy( parameters.get( "inclusionStrategy" ) ) );

            return rootNode;
        }
    }

    // --------------------------------------------------------------------------
    // POST
    // --------------------------------------------------------------------------

    @PostMapping( consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canCreate( user, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        Chart parsed = deserializeJsonEntity( request, response );
        parsed.getTranslations().clear();

        final Visualization visualization = convertToVisualization( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.CREATE )
            .addObject( visualization );

        ImportReport importReport = importService.importMetadata( params );
        ObjectReport objectReport = getObjectReport( importReport );
        WebMessage webMessage = WebMessageUtils.objectReport( objectReport );

        if ( objectReport != null && webMessage.getStatus() == Status.OK )
        {
            String location = contextService.getApiPath() + getSchema().getRelativeApiEndpoint() + "/"
                + objectReport.getUid();

            webMessage.setHttpStatus( HttpStatus.CREATED );
            response.setHeader( ContextUtils.HEADER_LOCATION, location );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    @PostMapping( consumes = { "application/xml", "text/xml" } )
    public void postXmlObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canCreate( user, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        Chart parsed = deserializeXmlEntity( request, response );
        parsed.getTranslations().clear();

        final Visualization visualization = convertToVisualization( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.CREATE )
            .addObject( visualization );

        ImportReport importReport = importService.importMetadata( params );
        ObjectReport objectReport = getObjectReport( importReport );
        WebMessage webMessage = WebMessageUtils.objectReport( objectReport );

        if ( objectReport != null && webMessage.getStatus() == Status.OK )
        {
            String location = contextService.getApiPath() + getSchema().getRelativeApiEndpoint() + "/"
                + objectReport.getUid();

            webMessage.setHttpStatus( HttpStatus.CREATED );
            response.setHeader( ContextUtils.HEADER_LOCATION, location );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    @PostMapping( "/{uid}/favorite" )
    @ResponseStatus( HttpStatus.OK )
    public void setAsFavorite( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        if ( !getSchema().isFavoritable() )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Objects of this class cannot be set as favorite" ) );
        }

        List<Visualization> entity = (List<Visualization>) getEntity( pvUid );

        if ( entity.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        Visualization object = entity.get( 0 );
        User user = currentUserService.getCurrentUser();

        object.setAsFavorite( user );
        manager.updateNoAcl( object );

        String message = String.format( "Object '%s' set as favorite for user '%s'", pvUid, user.getUsername() );
        webMessageService.send( WebMessageUtils.ok( message ), response, request );
    }

    @PostMapping( "/{uid}/subscriber" )
    @ResponseStatus( HttpStatus.OK )
    public void subscribe( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        if ( !getSchema().isSubscribable() )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Objects of this class cannot be subscribed to" ) );
        }

        List<SubscribableObject> entity = (List<SubscribableObject>) getEntity( pvUid );

        if ( entity.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        SubscribableObject object = entity.get( 0 );
        User user = currentUserService.getCurrentUser();

        object.subscribe( user );
        manager.updateNoAcl( object );

        String message = String.format( "User '%s' subscribed to object '%s'", user.getUsername(), pvUid );
        webMessageService.send( WebMessageUtils.ok( message ), response, request );
    }

    // --------------------------------------------------------------------------
    // PUT
    // --------------------------------------------------------------------------

    @PutMapping( value = "/{uid}", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, objects.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        Chart parsed = deserializeJsonEntity( request, response );
        parsed.setUid( pvUid );

        final Visualization visualization = convertToVisualization( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.UPDATE )
            .addObject( visualization );

        ImportReport importReport = importService.importMetadata( params );
        WebMessage webMessage = WebMessageUtils.objectReport( importReport );

        if ( importReport.getStatus() != Status.OK )
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    @PutMapping( value = "/{uid}", consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE } )
    public void putXmlObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, objects.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        Chart parsed = deserializeXmlEntity( request, response );
        parsed.setUid( pvUid );

        final Visualization visualization = convertToVisualization( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.UPDATE )
            .addObject( visualization );

        ImportReport importReport = importService.importMetadata( params );
        WebMessage webMessage = WebMessageUtils.objectReport( importReport );

        if ( importReport.getStatus() != Status.OK )
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    // --------------------------------------------------------------------------
    // DELETE
    // --------------------------------------------------------------------------

    @DeleteMapping( "/{uid}" )
    @ResponseStatus( HttpStatus.OK )
    public void deleteObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canDelete( user, objects.get( 0 ) ) )
        {
            throw new DeleteAccessDeniedException( "You don't have the proper permissions to delete this object." );
        }

        MetadataImportParams params = new MetadataImportParams()
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.DELETE )
            .addObject( objects.get( 0 ) );

        ImportReport importReport = importService.importMetadata( params );

        webMessageService.send( WebMessageUtils.objectReport( importReport ), response, request );
    }

    @DeleteMapping( "/{uid}/favorite" )
    @ResponseStatus( HttpStatus.OK )
    public void removeAsFavorite( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        if ( !getSchema().isFavoritable() )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Objects of this class cannot be set as favorite" ) );
        }

        List<Visualization> entity = (List<Visualization>) getEntity( pvUid );

        if ( entity.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        Visualization object = entity.get( 0 );
        User user = currentUserService.getCurrentUser();

        object.removeAsFavorite( user );
        manager.updateNoAcl( object );

        String message = String.format( "Object '%s' removed as favorite for user '%s'", pvUid, user.getUsername() );
        webMessageService.send( WebMessageUtils.ok( message ), response, request );
    }

    @DeleteMapping( "/{uid}/subscriber" )
    @ResponseStatus( HttpStatus.OK )
    public void unsubscribe( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        if ( !getSchema().isSubscribable() )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Objects of this class cannot be subscribed to" ) );
        }

        List<SubscribableObject> entity = (List<SubscribableObject>) getEntity( pvUid );

        if ( entity.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        SubscribableObject object = entity.get( 0 );
        User user = currentUserService.getCurrentUser();

        object.unsubscribe( user );
        manager.updateNoAcl( object );

        String message = String.format( "User '%s' removed as subscriber of object '%s'", user.getUsername(), pvUid );
        webMessageService.send( WebMessageUtils.ok( message ), response, request );
    }

    // --------------------------------------------------------------------------
    // Identifiable object collections add, delete
    // --------------------------------------------------------------------------

    @GetMapping( "/{uid}/{property}/{itemId}" )
    public @ResponseBody RootNode getCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        @RequestParam Map<String, String> parameters,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();
        setUserContext( user, translateParams );

        try
        {
            if ( !aclService.canRead( user, getEntityClass() ) )
            {
                throw new ReadAccessDeniedException(
                    "You don't have the proper permissions to read objects of this type." );
            }

            RootNode rootNode = getObjectInternal( pvUid, parameters, Lists.newArrayList(),
                Lists.newArrayList( pvProperty + "[:all]" ), user );

            // TODO optimize this using field filter (collection filtering)
            if ( !rootNode.getChildren().isEmpty() && rootNode.getChildren().get( 0 ).isCollection() )
            {
                rootNode.getChildren().get( 0 ).getChildren().stream().filter( Node::isComplex ).forEach( node -> {
                    node.getChildren().stream()
                        .filter( child -> child.isSimple() && child.getName().equals( "id" )
                            && !((SimpleNode) child).getValue().equals( pvItemId ) )
                        .forEach( child -> rootNode.getChildren().get( 0 ).removeChild( node ) );
                } );
            }

            if ( rootNode.getChildren().isEmpty() || rootNode.getChildren().get( 0 ).getChildren().isEmpty() )
            {
                throw new WebMessageException(
                    WebMessageUtils.notFound( pvProperty + " with ID " + pvItemId + " could not be found." ) );
            }

            response.setHeader( ContextUtils.HEADER_CACHE_CONTROL,
                CacheControl.noCache().cachePrivate().getHeaderValue() );

            return rootNode;
        }
        finally
        {
            UserContext.reset();
        }
    }

    @PostMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        IdentifiableObjects identifiableObjects = renderService.fromJson( request.getInputStream(),
            IdentifiableObjects.class );

        collectionService.delCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( identifiableObjects.getDeletions() ) );
        collectionService.addCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( identifiableObjects.getAdditions() ) );
    }

    @PostMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_XML_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        IdentifiableObjects identifiableObjects = renderService.fromXml( request.getInputStream(),
            IdentifiableObjects.class );

        collectionService.delCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( identifiableObjects.getDeletions() ) );
        collectionService.addCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( identifiableObjects.getAdditions() ) );
    }

    @PutMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void replaceCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        IdentifiableObjects identifiableObjects = renderService.fromJson( request.getInputStream(),
            IdentifiableObjects.class );

        collectionService.replaceCollectionItems( objects.get( 0 ), pvProperty,
            identifiableObjects.getIdentifiableObjects() );
    }

    @PutMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_XML_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void replaceCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        IdentifiableObjects identifiableObjects = renderService.fromXml( request.getInputStream(),
            IdentifiableObjects.class );

        collectionService.replaceCollectionItems( objects.get( 0 ), pvProperty,
            identifiableObjects.getIdentifiableObjects() );
    }

    @PostMapping( "/{uid}/{property}/{itemId}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        response.setStatus( HttpServletResponse.SC_NO_CONTENT );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        collectionService.addCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( new BaseIdentifiableObject( pvItemId, "", "" ) ) );
    }

    @DeleteMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void deleteCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        IdentifiableObjects identifiableObjects = renderService.fromJson( request.getInputStream(),
            IdentifiableObjects.class );

        collectionService.delCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( identifiableObjects.getIdentifiableObjects() ) );
    }

    @DeleteMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_XML_VALUE )
    public void deleteCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        IdentifiableObjects identifiableObjects = renderService.fromXml( request.getInputStream(),
            IdentifiableObjects.class );

        collectionService.delCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( identifiableObjects.getIdentifiableObjects() ) );
    }

    @DeleteMapping( "/{uid}/{property}/{itemId}" )
    public void deleteCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Visualization> objects = (List<Visualization>) getEntity( pvUid );
        response.setStatus( HttpServletResponse.SC_NO_CONTENT );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( Chart.class, pvUid ) );
        }

        collectionService.delCollectionItems( objects.get( 0 ), pvProperty,
            Lists.newArrayList( new BaseIdentifiableObject( pvItemId, "", "" ) ) );
    }

    // --------------------------------------------------------------------------
    // Hooks
    // --------------------------------------------------------------------------

    protected Chart deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        return renderService.fromJson( request.getInputStream(), Chart.class );
    }

    protected Chart deserializeXmlEntity( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        return renderService.fromXml( request.getInputStream(), Chart.class );
    }

    /**
     * Override to process entities after it has been retrieved from storage and
     * before it is returned to the view. Entities is null-safe.
     */
    protected void postProcessResponseEntities( List<Chart> entityList, WebOptions options,
        Map<String, String> parameters )
    {
    }

    /**
     * Override to process a single entity after it has been retrieved from
     * storage and before it is returned to the view. Entity is null-safe.
     */
    protected void postProcessResponseEntity( Chart entity, WebOptions options, Map<String, String> parameters )
        throws Exception
    {
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------

    protected void setUserContext( TranslateParams translateParams )
    {
        setUserContext( currentUserService.getCurrentUser(), translateParams );
    }

    protected void setUserContext( User user, TranslateParams translateParams )
    {
        Locale dbLocale = getLocaleWithDefault( translateParams );
        UserContext.setUser( user );
        UserContext.setUserSetting( UserSettingKey.DB_LOCALE, dbLocale );
    }

    protected Locale getLocaleWithDefault( TranslateParams translateParams )
    {
        return translateParams.isTranslate()
            ? translateParams
                .getLocaleWithDefault( (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ) )
            : null;
    }

    protected List<Visualization> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders )
        throws QueryParserException
    {
        List<Visualization> entityList;
        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, getPaginationData( options ),
            options.getRootJunction() );
        query.setDefaultOrder();
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );

        if ( options.getOptions().containsKey( "query" ) )
        {
            entityList = Lists.newArrayList( manager.filter( getEntityClass(), options.getOptions().get( "query" ) ) );
        }
        else
        {
            entityList = (List<Visualization>) queryService.query( query );
        }

        return entityList;
    }

    private long countTotal( WebOptions options, List<String> filters, List<Order> orders )
    {
        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, new Pagination(),
            options.getRootJunction() );

        return queryService.count( query );
    }

    private String composePaginationCountKey( User currentUser, List<String> filters, WebOptions options )
    {
        return currentUser.getUsername() + "." + getEntityName() + "." + String.join( "|", filters ) + "."
            + options.getRootJunction().name();
    }

    private List<?> getEntity( String uid )
    {
        return getEntity( uid, NO_WEB_OPTIONS );
    }

    private ObjectReport getObjectReport( ImportReport importReport )
    {
        return importReport.getFirstObjectReport();
    }

    protected List<Visualization> getEntity( String uid, WebOptions options )
    {
        ArrayList<Visualization> list = new ArrayList<>();
        java.util.Optional<Visualization> identifiableObject = java.util.Optional
            .ofNullable( manager.getNoAcl( getEntityClass(), uid ) );

        identifiableObject.ifPresent( list::add );

        return list; // TODO consider ACL
    }

    private Schema schema;

    protected Schema getSchema()
    {
        if ( schema == null )
        {
            schema = schemaService.getDynamicSchema( getEntityClass() );
        }

        return schema;
    }

    protected Schema getSchema( Class<?> klass )
    {
        return schemaService.getDynamicSchema( klass );
    }

    private boolean fieldsContains( String match, List<String> fields )
    {
        for ( String field : fields )
        {
            // for now assume href/access if * or preset is requested
            if ( field.contains( match ) || field.equals( "*" ) || field.startsWith( ":" ) )
            {
                return true;
            }
        }

        return false;
    }

    protected boolean hasHref( List<String> fields )
    {
        return fieldsContains( "href", fields );
    }

    protected boolean hasAccess( List<String> fields )
    {
        return fieldsContains( "access", fields );
    }

    protected void handleLinksAndAccess( List<Chart> entityList, List<String> fields, boolean deep, User user )
    {
        boolean generateLinks = hasHref( fields );

        if ( generateLinks )
        {
            linkService.generateLinks( entityList, deep );
        }
    }

    protected void handleAttributeValues( List<Chart> entityList, List<String> fields )
    {
        List<String> hasAttributeValues = fields.stream().filter( field -> field.contains( "attributeValues" ) )
            .collect( Collectors.toList() );

        if ( !hasAttributeValues.isEmpty() )
        {
            attributeService.generateAttributes( entityList );
        }
    }

    private InclusionStrategy.Include getInclusionStrategy( String inclusionStrategy )
    {
        if ( inclusionStrategy != null )
        {
            Optional<InclusionStrategy.Include> optional = Enums.getIfPresent( InclusionStrategy.Include.class,
                inclusionStrategy );

            if ( optional.isPresent() )
            {
                return optional.get();
            }
        }

        return InclusionStrategy.Include.NON_NULL;
    }

    /**
     * Serializes an object, tries to guess output format using this order.
     *
     * @param request HttpServletRequest from current session
     * @param response HttpServletResponse from current session
     * @param object Object to serialize
     */
    protected void serialize( HttpServletRequest request, HttpServletResponse response, Object object )
        throws IOException
    {
        String type = request.getHeader( "Accept" );
        type = !StringUtils.isEmpty( type ) ? type : request.getContentType();
        type = !StringUtils.isEmpty( type ) ? type : MediaType.APPLICATION_JSON_VALUE;

        // allow type to be overridden by path extension
        if ( request.getPathInfo().endsWith( ".json" ) )
        {
            type = MediaType.APPLICATION_JSON_VALUE;
        }
        else if ( request.getPathInfo().endsWith( ".xml" ) )
        {
            type = MediaType.APPLICATION_XML_VALUE;
        }

        if ( isCompatibleWith( type, MediaType.APPLICATION_JSON ) )
        {
            renderService.toJson( response.getOutputStream(), object );
        }
        else if ( isCompatibleWith( type, MediaType.APPLICATION_XML ) )
        {
            renderService.toXml( response.getOutputStream(), object );
        }
    }

    /**
     * Deserializes a payload from the request, handles JSON/XML payloads
     *
     * @param request HttpServletRequest from current session
     * @return Parsed entity or null if invalid type
     */
    protected Chart deserialize( HttpServletRequest request )
        throws IOException
    {
        String type = request.getContentType();
        type = !StringUtils.isEmpty( type ) ? type : MediaType.APPLICATION_JSON_VALUE;

        // allow type to be overridden by path extension
        if ( request.getPathInfo().endsWith( ".json" ) )
        {
            type = MediaType.APPLICATION_JSON_VALUE;
        }
        else if ( request.getPathInfo().endsWith( ".xml" ) )
        {
            type = MediaType.APPLICATION_XML_VALUE;
        }

        if ( isCompatibleWith( type, MediaType.APPLICATION_JSON ) )
        {
            return renderService.fromJson( request.getInputStream(), Chart.class );
        }
        else if ( isCompatibleWith( type, MediaType.APPLICATION_XML ) )
        {
            return renderService.fromXml( request.getInputStream(), Chart.class );
        }

        return null;
    }

    /**
     * Are we receiving JSON data?
     *
     * @param request HttpServletRequest from current session
     * @return true if JSON compatible
     */
    protected boolean isJson( HttpServletRequest request )
    {
        String type = request.getContentType();
        type = !StringUtils.isEmpty( type ) ? type : MediaType.APPLICATION_JSON_VALUE;

        // allow type to be overridden by path extension
        if ( request.getPathInfo().endsWith( ".json" ) )
        {
            type = MediaType.APPLICATION_JSON_VALUE;
        }

        return isCompatibleWith( type, MediaType.APPLICATION_JSON );
    }

    /**
     * Are we receiving XML data?
     *
     * @param request HttpServletRequest from current session
     * @return true if XML compatible
     */
    protected boolean isXml( HttpServletRequest request )
    {
        String type = request.getContentType();
        type = !StringUtils.isEmpty( type ) ? type : MediaType.APPLICATION_JSON_VALUE;

        // allow type to be overridden by path extension
        if ( request.getPathInfo().endsWith( ".xml" ) )
        {
            type = MediaType.APPLICATION_XML_VALUE;
        }

        return isCompatibleWith( type, MediaType.APPLICATION_XML );
    }

    protected boolean isCompatibleWith( String type, MediaType mediaType )
    {
        try
        {
            return !StringUtils.isEmpty( type ) && MediaType.parseMediaType( type ).isCompatibleWith( mediaType );
        }
        catch ( Exception ignored )
        {
        }

        return false;
    }

    // --------------------------------------------------------------------------
    // Reflection helpers
    // --------------------------------------------------------------------------

    private String entityName;

    private String entitySimpleName;

    protected Class<Visualization> getEntityClass()
    {
        return Visualization.class;
    }

    protected String getEntityName()
    {
        if ( entityName == null )
        {
            entityName = getEntityClass().getName();
        }

        return entityName;
    }

    protected String getEntitySimpleName()
    {
        if ( entitySimpleName == null )
        {
            entitySimpleName = getEntityClass().getSimpleName();
        }

        return entitySimpleName;
    }
}
