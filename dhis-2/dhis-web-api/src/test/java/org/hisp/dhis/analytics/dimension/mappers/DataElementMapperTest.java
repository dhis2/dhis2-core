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
package org.hisp.dhis.analytics.dimension.mappers;

import static org.hisp.dhis.analytics.dimension.DimensionMapperTestSupport.asserter;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.webapi.dimension.DimensionResponse;
import org.hisp.dhis.webapi.dimension.mappers.DataElementMapper;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class DataElementMapperTest
{

    private static final DimensionItemType DIMENSION_ITEM_TYPE = DimensionItemType.DATA_ELEMENT;

    @Test
    void testDataElementObjectMapperId()
    {
        asserter( new DataElementMapper(),
            DataElement::new,
            ImmutableList.of(
                b -> b.setDimensionItemType( DIMENSION_ITEM_TYPE ),
                b -> b.setValueType( ValueType.TEXT ),
                b -> b.setUid( "DE_ID" ) ),
            ImmutableList.of(
                Pair.of( DimensionResponse::getDimensionType, DIMENSION_ITEM_TYPE ),
                Pair.of( DimensionResponse::getId, "PROGRAM_STAGE_ID.DE_ID" ) ),
            "PROGRAM_STAGE_ID" );
    }

}
