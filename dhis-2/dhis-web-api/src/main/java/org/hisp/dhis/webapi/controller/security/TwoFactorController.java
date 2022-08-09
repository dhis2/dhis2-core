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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;
import static org.hisp.dhis.feedback.ErrorCode.E3023;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;

import org.apache.commons.validator.routines.LongValidator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.TwoFactoryAuthenticationUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Håkonsen
 * @author Morten Svanaes
 */
@RestController
@RequestMapping( value = "/2fa" )
@ApiVersion( { DhisApiVersion.DEFAULT,
    DhisApiVersion.ALL } )
@AllArgsConstructor
public class TwoFactorController
{
    private final UserService defaultUserService;

    private final SystemSettingManager systemSettingManager;

    @GetMapping( value = "/qr", produces = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.ACCEPTED )
    @ResponseBody
    public Map<String, Object> getQrCode( @CurrentUser User currentUser )
        throws WebMessageException
    {
        if ( currentUser == null )
        {
            throw new BadCredentialsException( "No current user" );
        }

        if ( currentUser.getTwoFA() )
        {
            throw new WebMessageException( conflict( ErrorCode.E3022.getMessage(), ErrorCode.E3022 ) );
        }

        defaultUserService.generateTwoFactorSecret( currentUser );

        String appName = systemSettingManager.getStringSetting( SettingKey.APPLICATION_TITLE );
        String url = TwoFactoryAuthenticationUtils.generateQrUrl( appName, currentUser );

        Map<String, Object> map = new HashMap<>();
        map.put( "url", url );

        return map;
    }

    @GetMapping( value = "/authenticate", produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage authenticate(
        @RequestParam String code, @CurrentUser User currentUser )
    {
        if ( currentUser == null )
        {
            throw new BadCredentialsException( "No current user" );
        }

        if ( !TwoFactoryAuthenticationUtils.verify( currentUser, code ) )
        {
            return unauthorized( "2FA code not authenticated" );
        }
        else
        {
            return ok( "2FA code authenticated" );
        }
    }

    @PostMapping( value = "/enable", consumes = { "text/*", "application/*" } )
    @ResponseStatus( HttpStatus.OK )
    @ResponseBody
    public void enable(
        @RequestBody Map<String, String> body, @CurrentUser( required = true ) User currentUser )
        throws WebMessageException
    {
        validateCode( currentUser, body.get( "code" ) );
        defaultUserService.set2FA( currentUser, true );
    }

    @PostMapping( value = "/disable", consumes = { "text/*", "application/*" } )
    @ResponseStatus( HttpStatus.OK )
    @ResponseBody
    public void disable(
        @RequestBody Map<String, String> body, @CurrentUser( required = true ) User currentUser )
        throws WebMessageException
    {
        validateCode( currentUser, body.get( "code" ) );
        defaultUserService.set2FA( currentUser, false );
    }

    private static void validateCode( User currentUser, String code )
        throws WebMessageException
    {
        if ( currentUser == null )
        {
            throw new BadCredentialsException( "No current user" );
        }

        if ( code == null || !LongValidator.getInstance().isValid( code )
            || !TwoFactoryAuthenticationUtils.verify( currentUser, code ) )
        {
            throw new WebMessageException( conflict( E3023.getMessage(), E3023 ) );
        }
    }

}
