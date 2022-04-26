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

package org.hisp.dhis.tracker.teis;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.importer.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hisp.dhis.helpers.matchers.CustomMatchers.hasToStringContaining;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
class TrackedEntityInstanceQueryTests
    extends TrackerApiTest
{
    private TrackerActions trackerActions;

    private RestApiActions trackedEntityInstanceQueryActions;

    private final String ou = Constants.ORG_UNIT_IDS[0];

    private final Pair<String, String> firstNameAttribute = Pair
        .of( "dIVt4l5vIOa", "TrackedEntityInstanceQueryTests_Firstname" + DataGenerator.randomString( 4 ) );

    private final Pair<String, String> lastNameAttribute = Pair
        .of( "kZeSYCgaHTk", "TrackedEntityInstanceQueryTests_Lastname" + DataGenerator.randomString( 4 ) );

    private final Pair<String, String> ageProgramAttribute = Pair.of(
        "ypGAwVRNtVY", "11"
    );

    private String teiId;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();
        trackedEntityInstanceQueryActions = new RestApiActions( "/trackedEntityInstances/query" );

        new LoginActions().loginAsSuperUser();

        teiId = createTei();
    }

    Stream<Arguments> queryShouldReturnTeisMatchingAttributeValue()
    {
        return Stream.of(
            Arguments.of( "query=LIKE:first" ),
            Arguments.of( String
                .format( "attribute=%s:LIKE:%s", firstNameAttribute.getKey(), firstNameAttribute.getValue().substring( 0, 8 ) ) ),
            Arguments.of( String.format( "attribute:%s&query=LIKE:%s", lastNameAttribute.getKey(),
                lastNameAttribute.getValue().substring( 10, 15 ) ) ),
            Arguments.of(
                String.format( "attribute=%s:EQ:%s&attribute=%s:EQ:%s", lastNameAttribute.getKey(), lastNameAttribute.getValue(),
                    firstNameAttribute.getKey(), firstNameAttribute.getValue() ) ),
            Arguments.of(
                String.format( "attribute=%s:EQ:%s", ageProgramAttribute.getKey(), ageProgramAttribute.getValue() )
            )
        );
    }

    @ParameterizedTest( name = "/query with params [{arguments}]" )
    @MethodSource
    public void queryShouldReturnTeisMatchingAttributeValue( String attributesQueryParams )
    {

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "trackedEntityType", Constants.TRACKED_ENTITY_TYPE )
            .add( "ouMode", "ACCESSIBLE" );

        trackedEntityInstanceQueryActions.get( String.format( "%s&%s", queryParamsBuilder.build(), attributesQueryParams ) )
            .validate()
            .statusCode( 200 )
            .body( "height", greaterThanOrEqualTo( 1 ) )
            .body( "rows", hasItem(
                hasToStringContaining( Arrays.asList( teiId, lastNameAttribute.getValue(), firstNameAttribute.getValue() ) ) ) );
    }

    private String createTei()
    {
        String uid = new IdGenerator().generateUniqueId();

        JsonObject tei = new TeiDataBuilder().setTeiType( Constants.TRACKED_ENTITY_TYPE )
            .setOu( ou )
            .addAttribute( firstNameAttribute.getKey(), firstNameAttribute.getValue() )
            .addAttribute( lastNameAttribute.getKey(), lastNameAttribute.getValue() )
            .addEnrollment( new EnrollmentDataBuilder()
                .setProgram( Constants.TRACKER_PROGRAM_ID )
                .setOu( ou )
                .addAttribute( ageProgramAttribute.getKey(), ageProgramAttribute.getValue() )
            )
            .setId( uid )
            .array();

        trackerActions.postAndGetJobReport( tei ).validateSuccessfulImport();

        return uid;
    }
}
