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

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.keyjsonvalue.KeyJsonValue;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( "/dataStore" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class KeyJsonValueController
{
    @Autowired
    private KeyJsonValueService service;

    @Autowired
    private WebMessageService messageService;

    /**
     * Returns a JSON array of strings representing the different namespaces
     * used. If no namespaces exist, an empty array is returned.
     */
    @RequestMapping( value = "", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<String> getNamespaces( HttpServletResponse response )
    {
        setNoStore( response );

        return service.getNamespaces();
    }

    /**
     * Returns a list of strings representing keys in the given namespace.
     */
    @RequestMapping( value = "/{namespace}", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<String> getKeysInNamespace( @RequestParam( required = false ) Date lastUpdated,
        @PathVariable String namespace,
        HttpServletResponse response )
        throws Exception
    {
        setNoStore( response );

        List<String> keys = service.getKeysInNamespace( namespace, lastUpdated );
        if ( keys.isEmpty() )
        {
            throw new NotFoundException( ("The namespace '" + namespace + "' was not found.") );
        }
        return keys;
    }

    /**
     * Deletes all keys with the given namespace.
     */
    @RequestMapping( value = "/{namespace}", method = RequestMethod.DELETE )
    public void deleteNamespace( @PathVariable String namespace, HttpServletResponse response )
        throws Exception
    {
        if ( !service.isUsedNamespace( namespace ) )
        {
            throw new NotFoundException( ("The namespace '" + namespace + "' was not found.") );
        }

        service.deleteNamespace( namespace );

        messageService.sendJson( WebMessageUtils.ok( "Namespace '" + namespace + "' deleted." ), response );
    }

    /**
     * Retrieves the value of the KeyJsonValue represented by the given key from
     * the given namespace.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody String getKeyJsonValue( @PathVariable String namespace, @PathVariable String key,
        HttpServletResponse response )
        throws Exception
    {
        return getExistingEntry( namespace, key ).getValue();
    }

    /**
     * Retrieves the KeyJsonValue represented by the given key from the given
     * namespace.
     */
    @RequestMapping( value = "/{namespace}/{key}/metaData", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody KeyJsonValue getKeyJsonValueMetaData( @PathVariable String namespace, @PathVariable String key,
        HttpServletResponse response )
        throws Exception
    {
        KeyJsonValue entry = getExistingEntry( namespace, key );

        KeyJsonValue metaData = new KeyJsonValue();
        BeanUtils.copyProperties( metaData, entry );
        metaData.setValue( null );
        metaData.setJbPlainValue( null );
        metaData.setEncryptedValue( null );
        return metaData;
    }

    /**
     * Creates a new KeyJsonValue Object on the given namespace with the key and
     * value supplied.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.POST, produces = "application/json", consumes = "application/json" )
    public void addKeyJsonValue( @PathVariable String namespace, @PathVariable String key, @RequestBody String body,
        @RequestParam( defaultValue = "false" ) boolean encrypt, HttpServletResponse response )
        throws Exception
    {
        KeyJsonValue entry = new KeyJsonValue();
        entry.setKey( key );
        entry.setNamespace( namespace );
        entry.setValue( body );
        entry.setEncrypted( encrypt );

        service.addKeyJsonValue( entry );

        messageService.sendJson( WebMessageUtils.created( "Key '" + key + "' created." ), response );
    }

    /**
     * Update a key in the given namespace.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.PUT, produces = "application/json", consumes = "application/json" )
    public void updateKeyJsonValue( @PathVariable String namespace, @PathVariable String key, @RequestBody String body,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        KeyJsonValue entry = getExistingEntry( namespace, key );
        entry.setValue( body );

        service.updateKeyJsonValue( entry );

        messageService.sendJson( WebMessageUtils.ok( "Key '" + key + "' updated." ), response );
    }

    /**
     * Delete a key from the given namespace.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.DELETE, produces = "application/json" )
    public void deleteKeyJsonValue( @PathVariable String namespace, @PathVariable String key,
        HttpServletResponse response )
        throws Exception
    {
        KeyJsonValue entry = getExistingEntry( namespace, key );
        service.deleteKeyJsonValue( entry );

        messageService.sendJson( WebMessageUtils.ok( "Key '" + key + "' deleted from namespace '" + namespace + "'." ),
            response );
    }

    private KeyJsonValue getExistingEntry( String namespace, String key )
        throws NotFoundException
    {
        KeyJsonValue entry = service.getKeyJsonValue( namespace, key );
        if ( entry == null )
        {
            throw new NotFoundException( "The key '" + key + "' was not found in the namespace '" + namespace + "'." );
        }
        return entry;
    }
}
