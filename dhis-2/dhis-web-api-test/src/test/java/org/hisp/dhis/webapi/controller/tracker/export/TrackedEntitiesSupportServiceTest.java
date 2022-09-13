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

import static org.hisp.dhis.webapi.controller.tracker.export.TrackedEntitiesSupportService.getTrackedEntityInstanceParams;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TrackedEntitiesSupportServiceTest
{

    @Test
    void getTrackedEntityInstanceParamsWithStar()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "*" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithPresetAll()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( ":all" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExcluded()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "!*" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyRelationships()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "relationships" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyProgramOwners()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "programOwners" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithOnlyOneExclusion()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExceptRelationships()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "*", "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithAllExceptProgramOwners()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields( "*", "!programOwners" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithSubFields()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            fields( "programOwners[orgUnit]", "relationships[from[trackedEntity],to[*]]" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsWithExcludedSubFields()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            fields( "!enrollments[uid,enrolledAt]", "relationships[relationship]" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsOnlyIncludeIfFieldIsRoot()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            fields( "enrollments[events,relationships]" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsOnlyIncludeIfNotAlsoExcluded()
    {

        // is order independent
        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            fields( "relationships", "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );

        params = getTrackedEntityInstanceParams( fields( "!relationships", "relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void getTrackedEntityInstanceParamsRootInclusionPrecedesSubfieldExclusion()
    {

        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams(
            fields( "enrollments", "enrollments[!status]" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    static Stream<Arguments> getTrackedEntityInstanceParamsEnrollmentsAndEvents()
    {
        // events is a child of enrollments not TEI
        return Stream.of(
            arguments( fields( "*", "!enrollments" ), false,
                false ),
            arguments( fields( "!events" ), false, false ),
            arguments( fields( "events" ), false, false ),
            arguments( fields( "!enrollments" ), false, false ),
            arguments( fields( "!enrollments", "enrollments" ),
                false, false ),
            arguments( fields( "!enrollments[*]" ), false,
                false ),
            arguments( fields( "enrollments[createdAt]", "enrollments", "enrollments", "!enrollments",
                "enrollments[notes]", "enrollments[enrollment,updatedAt]" ),
                false, false ),
            arguments( fields( "enrollments[!events]" ), true,
                false ),
            arguments( fields( "enrollments[*,!events]" ), true,
                false ),
            arguments( fields( "enrollments", "enrollments[!events]" ),
                true, false ),
            arguments( fields( "enrollments[!status]" ), true,
                true ),
            arguments( fields( "enrollments" ), true, true ),
            arguments( fields( "enrollments", "!events" ), true,
                true ),
            arguments( fields( "enrollments[*]" ), true, true ),
            arguments( fields( "enrollments[events]" ), true,
                true ),
            arguments( fields( "enrollments[events[dataValues[*]]]" ),
                true, true ),
            arguments( fields( "enrollments[status,events]" ),
                true, true ) );
    }

    @MethodSource
    @ParameterizedTest
    void getTrackedEntityInstanceParamsEnrollmentsAndEvents( List<FieldPath> fields, boolean expectEnrollments,
        boolean expectEvents )
    {
        TrackedEntityInstanceParams params = getTrackedEntityInstanceParams( fields );

        assertEquals( expectEvents, params.isIncludeEvents() );
        assertEquals( expectEnrollments, params.isIncludeEnrollments() );
    }

    private static List<FieldPath> fields( String... fields )
    {
        return FieldFilterParser
            .parse( Collections.singleton( StringUtils.join( fields, "," ) ) );
    }
}