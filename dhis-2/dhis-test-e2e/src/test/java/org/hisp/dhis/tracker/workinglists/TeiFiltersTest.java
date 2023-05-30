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
package org.hisp.dhis.tracker.workinglists;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import java.io.File;
import java.io.IOException;

import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TeiFiltersTest
    extends TrackerApiTest
{
    private RestApiActions workingListActions;

    private File workingListFile = new File( "src/test/resources/tracker/workinglists/trackedEntityFilters.json" );

    @BeforeAll
    public void beforeAll()
    {
        workingListActions = new RestApiActions( "/trackedEntityInstanceFilters" );

        new LoginActions().loginAsSuperUser();
    }

    @Test
    public void shouldImportTeiWorkingList()
        throws IOException
    {
        JsonObject payload = new JsonFileReader( workingListFile )
            .get();

        String uid = workingListActions.post( payload )
            .validateStatus( 201 )
            .extractUid();

        workingListActions.get( uid )
            .validate()
            .statusCode( 200 )
            .body( "", matchesJSON( payload ) );
    }

    @Test
    public void shouldValidateAssignedUsersMode()
        throws IOException
    {
        JsonObject payload = new JsonFileReader( workingListFile )
            .getAsObjectBuilder()
            .addPropertyByJsonPath( "entityQueryCriteria.assignedUserMode", "PROVIDED" )
            .build();

        workingListActions.post( payload )
            .validate()
            .statusCode( 409 )
            .body( "message",
                containsStringIgnoringCase( "Assigned Users cannot be empty with PROVIDED assigned user mode" ) );
    }

    @Test
    public void shouldValidateOrgUnitMode()
        throws IOException
    {
        JsonObject payload = new JsonFileReader( workingListFile )
            .getAsObjectBuilder()
            .addPropertyByJsonPath( "entityQueryCriteria.ouMode", "SELECTED" )
            .build();

        workingListActions.post( payload )
            .validate()
            .statusCode( 409 )
            .body( "message",
                containsStringIgnoringCase( "Organisation Unit cannot be empty with SELECTED org unit mode" ) );

    }

    @Test
    public void shouldValidateAttribute()
        throws IOException
    {
        JsonObject payload = new JsonFileReader( workingListFile )
            .replaceStringsWithIds( "dIVt4l5vIOa" )
            .get();

        workingListActions.post( payload )
            .validateStatus( 409 )
            .validate().body( "message", containsStringIgnoringCase( "no tracked entity attribute found" ) );
    }
}
