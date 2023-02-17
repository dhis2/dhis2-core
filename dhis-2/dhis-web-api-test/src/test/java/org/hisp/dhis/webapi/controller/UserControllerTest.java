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

import static java.util.Collections.emptySet;
import static org.hisp.dhis.web.WebClient.Accept;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.message.FakeMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.RestoreType;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.user.UserController}.
 *
 * @author Jan Bernitt
 */
@Slf4j
class UserControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserGroupService userGroupService;

    private User peter;

    @BeforeEach
    void setUp()
    {
        peter = createUserWithAuth( "peter" );

        this.peter = switchToNewUser( "Peter" );
        switchToSuperuser();
        assertStatus( HttpStatus.OK, PATCH( "/users/{id}?importReportMode=ERRORS", peter.getUid(),
            Body( "[{'op': 'replace', 'path': '/email', 'value': 'peter@pan.net'}]" ) ) );

        User user = userService.getUser( peter.getUid() );
        assertEquals( "peter@pan.net", user.getEmail() );
    }

    @Test
    void testResetToInvite()
    {
        assertStatus( HttpStatus.NO_CONTENT, POST( "/users/" + peter.getUid() + "/reset" ) );
        OutboundMessage email = assertMessageSendTo( "peter@pan.net" );
        assertValidToken( extractTokenFromEmailText( email.getText() ) );
    }

    @Test
    void testResetToInvite_NoEmail()
    {
        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op': 'replace', 'path': '/email', 'value': null}]" ) ) );
        assertEquals( "User account does not have a valid email address",
            POST( "/users/" + peter.getUid() + "/reset" ).error( HttpStatus.CONFLICT ).getMessage() );
    }

    @Test
    void testUpdateValueInLegacyUserCredentials()
    {
        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op': 'add', 'path': '/openId', 'value': 'mapping value'}]" ) ) );

        User user = userService.getUser( peter.getUid() );
        assertEquals( "mapping value", user.getOpenId() );
    }

    @Test
    void testUpdateOpenIdInLegacyFormat()
    {
        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op': 'add', 'path': '/userCredentials/openId', 'value': 'mapping value'}]" ) ) );

        User user = userService.getUser( peter.getUid() );
        assertEquals( "mapping value", user.getOpenId() );
        assertEquals( "mapping value", user.getUserCredentials().getOpenId() );
    }

    @Test
    void testUpdateRoles()
    {
        UserRole userRole = createUserRole( "ROLE_B", "ALL" );
        userService.addUserRole( userRole );
        String newRoleUid = userService.getUserRoleByName( "ROLE_B" ).getUid();

        User peterBefore = userService.getUser( this.peter.getUid() );
        String mainRoleUid = peterBefore.getUserRoles().iterator().next().getUid();

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", this.peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'add','path':'/userRoles','value':[{'id':'" + newRoleUid + "'},{'id':'" + mainRoleUid
                + "'}]}]" ) ) );

        User peterAfter = userService.getUser( this.peter.getUid() );
        Set<UserRole> userRoles = peterAfter.getUserRoles();

        assertEquals( 2, userRoles.size() );
    }

    @Test
    void testUpdateRolesLegacy()
    {
        UserRole userRole = createUserRole( "ROLE_B", "ALL" );
        userService.addUserRole( userRole );
        String newRoleUid = userService.getUserRoleByName( "ROLE_B" ).getUid();

        User peterBefore = userService.getUser( this.peter.getUid() );
        String mainRoleUid = peterBefore.getUserRoles().iterator().next().getUid();

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", this.peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'add','path':'/userCredentials/userRoles','value':[{'id':'" + newRoleUid
                + "'},{'id':'" + mainRoleUid + "'}]}]" ) ) );

        User peterAfter = userService.getUser( this.peter.getUid() );
        Set<UserRole> userRoles = peterAfter.getUserRoles();

        assertEquals( 2, userRoles.size() );
    }

    @Test
    void testAddGroups()
    {
        UserGroup userGroupA = createUserGroup( 'A', emptySet() );
        manager.save( userGroupA );

        UserGroup userGroupB = createUserGroup( 'B', emptySet() );
        manager.save( userGroupB );

        User peterBefore = userService.getUser( this.peter.getUid() );

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", this.peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'add','path':'/userGroups','value':[{'id':'" + userGroupA.getUid() + "'},{'id':'"
                + userGroupB.getUid()
                + "'}]}]" ) ) );

        User peterAfter = userService.getUser( this.peter.getUid() );
        Set<UserGroup> userGroups = peterAfter.getGroups();

        assertEquals( 2, userGroups.size() );
    }

    @Test
    void testAddThenRemoveGroups()
    {
        UserGroup userGroupA = createUserGroup( 'A', emptySet() );
        manager.save( userGroupA );

        UserGroup userGroupB = createUserGroup( 'B', emptySet() );
        manager.save( userGroupB );

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", this.peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'add','path':'/userGroups','value':[{'id':'" + userGroupA.getUid() + "'},{'id':'"
                + userGroupB.getUid()
                + "'}]}]" ) ) );

        User peterAfter = userService.getUser( this.peter.getUid() );
        Set<UserGroup> userGroups = peterAfter.getGroups();
        assertEquals( 2, userGroups.size() );

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", this.peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'add','path':'/userGroups','value':[]}]" ) ) );

        peterAfter = userService.getUser( this.peter.getUid() );
        userGroups = peterAfter.getGroups();
        assertEquals( 0, userGroups.size() );
    }

    @Test
    void testResetToInvite_NoAuthority()
    {
        switchToNewUser( "someone" );
        assertEquals( "You don't have the proper permissions to update this user.",
            POST( "/users/" + peter.getUid() + "/reset" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    void testResetToInvite_NoSuchUser()
    {
        assertEquals( "User with id does-not-exist could not be found.",
            POST( "/users/does-not-exist/reset" ).error( HttpStatus.NOT_FOUND ).getMessage() );
    }

    @Test
    void testReplicateUser()
    {
        assertWebMessage( "Created", 201, "OK", "User replica created",
            POST( "/users/" + peter.getUid() + "/replica", "{'username':'peter2','password':'Saf€sEcre1'}" )
                .content() );
    }

    @Test
    void testReplicateUser_UserNameAlreadyTaken()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Username already taken: peter",
            POST( "/users/" + peter.getUid() + "/replica", "{'username':'peter','password':'Saf€sEcre1'}" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testReplicateUser_UserNotFound()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "User not found: notfoundid",
            POST( "/users/notfoundid/replica", "{'username':'peter2','password':'Saf€sEcre1'}" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testReplicateUser_UserNameNotSpecified()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Username must be specified",
            POST( "/users/" + peter.getUid() + "/replica", "{'password':'Saf€sEcre1'}" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testReplicateUser_PasswordNotSpecified()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Password must be specified",
            POST( "/users/" + peter.getUid() + "/replica", "{'username':'peter2'}" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testReplicateUser_PasswordNotValid()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            "Password must have at least 8 characters, one digit, one uppercase",
            POST( "/users/" + peter.getUid() + "/replica", "{'username':'peter2','password':'lame'}" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testGetUserLegacyUserCredentialsIdPresent()
    {
        JsonResponse response = GET( "/users/{id}", peter.getUid() ).content();
        JsonObject userCredentials = response.getObject( "userCredentials" );
        JsonValue id = userCredentials.get( "id" );
        assertTrue( id.exists() );
    }

    @Test
    void testPutJsonObject()
    {
        JsonObject user = GET( "/users/{id}", peter.getUid() ).content();
        assertWebMessage( "OK", 200, "OK", null,
            PUT( "/38/users/" + peter.getUid(), user.toString() ).content( HttpStatus.OK ) );
    }

    @Test
    void testPutJsonObject_WithSettings()
    {
        JsonUser user = GET( "/users/{id}", peter.getUid() ).content().as( JsonUser.class );
        assertWebMessage( "OK", 200, "OK", null,
            PUT( "/38/users/" + peter.getUid(),
                user.node().addMember( "settings", "{\"uiLocale\":\"de\"}" ).toString() ).content( HttpStatus.OK ) );
        assertEquals( "de", GET( "/userSettings/keyUiLocale?userId=" + user.getId(), Accept( "text/plain" ) )
            .content( "text/plain" ) );
    }

    @Test
    void testPutJsonObject_Pre38()
    {
        JsonObject user = GET( "/users/{uid}", peter.getUid() ).content();
        JsonImportSummary summary = PUT( "/37/users/" + peter.getUid(), user.toString() ).content( HttpStatus.OK )
            .as( JsonImportSummary.class );
        assertEquals( "ImportReport", summary.getResponseType() );
        assertEquals( "OK", summary.getStatus() );
        assertEquals( 1, summary.getStats().getUpdated() );
        assertEquals( peter.getUid(), summary.getTypeReports().get( 0 ).getObjectReports().get( 0 ).getUid() );
    }

    @Test
    void testPutProperty_InvalidWhatsapp()
    {
        JsonWebMessage msg = assertWebMessage( "Conflict", 409, "ERROR",
            "One or more errors occurred, please see full details in import report.",
            PATCH( "/users/" + peter.getUid() + "?importReportMode=ERRORS",
                "[{'op': 'add', 'path': '/whatsApp', 'value': 'not-a-phone-no'}]" ).content( HttpStatus.CONFLICT ) );
        JsonErrorReport report = msg.getResponse()
            .find( JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4027 );
        assertEquals( "whatsApp", report.getErrorProperty() );
    }

    @Test
    void testPostJsonObject()
    {
        assertWebMessage( "Created", 201, "OK", null,
            POST( "/users/",
                "{'surname':'S.','firstName':'Harry', 'username':'harrys', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}" )
                    .content( HttpStatus.CREATED ) );
    }

    @Test
    void testPostJsonObjectInvalidUsernameLegacyFormat()
    {
        JsonWebMessage msg = assertWebMessage( "Conflict", 409, "ERROR",
            "One or more errors occurred, please see full details in import report.",
            POST( "/users/", "{'surname':'S.','firstName':'Harry','userCredentials':{'username':'_Harrys'}}" )
                .content( HttpStatus.CONFLICT ) );
        JsonErrorReport report = msg.getResponse()
            .find( JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4049 );
        assertEquals( "username", report.getErrorProperty() );
    }

    @Test
    void testPostJsonObjectInvalidUsername()
    {
        JsonWebMessage msg = assertWebMessage( "Conflict", 409, "ERROR",
            "One or more errors occurred, please see full details in import report.",
            POST( "/users/", "{'surname':'S.','firstName':'Harry','username':'_Harrys'}" )
                .content( HttpStatus.CONFLICT ) );
        JsonErrorReport report = msg.getResponse()
            .find( JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4049 );
        assertEquals( "username", report.getErrorProperty() );
    }

    @Test
    void testPutLegacyFormat()
    {
        JsonObject user = GET( "/users/{id}", peter.getUid() ).content();

        String jsonString = "{'openId':'test'}";
        JsonElement jsonElement = new Gson().fromJson( jsonString, JsonElement.class );

        com.google.gson.JsonObject asJsonObject = new Gson().fromJson( user.toString(), JsonElement.class )
            .getAsJsonObject();
        asJsonObject.add( "userCredentials", jsonElement );

        PUT( "/37/users/" + peter.getUid(), asJsonObject.toString() );

        User userAfter = userService.getUser( peter.getUid() );
        assertEquals( "test", userAfter.getOpenId() );
    }

    @Test
    void testPutNewFormat()
    {
        JsonObject user = GET( "/users/{id}", peter.getUid() ).content();

        com.google.gson.JsonObject asJsonObject = new Gson().fromJson( user.toString(), JsonElement.class )
            .getAsJsonObject();
        asJsonObject.addProperty( "openId", "test" );

        PUT( "/37/users/" + peter.getUid(), asJsonObject.toString() );

        User userAfter = userService.getUser( peter.getUid() );
        assertEquals( "test", userAfter.getOpenId() );
    }

    @Test
    void testPostJsonInvite()
    {
        UserRole userRole = createUserRole( "inviteRole", "ALL" );
        userService.addUserRole( userRole );
        UserRole inviteRole = userService.getUserRoleByName( "inviteRole" );
        String roleUid = inviteRole.getUid();

        assertWebMessage( "Created", 201, "OK", null, POST( "/users/invite",
            "{'surname':'S.','firstName':'Harry', 'email':'test@example.com', 'username':'harrys', 'userRoles': [{'id': '"
                + roleUid + "'}]}" )
                    .content( HttpStatus.CREATED ) );
    }

    @Test
    void testPatchUserGroups()
    {
        UserGroup userGroupA = createUserGroup( 'A', Set.of() );
        userGroupA.setUid( "GZSvMCVowAx" );
        manager.save( userGroupA );

        UserGroup userGroupB = createUserGroup( 'B', Set.of() );
        userGroupB.setUid( "B6JNeAQ6akX" );
        manager.save( userGroupB );

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", peter.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op': 'add', 'path': '/userGroups', 'value': [ { 'id': 'GZSvMCVowAx' }, { 'id': 'B6JNeAQ6akX' } ] } ]" ) ) );

        JsonResponse response = GET( "/users/{id}?fields=userGroups", peter.getUid() ).content( HttpStatus.OK );
        assertEquals( 2, response.getArray( "userGroups" ).size() );

        assertStatus( HttpStatus.OK, PATCH( "/users/{id}", peter.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op': 'add', 'path': '/userGroups', 'value': [ { 'id': 'GZSvMCVowAx' } ] } ]" ) ) );
        response = GET( "/users/{id}?fields=userGroups", peter.getUid() ).content( HttpStatus.OK );
        assertEquals( 1, response.getArray( "userGroups" ).size() );
    }

    private String extractTokenFromEmailText( String message )
    {
        int tokenPos = message.indexOf( "?token=" );
        return message.substring( tokenPos + 7, message.indexOf( '\n', tokenPos ) ).trim();
    }

    /**
     * Unfortunately this is not yet a spring endpoint so we have to do it
     * directly instead of using the REST API.
     */
    private void assertValidToken( String token )
    {
        String[] idAndRestoreToken = securityService.decodeEncodedTokens( token );
        String idToken = idAndRestoreToken[0];
        String restoreToken = idAndRestoreToken[1];
        User user = userService.getUserByIdToken( idToken );
        assertNotNull( user );
        ErrorCode errorCode = securityService.validateRestoreToken( user, restoreToken,
            RestoreType.RECOVER_PASSWORD );
        assertNull( errorCode );
    }

    private OutboundMessage assertMessageSendTo( String email )
    {
        List<OutboundMessage> messagesByEmail = ((FakeMessageSender) messageSender).getMessagesByEmail( email );
        assertTrue( messagesByEmail.size() > 0 );
        return messagesByEmail.get( 0 );
    }

    @Test
    void testIllegalUpdateNoPermission()
    {
        switchContextToUser( this.peter );
        assertEquals( "You don't have the proper permissions to update this user.",
            PUT( "/37/users/" + peter.getUid(), "{\"userCredentials\":{\"twoFA\":true}}" ).error( HttpStatus.FORBIDDEN )
                .getMessage() );
    }

    @Test
    void testReset2FAPrivilegedWithNonAdminUser()
    {
        User newUser = makeUser( "X", List.of( "ALL" ) );
        newUser.setEmail( "valid@email.com" );
        String secret = Base32.random();
        newUser.setSecret( secret );
        userService.addUser( newUser );

        switchContextToUser( newUser );

        HttpResponse post = POST( "/users/" + newUser.getUid() + "/twoFA/disabled" );

        String message = post.error( HttpStatus.FORBIDDEN )
            .getMessage();
        assertEquals( "Not allowed to disable 2FA for current user",
            message );

        User userByUsername = userService.getUserByUsername( newUser.getUsername() );
        assertEquals( secret, userByUsername.getSecret() );
    }

    @Test
    void testReset2FAPrivilegedWithAdminUser()
    {
        User newUser = makeUser( "X", List.of( "ALL" ) );
        newUser.setEmail( "valid@email.com" );
        String secret = Base32.random();
        newUser.setSecret( secret );
        userService.addUser( newUser );

        POST( "/users/" + newUser.getUid() + "/twoFA/disabled" ).content( HttpStatus.OK );

        User userByUsername = userService.getUserByUsername( newUser.getUsername() );

        assertNull( userByUsername.getSecret() );
    }
}
