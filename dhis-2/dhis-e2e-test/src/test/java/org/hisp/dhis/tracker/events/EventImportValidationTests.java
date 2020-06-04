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

package org.hisp.dhis.tracker.events;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportValidationTests
    extends ApiTest
{
    private EventActions eventActions;
    private ProgramActions programActions;

    private String programId;
    private String programStageId;

    @BeforeAll
    public void beforeAll()
    {
        eventActions = new EventActions();
        programActions = new ProgramActions();

        new LoginActions().loginAsAdmin();

        setupData();
    }

    @Test
    public void eventImportShouldValidateOu()
    {
        JsonObject jsonObject = eventActions.createEventBody( null, programId, programStageId );

        eventActions.post( jsonObject )
            .validate().statusCode( 409 )
            .body( "status", equalTo("ERROR") )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "importSummaries.description[0]", containsString( "Event.orgUnit does not point to a valid organisation unit" ) );

    }

    private void setupData()
    {
        programId = programActions
            .get( "", new QueryParamsBuilder().addAll( "filter=programType:eq:WITHOUT_REGISTRATION", "filter=name:$like:TA", "pageSize=1" ) )
            .extractString("programs.id[0]");

        assertNotNull( programId, "Failed to find a suitable program");

        programStageId = programActions.programStageActions.get( "", new QueryParamsBuilder().add( "filter=program.id:eq:" + programId ))
            .extractString("programStages.id[0]");

        assertNotNull( programStageId, "Failed to find a program stage" );

    }
}
