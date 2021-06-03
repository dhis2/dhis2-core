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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.JsonString;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Tests the new collection API for collection property of a single specific
 * owner object.
 *
 * @author Jan Bernitt
 */
public class GistQueryControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

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
    public void testGistPropertyList_PagerWithTotal_CountQueryNonExistingPage()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users&total=true&page=6",
            getSuperuserUid() ).content();

        assertHasPager( gist, 6, 50, 1 );
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

    @Test
    public void testGistPropertyList_DisplayNameWithLocale()
    {
        assertStatus( HttpStatus.NO_CONTENT, PUT( "/organisationUnits/" + orgUnitId + "/translations",
            "{'translations': [" +
                "{'locale':'sv', 'property':'name', 'value':'enhet A'}, " +
                "{'locale':'de', 'property':'name', 'value':'Einheit A'}]}" ) );

        JsonString displayName = GET( "/organisationUnits/{id}/gist?fields=displayName&locale=de&headless=true",
            orgUnitId ).content();
        assertEquals( "Einheit A", displayName.string() );

        displayName = GET( "/organisationUnits/{id}/gist?fields=displayName&locale=sv&headless=true",
            orgUnitId ).content();
        assertEquals( "enhet A", displayName.string() );
    }

    @Test
    public void testGistPropertyList_Access()
    {
        JsonArray groups = GET( "/users/{uid}/userGroups/gist?fields=id,access&headless=true",
            getSuperuserUid() ).content();

        assertEquals( 1, groups.size() );
        JsonObject group = groups.getObject( 0 );
        JsonObject access = group.getObject( "access" );
        assertTrue( access.has( "manage", "externalize", "write", "read", "update", "delete" ) );
        assertTrue( access.getBoolean( "manage" ).booleanValue() );
        assertTrue( access.getBoolean( "externalize" ).booleanValue() );
        assertTrue( access.getBoolean( "write" ).booleanValue() );
        assertTrue( access.getBoolean( "read" ).booleanValue() );
        assertTrue( access.getBoolean( "update" ).booleanValue() );
        assertTrue( access.getBoolean( "delete" ).booleanValue() );
    }

    /*
     * Tests for filters
     */

    @Test
    public void testGistObjectList_FilterBy_Null()
    {
        assertEquals( 1, GET( "/users/gist?filter=skype:null&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:null&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_NotNull()
    {
        assertEquals( 1, GET( "/users/gist?filter=code:!null&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=skype:!null&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_Eq()
    {
        assertEquals( 1, GET( "/users/gist?filter=code:eq:admin&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=code:eq:Hans&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_NotEq()
    {
        assertEquals( 1, GET( "/users/gist?filter=code:!eq:Paul&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=code:neq:Paul&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=code:ne:Paul&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=code:ne:admin&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_Empty()
    {
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnitId );
        ou.setComment( "" );
        organisationUnitService.updateOrganisationUnit( ou );

        assertEquals( 1, GET( "/organisationUnits/gist?filter=comment:empty&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:empty&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_NotEmpty()
    {
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnitId );
        ou.setComment( "" );
        organisationUnitService.updateOrganisationUnit( ou );

        assertEquals( 1, GET( "/users/gist?filter=surname:!empty&headless=true" ).content().size() );
        assertEquals( 0, GET( "/organisationUnits/gist?filter=comment:!empty&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_LessThan()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.lastUpdated:lt:now&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.lastUpdated:lt:2000-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_LessThanOrEqual()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:le:now&headless=true" ).content().size() );
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:lte:now&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.created:lte:2000-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_GreaterThan()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.lastUpdated:gt:2000-01-01&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.lastUpdated:gt:2525-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_GreaterThanOrEqual()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:gte:2000-01-01&headless=true" ).content().size() );
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:ge:2000-01-01&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.created:ge:2525-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testGistObjectList_FilterBy_Like()
    {
        assertEquals( 1, GET( "/users/gist?filter=surname:like:mi&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:ilike:mi&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:like:?dmin&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:like:ad*&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:like:Zulu&headless=true" ).content().size() );
    }

    @Test
    public void testGistObject_FilterBy_NotLike()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.username:!like:mike&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:!ilike:?min&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:!like:?min&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:!like:ap*&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:!like:ad?in&headless=true" ).content().size() );
    }

    @Test
    public void testGistObject_FilterBy_StartsWith()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.username:$like:ad&headless=true" ).content().size() );
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.username:$ilike:adm&headless=true" ).content().size() );
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.username:startsWith:admi&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.username:startsWith:bat&headless=true" ).content().size() );
    }

    @Test
    public void testGistObject_FilterBy_NotStartsWith()
    {
        assertEquals( 1, GET( "/users/gist?filter=firstName:!$like:mike&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!$ilike:bat&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!startsWith:tic&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=firstName:!startsWith:ad&headless=true" ).content().size() );
    }

    @Test
    public void testGistObject_FilterBy_EndsWith()
    {
        assertEquals( 1, GET( "/users/gist?filter=firstName:like$:dmin&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:ilike$:in&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:endsWith:min&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=firstName:endsWith:bat&headless=true" ).content().size() );
    }

    @Test
    public void testGistObject_FilterBy_NotEndsWith()
    {
        assertEquals( 1, GET( "/users/gist?filter=firstName:!like$:mike&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!ilike$:bat&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!endsWith:tic&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=firstName:!endsWith:min&headless=true" ).content().size() );
    }

    @Test
    public void testGistPropertyList_FilterBy_Name_In()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String url = "/organisationUnits/{id}/dataSets/gist?filter=name:in:[plus3,plus5]&fields=name&order=name";
        JsonObject gist = GET( url, orgUnitId ).content();

        assertEquals( asList( "plus3", "plus5" ), gist.getArray( "dataSets" ).stringValues() );
    }

    @Test
    public void testGistPropertyList_FilterBy_Name_NotIn()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "item" );

        assertEquals( asList( "item3", "item4" ),
            GET( "/dataSets/gist?fields=name&filter=name:in:[item3,item4]&headless=true&order=name", orgUnitId )
                .content().stringValues() );
    }

    @Test
    public void testGistPropertyList_FilterBy_Id_In()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        List<String> dsUids = GET( "/dataSets/gist?fields=id&filter=name:in:[plus2,plus6]&headless=true" )
            .content().stringValues();
        assertEquals( 2, dsUids.size() );

        String url = "/organisationUnits/{id}/dataSets/gist?filter=id:in:[{ds}]&fields=name&order=name";
        JsonObject gist = GET( url, orgUnitId, String.join( ",", dsUids ) ).content();

        assertEquals( asList( "plus2", "plus6" ), gist.getArray( "dataSets" ).stringValues() );
    }

    @Test
    public void testGistPropertyList_FilterBy_1toMany_Size()
    {
        String fields = "id,userCredentials.username,userCredentials.twoFA";
        String filter = "userCredentials.created:gt:2021-01-01,userGroups:gt:0";
        JsonObject users = GET( "/userGroups/{uid}/users/gist?fields={fields}&filter={filter}&headless=true",
            userGroupId, fields, filter ).content().getObject( 0 );

        assertTrue( users.has( "id", "userCredentials" ) );
        assertTrue( users.getObject( "userCredentials" ).has( "username", "twoFA" ) );
    }

    @Test
    public void testGistList_FilterBy_1toMany_In()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "in" );

        String url = "/dataSets/gist?filter=organisationUnits.id:in:[{id}]&fields=name&filter=name:startsWith:in&headless=true";
        assertEquals( 10, GET( url, orgUnitId ).content().size() );
    }

    @Test
    public void testGistList_FilterBy_1toMany_NotIn()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "notIn" );

        String url = "/dataSets/gist?filter=organisationUnits.id:!in:[{id}]&fields=name&filter=name:startsWith:notIn&headless=true";
        assertEquals( 10, GET( url, "fakeUid" ).content().size() );
    }

    @Test
    public void testGistList_FilterBy_1toMany_Empty()
    {
        createDataSetsForOrganisationUnit( 7, orgUnitId, "empty" );
        String url = "/dataSets/gist?filter=sections:empty&fields=name&filter=name:startsWith:empty&headless=true";
        assertEquals( 7, GET( url ).content().size() );
    }

    @Test
    public void testGistList_FilterBy_1toMany_NotEmpty()
    {
        createDataSetsForOrganisationUnit( 8, orgUnitId, "non-empty" );

        String url = "/dataSets/gist?filter=organisationUnits:!empty&fields=name&filter=name:startsWith:non&headless=true";
        assertEquals( 8, GET( url ).content().size() );
    }

    /**
     * When an property is a collection of identifiable objects one is allowed
     * to use the {@code in} filter on the collection property which has the
     * same effect as if one would use {@code property.id}.
     */
    @Test
    public void testGistList_FilterBy_1toMany_In_ShortSyntax()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String url = "/dataSets/gist?filter=organisationUnits:in:[{id}]&fields=name&filter=name:startsWith:plus&headless=true";
        assertEquals( 10, GET( url, orgUnitId ).content().size() );
    }

    @Test
    public void testGistList_FilterBy_1toMany_Like()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String orgUnitName = GET( "/organisationUnits/{id}/name/gist", orgUnitId ).content().string();
        String url = "/dataSets/gist?filter=organisationUnits.name:like:{name}&fields=name&filter=name:startsWith:plus&headless=true";
        assertEquals( 10, GET( url, orgUnitName ).content().size() );
    }

    @Test
    public void testGistObject_FilterBy_1to1_1toMany_Eq()
    {
        // filter asks: does the user's userCredential have a user role which
        // name is equal to "Superuser"
        assertEquals( getSuperuserUid(),
            GET( "/users/{id}/gist?fields=id&filter=userCredentials.userRoles.name:eq:Superuser",
                getSuperuserUid() ).content().string() );
    }

    /*
     * Validation
     */

    @Test
    public void testGistObjectList_Filter_MisplacedArgument()
    {
        assertEquals( "Filter `surname:null:[value]` uses an unary operator and does not need an argument.",
            GET( "/users/gist?filter=surname:null:value" ).error( HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    public void testGistObjectList_Filter_MissingArgument()
    {
        assertEquals( "Filter `surname:eq:[]` uses a binary operator that does need an argument.",
            GET( "/users/gist?filter=surname:eq" ).error( HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    public void testGistObjectList_Filter_TooManyArguments()
    {
        assertEquals( "Filter `surname:gt:[a, b]` can only be used with a single argument.",
            GET( "/users/gist?filter=surname:gt:[a,b]" ).error( HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    public void testGistObjectList_Filter_CanRead_UserDoesNotExist()
    {
        assertEquals(
            "Filtering by user access in filter `surname:canread:[not-a-UID]` requires permissions to manage the user filtered by.",
            GET( "/users/gist?filter=surname:canRead:not-a-UID" ).error( HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    public void testGistObjectList_Filter_CanRead_NotAuthorized()
    {
        String uid = getSuperuserUid();
        switchToGuestUser();
        assertEquals(
            "Filtering by user access in filter `surname:canread:[" + uid
                + "]` requires permissions to manage the user filtered by.",
            GET( "/users/gist?filter=surname:canRead:{id}", uid ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testGistObjectList_Filter_CanAccessMissingPattern()
    {
        assertEquals(
            "Filter `surname:canaccess:[fake-UID]` requires a user ID and a access pattern argument.",
            GET( "/users/gist?filter=surname:canAccess:fake-UID" ).error( HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    public void testGistObjectList_Filter_CanAccessMaliciousPattern()
    {
        assertEquals(
            "Filter `surname:canaccess:[fake-UID, drop tables]` pattern argument must be 2 to 8 letters allowing letters 'r', 'w', '_' and '%'.",
            GET( "/users/gist?filter=surname:canAccess:[fake-UID,drop tables]" ).error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    public void testGistObjectList_Order_CollectionProperty()
    {
        assertEquals( "Property `userGroup` cannot be used as order property.",
            GET( "/users/gist?order=userGroups" ).error( HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    public void testGistObjectList_Field_UnknownPreset()
    {
        assertEquals( "Field not supported: `:unknown`",
            GET( "/organisationUnits/gist?fields=:unknown" ).error( HttpStatus.CONFLICT ).getMessage() );
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

    /**
     * The guest user will get the {@code Test_skipSharingCheck} authority so we
     * do not get errors from the H2 database complaining that it does not
     * support JSONB functions. Obviously this has an impact on the results
     * which are not longer filter, the {@code sharing} is ignored.
     */
    private void switchToGuestUser()
    {
        switchToNewUser( "guest", "Test_skipSharingCheck" );
    }
}
