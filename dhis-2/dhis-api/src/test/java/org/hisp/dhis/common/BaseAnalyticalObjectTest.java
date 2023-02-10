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

import static org.hisp.dhis.common.AnalyticsType.EVENT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.mapping.MapView.LAYER_THEMATIC1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
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
        tea.setValueType( TEXT );
        tea.setOptionSet( new OptionSet( "name", BOOLEAN ) );

        TrackedEntityAttributeDimension tead = new TrackedEntityAttributeDimension( tea, null, "EQ:10" );

        EventChart eventChart = new EventChart();
        eventChart.setAutoFields();
        eventChart.getColumnDimensions().add( tea.getUid() );
        eventChart.getAttributeDimensions().add( tead );
        eventChart.populateAnalyticalProperties();

        assertEquals( 1, eventChart.getColumns().size() );

        DimensionalObject dim = eventChart.getColumns().get( 0 );

        assertNotNull( dim );
        assertEquals( PROGRAM_ATTRIBUTE, dim.getDimensionType() );
        assertEquals( EVENT, dim.getAnalyticsType() );
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

        assertEquals( deA, deC );
        assertNotEquals( deA, deB );
        assertNotEquals( dsA, dsD );
    }

    @Test
    void testAddDataDimensionItem()
    {
        DataElement deA = new DataElement();
        deA.setAutoFields();

        MapView mv = new MapView( LAYER_THEMATIC1 );
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

        MapView mv = new MapView( LAYER_THEMATIC1 );
        mv.addDataDimensionItem( deA );
        mv.addDataDimensionItem( deB );

        assertEquals( 2, mv.getDataDimensionItems().size() );

        mv.removeDataDimensionItem( deA );

        assertEquals( 1, mv.getDataDimensionItems().size() );
        assertEquals( deB, mv.getDataDimensionItems().get( 0 ).getDataElement() );
    }

    @Test
    void testGetDimensionalObjectForDimensionUid()
    {
        String dimensionUid = "abc123abc11";
        String dimensionName = "anyName";
        String dimensionFilter = "gt:71";
        ProgramStage programStage = new ProgramStage( "anyName", null );

        TrackedEntityDataElementDimension dataElementDimension = stubTrackedEntityDataElementDimension( dimensionName,
            dimensionUid, dimensionFilter );
        dataElementDimension.setProgramStage( programStage );

        List<TrackedEntityDataElementDimension> trackedEntityDataElementDimensions = List.of( dataElementDimension );

        EventVisualization ev = new EventVisualization( "anyName" );
        ev.setDataElementDimensions( trackedEntityDataElementDimensions );

        Optional<DimensionalObject> result = ev.getDimensionalObject( dimensionUid );

        assertNotNull( result.get(), "Must have a result." );

        BaseDimensionalObject baseDimensionalObject = (BaseDimensionalObject) result.get();

        assertEquals( PROGRAM_DATA_ELEMENT, baseDimensionalObject.getDimensionType() );
        assertEquals( dimensionUid, baseDimensionalObject.getDimension() );
        assertEquals( dimensionFilter, baseDimensionalObject.getFilter() );
        assertNull( baseDimensionalObject.getDisplayName() );
        assertNotNull( baseDimensionalObject.getProgramStage() );
        assertEquals( dataElementDimension.getDataElement().getValueType(), baseDimensionalObject.getValueType() );
    }

    @Test
    void testPopulateDimensionsForSameDimensionDifferentStages()
    {
        String sameDimensionUid = "abc123abc11";
        String dimensionName = "anyName";
        String dimensionFilter = "gt:71";
        ProgramStage programStage1 = new ProgramStage( "anyStage1", null );
        programStage1.setUid( "ps1234abc11" );

        ProgramStage programStage2 = new ProgramStage( "anyStage2", null );
        programStage1.setUid( "ps1234abc22" );

        TrackedEntityDataElementDimension dataElementDimension1 = stubTrackedEntityDataElementDimension( dimensionName,
            sameDimensionUid, dimensionFilter );
        dataElementDimension1.setProgramStage( programStage1 );

        TrackedEntityDataElementDimension dataElementDimension2 = stubTrackedEntityDataElementDimension( dimensionName,
            sameDimensionUid, dimensionFilter );
        dataElementDimension2.setProgramStage( programStage2 );

        List<TrackedEntityDataElementDimension> trackedEntityDataElementDimensions = List.of( dataElementDimension1,
            dataElementDimension2 );

        List<String> sameDimensions = List.of( sameDimensionUid, sameDimensionUid );
        List<DimensionalObject> dimensionalObjects = new ArrayList<>();

        EventVisualization ev = new EventVisualization( "anyName" );
        ev.setDataElementDimensions( trackedEntityDataElementDimensions );

        ev.populateDimensions( sameDimensions, dimensionalObjects );

        BaseDimensionalObject baseDimensionalObject1 = (BaseDimensionalObject) dimensionalObjects.get( 0 );
        BaseDimensionalObject baseDimensionalObject2 = (BaseDimensionalObject) dimensionalObjects.get( 1 );

        assertEquals( 2, dimensionalObjects.size() );
        assertEquals( PROGRAM_DATA_ELEMENT, baseDimensionalObject1.getDimensionType() );
        assertEquals( sameDimensionUid, baseDimensionalObject1.getDimension() );
        assertEquals( dimensionFilter, baseDimensionalObject1.getFilter() );
        assertNull( baseDimensionalObject1.getDisplayName() );
        assertNotNull( baseDimensionalObject1.getProgramStage() );
        assertEquals( programStage1, baseDimensionalObject1.getProgramStage() );
        assertEquals( dataElementDimension1.getDataElement().getValueType(), baseDimensionalObject1.getValueType() );

        assertEquals( PROGRAM_DATA_ELEMENT, baseDimensionalObject2.getDimensionType() );
        assertEquals( sameDimensionUid, baseDimensionalObject2.getDimension() );
        assertNotNull( baseDimensionalObject2.getProgramStage() );
        assertEquals( programStage2, baseDimensionalObject2.getProgramStage() );
    }

    private TrackedEntityDataElementDimension stubTrackedEntityDataElementDimension( String deName, String deUid,
        String deFilter )
    {
        DataElement dataElement = new DataElement( deName );
        dataElement.setUid( deUid );
        dataElement.setValueType( INTEGER );

        return new TrackedEntityDataElementDimension( dataElement, null, null, deFilter );
    }
}
