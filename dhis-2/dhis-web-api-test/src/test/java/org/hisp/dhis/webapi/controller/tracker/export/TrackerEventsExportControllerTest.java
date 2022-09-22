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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;

class TrackerEventsExportControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testValidateWrongOuMode()
    {
        JsonResponse response = GET( "/tracker/events?ouMode=WRONG" ).content( HttpStatus.BAD_REQUEST );

        assertEquals( "ERROR", response.getString( "status" ).string() );
        assertEquals( "ouMode -> The value `WRONG` is not a valid OrganisationUnitSelectionMode. " +
            "Value must be one of (SELECTED, CHILDREN, DESCENDANTS, ACCESSIBLE, CAPTURE, ALL)",
            response.getString( "message" ).string() );
    }

    @Test
    void testValidateWrongProgramStatus()
    {
        JsonResponse response = GET( "/tracker/events?programStatus=WRONG" ).content( HttpStatus.BAD_REQUEST );

        assertEquals( "ERROR", response.getString( "status" ).string() );
        assertEquals( "programStatus -> The value `WRONG` is not a valid ProgramStatus. " +
            "Value must be one of (ACTIVE, COMPLETED, CANCELLED)",
            response.getString( "message" ).string() );
    }

    @Test
    void testValidateWrongAssignedUserMode()
    {
        JsonResponse response = GET( "/tracker/events?assignedUserMode=WRONG" ).content( HttpStatus.BAD_REQUEST );

        assertEquals( "ERROR", response.getString( "status" ).string() );
        assertEquals( "assignedUserMode -> The value `WRONG` is not a valid AssignedUserSelectionMode. " +
            "Value must be one of (CURRENT, PROVIDED, NONE, ANY)",
            response.getString( "message" ).string() );
    }

    @Test
    void testValidateWrongEventStatus()
    {
        JsonResponse response = GET( "/tracker/events?status=WRONG" ).content( HttpStatus.BAD_REQUEST );

        assertEquals( "ERROR", response.getString( "status" ).string() );
        assertEquals( "status -> The value `WRONG` is not a valid EventStatus. " +
            "Value must be one of (ACTIVE, COMPLETED, VISITED, SCHEDULE, OVERDUE, SKIPPED)",
            response.getString( "message" ).string() );
    }
}
