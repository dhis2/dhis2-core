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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@code describe=true} parameter of the Gist API.
 *
 * @author Jan Bernitt
 */
class GistDescribeControllerTest extends AbstractGistControllerTest
{

    @Test
    void testDescribe_Object()
    {
        JsonObject description = GET( "/users/{uid}/gist?describe=true", getSuperuserUid() ).content();
        assertBaseDescription( description );
        assertFalse( description.getObject( "hql" ).has( "count" ) );
    }

    @Test
    void testDescribe_ObjectList()
    {
        JsonObject description = GET( "/users/gist?describe=true", getSuperuserUid() ).content();
        assertBaseDescription( description );
        assertFalse( description.getObject( "hql" ).has( "count" ) );
    }

    @Test
    void testDescribe_ObjectCollectionList()
    {
        JsonObject description = GET( "/users/{uid}/userGroups/gist?describe=true", getSuperuserUid() ).content();
        assertBaseDescription( description );
        assertFalse( description.getObject( "hql" ).has( "count" ) );
    }

    @Test
    void testDescribe_Error_PlanningFailed()
    {
        JsonObject description = GET( "/users/{uid}/userGroups/gist?describe=true&filter=foo:eq:bar",
            getSuperuserUid() ).content();
        assertTrue( description.has( "error", "unplanned", "status" ) );
        assertTrue( description.getObject( "error" ).has( "type", "message" ) );
        assertTrue( description.getObject( "unplanned" ).has( "fields", "filters", "orders" ) );
        assertEquals( "planning-failed", description.getString( "status" ).string() );
    }

    @Test
    void testDescribe_Error_ValidationFailed()
    {
        JsonObject description = GET( "/users/gist?describe=true&fields=password", getSuperuserUid() )
            .content();
        assertTrue( description.has( "error", "unplanned", "planned", "status" ) );
        assertTrue( description.getObject( "error" ).has( "type", "message" ) );
        assertTrue( description.getObject( "unplanned" ).has( "fields", "filters", "orders" ) );
        assertEquals( "validation-failed", description.getString( "status" ).string() );
        assertEquals( "Property `password` is not readable.",
            description.getObject( "error" ).getString( "message" ).string() );
    }

    @Test
    void testDescribe_Total()
    {
        JsonObject description = GET( "/users/gist?describe=true&total=true", getSuperuserUid() ).content();
        assertBaseDescription( description );
        assertTrue( description.getObject( "hql" ).has( "count" ) );
    }

    @Test
    void testDescribe_FetchParameters()
    {
        JsonObject description = GET( "/users/gist?describe=true&filter=surname:startsWith:Jo", getSuperuserUid() )
            .content();
        assertBaseDescription( description );
        JsonObject hql = description.getObject( "hql" );
        assertTrue( hql.has( "parameters" ) );
        JsonObject parameters = hql.getObject( "parameters" );
        assertTrue( parameters.isObject() );
        assertEquals( 1, parameters.size() );
        // starts with is case-insensitive so both term and DB field are lowered
        assertEquals( "jo%", parameters.getString( "f_0" ).string() );
    }

    @Test
    void testDescribe_Authorisation_Guest()
    {
        switchToGuestUser();
        JsonObject description = GET( "/users/{uid}/gist?describe=true", getSuperuserUid() ).content();
        assertFalse( description.has( "hql" ) );
    }

    @Test
    void testDescribe_Authorisation_Admin()
    {
        switchToNewUser( "guest", "Test_skipSharingCheck", "F_METADATA_EXPORT" );
        assertBaseDescription( GET( "/users/{uid}/gist?describe=true", getSuperuserUid() ).content() );
    }

    private void assertBaseDescription( JsonObject description )
    {
        assertTrue( description.has( "status", "hql", "planned", "unplanned" ) );
        assertTrue( description.getObject( "hql" ).has( "fetch", "parameters" ) );
        assertTrue( description.getObject( "planned" ).has( "fields", "filters", "orders", "summary" ) );
        assertTrue( description.getObject( "planned" ).has( "fields", "filters", "orders" ) );
        assertEquals( "ok", description.getString( "status" ).string() );
    }
}
