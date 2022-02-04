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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link SqlViewController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class SqlViewControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testExecuteView_NoSuchView()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "SQL view does not exist: xyz",
            POST( "/sqlViews/xyz/execute" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testExecuteView_ValidationError()
    {
        String uid = assertStatus( HttpStatus.CREATED,
            POST( "/sqlViews/", "{'name':'My SQL View','sqlQuery':'select 1 from userinfo'}" ) );
        assertWebMessage( "Conflict", 409, "ERROR", "SQL query contains references to protected tables",
            POST( "/sqlViews/" + uid + "/execute" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testRefreshMaterializedView()
    {
        String uid = assertStatus( HttpStatus.CREATED,
            POST( "/sqlViews/", "{'name':'My SQL View','sqlQuery':'select 1 from userinfo'}" ) );
        assertWebMessage( "Conflict", 409, "ERROR", "View could not be refreshed",
            POST( "/sqlViews/" + uid + "/refresh" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testRefreshMaterializedView_NoSuchView()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "SQL view does not exist: xyz",
            POST( "/sqlViews/xyz/refresh" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testCreateWithDefaultValues()
    {
        String uid = assertStatus( HttpStatus.CREATED,
            POST( "/sqlViews/", "{'name':'My SQL View','sqlQuery':'select 1 from userinfo'}" ) );

        JsonResponse sqlView = GET( "/sqlViews/{uid}", uid ).content();
        assertEquals( "VIEW", sqlView.getString( "type" ).string() );
        assertEquals( "RESPECT_SYSTEM_SETTING", sqlView.getString( "cacheStrategy" ).string() );
    }
}
