/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.common;

import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class MetadataItemTest
{
    @Test
    void testCreateForDataElement()
    {
        DataElement deA = createDataElement( 'A' );
        deA.setValueType( ValueType.INTEGER_ZERO_OR_POSITIVE );
        deA.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );

        MetadataItem miA = new MetadataItem( "MIA", deA );

        assertEquals( "MIA", miA.getName() );
        assertEquals( ValueType.NUMBER, miA.getValueType() );
        assertEquals( AggregationType.AVERAGE_SUM_ORG_UNIT, miA.getAggregationType() );
    }

    @Test
    void testCreateForDataElementOperand()
    {
        DataElement deA = createDataElement( 'A' );
        deA.setValueType( ValueType.BOOLEAN );
        deA.setAggregationType( AggregationType.COUNT );

        CategoryOptionCombo cocA = createCategoryOptionCombo( 'A' );

        DataElementOperand doA = new DataElementOperand( deA, cocA );

        MetadataItem miA = new MetadataItem( "MIA", doA );

        assertEquals( "MIA", miA.getName() );
        assertEquals( ValueType.BOOLEAN, miA.getValueType() );
        assertEquals( AggregationType.COUNT, miA.getAggregationType() );
    }
}
