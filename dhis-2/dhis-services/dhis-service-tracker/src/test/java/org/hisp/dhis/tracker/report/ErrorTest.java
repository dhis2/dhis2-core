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
package org.hisp.dhis.tracker.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.junit.jupiter.api.Test;

class ErrorTest
{

    // The actual error message does not matter for the purpose of this test.
    // The only important thing is that it contains one argument that will be
    // interpolated.
    // This argument is what we assert on.
    private final static TrackerErrorCode SINGLE_ARG_ERROR = TrackerErrorCode.E1006;

    private final static TrackerErrorCode DOUBLE_ARG_ERROR = TrackerErrorCode.E1000;

    @Test
    void addArgAddsObjectToErrorMessage()
    {

        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( (Object) "INTERPOLATED_ARGUMENT" )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "INTERPOLATED_ARGUMENT" ) );
    }

    @Test
    void addArgAddsStringToErrorMessage()
    {

        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( "INTERPOLATED_ARGUMENT" )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "INTERPOLATED_ARGUMENT" ) );
    }

    @Test
    void addArgDoesNotThrowIfInstantIsNull()
    {

        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( (Instant) null )
            .build();

        assertNotNull( error.getErrorMessage() );
    }

    @Test
    void addArgAddsInstantToErrorMessage()
    {

        final Instant now = Instant.now();
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( now )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains(
            DateFormat.getInstance().format( Date.from( now ) ) ) );
    }

    @Test
    void addArgAddsDateToErrorMessage()
    {

        final Date now = Date.from( Instant.now() );
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( now )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains(
            DateFormat.getInstance().format( now ) ) );
    }

    @Test
    void addArgAddsIdentifiableObjectUsingUIDToErrorMessage()
    {

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( "1234" );
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( TrackerIdScheme.UID, orgUnit )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "`OrganisationUnit (1234)`" ) );
    }

    @Test
    void addArgAddsIdentifiableObjectUsingNameToErrorMessage()
    {

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setName( "Favorite Place" );
        orgUnit.setUid( "1234" );
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( TrackerIdScheme.NAME, orgUnit )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "`OrganisationUnit (Favorite Place)`" ) );
    }

    @Test
    void addArgAddsEnrollmentToErrorMessage()
    {

        Enrollment enrollment = Enrollment.builder()
            .enrollment( "1234" )
            .build();
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( enrollment )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "`Enrollment (1234)`" ) );
    }

    @Test
    void addArgAddsEventToErrorMessage()
    {

        Event event = Event.builder()
            .event( "1234" )
            .build();
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( event )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "`Event (1234)`" ) );
    }

    @Test
    void addArgAddsTrackedEntityToErrorMessage()
    {

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( "1234" )
            .build();
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( trackedEntity )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "`TrackedEntity (1234)`" ) );
    }

    @Test
    void addArgAddsRelationshipToErrorMessage()
    {

        Relationship relationship = Relationship.builder()
            .relationship( "1234" )
            .build();
        Error error = Error.builder()
            .errorCode( SINGLE_ARG_ERROR )
            .addArg( relationship )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "`Relationship (1234)`" ) );
    }

    @Test
    void addArgsAddsObjectsToErrorMessage()
    {

        Error error = Error.builder()
            .errorCode( DOUBLE_ARG_ERROR )
            .addArgs( "INTERPOLATED_ARGUMENT", 981 )
            .build();

        assertNotNull( error.getErrorMessage() );
        assertTrue( error.getErrorMessage().contains( "INTERPOLATED_ARGUMENT" ) );
        assertTrue( error.getErrorMessage().contains( "981" ) );
    }
}