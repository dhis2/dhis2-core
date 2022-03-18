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
package org.hisp.dhis.webapi.service;

import static org.hisp.dhis.webapi.service.TrackedEntityInstanceSupportService.getTrackedEntityInstanceParams;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.junit.jupiter.api.Test;

class TrackedEntityInstanceSupportServiceTest
{

    @Test
    void getTrackedEntityInstanceParamsWithStar()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "*" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithPresetAll()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( ":all" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExcluded()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "!*" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyRelationships()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "relationships" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyEnrollments()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "enrollments" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyEvents()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "events" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyProgramOwners()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "programOwners" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyOneExclusion()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExceptRelationships()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "*", "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExceptEnrollments()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "*", "!enrollments" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExceptEvents()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "*", "!events" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExceptProgramOwners()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( List.of( "*", "!programOwners" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithSubFields()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            List.of( "enrollments[events[dataValues[*]]]", "relationships[from[trackedEntity],to[trackedEntity]]" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithExcludedSubFields()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            List.of( "!enrollments[uid,enrolledAt]", "relationships[relationship]" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsOnlyIncludeIfFieldIsRoot()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            List.of( "enrollments[events,relationships]" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

}