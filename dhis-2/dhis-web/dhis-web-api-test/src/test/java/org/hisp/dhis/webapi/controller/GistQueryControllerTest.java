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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.JsonResponse;
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
    private String userGroupId;

    private String orgUnitId;

    private String dataSetId;

    @Before
    public void setUp()
    {
        userGroupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'groupX', 'users':[{'id':'" + getSuperuserUid() + "'}]}" ) );

        assertStatus( HttpStatus.NO_CONTENT,
            PATCH( "/users/{id}/birthday", getSuperuserUid(), Body( "{'birthday': '1980-12-12'}" ) ) );

        orgUnitId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'unitA', 'shortName':'unitA', 'openingDate':'2021-01-01'}" ) );

        dataSetId = assertStatus( HttpStatus.CREATED, POST( "/dataSets/",
            "{'name':'set1', 'organisationUnits': [{'id':'" + orgUnitId + "'}], 'periodType':'Daily'}" ) );
    }

    @Test
    public void testGistPropertyList_FilterByCollectionSize()
    {
        String fields = "id,userCredentials.username,userCredentials.twoFA";
        String filter = "userCredentials.created:gt:2021-01-01,userGroups:gt:0";
        JsonObject users = GET( "/userGroups/{uid}/users/gist?fields={fields}&filter={filter}&headless=true",
            userGroupId, fields, filter ).content().getObject( 0 );

        assertTrue( users.has( "id", "userCredentials" ) );
        assertTrue( users.getObject( "userCredentials" ).has( "username", "twoFA" ) );
    }

    @Test
    public void testGistPropertyList_EmbeddedObjectsField()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=id,sharing,users&headless=true",
            getSuperuserUid() ).content().getObject( 0 );

        assertTrue( groups.has( "id", "sharing" ) );
        assertTrue( groups.getObject( "sharing" ).has( "owner", "external", "users", "userGroups", "public" ) );
    }

    @Test
    public void testGistPropertyList_SimpleField()
    {
        assertEquals( singletonList( "groupX" ),
            GET( "/users/{uid}/userGroups/gist?fields=name&headless=true", getSuperuserUid() )
                .content().stringValues() );
    }

    @Test
    public void testGistObject_SimpleField()
    {
        assertEquals( "admin", GET( "/users/{uid}/surname/gist", getSuperuserUid() ).content().string() );
    }

    @Test
    public void testGistObject_RenameField()
    {
        JsonObject user = GET(
            "/users/{uid}/gist?fields=surname~rename(name),userCredentials[username~rename(alias)]~rename(account)",
            getSuperuserUid() ).content();

        assertEquals( "admin", user.getString( "name" ).string() );
        assertEquals( "admin", user.getObject( "account" ).getString( "alias" ).string() );
        assertEquals( 2, user.size() );
    }

    @Test
    public void testGistObject_RemoveField_BangSyntax()
    {
        JsonObject user = GET( "/users/{uid}/gist?fields=*,!surname", getSuperuserUid() ).content();

        assertFalse( user.has( "surname" ) );
    }

    @Test
    public void testGistObject_RemoveField_MinusSyntax()
    {
        JsonObject user = GET( "/users/{uid}/gist?fields=*,-surname", getSuperuserUid() ).content();

        assertFalse( user.has( "surname" ) );
    }

    @Test
    public void testGistPropertyList_TransformToIdsField()
    {
        JsonObject gist = GET( "/dataSets/{id}/organisationUnits/gist?fields=*,dataSets::ids", dataSetId ).content();

        assertHasPager( gist, 1, 50 );
        JsonObject orgUnit = gist.getArray( "organisationUnits" ).getObject( 0 );
        assertEquals( dataSetId, orgUnit.getArray( "dataSets" ).getString( 0 ).string() );
    }

    @Test
    public void testGistPropertyList_AutoTypeMediumUsesSizeTransformation()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users&auto=M", getSuperuserUid() ).content();

        assertHasPager( gist, 1, 50 );
        assertEquals( 1, gist.getArray( "userGroups" ).getObject( 0 ).getNumber( "users" ).intValue() );
    }

    @Test
    public void testGistPropertyList_AutoTypeMediumUsesIdsTransformation()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users&auto=L", getSuperuserUid() ).content();

        assertHasPager( gist, 1, 50 );
        assertEquals( getSuperuserUid(),
            gist.getArray( "userGroups" ).getObject( 0 ).getArray( "users" ).getString( 0 ).string() );
    }

    @Test
    public void testGistPropertyList_TransformToPluckStringProperty()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users::pluck(surname)", getSuperuserUid() )
            .content();

        assertHasPager( gist, 1, 50 );
        assertEquals( "admin",
            gist.getArray( "userGroups" ).getObject( 0 ).getArray( "users" ).getString( 0 ).string() );
    }

    @Test
    public void testGetObjectProperty_TransformHasMemberWithId()
    {
        String url = "/users/{uid}/userGroups/gist?fields=name,users::member({uid})";

        // member(id) with a user that is a member
        JsonObject gist = GET( url, getSuperuserUid(), getSuperuserUid() ).content();
        assertTrue( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );

        // member(id) with a user that is not a member
        gist = GET( url, getSuperuserUid(), "non-existing-user-uid" ).content();
        assertFalse( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );
    }

    @Test
    public void testGistPropertyList_TransformerHasNoMemberWithId()
    {
        String url = "/users/{uid}/userGroups/gist?fields=name,users::not-member({uid})";

        // not-member(id) with a user that is a member
        JsonObject gist = GET( url, getSuperuserUid(), getSuperuserUid() ).content();
        assertFalse( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );

        // not-member(id) with a user that is not a member
        gist = GET( url, getSuperuserUid(), "non-existing-user-uid" ).content();
        assertTrue( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );
    }

    @Test
    public void testGistPropertyList_PagerWithTotal_ResultBased()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users&total=true",
            getSuperuserUid() ).content();

        assertHasPager( gist, 1, 50, 1 );
    }

    @Test
    public void testGistPropertyList_PagerWithTotal_CountQuery()
    {
        // create some members we can count a total for
        createDataSetsForOrganisationUnit( 10, orgUnitId, "extra" );

        String url = "/organisationUnits/{id}/dataSets/gist?total=true&pageSize=3&order=name&filter=name:startsWith:extra";
        JsonObject gist = GET( url, orgUnitId ).content();
        assertHasPager( gist, 1, 3, 10 );

        // now page 2
        gist = GET( url + "&page=2", orgUnitId ).content();
        assertHasPager( gist, 2, 3, 10 );
        assertEquals(
            "/organisationUnits/{id}/dataSets/gist?total=true&pageSize=3&order=name&filter=name:startsWith:extra&page=1"
                .replace( "{id}", orgUnitId ),
            gist.getObject( "pager" ).getString( "prevPage" ).string() );
        assertEquals(
            "/organisationUnits/{id}/dataSets/gist?total=true&pageSize=3&order=name&filter=name:startsWith:extra&page=3"
                .replace( "{id}", orgUnitId ),
            gist.getObject( "pager" ).getString( "nextPage" ).string() );
        JsonArray dataSets = gist.getArray( "dataSets" );
        assertEquals( "extra3", dataSets.getObject( 0 ).getString( "name" ).string() );
        assertEquals( "extra4", dataSets.getObject( 1 ).getString( "name" ).string() );
        assertEquals( "extra5", dataSets.getObject( 2 ).getString( "name" ).string() );
    }

    @Test
    public void testGistPropertyList_FilterByIn()
    {
        // create some items we can filter
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String url = "/organisationUnits/{id}/dataSets/gist?filter=name:in:[plus3,plus5]&fields=name&order=name";
        JsonObject gist = GET( url, orgUnitId ).content();

        assertEquals( asList( "plus3", "plus5" ), gist.getArray( "dataSets" ).stringValues() );
    }

    @Test
    public void testGistPropertyList_FilterByInUid()
    {
        // create some items we can filter
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        List<String> dsUids = GET( "/dataSets/gist?fields=id&filter=name:in:[plus2,plus6]&headless=true" )
            .content().stringValues();
        assertEquals( 2, dsUids.size() );

        String url = "/organisationUnits/{id}/dataSets/gist?filter=id:in:[{ds}]&fields=name&order=name";
        JsonObject gist = GET( url, orgUnitId, String.join( ",", dsUids ) ).content();

        assertEquals( asList( "plus2", "plus6" ), gist.getArray( "dataSets" ).stringValues() );
    }

    @Test
    public void testGistList_FilterByPropertyInUid()
    {
        // create some items we can filter
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String url = "/dataSets/gist?filter=organisationUnits.id:in:[{id}]&fields=name&filter=name:startsWith:plus&headless=true";
        JsonResponse gist = GET( url, orgUnitId ).content();
        assertEquals( 10, gist.size() );
    }

    @Test
    public void testGistList_FilterByPropertyLikeName()
    {
        // create some items we can filter
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String orgUnitName = GET( "/organisationUnits/{id}/name/gist", orgUnitId ).content().string();
        String url = "/dataSets/gist?filter=organisationUnits.name:like:{name}&fields=name&filter=name:startsWith:plus&headless=true";
        JsonResponse gist = GET( url, orgUnitName ).content();
        assertEquals( 10, gist.size() );
    }

    @Test
    public void testGistPropertyList_EndpointsWithAbsoluteURLs()
    {
        JsonObject groups = GET( "/users/{uid}/userGroups/gist?fields=name,users&absoluteUrls=true",
            getSuperuserUid() ).content();

        assertTrue( groups.getArray( "userGroups" ).getObject( 0 ).getObject( "apiEndpoints" ).getString( "users" )
            .string().startsWith( "http://" ) );
    }

    @Test
    public void testGistObject_ComplexField_SquareBracketsSyntax()
    {
        JsonObject user = GET( "/users/{uid}/gist?fields=id,userCredentials[id,username]",
            getSuperuserUid() ).content();

        assertEquals( 2, user.size() );
        assertEquals( asList( "id", "username" ), user.getObject( "userCredentials" ).names() );
    }

    @Test
    public void testGistPropertyList_DisplayName()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=displayName,id", getSuperuserUid() ).content();

        JsonArray groups = gist.getArray( "userGroups" );
        assertEquals( 1, groups.size() );
        JsonObject group = groups.getObject( 0 );
        assertEquals( asList( "displayName", "id" ), group.names() );
        assertEquals( "groupX", group.getString( "displayName" ).string() );
    }

    private void createDataSetsForOrganisationUnit( int count, String organisationUnitId, String namePrefix )
    {
        for ( int i = 0; i < count; i++ )
        {
            assertStatus( HttpStatus.CREATED, POST( "/dataSets/", "{'name':'" + namePrefix + i
                + "', 'organisationUnits': [{'id':'" + organisationUnitId + "'}], 'periodType':'Daily'}" ) );
        }
    }

    private static void assertHasPager( JsonObject response, int page, int pageSize )
    {
        assertHasPager( response, page, pageSize, null );
    }

    private static void assertHasPager( JsonObject response, int page, int pageSize, Integer total )
    {
        JsonObject pager = response.getObject( "pager" );
        assertTrue( "Pager is missing", pager.exists() );
        assertEquals( page, pager.getNumber( "page" ).intValue() );
        assertEquals( pageSize, pager.getNumber( "pageSize" ).intValue() );
        if ( total != null )
        {
            assertEquals( total.intValue(), pager.getNumber( "total" ).intValue() );
            assertEquals( (int) Math.ceil( total / (double) pageSize ), pager.getNumber( "pageCount" ).intValue() );
        }
    }
}
