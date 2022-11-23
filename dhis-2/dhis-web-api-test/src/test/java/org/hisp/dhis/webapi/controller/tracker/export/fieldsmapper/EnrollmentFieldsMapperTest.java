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

import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.EnrollmentFieldsParamMapper.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EnrollmentFieldsMapperTest
{
    static Stream<Arguments> getEnrollmentParamsMultipleCases()
    {
        return Stream.of(
            arguments( List.of( "!*" ), false, false, false ),
            arguments( List.of( "*" ), true, true, true ),
            arguments( List.of( "*", "!relationships" ), true, true, false ),
            arguments( List.of( "*", "!attributes" ), false, true, true ),
            arguments( List.of( "*", "!events" ), true, false, true ),
            arguments( List.of( "!events" ), false, false, false ),
            arguments( List.of( "events" ), false, true, false ),
            arguments( List.of( "!attributes" ), false, false, false ),
            arguments( List.of( "attributes" ), true, false, false ),
            arguments( List.of( "!attributes", "attributes" ), false, false, false ),
            arguments( List.of( "!attributes[*]" ), false, false, false ),
            arguments( List.of( "attributes[createdAt]", "attributes", "attributes", "!attributes" ), false, false,
                false ),
            arguments( List.of( "events[!createdAt]" ), false, true, false ),
            arguments( List.of( "attributes", "!events" ), true, false, false ),
            arguments( List.of( "relationships", "!attributes" ), false, false, true ),
            arguments( List.of( "events", "!relationships" ), false, true, false ),
            arguments( List.of( "attributes[*]", "events[*]", "relationships[*]" ), true, true, true ),
            arguments( List.of( "relationships[*]", "!relationships[*]" ), false, false, false ),
            arguments( List.of( "!relationships[*]", "relationships[*]" ), false, false, false ),
            arguments( List.of( "relationships", "relationships[!from]" ), false, false, true ) );
    }

    @MethodSource
    @ParameterizedTest
    void getEnrollmentParamsMultipleCases( List<String> fields, boolean expectAttributes,
        boolean expectEvents, boolean expectRelationships )
    {
        TrackedEntityInstanceParams params = map( fields );

        assertEquals( expectAttributes, params.getEnrollmentParams().isIncludeAttributes() );
        assertEquals( expectEvents, params.getEnrollmentParams().isIncludeEvents() );
        assertEquals( expectRelationships, params.getEnrollmentParams().isIncludeRelationships() );
    }
}