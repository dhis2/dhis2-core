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

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class DataElementOperandServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementOperandService operandService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    
    private DataElementCategoryOptionCombo coc;
    
    @Override
    public void setUpTest()
    {
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        
        coc = categoryService.getDefaultDataElementCategoryOptionCombo();
    }
    
    @Test
    public void testAddGet()
    {
        DataElementOperand opA = new DataElementOperand( deA, coc );
        DataElementOperand opB = new DataElementOperand( deB, coc );
        DataElementOperand opC = new DataElementOperand( deC, coc );
        
        int idA = operandService.addDataElementOperand( opA );
        int idB = operandService.addDataElementOperand( opB );
        int idC = operandService.addDataElementOperand( opC );
        
        assertEquals( opA, operandService.getDataElementOperand( idA ) );
        assertEquals( opB, operandService.getDataElementOperand( idB ) );
        assertEquals( opC, operandService.getDataElementOperand( idC ) );
    }

    @Test
    public void testDelete()
    {
        DataElementOperand opA = new DataElementOperand( deA, coc );
        DataElementOperand opB = new DataElementOperand( deB, coc );
        DataElementOperand opC = new DataElementOperand( deC, coc );
        
        int idA = operandService.addDataElementOperand( opA );
        int idB = operandService.addDataElementOperand( opB );
        int idC = operandService.addDataElementOperand( opC );
        
        assertNotNull( operandService.getDataElementOperand( idA ) );
        assertNotNull( operandService.getDataElementOperand( idB ) );
        assertNotNull( operandService.getDataElementOperand( idC ) );
        
        operandService.deleteDataElementOperand( opA );
        operandService.deleteDataElementOperand( opB );

        assertNull( operandService.getDataElementOperand( idA ) );
        assertNull( operandService.getDataElementOperand( idB ) );
        assertNotNull( operandService.getDataElementOperand( idC ) );

        operandService.deleteDataElementOperand( opC );
        
        assertNull( operandService.getDataElementOperand( idA ) );
        assertNull( operandService.getDataElementOperand( idB ) );
        assertNull( operandService.getDataElementOperand( idC ) );
    }
    
    public void testGetByUid()
    {
        DataElementOperand opA = new DataElementOperand( deA, coc );
        DataElementOperand opB = new DataElementOperand( deB, coc );
        
        operandService.addDataElementOperand( opA );
        operandService.addDataElementOperand( opB );
        
        String uidA = deA.getUid() + DataElementOperand.SEPARATOR + coc.getUid();
        String uidB = deB.getUid() + DataElementOperand.SEPARATOR + coc.getUid();
        
        assertEquals( opA, operandService.getDataElementOperandByUid( uidA ) );
        assertEquals( opB, operandService.getDataElementOperandByUid( uidB ) );
        
        assertNull( operandService.getDataElementOperandByUid( null ) );
        assertNull( operandService.getDataElementOperandByUid( "SomeFunnyUid" ) );
    }
}
