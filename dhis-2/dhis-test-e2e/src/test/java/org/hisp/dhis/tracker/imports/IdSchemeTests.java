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
package org.hisp.dhis.tracker.imports;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.tracker.imports.databuilder.EventDataBuilder;
import org.hisp.dhis.tracker.imports.databuilder.RelationshipDataBuilder;
import org.hisp.dhis.tracker.imports.databuilder.TeiDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;
import io.restassured.path.json.JsonPath;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class IdSchemeTests
    extends TrackerApiTest
{
    private final static String METADATA_FILE_PATH = "src/test/resources/tracker/idSchemesMetadata.json";

    private final static String ATTRIBUTE_ID = "f3JrwRTeSSz";

    @BeforeAll
    public void beforeAll()
    {
        new MetadataActions()
            .importAndValidateMetadata( new File( METADATA_FILE_PATH ) );
    }

    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsAdmin();
    }

    private static Stream<Arguments> provideIdSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE" ),
            Arguments.arguments( "NAME" ),
            Arguments.arguments( "ATTRIBUTE:" + ATTRIBUTE_ID ) );
    }

    @MethodSource( "provideIdSchemeArguments" )
    @ParameterizedTest( name = "POST to /tracker?idScheme={0}" )
    public void shouldImportTrackerProgramDataByIdScheme( String idScheme )
    {
        String teiId = new IdGenerator().generateUniqueId();

        TestData data = new TestData( idScheme );

        EventDataBuilder eventDataBuilder = new EventDataBuilder()
            .setOu( data.getOrgUnit() )
            .setProgramStage( data.getTrackerProgramStage() )
            .addDataValue( data.getDataElement(), DataGenerator.randomString() )
            .setAssignedUser( Constants.SUPER_USER_ID )
            .setProgram( data.getTrackerProgram() );

        EnrollmentDataBuilder enrollmentDataBuilder = new EnrollmentDataBuilder()
            .setOu( data.getOrgUnit() )
            .setProgram( data.getTrackerProgram() )
            .addEvent( eventDataBuilder );

        JsonObject payload = new TeiDataBuilder()
            .setId( teiId )
            .setTeiType( data.getTrackedEntityType() )
            .setOu( data.getOrgUnit() )
            .addAttribute( data.getTrackedEntityAttribute(), DataGenerator.randomString() )
            .addEnrollment( enrollmentDataBuilder )
            .addRelationship( new RelationshipDataBuilder()
                .setRelationshipType( data.getRelationshipType() )
                .setFromTrackedEntity( this.createTei() )
                .setToTrackedEntity( teiId ) )
            .array();

        trackerImportExportActions.postAndGetJobReport( payload,
            new QueryParamsBuilder()
                .addAll( "async=false", "idScheme=" + idScheme ) )
            .validateSuccessfulImport();
    }

    @MethodSource( "provideIdSchemeArguments" )
    @ParameterizedTest( name = "POST to /tracker?idScheme={0}" )
    public void shouldImportEventProgramDataByIdScheme( String idScheme )
    {
        TestData data = new TestData( idScheme );

        JsonObject object = new EventDataBuilder()
            .setOu( data.getOrgUnit() )
            .setProgramStage( data.getEventProgramStage() )
            .setProgram( data.getEventProgram() )
            .setAttributeCategoryOptions( Arrays.asList( data.getCategoryOption() ) )
            .array();

        trackerImportExportActions
            .postAndGetJobReport( object, new QueryParamsBuilder().addAll( "async=false", "idScheme=" + idScheme ) )
            .validateSuccessfulImport();
    }

    @Test
    public void shouldImportEventsWithDifferentIdSchemes()
    {
        String programIdScheme = "CODE";
        String ouIdScheme = "NAME";
        String programStageIdScheme = "ATTRIBUTE:" + ATTRIBUTE_ID;
        TestData data = new TestData();

        JsonObject event = new EventDataBuilder()
            .setProgram( new TestData( programIdScheme ).getEventProgram() )
            .setProgramStage( new TestData( programStageIdScheme ).getEventProgramStage() )
            .setOu( new TestData( ouIdScheme ).getOrgUnit() )
            .setAttributeCategoryOptions( Arrays.asList( data.getCategoryOption() ) )
            .array();

        trackerImportExportActions
            .postAndGetJobReport( event, new QueryParamsBuilder()
                .add( "programIdScheme", programIdScheme )
                .add( "orgUnitIdScheme", ouIdScheme )
                .add( "programStageIdScheme", programStageIdScheme ) )
            .validateSuccessfulImport();
    }

    @Getter
    @Setter
    private static class TestData
    {
        private JsonPath jsonPath;

        private String trackedEntityType = "mthkj6qr5y9";

        private String orgUnit = "yMXcwGmzIWY";

        private String trackedEntityAttribute = "Kg6I0Cl3C7r";

        private String trackerProgram = "iAI6kmFqoOc";

        private String trackerProgramStage = "eTaBehVASzG";

        private String dataElement = "VkoGQvbzHk2";

        private String eventProgram = "jDnjGYZFkA4";

        private String eventProgramStage = "fFNTQZPt2J4";

        private String categoryOption = "fjvZIRlTBrp";

        private String relationshipType = "XDaaLiqMYKy";

        public TestData()
        {

        }

        public TestData( String idScheme )
        {
            jsonPath = JsonPath.from( new File( METADATA_FILE_PATH ) );
            String propertyName;
            if ( idScheme.toLowerCase().contains( "attribute" ) )
            {
                propertyName = "attributeValues.value[0]";
            }
            else
            {
                propertyName = idScheme.toLowerCase( Locale.ROOT );
            }

            this.setOrgUnit( extractProperty( "organisationUnits", orgUnit, propertyName ) );
            this.setTrackedEntityType( extractProperty( "trackedEntityTypes", trackedEntityType, propertyName ) );
            this.setTrackerProgram( extractProperty( "programs", trackerProgram, propertyName ) );
            this.setTrackerProgramStage( extractProperty( "programStages", trackerProgramStage, propertyName ) );
            this.setTrackedEntityAttribute(
                extractProperty( "trackedEntityAttributes", trackedEntityAttribute, propertyName ) );
            this.setDataElement( extractProperty( "dataElements", dataElement, propertyName ) );
            this.setEventProgram( extractProperty( "programs", eventProgram, propertyName ) );
            this.setEventProgramStage(
                extractProperty( "programStages", eventProgramStage, propertyName ) );
            this.setCategoryOption( extractProperty( "categoryOptions", categoryOption, propertyName ) );
            this.setRelationshipType( extractProperty( "relationshipTypes", relationshipType, propertyName ) );
        }

        private String extractProperty( String metadataCollection, String id, String propertyName )
        {
            return jsonPath
                .getString( String.format( "%s.find{it.id=='%s'}.%s", metadataCollection, id, propertyName ) );
        }
    }

    private String createTei()
    {
        return trackerImportExportActions
            .postAndGetJobReport(
                new TeiDataBuilder().array( new TestData().getTrackedEntityType(), new TestData().getOrgUnit() ) )
            .validateSuccessfulImport()
            .extractImportedTeis().get( 0 );
    }
}
