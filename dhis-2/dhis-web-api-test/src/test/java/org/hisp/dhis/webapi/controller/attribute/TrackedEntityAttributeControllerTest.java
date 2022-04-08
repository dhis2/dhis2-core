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
package org.hisp.dhis.webapi.controller.attribute;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.http.HttpStatus;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * @author Luca Cambi
 */
class TrackedEntityAttributeControllerTest extends DhisWebSpringTest
{
    @Test
    void shouldGenerateRandomValuesOrgUnitCodeAndRandom()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttribute.setGenerated( true );

        String pattern = "ORG_UNIT_CODE() + RANDOM(#######)";

        TextPattern textPattern = TextPatternParser.parse( pattern );

        textPattern.setOwnerObject( Objects.fromClass( trackedEntityAttribute.getClass() ) );
        textPattern.setOwnerUid( trackedEntityAttribute.getUid() );

        trackedEntityAttribute.setTextPattern( textPattern );
        trackedEntityAttribute.setPattern( pattern );

        mvc.perform( post( TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( TestUtils.convertObjectToJsonBytes( trackedEntityAttribute ) ) )
            .andExpect( status().is( HttpStatus.SC_CREATED ) );

        mvc.perform( get( TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT + "/" + trackedEntityAttribute.getUid()
            + "/generateAndReserve" ).param( "ORG_UNIT_CODE", "A030101" ).session( session ) )
            .andExpect( status().isOk() );
    }

}
