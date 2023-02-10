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
package org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TrackedEntityFieldsParamMapperTest
{
    @Test
    void mapWithStar()
    {
        TrackedEntityInstanceParams params = map( "*" );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithPresetAll()
    {
        TrackedEntityInstanceParams params = map( ":all" );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithAllExcluded()
    {
        TrackedEntityInstanceParams params = map( "!*" );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithOnlyRelationships()
    {
        TrackedEntityInstanceParams params = map( "relationships" );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithOnlyProgramOwners()
    {
        TrackedEntityInstanceParams params = map( "programOwners" );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithOnlyOneExclusion()
    {
        TrackedEntityInstanceParams params = map( "!relationships" );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithAllExceptRelationships()
    {
        TrackedEntityInstanceParams params = map( "*,!relationships" );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithAllExceptProgramOwners()
    {
        TrackedEntityInstanceParams params = map( "*,!programOwners" );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithSubFields()
    {
        TrackedEntityInstanceParams params = map( "programOwners[orgUnit],relationships[from[trackedEntity],to[*]]" );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithExcludedSubFields()
    {
        TrackedEntityInstanceParams params = map( "!enrollments[uid,enrolledAt],relationships[relationship]" );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapOnlyIncludeIfFieldIsRoot()
    {
        TrackedEntityInstanceParams params = map( "enrollments[events,relationships]" );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapOnlyIncludeIfNotAlsoExcluded()
    {
        // is order independent
        TrackedEntityInstanceParams params = map( "relationships,!relationships" );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );

        params = map( "!relationships,relationships" );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapRootInclusionPrecedesSubfieldExclusion()
    {
        TrackedEntityInstanceParams params = map( "enrollments,enrollments[!status]" );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    static Stream<Arguments> mapEnrollmentsAndEvents()
    {
        // events is a child of enrollments not TEI
        return Stream.of(
            arguments( "*,!enrollments", false, false ),
            arguments( "!events", false, false ),
            arguments( "events", false, false ),
            arguments( "!enrollments", false, false ),
            arguments( "!enrollments,enrollments", false, false ),
            arguments( "!enrollments[*]", false, false ),
            arguments(
                "enrollments[createdAt],enrollments,enrollments,!enrollments,enrollments[notes],enrollments[enrollment,updatedAt]",
                false, false ),
            arguments( "enrollments[!events]", true, false ),
            arguments( "enrollments[*,!events]", true, false ),
            arguments( "enrollments,enrollments[!events]", true, false ),
            arguments( "enrollments[!status]", true, true ),
            arguments( "enrollments", true, true ),
            arguments( "enrollments,!events", true, true ),
            arguments( "enrollments[*]", true, true ),
            arguments( "enrollments[events]", true, true ),
            arguments( "enrollments[events[dataValues[*]]]", true, true ),
            arguments( "enrollments[status,events]", true, true ) );
    }

    @MethodSource
    @ParameterizedTest
    void mapEnrollmentsAndEvents( String fields, boolean expectEnrollments,
        boolean expectEvents )
    {
        TrackedEntityInstanceParams params = map( fields );

        assertEquals( expectEvents, params.getTeiEnrollmentParams().isIncludeEvents() );
        assertEquals( expectEnrollments, params.isIncludeEnrollments() );
    }

    static Stream<Arguments> shouldSetCorrectRelationshipsWhenMixedRelationshipFields()
    {
        return Stream.of(
            arguments( "!relationships,enrollments[relationships,events]", false, true,
                true ),
            arguments( "relationships,enrollments[!relationships,events]", true, false,
                true ),
            arguments( "relationships,enrollments[relationships,events[!relationships]]",
                true, true, false ),
            arguments( "!relationships,enrollments[!relationships,events[!relationships]]",
                false, false, false ),
            arguments( "!enrollments,relationships,enrollments[relationships,events[!relationships]]",
                true, false, false ) );
    }

    @MethodSource
    @ParameterizedTest
    void shouldSetCorrectRelationshipsWhenMixedRelationshipFields( String fields,
        boolean expectTeiRelationship,
        boolean expectEnrollmentRelationship,
        boolean expectEventRelationships )
    {

        TrackedEntityInstanceParams params = map( fields );

        assertEquals( expectTeiRelationship, params.isIncludeRelationships() );
        assertEquals( expectEnrollmentRelationship, params.getEnrollmentParams().isIncludeRelationships() );
        assertEquals( expectEventRelationships, params.getEventParams().isIncludeRelationships() );
    }

    private static TrackedEntityInstanceParams map( String fields )
    {
        return TrackedEntityFieldsParamMapper.map( FieldFilterParser.parse( fields ) );
    }
}