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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;

import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.tracker.RelationshipActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicatesApiTest;
import org.hisp.dhis.tracker.importer.databuilder.RelationshipDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesRelationshipTests
    extends PotentialDuplicatesApiTest
{
    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsAdmin();
    }

    @Test
    public void shouldAutoMergeRelationshipsWithNonSuperUser()
    {
        // arrange
        String teiA = createTei();
        String teiB = createTei();
        String teiC = createTei();

        createUniDirectionalRelationship( teiB, teiC ).validateSuccessfulImport();
        String relationship2 = createUniDirectionalRelationship( teiA, teiC ).extractImportedRelationships().get( 0 );
        String relationship3 = createUniDirectionalRelationship( teiC, teiB ).extractImportedRelationships().get( 0 );
        createUniDirectionalRelationship( teiA, teiB ).validateSuccessfulImport();

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        // act
        String username = createUserWithAccessToMerge();
        new LoginActions().loginAsUser( username, Constants.USER_PASSWORD );

        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate )
            .validate().statusCode( 200 );

        // assert
        trackerActions.getTrackedEntity( teiA + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "relationships", hasSize( 2 ) )
            .body( "relationships.relationship", hasItems( relationship3, relationship2 ) );
    }

    @Test
    public void shouldManuallyMergeRelationship()
    {
        String teiA = createTei();
        String teiB = createTei();
        String teiC = createTei();

        String relationship = createRelationship( teiB, teiC ).extractImportedRelationships().get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        potentialDuplicatesActions.manualMergePotentialDuplicate( potentialDuplicate,
            new JsonObjectBuilder().addArray( "relationships", Arrays.asList( relationship ) ).build() )
            .validate().statusCode( 200 );

        trackerActions.getTrackedEntity( teiA + "?fields=*" )
            .validate()
            .statusCode( 200 )
            .body( "relationships", hasSize( 1 ) )
            .body( "relationships.relationship", hasItems( relationship ) );
    }

    @Test
    public void shouldRemoveDuplicateRelationshipWhenAutoMerging()
    {
        String teiA = createTei();
        String teiB = createTei();

        String relationship = createRelationship( teiA, teiB ).extractImportedRelationships().get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate ).validate().statusCode( 200 );

        trackerActions.getTrackedEntity( teiA + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "relationships", hasSize( 0 ) );

        new RelationshipActions().get( relationship ).validateStatus( 404 );
    }

    @Test
    public void shouldNotMergeManuallyWhenThereAreDuplicateRelationships()
    {
        String teiA = createTei();
        String teiB = createTei();

        String relationship = createRelationship( teiA, teiB ).extractImportedRelationships().get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        potentialDuplicatesActions.manualMergePotentialDuplicate( potentialDuplicate,
            new JsonObjectBuilder().addArray( "relationships", Arrays.asList( relationship ) ).build() )
            .validate().statusCode( 409 )
            .body( "message", containsString( "A similar relationship already exists on original" ) );
    }

    private TrackerApiResponse createRelationship( String teiA, String teiB )
    {
        JsonObject payload = new RelationshipDataBuilder()
            .buildBidirectionalRelationship( teiA, teiB )
            .array();

        return trackerActions.postAndGetJobReport( payload )
            .validateSuccessfulImport();
    }

    private TrackerApiResponse createUniDirectionalRelationship( String teiA, String teiB )
    {
        JsonObject payload = new RelationshipDataBuilder()
            .buildUniDirectionalRelationship( teiA, teiB )
            .array();

        return trackerActions.postAndGetJobReport( payload )
            .validateSuccessfulImport();
    }
}
