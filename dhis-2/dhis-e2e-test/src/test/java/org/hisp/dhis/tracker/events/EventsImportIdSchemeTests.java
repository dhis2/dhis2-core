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
import com.sun.tools.jxc.ap.Const;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventsImportIdSchemeTests extends ApiTest
{
    private OrgUnitActions orgUnitActions;

    private ProgramActions programActions;

    private EventActions eventActions;

    private static String orgUnitName = "TA EventsImportIdSchemeTests ou name";

    private static String orgUnitCode = "TA EventsImportIdSchemeTests ou code";

    private String attributeValue = "TA EventsImportIdSchemeTests attribute";

    private String orgUnitId;

    private static String attributeId;

    private String programId = Constants.EVENT_PROGRAM_ID;


    @BeforeAll
    public void beforeAll() {
        orgUnitActions = new OrgUnitActions();
        eventActions = new EventActions();
        programActions = new ProgramActions();

        new LoginActions().loginAsSuperUser();

        setupData();
    }

    private static Stream<Arguments> provideOrgUnitIdSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" ),
            Arguments.arguments( "ATTRIBUTE:" + attributeId, "attributeValues.value[0]" )
        );
    }

    @ParameterizedTest
    @MethodSource( "provideOrgUnitIdSchemeArguments" )
    public void eventsShouldBeImportedWithOrgUnitScheme(String ouScheme, String ouProperty)
        throws Exception
    {
        String ouPropertyValue = orgUnitActions.get( orgUnitId ).extractString( ouProperty );

        assertNotNull(ouPropertyValue, String.format(  "Org unit progperty %s was not present.", ouProperty));

        JsonObject object = new FileReaderUtils().read(  new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWith( "orgUnit", ouPropertyValue)
            .replacePropertyValuesWithIds( "event" )
            .get( JsonObject.class );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "skipCache=true" )
            .add( "orgUnitIdScheme=" + ouScheme );

        ApiResponse response = eventActions.post( object, queryParamsBuilder );

        response.validate().statusCode( 200 )
            .rootPath( "response" )
            .body( "status",  equalTo("SUCCESS") )
            .body( "ignored", equalTo( 0 ) )
            .body( "imported", greaterThan(1) )
            .body( "importSummaries.reference", everyItem( notNullValue() ) );

        String eventId = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventId );

        eventActions.get( eventId ).validate()
            .statusCode( 200 )
            .body( "orgUnit", equalTo( orgUnitId ) );
    }

    private void setupData() {

        attributeId = new AttributeActions().createUniqueAttribute( "organisationUnit", "TEXT" );

        assertNotNull( attributeId, "Failed to setup attribute" );
        OrgUnit orgUnit = orgUnitActions.generateDummy();

        orgUnit.setCode( orgUnitCode );
        orgUnit.setName( orgUnitName );

        orgUnitId = orgUnitActions.create( orgUnit );
        assertNotNull( orgUnitId, "Failed to setup org unit" );

        programActions.addOrganisationUnits( programId, orgUnitId ).validate().statusCode( 200 );

        orgUnitActions.addAttributeValue( orgUnitId, attributeId, attributeValue );
    }
}
