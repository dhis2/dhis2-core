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

import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.TrackedEntityFieldsParamMapper.map;
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

class TrackedEntityFieldsParamMapperTest
{

    @Test
    void mapWithStar()
    {

        TrackedEntityInstanceParams params = map( fields( "*" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithPresetAll()
    {

        TrackedEntityInstanceParams params = map( fields( ":all" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithAllExcluded()
    {

        TrackedEntityInstanceParams params = map( fields( "!*" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithOnlyRelationships()
    {

        TrackedEntityInstanceParams params = map( fields( "relationships" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithOnlyProgramOwners()
    {

        TrackedEntityInstanceParams params = map( fields( "programOwners" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithOnlyOneExclusion()
    {

        TrackedEntityInstanceParams params = map( fields( "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithAllExceptRelationships()
    {

        TrackedEntityInstanceParams params = map( fields( "*", "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithAllExceptProgramOwners()
    {

        TrackedEntityInstanceParams params = map( fields( "*", "!programOwners" ) );

        assertTrue( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithSubFields()
    {

        TrackedEntityInstanceParams params = map(
            fields( "programOwners[orgUnit]", "relationships[from[trackedEntity],to[*]]" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertTrue( params.isIncludeProgramOwners() );
    }

    @Test
    void mapWithExcludedSubFields()
    {

        TrackedEntityInstanceParams params = map(
            fields( "!enrollments[uid,enrolledAt]", "relationships[relationship]" ) );

        assertTrue( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapOnlyIncludeIfFieldIsRoot()
    {

        TrackedEntityInstanceParams params = map(
            fields( "enrollments[events,relationships]" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapOnlyIncludeIfNotAlsoExcluded()
    {

        // is order independent
        TrackedEntityInstanceParams params = map(
            fields( "relationships", "!relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );

        params = map( fields( "!relationships", "relationships" ) );

        assertFalse( params.isIncludeRelationships() );
        assertFalse( params.isIncludeEnrollments() );
        assertFalse( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    @Test
    void mapRootInclusionPrecedesSubfieldExclusion()
    {

        TrackedEntityInstanceParams params = map(
            fields( "enrollments", "enrollments[!status]" ) );

        assertFalse( params.isIncludeRelationships() );
        assertTrue( params.isIncludeEnrollments() );
        assertTrue( params.getTeiEnrollmentParams().isIncludeEvents() );
        assertFalse( params.isIncludeProgramOwners() );
    }

    static Stream<Arguments> mapEnrollmentsAndEvents()
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
    void mapEnrollmentsAndEvents( List<FieldPath> fields, boolean expectEnrollments,
        boolean expectEvents )
    {
        TrackedEntityInstanceParams params = map( fields );

        assertEquals( expectEvents, params.getTeiEnrollmentParams().isIncludeEvents() );
        assertEquals( expectEnrollments, params.isIncludeEnrollments() );
    }

    static Stream<Arguments> shouldSetCorrectRelationshipsWhenMixedRelationshipFields()
    {
        return Stream.of(
            arguments( fields( "!relationships,enrollments[relationships,events]" ), false, true,
                true ),
            arguments( fields( "relationships,enrollments[!relationships,events]" ), true, false,
                true ),
            arguments( fields( "relationships,enrollments[relationships,events[!relationships]]" ),
                true, true, false ),
            arguments( fields( "!relationships,enrollments[!relationships,events[!relationships]]" ),
                false, false, false ),
            arguments( fields( "!enrollments", "relationships,enrollments[relationships,events[!relationships]]" ),
                true, false, false ) );
    }

    @MethodSource
    @ParameterizedTest
    void shouldSetCorrectRelationshipsWhenMixedRelationshipFields( List<FieldPath> fields,
        boolean expectTeiRelationship,
        boolean expectEnrollmentRelationship,
        boolean expectEventRelationships )
    {

        TrackedEntityInstanceParams params = map( fields );

        assertEquals( expectTeiRelationship, params.isIncludeRelationships() );
        assertEquals( expectEnrollmentRelationship, params.getEnrollmentParams().isIncludeRelationships() );
        assertEquals( expectEventRelationships, params.getEventParams().isIncludeRelationships() );
    }

    private static List<FieldPath> fields( String... fields )
    {
        return FieldFilterParser
            .parse( Collections.singleton( StringUtils.join( fields, "," ) ) );
    }
}