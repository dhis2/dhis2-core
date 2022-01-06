/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.dimension.mappers;

import static org.hisp.dhis.analytics.dimension.DimensionMapperTestSupport.asserter;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.dimension.DimensionResponse;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ProgramStageDataElementMapperTest
{

    private static final ValueType VALUE_TYPE = ValueType.TEXT;

    private static final String DATA_ELEMENT_UID = "DEUID";

    private static final String PROGRAM_STAGE_UID = "PSUID";

    @Test
    public void testProgramStageDataElementMapper()
    {
        asserter( new ProgramStageDataElementMapper(),
            ProgramStageDataElement::new,
            ImmutableList.of( this::setDataElement, this::setProgramStage ),
            ImmutableList.of(
                Pair.of( DimensionResponse::getValueType, VALUE_TYPE ),
                Pair.of( DimensionResponse::getId, PROGRAM_STAGE_UID + "." + DATA_ELEMENT_UID ) ) );
    }

    private void setDataElement( ProgramStageDataElement programStageDataElement )
    {
        DataElement dataElement = new DataElement();
        dataElement.setUid( DATA_ELEMENT_UID );
        dataElement.setValueType( VALUE_TYPE );
        programStageDataElement.setDataElement( dataElement );
    }

    private void setProgramStage( ProgramStageDataElement programStageDataElement )
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE_UID );
        programStageDataElement.setProgramStage( programStage );
    }
}
