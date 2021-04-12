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
package org.hisp.dhis.webapi.controller;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the new collection API for collection property of a single specific
 * owner object.
 *
 * @author Jan Bernitt
 */
public class GistQueryControllerTest extends DhisControllerConvenienceTest
{
    private String groupId;

    @Before
    public void setUp()
    {
        groupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'groupX', 'users':[{'id':'" + getSuperuserUid() + "'}]}" ) );

        assertStatus( HttpStatus.NO_CONTENT,
            PATCH( "/users/{id}/birthday", getSuperuserUid(), Body( "{'birthday': '1980-12-12'}" ) ) );
    }

    // TODO previousPasswords

    @Test
    public void testGetObjectPropertyItems_CollectionSizeFilter()
    {
        String fields = "id,userCredentials.username,userCredentials.twoFA";
        String filter = "userCredentials.created:gt:2021-01-01,userGroups:gt:0";
        JsonObject users = GET( "/userGroups/{uid}/users/gist?fields={fields}&filter={filter}&headless=true",
            groupId, fields, filter ).content().getObject( 0 );

        assertTrue( users.has( "id", "userCredentials" ) );
        assertTrue( users.getObject( "userCredentials" ).has( "username", "twoFA" ) );
    }

    @Test
    public void testGetObjectPropertyItems_EmbeddedObjects()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=id,sharing,users&headless=true",
            getSuperuserUid() ).content().getObject( 0 );

        assertTrue( groups.has( "id", "sharing" ) );
        assertTrue( groups.getObject( "sharing" ).has( "owner", "external", "users", "userGroups", "public" ) );
    }

    @Test
    public void testGetObjectPropertyItems_SingleField()
    {
        JsonArray groupNames = GET( "/users/{uid}/userGroups/gist?fields=name&headless=true", getSuperuserUid() )
            .content();

        assertEquals( singletonList( "groupX" ), groupNames.stringValues() );
    }

    @Test
    public void testGetObjectPropertyItems_SingleNonCollectionField()
    {
        JsonObject surname = GET( "/users/{uid}/surname", getSuperuserUid() ).content();

        assertEquals( "admin", surname.getString( "surname" ).string() );
        assertEquals( 1, surname.size() );
    }

    @Test
    public void testGetObjectPropertyItems_AliasField()
    {
        String unitId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'unitA', 'shortName':'unitA', 'openingDate':'2021-01-01'}" ) );

        String setId = assertStatus( HttpStatus.CREATED, POST( "/dataSets/",
            "{'name':'set1', 'organisationUnits': [{'id':'" + unitId + "'}], 'periodType':'Daily'}" ) );
        System.out.println( GET( "/dataSets/{id}/organisationUnits/gist", setId ).content() );
    }

    @Test
    public void testGetObjectPropertyItems_RefCount()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users&all=M", getSuperuserUid() )
            .content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectPropertyItems_RefIds()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users&all=L", getSuperuserUid() )
            .content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectPropertyItems_Pluck()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users::pluck(surname)", getSuperuserUid() )
            .content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectProperty_Member()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users::member({uid})",
            getSuperuserUid(), getSuperuserUid() ).content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectProperty_NotMember()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users::not-member({uid})",
            getSuperuserUid(), getSuperuserUid() ).content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectPropertyItems_Total()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users&total=true",
            getSuperuserUid() ).content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectPropertyItems_Absolute()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users&absolute=true",
            getSuperuserUid() ).content();

        System.out.println( groups );
    }

    @Test
    public void testGetObjectPropertyItems_SquareBracketsSyntax()
    {
        JsonObject user = GET( "/users/{uid}/gist?fields=id,userCredentials[id,username]",
            getSuperuserUid() ).content();

        assertEquals( 2, user.size() );
        assertEquals( asList( "id", "username" ), user.getObject( "userCredentials" ).names() );
    }

    @Test
    public void testGetObjectPropertyItems_DisplayName()
    {
        JsonObject response = GET( "/users/{uid}/userGroups/gist?fields=displayName,id",
            getSuperuserUid() ).content();

        // TODO set translation for name and verify we got the actual
        // translation

        JsonArray groups = response.getArray( "userGroups" );
        assertEquals( 1, groups.size() );
        JsonObject group = groups.getObject( 0 );
        assertEquals( asList( "displayName", "id" ), group.names() );
    }
}
