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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Test;

/**
 * Tests the {@link org.hisp.dhis.gist.GistQuery.Filter} related features of the
 * Gist API.
 *
 * @author Jan Bernitt
 */
public class GistFilterControllerTest extends AbstractGistControllerTest
{
    @Test
    public void testFilter_Null()
    {
        assertEquals( 1, GET( "/users/gist?filter=skype:null&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:null&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_NotNull()
    {
        assertEquals( 1, GET( "/users/gist?filter=code:!null&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=skype:!null&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_Eq()
    {
        assertEquals( 1, GET( "/users/gist?filter=code:eq:admin&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=code:eq:Hans&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_NotEq()
    {
        assertEquals( 1, GET( "/users/gist?filter=code:!eq:Paul&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=code:neq:Paul&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=code:ne:Paul&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=code:ne:admin&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_Empty()
    {
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnitId );
        ou.setComment( "" );
        organisationUnitService.updateOrganisationUnit( ou );

        assertEquals( 1, GET( "/organisationUnits/gist?filter=comment:empty&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:empty&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_NotEmpty()
    {
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnitId );
        ou.setComment( "" );
        organisationUnitService.updateOrganisationUnit( ou );

        assertEquals( 1, GET( "/users/gist?filter=surname:!empty&headless=true" ).content().size() );
        assertEquals( 0, GET( "/organisationUnits/gist?filter=comment:!empty&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_LessThan()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.lastUpdated:lt:now&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.lastUpdated:lt:2000-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_LessThanOrEqual()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:le:now&headless=true" ).content().size() );
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:lte:now&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.created:lte:2000-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_GreaterThan()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.lastUpdated:gt:2000-01-01&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.lastUpdated:gt:2525-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_GreaterThanOrEqual()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:gte:2000-01-01&headless=true" ).content().size() );
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.created:ge:2000-01-01&headless=true" ).content().size() );
        assertEquals( 0,
            GET( "/users/gist?filter=userCredentials.created:ge:2525-01-01&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_Like()
    {
        assertEquals( 1, GET( "/users/gist?filter=surname:like:mi&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:ilike:mi&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:like:?dmin&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:like:ad*&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:like:Zulu&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_NotLike()
    {
        assertEquals( 1,
            GET( "/users/gist?filter=userCredentials.username:!like:mike&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:!ilike:?min&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:!like:?min&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=surname:!like:ap*&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=surname:!like:ad?in&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_StartsWith()
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
    public void testFilter_NotStartsWith()
    {
        assertEquals( 1, GET( "/users/gist?filter=firstName:!$like:mike&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!$ilike:bat&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!startsWith:tic&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=firstName:!startsWith:ad&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_EndsWith()
    {
        assertEquals( 1, GET( "/users/gist?filter=firstName:like$:dmin&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:ilike$:in&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:endsWith:min&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=firstName:endsWith:bat&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_NotEndsWith()
    {
        assertEquals( 1, GET( "/users/gist?filter=firstName:!like$:mike&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!ilike$:bat&headless=true" ).content().size() );
        assertEquals( 1, GET( "/users/gist?filter=firstName:!endsWith:tic&headless=true" ).content().size() );
        assertEquals( 0, GET( "/users/gist?filter=firstName:!endsWith:min&headless=true" ).content().size() );
    }

    @Test
    public void testFilter_In()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String url = "/organisationUnits/{id}/dataSets/gist?filter=name:in:[plus3,plus5]&fields=name&order=name";
        JsonObject gist = GET( url, orgUnitId ).content();

        assertEquals( asList( "plus3", "plus5" ), gist.getArray( "dataSets" ).stringValues() );
    }

    @Test
    public void testFilter_NotIn()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "item" );

        assertEquals( asList( "item3", "item4" ),
            GET( "/dataSets/gist?fields=name&filter=name:in:[item3,item4]&headless=true&order=name", orgUnitId )
                .content().stringValues() );
    }

    @Test
    public void testFilter_In_Id()
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
    public void testFilter_In_1toMany()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "in" );

        String url = "/dataSets/gist?filter=organisationUnits.id:in:[{id}]&fields=name&filter=name:startsWith:in&headless=true";
        assertEquals( 10, GET( url, orgUnitId ).content().size() );
    }

    @Test
    public void testFilter_NotIn_1toMany()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "notIn" );

        String url = "/dataSets/gist?filter=organisationUnits.id:!in:[{id}]&fields=name&filter=name:startsWith:notIn&headless=true";
        assertEquals( 10, GET( url, "fakeUid" ).content().size() );
    }

    @Test
    public void testFilter_Empty_1toMany()
    {
        createDataSetsForOrganisationUnit( 7, orgUnitId, "empty" );
        String url = "/dataSets/gist?filter=sections:empty&fields=name&filter=name:startsWith:empty&headless=true";
        assertEquals( 7, GET( url ).content().size() );
    }

    @Test
    public void testFilter_NotEmpty_1toMany()
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
    public void testFilter_In_1toMany_ShortSyntax()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String url = "/dataSets/gist?filter=organisationUnits:in:[{id}]&fields=name&filter=name:startsWith:plus&headless=true";
        assertEquals( 10, GET( url, orgUnitId ).content().size() );
    }

    @Test
    public void testFilter_Like_1toMany()
    {
        createDataSetsForOrganisationUnit( 10, orgUnitId, "plus" );

        String orgUnitName = GET( "/organisationUnits/{id}/name/gist", orgUnitId ).content().string();
        String url = "/dataSets/gist?filter=organisationUnits.name:like:{name}&fields=name&filter=name:startsWith:plus&headless=true";
        assertEquals( 10, GET( url, orgUnitName ).content().size() );
    }

    @Test
    public void testFilter_Eq_1to1_1toMany()
    {
        // filter asks: does the user's userCredential have a user role which
        // name is equal to "Superuser"
        assertEquals( getSuperuserUid(),
            GET( "/users/{id}/gist?fields=id&filter=userCredentials.userRoles.name:eq:Superuser",
                getSuperuserUid() ).content().string() );
    }

    @Test
    public void testFilter_Gt_1toMany()
    {
        String fields = "id,userCredentials.username,userCredentials.twoFA";
        String filter = "userCredentials.created:gt:2021-01-01,userGroups:gt:0";
        JsonObject users = GET( "/userGroups/{uid}/users/gist?fields={fields}&filter={filter}&headless=true",
            userGroupId, fields, filter ).content().getObject( 0 );

        assertTrue( users.has( "id", "userCredentials" ) );
        assertTrue( users.getObject( "userCredentials" ).has( "username", "twoFA" ) );
    }
}
