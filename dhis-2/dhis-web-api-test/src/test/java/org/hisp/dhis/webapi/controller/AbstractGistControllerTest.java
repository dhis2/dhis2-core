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

import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for controller tests of the Gist API.
 *
 * @author Jan Bernitt
 */
abstract class AbstractGistControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    protected OrganisationUnitService organisationUnitService;

    protected String userGroupId;

    protected String orgUnitId;

    protected String dataSetId;

    protected User userA;

    @BeforeEach
    void setUp()
    {
        userA = createUserWithAuth( "userA", "ALL" );

        switchContextToUser( userA );

        userGroupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'groupX', 'users':[{'id':'" + getSuperuserUid() + "'}]}" ) );
        assertStatus( HttpStatus.OK, PATCH( "/users/{id}?importReportMode=ERRORS", getSuperuserUid(),
            Body( "[{'op': 'add', 'path': '/birthday', 'value': '1980-12-12'}]" ) ) );
        orgUnitId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'unitA', 'shortName':'unitA', 'openingDate':'2021-01-01'}" ) );
        dataSetId = assertStatus( HttpStatus.CREATED, POST( "/dataSets/",
            "{'name':'set1', 'organisationUnits': [{'id':'" + orgUnitId + "'}], 'periodType':'Daily'}" ) );
    }

    protected final void createDataSetsForOrganisationUnit( int count, String organisationUnitId, String namePrefix )
    {
        for ( int i = 0; i < count; i++ )
        {
            assertStatus( HttpStatus.CREATED, POST( "/dataSets/", "{'name':'" + namePrefix + i
                + "', 'organisationUnits': [{'id':'" + organisationUnitId + "'}], 'periodType':'Daily'}" ) );
        }
    }

    protected final void createDataSetsForOrganisationUnit( String organisationUnitId, String... names )
    {
        for ( String name : names )
        {
            assertStatus( HttpStatus.CREATED, POST( "/dataSets/", "{'name':'" + name
                + "', 'organisationUnits': [{'id':'" + organisationUnitId + "'}], 'periodType':'Daily'}" ) );
        }
    }

    static void assertHasPager( JsonObject response, int page, int pageSize )
    {
        assertHasPager( response, page, pageSize, null );
    }

    static void assertHasPager( JsonObject response, int page, int pageSize, Integer total )
    {
        JsonObject pager = response.getObject( "pager" );
        assertTrue( pager.exists(), "Pager is missing" );
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
    protected final void switchToGuestUser()
    {
        switchToNewUser( "guest", "Test_skipSharingCheck" );
    }
}
