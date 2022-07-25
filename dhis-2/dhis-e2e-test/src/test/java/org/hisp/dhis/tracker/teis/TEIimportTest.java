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
package org.hisp.dhis.tracker.teis;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import java.io.File;

import org.hamcrest.Matchers;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TEIimportTest
    extends TrackerApiTest
{
    JsonObject object;

    @BeforeAll
    public void before()
        throws Exception
    {
        loginActions.loginAsSuperUser();

        object = new FileReaderUtils()
            .read( new File( "src/test/resources/tracker/teis/teisWithEventsAndEnrollments.json" ) )
            .get( JsonObject.class );
        teiActions.post( object ).validate().statusCode( 200 );
    }

    @Test
    public void teisShouldBeUpdatedAndDeletedInBulk()
    {
        // arrange

        JsonArray teis = object.getAsJsonArray( "trackedEntityInstances" );

        JsonObject tei1event = teis.get( 0 ).getAsJsonObject()
            .getAsJsonArray( "enrollments" ).get( 0 ).getAsJsonObject()
            .getAsJsonArray( "events" )
            .get( 0 )
            .getAsJsonObject();

        JsonObject tei2enrollment = teis.get( 1 ).getAsJsonObject()
            .getAsJsonArray( "enrollments" ).get( 0 ).getAsJsonObject();

        tei1event.addProperty( "deleted", true );
        tei2enrollment.addProperty( "status", "COMPLETED" );

        // act
        ApiResponse response = teiActions.post( object, new QueryParamsBuilder().addAll( "strategy=SYNC" ) );

        // assert
        String eventId = response.validate()
            .statusCode( 200 )
            .body( "response", notNullValue() )
            .rootPath( "response" )
            .body( "updated", Matchers.greaterThanOrEqualTo( 2 ) )
            .appendRootPath( "importSummaries[0]" )
            .body( "importCount.updated", greaterThanOrEqualTo( 1 ) )
            .appendRootPath( "enrollments.importSummaries[0].events.importSummaries[0]" )
            .body(
                "status", Matchers.equalTo( "SUCCESS" ),
                "reference", notNullValue(),
                "importCount.deleted", Matchers.equalTo( 1 ),
                "description", Matchers.stringContainsInOrder( "Deletion of event", "was successful" ) )
            .extract()
            .path( "response.importSummaries[0].enrollments.importSummaries[0].events.importSummaries[0].reference" );

        String enrollmentId = response.validate()
            .rootPath( "response.importSummaries[1].enrollments.importSummaries[0]" )
            .body(
                "status", Matchers.equalTo( "SUCCESS" ),
                "reference", notNullValue(),
                "importCount.updated", Matchers.equalTo( 1 ) )
            .extract().path( "response.importSummaries[1].enrollments.importSummaries[0].reference" );

        // check if updates on event and enrollment were done.

        response = enrollmentActions.get( enrollmentId );

        response.validate().statusCode( 200 )
            .body( "status", Matchers.equalTo( "COMPLETED" ) );

        response = eventActions.get( eventId );

        response.validate().statusCode( 404 );

    }
}
