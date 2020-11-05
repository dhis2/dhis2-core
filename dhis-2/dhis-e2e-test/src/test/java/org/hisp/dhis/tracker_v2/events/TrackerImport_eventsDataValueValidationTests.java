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

package org.hisp.dhis.tracker_v2.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.metadata.SharingActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.utils.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImport_eventsDataValueValidationTests
    extends ApiTest
{
    private TrackerActions trackerActions;

    private ProgramActions programActions;

    private SharingActions sharingActions;

    private RestApiActions dataElementActions;

    private EventActions eventActions;

    private static String OU_ID = Constants.ORG_UNIT_IDS[0];

    private String programId;

    private String programStageId;

    private String mandatoryDataElementId;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();
        programActions = new ProgramActions();
        sharingActions = new SharingActions();
        dataElementActions = new RestApiActions( "/dataElements" );
        eventActions = new EventActions();

        new LoginActions().loginAsSuperUser();

        setupData();
    }

    @Test
    public void shouldNotValidateDataValuesOnUpdateWithOnCompleteStrategy()
    {
        setValidationStrategy( programStageId, "ON_COMPLETE" );

        JsonObject events = trackerActions.createEventsBody( OU_ID, programId, programStageId );

        ApiResponse response = trackerActions.postAndGetJobReport( events );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "stats.created", equalTo( 1 ) )
            .body( "bundleReport.typeReportMap.EVENT", notNullValue() )
            .rootPath( "bundleReport.typeReportMap.EVENT" )
            .body( "stats.created", Matchers.equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports[0].errorReports", empty() );
    }

    @ParameterizedTest
    @CsvSource(
        {"ON_COMPLETE,COMPLETED", "ON_UPDATE_AND_INSERT,ACTIVE"}
    )
    public void shouldValidateWhenNoDataValue(String programStageStatus, String eventStatus)
    {
        setValidationStrategy( programStageId, programStageStatus );

        JsonObject event = createEventBodyWithStatus( eventStatus );

        ApiResponse response = trackerActions.postAndGetJobReport( event );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .body( "stats.ignored", equalTo( 1 ) )
            .body( "stats.imported", equalTo( 0 ) )
            .body( "bundleReport.typeReportMap.EVENT", notNullValue() )
            .rootPath( "bundleReport.typeReportMap.EVENT" )
            .body( "stats.created", Matchers.equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports[0].errorReports", not( empty() ) )
            .body( "objectReports[0].errorReports.message", equalTo( "value_required_but_not_provided" ) );
    }


    @Test
    public void shouldImportEventsWithCompulsoryDataValues()
    {
        JsonObject events = trackerActions.createEventsBody( OU_ID, programId, programStageId );

        addDataValue( events.getAsJsonArray( "events" ).get(0).getAsJsonObject(), mandatoryDataElementId, "TEXT VALUE" );

        ApiResponse response = trackerActions.postAndGetJobReport( events );

        response.validate().statusCode( 200 )
            .body( "status", equalTo("OK") )
            .body( "stats.created", equalTo( 1 ) )
            .body( "bundleReport.typeReportMap.EVENT", notNullValue() )
            .rootPath( "bundleReport.typeReportMap.EVENT" )
            .body( "stats.created", Matchers.equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports[0].errorReports", empty() );


        String eventId = response.extractString( "bundleReport.typeReportMap.EVENT.objectReports.uid[0]" );
        assertNotNull( eventId, "Failed to extract eventId" );

        eventActions.get( eventId )
            .validate()
            .statusCode( 200 )
            .body( "dataValues", not( Matchers.emptyArray() ) );

    }


    private JsonObject createEventBodyWithStatus( String status )
    {
        JsonObject body = trackerActions.createEventsBody(OU_ID, programId , programStageId);

        body.getAsJsonArray( "events" ).get( 0 ).getAsJsonObject().addProperty( "status", status );
        return body;
    }


    private void setupData()
    {
        ApiResponse response = programActions.createEventProgram( OU_ID );
        programId = response.extractUid();
        assertNotNull( programId, "Failed to create a program" );

        sharingActions.setupSharingForConfiguredUserGroup( "program", programId );

        programStageId = programActions.get( programId, new QueryParamsBuilder().add( "fields=*" ) )
            .extractString( "programStages.id[0]" );

        assertNotNull( programStageId, "Failed to create a programStage" );

        String dataElementId = dataElementActions
            .get( "?fields=id&filter=domainType:eq:TRACKER&filter=valueType:eq:TEXT&pageSize=1" )
            .extractString( "dataElements.id[0]" );

        assertNotNull( dataElementId, "Failed to find data elements" );
        mandatoryDataElementId = dataElementId;

        programActions.addDataElement( programStageId, dataElementId, true ).validate().statusCode( 200 );

    }

    private void addDataValue( JsonObject body, String dataElementId, String value )
    {
        JsonArray dataValues = new JsonArray();

        JsonObject dataValue = new JsonObject();

        dataValue.addProperty( "dataElement", dataElementId );
        dataValue.addProperty( "value", value );

        dataValues.add( dataValue );
        body.add( "dataValues", dataValues );
    }

    private void setValidationStrategy( String programStageId, String strategy )
    {
        JsonObject body = JsonObjectBuilder.jsonObject()
            .addProperty( "validationStrategy", strategy )
            .build();

        programActions.programStageActions.patch( programStageId, body )
            .validate().statusCode( 204 );

        programActions.programStageActions.get( programStageId )
            .validate().body( "validationStrategy", equalTo( strategy ) );
    }
}
