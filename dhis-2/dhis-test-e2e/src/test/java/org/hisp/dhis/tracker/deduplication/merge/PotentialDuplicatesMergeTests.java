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
package org.hisp.dhis.tracker.deduplication.merge;

import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.TrackedEntityTypeActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicatesApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesMergeTests
    extends PotentialDuplicatesApiTest
{
    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsSuperUser();
    }

    @Test
    public void shouldUpdateLastUpdatedInfo()
    {
        String teiA = createTei( Constants.TRACKED_ENTITY_TYPE );
        String teiB = createTeiWithEnrollmentsAndEvents().extractImportedTeis().get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        String admin_username = "taadmin";
        loginActions.loginAsUser( admin_username, USER_PASSWORD );

        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate );

        potentialDuplicatesActions.get( potentialDuplicate )
            .validate()
            .statusCode( 200 )
            .body( "status", equalTo( "MERGED" ) )
            .body( "lastUpdatedByUserName", equalTo( admin_username ) );

        trackerActions.getTrackedEntity( teiA + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "createdBy.username", equalTo( "tasuperadmin" ) )
            .body( "updatedBy.username", equalTo( admin_username ) )
            .body( "enrollments.updatedBy.username", everyItem( equalTo( admin_username ) ) );
    }

    @ValueSource( strings = { "enrollments", "relationships", "trackedEntityAttributes" } )
    @ParameterizedTest
    public void shouldCheckReferences( String property )
    {
        String id = "nlXNK4b7LVr"; // id of a program. Valid, but there won't be
                                  // any enrollments, relationships or TEAs
                                  // with that id.
        String teiA = createTei( Constants.TRACKED_ENTITY_TYPE );
        String teiB = createTei( Constants.TRACKED_ENTITY_TYPE );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        potentialDuplicatesActions
            .manualMergePotentialDuplicate( potentialDuplicate, new JsonObjectBuilder().addArray( property,
                Arrays.asList( id ) ).build() )
            .validate().statusCode( 409 )
            .body( "message",
                both( containsString( "Merging conflict: Duplicate has no" ) ).and( containsString( id ) ) );
    }

    @Test
    public void shouldNotMergeDifferentTypeTeis()
    {
        String trackedEntityType = new TrackedEntityTypeActions().create();

        String teiA = createTei( Constants.TRACKED_ENTITY_TYPE );
        String teiB = createTei( trackedEntityType );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate )
            .validate().statusCode( 409 )
            .body( "message", Matchers.containsString( "Entities have different Tracked Entity Types" ) );
    }

}
