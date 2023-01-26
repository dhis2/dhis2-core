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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.analytics.AggregationType;
import org.junit.jupiter.api.Test;

class ValueTypeTest
{
    @Test
    void rightInstancesOfEnumsAreConstructedWhenUsed()
    {
        ValueType posInt = ValueType.INTEGER_POSITIVE;
        ValueType longText = ValueType.LONG_TEXT;

        assertTrue( posInt.isNumeric() );
        assertTrue( longText.isText() );
    }

    @Test
    void aggregatableFlagOfTextValueTypeIsTrueWhenCalled()
    {
        assertTrue( ValueType.TEXT.isAggregatable( AggregationType.NONE ) );
        assertTrue( ValueType.TEXT.isAggregatable( AggregationType.LAST_LAST_ORG_UNIT ) );
        assertTrue( ValueType.TEXT.isAggregatable( AggregationType.FIRST_FIRST_ORG_UNIT ) );
        assertTrue( ValueType.LONG_TEXT.isAggregatable( AggregationType.COUNT ) );
        assertTrue( ValueType.LETTER.isAggregatable( AggregationType.SUM ) );
    }

    @Test
    void aggregatableFlagOfTextValueTypeIsFalseWhenCalled()
    {
        assertFalse( ValueType.TEXT.isAggregatable( AggregationType.COUNT ) );
        assertFalse( ValueType.TEXT.isAggregatable( AggregationType.SUM ) );
        assertFalse( ValueType.LONG_TEXT.isAggregatable( AggregationType.CUSTOM ) );
        assertFalse( ValueType.LETTER.isAggregatable( AggregationType.DEFAULT ) );
    }

    @Test
    void aggregatableFlagOfNumericValueTypeIsTrueWhenCalled()
    {
        assertTrue( ValueType.NUMBER.isAggregatable( AggregationType.COUNT ) );
        assertTrue( ValueType.UNIT_INTERVAL.isAggregatable( AggregationType.AVERAGE ) );
        assertTrue( ValueType.PERCENTAGE.isAggregatable( AggregationType.LAST ) );
        assertTrue( ValueType.INTEGER_POSITIVE.isAggregatable( AggregationType.LAST ) );
        assertTrue( ValueType.INTEGER_NEGATIVE.isAggregatable( AggregationType.LAST ) );
        assertTrue( ValueType.INTEGER_ZERO_OR_POSITIVE.isAggregatable( AggregationType.LAST ) );
    }

    @Test
    void aggregatableFlagOfNumericValueTypeIsFalseWhenCalled()
    {
        assertFalse( ValueType.NUMBER.isAggregatable( AggregationType.NONE ) );
        assertFalse( ValueType.UNIT_INTERVAL.isAggregatable( AggregationType.NONE ) );
        assertFalse( ValueType.PERCENTAGE.isAggregatable( AggregationType.DEFAULT ) );
        assertFalse( ValueType.INTEGER_POSITIVE.isAggregatable( AggregationType.CUSTOM ) );
        assertFalse( ValueType.INTEGER_NEGATIVE.isAggregatable( AggregationType.NONE ) );
        assertFalse( ValueType.INTEGER_ZERO_OR_POSITIVE.isAggregatable( AggregationType.NONE ) );
    }

    @Test
    void aggregatableFlagOfFileResourceValueTypeIsTrueWhenCalled()
    {
        assertTrue( ValueType.FILE_RESOURCE.isAggregatable( AggregationType.COUNT ) );
    }

    @Test
    void aggregatableFlagOfFileResourceValueTypeIsFalseWhenCalled()
    {
        assertFalse( ValueType.FILE_RESOURCE.isAggregatable( AggregationType.AVERAGE ) );
        assertFalse( ValueType.FILE_RESOURCE.isAggregatable( AggregationType.NONE ) );
        assertFalse( ValueType.FILE_RESOURCE.isAggregatable( AggregationType.DEFAULT ) );
    }

    @Test
    void referenceTypeTest()
    {
        assertFalse( ValueType.REFERENCE.isAggregatable() );
        assertTrue( ValueType.REFERENCE.isReference() );
        assertFalse( ValueType.NUMBER.isReference() );
    }
}
