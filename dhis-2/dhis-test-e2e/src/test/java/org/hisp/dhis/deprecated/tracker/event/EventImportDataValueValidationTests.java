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
package org.hisp.dhis.deprecated.tracker.event;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.metadata.DataElementActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.deprecated.tracker.DeprecatedTrackerApiTest;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportDataValueValidationTests
    extends DeprecatedTrackerApiTest
{
    private static String OU_ID = Constants.ORG_UNIT_IDS[0];

    private DataElementActions dataElementActions;

    private String programId;

    private String programStageId;

    private String mandatoryDataElementId;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        dataElementActions = new DataElementActions();

        loginActions.loginAsAdmin();

        setupData();
    }

    @Test
    public void shouldNotValidateDataValuesOnUpdateWithOnCompleteStrategy()
    {
        programActions.programStageActions.setValidationStrategy( programStageId, "ON_COMPLETE" );

        JsonObject events = eventActions.createEventBody( OU_ID, programId, programStageId );

        ApiResponse response = eventActions.post( events, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "response.ignored", equalTo( 0 ) )
            .body( "response.imported", equalTo( 1 ) );
    }

    @Test
    public void shouldValidateDataValuesOnCompleteWhenEventIsCompleted()
    {
        programActions.programStageActions.setValidationStrategy( programStageId, "ON_COMPLETE" );

        JsonObject event = eventActions.createEventBody( OU_ID, programId, programStageId );
        event.addProperty( "status", "COMPLETED" );

        ApiResponse response = eventActions.post( event, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "imported", equalTo( 0 ) )
            .body( "importSummaries[0].conflicts[0].value", equalTo( "value_required_but_not_provided" ) );
    }

    @Test
    public void shouldValidateCompletedOnInsert()
    {
        programActions.programStageActions.setValidationStrategy( programStageId, "ON_UPDATE_AND_INSERT" );

        JsonObject event = eventActions.createEventBody( OU_ID, programId, programStageId );
        event.addProperty( "status", "COMPLETED" );

        ApiResponse response = eventActions.post( event, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "imported", equalTo( 0 ) )
            .body( "importSummaries[0].conflicts[0].value", equalTo( "value_required_but_not_provided" ) );
    }

    @Test
    public void shouldValidateDataValuesOnUpdate()
    {
        programActions.programStageActions.setValidationStrategy( programStageId, "ON_UPDATE_AND_INSERT" );

        JsonObject events = eventActions.createEventBody( OU_ID, programId, programStageId );

        ApiResponse response = eventActions.post( events, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "imported", equalTo( 0 ) )
            .body( "importSummaries[0].conflicts[0].value", equalTo( "value_required_but_not_provided" ) );
    }

    @Test
    public void shouldImportEventsWithCompulsoryDataValues()
    {
        JsonObject events = eventActions.createEventBody( OU_ID, programId, programStageId );

        addDataValue( events, mandatoryDataElementId, "TEXT VALUE" );

        ApiResponse response = eventActions.post( events );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "response.imported", equalTo( 1 ) );

        String eventID = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventID, "Failed to extract eventId" );

        eventActions.get( eventID )
            .validate()
            .statusCode( 200 )
            .body( "dataValues", not( Matchers.emptyArray() ) );
    }

    private void setupData()
        throws Exception
    {
        programId = new IdGenerator().generateUniqueId();
        programStageId = new IdGenerator().generateUniqueId();

        JsonObject jsonObject = new JsonObjectBuilder(
            new FileReaderUtils()
                .readJsonAndGenerateData( new File( "src/test/resources/tracker/eventProgram.json" ) ) )
                    .addPropertyByJsonPath( "programStages[0].program.id", programId )
                    .addPropertyByJsonPath( "programs[0].id", programId )
                    .addPropertyByJsonPath( "programs[0].programStages[0].id", programStageId )
                    .addPropertyByJsonPath( "programStages[0].id", programStageId )
                    .addPropertyByJsonPath( "programStages[0].programStageDataElements", null )
                    .build();

        new MetadataActions().importAndValidateMetadata( jsonObject );

        String dataElementId = dataElementActions
            .get( "?fields=id&filter=domainType:eq:TRACKER&filter=valueType:eq:TEXT&pageSize=1" )
            .extractString( "dataElements.id[0]" );

        assertNotNull( dataElementId, "Failed to create data elements" );
        mandatoryDataElementId = dataElementId;

        programActions.addDataElement( programStageId, dataElementId, true ).validate().statusCode( 200 );
    }

    private void addDataValue( JsonObject body, String dataElementId, String value )
    {
        JsonObjectBuilder.jsonObject( body )
            .addArray( "dataValues", new JsonObjectBuilder()
                .addProperty( "dataElement", dataElementId )
                .addProperty( "value", value )
                .build() );

    }
}
