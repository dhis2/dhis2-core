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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentFieldsMapperTest extends DhisControllerConvenienceTest
{
    @Autowired
    EnrollmentFieldsParamMapper mapper;

    static Stream<Arguments> getEnrollmentParamsMultipleCases()
    {
        return Stream.of(
            // This value does not make sense as it means exclude all.
            // We initially assumed field filtering would exclude all fields but is does not. Keeping this test as a reminder of its behavior.
            arguments( "!*", true, true, true ),
            arguments( "*", true, true, true ),
            arguments( "*,!relationships", true, true, false ),
            arguments( "*,!attributes", false, true, true ),
            arguments( "*,!events", true, false, true ),
            arguments( "!events", false, false, false ),
            arguments( "events", false, true, false ),
            arguments( "!attributes", false, false, false ),
            arguments( "attributes", true, false, false ),
            arguments( "!attributes", "attributes", false, false, false ),
            arguments( "!attributes[*]", false, false, false ),
            arguments( "attributes[createdAt],attributes,attributes,!attributes", false, false, false ),
            arguments( "events[!createdAt]", false, true, false ),
            arguments( "attributes,!events", true, false, false ),
            arguments( "relationships,!attributes", false, false, true ),
            arguments( "events,!relationships", false, true, false ),
            arguments( "attributes[*],events[*],relationships[*]", true, true, true ),
            arguments( "relationships[*],!relationships[*]", false, false, false ),
            arguments( "!relationships[*],relationships[*]", false, false, false ),
            arguments( "relationships,relationships[!from]", false, false, true ) );
    }

    @MethodSource
    @ParameterizedTest
    void getEnrollmentParamsMultipleCases( String fields, boolean expectAttributes,
        boolean expectEvents, boolean expectRelationships )
    {
        EnrollmentParams params = mapper.map( FieldFilterParser.parse( fields ) );

        assertEquals( expectAttributes, params.isIncludeAttributes() );
        assertEquals( expectEvents, params.isIncludeEvents() );
        assertEquals( expectRelationships, params.isIncludeRelationships() );
    }
}