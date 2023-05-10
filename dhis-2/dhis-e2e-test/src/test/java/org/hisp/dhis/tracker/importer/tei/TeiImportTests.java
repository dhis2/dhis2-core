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
package org.hisp.dhis.tracker.importer.tei;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hisp.dhis.Constants;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.importer.databuilder.RelationshipDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TeiImportTests
    extends TrackerApiTest
{
    private String teiId;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        loginActions.loginAsSuperUser();

        teiId = super.importTei();
    }

    @Test
    public void shouldImportTei()
    {
        // arrange
        JsonObject trackedEntities = new TeiDataBuilder()
            .setTeiType( Constants.TRACKED_ENTITY_TYPE )
            .setOu( Constants.ORG_UNIT_IDS[0] )
            .array();

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response
            .validateSuccessfulImport()
            .validateTeis()
            .body( "stats.created", equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports[0].errorReports", empty() );

        // assert that the tei was imported
        String teiId = response.extractImportedTeis().get( 0 );

        ApiResponse teiResponse = trackerActions.getTrackedEntity( teiId );

        teiResponse.validate()
            .statusCode( 200 );

        assertThat( teiResponse.getBody(),
            matchesJSON( trackedEntities.get( "trackedEntities" ).getAsJsonArray().get( 0 ) ) );
    }

    @Test
    public void shouldImportTeiAndEnrollmentWithAttributes()
        throws Exception
    {
        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData(
                new File( "src/test/resources/tracker/importer/teis/teiWithEnrollmentAndAttributes.json" ) );

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( teiBody );

        // assert
        response.validateSuccessfulImport()
            .validate()
            .body( "stats.created", equalTo( 2 ) )
            .rootPath( "bundleReport.typeReportMap" )
            .body( "TRACKED_ENTITY.objectReports", hasSize( 1 ) )
            .body( "ENROLLMENT.objectReports", hasSize( 1 ) );

        // assert that the TEI was imported
        String teiId = response.extractImportedTeis().get( 0 );

        ApiResponse teiResponse = trackerActions.getTrackedEntity( teiId );

        teiResponse.validate().statusCode( 200 );
    }

    @Test
    public void shouldImportTeisWithEnrollmentsEventsAndRelationship()
        throws Exception
    {
        JsonObject teiPayload = new FileReaderUtils()
            .readJsonAndGenerateData(
                new File( "src/test/resources/tracker/importer/teis/teisWithEnrollmentsAndEvents.json" ) );

        JsonObject teiToTeiRelationship = new RelationshipDataBuilder().buildTrackedEntityRelationship( "Kj6vYde4LHh",
            "Nav6inZRw1u", "xLmPUYJX8Ks" );

        JsonObjectBuilder.jsonObject( teiPayload )
            .addArray( "relationships", teiToTeiRelationship );

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( teiPayload );

        response.validateSuccessfulImport()
            .validate()
            .body( "stats.created", equalTo( 7 ) )
            .rootPath( "bundleReport.typeReportMap" )
            .body( "TRACKED_ENTITY.objectReports", hasSize( 2 ) )
            .body( "ENROLLMENT.objectReports", hasSize( 2 ) )
            .body( "EVENT.objectReports", hasSize( 2 ) )
            .body( "RELATIONSHIP.objectReports", hasSize( 1 ) );

        JsonObject teiBody = teiPayload.get( "trackedEntities" ).getAsJsonArray().get( 0 ).getAsJsonObject();

        ApiResponse trackedEntityResponse = trackerActions
            .getTrackedEntity( teiBody.get( "trackedEntity" ).getAsString() + "?fields=*" ).validateStatus( 200 );

        // Compare Tei Relationship
        assertThat( trackedEntityResponse.getBody().getAsJsonArray( "relationships" ).get( 0 ),
            matchesJSON( teiToTeiRelationship ) );

        // Compare Enrollment attributes and events
        JsonArray enrollmentsAttributes = trackedEntityResponse.getBody().getAsJsonArray( "enrollments" ).get( 0 )
            .getAsJsonObject().getAsJsonArray( "attributes" );

        List<String> expectedAttributes = new ArrayList<>();

        enrollmentsAttributes
            .forEach( ea -> expectedAttributes.add( ea.getAsJsonObject().get( "attribute" ).getAsString() ) );

        assertThat( teiBody.getAsJsonObject( "events" ),
            equalTo( trackedEntityResponse.getBody().getAsJsonObject( "events" ) ) );
        assertThat( expectedAttributes, containsInAnyOrder( "kZeSYCgaHTk", "dIVt4l5vIOa" ) );
    }

    Stream<Arguments> shouldImportTeiAttributes()
    {
        return Stream.of(
            Arguments.of( "xuAl9UIMcmI", Constants.ORG_UNIT_IDS[1], "ORGANISATION_UNIT" ),
            Arguments.of( "kZeSYCgaHTk", "TEXT_ATTRIBUTE_VALUE", "TEXT" ),
            Arguments.of( "aIga5mPOFOJ", "TA_MALE", "TEXT with optionSet" ),
            Arguments.of( "ypGAwVRNtVY", "10", "NUMBER" ),
            Arguments.of( "x5yfLot5VCM", "2010-10-01", "DATE" ) );
    }

    @MethodSource( )
    @ParameterizedTest( name = "update tei with attribute value type {2}" )
    public void shouldImportTeiAttributes( String teaId, String attributeValue, String attValueType )
    {
        JsonObject payload = new TeiDataBuilder()
            .setId( teiId )
            .setOu( Constants.ORG_UNIT_IDS[0] )
            .addAttribute( teaId, attributeValue )
            .array();

        trackerActions.postAndGetJobReport( payload )
            .validateSuccessfulImport();

        trackerActions.getTrackedEntity( teiId + "?fields=attributes" )
            .validate()
            .body( "attributes.value", hasItem( attributeValue ) );
    }

}
