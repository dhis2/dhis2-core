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
package org.hisp.dhis.security.apikey;

import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePatToken;
import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePersonalAccessToken;
import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.hashToken;
import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.isValidTokenChecksum;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiTokenServiceImplTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private ApiTokenStore apiTokenStore;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    @Qualifier( value = "xmlMapper" )
    public ObjectMapper xmlMapper;

    @Autowired
    private UserService _userService;

    protected MockMvc mvc;

    @BeforeEach
    final void setup()
        throws Exception
    {
        userService = _userService;
        preCreateInjectAdminUser();
    }

    public ApiToken createAndSaveToken()
    {
        long thirtyDaysInTheFuture = System.currentTimeMillis() + TimeUnit.DAYS.toMillis( 30 );
        ApiKeyTokenGenerator.TokenWrapper apiTokenPair = generatePersonalAccessToken( null, thirtyDaysInTheFuture );
        apiTokenStore.save( apiTokenPair.getApiToken() );
        return apiTokenPair.getApiToken();
    }

    @Test
    void testListTokens()
    {
        createAndSaveToken();
        createAndSaveToken();
        final List<ApiToken> all = apiTokenService.getAll();
        assertEquals( 2, all.size() );
    }

    @Test
    void testCantListOthersTokens()
    {
        createAndSaveToken();
        createAndSaveToken();
        switchToOtherUser();
        final List<ApiToken> all = apiTokenService.getAll();
        assertEquals( 0, all.size() );
    }

    @Test
    void testSaveGet()
    {
        final ApiToken tokenA = createAndSaveToken();
        final ApiToken tokenB = apiTokenService.getByKey( tokenA.getKey() );
        assertEquals( tokenB.getKey(), tokenA.getKey() );
    }

    @Test
    void testGetAllByUser()
    {
        final ApiToken token = createAndSaveToken();
        createAndSaveToken();
        CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
        User user = userService.getUserByUsername( currentUserDetails.getUsername() );
        List<ApiToken> allOwning = apiTokenService.getAllOwning( user );

        assertEquals( 2, allOwning.size() );
        assertEquals( allOwning.get( 0 ).getKey(), token.getKey() );
    }

    @Test
    void testSaveGetCurrentUser()
    {
        final ApiToken tokenA = createAndSaveToken();
        CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
        User user = userService.getUserByUsername( currentUserDetails.getUsername() );
        final ApiToken tokenB = apiTokenService.getByKey( tokenA.getKey(), user );
        assertEquals( tokenB.getKey(), tokenA.getKey() );
    }

    @Test
    void testShouldDeleteTokensWhenUserIsDeleted()
    {
        User userB = createUserWithAuth( "userB" );
        injectSecurityContext( userB );

        String apiTokenCreator = CurrentUserUtil.getCurrentUsername();
        createAndSaveToken();
        createAndSaveToken();

        User adminUser = userService.getUserByUsername( "admin_test" );
        injectSecurityContext( adminUser );

        userService.deleteUser( userService.getUserByUsername( apiTokenCreator ) );

        List<ApiToken> all = apiTokenService.getAll();
        assertEquals( 0, all.size() );
    }

    @Test
    void testUpdate()
    {
        final ApiToken tokenA = createAndSaveToken();
        final ApiToken tokenB = apiTokenService.getByKey( tokenA.getKey() );
        assertEquals( tokenB.getKey(), tokenA.getKey() );
        tokenB.addIpToAllowedList( "1.1.1.1" );
        apiTokenService.update( tokenB );
        final ApiToken tokenC = apiTokenService.getByKey( tokenA.getKey() );
        assertTrue( tokenC.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
    }

    @Test
    void testCantUpdateOthersTokens()
    {
        final ApiToken tokenA = createAndSaveToken();
        final ApiToken tokenB = apiTokenService.getByKey( tokenA.getKey() );
        assertEquals( tokenB.getKey(), tokenA.getKey() );
        tokenB.addIpToAllowedList( "1.1.1.1" );
        switchToOtherUser();
        assertThrows( UpdateAccessDeniedException.class, () -> apiTokenService.update( tokenB ) );
    }

    @Test
    void testDelete()
    {
        final ApiToken tokenA = createAndSaveToken();
        final ApiToken tokenB = apiTokenService.getByKey( tokenA.getKey() );
        assertEquals( tokenB.getKey(), tokenA.getKey() );
        apiTokenService.delete( tokenB );
        assertNull( apiTokenService.getByUid( tokenA.getUid() ) );
    }

    @Test
    void testCantDeleteOthersToken()
    {
        final ApiToken tokenA = createAndSaveToken();
        final ApiToken tokenB = apiTokenService.getByKey( tokenA.getKey() );
        assertEquals( tokenB.getKey(), tokenA.getKey() );
        switchToOtherUser();
        assertThrows( DeleteAccessDeniedException.class, () -> apiTokenService.delete( tokenB ) );
    }

    private void switchToOtherUser()
    {
        final User otherUser = createUserWithAuth( "otherUser" );
        injectSecurityContext( otherUser );
    }

    @Test
    void testValidateChecksums()
    {
        char[] token = generatePatToken( ApiTokenType.PERSONAL_ACCESS_TOKEN_V1 );
        assertTrue( isValidTokenChecksum( token ) );
    }

    @Test
    void testHashingToken()
    {
        char[] token = generatePatToken( ApiTokenType.PERSONAL_ACCESS_TOKEN_V1 );
        String hashedToken = hashToken( token );
        assertTrue( HashUtils.isValidSHA256HexFormat( hashedToken ) );
    }
}
