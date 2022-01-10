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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitsParentAssignmentTests
    extends ApiTest
{
    private LoginActions loginActions;

    private OrgUnitActions orgUnitActions;

    @BeforeEach
    public void setUp()
    {
        loginActions = new LoginActions();
        orgUnitActions = new OrgUnitActions();

        loginActions.loginAsSuperUser();
    }

    @Test
    public void shouldAssignReferenceToBoth()
    {
        String orgUnitId = orgUnitActions.createOrgUnit();

        assertNotNull( orgUnitId, "Parent org unit wasn't created" );
        String childId = orgUnitActions.createOrgUnitWithParent( orgUnitId );

        assertNotNull( childId, "Child org unit wasn't created" );

        ApiResponse response = orgUnitActions.get( childId );
        response.validate()
            .statusCode( 200 )
            .body( "parent.id", Matchers.equalTo( orgUnitId ) );

        response = orgUnitActions.get( orgUnitId );
        response.validate()
            .statusCode( 200 )
            .body( "children", Matchers.not( Matchers.emptyArray() ) )
            .body( "children.id", Matchers.not( Matchers.emptyArray() ) )
            .body( "children.id[0]", Matchers.equalTo( childId ) );
    }

    @Test
    public void shouldAdjustTheOrgUnitTree()
    {
        String parentOrgUnitId = orgUnitActions.createOrgUnit( 1 );
        String intOrgUnit = orgUnitActions.createOrgUnitWithParent( parentOrgUnitId, 1 );
        String childOrgUnitId = orgUnitActions.createOrgUnitWithParent( intOrgUnit );

        orgUnitActions.get( intOrgUnit )
            .validate()
            .statusCode( 200 )
            .body( "level", equalTo( 2 ) );

        orgUnitActions.get( childOrgUnitId )
            .validate()
            .statusCode( 200 )
            .body( "level", equalTo( 3 ) );
    }
}
