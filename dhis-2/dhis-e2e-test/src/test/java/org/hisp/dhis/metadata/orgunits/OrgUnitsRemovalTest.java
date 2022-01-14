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
package org.hisp.dhis.metadata.orgunits;

import static org.hamcrest.CoreMatchers.*;

import java.io.File;

import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitsRemovalTest
    extends ApiTest
{
    private OrgUnitActions orgUnitActions;

    private MetadataActions metadataActions;

    private RestApiActions orgUnitGroupActions;

    private RestApiActions orgUnitSetActions;

    private String parentId = "PIBHO8qBw9o";

    private String groupId = "IViMsXfUyWn";

    private String setId = "XcKGktFuGFj";

    @BeforeEach
    public void beforeAll()
    {
        orgUnitActions = new OrgUnitActions();
        orgUnitGroupActions = new RestApiActions( "/organisationUnitGroups" );
        orgUnitSetActions = new RestApiActions( "/organisationUnitGroupSets" );
        metadataActions = new MetadataActions();

        new LoginActions().loginAsSuperUser();

        metadataActions
            .importAndValidateMetadata( new File( "src/test/resources/metadata/orgunits/ou_with_group_and_set.json" ) );

    }

    @Test
    public void shouldRemoveGroupReferenceWhenGroupIsDeleted()
    {
        orgUnitGroupActions.delete( groupId )
            .validate()
            .statusCode( 200 );

        orgUnitActions.get( parentId )
            .validate()
            .statusCode( 200 )
            .body( "organisationUnitGroups.id", is( Matchers.empty() ) );
    }

    @Test
    public void shouldRemoveSetReferenceWhenSetIsDeleted()
    {
        orgUnitSetActions.delete( setId )
            .validate()
            .statusCode( 200 );

        orgUnitActions.get( parentId )
            .validate()
            .statusCode( 200 )
            .body( "organisationUnitGroups.id", Matchers.contains( groupId ) );

        orgUnitGroupActions.get( groupId )
            .validate()
            .statusCode( 200 )
            .body( "groupSets.id", is( Matchers.empty() ) );
    }

    @Test
    public void shouldNotRemoveParentOrgUnit()
    {
        orgUnitActions.delete( parentId )
            .validate()
            .statusCode( 409 )
            .body( "message",
                containsStringIgnoringCase(
                    "Object could not be deleted because it is associated with another object: OrganisationUnit" ) )
            .body( "errorCode", equalTo( "E4030" ) );

    }

}
