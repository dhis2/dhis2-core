package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class OperandTest
{
    @Test
    public void testGetRelevantAggregationLevel()
    {
        DataElementOperand operand = new DataElementOperand( "a", "a", "Operand", null, new ArrayList<>(), 0 );

        assertNull( operand.getRelevantAggregationLevel( 1 ) );

        operand = new DataElementOperand( "a", "a", "Operand", null, Arrays.asList( 3, 5 ), 0 );

        assertEquals( new Integer( 3 ), operand.getRelevantAggregationLevel( 1 ) );
        assertEquals( new Integer( 3 ), operand.getRelevantAggregationLevel( 2 ) );
        assertEquals( new Integer( 3 ), operand.getRelevantAggregationLevel( 3 ) );
        assertEquals( new Integer( 5 ), operand.getRelevantAggregationLevel( 4 ) );
        assertEquals( new Integer( 5 ), operand.getRelevantAggregationLevel( 5 ) );
        assertNull( operand.getRelevantAggregationLevel( 6 ) );
    }

    @Test
    public void testAggregationLevelIsValid()
    {
        DataElementOperand operand = new DataElementOperand( "a", "a", "Operand", null, new ArrayList<>(), 0 );

        assertTrue( operand.aggregationLevelIsValid( 1, 3 ) );
        assertTrue( operand.aggregationLevelIsValid( 4, 3 ) );

        operand = new DataElementOperand( "a", "a", "Operand", null, Arrays.asList( 3, 5 ), 0 );

        assertTrue( operand.aggregationLevelIsValid( 2, 2 ) );
        assertTrue( operand.aggregationLevelIsValid( 2, 3 ) );
        assertFalse( operand.aggregationLevelIsValid( 2, 4 ) );

        assertTrue( operand.aggregationLevelIsValid( 3, 3 ) );
        assertFalse( operand.aggregationLevelIsValid( 3, 4 ) );

        assertTrue( operand.aggregationLevelIsValid( 4, 4 ) );
        assertTrue( operand.aggregationLevelIsValid( 4, 5 ) );
        assertFalse( operand.aggregationLevelIsValid( 4, 6 ) );

        assertTrue( operand.aggregationLevelIsValid( 5, 5 ) );
        assertFalse( operand.aggregationLevelIsValid( 5, 6 ) );

        assertTrue( operand.aggregationLevelIsValid( 6, 6 ) );
        assertTrue( operand.aggregationLevelIsValid( 6, 7 ) );
    }

    @Test
    public void testHashCode()
    {
        DataElement dataElementA = new DataElement();
        dataElementA.setUid( "DE_UID_AAA" );

        DataElement dataElementB = new DataElement();
        dataElementB.setUid( "DE_UID_BBB" );

        DataElementCategoryOptionCombo categoryOptionComboA = new DataElementCategoryOptionCombo();
        categoryOptionComboA.setUid( "COC_UID_AAA" );

        DataElementCategoryOptionCombo categoryOptionComboB = new DataElementCategoryOptionCombo();
        categoryOptionComboB.setUid( "COC_UID_BBB" );

        DataElementOperand dataElementOperandA = new DataElementOperand( dataElementA, categoryOptionComboA );
        DataElementOperand dataElementOperandB = new DataElementOperand( dataElementB, categoryOptionComboB );
        DataElementOperand dataElementOperandC = new DataElementOperand( dataElementA, categoryOptionComboB );
        DataElementOperand dataElementOperandD = new DataElementOperand( dataElementB, categoryOptionComboA );

        Set<DataElementOperand> dataElementOperands = new HashSet<>();
        dataElementOperands.add( dataElementOperandA );
        dataElementOperands.add( dataElementOperandB );
        dataElementOperands.add( dataElementOperandC );
        dataElementOperands.add( dataElementOperandD );

        assertEquals( 4, dataElementOperands.size() );
    }
    
    @Test
    public void testEquals()
    {
        DataElement dataElementA = new DataElement();
        dataElementA.setUid( "DE_UID_AAA" );

        DataElement dataElementB = new DataElement();
        dataElementB.setUid( "DE_UID_BBB" );

        DataElementCategoryOptionCombo categoryOptionComboA = new DataElementCategoryOptionCombo();
        categoryOptionComboA.setUid( "COC_UID_AAA" );

        DataElementCategoryOptionCombo categoryOptionComboB = new DataElementCategoryOptionCombo();
        categoryOptionComboB.setUid( "COC_UID_BBB" );

        DataElementOperand dataElementOperandA = new DataElementOperand( dataElementA, categoryOptionComboA );
        DataElementOperand dataElementOperandB = new DataElementOperand( dataElementB, categoryOptionComboB );
        DataElementOperand dataElementOperandC = new DataElementOperand( dataElementA, categoryOptionComboA );
        DataElementOperand dataElementOperandD = new DataElementOperand( dataElementB, categoryOptionComboB );
        
        assertEquals( dataElementOperandA, dataElementOperandC );
        assertEquals( dataElementOperandB, dataElementOperandD );
        assertNotEquals( dataElementOperandA, dataElementOperandB );
        assertNotEquals( dataElementOperandC, dataElementOperandD );

    }
}
