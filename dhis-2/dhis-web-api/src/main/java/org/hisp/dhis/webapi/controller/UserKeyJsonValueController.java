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

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.userkeyjsonvalue.UserKeyJsonValue;
import org.hisp.dhis.userkeyjsonvalue.UserKeyJsonValueService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( "/userDataStore" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class UserKeyJsonValueController
{
    @Autowired
    private UserKeyJsonValueService userKeyJsonValueService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * Returns a JSON array of strings representing the different namespaces
     * used. If no namespaces exist, an empty array is returned.
     */
    @GetMapping( value = "", produces = "application/json" )
    public @ResponseBody List<String> getNamespaces( HttpServletResponse response )
    {
        setNoStore( response );

        return userKeyJsonValueService.getNamespacesByUser( currentUserService.getCurrentUser() );
    }

    /**
     * Returns a JSON array of strings representing the different keys used in a
     * given namespace. If no namespaces exist, an empty array is returned.
     */
    @GetMapping( value = "/{namespace}", produces = "application/json" )
    public @ResponseBody List<String> getKeys( @PathVariable String namespace, HttpServletResponse response )
        throws WebMessageException
    {
        if ( !userKeyJsonValueService.getNamespacesByUser( currentUserService.getCurrentUser() ).contains( namespace ) )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "The namespace '" + namespace + "' was not found." ) );
        }

        setNoStore( response );

        return userKeyJsonValueService.getKeysByUserAndNamespace( currentUserService.getCurrentUser(), namespace );
    }

    /**
     * Deletes all keys with the given user and namespace.
     */
    @DeleteMapping( "/{namespace}" )
    @ResponseBody
    public WebMessage deleteKeys( @PathVariable String namespace )
    {
        userKeyJsonValueService.deleteNamespaceFromUser( currentUserService.getCurrentUser(), namespace );

        return WebMessageUtils.ok( "All keys from namespace '" + namespace + "' deleted." );
    }

    /**
     * Retrieves the value of the KeyJsonValue represented by the given key and
     * namespace from the current user.
     */
    @GetMapping( value = "/{namespace}/{key}", produces = "application/json" )
    public @ResponseBody String getUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key, HttpServletResponse response )
        throws WebMessageException
    {
        UserKeyJsonValue userKeyJsonValue = userKeyJsonValueService.getUserKeyJsonValue(
            currentUserService.getCurrentUser(), namespace, key );

        if ( userKeyJsonValue == null )
        {
            throw new WebMessageException( WebMessageUtils
                .notFound( "The key '" + key + "' was not found in the namespace '" + namespace + "'." ) );
        }

        setNoStore( response );

        return userKeyJsonValue.getValue();
    }

    /**
     * Creates a new KeyJsonValue Object on the current user with the key,
     * namespace and value supplied.
     */
    @PostMapping( value = "/{namespace}/{key}", produces = "application/json", consumes = "application/json" )
    @ResponseBody
    public WebMessage addUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key,
        @RequestBody String body,
        @RequestParam( defaultValue = "false" ) boolean encrypt )
        throws IOException
    {
        if ( userKeyJsonValueService.getUserKeyJsonValue( currentUserService.getCurrentUser(), namespace,
            key ) != null )
        {
            return WebMessageUtils
                .conflict( "The key '" + key + "' already exists in the namespace '" + namespace + "'." );
        }

        if ( !renderService.isValidJson( body ) )
        {
            return WebMessageUtils.badRequest( "The data is not valid JSON." );
        }

        UserKeyJsonValue userKeyJsonValue = new UserKeyJsonValue();

        userKeyJsonValue.setKey( key );
        userKeyJsonValue.setCreatedBy( currentUserService.getCurrentUser() );
        userKeyJsonValue.setNamespace( namespace );
        userKeyJsonValue.setValue( body );
        userKeyJsonValue.setEncrypted( encrypt );

        userKeyJsonValueService.addUserKeyJsonValue( userKeyJsonValue );

        return WebMessageUtils.created( "Key '" + key + "' in namespace '" + namespace + "' created." );
    }

    /**
     * Update a key.
     */
    @PutMapping( value = "/{namespace}/{key}", produces = "application/json", consumes = "application/json" )
    @ResponseBody
    public WebMessage updateUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key,
        @RequestBody String body )
        throws IOException
    {
        UserKeyJsonValue userKeyJsonValue = userKeyJsonValueService.getUserKeyJsonValue(
            currentUserService.getCurrentUser(), namespace, key );

        if ( userKeyJsonValue == null )
        {
            return WebMessageUtils
                .notFound( "The key '" + key + "' was not found in the namespace '" + namespace + "'." );
        }

        if ( !renderService.isValidJson( body ) )
        {
            return WebMessageUtils.badRequest( "The data is not valid JSON." );
        }

        userKeyJsonValue.setValue( body );

        userKeyJsonValueService.updateUserKeyJsonValue( userKeyJsonValue );

        return WebMessageUtils.ok( "Key '" + key + "' in namespace '" + namespace + "' updated." );
    }

    /**
     * Delete a key.
     */
    @DeleteMapping( value = "/{namespace}/{key}", produces = "application/json" )
    @ResponseBody
    public WebMessage deleteUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key )
    {
        UserKeyJsonValue userKeyJsonValue = userKeyJsonValueService.getUserKeyJsonValue(
            currentUserService.getCurrentUser(), namespace, key );

        if ( userKeyJsonValue == null )
        {
            return WebMessageUtils
                .notFound( "The key '" + key + "' was not found in the namespace '" + namespace + "'." );
        }

        userKeyJsonValueService.deleteUserKeyJsonValue( userKeyJsonValue );

        return WebMessageUtils.ok( "Key '" + key + "' deleted from the namespace '" + namespace + "'." );
    }
}
