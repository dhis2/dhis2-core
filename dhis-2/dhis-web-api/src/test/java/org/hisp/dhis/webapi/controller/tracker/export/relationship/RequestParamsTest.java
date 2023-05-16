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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.Test;

class RequestParamsTest
{
    @Test
    void getIdentifierParamIfTrackedEntityIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam(), "should return cached identifier" );
    }

    @Test
    void getIdentifierParamIfTeiIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setTei( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
    }

    @Test
    void getIdentifierNameIfTrackedEntityIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );

        assertEquals( "trackedEntity", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierNameIfTeiIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setTei( "Hq3Kc6HK4OZ" );

        assertEquals( "trackedEntity", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierClassIfTrackedEntityIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );

        assertEquals( TrackedEntity.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierClassIfTeiIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setTei( "Hq3Kc6HK4OZ" );

        assertEquals( TrackedEntity.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierParamIfEnrollmentIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
    }

    @Test
    void getIdentifierNameIfEnrollmentIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        assertEquals( "enrollment", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierClassIfEnrollmentIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        assertEquals( Enrollment.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierParamIfEventIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
    }

    @Test
    void getIdentifierNameIfEventIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        assertEquals( "event", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierClassIfEventIsSet()
        throws BadRequestException
    {

        RequestParams criteria = new RequestParams();
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        assertEquals( Event.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierParamThrowsIfNoParamsIsSet()
    {
        RequestParams criteria = new RequestParams();

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierParam );

        assertEquals( "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierNameThrowsIfNoParamsIsSet()
    {
        RequestParams criteria = new RequestParams();

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierName );

        assertEquals( "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfAllParamsAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierParam );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierNameThrowsIfAllParamsAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierName );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierClassThrowsIfAllParamsAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierClass );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfTrackedEntityAndEnrollmentAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierParam );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfTeiAndEnrollmentAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierParam );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierClassThrowsIfTrackedEntityAndEnrollmentAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierClass );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierClassThrowsIfTeiAndEnrollmentAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierClass );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfEnrollmentAndEventAreSet()
    {
        RequestParams criteria = new RequestParams();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        BadRequestException exception = assertThrows( BadRequestException.class, criteria::getIdentifierParam );

        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getMessage() );
    }
}