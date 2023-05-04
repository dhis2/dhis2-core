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
package org.hisp.dhis.tracker.imports.preheat.mappers;

import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.attributeValue;
import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.attributeValues;
import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.setIdSchemeFields;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

class ProgramInstanceMapperTest
{

    @Test
    void testIdSchemeRelatedFieldsAreMapped()
    {

        Program program = setIdSchemeFields(
            new Program(),
            "WTTYiPQDqh1",
            "friendship",
            "red",
            attributeValues( "m0GpPuMUfFW", "yellow" ) );
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( program );

        Enrollment mapped = ProgramInstanceMapper.INSTANCE.map( enrollment );

        assertEquals( "WTTYiPQDqh1", mapped.getProgram().getUid() );
        assertEquals( "friendship", mapped.getProgram().getName() );
        assertEquals( "red", mapped.getProgram().getCode() );
        assertContainsOnly( Set.of( attributeValue( "m0GpPuMUfFW", "yellow" ) ),
            mapped.getProgram().getAttributeValues() );
    }
}