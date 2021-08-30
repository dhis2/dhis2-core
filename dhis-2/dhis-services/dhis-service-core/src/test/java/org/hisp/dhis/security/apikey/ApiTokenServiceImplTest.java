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
package org.hisp.dhis.security.apikey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class ApiTokenServiceImplTest extends DhisSpringTest
{

    @Autowired
    private DataElementStore dataElementStore;

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

    @Before
    public final void setup()
        throws Exception
    {
        userService = _userService;
    }

    public ApiToken createAndSaveToken()
    {
        final ApiToken token = new ApiToken();
        token.setType( ApiToken.ApiTokenType.PERSONAL_ACCESS_TOKEN.ordinal() );
        final ApiToken object = apiTokenService.initToken( token );
        apiTokenStore.save( object );

        return token;
    }

    @Test
    public void testListTokens()
    {
        preCreateInjectAdminUser();

        createAndSaveToken();
        createAndSaveToken();

        final List<ApiToken> all = apiTokenService.getAll();

        assertEquals( 2, all.size() );
    }

    @Test
    public void testCantListOthersTokens()
    {
        preCreateInjectAdminUser();

        createAndSaveToken();
        createAndSaveToken();

        switchToOtherUser();

        final List<ApiToken> all = apiTokenService.getAll();

        assertEquals( 0, all.size() );
    }

    @Test
    public void testSaveGet()
    {
        preCreateInjectAdminUser();

        final ApiToken apiToken0 = createAndSaveToken();
        final ApiToken apiToken1 = apiTokenService.getWithKey( apiToken0.getKey() );

        assertEquals( apiToken1.getKey(), apiToken0.getKey() );
    }

    @Test
    public void testUpdate()
    {
        preCreateInjectAdminUser();

        final ApiToken apiToken0 = createAndSaveToken();
        final ApiToken apiToken1 = apiTokenService.getWithKey( apiToken0.getKey() );

        assertEquals( apiToken1.getKey(), apiToken0.getKey() );

        apiToken1.addIpToAllowedList( "1.1.1.1" );
        apiTokenService.update( apiToken1 );

        final ApiToken apiToken2 = apiTokenService.getWithKey( apiToken0.getKey() );
        assertTrue( apiToken2.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
    }

    @Test( expected = UpdateAccessDeniedException.class )
    public void testCantUpdateOthersTokens()
    {
        preCreateInjectAdminUser();

        final ApiToken apiToken0 = createAndSaveToken();
        final ApiToken apiToken1 = apiTokenService.getWithKey( apiToken0.getKey() );
        assertEquals( apiToken1.getKey(), apiToken0.getKey() );

        apiToken1.addIpToAllowedList( "1.1.1.1" );

        switchToOtherUser();

        apiTokenService.update( apiToken1 );
    }

    @Test
    public void testDelete()
    {
        preCreateInjectAdminUser();

        final ApiToken apiToken0 = createAndSaveToken();
        final ApiToken apiToken1 = apiTokenService.getWithKey( apiToken0.getKey() );
        assertEquals( apiToken1.getKey(), apiToken0.getKey() );

        apiTokenService.delete( apiToken1 );
        assertNull( apiTokenService.getWithUid( apiToken0.getUid() ) );
    }

    @Test( expected = DeleteAccessDeniedException.class )
    public void testCantDeleteOthersToken()
    {
        preCreateInjectAdminUser();

        final ApiToken apiToken0 = createAndSaveToken();
        final ApiToken apiToken1 = apiTokenService.getWithKey( apiToken0.getKey() );
        assertEquals( apiToken1.getKey(), apiToken0.getKey() );

        switchToOtherUser();

        apiTokenService.delete( apiToken1 );
        assertNull( apiTokenService.getWithUid( apiToken0.getUid() ) );
    }

    private void switchToOtherUser()
    {
        final User otherUser = createUser( "otherUser" );
        injectSecurityContext( otherUser );
    }
}