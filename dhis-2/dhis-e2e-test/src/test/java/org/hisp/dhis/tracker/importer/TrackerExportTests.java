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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hisp.dhis.Constants;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerExportTests
    extends TrackerNtiApiTest
{
    private static String teiId;

    private static String enrollmentId;

    private static String eventId;

    private static String relationshipId;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        loginActions.loginAsSuperUser();

        TrackerApiResponse response = trackerActions.postAndGetJobReport(
            new File( "src/test/resources/tracker/importer/teis/teisWithEnrollmentsAndEvents.json" ) );

        teiId = response.validateSuccessfulImport().extractImportedTeis().get( 0 );
        enrollmentId = response.extractImportedEnrollments().get( 0 );
        relationshipId = importRelationshipBetweenTeis( teiId, response.extractImportedTeis().get( 1 ) )
            .extractImportedRelationships().get( 0 );
        eventId = response.extractImportedEvents().get( 0 );
    }

    private Stream<Arguments> shouldReturnRequestedFields()
    {
        return Stream.of(
            Arguments.of( "/trackedEntities/" + teiId,
                "enrollments[createdAt],relationships[from[trackedEntity[trackedEntity]],to[trackedEntity[trackedEntity]]]",
                "enrollments.createdAt,relationships.from.trackedEntity.trackedEntity,relationships.to.trackedEntity.trackedEntity" ),
            Arguments.of( "/trackedEntities/" + teiId, "trackedEntity,enrollments", null ),
            Arguments.of( "/enrollments/" + enrollmentId, "program,status,enrolledAt", null ),
            Arguments.of( "/trackedEntities/" + teiId, "*",
                "trackedEntity,trackedEntityType,createdAt,updatedAt,orgUnit,inactive,deleted,potentialDuplicate,updatedBy,attributes",
                null ),
            Arguments.of( "/events/" + eventId, "enrollment,createdAt", null ),
            Arguments.of( "/relationships/" + relationshipId, "from,to[trackedEntity[trackedEntity]]",
                "from,to.trackedEntity.trackedEntity" ) );
    }

    @MethodSource
    @ParameterizedTest
    public void shouldReturnRequestedFields( String endpoint, String fields, String fieldsToValidate )
    {
        ApiResponse response = trackerActions.get( endpoint + "?fields=" + fields );

        response.validate()
            .statusCode( 200 );

        List<String> fieldList = fieldsToValidate == null ? splitFields( fields ) : splitFields( fieldsToValidate );

        fieldList.forEach(
            p -> {
                response.validate()
                    .body( p, allOf( not( nullValue() ), not( contains( nullValue() ) ), not( emptyIterable() ) ) );
            } );
    }

    @Test
    public void singleTeiAndCollectionTeiShouldReturnSameResult()
        throws Exception
    {

        TrackerApiResponse trackedEntity = trackerActions.getTrackedEntity( "Kj6vYde4LHh",
            new QueryParamsBuilder()
                .add( "fields", "*" )
                .add( "includeAllAttributes", "true" ) );

        TrackerApiResponse trackedEntities = trackerActions.getTrackedEntities( new QueryParamsBuilder()
            .add( "fields", "*" )
            .add( "includeAllAttributes", "true" )
            .add( "trackedEntity", "Kj6vYde4LHh" )
            .add( "orgUnit", "O6uvpzGd5pu" ) );

        JSONAssert.assertEquals( trackedEntity.getBody().toString(),
            trackedEntities.extractJsonObject( "instances[0]" ).toString(),
            false );

    }

    private List<String> splitFields( String fields )
    {
        List<String> split = new ArrayList<>();

        // separate fields using comma delimiter, skipping commas within []
        Arrays.stream( fields.split( "(?![^)(]*\\([^)(]*?\\)\\)),(?![^\\[]*\\])" ) ).forEach( field -> {
            if ( field.contains( "[" ) )
            {
                for ( String s : field.substring( field.indexOf( "[" ) + 1, field.indexOf( "]" ) ).split( "," ) )
                {
                    if ( s.equalsIgnoreCase( "*" ) )
                    {
                        split.add( field.substring( 0, field.indexOf( "[" ) ) );
                        return;
                    }

                    split.add( field.substring( 0, field.indexOf( "[" ) ) + "." + s );
                }

                return;
            }

            split.add( field );
        } );

        return split;
    }

    @Test
    public void shouldReturnSingleTeiGivenFilter()
    {
        trackerActions.get( "trackedEntities?orgUnit=O6uvpzGd5pu&program=f1AyMswryyQ&filter=kZeSYCgaHTk:in:Bravo" )
            .validate()
            .statusCode( 200 )
            .body( "instances.findAll { it.trackedEntity == 'Kj6vYde4LHh' }.size()", is( 1 ) )
            .body( "instances.attributes.flatten().findAll { it.attribute == 'kZeSYCgaHTk' }.value",
                everyItem( is( "Bravo" ) ) );
    }

    Stream<Arguments> shouldReturnTeisMatchingAttributeCriteria()
    {
        return Stream.of(
            Arguments.of( "like", "av", containsString( "av" ) ),
            Arguments.of( "sw", "Te", startsWith( "Te" ) ),
            Arguments.of( "ew", "AVO", endsWith( "avo" ) ),
            Arguments.of( "ew", "Bravo", endsWith( "Bravo" ) ),
            Arguments.of( "in", "Bravo", equalTo( "Bravo" ) ) );
    }

    @MethodSource( )
    @ParameterizedTest
    public void shouldReturnTeisMatchingAttributeCriteria( String operator, String searchCriteria,
        Matcher everyItemMatcher )
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "orgUnit", "O6uvpzGd5pu" )
            .add( "program", Constants.TRACKER_PROGRAM_ID )
            .add( "attribute", String.format( "kZeSYCgaHTk:%s:%s", operator, searchCriteria ) );

        trackerActions.getTrackedEntities( queryParamsBuilder )
            .validate().statusCode( 200 )
            .body( "instances", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "instances.attributes.flatten().findAll { it.attribute == 'kZeSYCgaHTk' }.value",
                everyItem( everyItemMatcher ) );
    }

    @Test
    public void shouldReturnSingleTeiGivenFilterWhileSkippingPaging()
    {

        trackerActions
            .get(
                "trackedEntities?skipPaging=true&orgUnit=O6uvpzGd5pu&program=f1AyMswryyQ&filter=kZeSYCgaHTk:in:Bravo" )
            .validate()
            .statusCode( 200 )
            .body( "instances.findAll { it.trackedEntity == 'Kj6vYde4LHh' }.size()", is( 1 ) )
            .body( "instances.attributes.flatten().findAll { it.attribute == 'kZeSYCgaHTk' }.value",
                everyItem( is( "Bravo" ) ) );
    }

    @Test
    public void shouldReturnRelationshipsByTei()
    {
        trackerActions.getRelationship( "?trackedEntity=" + teiId )
            .validate()
            .statusCode( 200 )
            .body( "instances", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .rootPath( "instances[0]" )
            .body( "relationship", equalTo( relationshipId ) )
            .body( "from.trackedEntity.trackedEntity", equalTo( teiId ) )
            .body( "to.trackedEntity.trackedEntity", notNullValue() );
    }
}
