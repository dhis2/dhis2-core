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

import static org.hisp.dhis.user.UserService.TWO_FACTOR_CODE_APPROVAL_PREFIX;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpMethod;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.security.TwoFactorController;
import org.junit.jupiter.api.Test;

import org.jboss.aerogear.security.otp.Totp;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Tests the {@link TwoFactorController} sing (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class TwoFactorControllerTest extends DhisControllerConvenienceTest
{
    @Test
    void testQr2FAConflictMustDisableFirst()
    {
        assertNull( getCurrentUser().getSecret() );

        User user = userService.getUser( CurrentUserUtil.getCurrentUserDetails().getUid() );
        userService.generateTwoFactorSecretForApproval( user );

        user = userService.getUser( CurrentUserUtil.getCurrentUserDetails().getUid() );
        assertNotNull( user.getSecret() );

        String code = new Totp( replaceApprovalPartOfTheSecret( user ) ).now();

        assertStatus( HttpStatus.OK, POST( "/2fa/enable", "{'code':'" + code + "'}" ) );

        user = userService.getUser( CurrentUserUtil.getCurrentUserDetails().getUid() );
        assertNotNull( user.getSecret() );
    }

    @NotNull private static String replaceApprovalPartOfTheSecret( User user )
    {
        return user.getSecret().replace( TWO_FACTOR_CODE_APPROVAL_PREFIX, "" );
    }

    @Test
    void testEnable2FA()
    {
        User newUser = makeUser( "X", List.of( "TEST" ) );
        newUser.setEmail( "valid.x@email.com" );
        userService.addUser( newUser );
        userService.generateTwoFactorSecretForApproval( newUser );

        switchToNewUser( newUser );

        String code = new Totp( replaceApprovalPartOfTheSecret( newUser ) ).now();
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
        // newUser.setTwoFA( true );
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
