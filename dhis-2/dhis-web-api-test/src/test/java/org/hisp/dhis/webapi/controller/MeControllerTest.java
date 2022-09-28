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

import static java.util.Collections.singletonList;
import static org.hisp.dhis.web.WebClientUtils.assertSeries;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.HttpStatus.Series;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.user.MeController} API.
 *
 * @author Jan Bernitt
 */
class MeControllerTest extends DhisControllerConvenienceTest
{
    private User userA;

    @BeforeEach
    void setUp()
    {
        userA = createUserWithAuth( "userA", "ALL" );

        switchContextToUser( userA );
    }

    @Test
    void testGetCurrentUser()
    {
        switchToSuperuser();
        assertEquals( getCurrentUser().getUid(), GET( "/me" ).content().as( JsonUser.class ).getId() );
    }

    @Test
    void testGetCurrentUserDataApprovalWorkflows()
    {
        JsonArray workflows = GET( "/me/dataApprovalWorkflows" ).content().getArray( "dataApprovalWorkflows" );
        assertTrue( workflows.isArray() );
        assertTrue( workflows.isEmpty() );
    }

    @Test
    void testGetAuthorities()
    {
        assertEquals( singletonList( "ALL" ), GET( "/me/authorities" ).content( HttpStatus.OK ).stringValues() );
    }

    @Test
    void testUpdateCurrentUser()
    {
        assertSeries( Series.SUCCESSFUL, PUT( "/me", "{'surname':'Lars'}" ) );
        assertEquals( "Lars", GET( "/me" ).content().as( JsonUser.class ).getSurname() );
    }

    @Test
    void testHasAuthority()
    {
        assertTrue( GET( "/me/authorities/ALL" ).content( HttpStatus.OK ).booleanValue() );
        // with no authorities
        switchToNewUser( "Kalle" );
        assertFalse( GET( "/me/authorities/missing" ).content( HttpStatus.OK ).booleanValue() );
    }

    @Test
    void testGetSettings()
    {
        JsonObject settings = GET( "/me/settings" ).content( HttpStatus.OK );
        assertTrue( settings.isObject() );
        assertFalse( settings.isEmpty() );
        assertTrue( settings.get( "keyMessageSmsNotification" ).exists() );
        assertEquals( "en", settings.getString( "keyUiLocale" ).string() );
    }

    @Test
    void testGetSetting()
    {
        assertEquals( "en", GET( "/me/settings/{key}", "keyUiLocale" ).content( HttpStatus.OK ).string() );
    }

    @Test
    void testGetSetting_Missing()
    {
        assertEquals( "Key is not supported: missing",
            GET( "/me/settings/missing" ).error( Series.CLIENT_ERROR ).getMessage() );
    }

    @Test
    void testChangePassword()
    {
        assertStatus( HttpStatus.ACCEPTED,
            PUT( "/me/changePassword", "{'oldPassword':'district','newPassword':'$ecrEt42'}" ) );
    }

    @Test
    void testChangePassword_WrongNew()
    {
        assertEquals( "Password must have at least 8, and at most 40 characters",
            PUT( "/me/changePassword", "{'oldPassword':'district','newPassword':'secret'}" )
                .error( Series.CLIENT_ERROR ).getMessage() );
    }

    @Test
    void testChangePassword_WrongOld()
    {
        assertEquals( "OldPassword is incorrect",
            PUT( "/me/changePassword", "{'oldPassword':'wrong','newPassword':'secret'}" ).error( Series.CLIENT_ERROR )
                .getMessage() );
    }

    @Test
    void testChangePassword_NoUser()
    {
        switchContextToUser( null );
        assertEquals( "User object is null, user is not authenticated.",
            PUT( "/me/changePassword", "{'oldPassword':'district','newPassword':'$ecrEt42'}" )
                .error( Series.CLIENT_ERROR ).getMessage() );
    }

    @Test
    void testVerifyPasswordText()
    {
        assertTrue( POST( "/me/verifyPassword", "text/plain:district" ).content().getBoolean( "isCorrectPassword" )
            .booleanValue() );
        assertFalse( POST( "/me/verifyPassword", "text/plain:wrong" ).content().getBoolean( "isCorrectPassword" )
            .booleanValue() );
    }

    @Test
    void testVerifyPasswordJson()
    {
        assertTrue( POST( "/me/verifyPassword", "{'password':'district'}" ).content().getBoolean( "isCorrectPassword" )
            .booleanValue() );
        assertFalse( POST( "/me/verifyPassword", "{'password':'wrong'}" ).content().getBoolean( "isCorrectPassword" )
            .booleanValue() );
    }

    public interface JsonPasswordValidation extends JsonObject
    {

        default boolean isValidPassword()
        {
            return getBoolean( "isValidPassword" ).booleanValue();
        }

        default String getErrorMessage()
        {
            return getString( "errorMessage" ).string();
        }
    }

    @Test
    void testValidatePasswordText()
    {
        JsonPasswordValidation result = POST( "/me/validatePassword", "text/plain:$ecrEt42" ).content()
            .as( JsonPasswordValidation.class );
        assertTrue( result.isValidPassword() );
        assertNull( result.getErrorMessage() );
    }

    @Test
    void testValidatePasswordText_NoDigits()
    {
        JsonPasswordValidation result = POST( "/me/validatePassword", "text/plain:secret" ).content()
            .as( JsonPasswordValidation.class );
        assertFalse( result.isValidPassword() );
        assertEquals( "Password must have at least 8, and at most 40 characters", result.getErrorMessage() );
    }

    @Test
    void testGetDashboard()
    {
        JsonObject dashboard = GET( "/me/dashboard" ).content();
        assertEquals( 0, dashboard.getNumber( "unreadInterpretations" ).intValue() );
        assertEquals( 0, dashboard.getNumber( "unreadMessageConversations" ).intValue() );
    }

    @Test
    void testUpdateInterpretationsLastRead()
    {
        assertStatus( HttpStatus.NO_CONTENT, POST( "/me/dashboard/interpretations/read" ) );
    }

    @Test
    void testGetApprovalLevels()
    {
        assertTrue( GET( "/me/dataApprovalLevels" ).content( HttpStatus.OK ).isArray() );
    }

    @Test
    void testEnable2FAOnMeControllerNotAllowed()
    {
        assertFalse( userA.isTwoFA() );

        assertStatus( HttpStatus.BAD_REQUEST,
            PUT( "/me", "{'userCredentials':{'twoFA':true}}" ) );
    }

    @Test
    void testLegacyUserCredentialsIdPresent()
    {
        JsonResponse response = GET( "/me?fields=id,userCredentials" ).content();
        JsonObject userCredentials = response.getObject( "userCredentials" );
        JsonValue id = userCredentials.get( "id" );
        assertTrue( id.exists() );
    }
}
