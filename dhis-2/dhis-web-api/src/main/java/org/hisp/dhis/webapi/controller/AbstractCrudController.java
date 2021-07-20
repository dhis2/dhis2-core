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

import static java.util.Collections.singletonList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.validateAndThrowErrors;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.SubscribableObject;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
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
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsonpatch.JsonPatchManager;
import org.hisp.dhis.patch.Patch;
import org.hisp.dhis.patch.PatchParams;
import org.hisp.dhis.patch.PatchService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.sharing.SharingService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public abstract class AbstractCrudController<T extends IdentifiableObject> extends AbstractFullReadOnlyController<T>
{

    // --------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------

    @Autowired
    protected SchemaValidator schemaValidator;

    @Autowired
    protected RenderService renderService;

    @Autowired
    protected MetadataImportService importService;

    @Autowired
    protected MetadataExportService exportService;

    @Autowired
    protected WebMessageService webMessageService;

    @Autowired
    protected HibernateCacheManager hibernateCacheManager;

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected MergeService mergeService;

    @Autowired
    protected JsonPatchManager jsonPatchManager;

    @Autowired
    protected PatchService patchService;

    @Autowired
    @Qualifier( "xmlMapper" )
    protected ObjectMapper xmlMapper;

    @Autowired
    protected UserService userService;

    @Autowired
    protected SharingService sharingService;

    @PutMapping( value = "/{uid}/translations" )
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

        if ( typeReport.hasErrorReports() )
        {
            WebMessage webMessage = WebMessageUtils.typeReport( typeReport );
            webMessageService.send( webMessage, response, request );
            return;
        }

        validateAndThrowErrors( () -> schemaValidator.validate( persistedObject ) );
        manager.updateTranslations( persistedObject, object.getTranslations() );

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }

    // --------------------------------------------------------------------------
    // OLD PATCH
    // --------------------------------------------------------------------------

    @PatchMapping( value = "/{uid}" )
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

    @RequestMapping( value = "/{uid}/{property}", method = { RequestMethod.PUT, RequestMethod.PATCH } )
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

    // --------------------------------------------------------------------------
    // PATCH
    // --------------------------------------------------------------------------

    /**
     * Adds support for HTTP Patch using JSON Patch (RFC 6902), updated object
     * is run through normal metadata importer and internally looks like a
     * normal PUT (after the JSON Patch has been applied).
     *
     * For now we only support the official mimetype
     * "application/json-patch+json" but in future releases we might also want
     * to support "application/json" after the old patch behavior has been
     * removed.
     */
    @ResponseBody
    @PatchMapping( path = "/{uid}", consumes = { "application/json-patch+json" } )
    public void partialUpdateObject(
        @PathVariable( "uid" ) String pvUid,
        @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( rpParameters );
        List<T> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( getEntityClass(), pvUid ) );
        }

        final T persistedObject = entities.get( 0 );

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( user, persistedObject ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        prePatchEntity( persistedObject );

        final JsonPatch patch = jsonMapper.readValue( request.getInputStream(), JsonPatch.class );
        final T patchedObject = (T) jsonPatchManager.apply( patch, persistedObject );

        // we don't allow changing UIDs
        ((BaseIdentifiableObject) patchedObject).setUid( persistedObject.getUid() );

        Map<String, List<String>> parameterValuesMap = contextService.getParameterValuesMap();

        if ( !parameterValuesMap.containsKey( "importReportMode" ) )
        {
            parameterValuesMap.put( "importReportMode", Collections.singletonList( "ERRORS_NOT_OWNER" ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( parameterValuesMap );

        params.setUser( user )
            .setImportReportMode( ImportReportMode.ERRORS_NOT_OWNER )
            .setImportStrategy( ImportStrategy.UPDATE )
            .addObject( patchedObject );

        ImportReport importReport = importService.importMetadata( params );
        WebMessage webMessage = WebMessageUtils.objectReport( importReport );

        if ( importReport.getStatus() == Status.OK )
        {
            T entity = manager.get( getEntityClass(), pvUid );
            postPatchEntity( entity );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        webMessageService.send( webMessage, response, request );
    }

    // --------------------------------------------------------------------------
    // POST
    // --------------------------------------------------------------------------

    @PostMapping( consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postObject( request, response, deserializeJsonEntity( request, response ) );
    }

    @PostMapping( consumes = { "application/xml", "text/xml" } )
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
        return importReport.getFirstObjectReport();
    }

    @PostMapping( value = "/{uid}/favorite" )
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

    @PostMapping( value = "/{uid}/subscriber" )
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

    @PutMapping( value = "/{uid}", consumes = MediaType.APPLICATION_JSON_VALUE )
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

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );

        params.setUser( user )
            .setImportStrategy( ImportStrategy.UPDATE )
            .addObject( parsed );

        // default to FULL unless ERRORS_NOT_OWNER has been requested
        if ( ImportReportMode.ERRORS_NOT_OWNER != params.getImportReportMode() )
        {
            params.setImportReportMode( ImportReportMode.FULL );
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

    @PutMapping( value = "/{uid}", consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE } )
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

    @DeleteMapping( value = "/{uid}" )
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

        postDeleteEntity( pvUid );

        webMessageService.send( WebMessageUtils.objectReport( importReport ), response, request );
    }

    @DeleteMapping( value = "/{uid}/favorite" )
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

    @DeleteMapping( value = "/{uid}/subscriber" )
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

    @PostMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_JSON_VALUE )
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

    @PostMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_XML_VALUE )
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

    @PutMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_JSON_VALUE )
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

    @PutMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_XML_VALUE )
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

    @PostMapping( value = "/{uid}/{property}/{itemId}" )
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

    @DeleteMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_JSON_VALUE )
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

    @DeleteMapping( value = "/{uid}/{property}", consumes = MediaType.APPLICATION_XML_VALUE )
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

    @DeleteMapping( value = "/{uid}/{property}/{itemId}" )
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

        if ( typeReport.hasErrorReports() )
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

    protected void postDeleteEntity( String entityUid )
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

}
