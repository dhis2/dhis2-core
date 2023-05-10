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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.hisp.dhis.Constants;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicatesApiTestDeprecated;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesEnrollmentsTests
    extends PotentialDuplicatesApiTestDeprecated
{
    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsAdmin();
    }

    @Test
    public void shouldNotBeMergedWhenBothTeisEnrolledInSameProgram()
    {
        String teiA = createTeiWithEnrollmentsAndEvents( Constants.TRACKER_PROGRAM_ID, "nlXNK4b7LVr" )
            .extractImportedTeis()
            .get( 0 );
        String teiB = createTeiWithEnrollmentsAndEvents( Constants.TRACKER_PROGRAM_ID, "nlXNK4b7LVr" )
            .extractImportedTeis()
            .get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.postPotentialDuplicate( teiA, teiB, "OPEN" )
            .validateStatus( 200 )
            .extractString( "id" );

        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate )
            .validate().statusCode( 409 )
            .body( "message", containsString( "Both entities enrolled in the same program" ) );
    }

    @Test
    public void shouldBeManuallyMerged()
    {
        String teiA = createTeiWithEnrollmentsAndEvents( TRACKER_PROGRAM_ID, TRACKER_PROGRAM_STAGE_ID )
            .extractImportedTeis()
            .get( 0 );

        TrackerApiResponse teiBResponse = createTeiWithEnrollmentsAndEvents( Constants.ANOTHER_TRACKER_PROGRAM_ID,
            "PaOOjwLVW2X" );
        String teiB = teiBResponse.extractImportedTeis().get( 0 );
        String enrollmentToMerge = teiBResponse.extractImportedEnrollments().get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.postPotentialDuplicate( teiA, teiB, "OPEN" )
            .validateStatus( 200 )
            .extractString( "id" );

        potentialDuplicatesActions
            .manualMergePotentialDuplicate( potentialDuplicate, new JsonObjectBuilder().addArray( "enrollments",
                Arrays.asList( enrollmentToMerge ) ).build() )
            .validate().statusCode( 200 );

        trackerActions.getTrackedEntity( teiA + "?fields=enrollments" )
            .validate().statusCode( 200 )
            .body( "enrollments", hasSize( 2 ) );
    }

    @Test
    public void shouldAutoMergeWithEnrollmentsAndEvents()
        throws IOException
    {
        // arrange
        TrackerApiResponse originalTeiResponse = createTeiWithEnrollmentsAndEvents(
            Constants.ANOTHER_TRACKER_PROGRAM_ID,
            "PaOOjwLVW2X" );
        String teiA = originalTeiResponse.extractImportedTeis().get( 0 );
        String enrollmentA = originalTeiResponse.extractImportedEnrollments().get( 0 );

        TrackerApiResponse duplicateTeiResponse = trackerActions.postAndGetJobReport(
            new JsonFileReader(
                new File( "src/test/resources/tracker/importer/teis/teiWithEnrollmentAndEventsNested.json" ) )
                    .get() );

        String teiB = duplicateTeiResponse.extractImportedTeis().get( 0 );
        String enrollmentB = duplicateTeiResponse.extractImportedEnrollments().get( 0 );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        // act
        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate )
            .validate().statusCode( 200 );

        // assert
        potentialDuplicatesActions.get( potentialDuplicate )
            .validate()
            .statusCode( 200 )
            .body( "status", equalTo( "MERGED" ) );

        trackerActions.getTrackedEntity( teiA + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "enrollments", hasSize( 2 ) )
            .body( "enrollments.enrollment", hasItems( enrollmentA, enrollmentB ) )
            .rootPath( String.format( "enrollments.find{it.enrollment=='%s'}", enrollmentA ) )
            .body( "events", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "events.dataValues", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .noRootPath().rootPath( String.format( "enrollments.find{it.enrollment=='%s'}", enrollmentB ) )
            .body( "events", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "events.dataValues", hasSize( greaterThanOrEqualTo( 1 ) ) );

        trackerActions.getTrackedEntity( teiB )
            .validate().statusCode( 404 );
    }

    @Test
    public void shouldMergeWithNonSuperUser()
    {
        // arrange
        String teiB = createTeiWithoutEnrollment( Constants.ORG_UNIT_IDS[0] );

        TrackerApiResponse imported = trackerActions.postAndGetJobReport( new TeiDataBuilder()
            .buildWithEnrollmentAndEvent( Constants.TRACKED_ENTITY_TYPE, Constants.ORG_UNIT_IDS[0], TRACKER_PROGRAM_ID,
                TRACKER_PROGRAM_STAGE_ID ) )
            .validateSuccessfulImport();

        String teiA = imported.extractImportedTeis().get( 0 );
        String enrollment = imported.extractImportedEnrollments().get( 0 );
        assertThat( enrollment, notNullValue() );

        String potentialDuplicate = potentialDuplicatesActions.createAndValidatePotentialDuplicate( teiA, teiB,
            "OPEN" );

        String username = createUserWithAccessToMerge();
        loginActions.loginAsUser( username, USER_PASSWORD );

        // act
        potentialDuplicatesActions.autoMergePotentialDuplicate( potentialDuplicate ).validate().statusCode( 200 );

        // assert
        potentialDuplicatesActions.get( potentialDuplicate )
            .validate()
            .statusCode( 200 )
            .body( "status", equalTo( "MERGED" ) );

        trackerActions.getTrackedEntity( teiA + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "enrollments", hasSize( 1 ) )
            .body( "enrollments.enrollment", hasItems( enrollment ) )
            .body( String.format( "enrollments.find{it.enrollment=='%s'}.events", enrollment ), hasSize( 1 ) );

        trackerActions.getTrackedEntity( teiB ).validate().statusCode( 404 );
    }

    private String createTeiWithoutEnrollment( String ouId )
    {
        JsonObject object = new TeiDataBuilder().array( Constants.TRACKED_ENTITY_TYPE, ouId );

        return trackerActions.postAndGetJobReport( object ).extractImportedTeis().get( 0 );
    }
}
