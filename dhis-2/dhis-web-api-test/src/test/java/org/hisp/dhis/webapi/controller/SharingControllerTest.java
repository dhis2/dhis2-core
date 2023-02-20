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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link SharingController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class SharingControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testPutSharing()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups", "{'name':'test'}" ) );
        JsonIdentifiableObject group = GET( "/userGroups/{id}", groupId ).content().as( JsonIdentifiableObject.class );
        assertWebMessage( "OK", 200, "OK", "Access control set",
            PUT( "/sharing?type=userGroup&id=" + groupId, group.getSharing().node().getDeclaration() )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testPostSharing()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups", "{'name':'test'}" ) );
        JsonIdentifiableObject group = GET( "/userGroups/{id}", groupId ).content().as( JsonIdentifiableObject.class );
        assertWebMessage( "OK", 200, "OK", "Access control set",
            POST( "/sharing?type=userGroup&id=" + groupId, group.getSharing().node().getDeclaration() )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testPostSharing_NoSuchType()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Type xyz is not supported.",
            POST( "/sharing?type=xyz&id=abc", "{}" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testPostSharing_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Object of type userGroup with ID xyz was not found.",
            POST( "/sharing?type=userGroup&id=xyz", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testPostSharing_InvalidPublicSharing()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups", "{'name':'test'}" ) );
        JsonIdentifiableObject group = GET( "/userGroups/{id}", groupId ).content().as( JsonIdentifiableObject.class );
        JsonNode sharing = group.getSharing().node().extract().addMember( "publicAccess", "\"xyz\"" );
        assertWebMessage( "Conflict", 409, "ERROR", "Invalid public access string: xyz",
            POST( "/sharing?type=userGroup&id=" + groupId, "{'object': " + sharing.getDeclaration() + "}" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testGetSharing()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups", "{'name':'test'}" ) );
        JsonObject sharing = GET( "/sharing?type=userGroup&id=" + groupId ).content( HttpStatus.OK );
        JsonObject meta = sharing.getObject( "meta" );
        assertTrue( meta.getBoolean( "allowPublicAccess" ).booleanValue() );
        assertFalse( meta.getBoolean( "allowExternalAccess" ).booleanValue() );
        JsonObject object = sharing.getObject( "object" );
        assertEquals( groupId, object.getString( "id" ).string() );
        assertEquals( "test", object.getString( "name" ).string() );
        assertEquals( "test", object.getString( "displayName" ).string() );
        assertEquals( "rw------", object.getString( "publicAccess" ).string() );
        assertFalse( object.getBoolean( "externalAccess" ).booleanValue() );
        assertEquals( "FirstNameadmin Surnameadmin", object.getObject( "user" ).getString( "name" ).string() );
        assertEquals( 0, object.getArray( "userGroupAccesses" ).size() );
        assertEquals( 0, object.getArray( "userAccesses" ).size() );
    }

    @Test
    void testSearchUserGroups()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups", "{'name':'test'}" ) );
        JsonObject matches = GET( "/sharing/search?key=" + groupId ).content( HttpStatus.OK );
        assertTrue( matches.has( "userGroups", "users" ) );
        assertEquals( 0, matches.getArray( "userGroups" ).size() );
        assertEquals( 0, matches.getArray( "users" ).size() );
    }

    @Test
    void testSuperUserGetPrivateObject()
    {
        String dataSetId = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets",
                "{'name':'test','shortName':'test','periodType':'Monthly','sharing':{'public':'--------'}}" ) );
        GET( "/sharing?type=dataSet&id=" + dataSetId ).content( HttpStatus.OK );
        switchToNewUser( "A", "test" );
        GET( "/sharing?type=dataSet&id=" + dataSetId ).content( HttpStatus.FORBIDDEN );
        switchToSuperuser();
        GET( "/sharing?type=dataSet&id=" + dataSetId ).content( HttpStatus.OK );
    }
}
