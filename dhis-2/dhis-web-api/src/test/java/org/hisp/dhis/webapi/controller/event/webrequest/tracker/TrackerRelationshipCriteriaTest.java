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
package org.hisp.dhis.webapi.controller.event.webrequest.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.jupiter.api.Test;

class TrackerRelationshipCriteriaTest
{
    @Test
    void getIdentifierParamIfTrackedEntityIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam(), "should return cached identifier" );
    }

    @Test
    void getIdentifierParamIfTeiIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTei( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
    }

    @Test
    void getIdentifierNameIfTrackedEntityIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );

        assertEquals( "trackedEntity", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierNameIfTeiIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTei( "Hq3Kc6HK4OZ" );

        assertEquals( "trackedEntity", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierClassIfTrackedEntityIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );

        assertEquals( TrackedEntityInstance.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierClassIfTeiIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTei( "Hq3Kc6HK4OZ" );

        assertEquals( TrackedEntityInstance.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierParamIfEnrollmentIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
    }

    @Test
    void getIdentifierNameIfEnrollmentIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        assertEquals( "enrollment", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierClassIfEnrollmentIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        assertEquals( ProgramInstance.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierParamIfEventIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        assertEquals( "Hq3Kc6HK4OZ", criteria.getIdentifierParam() );
    }

    @Test
    void getIdentifierNameIfEventIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        assertEquals( "event", criteria.getIdentifierName() );
    }

    @Test
    void getIdentifierClassIfEventIsSet()
        throws WebMessageException
    {

        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        assertEquals( ProgramStageInstance.class, criteria.getIdentifierClass() );
    }

    @Test
    void getIdentifierParamThrowsIfNoParamsIsSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierParam );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierNameThrowsIfNoParamsIsSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierName );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfAllParamsAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierParam );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierNameThrowsIfAllParamsAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierName );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierClassThrowsIfAllParamsAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierClass );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfTrackedEntityAndEnrollmentAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierParam );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfTeiAndEnrollmentAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierParam );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierClassThrowsIfTrackedEntityAndEnrollmentAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTrackedEntity( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierClass );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierClassThrowsIfTeiAndEnrollmentAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setTei( "Hq3Kc6HK4OZ" );
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierClass );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }

    @Test
    void getIdentifierParamThrowsIfEnrollmentAndEventAreSet()
    {
        TrackerRelationshipCriteria criteria = new TrackerRelationshipCriteria();
        criteria.setEnrollment( "Hq3Kc6HK4OZ" );
        criteria.setEvent( "Hq3Kc6HK4OZ" );

        WebMessageException exception = assertThrows( WebMessageException.class, criteria::getIdentifierParam );

        assertEquals( BAD_REQUEST.value(), exception.getWebMessage().getHttpStatusCode() );
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            exception.getWebMessage().getMessage() );
    }
}