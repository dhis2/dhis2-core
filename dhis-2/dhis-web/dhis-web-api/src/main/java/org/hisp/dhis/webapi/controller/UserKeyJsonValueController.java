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

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.userkeyjsonvalue.UserKeyJsonValue;
import org.hisp.dhis.userkeyjsonvalue.UserKeyJsonValueService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( "/userDataStore" )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class UserKeyJsonValueController
{
    @Autowired
    private UserKeyJsonValueService userKeyJsonValueService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private WebMessageService messageService;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * Returns a JSON array of strings representing the different namespaces used.
     * If no namespaces exist, an empty array is returned.
     */
    @RequestMapping( value = "", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<String> getNamespaces( HttpServletResponse response )
        throws IOException
    {
        return userKeyJsonValueService.getNamespacesByUser( currentUserService.getCurrentUser() );
    }

    /**
     * Returns a JSON array of strings representing the different keys used in a given namespace.
     * If no namespaces exist, an empty array is returned.
     */
    @RequestMapping( value = "/{namespace}", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<String> getKeys( @PathVariable String namespace, HttpServletResponse response )
        throws IOException
    {
        return userKeyJsonValueService.getKeysByUserAndNamespace( currentUserService.getCurrentUser(), namespace );
    }

    /**
     * Deletes all keys with the given user and namespace.
     */
    @RequestMapping( value = "/{namespace}", method = RequestMethod.DELETE )
    public void deleteKeys(
        @PathVariable String namespace,
        HttpServletResponse response )
        throws WebMessageException
    {
        userKeyJsonValueService.deleteNamespaceFromUser( currentUserService.getCurrentUser(), namespace );

        messageService.sendJson( WebMessageUtils.ok( "All keys from namespace '" + namespace + "' deleted." ), response );
    }

    /**
     * Retrieves the value of the KeyJsonValue represented by the given key and namespace from
     * the current user.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody String getUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key )
        throws IOException, WebMessageException
    {
        UserKeyJsonValue userKeyJsonValue = userKeyJsonValueService.getUserKeyJsonValue(
            currentUserService.getCurrentUser(), namespace, key );

        if ( userKeyJsonValue == null )
        {
            throw new WebMessageException( WebMessageUtils
                .notFound( "The key '" + key + "' was not found in the namespace '" + namespace + "'." ) );
        }

        return userKeyJsonValue.getValue();
    }

    /**
     * Creates a new KeyJsonValue Object on the current user with the key, namespace and value supplied.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.POST, produces = "application/json", consumes = "application/json" )
    public void addUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key,
        @RequestBody String body,
        @RequestParam( defaultValue = "false" ) boolean encrypt,
        HttpServletResponse response )
        throws IOException, WebMessageException
    {
        if ( userKeyJsonValueService.getUserKeyJsonValue( currentUserService.getCurrentUser(), namespace, key ) !=
            null )
        {
            throw new WebMessageException( WebMessageUtils
                .conflict( "The key '" + key + "' already exists in the namespace '" + namespace + "'." ) );
        }

        if ( !renderService.isValidJson( body ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "The data is not valid JSON." ) );
        }

        UserKeyJsonValue userKeyJsonValue = new UserKeyJsonValue();

        userKeyJsonValue.setKey( key );
        userKeyJsonValue.setUser( currentUserService.getCurrentUser() );
        userKeyJsonValue.setNamespace( namespace );
        userKeyJsonValue.setValue( body );
        userKeyJsonValue.setEncrypted( encrypt );

        userKeyJsonValueService.addUserKeyJsonValue( userKeyJsonValue );

        response.setStatus( HttpServletResponse.SC_CREATED );
        messageService.sendJson( WebMessageUtils.created( "Key '" + key + "' in namespace '" + namespace + "' created." ), response );
    }

    /**
     * Update a key.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.PUT, produces = "application/json", consumes = "application/json" )
    public void updateUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key,
        @RequestBody String body,
        HttpServletResponse response )
        throws WebMessageException, IOException
    {
        UserKeyJsonValue userKeyJsonValue = userKeyJsonValueService.getUserKeyJsonValue(
            currentUserService.getCurrentUser(), namespace, key );

        if ( userKeyJsonValue == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "The key '" + key + "' was not found in the namespace '" + namespace + "'." ) );
        }

        if ( !renderService.isValidJson( body ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "The data is not valid JSON." ) );
        }

        userKeyJsonValue.setValue( body );

        userKeyJsonValueService.updateUserKeyJsonValue( userKeyJsonValue );

        response.setStatus( HttpServletResponse.SC_OK );
        messageService.sendJson( WebMessageUtils.created( "Key '" + key + "' in namespace '" + namespace + "' updated." ), response );
    }

    /**
     * Delete a key.
     */
    @RequestMapping( value = "/{namespace}/{key}", method = RequestMethod.DELETE, produces = "application/json" )
    public void deleteUserKeyJsonValue(
        @PathVariable String namespace,
        @PathVariable String key,
        HttpServletResponse response )
        throws WebMessageException
    {
        UserKeyJsonValue userKeyJsonValue = userKeyJsonValueService.getUserKeyJsonValue(
            currentUserService.getCurrentUser(), namespace, key );

        if ( userKeyJsonValue == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "The key '" + key + "' was not found in the namespace '" + namespace + "'." ) );
        }

        userKeyJsonValueService.deleteUserKeyJsonValue( userKeyJsonValue );

        messageService.sendJson( WebMessageUtils.ok( "Key '" + key + "' deleted from the namespace '" + namespace + "'." ), response );
    }
}
