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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.security.TwoFactorController;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link TwoFactorController} sing (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class TwoFactorControllerTest extends DhisControllerConvenienceTest
{
    @Test
    void testAuthenticate2FA()
    {
        // Generate a new secret for
        GET( "/2fa/qr" ).content( HttpStatus.ACCEPTED );
        assertWebMessage( "Unauthorized", 401, "ERROR", "2FA code not authenticated",
            GET( "/2fa/authenticate?code=xyz" ).content( HttpStatus.UNAUTHORIZED ) );
    }

    @Test
    void testQr2FA()
    {
        assertFalse( getCurrentUser().getTwoFA() );
        assertNull( getCurrentUser().getSecret() );
        JsonResponse content = GET( "/2fa/qr" ).content( HttpStatus.ACCEPTED );
        User user = userService.getUser( CurrentUserUtil.getCurrentUserDetails().getUid() );
        assertNotNull( user.getSecret() );

        String url = content.getMap( "url", JsonString.class ).toString();
        assertTrue( url.startsWith( "\"https://chart.googleapis.com" ) );
    }

    @Test
    void testQr2FAConflictMustDisableFirst()
    {
        assertFalse( getCurrentUser().getTwoFA() );
        assertNull( getCurrentUser().getSecret() );
        GET( "/2fa/qr" ).content( HttpStatus.ACCEPTED );
        User user = userService.getUser( CurrentUserUtil.getCurrentUserDetails().getUid() );
        assertNotNull( user.getSecret() );

        String code = new Totp( user.getSecret() ).now();
        assertStatus( HttpStatus.OK, POST( "/2fa/enable", "{'code':'" + code + "'}" ) );

        user = userService.getUser( CurrentUserUtil.getCurrentUserDetails().getUid() );
        assertNotNull( user.getSecret() );
        assertTrue( user.getTwoFA() );

        GET( "/2fa/qr" ).content( HttpStatus.CONFLICT );
    }

    @Test
    void testEnable2FA()
    {
        User newUser = makeUser( "X", List.of( "TEST" ) );
        newUser.setEmail( "valid.x@email.com" );
        newUser.setTwoFA( true );
        userService.addUser( newUser );
        userService.generateTwoFactorSecretForApproval( newUser );

        switchToNewUser( newUser );

        String code = new Totp( getCurrentUser().getSecret() ).now();

        assertStatus( HttpStatus.OK, POST( "/2fa/enable", "{'code':'" + code + "'}" ) );
    }

    @Test
    void testEnable2FAWrongCode()
    {
        assertEquals( "Invalid 2FA code",
            POST( "/2fa/enable", "{'code':'wrong'}" ).error( HttpStatus.Series.CLIENT_ERROR ).getMessage() );
    }

    @Test
    void testDisable2FA()
    {
        User newUser = makeUser( "Y", List.of( "TEST" ) );
        newUser.setEmail( "valid.y@email.com" );
        newUser.setTwoFA( true );
        userService.addUser( newUser );
        userService.generateTwoFactorSecretForApproval( newUser );

        switchToNewUser( newUser );

        String code = new Totp( getCurrentUser().getSecret() ).now();

        assertStatus( HttpStatus.OK, POST( "/2fa/disable", "{'code':'" + code + "'}" ) );
    }

    @Test
    void testDisable2FAWrongCode()
    {
        assertEquals( "Invalid 2FA code",
            POST( "/2fa/disable", "{'code':'wrong'}" ).error( HttpStatus.Series.CLIENT_ERROR ).getMessage() );
    }

}
