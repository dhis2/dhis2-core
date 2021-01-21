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

package org.hisp.dhis.tracker.importer.enrollments;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EnrollmentsTests
    extends
    ApiTest
{
    private TrackerActions trackerActions;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();
    }

    @ParameterizedTest
    @ValueSource( strings = {
        "src/test/resources/tracker/importer/teis/teiWithEnrollments.json",
        "src/test/resources/tracker/importer/teis/teiAndEnrollment.json"
    } )
    public void shouldImportTeisWithEnrollments( String file )
    {
        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( new File( file ) );

        response.validateSuccessfulImport()
            .validate()
            .body( "status", Matchers.equalTo( "OK" ) )
            .body( "stats.created", equalTo( 2 ) );

        response
            .validateTeis()
            .body( "stats.created", Matchers.equalTo( 1 ) );

        response.validateEnrollments()
            .body( "stats.created", Matchers.equalTo( 1 ) );

        // assert that the tei was imported
        String teiId = response.extractImportedTeis().get( 0 );
        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        trackerActions.get( "/enrollments/" + enrollmentId )
            .validate()
            .statusCode( 200 )
            .body( "trackedEntity", equalTo( teiId ) );
    }

    @Test
    public void shouldImportEnrollmentToExistingTei()
        throws Exception
    {
        JsonObject teiPayload = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/teis/tei.json" ) )
            .get( JsonObject.class );

        String teiId = trackerActions.postAndGetJobReport( teiPayload ).validateSuccessfulImport().extractImportedTeis().get( 0 );

        JsonObject enrollmentPayload = new FileReaderUtils()
            .read( new File( "src/test/resources/tracker/importer/enrollments/enrollment.json" ) )
            .replacePropertyValuesWith( "trackedEntity", teiId )
            .get( JsonObject.class );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( enrollmentPayload );

        response.validateSuccessfulImport()
            .validateEnrollments()
            .body( "stats.created", equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports.uid", notNullValue() );

        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        ApiResponse enrollmentResponse = trackerActions.get( "/enrollments/" + enrollmentId );

        assertThat( enrollmentResponse.getBody(),
            matchesJSON( enrollmentPayload.get( "enrollments" ).getAsJsonArray().get( 0 ).getAsJsonObject() ) );
    }
}
