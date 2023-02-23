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

import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_ATTRIBUTES;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_EVENTS;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_EVENTS_RELATIONSHIPS;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_RELATIONSHIPS;
import static org.hisp.dhis.dxf2.events.Param.EVENTS;
import static org.hisp.dhis.dxf2.events.Param.PROGRAM_OWNERS;
import static org.hisp.dhis.dxf2.events.Param.RELATIONSHIPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntityFieldsParamMapperTest extends DhisControllerConvenienceTest
{
    @Autowired
    TrackedEntityFieldsParamMapper mapper;

    @ParameterizedTest
    @ValueSource( strings = { "*", "!*" } )
    void mapWithStar( String fields )
    {
        // This value "!*" does not make sense as it means exclude all.
        // We initially assumed field filtering would exclude all fields but is does not. Keeping this test as a reminder of its behavior.
        TrackedEntityInstanceParams params = map( fields );

        assertTrue( params.hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertTrue( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithOnlyRelationships()
    {
        TrackedEntityInstanceParams params = map( "relationships" );

        assertTrue( params.hasIncluded( RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithOnlyProgramOwners()
    {
        TrackedEntityInstanceParams params = map( "programOwners" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertTrue( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithOnlyOneExclusion()
    {
        TrackedEntityInstanceParams params = map( "!relationships" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithAllExceptRelationships()
    {
        TrackedEntityInstanceParams params = map( "*,!relationships" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertTrue( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithAllExceptProgramOwners()
    {
        TrackedEntityInstanceParams params = map( "*,!programOwners" );

        assertTrue( params.hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithSubFields()
    {
        TrackedEntityInstanceParams params = map( "programOwners[orgUnit],relationships[from[trackedEntity],to[*]]" );

        assertTrue( params.hasIncluded( RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertTrue( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapWithExcludedSubFields()
    {
        TrackedEntityInstanceParams params = map( "enrollments[!uid,!relationships],relationships[relationship]" );

        assertTrue( params.hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_ATTRIBUTES ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapOnlyIncludeIfFieldIsRoot()
    {
        TrackedEntityInstanceParams params = map( "enrollments[events,relationships]" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapNestedFieldsMatchesInnerParamsFields()
    {
        TrackedEntityInstanceParams params = map( "enrollments[events[relationships],relationships]" );

        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.getEnrollmentParams().hasIncluded( EVENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertTrue( params.getEnrollmentParams().hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS_RELATIONSHIPS ) );
        assertTrue( params.getEnrollmentParams().getEventParams().hasIncluded( RELATIONSHIPS ) );
    }

    @Test
    void mapOnlyIncludeIfNotAlsoExcluded()
    {
        // is order independent
        TrackedEntityInstanceParams params = map( "relationships,!relationships" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );

        params = map( "!relationships,relationships" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS ) );
        assertFalse( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
    }

    @Test
    void mapRootInclusionPrecedesSubfieldExclusion()
    {
        TrackedEntityInstanceParams params = map( "enrollments,enrollments[!status]" );

        assertFalse( params.hasIncluded( RELATIONSHIPS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS ) );
        assertTrue( params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertFalse( params.hasIncluded( PROGRAM_OWNERS ) );
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

        assertEquals( expectEvents, params.hasIncluded( ENROLLMENTS_EVENTS ) );
        assertEquals( expectEnrollments, params.hasIncluded( ENROLLMENTS ) );
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

        assertEquals( expectTeiRelationship, params.hasIncluded( RELATIONSHIPS ) );
        assertEquals( expectEnrollmentRelationship, params.hasIncluded( ENROLLMENTS_RELATIONSHIPS ) );
        assertEquals( expectEventRelationships, params.hasIncluded( ENROLLMENTS_EVENTS_RELATIONSHIPS ) );
    }

    private TrackedEntityInstanceParams map( String fields )
    {
        return mapper.map( FieldFilterParser.parse( fields ) );
    }
}