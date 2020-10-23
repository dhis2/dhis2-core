/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.tracker_v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImportTests
    extends ApiTest
{
    private TrackerActions trackerActions;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();
    }

    @Test
    public void shouldReturnErrorReportsWhenTeiIncorrect()
    {
        JsonObject trackedEntity = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", "" )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .build();

        JsonObject trackedEntities = createTeiBody( trackedEntity );

        ApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response.validate()
            .statusCode( 200 )
            .body( "status", equalTo( "ERROR" ) )
            .body( "trackerValidationReport.errorReports", notNullValue() )
            .rootPath( "trackerValidationReport.errorReports[0]" )
            .body( "message", containsStringIgnoringCase( "Could not find TrackedEntityType" ) )
            .noRootPath()
            .body( "stats.ignored", equalTo( 1 ) )
            .body( "stats.total", equalTo( 1 ) );
    }

    @Test
    public void shouldImportTei()
    {
        JsonObject trackedEntity = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", "Q9GufDoplCL" )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .build();

        JsonObject trackedEntities = createTeiBody( trackedEntity );

        ApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response.validate()
            .statusCode( 200 )
            .body( "stats.created", equalTo( 1 ) );
    }

    @Test
    public void shouldImportTeiWithAttributes()
    {
        ApiResponse response = trackerActions.postAndGetJobReport( new File( "src/test/resources/tracker_v2/teis/teis.json" ) );

        response.validate()
            .statusCode( 200 );
    }

    private JsonObject createTeiBody( JsonObject innerTei )
    {
        JsonObject trackedEntities = new JsonObject();
        JsonArray tea = new JsonArray();

        tea.add( innerTei );

        trackedEntities.add( "trackedEntities", tea );

        return trackedEntities;
    }
}
