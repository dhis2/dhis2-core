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
package org.hisp.dhis.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;

import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class OAuth2ClientStoreTest extends TransactionalIntegrationTest
{

    @Autowired
    private OAuth2ClientStore oAuth2ClientStore;

    private OAuth2Client clientA;

    private OAuth2Client clientB;

    private OAuth2Client clientC;

    @Override
    public void setUpTest()
    {
        clientA = new OAuth2Client();
        clientA.setName( "clientA" );
        clientA.setCid( "clientA" );
        clientB = new OAuth2Client();
        clientB.setName( "clientB" );
        clientB.setCid( "clientB" );
        clientC = new OAuth2Client();
        clientC.setName( "clientC" );
        clientC.setCid( "clientC" );
    }

    @Test
    void testGetAll()
    {
        oAuth2ClientStore.save( clientA );
        oAuth2ClientStore.save( clientB );
        oAuth2ClientStore.save( clientC );
        Collection<OAuth2Client> all = oAuth2ClientStore.getAll();
        assertEquals( 3, all.size() );
    }

    @Test
    void testGetByClientID()
    {
        oAuth2ClientStore.save( clientA );
        oAuth2ClientStore.save( clientB );
        oAuth2ClientStore.save( clientC );
        assertNotNull( oAuth2ClientStore.getByClientId( "clientA" ) );
        assertNotNull( oAuth2ClientStore.getByClientId( "clientB" ) );
        assertNotNull( oAuth2ClientStore.getByClientId( "clientC" ) );
    }
}
