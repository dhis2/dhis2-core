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
package org.hisp.dhis.tracker.importer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Luca Cambi
 */
public class TrackerEnrollmentExportTests
    extends TrackerNtiApiTest
{
    private static String enrollmentId;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        loginActions.loginAsSuperUser();

        TrackerApiResponse response = trackerActions.postAndGetJobReport(
            new File( "src/test/resources/tracker/importer/teis/teisWithEnrollmentsAndEvents.json" ) );

        String teiId = response.validateSuccessfulImport()
            .extractImportedTeis()
            .get( 0 );
        enrollmentId = response.extractImportedEnrollments().get( 0 );
        importRelationshipEnrollmentToTei( enrollmentId, teiId ).validateSuccessfulImport();
    }

    private Stream<Arguments> shouldReturnRequestedEnrollmentFields()
    {
        return Stream.of(
            Arguments.of( "events, relationships, attributes", not( emptyIterable() ), not( emptyIterable() ),
                not( emptyIterable() ),
                Arguments.of( "events, !relationships, attributes", not( emptyIterable() ), emptyIterable(),
                    not( emptyIterable() ) ),
                Arguments.of( "events, relationships, !attributes", not( emptyIterable() ), not( emptyIterable() ),
                    emptyIterable() ),
                Arguments.of( "!events, relationships, attributes", emptyIterable(), not( emptyIterable() ),
                    not( emptyIterable() ) ),
                Arguments.of( "!events, !relationships, !attributes", emptyIterable(), emptyIterable(),
                    emptyIterable() ),
                Arguments.of( "!events, !relationships, attributes", emptyIterable(), emptyIterable(),
                    not( emptyIterable() ) ),
                Arguments.of( "events, !relationships, !attributes", not( emptyIterable() ), emptyIterable(),
                    emptyIterable() ),
                Arguments.of( "!events, relationships, !attributes", emptyIterable(), not( emptyIterable() ),
                    emptyIterable() ) ) );
    }

    @MethodSource
    @ParameterizedTest
    public void shouldReturnRequestedEnrollmentFields( String fields, Matcher<JsonArray> eventExists,
        Matcher<JsonArray> relationshipsExists, Matcher<JsonArray> attributesExists )
    {
        ApiResponse response = trackerActions.get( "/enrollments/" + enrollmentId + "?fields=" + fields );

        response.validate()
            .statusCode( 200 );

        JsonObject enrollment = response.getBody();

        assertThat( enrollment.getAsJsonArray( "events" ), eventExists );
        assertThat( enrollment.getAsJsonArray( "relationships" ), relationshipsExists );
        assertThat( enrollment.getAsJsonArray( "attributes" ), attributesExists );
    }
}
