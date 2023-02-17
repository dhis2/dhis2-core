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
import static org.hisp.dhis.datastore.DatastoreQuery.parseFields;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.apache.commons.beanutils.BeanUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreParams;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.DatastoreQuery.Field;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.JsonWriter;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Stian Sandvold
 */
@OpenApi.Tags( "data" )
@Controller
@RequestMapping( "/dataStore" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class DatastoreController
{
    private final DatastoreService service;

    private final AclService aclService;

    private final ObjectMapper jsonMapper;

    /**
     * Returns a JSON array of strings representing the different namespaces
     * used. If no namespaces exist, an empty array is returned.
     */
    @GetMapping( value = "", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody List<String> getNamespaces( HttpServletResponse response )
    {
        setNoStore( response );

        return service.getNamespaces();
    }

    /**
     * The path {@code /{namespace}} is clashing with
     * {@link #getEntries(String, String, boolean, DatastoreParams, HttpServletResponse)}
     * therefore a collision free alternative was added
     * {@code /{namespace}/keys}.
     */
    @OpenApi.Response( status = Status.NOT_FOUND, value = WebMessage.class )
    @GetMapping( value = { "/{namespace}/keys" }, produces = APPLICATION_JSON_VALUE )
    public @ResponseBody List<String> getKeysInNamespace( @RequestParam( required = false ) Date lastUpdated,
        @PathVariable String namespace,
        HttpServletResponse response )
        throws NotFoundException
    {
        return getKeysInNamespaceLegacy( lastUpdated, namespace, response );
    }

    /**
     * Returns a list of strings representing keys in the given namespace.
     */
    @OpenApi.Ignore
    @GetMapping( value = "/{namespace}", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody List<String> getKeysInNamespaceLegacy( @RequestParam( required = false ) Date lastUpdated,
        @PathVariable String namespace,
        HttpServletResponse response )
        throws NotFoundException
    {
        setNoStore( response );

        List<String> keys = service.getKeysInNamespace( namespace, lastUpdated );

        if ( keys.isEmpty() && !service.isUsedNamespace( namespace ) )
        {
            throw new NotFoundException( String.format( "Namespace not found: '%s'", namespace ) );
        }

        return keys;
    }

    @Value
    private static class Pager
    {
        int page;

        int pageSize;
    }

    @Value
    private static class EntriesResponse
    {
        Pager pager;

        List<Map<String, JsonNode>> entries;
    }

    @OpenApi.Response( status = Status.OK, value = EntriesResponse.class )
    @OpenApi.Response( status = Status.BAD_REQUEST, value = WebMessage.class )
    @GetMapping( value = "/{namespace}", params = "fields", produces = APPLICATION_JSON_VALUE )
    public void getEntries( @PathVariable String namespace,
        @RequestParam( required = true ) String fields,
        @RequestParam( required = false, defaultValue = "false" ) boolean includeAll,
        DatastoreParams params, HttpServletResponse response )
        throws IOException
    {
        DatastoreQuery query = service.plan( DatastoreQuery.builder()
            .namespace( namespace )
            .fields( parseFields( fields ) )
            .includeAll( includeAll )
            .build().with( params ) );

        response.setContentType( APPLICATION_JSON_VALUE );
        setNoStore( response );

        try ( PrintWriter writer = response.getWriter();
            JsonWriter out = new JsonWriter( writer ) )
        {
            try
            {
                List<String> members = query.getFields().stream().map( Field::getAlias ).collect( toList() );
                service.getFields( query, entries -> {
                    if ( !query.isHeadless() )
                    {
                        writer.write( "{\"pager\":{" );
                        writer.write( "\"page\":" + query.getPage() + "," );
                        writer.write( "\"pageSize\":" + query.getPageSize() );
                        writer.write( "},\"entries\":" );
                    }
                    out.writeEntries( members, entries );
                    return true;
                } );
                if ( !query.isHeadless() )
                {
                    writer.write( "}" );
                }
            }
            catch ( RuntimeException ex )
            {
                response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
                Throwable cause = ex.getCause();
                String msg = "Unknown error when running the query: "
                    + (cause != null && ex.getMessage().contains( "could not extract ResultSet" )
                        ? cause.getMessage()
                        : ex.getMessage());
                if ( cause != null && cause.getMessage().contains( "cannot cast type " )
                    && cause.getMessage().contains( " to double precision" ) )
                {
                    String sortProperty = query.getOrder().getPath();
                    msg = "Cannot use numeric sort order on property `" + sortProperty
                        + "` as the property contains non-numeric values for matching entries. Use " + query.getOrder()
                            .getDirection().name().substring( 1 )
                        + " instead or apply a filter that only matches entries with numeric values for " + sortProperty
                        + ".";
                }
                jsonMapper.writeValue( writer, WebMessageUtils.badRequest( msg ) );
            }
        }
    }

    /**
     * Deletes all keys with the given namespace.
     */
    @OpenApi.Response( status = Status.NOT_FOUND, value = WebMessage.class )
    @ResponseBody
    @DeleteMapping( "/{namespace}" )
    public WebMessage deleteNamespace( @PathVariable String namespace )
        throws NotFoundException
    {
        if ( !service.isUsedNamespace( namespace ) )
        {
            throw new NotFoundException( String.format( "Namespace not found: '%s'", namespace ) );
        }

        service.deleteNamespace( namespace );

        return ok( String.format( "Namespace deleted: '%s'", namespace ) );
    }

    /**
     * Retrieves the value of the KeyJsonValue represented by the given key from
     * the given namespace.
     */
    @OpenApi.Response( status = Status.NOT_FOUND, value = WebMessage.class )
    @GetMapping( value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody String getKeyJsonValue( @PathVariable String namespace, @PathVariable String key )
        throws NotFoundException
    {
        return getExistingEntry( namespace, key ).getValue();
    }

    /**
     * Retrieves the KeyJsonValue represented by the given key from the given
     * namespace.
     */
    @OpenApi.Response( status = Status.NOT_FOUND, value = WebMessage.class )
    @GetMapping( value = "/{namespace}/{key}/metaData", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody DatastoreEntry getKeyJsonValueMetaData( @PathVariable String namespace,
        @PathVariable String key,
        @CurrentUser User currentUser )
        throws NotFoundException,
        InvocationTargetException,
        IllegalAccessException
    {
        DatastoreEntry entry = getExistingEntry( namespace, key );

        DatastoreEntry metaData = new DatastoreEntry();
        BeanUtils.copyProperties( metaData, entry );
        metaData.setValue( null );
        metaData.setJbPlainValue( null );
        metaData.setEncryptedValue( null );
        metaData.setAccess( aclService.getAccess( entry, currentUser ) );
        return metaData;
    }

    /**
     * Creates a new KeyJsonValue Object on the given namespace with the key and
     * value supplied.
     */
    @ResponseBody
    @PostMapping( value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.CREATED )
    public WebMessage addKeyJsonValue( @PathVariable String namespace, @PathVariable String key,
        @RequestBody String value,
        @RequestParam( defaultValue = "false" ) boolean encrypt, HttpServletRequest request )
    {
        DatastoreEntry entry = new DatastoreEntry();
        entry.setKey( key );
        entry.setNamespace( namespace );
        entry.setValue( value );
        entry.setEncrypted( encrypt );

        service.addEntry( entry );

        return created( String.format( "Key created: '%s'", key ) );
    }

    /**
     * Update a key in the given namespace.
     */
    @OpenApi.Response( status = Status.NOT_FOUND, value = WebMessage.class )
    @ResponseBody
    @PutMapping( value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE )
    public WebMessage updateKeyJsonValue( @PathVariable String namespace, @PathVariable String key,
        @RequestBody String value )
        throws Exception
    {
        DatastoreEntry entry = getExistingEntry( namespace, key );
        entry.setValue( value );

        service.updateEntry( entry );

        return ok( String.format( "Key updated: '%s'", key ) );
    }

    /**
     * Delete a key from the given namespace.
     */
    @OpenApi.Response( status = Status.NOT_FOUND, value = WebMessage.class )
    @ResponseBody
    @DeleteMapping( value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE )
    public WebMessage deleteKeyJsonValue( @PathVariable String namespace, @PathVariable String key )
        throws Exception
    {
        DatastoreEntry entry = getExistingEntry( namespace, key );
        service.deleteEntry( entry );

        return ok( String.format( "Key '%s' deleted from namespace '%s'", key, namespace ) );
    }

    private DatastoreEntry getExistingEntry( String namespace, String key )
        throws NotFoundException
    {
        DatastoreEntry entry = service.getEntry( namespace, key );

        if ( entry == null )
        {
            throw new NotFoundException( String.format( "Key '%s' not found in namespace '%s'", key, namespace ) );
        }

        return entry;
    }
}
