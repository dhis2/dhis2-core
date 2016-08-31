package org.hisp.dhis.common;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.junit.Test;

import static org.junit.Assert.*;

/**
* @author Lars Helge Overland
*/
public class BaseAnalyticalObjectTest
{
    @Test
    public void testSortKeys()
    {
        Map<String, Object> valueMap = new HashMap<>();
        
        valueMap.put( "b1-a1-c1", 1d );
        valueMap.put( "a2-c2-b2", 2d );
        valueMap.put( "c3-b3-a3", 3d );
        valueMap.put( "a4-b4-c4", 4d );
        
        BaseAnalyticalObject.sortKeys( valueMap );
        
        assertEquals( 4, valueMap.size() );
        assertTrue( valueMap.containsKey( "a1-b1-c1" ) );
        assertTrue( valueMap.containsKey( "a2-b2-c2" ) );
        assertTrue( valueMap.containsKey( "a3-b3-c3" ) );
        assertTrue( valueMap.containsKey( "a4-b4-c4" ) );
        
        Object d1 = 1d;
        Object d2 = 2d;
        Object d3 = 3d;
        Object d4 = 4d;
        
        assertEquals( d1, valueMap.get( "a1-b1-c1" ) );
        assertEquals( d2, valueMap.get( "a2-b2-c2" ) );
        assertEquals( d3, valueMap.get( "a3-b3-c3" ) );
        assertEquals( d4, valueMap.get( "a4-b4-c4" ) );
        
        valueMap = new HashMap<>();
        
        valueMap.put( "b1", 1d );
        valueMap.put( "b2", 2d );

        BaseAnalyticalObject.sortKeys( valueMap );

        assertEquals( 2, valueMap.size() );
        assertTrue( valueMap.containsKey( "b1" ) );
        assertTrue( valueMap.containsKey( "b2" ) );
        
        assertEquals( d1, valueMap.get( "b1" ) );
        assertEquals( d2, valueMap.get( "b2" ) );

        valueMap = new HashMap<>();
        
        valueMap.put( null, 1d );
        
        BaseAnalyticalObject.sortKeys( valueMap );

        assertEquals( 0, valueMap.size() );
    }
    
    @Test
    public void testSortKey()
    {
        String expected = "a-b-c";
        assertEquals( expected, BaseAnalyticalObject.sortKey( "b-c-a" ) );
    }
    
    @Test
    public void testPopulateAnalyticalProperties()
    {
        TrackedEntityAttribute tea = new TrackedEntityAttribute();
        tea.setAutoFields();

        TrackedEntityAttributeDimension tead = new TrackedEntityAttributeDimension( tea, null, "EQ:10" );

        EventChart eventChart = new EventChart();
        eventChart.setAutoFields();
        eventChart.getColumnDimensions().add( tea.getUid() );
        eventChart.getAttributeDimensions().add( tead );
        
        eventChart.populateAnalyticalProperties();
        
        assertEquals( 1, eventChart.getColumns().size() );
        
        DimensionalObject dim = eventChart.getColumns().get( 0 );
        
        assertNotNull( dim );
        assertEquals( DimensionType.PROGRAM_ATTRIBUTE, dim.getDimensionType() );
        assertEquals( AnalyticsType.EVENT, dim.getAnalyticsType() );
        assertEquals( tead.getFilter(), dim.getFilter() );
    }

    @Test
    public void testGetIdentifier()
    {
        DataElementGroup oA = new DataElementGroup();
        DataElementGroup oB = new DataElementGroup();
        DataElementGroup oC = new DataElementGroup();
        
        oA.setUid( "a1" );
        oB.setUid( "b1" );
        oC.setUid( "c1" );
        
        List<DimensionalItemObject> column = new ArrayList<>();
        column.add( oC );
        column.add( oA );
        
        List<DimensionalItemObject> row = new ArrayList<>();
        row.add( oB );
        
        assertEquals( "a1-b1-c1", BaseAnalyticalObject.getIdentifier( column, row ) );
        assertEquals( "b1", BaseAnalyticalObject.getIdentifier( new ArrayList<>(), row ) );
        assertEquals( "b1", BaseAnalyticalObject.getIdentifier( null, row ) );
    }
    
    @Test
    public void testEquals()
    {
        DataElement deA = new DataElement();
        deA.setUid( "A" );
        deA.setCode( "A" );
        deA.setName( "A" );

        DataElement deB = new DataElement();
        deB.setUid( "B" );
        deB.setCode( "B" );
        deB.setName( "B" );

        DataElement deC = new DataElement();
        deC.setUid( "A" );
        deC.setCode( "A" );
        deC.setName( "A" );
        
        DataSet dsA = new DataSet();
        dsA.setUid( "A" );
        dsA.setCode( "A" );
        dsA.setName( "A" );

        DataSet dsD = new DataSet();
        dsD.setUid( "D" );
        dsD.setCode( "D" );
        dsD.setName( "D" );
        
        assertTrue( deA.equals( deC ) );
        
        assertFalse( deA.equals( deB ) );
        assertFalse( deA.equals( dsA ) );
        assertFalse( deA.equals( dsD ) );
        assertFalse( dsA.equals( dsD ) );        
    }
    
    @Test
    public void testAddDataDimensionItem()
    {        
        DataElement deA = new DataElement();
        deA.setAutoFields();

        MapView mv = new MapView( MapView.LAYER_THEMATIC1 );
        
        mv.addDataDimensionItem( deA );
        
        assertEquals( 1, mv.getDataDimensionItems().size() );        
    }

    @Test
    public void testRemoveDataDimensionItem()
    {        
        DataElement deA = new DataElement();
        DataElement deB = new DataElement();
        deA.setAutoFields();
        deB.setAutoFields();

        MapView mv = new MapView( MapView.LAYER_THEMATIC1 );
        
        mv.addDataDimensionItem( deA );
        mv.addDataDimensionItem( deB );
        
        assertEquals( 2, mv.getDataDimensionItems().size() );
        
        mv.removeDataDimensionItem( deA );

        assertEquals( 1, mv.getDataDimensionItems().size() );
        assertEquals( deB, mv.getDataDimensionItems().get( 0 ).getDataElement() );        
    }
}
