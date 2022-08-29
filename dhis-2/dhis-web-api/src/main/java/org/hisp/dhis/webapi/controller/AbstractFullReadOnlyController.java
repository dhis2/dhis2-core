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
package org.hisp.dhis.webapi.controller;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.springframework.http.CacheControl.noCache;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.commons.jackson.config.WriteDateStdSerializer;
import org.hisp.dhis.commons.jackson.config.WriteInstantStdSerializer;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.PaginationUtils;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvWriteException;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Base controller for APIs that only want to offer read only access through
 * both Gist API and full API.
 *
 * @author Jan Bernitt
 */
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public abstract class AbstractFullReadOnlyController<T extends IdentifiableObject>
    extends AbstractGistReadOnlyController<T>
{
    protected static final String DEFAULTS = "INCLUDE";

    protected static final WebOptions NO_WEB_OPTIONS = new WebOptions( new HashMap<>() );

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected UserSettingService userSettingService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected QueryService queryService;

    @Autowired
    protected FieldFilterService oldFieldFilterService;

    @Autowired
    protected org.hisp.dhis.fieldfiltering.FieldFilterService fieldFilterService;

    @Autowired
    protected LinkService linkService;

    @Autowired
    protected AclService aclService;

    @Autowired
    protected AttributeService attributeService;

    // --------------------------------------------------------------------------
    // Custom CSV mapper for field filtering
    // --------------------------------------------------------------------------

    protected static final CsvMapper CSV_MAPPER = new CsvMapper();

    static
    {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure( JsonGenerator.Feature.IGNORE_UNKNOWN, true );
        csvMapper.registerModule( new SimpleModule()
            .addSerializer( Date.class, new WriteDateStdSerializer() )
            .addSerializer( Instant.class, new WriteInstantStdSerializer() ) );
        csvMapper.registerModule( new Jdk8Module() );
    }

    // --------------------------------------------------------------------------
    // Hooks
    // --------------------------------------------------------------------------

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

    /**
     * Allows to append new filters to the incoming ones. Recommended only on
     * very specific cases where forcing a new filter, programmatically, make
     * sense.
     */
    protected void forceFiltering( final WebOptions webOptions, final List<String> filters )
    {
    }

    // --------------------------------------------------------------------------
    // GET Full
    // --------------------------------------------------------------------------

    @GetMapping
    public @ResponseBody ResponseEntity<StreamingJsonRoot<T>> getObjectList(
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, @CurrentUser User currentUser )
        throws QueryParserException
    {
        List<Order> orders = orderParams.getOrders( getSchema() );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );

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

        forceFiltering( options, filters );

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
                totalCount = countTotal( options, filters, orders );
            }

            pager = new Pager( options.getPage(), totalCount, options.getPageSize() );
        }

        postProcessResponseEntities( entities, options, rpParameters );

        handleLinksAndAccess( entities, fields, false );
        linkService.generatePagerLinks( pager, getEntityClass() );

        cachePrivate( response );

        return ResponseEntity.ok( new StreamingJsonRoot<>( pager, getSchema().getCollectionName(),
            FieldFilterParams.of( entities, fields ) ) );
    }

    @GetMapping( produces = { "text/csv", "application/text" } )
    public ResponseEntity<String> getObjectListCsv(
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        @CurrentUser User currentUser,
        @RequestParam( defaultValue = "," ) char separator,
        @RequestParam( defaultValue = ";" ) String arraySeparator,
        @RequestParam( defaultValue = "false" ) boolean skipHeader,
        HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        List<Order> orders = orderParams.getOrders( getSchema() );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );

        WebOptions options = new WebOptions( rpParameters );
        WebMetadata metadata = new WebMetadata();

        if ( fields.isEmpty() || fields.contains( "*" ) || fields.contains( ":all" ) )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        // only support metadata
        if ( !getSchema().isMetadata() )
        {
            throw new HttpClientErrorException( HttpStatus.NOT_FOUND );
        }

        if ( !aclService.canRead( currentUser, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException(
                "You don't have the proper permissions to read objects of this type." );
        }

        List<T> entities = getEntityList( metadata, options, filters, orders );

        CsvSchema schema;
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        Map<String, Function<T, Object>> obj2valueByProperty = new LinkedHashMap<>();

        for ( String field : fields )
        {
            // We just split on ',' here, we do not try and deep dive into
            // objects using [], if the client provides id,name,group[id]
            // then the group[id] part is simply ignored.
            for ( String fieldName : field.split( "," ) )
            {
                Property property = getSchema().getProperty( fieldName );

                if ( property == null )
                {
                    if ( CodeGenerator.isValidUid( fieldName ) )
                    {
                        schemaBuilder.addColumn( fieldName );
                        obj2valueByProperty.put( fieldName, obj -> getAttributeValue( obj, fieldName ) );
                    }
                    continue;
                }

                if ( (property.isCollection() && property.itemIs( PropertyType.REFERENCE )) )
                {
                    schemaBuilder.addArrayColumn( property.getCollectionName() );
                    obj2valueByProperty.put( property.getCollectionName(), obj -> getCollectionValue( obj, property ) );
                }
                else
                {
                    schemaBuilder.addColumn( property.getName() );
                    obj2valueByProperty.put( property.getName(),
                        obj -> ReflectionUtils.invokeMethod( obj, property.getGetterMethod() ) );
                }
            }
        }

        schema = schemaBuilder.build()
            .withColumnSeparator( separator )
            .withArrayElementSeparator( arraySeparator );

        if ( !skipHeader )
        {
            schema = schema.withHeader();
        }

        try ( StringWriter strW = new StringWriter() )
        {
            SequenceWriter seqW = CSV_MAPPER.writer( schema )
                .writeValues( strW );

            Object[] row = new Object[obj2valueByProperty.size()];

            for ( T e : entities )
            {
                int i = 0;

                for ( Function<T, Object> toValue : obj2valueByProperty.values() )
                {
                    Object o = toValue.apply( e );

                    if ( o instanceof Collection )
                    {
                        row[i++] = ((Collection<?>) o).stream()
                            .map( String::valueOf )
                            .collect( Collectors.joining( arraySeparator ) );
                    }
                    else
                    {
                        row[i++] = o;
                    }
                }

                seqW.write( row );
            }

            seqW.close();

            return ResponseEntity.ok( strW.toString() );
        }
        catch ( CsvWriteException ex )
        {
            response.setContentType( MediaType.APPLICATION_JSON_VALUE );

            throw new WebMessageException( conflict(
                "Invalid property selected. Make sure all properties are either simple or collections of refs / simple.",
                ex.getMessage() ) );
        }
    }

    private static List<String> getCollectionValue( Object obj, Property property )
    {
        Object value = ReflectionUtils.invokeMethod( obj, property.getGetterMethod() );

        @SuppressWarnings( "unchecked" )
        Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) value;

        return collection.stream().map( PrimaryKeyObject::getUid ).collect( toList() );
    }

    private static Object getAttributeValue( Object obj, String attrId )
    {
        if ( obj instanceof BaseIdentifiableObject )
        {
            AttributeValue attr = ((BaseIdentifiableObject) obj).getAttributeValue( attrId );
            return attr == null ? null : attr.getValue();
        }
        return null;
    }

    @GetMapping( "/{uid}" )
    @SuppressWarnings( "unchecked" )
    public @ResponseBody ResponseEntity<?> getObject(
        @PathVariable( "uid" ) String pvUid,
        @RequestParam Map<String, String> rpParameters,
        @CurrentUser User currentUser,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        if ( !aclService.canRead( currentUser, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException(
                "You don't have the proper permissions to read objects of this type." );
        }

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        forceFiltering( new WebOptions( rpParameters ), filters );

        if ( fields.isEmpty() )
        {
            fields.add( "*" );
        }

        cachePrivate( response );

        WebOptions options = new WebOptions( rpParameters );
        List<T> entities = getEntity( pvUid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( notFound( getEntityClass(), pvUid ) );
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, new ArrayList<>(),
            getPaginationData( options ), options.getRootJunction() );
        query.setUser( currentUser );
        query.setObjects( entities );
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );

        entities = (List<T>) queryService.query( query );

        handleLinksAndAccess( entities, fields, true );
        handleAttributeValues( entities, fields );

        for ( T entity : entities )
        {
            postProcessResponseEntity( entity, options, rpParameters );
        }

        return ResponseEntity.ok( new StreamingJsonRoot<>( null, null,
            FieldFilterParams.of( entities, fields ) ) );
    }

    @GetMapping( "/{uid}/{property}" )
    public @ResponseBody ResponseEntity<ObjectNode> getObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        @CurrentUser User currentUser,
        HttpServletResponse response )
        throws Exception
    {
        if ( !"translations".equals( pvProperty ) )
        {
            setTranslationParams( translateParams );
        }

        if ( !aclService.canRead( currentUser, getEntityClass() ) )
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

        ObjectNode objectNode = getObjectInternal( pvUid, rpParameters, Lists.newArrayList(),
            Lists.newArrayList( pvProperty + fieldFilter ), currentUser );

        return ResponseEntity.ok( objectNode );
    }

    @SuppressWarnings( "unchecked" )
    private ObjectNode getObjectInternal( String uid, Map<String, String> parameters,
        List<String> filters, List<String> fields, User currentUser )
        throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        List<T> entities = getEntity( uid, options );

        if ( entities.isEmpty() )
        {
            throw new WebMessageException( notFound( getEntityClass(), uid ) );
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, new ArrayList<>(),
            getPaginationData( options ), options.getRootJunction() );
        query.setUser( currentUser );
        query.setObjects( entities );
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );

        entities = (List<T>) queryService.query( query );

        handleLinksAndAccess( entities, fields, true );
        handleAttributeValues( entities, fields );

        for ( T entity : entities )
        {
            postProcessResponseEntity( entity, options, parameters );
        }

        List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes( entities, fields );

        return objectNodes.isEmpty() ? fieldFilterService.createObjectNode() : objectNodes.get( 0 );
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

    private void cachePrivate( HttpServletResponse response )
    {
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL,
            noCache().cachePrivate().getHeaderValue() );
    }

    private boolean hasHref( List<String> fields )
    {
        return fieldsContains( "href", fields );
    }

    private void handleLinksAndAccess( List<T> entityList, List<String> fields, boolean deep )
    {
        if ( hasHref( fields ) )
        {
            linkService.generateLinks( entityList, deep );
        }
    }

    private void handleAttributeValues( List<T> entityList, List<String> fields )
    {
        List<String> hasAttributeValues = fields.stream().filter( field -> field.contains( "attributeValues" ) )
            .collect( toList() );

        if ( !hasAttributeValues.isEmpty() )
        {
            attributeService.generateAttributes( entityList );
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

    // --------------------------------------------------------------------------
    // Reflection helpers
    // --------------------------------------------------------------------------

    private String entityName;

    private String entitySimpleName;

    protected final String getEntityName()
    {
        if ( entityName == null )
        {
            entityName = getEntityClass().getName();
        }

        return entityName;
    }

    protected final String getEntitySimpleName()
    {
        if ( entitySimpleName == null )
        {
            entitySimpleName = getEntityClass().getSimpleName();
        }

        return entitySimpleName;
    }

    protected final List<T> getEntity( String uid )
    {
        return getEntity( uid, NO_WEB_OPTIONS );
    }

    protected List<T> getEntity( String uid, WebOptions options )
    {
        ArrayList<T> list = new ArrayList<>();
        getEntity( uid, getEntityClass() ).ifPresent( list::add );
        return list; // TODO consider ACL
    }

    protected final <E extends IdentifiableObject> java.util.Optional<E> getEntity( String uid, Class<E> entityType )
    {
        return java.util.Optional.ofNullable( manager.getNoAcl( entityType, uid ) );
    }

    protected final Schema getSchema( Class<?> klass )
    {
        return schemaService.getDynamicSchema( klass );
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------

    protected final void setTranslationParams( TranslateParams translateParams )
    {
        Locale dbLocale = getLocaleWithDefault( translateParams );
        CurrentUserUtil.setUserSetting( UserSettingKey.DB_LOCALE, dbLocale );
    }

    private Locale getLocaleWithDefault( TranslateParams translateParams )
    {
        return translateParams.isTranslate()
            ? translateParams.getLocaleWithDefault(
                (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ) )
            : null;
    }

    protected final Pagination getPaginationData( WebOptions options )
    {
        return PaginationUtils.getPaginationData( options );
    }
}
