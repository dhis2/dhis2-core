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
package org.hisp.dhis.tracker.deduplication;

import static org.hamcrest.Matchers.*;

import java.util.Arrays;

import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesTests
    extends PotentialDuplicatesApiTest
{
    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsAdmin();
    }

    @ParameterizedTest
    @ValueSource( strings = { "OPEN", "INVALID", "MERGED" } )
    public void shouldFilterByStatus( String status )
    {
        String teiA = createTei();
        String teiB = createTei();

        potentialDuplicatesActions.createPotentialDuplicate( teiA, teiB, status ).validate().statusCode( 200 );

        ApiResponse response = potentialDuplicatesActions.get( "", new QueryParamsBuilder().add( "status=" + status ) );
        response
            .validate()
            .body( "identifiableObjects", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "identifiableObjects.status", everyItem( equalTo( status ) ) );
    }

    @Test
    public void shouldReturnAllStatuses()
    {
        Arrays.asList( "OPEN", "MERGED", "INVALID" ).forEach( status -> {
            String teiA = createTei();
            String teiB = createTei();

            potentialDuplicatesActions.createPotentialDuplicate( teiA, teiB, status ).validate().statusCode( 200 );
        } );

        potentialDuplicatesActions.get( "", new QueryParamsBuilder().add( "status=ALL" ) )
            .validate()
            .body( "identifiableObjects", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "identifiableObjects.status",
                allOf( hasItem( "OPEN" ), hasItem( "INVALID" ), hasItem( "MERGED" ) ) );
    }

    @Test
    public void shouldRequireBothTeis()
    {
        potentialDuplicatesActions.createPotentialDuplicate( null, createTei(), "OPEN" )
            .validate()
            .statusCode( equalTo( 400 ) )
            .body( "status", equalTo( "ERROR" ) )
            .body( "message", containsStringIgnoringCase( "missing required input property" ) );
    }

    @CsvSource( {
        "MERGED,INVALID,false",
        "OPEN,INVALID,true",
        "OPEN,MERGED,false",
        "INVALID,OPEN,true",
        "MERGED,OPEN,false"
    } )
    @ParameterizedTest
    public void shouldUpdateStatus( String status, String newStatus, boolean shouldUpdate )
    {
        String duplicateId = potentialDuplicatesActions.createAndValidatePotentialDuplicate( createTei(), createTei(),
            status );

        ApiResponse response = potentialDuplicatesActions.update( duplicateId + "?status=" + newStatus,
            new JsonObjectBuilder().build() );

        if ( shouldUpdate )
        {
            response.validate().statusCode( 200 );

            potentialDuplicatesActions.get( duplicateId ).validate()
                .statusCode( 200 )
                .body( "status", equalTo( newStatus ) );
            return;
        }

        response.validate().statusCode( 400 ).body( "status", equalTo( "ERROR" ) );
    }

    @Test
    public void shouldGetDuplicatesByTei()
    {
        String teiA = createTei();
        String teiB = createTei();
        String teiC = createTei();
        String teiD = createTei();

        potentialDuplicatesActions.createPotentialDuplicate( teiA, teiB, "OPEN" ).validate().statusCode( 200 );
        potentialDuplicatesActions.createPotentialDuplicate( teiC, teiA, "INVALID" ).validate().statusCode( 200 );
        potentialDuplicatesActions.createPotentialDuplicate( teiD, teiA, "OPEN" ).validate().statusCode( 200 );

        potentialDuplicatesActions.get( "", new QueryParamsBuilder().add( "teis=" + teiA ) )
            .validate().statusCode( 200 )
            .body( "identifiableObjects", hasSize( 2 ) );

        potentialDuplicatesActions
            .get( "", new QueryParamsBuilder().addAll( "teis=" + teiB + "," + teiC, "status=ALL" ) )
            .validate().statusCode( 200 )
            .body( "identifiableObjects", hasSize( 2 ) );

        potentialDuplicatesActions.get( "", new QueryParamsBuilder().addAll( "teis=" + teiA, "status=INVALID" ) )
            .validate().statusCode( 200 )
            .body( "identifiableObjects", hasSize( 1 ) );

        potentialDuplicatesActions.get( "", new QueryParamsBuilder().addAll( "teis=" + teiA, "status=OPEN" ) )
            .validate().statusCode( 200 )
            .body( "identifiableObjects", hasSize( 2 ) );

        potentialDuplicatesActions.get( "", new QueryParamsBuilder().addAll( "teis=" + teiA, "status=MERGED" ) )
            .validate().statusCode( 200 )
            .body( "identifiableObjects", hasSize( 0 ) );

        potentialDuplicatesActions.get( "", new QueryParamsBuilder().addAll( "teis=" + teiA, "status=ALL" ) )
            .validate().statusCode( 200 )
            .body( "identifiableObjects", hasSize( 3 ) );
    }
}
