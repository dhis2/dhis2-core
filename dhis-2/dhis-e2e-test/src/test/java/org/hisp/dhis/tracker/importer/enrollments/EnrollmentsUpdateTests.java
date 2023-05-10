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
package org.hisp.dhis.tracker.importer.enrollments;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import org.hamcrest.Matchers;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EnrollmentsUpdateTests
    extends TrackerApiTest
{
    @BeforeAll
    public void beforeAll()
    {
        loginActions.loginAsSuperUser();
    }

    @Test
    public void shouldNotUpdateImmutableProperties()
        throws Exception
    {
        String enrollmentId = importEnrollment();
        String program = new ProgramActions().createProgram( "WITH_REGISTRATION" ).extractUid();
        JsonObject body = trackerActions.get( "/enrollments/" + enrollmentId ).getBody();

        body = JsonObjectBuilder.jsonObject( body )
            .addProperty( "enrollment", enrollmentId )
            .addProperty( "trackedEntity", importTei() )
            .addProperty( "program", program )
            .wrapIntoArray( "enrollments" );

        trackerActions.postAndGetJobReport( body, new QueryParamsBuilder().add( "importStrategy=UPDATE" ) )
            .validateErrorReport()
            .body( "", hasSize( Matchers.greaterThanOrEqualTo( 2 ) ) )
            .body( "errorCode", hasItems( "E1127", "E1127" ) )
            .body( "message", Matchers.hasItem( Matchers.containsString( "trackedEntity" ) ) )
            .body( "message", Matchers.hasItem( Matchers.containsString( "program" ) ) );
    }
}
