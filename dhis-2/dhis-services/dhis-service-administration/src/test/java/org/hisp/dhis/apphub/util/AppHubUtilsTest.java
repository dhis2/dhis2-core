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
package org.hisp.dhis.apphub.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.apphub.AppHubUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

/**
 * @author Lars Helge Overland
 */
public class AppHubUtilsTest
{
    @Test
    public void testValidateQuery()
    {
        AppHubUtils.validateQuery( "apps" );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateInvalidQueryA()
    {
        AppHubUtils.validateQuery( "apps/../../evil/endpoint" );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateInvalidQueryB()
    {
        AppHubUtils.validateQuery( "http://evildomain" );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateInvalidQueryC()
    {
        AppHubUtils.validateQuery( "" );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateInvalidQueryD()
    {
        AppHubUtils.validateQuery( null );
    }

    @Test
    public void testValidateApiVersionA()
    {
        AppHubUtils.validateApiVersion( "v2" );
    }

    @Test
    public void testValidateApiVersionB()
    {
        AppHubUtils.validateApiVersion( "v146" );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateInvalidApiVersionA()
    {
        AppHubUtils.validateApiVersion( "25" );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateInvalidApiVersionB()
    {
        AppHubUtils.validateApiVersion( "malicious_script.js" );
    }

    @Test
    public void testSanitizeQuery()
    {
        assertEquals( "apps", AppHubUtils.sanitizeQuery( "apps" ) );
        assertEquals( "apps", AppHubUtils.sanitizeQuery( "/apps" ) );
        assertEquals( "apps", AppHubUtils.sanitizeQuery( "//apps" ) );
    }

    @Test
    public void testGetJsonRequestEntity()
    {
        HttpEntity<String> entity = AppHubUtils.getJsonRequestEntity();

        assertTrue( entity.getHeaders().getAccept().contains( MediaType.APPLICATION_JSON ) );
    }
}
