package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( "/userSettings" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class UserSettingController
{
    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private WebMessageService webMessageService;

    private static final Set<String> USER_SETTING_NAMES = Sets.newHashSet(
        UserSettingKey.values() ).stream().map( UserSettingKey::getName ).collect( Collectors.toSet() );

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{key}", method = RequestMethod.POST )
    public void setUserSetting(
        @PathVariable( value = "key" ) String key,
        @RequestParam( value = "user", required = false ) String username,
        @RequestParam( value = "value", required = false ) String value,
        @RequestBody( required = false ) String valuePayload,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        if ( key == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key must be specified" ) );
        }

        if ( value == null && valuePayload == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Value must be specified as query param or as payload" ) );
        }

        value = ObjectUtils.firstNonNull( value, valuePayload );

        Optional<UserSettingKey> keyEnum = UserSettingKey.getByName( key );

        if ( !keyEnum.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key is not supported: " + key ) );
        }

        Serializable valueObject = UserSettingKey.getAsRealClass( key, value );

        if ( username == null )
        {
            userSettingService.saveUserSetting( keyEnum.get(), valueObject );
        }
        else
        {
            userSettingService.saveUserSetting( keyEnum.get(), valueObject, username );
        }

        webMessageService.send( WebMessageUtils.ok( "User setting saved" ), response, request );
    }

    @RequestMapping( value = "/{key}", method = RequestMethod.GET )
    public @ResponseBody String getUserSetting(
        @PathVariable( "key" ) String key,
        @RequestParam( value = "user", required = false ) String username,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException, WebMessageException
    {
        Optional<UserSettingKey> keyEnum = UserSettingKey.getByName( key );

        if ( !keyEnum.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key is not supported: " + key ) );
        }

        User user = null;

        if ( username != null )
        {
            UserCredentials credentials = userService.getUserCredentialsByUsername( username );

            if ( credentials != null )
            {
                user = credentials.getUserInfo();
            }
            else
            {
                throw new WebMessageException( WebMessageUtils.conflict( "User does not exist: " + username ) );
            }
        }

        Serializable value = userSettingService.getUserSetting( keyEnum.get(), user );

        if ( value == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "User setting not found for key: " + key ) );
        }

        return String.valueOf( value );
    }

    @RequestMapping( method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody Map<String, Serializable> getUserSettingsByUser( @RequestParam( required = false ) String user,
        @RequestParam( required = false, defaultValue = "true" ) boolean useFallback,
        HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException, IOException
    {
        UserCredentials credentials = userService.getUserCredentialsByUsername( user );

        User us = credentials != null ? credentials.getUser() : null;

        if ( us == null )
        {
            us = currentUserService.getCurrentUser();
        }

        return userSettingService.getUserSettingsWithFallbackByUserAsMap( us, USER_SETTING_NAMES, useFallback );
    }

    @RequestMapping( value = "/{key}", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeSystemSetting( @PathVariable( "key" ) String key )
        throws WebMessageException
    {
        Optional<UserSettingKey> keyEnum = UserSettingKey.getByName( key );

        if ( !keyEnum.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key is not supported: " + key ) );
        }

        userSettingService.deleteUserSetting( keyEnum.get() );
    }
}
