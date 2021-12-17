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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class BaseAnalyticalObjectTest
{

    @Test
    void testPopulateAnalyticalProperties()
    {
        TrackedEntityAttribute tea = new TrackedEntityAttribute();
        tea.setAutoFields();
        tea.setValueType( ValueType.TEXT );
        tea.setOptionSet( new OptionSet( "name", ValueType.BOOLEAN ) );

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
        assertEquals( tead.getAttribute().getValueType(), dim.getValueType() );
        assertEquals( tead.getAttribute().getOptionSet(), dim.getOptionSet() );
    }

    @Test
    void testEquals()
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
        assertFalse( dsA.equals( dsD ) );
    }

    @Test
    void testAddDataDimensionItem()
    {
        DataElement deA = new DataElement();
        deA.setAutoFields();

        MapView mv = new MapView( MapView.LAYER_THEMATIC1 );
        mv.addDataDimensionItem( deA );

        assertEquals( 1, mv.getDataDimensionItems().size() );
    }

    @Test
    void testRemoveDataDimensionItem()
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
