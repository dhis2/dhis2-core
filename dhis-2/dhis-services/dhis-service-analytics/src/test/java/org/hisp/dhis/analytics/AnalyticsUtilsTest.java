package org.hisp.dhis.analytics;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElement;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.grid.ListGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtilsTest 
    extends DhisSpringTest
{
    @Test
    public void testGetByDataDimensionType()
    {
        Program prA = createProgram( 'A' );
        
        DataElement deA = createDataElement( 'A', new DataElementCategoryCombo() );
        DataElement deB = createDataElement( 'B', new DataElementCategoryCombo() );
        ProgramDataElement pdeA = new ProgramDataElement( prA, deA );
        ProgramDataElement pdeB = new ProgramDataElement( prA, deB );
        ProgramIndicator piA = createProgramIndicator( 'A', prA, null, null );
        
        List<DimensionalItemObject> list = Lists.newArrayList( deA, deB, pdeA, pdeB, piA );
        
        assertEquals( Lists.newArrayList( deA, deB ), AnalyticsUtils.getByDataDimensionItemType( DataDimensionItemType.DATA_ELEMENT, list ) );
        assertEquals( Lists.newArrayList( pdeA, pdeB ), AnalyticsUtils.getByDataDimensionItemType( DataDimensionItemType.PROGRAM_DATA_ELEMENT, list ) );
        assertEquals( Lists.newArrayList( piA ), AnalyticsUtils.getByDataDimensionItemType( DataDimensionItemType.PROGRAM_INDICATOR, list ) );
        assertEquals( Lists.newArrayList(), AnalyticsUtils.getByDataDimensionItemType( DataDimensionItemType.PROGRAM_ATTRIBUTE, list ) );
    }
    
    @Test
    public void testConvertDxToOperand()
    {
        Map<String, Double> map = new HashMap<>();
        map.put( "GauDLAiXPKT-kC1OT9Q1n1j-R9U8q7X1aJG", 10d );
        map.put( "YkRvCLedQa4-h1dJ9W4dWor-Zrd4DAf8M99", 11d );
        map.put( "PcfRp1HETO8-zqXKIEycBck-KBJBZopYMPV", 12d );
        
        Map<String, Double> convertedMap = AnalyticsUtils.convertDxToOperand( map );
        
        assertTrue( convertedMap.containsKey( "GauDLAiXPKT.kC1OT9Q1n1j-R9U8q7X1aJG" ) );
        assertTrue( convertedMap.containsKey( "YkRvCLedQa4.h1dJ9W4dWor-Zrd4DAf8M99" ) );
        assertTrue( convertedMap.containsKey( "PcfRp1HETO8.zqXKIEycBck-KBJBZopYMPV" ) );
    }
    
    @Test
    public void testGetRoundedValueObject()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder().build();
        DataQueryParams paramsB = DataQueryParams.newBuilder().withSkipRounding( true ).build();
        
        assertEquals( null, AnalyticsUtils.getRoundedValueObject( paramsA, null ) );
        assertEquals( "Car", AnalyticsUtils.getRoundedValueObject( paramsA, "Car" ) );
        assertEquals( 3d, AnalyticsUtils.getRoundedValueObject( paramsA, 3d ) );
        assertEquals( 3.1, (Double) AnalyticsUtils.getRoundedValueObject( paramsA, 3.123 ), 0.01 );
        assertEquals( 3.123, (Double) AnalyticsUtils.getRoundedValueObject( paramsB, 3.123 ), 0.01 );
    }

    @Test
    public void testGetRoundedValueDouble()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder().build();
        DataQueryParams paramsB = DataQueryParams.newBuilder().withSkipRounding( true ).build();
        
        assertEquals( null, AnalyticsUtils.getRoundedValue( paramsA, null, null ) );
        assertEquals( 3d, AnalyticsUtils.getRoundedValue( paramsA, null, 3d ), 0.01 );
        assertEquals( 3.1, AnalyticsUtils.getRoundedValue( paramsA, null, 3.123 ), 0.01 );
        assertEquals( 3.1, AnalyticsUtils.getRoundedValue( paramsA, 1, 3.123 ), 0.01 );
        assertEquals( 3.12, AnalyticsUtils.getRoundedValue( paramsA, 2, 3.123 ), 0.01 );
        assertEquals( 3.123, AnalyticsUtils.getRoundedValue( paramsB, 2, 3.123 ), 0.01 );
    }
    
    @Test
    public void testGetAggregatedDataValueMapping()
    {
        Grid grid = new ListGrid();
        
        grid.addRow();
        grid.addValue( "de1" );
        grid.addValue( "ou2" );
        grid.addValue( "pe1" );
        grid.addValue( 3 );

        grid.addRow();
        grid.addValue( "de2" );
        grid.addValue( "ou3" );
        grid.addValue( "pe2" );
        grid.addValue( 5 );
        
        Map<String, Object> map = AnalyticsUtils.getAggregatedDataValueMapping( grid );
        
        assertEquals( 3, map.get( "de1" + DIMENSION_SEP + "ou2" + DIMENSION_SEP + "pe1" ) );
        assertEquals( 5, map.get( "de2" + DIMENSION_SEP + "ou3" + DIMENSION_SEP + "pe2" ) );
    }
    
    @Test
    public void testGetDimensionalItemObjectMap()
    {
        DataElement deA = createDataElement( 'A' );
        Indicator inA = createIndicator( 'A', null );
        DataSet dsA = createDataSet( 'A' );

        DimensionalObject dx = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, inA, dsA ) );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( dx )
            .withDisplayProperty( DisplayProperty.NAME )
            .build();
        
        Map<String, DimensionalItemObject> map = AnalyticsUtils.getDimensionalItemObjectMap( params );
        
        assertEquals( map.get( deA.getDimensionItem() ), deA );
        assertEquals( map.get( inA.getDimensionItem() ), inA );
        assertEquals( map.get( dsA.getDimensionItem() ), dsA );
    }

    @Test
    public void testGetDimensionalItemObjectMapIdScheme()
    {
        IdScheme idScheme = IdScheme.CODE;
        
        DataElement deA = createDataElement( 'A' );
        Indicator inA = createIndicator( 'A', null );
        DataSet dsA = createDataSet( 'A' );

        DimensionalObject dx = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, inA, dsA ) );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( dx )
            .withDisplayProperty( DisplayProperty.NAME )
            .withOutputIdScheme( IdScheme.CODE )
            .build();
        
        Map<String, DimensionalItemObject> map = AnalyticsUtils.getDimensionalItemObjectMap( params );
        
        assertEquals( map.get( deA.getDimensionItem( idScheme ) ), deA );
        assertEquals( map.get( inA.getDimensionItem( idScheme ) ), inA );
        assertEquals( map.get( dsA.getDimensionItem( idScheme ) ), dsA );

        assertEquals( map.get( "DataElementCodeA" ), deA );
        assertEquals( map.get( "IndicatorCodeA" ), inA );
        assertEquals( map.get( "DataSetCodeA" ), dsA );
    }
    
    @Test
    public void testGetDimensionItemNameMap()
    {
        DataElement deA = createDataElement( 'A' );
        Indicator inA = createIndicator( 'A', null );
        DataSet dsA = createDataSet( 'A' );
        
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        
        DimensionalObject dx = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, inA, dsA ) );
        DimensionalObject ou = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouA, ouB ) );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( dx )
            .addDimension( ou )
            .withDisplayProperty( DisplayProperty.NAME )
            .build();
        
        Map<String, String> map = AnalyticsUtils.getDimensionItemNameMap( params );
        
        assertEquals( map.get( deA.getDimensionItem() ), deA.getDisplayName() );
        assertEquals( map.get( inA.getDimensionItem() ), inA.getDisplayName() );
        assertEquals( map.get( dsA.getDimensionItem() ), dsA.getDisplayName() );
        assertEquals( map.get( ouA.getDimensionItem() ), ouA.getDisplayName() );
        assertEquals( map.get( ouB.getDimensionItem() ), ouB.getDisplayName() );
    }
    
    @Test
    public void testGetCocNameMap()
    {
        DataElementCategoryCombo ccA = createCategoryCombo( 'A', new DataElementCategory[0] );
        DataElementCategoryCombo ccB = createCategoryCombo( 'B', new DataElementCategory[0] );
        
        DataElementCategoryOptionCombo cocA = createCategoryOptionCombo( 'A' );
        DataElementCategoryOptionCombo cocB = createCategoryOptionCombo( 'B' );
        
        ccA.getOptionCombos().add( cocA );
        ccB.getOptionCombos().add( cocB );
        
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        
        deA.setDataElementCategoryCombo( ccA );
        deB.setDataElementCategoryCombo( ccB );
        
        DimensionalObject dx = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( dx )
            .withDisplayProperty( DisplayProperty.NAME )
            .build();
        
        Map<String, String> map = AnalyticsUtils.getCocNameMap( params );
        
        assertEquals( map.get( cocA.getUid() ), cocA.getName() );
        assertEquals( map.get( cocB.getUid() ), cocB.getName() );
    }
    
    @Test
    public void testGetDataValueSetFromGrid()
    {
        DataElement dxB = createDataElement( 'B' );
        dxB.setUid( "dxB" );
        dxB.setValueType( ValueType.NUMBER );
        dxB.setAggregateExportCategoryOptionCombo( "coA" );
        
        DataElement dxC = createDataElement( 'C' );
        dxC.setUid( "dxC" );
        dxC.setValueType( ValueType.INTEGER );
        dxC.setAggregateExportAttributeOptionCombo( "aoA" );
        
        IndicatorType itA = new IndicatorType();
        
        Indicator dxD = createIndicator( 'D', itA );
        dxD.setUid( "dxD" );
        dxD.setDecimals( 2 );
        dxD.setAggregateExportCategoryOptionCombo( "coB" );

        Indicator dxE = createIndicator( 'E', itA );
        dxE.setUid( "dxE" );
        dxE.setDecimals( 0 );

        Map<String, DimensionalItemObject> itemMap = Maps.newHashMap();
        itemMap.put( dxB.getUid(), dxB );
        itemMap.put( dxC.getUid(), dxC );
        itemMap.put( dxD.getUid(), dxD );
        itemMap.put( dxE.getUid(), dxE );

        Grid grid = new ListGrid();
        
        grid.getMetaData().put( AnalyticsMetaDataKey.DIMENSION_ITEMS.getKey(), itemMap );
        
        grid.addHeader( new GridHeader( DimensionalObject.DATA_X_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.ORGUNIT_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.PERIOD_DIM_ID ) );
        
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxA", "ouA", "peA", 1d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxA", "ouA", "peB", 2d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxA", "ouB", "peA", 3d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxA", "ouB", "peB", 4d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxB", "ouA", "peA", 5d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxB", "ouA", "peB", 6d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxC", "ouA", "peA", 7d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxC", "ouA", "peB", 8d ) );

        grid.addRow().addValuesAsList( Lists.newArrayList( "dxD", "ouA", "peA", 9d ) );
        grid.addRow().addValuesAsList( Lists.newArrayList( "dxE", "ouA", "peB", 10d ) );
        
        DataValueSet dvs = AnalyticsUtils.getDataValueSetFromGrid( grid );
        
        assertNotNull( dvs );
        assertNotNull( dvs.getDataValues() );
        assertEquals( 10, dvs.getDataValues().size() );
        
        assertEquals( "dxA", dvs.getDataValues().get( 1 ).getDataElement() );
        assertEquals( "ouA", dvs.getDataValues().get( 1 ).getOrgUnit() );
        assertEquals( "peB", dvs.getDataValues().get( 1 ).getPeriod() );
        assertNull( dvs.getDataValues().get( 1 ).getCategoryOptionCombo() );
        assertNull( dvs.getDataValues().get( 1 ).getAttributeOptionCombo() );
        assertEquals( "2.0", dvs.getDataValues().get( 1 ).getValue() );
        
        assertEquals( "dxB", dvs.getDataValues().get( 4 ).getDataElement() );
        assertEquals( "ouA", dvs.getDataValues().get( 4 ).getOrgUnit() );
        assertEquals( "peA", dvs.getDataValues().get( 4 ).getPeriod() );
        assertEquals( "coA", dvs.getDataValues().get( 4 ).getCategoryOptionCombo() );
        assertNull( dvs.getDataValues().get( 4 ).getAttributeOptionCombo() );
        assertEquals( "5.0", dvs.getDataValues().get( 4 ).getValue() );

        assertEquals( "dxC", dvs.getDataValues().get( 6 ).getDataElement() );
        assertEquals( "ouA", dvs.getDataValues().get( 6 ).getOrgUnit() );
        assertEquals( "peA", dvs.getDataValues().get( 6 ).getPeriod() );
        assertNull( dvs.getDataValues().get( 6 ).getCategoryOptionCombo() );
        assertEquals( "aoA", dvs.getDataValues().get( 6 ).getAttributeOptionCombo() );
        assertEquals( "7", dvs.getDataValues().get( 6 ).getValue() );

        assertEquals( "dxD", dvs.getDataValues().get( 8 ).getDataElement() );
        assertEquals( "ouA", dvs.getDataValues().get( 8 ).getOrgUnit() );
        assertEquals( "peA", dvs.getDataValues().get( 8 ).getPeriod() );
        assertEquals( "coB", dvs.getDataValues().get( 8 ).getCategoryOptionCombo() );
        assertNull( dvs.getDataValues().get( 8 ).getAttributeOptionCombo() );
        assertEquals( "9.0", dvs.getDataValues().get( 8 ).getValue() );

        assertEquals( "dxE", dvs.getDataValues().get( 9 ).getDataElement() );
        assertEquals( "ouA", dvs.getDataValues().get( 9 ).getOrgUnit() );
        assertEquals( "peB", dvs.getDataValues().get( 9 ).getPeriod() );
        assertNull( dvs.getDataValues().get( 9 ).getCategoryOptionCombo() );
        assertNull( dvs.getDataValues().get( 9 ).getAttributeOptionCombo() );
        assertEquals( "10", dvs.getDataValues().get( 9 ).getValue() );        
    }
}
