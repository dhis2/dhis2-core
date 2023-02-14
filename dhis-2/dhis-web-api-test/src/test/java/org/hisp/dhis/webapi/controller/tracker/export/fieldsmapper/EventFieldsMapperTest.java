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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class EventFieldsMapperTest extends DhisControllerConvenienceTest
{
    @Autowired
    EventFieldsParamMapper eventFieldsParamMapper;

    static Stream<Arguments> getEventParamsMultipleCases()
    {
        return Stream.of(
            // TODO(ivo): this is a potential bug :( while the parser does parse this into FieldPath(name=all, path=[], exclude=true, preset=true, transformers=[], fullPath=null)
            // if you make requests using it to a metadata API you'll get the same result whether its an inclusion or exclusion
            // try: curl --silent -u admin:district -H 'content-type: application/json' 'https://play.dhis2.org/2.39.1/api/organisationUnits?page=1&pageSize=1&fields=!*' | jq .organisationUnits
            // so the FieldFilterService.apply differs from implementation and assumption of what it would do
            arguments( "!*", true ), // expected value is false on master
            arguments( "*", true ),
            arguments( "relationships", true ),
            arguments( "*,!relationships", false ),
            arguments( "relationships[*]", true ),
            arguments( "relationships[*],!relationships[*]", false ),
            arguments( "!relationships[*],relationships[*]", false ),
            arguments( "relationships,relationships[!from]", true ) );
    }

    @MethodSource
    @ParameterizedTest
    void getEventParamsMultipleCases( String fields, boolean expectRelationships )
    {
        EventParams params = eventFieldsParamMapper.map( FieldFilterParser.parse( fields ) );

        assertEquals( expectRelationships, params.isIncludeRelationships() );
    }
}