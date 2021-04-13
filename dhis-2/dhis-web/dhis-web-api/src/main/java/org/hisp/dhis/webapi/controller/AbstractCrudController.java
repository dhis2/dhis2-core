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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.validateAndThrowErrors;
import static org.springframework.http.CacheControl.noCache;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.NamedParams;
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
import org.hisp.dhis.gist.GistAutoType;
import org.hisp.dhis.gist.GistQuery;
import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.gist.GistService;
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
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.sharing.SharingService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.webapi.JsonBuilder;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.PaginationUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Enums;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public abstract class AbstractCrudController<T extends IdentifiableObject>
{
    protected static final WebOptions NO_WEB_OPTIONS = new WebOptions( new HashMap<>() );

    protected static final String DEFAULTS = "INCLUDE";

    private final Cache<String, Long> paginationCountCache = new Cache2kBuilder<String, Long>()
    {
    }
        .expireAfterWrite( 1, TimeUnit.MINUTES )
        .build();

    // --------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------

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
    protected SchemaValidator schemaValidator;

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

    @Autowired
    protected UserService userService;

    @Autowired
    protected SharingService sharingService;

    @Autowired
    private GistService gistService;

    // --------------------------------------------------------------------------
    // GET
    // --------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/gist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody ResponseEntity<JsonNode> getObjectGist(
        @PathVariable( "uid" ) String uid,
        HttpServletRequest request, HttpServletResponse response )
        throws NotFoundException
    {
        return gistToJsonObjectResponse( uid, createGistQuery( request, getEntityClass(), GistAutoType.L )
            .withFilter( new Filter( "id", Comparison.EQ, uid ) ) );
    }

    @RequestMapping( value = "/gist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody ResponseEntity<JsonNode> getObjectListGist(
        HttpServletRequest request, HttpServletResponse response )
    {
        return gistToJsonArrayResponse( request, createGistQuery( request, getEntityClass(), GistAutoType.S ),
            getSchema() );
    }

    @RequestMapping( value = "/{uid}/{property}/gist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody ResponseEntity<JsonNode> getObjectPropertyGist(
        @PathVariable( "uid" ) String uid,
        @PathVariable( "property" ) String property,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Property objProperty = getSchema().getProperty( property );
        if ( objProperty == null )
        {
            throw new BadRequestException( "No such property: " + property );
        }
        if ( !objProperty.isCollection() )
        {
            return gistToJsonObjectResponse( uid, createGistQuery( request, getEntityClass(), GistAutoType.L )
                .withFilter( new Filter( "id", Comparison.EQ, uid ) )
                .withField( property ) );
        }
        GistQuery query = createGistQuery( request, (Class<IdentifiableObject>) objProperty.getItemKlass(),
            GistAutoType.M )
                .withOwner( Owner.builder()
                    .id( uid )
                    .type( getEntityClass() )
                    .collectionProperty( property ).build() );
        return gistToJsonArrayResponse( request, query,
            schemaService.getDynamicSchema( objProperty.getItemKlass() ) );
    }

    private static GistQuery createGistQuery( HttpServletRequest request,
        Class<? extends IdentifiableObject> elementType, GistAutoType autoDefault )
    {
        NamedParams params = new NamedParams( request::getParameter, request::getParameterValues );
        return GistQuery.builder()
            .elementType( elementType )
            .autoType( params.getEnum( "auto", autoDefault ) )
            .contextRoot( ContextUtils.getRootPath( request ) )
            .translationLocale( UserContext.getUserSetting( UserSettingKey.DB_LOCALE ) )
            .build()
            .with( params );
    }

    private ResponseEntity<JsonNode> gistToJsonObjectResponse( String uid, GistQuery query )
        throws NotFoundException
    {
        query = gistService.plan( query );
        List<?> elements = gistService.gist( query );
        JsonNode body = new JsonBuilder( jsonMapper ).skipNullOrEmpty().toArray( query.getFieldNames(), elements );
        if ( body.isEmpty() )
        {
            throw NotFoundException.notFoundUid( uid );
        }
        return ResponseEntity.ok().cacheControl( noCache().cachePrivate() ).body( body.get( 0 ) );
    }

    private ResponseEntity<JsonNode> gistToJsonArrayResponse( HttpServletRequest request,
        GistQuery query, Schema dynamicSchema )
    {
        query = gistService.plan( query );
        List<?> elements = gistService.gist( query );
        JsonBuilder responseBuilder = new JsonBuilder( jsonMapper );
        JsonNode body = responseBuilder.skipNullOrEmpty().toArray( query.getFieldNames(), elements );
        if ( !query.isHeadless() )
        {
            body = responseBuilder.toObject( asList( "pager", dynamicSchema.getPlural() ),
                gistService.pager( query, elements, request.getParameterMap() ), body );
        }
        return ResponseEntity.ok().cacheControl( noCache().cachePrivate() ).body( body );
    }

    @RequestMapping( method = RequestMethod.GET )
    public @ResponseBody RootNode getObjectList(
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, User currentUser )
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

        List<T> entities = getEntityList( metadata, options, filters, orders );

        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            long totalCount;

            if ( options.getOptions().containsKey( "query" ) )
            {
                totalCount = entities.size();

                long skip = (long) (options.getPage() - 1) * options.getPageSize();
                entities = entities.stream()
                    .skip( skip )
                    .limit( options.getPageSize() )
                    .collect( toList() );
            }
            else
            {
                String cacheKey = composePaginationCountKey( currentUser, filters, options );
                totalCount = paginationCountCache.computeIfAbsent( cacheKey,
                    () -> countTotal( options, filters, orders ) );
            }

            pager = new Pager( options.getPage(), totalCount, options.getPageSize() );
        }

        postProcessResponseEntities( entities, options, rpParameters );

        handleLinksAndAccess( entities, fields, false );

        handleAttributeValues( entities, fields );

        linkService.generatePagerLinks( pager, getEntityClass() );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.getConfig().setInclusionStrategy( getInclusionStrategy( rpParameters.get( "inclusionStrategy" ) ) );

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( getEntityClass(),
            new FieldFilterParams( entities, fields, Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) ) ) );

        cachePrivate( response );

        return rootNode;
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.GET )
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

        cachePrivate( response );

        return getObjectInternal( pvUid, rpParameters, filters, fields, user );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.GET )
    public @ResponseBody RootNode getObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        HttpServletResponse response )
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

            cachePrivate( response );

            return getObjectInternal( pvUid, rpParameters, Lists.newArrayList(),
                Lists.newArrayList( pvProperty + fieldFilter ), user );
        }
        finally
        {
            UserContext.reset();
        }
    }

    private void cachePrivate( HttpServletResponse response )
    {
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL,
            noCache().cachePrivate().getHeaderValue() );
    }

    @RequestMapping( value = "/{uid}/translations", method = RequestMethod.PUT )
    public void replaceTranslations(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );
        List<T> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        T persistedObject = entities.get( 0 );

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        T object = renderService.fromJson( request.getInputStream(), getEntityClass() );

        TypeReport typeReport = new TypeReport( Translation.class );

        List<Translation> objectTranslations = Lists.newArrayList( object.getTranslations() );

        for ( int idx = 0; idx < object.getTranslations().size(); idx++ )
        {
            ObjectReport objectReport = new ObjectReport( Translation.class, idx );
            Translation translation = objectTranslations.get( idx );

            if ( translation.getLocale() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "locale" ).setErrorKlass( getEntityClass() ) );
            }

            if ( translation.getProperty() == null )
            {
                objectReport.addErrorReport( new ErrorReport( Translation.class, ErrorCode.E4000, "property" )
                    .setErrorKlass( getEntityClass() ) );
            }

            if ( translation.getValue() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "value" ).setErrorKlass( getEntityClass() ) );
            }

            typeReport.addObjectReport( objectReport );

            if ( !objectReport.isEmpty() )
            {
                typeReport.getStats().incIgnored();
            }
        }

        if ( !typeReport.getErrorReports().isEmpty() )
        {
            WebMessage webMessage = WebMessageUtils.typeReport( typeReport );
            webMessageService.send( webMessage, response, request );
            return;
        }

        validateAndThrowErrors( () -> schemaValidator.validate( persistedObject ) );
        manager.updateTranslations( persistedObject, object.getTranslations() );

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PATCH )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void partialUpdateObject(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );
        List<T> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        T persistedObject = entities.get( 0 );

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        Patch patch = diff( request );

        prePatchEntity( persistedObject );
        patchService.apply( patch, persistedObject );
        validateAndThrowErrors( () -> schemaValidator.validate( persistedObject ) );
        manager.update( persistedObject );
        postPatchEntity( persistedObject );
    }

    private Patch diff( HttpServletRequest request )
        throws IOException,
        WebMessageException
    {
        ObjectMapper mapper = isJson( request ) ? jsonMapper : isXml( request ) ? xmlMapper : null;
        if ( mapper == null )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Unknown payload format." ) );
        }
        return patchService.diff( new PatchParams( mapper.readTree( request.getInputStream() ) ) );
    }

    @RequestMapping( value = "/{uid}/{property}", method = { RequestMethod.PATCH } )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void updateObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );

        List<T> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        if ( !getSchema().haveProperty( pvProperty ) )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Property " + pvProperty + " does not exist on " + getEntityName() ) );
        }

        Property property = getSchema().getProperty( pvProperty );
        T persistedObject = entities.get( 0 );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !property.isWritable() )
        {
            throw new UpdateAccessDeniedException( "This property is read-only." );
        }

        T object = deserialize( request );

        if ( object == null )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Unknown payload format." ) );
        }

        prePatchEntity( persistedObject );
        Object value = property.getGetterMethod().invoke( object );
        property.getSetterMethod().invoke( persistedObject, value );
        validateAndThrowErrors( () -> schemaValidator.validateProperty( property, object ) );
        manager.update( persistedObject );
        postPatchEntity( persistedObject );
    }

    @SuppressWarnings( "unchecked" )
    private RootNode getObjectInternal( String uid, Map<String, String> parameters,
        List<String> filters, List<String> fields, User user )
        throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        List<T> entities = getEntity( uid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), uid ) );
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, new ArrayList<>(),
            getPaginationData( options ), options.getRootJunction() );
        query.setUser( user );
        query.setObjects( entities );
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );

        entities = (List<T>) queryService.query( query );

        handleLinksAndAccess( entities, fields, true );

        handleAttributeValues( entities, fields );

        for ( T entity : entities )
        {
            postProcessResponseEntity( entity, options, parameters );
        }

        CollectionNode collectionNode = fieldFilterService.toCollectionNode( getEntityClass(),
            new FieldFilterParams( entities, fields, Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) )
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

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postObject( request, response, deserializeJsonEntity( request, response ) );
    }

    @RequestMapping( method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postObject( request, response, deserializeXmlEntity( request ) );
    }

    private void postObject( HttpServletRequest request, HttpServletResponse response, T parsed )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canCreate( user, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        parsed.getTranslations().clear();

        preCreateEntity( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL ).setUser( user ).setImportStrategy( ImportStrategy.CREATE )
            .addObject( parsed );

        postObject( request, response, getObjectReport( importService.importMetadata( params ) ) );
    }

    protected final void postObject( HttpServletRequest request, HttpServletResponse response,
        ObjectReport objectReport )
    {
        WebMessage webMessage = WebMessageUtils.objectReport( objectReport );

        if ( objectReport != null && webMessage.getStatus() == Status.OK )
        {
            String location = contextService.getApiPath() + getSchema().getRelativeApiEndpoint() + "/"
                + objectReport.getUid();

            webMessage.setHttpStatus( HttpStatus.CREATED );
            response.setHeader( ContextUtils.HEADER_LOCATION, location );
            T entity = manager.get( getEntityClass(), objectReport.getUid() );
            postCreateEntity( entity );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    private ObjectReport getObjectReport( ImportReport importReport )
    {
        if ( !importReport.getTypeReports().isEmpty() )
        {
            TypeReport typeReport = importReport.getTypeReports().get( 0 );

            if ( !typeReport.getObjectReports().isEmpty() )
            {
                return typeReport.getObjectReports().get( 0 );
            }
        }

        return null;
    }

    @RequestMapping( value = "/{uid}/favorite", method = RequestMethod.POST )
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

        List<T> entity = getEntity( pvUid );

        if ( entity.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        T object = entity.get( 0 );
        User user = currentUserService.getCurrentUser();

        object.setAsFavorite( user );
        manager.updateNoAcl( object );

        String message = String.format( "Object '%s' set as favorite for user '%s'", pvUid, user.getUsername() );
        webMessageService.send( WebMessageUtils.ok( message ), response, request );
    }

    @RequestMapping( value = "/{uid}/subscriber", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.OK )
    @SuppressWarnings( "unchecked" )
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
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
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

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        List<T> objects = getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, objects.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        T parsed = deserializeJsonEntity( request, response );
        ((BaseIdentifiableObject) parsed).setUid( pvUid );

        preUpdateEntity( objects.get( 0 ), parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.UPDATE )
            .addObject( parsed );

        if ( params.isSkipTranslation() )
        {
            // TODO this is a workaround to keep translations, preheat needs fix
            params.setSkipTranslation( false );
            T entity = manager.get( getEntityClass(), pvUid );
            ((BaseIdentifiableObject) parsed).setTranslations( new HashSet<>( entity.getTranslations() ) );
        }

        if ( params.isSkipSharing() )
        {
            // TODO this is a workaround to keep sharing
            params.setSkipSharing( false );
            T entity = manager.get( getEntityClass(), pvUid );
            ((BaseIdentifiableObject) parsed).setSharing( entity.getSharing() );
        }

        ImportReport importReport = importService.importMetadata( params );
        WebMessage webMessage = WebMessageUtils.objectReport( importReport );

        if ( importReport.getStatus() == Status.OK )
        {
            T entity = manager.get( getEntityClass(), pvUid );
            postUpdateEntity( entity );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_XML_VALUE } )
    public void putXmlObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        List<T> objects = getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, objects.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        T parsed = deserializeXmlEntity( request );
        ((BaseIdentifiableObject) parsed).setUid( pvUid );

        preUpdateEntity( objects.get( 0 ), parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.UPDATE )
            .addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );
        WebMessage webMessage = WebMessageUtils.objectReport( importReport );

        if ( importReport.getStatus() == Status.OK )
        {
            T entity = manager.get( getEntityClass(), pvUid );
            postUpdateEntity( entity );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    // --------------------------------------------------------------------------
    // DELETE
    // --------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.OK )
    public void deleteObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        List<T> objects = getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canDelete( user, objects.get( 0 ) ) )
        {
            throw new DeleteAccessDeniedException( "You don't have the proper permissions to delete this object." );
        }

        preDeleteEntity( objects.get( 0 ) );

        MetadataImportParams params = new MetadataImportParams()
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( user )
            .setImportStrategy( ImportStrategy.DELETE )
            .addObject( objects.get( 0 ) );

        ImportReport importReport = importService.importMetadata( params );

        postDeleteEntity();

        webMessageService.send( WebMessageUtils.objectReport( importReport ), response, request );
    }

    @RequestMapping( value = "/{uid}/favorite", method = RequestMethod.DELETE )
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

        List<T> entity = getEntity( pvUid );

        if ( entity.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        T object = entity.get( 0 );
        User user = currentUserService.getCurrentUser();

        object.removeAsFavorite( user );
        manager.updateNoAcl( object );

        String message = String.format( "Object '%s' removed as favorite for user '%s'", pvUid, user.getUsername() );
        webMessageService.send( WebMessageUtils.ok( message ), response, request );
    }

    @RequestMapping( value = "/{uid}/subscriber", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.OK )
    @SuppressWarnings( "unchecked" )
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
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
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

    @RequestMapping( value = "/{uid}/{property}/{itemId}", method = RequestMethod.GET )
    public @ResponseBody RootNode getCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        @RequestParam Map<String, String> parameters,
        TranslateParams translateParams,
        HttpServletResponse response )
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

            cachePrivate( response );

            return rootNode;
        }
        finally
        {
            UserContext.reset();
        }
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request )
        throws Exception
    {
        addCollectionItems( pvProperty, getEntity( pvUid ).get( 0 ),
            renderService.fromJson( request.getInputStream(), IdentifiableObjects.class ) );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request )
        throws Exception
    {
        addCollectionItems( pvProperty, getEntity( pvUid ).get( 0 ),
            renderService.fromXml( request.getInputStream(), IdentifiableObjects.class ) );
    }

    private void addCollectionItems( String pvProperty, T object, IdentifiableObjects items )
        throws Exception
    {
        preUpdateItems( object, items );
        collectionService.delCollectionItems( object, pvProperty, items.getDeletions() );
        collectionService.addCollectionItems( object, pvProperty, items.getAdditions() );
        postUpdateItems( object, items );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void replaceCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request )
        throws Exception
    {
        replaceCollectionItems( pvProperty, getEntity( pvUid ).get( 0 ),
            renderService.fromJson( request.getInputStream(), IdentifiableObjects.class ) );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void replaceCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request )
        throws Exception
    {
        replaceCollectionItems( pvProperty, getEntity( pvUid ).get( 0 ),
            renderService.fromXml( request.getInputStream(), IdentifiableObjects.class ) );
    }

    private void replaceCollectionItems( String pvProperty, T object, IdentifiableObjects items )
        throws Exception
    {
        preUpdateItems( object, items );
        collectionService.replaceCollectionItems( object, pvProperty, items.getIdentifiableObjects() );
        postUpdateItems( object, items );
    }

    @RequestMapping( value = "/{uid}/{property}/{itemId}", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletResponse response )
        throws Exception
    {
        List<T> objects = getEntity( pvUid );
        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        T object = objects.get( 0 );
        IdentifiableObjects items = new IdentifiableObjects();
        items.setAdditions( singletonList( new BaseIdentifiableObject( pvItemId, "", "" ) ) );

        preUpdateItems( object, items );
        collectionService.addCollectionItems( object, pvProperty, items.getIdentifiableObjects() );
        postUpdateItems( object, items );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request )
        throws Exception
    {
        deleteCollectionItems( pvProperty, getEntity( pvUid ).get( 0 ),
            renderService.fromJson( request.getInputStream(), IdentifiableObjects.class ) );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_XML_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request )
        throws Exception
    {
        deleteCollectionItems( pvProperty, getEntity( pvUid ).get( 0 ),
            renderService.fromXml( request.getInputStream(), IdentifiableObjects.class ) );
    }

    private void deleteCollectionItems( String pvProperty, T object, IdentifiableObjects items )
        throws Exception
    {
        preUpdateItems( object, items );
        collectionService.delCollectionItems( object, pvProperty, items.getIdentifiableObjects() );
        postUpdateItems( object, items );
    }

    @RequestMapping( value = "/{uid}/{property}/{itemId}", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletResponse response )
        throws Exception
    {
        List<T> objects = getEntity( pvUid );
        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        IdentifiableObjects items = new IdentifiableObjects();
        items.setIdentifiableObjects( singletonList( new BaseIdentifiableObject( pvItemId, "", "" ) ) );
        deleteCollectionItems( pvProperty, objects.get( 0 ), items );
    }

    @PutMapping( value = "/{uid}/sharing", consumes = "application/json" )
    public void setSharing( @PathVariable( "uid" ) String uid, HttpServletRequest request,
        HttpServletResponse response )
        throws WebMessageException,
        IOException
    {
        T entity = manager.get( getEntityClass(), uid );

        if ( entity == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), uid ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, entity ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        Sharing sharingObject = renderService.fromJson( request.getInputStream(), Sharing.class );

        TypeReport typeReport = new TypeReport( Sharing.class );

        typeReport.addObjectReport( sharingService.saveSharing( getEntityClass(), entity, sharingObject ) );

        if ( !typeReport.getErrorReports().isEmpty() )
        {
            WebMessage webMessage = WebMessageUtils.typeReport( typeReport );
            webMessageService.send( webMessage, response, request );
            return;
        }

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }

    // --------------------------------------------------------------------------
    // Hooks
    // --------------------------------------------------------------------------

    protected T deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        return renderService.fromJson( request.getInputStream(), getEntityClass() );
    }

    protected T deserializeXmlEntity( HttpServletRequest request )
        throws IOException
    {
        return renderService.fromXml( request.getInputStream(), getEntityClass() );
    }

    /**
     * Override to process entities after it has been retrieved from storage and
     * before it is returned to the view. Entities is null-safe.
     */
    protected void postProcessResponseEntities( List<T> entityList, WebOptions options, Map<String, String> parameters )
    {
    }

    /**
     * Override to process a single entity after it has been retrieved from
     * storage and before it is returned to the view. Entity is null-safe.
     */
    protected void postProcessResponseEntity( T entity, WebOptions options, Map<String, String> parameters )
        throws Exception
    {
    }

    protected void preCreateEntity( T entity )
        throws Exception
    {
    }

    protected void postCreateEntity( T entity )
    {
    }

    protected void preUpdateEntity( T entity, T newEntity )
        throws Exception
    {
    }

    protected void postUpdateEntity( T entity )
    {
    }

    protected void preDeleteEntity( T entity )
        throws Exception
    {
    }

    protected void postDeleteEntity()
    {
    }

    protected void prePatchEntity( T entity )
        throws Exception
    {
    }

    protected void postPatchEntity( T entity )
    {
    }

    protected void preUpdateItems( T entity, IdentifiableObjects items )
        throws Exception
    {
    }

    protected void postUpdateItems( T entity, IdentifiableObjects items )
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

    protected Pagination getPaginationData( WebOptions options )
    {
        return PaginationUtils.getPaginationData( options );
    }

    @SuppressWarnings( "unchecked" )
    protected List<T> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders )
        throws QueryParserException
    {
        List<T> entityList;
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
            entityList = (List<T>) queryService.query( query );
        }

        return entityList;
    }

    private long countTotal( WebOptions options, List<String> filters, List<Order> orders )
    {
        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, new Pagination(),
            options.getRootJunction() );

        return queryService.count( query );
    }

    private List<T> getEntity( String uid )
    {
        return getEntity( uid, NO_WEB_OPTIONS );
    }

    protected List<T> getEntity( String uid, WebOptions options )
    {
        ArrayList<T> list = new ArrayList<>();
        getEntity( uid, getEntityClass() ).ifPresent( list::add );
        return list; // TODO consider ACL
    }

    protected <E extends IdentifiableObject> java.util.Optional<E> getEntity( String uid, Class<E> entityType )
    {
        return java.util.Optional.ofNullable( manager.getNoAcl( entityType, uid ) );
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

    protected void handleLinksAndAccess( List<T> entityList, List<String> fields, boolean deep )
    {
        boolean generateLinks = hasHref( fields );

        if ( generateLinks )
        {
            linkService.generateLinks( entityList, deep );
        }
    }

    protected void handleAttributeValues( List<T> entityList, List<String> fields )
    {
        List<String> hasAttributeValues = fields.stream().filter( field -> field.contains( "attributeValues" ) )
            .collect( toList() );

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
    protected T deserialize( HttpServletRequest request )
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
            return renderService.fromJson( request.getInputStream(), getEntityClass() );
        }
        else if ( isCompatibleWith( type, MediaType.APPLICATION_XML ) )
        {
            return renderService.fromXml( request.getInputStream(), getEntityClass() );
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

    private Class<T> entityClass;

    private String entityName;

    private String entitySimpleName;

    @SuppressWarnings( "unchecked" )
    protected Class<T> getEntityClass()
    {
        if ( entityClass == null )
        {
            Type[] actualTypeArguments = ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments();
            entityClass = (Class<T>) actualTypeArguments[0];
        }

        return entityClass;
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

    private String composePaginationCountKey( User currentUser, List<String> filters, WebOptions options )
    {
        return currentUser.getUsername() + "." + getEntityName() + "." + String.join( "|", filters ) + "."
            + options.getRootJunction().name();
    }

}
