/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    private static final String TEI = "Kj6vYde4LHh";

    private static final String TEI_POTENTIAL_DUPLICATE = "Nav6inZRw1u";

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        loginActions.loginAsSuperUser();

        TrackerApiResponse response = importTeiWithEnrollmentAndEvent();
        teiId = response.validateSuccessfulImport().extractImportedTeis().get( 0 );
        enrollmentId = response.extractImportedEnrollments().get( 0 );
        relationshipId = response.extractImportedRelationships().get( 0 );
        eventId = response.extractImportedEvents().get( 0 );
    }

    private Stream<Arguments> provideParams()
    {
        return Stream.of(
            Arguments.of( "/trackedEntities/" + teiId,
                "enrollments.createdAt,relationships[from.trackedEntity,to.trackedEntity]",
                null ),
            Arguments.of( "/trackedEntities/" + teiId, "trackedEntity,enrollments", null ),
            Arguments.of( "/enrollments/" + enrollmentId, "program,status,enrolledAt", null ),
            Arguments.of( "/enrollments/" + enrollmentId, "**", "enrollment,updatedAt,createdAt,occurredAt,enrolledAt",
                null ),
            Arguments.of( "/trackedEntities/" + teiId, "*",
                "attributes,enrollments[createdAt,events],trackedEntity,orgUnit" ),
            Arguments.of( "/trackedEntities/" + teiId, "**", "attributes,enrollments[createdAt,events]" ),
            Arguments.of( "/events/" + eventId, "enrollment,createdAt", null ),
            Arguments.of( "/relationships/" + relationshipId, "from,to.trackedEntity[*]", null ) );
    }

    @MethodSource( "provideParams" )
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
    public void getTeiByPotentialDuplicateParamNull()
    {
        ApiResponse response = trackerActions.get( teiParamsBuilder().build() );

        response.validate().statusCode( 200 )
            .body( "trackedEntityInstances", iterableWithSize( 2 ) );

        assertThat( response.getBody().getAsJsonObject(),
            matchesJSON( new JsonObjectBuilder()
                .addArray( "trackedEntityInstances",
                    new JsonObjectBuilder().addProperty( "trackedEntityInstance", TEI ).build(),
                    new JsonObjectBuilder().addProperty( "trackedEntityInstance", TEI_POTENTIAL_DUPLICATE ).build() )
                .build() ) );
    }

    @Test
    public void getTeiByPotentialDuplicateParamFalse()
    {
        ApiResponse response = trackerActions.get( teiParamsBuilder().add( "potentialDuplicate=false" ).build() );

        response.validate().statusCode( 200 )
            .body( "trackedEntityInstances", iterableWithSize( 1 ) )
            .body( "trackedEntityInstances[0].trackedEntityInstance",
                equalTo( TEI ) )
            .body( "trackedEntityInstances[0].potentialDuplicate", equalTo( false ) );
    }

    @Test
    public void getTeiByPotentialDuplicateParamTrue()
    {
        ApiResponse response = trackerActions.get( teiParamsBuilder().add( "potentialDuplicate=true" ).build() );

        response.validate().statusCode( 200 )
            .body( "trackedEntityInstances", iterableWithSize( 1 ) )
            .body( "trackedEntityInstances[0].trackedEntityInstance",
                equalTo( TEI_POTENTIAL_DUPLICATE ) )
            .body( "trackedEntityInstances[0].potentialDuplicate", equalTo( true ) );
    }

    private static QueryParamsBuilder teiParamsBuilder()
    {
        return new QueryParamsBuilder().addAll(
            "trackedEntityInstance=" + TEI + ";" + TEI_POTENTIAL_DUPLICATE,
            "trackedEntityType=" + "Q9GufDoplCL",
            "ou=" + "O6uvpzGd5pu" );
    }
}
