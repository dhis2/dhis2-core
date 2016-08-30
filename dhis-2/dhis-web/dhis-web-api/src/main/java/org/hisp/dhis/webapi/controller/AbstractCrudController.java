package org.hisp.dhis.webapi.controller;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Enums;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.dxf2.common.Status;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.config.InclusionStrategy;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion.Version;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ApiVersion( { Version.DEFAULT, Version.ALL } )
public abstract class AbstractCrudController<T extends IdentifiableObject>
{
    protected static final WebOptions NO_WEB_OPTIONS = new WebOptions( new HashMap<>() );

    //--------------------------------------------------------------------------
    // Dependencies
    //--------------------------------------------------------------------------

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
    protected PreheatService preheatService;

    @Autowired
    protected NodeService nodeService;

    //--------------------------------------------------------------------------
    // GET
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET )
    public @ResponseBody RootNode getObjectList(
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams, OrderParams orderParams,
        HttpServletResponse response, HttpServletRequest request ) throws QueryParserException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<Order> orders = orderParams.getOrders( getSchema() );

        User user = currentUserService.getCurrentUser();
        setUserContext( user, translateParams );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        WebOptions options = new WebOptions( rpParameters );
        WebMetadata metadata = new WebMetadata();

        if ( !aclService.canRead( user, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to read objects of this type." );
        }

        List<T> entities = getEntityList( metadata, options, filters, orders, translateParams );
        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            pager = new Pager( options.getPage(), entities.size(), options.getPageSize() );
            entities = PagerUtils.pageCollection( entities, pager );
        }

        postProcessEntities( entities );
        postProcessEntities( entities, options, rpParameters, translateParams );

        handleLinksAndAccess( entities, fields, false, user );

        linkService.generatePagerLinks( pager, getEntityClass() );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.getConfig().setInclusionStrategy( getInclusionStrategy( rpParameters.get( "inclusionStrategy" ) ) );

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.filter( getEntityClass(), entities, fields ) );

        return rootNode;
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.GET )
    public @ResponseBody RootNode getObject(
        @PathVariable( "uid" ) String pvUid,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();
        setUserContext( user, translateParams );

        if ( !aclService.canRead( user, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to read objects of this type." );
        }

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        return getObjectInternal( pvUid, rpParameters, filters, fields, translateParams, user );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.GET )
    public @ResponseBody RootNode getObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !"translations".equals( pvProperty ) )
        {
            setUserContext( user, translateParams );
        }

        if ( !aclService.canRead( user, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to read objects of this type." );
        }

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        String fieldFilter = "[" + Joiner.on( ',' ).join( fields ) + "]";

        return getObjectInternal( pvUid, rpParameters, Lists.newArrayList(), Lists.newArrayList( pvProperty + fieldFilter ), translateParams, user );
    }

    @RequestMapping( value = "/{uid}/translations", method = RequestMethod.PUT )
    public void updateTranslations(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
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
        manager.updateTranslations( persistedObject, object.getTranslations() );

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PATCH )
    public void partialUpdateObject(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
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

        String payload = StreamUtils.copyToString( request.getInputStream(), Charset.forName( "UTF-8" ) );
        List<String> properties = new ArrayList<>();
        T object = null;

        if ( isJson( request ) )
        {
            properties = getJsonProperties( payload );
            object = renderService.fromJson( payload, getEntityClass() );
        }
        else if ( isXml( request ) )
        {
            properties = getXmlProperties( payload );
            object = renderService.fromXml( payload, getEntityClass() );
        }

        properties = getPersistedProperties( properties );

        if ( properties.isEmpty() || object == null )
        {
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
            return;
        }

        Schema schema = getSchema();

        for ( String keyProperty : properties )
        {
            Property property = schema.getProperty( keyProperty );

            Object value = property.getGetterMethod().invoke( object );
            property.getSetterMethod().invoke( persistedObject, value );
        }

        preheatService.refresh( persistedObject );
        manager.update( persistedObject );
    }

    private List<String> getJsonProperties( String payload ) throws IOException
    {
        ObjectMapper mapper = DefaultRenderService.getJsonMapper();
        JsonNode root = mapper.readTree( payload );

        return Lists.newArrayList( root.fieldNames() );
    }

    private List<String> getXmlProperties( String payload ) throws IOException
    {
        XmlMapper mapper = DefaultRenderService.getXmlMapper();
        JsonNode root = mapper.readTree( payload );

        return Lists.newArrayList( root.fieldNames() );
    }

    private List<String> getPersistedProperties( List<String> properties )
    {
        List<String> persistedProperties = new ArrayList<>();
        persistedProperties.addAll( properties.stream().filter( getSchema()::havePersistedProperty ).collect( Collectors.toList() ) );

        return persistedProperties;
    }

    @RequestMapping( value = "/{uid}/{property}", method = { RequestMethod.PUT, RequestMethod.PATCH } )
    public void updateObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );
        List<T> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        if ( !getSchema().haveProperty( pvProperty ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Property " + pvProperty + " does not exist on " + getEntityName() ) );
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

        Object value = property.getGetterMethod().invoke( object );

        property.getSetterMethod().invoke( persistedObject, value );

        preheatService.refresh( persistedObject );
        manager.update( persistedObject );
    }

    @SuppressWarnings( "unchecked" )
    private RootNode getObjectInternal( String uid, Map<String, String> parameters,
        List<String> filters, List<String> fields, TranslateParams translateParams, User user ) throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        List<T> entities = getEntity( uid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), uid ) );
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, new ArrayList<>(), options.getRootJunction() );
        query.setUser( user );
        query.setObjects( entities );

        entities = (List<T>) queryService.query( query );

        handleLinksAndAccess( entities, fields, true, user );

        for ( T entity : entities )
        {
            postProcessEntity( entity );
            postProcessEntity( entity, options, parameters, translateParams );
        }

        CollectionNode collectionNode = fieldFilterService.filter( getEntityClass(), entities, fields );

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

    //--------------------------------------------------------------------------
    // POST
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canCreate( user, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        T parsed = deserializeJsonEntity( request, response );
        parsed.getTranslations().clear();

        preCreateEntity( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setUser( user );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );
        ObjectReport objectReport = getObjectReport( importReport );
        WebMessage webMessage = WebMessageUtils.objectReport( objectReport );

        if ( objectReport != null && webMessage.getStatus() == Status.OK )
        {
            webMessage.setHttpStatus( HttpStatus.CREATED );
            response.setHeader( "Location", contextService.getApiPath() + getSchema().getRelativeApiEndpoint()
                + "/" + objectReport.getUid() );
            T entity = manager.get( objectReport.getUid() );
            postCreateEntity( entity );
        }

        webMessageService.send( webMessage, response, request );
    }

    @RequestMapping( method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canCreate( user, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        T parsed = deserializeXmlEntity( request, response );
        parsed.getTranslations().clear();

        preCreateEntity( parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setUser( user );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );
        ObjectReport objectReport = getObjectReport( importReport );
        WebMessage webMessage = WebMessageUtils.objectReport( objectReport );

        if ( objectReport != null && webMessage.getStatus() == Status.OK )
        {
            webMessage.setHttpStatus( HttpStatus.CREATED );
            response.setHeader( "Location", contextService.getApiPath() + getSchema().getRelativeApiEndpoint()
                + "/" + objectReport.getUid() );

            T entity = manager.get( objectReport.getUid() );
            postCreateEntity( entity );
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

    //--------------------------------------------------------------------------
    // PUT
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
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
        ((BaseIdentifiableObject) parsed).setTranslations( objects.get( 0 ).getTranslations() );

        preUpdateEntity( objects.get( 0 ), parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setUser( user );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );

        if ( importReport.getStatus() == Status.OK )
        {
            T entity = manager.get( pvUid );
            postUpdateEntity( entity );
        }

        webMessageService.send( WebMessageUtils.objectReport( importReport ), response, request );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE } )
    public void putXmlObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
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

        T parsed = deserializeXmlEntity( request, response );
        ((BaseIdentifiableObject) parsed).setUid( pvUid );
        ((BaseIdentifiableObject) parsed).setTranslations( objects.get( 0 ).getTranslations() );

        preUpdateEntity( objects.get( 0 ), parsed );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setUser( user );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );
        WebMessage objectReport = WebMessageUtils.objectReport( importReport );

        if ( importReport.getStatus() == Status.OK )
        {
            T entity = manager.get( pvUid );
            postUpdateEntity( entity );
        }

        webMessageService.send( objectReport, response, request );
    }

    //--------------------------------------------------------------------------
    // DELETE
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public void deleteObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
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

        MetadataImportParams importParams = new MetadataImportParams();
        importParams.setUser( user );
        importParams.setImportStrategy( ImportStrategy.DELETE );
        importParams.addObject( objects.get( 0 ) );

        ImportReport importReport = importService.importMetadata( importParams );

        postDeleteEntity();

        webMessageService.send( WebMessageUtils.objectReport( importReport ), response, request );
    }

    //--------------------------------------------------------------------------
    // Identifiable object collections add, delete
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/{property}/{itemId}", method = RequestMethod.GET )
    public @ResponseBody RootNode getCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        @RequestParam Map<String, String> parameters,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();
        setUserContext( user, translateParams );

        if ( !aclService.canRead( user, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to read objects of this type." );
        }

        RootNode rootNode = getObjectInternal( pvUid, parameters, Lists.newArrayList(), Lists.newArrayList( pvProperty + "[:all]" ), translateParams, user );

        // TODO optimize this using field filter (collection filtering)
        if ( !rootNode.getChildren().isEmpty() && rootNode.getChildren().get( 0 ).isCollection() )
        {
            rootNode.getChildren().get( 0 ).getChildren().stream().filter( Node::isComplex ).forEach( node ->
            {
                node.getChildren().stream()
                    .filter( child -> child.isSimple() && child.getName().equals( "id" ) && !((SimpleNode) child).getValue().equals( pvItemId ) )
                    .forEach( child -> rootNode.getChildren().get( 0 ).removeChild( node ) );
            } );
        }

        if ( rootNode.getChildren().isEmpty() || rootNode.getChildren().get( 0 ).getChildren().isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( pvProperty + " with ID " + pvItemId + " could not be found." ) );
        }

        return rootNode;
    }

    @RequestMapping( value = "/{uid}/{property}", method = { RequestMethod.POST, RequestMethod.PUT }, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void addCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        IdentifiableObjects identifiableObjects = renderService.fromJson( request.getInputStream(), IdentifiableObjects.class );

        for ( IdentifiableObject identifiableObject : identifiableObjects.getIdentifiableObjects() )
        {
            addCollectionItem( pvUid, pvProperty, identifiableObject.getUid(), request, response );
        }
    }

    @RequestMapping( value = "/{uid}/{property}", method = { RequestMethod.POST, RequestMethod.PUT }, consumes = MediaType.APPLICATION_XML_VALUE )
    public void addCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        IdentifiableObjects identifiableObjects = renderService.fromXml( request.getInputStream(), IdentifiableObjects.class );

        for ( IdentifiableObject identifiableObject : identifiableObjects.getIdentifiableObjects() )
        {
            addCollectionItem( pvUid, pvProperty, identifiableObject.getUid(), request, response );
        }
    }

    @RequestMapping( value = "/{uid}/{property}/{itemId}", method = { RequestMethod.POST, RequestMethod.PUT } )
    @SuppressWarnings( "unchecked" )
    public void addCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<T> objects = getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        if ( !getSchema().haveProperty( pvProperty ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Property " + pvProperty + " does not exist on " + getEntityName() ) );
        }

        Property property = getSchema().getProperty( pvProperty );

        if ( !property.isCollection() || !property.isIdentifiableObject() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Only adds within identifiable collection are allowed." ) );
        }

        IdentifiableObject inverseObject = manager.getNoAcl( (Class<? extends IdentifiableObject>) property.getItemKlass(), pvItemId );
        IdentifiableObject owningObject = objects.get( 0 );

        if ( inverseObject == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Collection " + pvProperty + " does not have an item with ID: " + pvItemId ) );
        }

        Collection<IdentifiableObject> collection;

        if ( property.isOwner() )
        {
            collection = (Collection<IdentifiableObject>) property.getGetterMethod().invoke( owningObject );
        }
        else
        {
            Schema owningSchema = getSchema( property.getItemKlass() );
            Property owningProperty = owningSchema.propertyByRole( property.getOwningRole() );
            collection = (Collection<IdentifiableObject>) owningProperty.getGetterMethod().invoke( inverseObject );

            IdentifiableObject o = owningObject;
            owningObject = inverseObject;
            inverseObject = o;
        }

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), owningObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        // if it already contains this object, don't add it. It might be a list and not set, and we don't want duplicates.
        if ( collection.contains( inverseObject ) )
        {
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
            return; // nothing to do, just return with OK
        }

        collection.add( inverseObject );

        manager.update( owningObject );
        manager.refresh( inverseObject );
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void deleteCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        IdentifiableObjects identifiableObjects = renderService.fromJson( request.getInputStream(), IdentifiableObjects.class );

        for ( IdentifiableObject identifiableObject : identifiableObjects.getIdentifiableObjects() )
        {
            deleteCollectionItem( pvUid, pvProperty, identifiableObject.getUid(), request, response );
        }
    }

    @RequestMapping( value = "/{uid}/{property}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_XML_VALUE )
    public void deleteCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        IdentifiableObjects identifiableObjects = renderService.fromXml( request.getInputStream(), IdentifiableObjects.class );

        for ( IdentifiableObject identifiableObject : identifiableObjects.getIdentifiableObjects() )
        {
            deleteCollectionItem( pvUid, pvProperty, identifiableObject.getUid(), request, response );
        }
    }

    @SuppressWarnings( "unchecked" )
    @RequestMapping( value = "/{uid}/{property}/{itemId}", method = RequestMethod.DELETE )
    public void deleteCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<T> objects = getEntity( pvUid );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        if ( !getSchema().haveProperty( pvProperty ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Property " + pvProperty + " does not exist on " + getEntityName() ) );
        }

        Property property = getSchema().getProperty( pvProperty );

        if ( !property.isCollection() || !property.isIdentifiableObject() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Only deletes within identifiable collection are allowed." ) );
        }

        Collection<IdentifiableObject> collection;
        IdentifiableObject inverseObject = manager.getNoAcl( (Class<? extends IdentifiableObject>) property.getItemKlass(), pvItemId );
        IdentifiableObject owningObject = objects.get( 0 );

        if ( property.isOwner() )
        {
            collection = (Collection<IdentifiableObject>) property.getGetterMethod().invoke( owningObject );
        }
        else
        {
            Schema owningSchema = getSchema( property.getItemKlass() );
            Property owningProperty = owningSchema.propertyByRole( property.getOwningRole() );
            collection = (Collection<IdentifiableObject>) owningProperty.getGetterMethod().invoke( inverseObject );

            IdentifiableObject o = owningObject;
            owningObject = inverseObject;
            inverseObject = o;
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), owningObject ) )
        {
            throw new DeleteAccessDeniedException( "You don't have the proper permissions to delete this object." );
        }

        if ( !collection.contains( inverseObject ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Collection " + pvProperty + " does not have an item with ID: " + pvItemId ) );
        }

        collection.remove( inverseObject );

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );

        manager.update( owningObject );
        manager.refresh( inverseObject );
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    protected T deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        return renderService.fromJson( request.getInputStream(), getEntityClass() );
    }

    protected T deserializeXmlEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        return renderService.fromXml( request.getInputStream(), getEntityClass() );
    }

    /**
     * Override to process entities after it has been retrieved from
     * storage and before it is returned to the view. Entities is null-safe.
     */
    protected void postProcessEntities( List<T> entityList, WebOptions options, Map<String, String> parameters, TranslateParams translateParams )
    {
    }

    /**
     * Override to process entities after it has been retrieved from
     * storage and before it is returned to the view. Entities is null-safe.
     */
    protected void postProcessEntities( List<T> entityList )
    {
    }

    /**
     * Override to process a single entity after it has been retrieved from
     * storage and before it is returned to the view. Entity is null-safe.
     */
    protected void postProcessEntity( T entity ) throws Exception
    {
    }

    /**
     * Override to process a single entity after it has been retrieved from
     * storage and before it is returned to the view. Entity is null-safe.
     */
    protected void postProcessEntity( T entity, WebOptions options, Map<String, String> parameters, TranslateParams translateParams ) throws Exception
    {
    }

    protected void preCreateEntity( T entity )
    {
    }

    protected void postCreateEntity( T entity )
    {
    }

    protected void preUpdateEntity( T entity, T newEntity )
    {
    }

    protected void postUpdateEntity( T entity )
    {
    }

    protected void preDeleteEntity( T entity )
    {
    }

    protected void postDeleteEntity()
    {
    }

    //--------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------

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
        return translateParams.isTranslate() ?
            translateParams.getLocaleWithDefault( (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ) ) : null;
    }

    @SuppressWarnings( "unchecked" )
    protected List<T> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders, TranslateParams translateParams ) throws QueryParserException
    {
        List<T> entityList;
        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, options.getRootJunction() );
        query.setDefaultOrder();

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

    private List<T> getEntity( String uid )
    {
        return getEntity( uid, NO_WEB_OPTIONS );
    }

    protected List<T> getEntity( String uid, WebOptions options )
    {
        ArrayList<T> list = new ArrayList<>();
        Optional<T> identifiableObject = Optional.fromNullable( manager.getNoAcl( getEntityClass(), uid ) );

        if ( identifiableObject.isPresent() )
        {
            list.add( identifiableObject.get() );
        }

        return list; //TODO consider ACL
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

    protected void addAccessProperties( List<T> objects, User user )
    {
        for ( T object : objects )
        {
            ((BaseIdentifiableObject) object).setAccess( aclService.getAccess( object, user ) );
        }
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

    protected void handleLinksAndAccess( List<T> entityList, List<String> fields, boolean deep, User user )
    {
        boolean generateLinks = hasHref( fields );
        boolean generateAccess = hasAccess( fields );

        if ( generateLinks )
        {
            linkService.generateLinks( entityList, deep );
        }

        if ( generateAccess && aclService.isSupported( getEntityClass() ) )
        {
            addAccessProperties( entityList, user );
        }
    }

    private InclusionStrategy.Include getInclusionStrategy( String inclusionStrategy )
    {
        if ( inclusionStrategy != null )
        {
            Optional<InclusionStrategy.Include> optional = Enums.getIfPresent( InclusionStrategy.Include.class, inclusionStrategy );

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
     * @param request  HttpServletRequest from current session
     * @param response HttpServletResponse from current session
     * @param object   Object to serialize
     */
    protected void serialize( HttpServletRequest request, HttpServletResponse response, Object object ) throws IOException
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
    protected T deserialize( HttpServletRequest request ) throws IOException
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

    //--------------------------------------------------------------------------
    // Reflection helpers
    //--------------------------------------------------------------------------

    private Class<T> entityClass;

    private String entityName;

    private String entitySimpleName;

    @SuppressWarnings( "unchecked" )
    protected Class<T> getEntityClass()
    {
        if ( entityClass == null )
        {
            Type[] actualTypeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
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
}
