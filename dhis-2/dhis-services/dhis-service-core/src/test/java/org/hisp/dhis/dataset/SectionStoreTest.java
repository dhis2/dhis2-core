package org.hisp.dhis.dataset;

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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class SectionStoreTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private SectionStore sectionStore;
    
    private DataSet dataSet;
    
    private Section sectionA;
    private Section sectionB;
    private Section sectionC;
    
    @Override
    public void setUpTest()
    {
        dataSet = createDataSet( 'A', new MonthlyPeriodType() );
        dataSetService.addDataSet( dataSet );
        
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        
        List<DataElement> dataElements = new ArrayList<>();
        dataElements.add( dataElementA );
        dataElements.add( dataElementB );

        sectionA = new Section( "SectionA", dataSet, dataElements, null );
        sectionB = new Section( "SectionB", dataSet, dataElements, null );
        sectionC = new Section( "SectionC", dataSet, dataElements, null );        
    }
    
    @Test
    public void testAddGet()
    {        
        sectionStore.save( sectionA );
        int idA = sectionA.getId();
        sectionStore.save( sectionB );
        int idB = sectionB.getId();
        sectionStore.save( sectionC );
        int idC = sectionC.getId();

        assertEquals( sectionA, sectionStore.get( idA ) );
        assertEquals( sectionB, sectionStore.get( idB ) );
        assertEquals( sectionC, sectionStore.get( idC ) );

        assertEquals( dataSet, sectionStore.get( idA ).getDataSet() );
        assertNotNull( sectionStore.get( idA ).getDataElements() );
        assertEquals( 2, sectionStore.get( idA ).getDataElements().size() );
    }
    
    @Test
    public void testDelete()
    {
        sectionStore.save( sectionA );
        int idA = sectionA.getId();
        sectionStore.save( sectionB );
        int idB = sectionB.getId();
        sectionStore.save( sectionC );
        int idC = sectionC.getId();

        assertNotNull( sectionStore.get( idA ) );
        assertNotNull( sectionStore.get( idB ) );
        assertNotNull( sectionStore.get( idC ) );
        
        sectionStore.delete( sectionA );
        
        assertNull( sectionStore.get( idA ) );
        assertNotNull( sectionStore.get( idB ) );
        assertNotNull( sectionStore.get( idC ) );
        
        sectionStore.delete( sectionB );
        
        assertNull( sectionStore.get( idA ) );
        assertNull( sectionStore.get( idB ) );
        assertNotNull( sectionStore.get( idC ) );       

        sectionStore.delete( sectionC );
        
        assertNull( sectionStore.get( idA ) );
        assertNull( sectionStore.get( idB ) );
        assertNull( sectionStore.get( idC ) );   
    }
}
