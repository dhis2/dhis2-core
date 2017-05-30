package org.hisp.dhis.reporttable;

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

import static org.hisp.dhis.reporttable.ReportTable.getColumnName;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mock.MockI18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class ReportTableTest
    extends DhisSpringTest
{
    private List<DataElement> dataElements;
    private List<DataElementCategoryOptionCombo> categoryOptionCombos;
    private List<Indicator> indicators;
    private List<ReportingRate> reportingRates;
    private List<Period> periods;
    private List<OrganisationUnit> units;
    private List<OrganisationUnit> relativeUnits;
    private List<OrganisationUnitGroup> groups;

    private PeriodType montlyPeriodType;

    private DataElement dataElementA;
    private DataElement dataElementB;
    
    private DataElementGroupSet deGroupSetA;
    
    private DataElementGroup deGroupA;
    private DataElementGroup deGroupB;
    
    private DataElementCategoryOptionCombo categoryOptionComboA;
    private DataElementCategoryOptionCombo categoryOptionComboB;

    private DataElementCategoryCombo categoryCombo;
    
    private IndicatorType indicatorType;
    
    private Indicator indicatorA;
    private Indicator indicatorB;

    private ReportingRate reportingRateA;
    private ReportingRate reportingRateB;
    
    private Period periodA;
    private Period periodB;
    private Period periodC;
    private Period periodD;
    
    private OrganisationUnit unitA;
    private OrganisationUnit unitB;
    private OrganisationUnit unitC;
    
    private OrganisationUnitGroupSet ouGroupSetA;
    
    private OrganisationUnitGroup ouGroupA;
    private OrganisationUnitGroup ouGroupB;
    
    private RelativePeriods relatives;

    private I18nFormat i18nFormat;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        dataElements = new ArrayList<>();
        categoryOptionCombos = new ArrayList<>();
        indicators = new ArrayList<>();
        reportingRates = new ArrayList<>();
        periods = new ArrayList<>();
        units = new ArrayList<>();
        relativeUnits = new ArrayList<>();
        groups = new ArrayList<>();
        
        montlyPeriodType = PeriodType.getPeriodTypeByName( MonthlyPeriodType.NAME );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        
        dataElementA.setId( 'A' );
        dataElementB.setId( 'B' );
        
        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        
        deGroupSetA = createDataElementGroupSet( 'A' );
        
        deGroupA = createDataElementGroup( 'A' );
        deGroupB = createDataElementGroup( 'B' );
        
        deGroupA.getGroupSets().add( deGroupSetA );
        deGroupB.getGroupSets().add( deGroupSetA );
        
        deGroupSetA.getMembers().add( deGroupA );
        deGroupSetA.getMembers().add( deGroupB );
        
        categoryOptionComboA = createCategoryOptionCombo( 'A', 'A', 'B' );
        categoryOptionComboB = createCategoryOptionCombo( 'B', 'C', 'D' );
        
        categoryOptionComboA.setId( 'A' );
        categoryOptionComboB.setId( 'B' );
        
        categoryOptionCombos.add( categoryOptionComboA );
        categoryOptionCombos.add( categoryOptionComboB );

        categoryCombo = new DataElementCategoryCombo( "CategoryComboA", DataDimensionType.DISAGGREGATION );
        categoryCombo.setId( 'A' );
        categoryCombo.setOptionCombos( new HashSet<>( categoryOptionCombos ) );
        
        indicatorType = createIndicatorType( 'A' );

        indicatorA = createIndicator( 'A', indicatorType );
        indicatorB = createIndicator( 'B', indicatorType );
        
        indicatorA.setId( 'A' );
        indicatorB.setId( 'B' );
                
        indicators.add( indicatorA );
        indicators.add( indicatorB );
        
        DataSet dataSetA = createDataSet( 'A', montlyPeriodType );
        DataSet dataSetB = createDataSet( 'B', montlyPeriodType );
        
        dataSetA.setId( 'A' );
        dataSetB.setId( 'B' );
        
        reportingRateA = new ReportingRate( dataSetA );
        reportingRateB = new ReportingRate( dataSetB );
        
        reportingRates.add( reportingRateA );
        reportingRates.add( reportingRateB );
        
        periodA = createPeriod( montlyPeriodType, getDate( 2008, 1, 1 ), getDate( 2008, 1, 31 ) );
        periodB = createPeriod( montlyPeriodType, getDate( 2008, 2, 1 ), getDate( 2008, 2, 28 ) );
        
        periodA.setId( 'A' );
        periodB.setId( 'B' );

        periods.add( periodA );
        periods.add( periodB );

        relatives = new RelativePeriods();
        
        relatives.setLastMonth( true );
        relatives.setThisYear( true );

        List<Period> rp = relatives.getRelativePeriods();
        
        periodC = rp.get( 0 );
        periodD = rp.get( 1 );

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B', unitA );
        unitC = createOrganisationUnit( 'C', unitA );
        
        unitA.setId( 'A' );
        unitB.setId( 'B' );
        unitC.setId( 'C' );
        unitA.getChildren().add( unitB );
        unitA.getChildren().add( unitC );
        
        units.add( unitA );
        units.add( unitB );
        relativeUnits.add( unitA );
        
        ouGroupSetA = createOrganisationUnitGroupSet( 'A' );
        
        ouGroupA = createOrganisationUnitGroup( 'A' );
        ouGroupB = createOrganisationUnitGroup( 'B' );
        
        ouGroupA.getGroupSets().add( ouGroupSetA );
        ouGroupB.getGroupSets().add( ouGroupSetA );
        
        ouGroupA.setId( 'A' );
        ouGroupB.setId( 'B' );

        ouGroupSetA.getOrganisationUnitGroups().add( ouGroupA );
        ouGroupSetA.getOrganisationUnitGroups().add( ouGroupB );
        
        groups.add( ouGroupA );
        groups.add( ouGroupB );
        
        i18nFormat = new MockI18nFormat();
    }
    
    private static List<String> getColumnNames( List<List<DimensionalItemObject>> cols )
    {
        List<String> columns = new ArrayList<>();
        
        for ( List<DimensionalItemObject> column : cols )
        {
            columns.add( ReportTable.getColumnName( column ) );
        }
        
        return columns;
    }
    
    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetColumnName()
    {
        List<DimensionalItemObject> a1 = Lists.newArrayList( unitA, periodC );
        
        assertNotNull( getColumnName( a1 ) );
        assertEquals( "organisationunitshorta_reporting_month", getColumnName( a1 ) );
        
        List<DimensionalItemObject> a2 = Lists.newArrayList( unitB, periodD );

        assertNotNull( getColumnName( a2 ) );
        assertEquals( "organisationunitshortb_year", getColumnName( a2 ) );
        
        List<DimensionalItemObject> a3 = Lists.newArrayList( ouGroupA, indicatorA );
        
        assertNotNull( getColumnName( a3 ) );
        assertEquals( "organisationunitgroupshorta_indicatorshorta", getColumnName( a3 ) );
    }
    
    @Test    
    public void testGetGrid()
    {
        ReportTable reportTable = new ReportTable( "Grid table",
            dataElements, new ArrayList<>(), new ArrayList<>(), periods, Lists.newArrayList( unitB, unitC ),
            true, true, false, null, null, null );

        reportTable.init( null, null, null, null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
                
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 11 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 21 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 12 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 22 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 13 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 23 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 14 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 24 );
        
        DisplayProperty property = DisplayProperty.NAME;
        
        Grid grid = reportTable.getGrid( new ListGrid(), valueMap, property, false );
        
        assertEquals( 8, grid.getWidth() );
        assertEquals( 8, grid.getHeaders().size() );
        assertEquals( 2, grid.getHeight() );
        
        assertEquals( unitB.getUid(), grid.getValue( 0, 0 ) );
        assertEquals( unitB.getDisplayProperty( property ), grid.getValue( 0, 1 ) );
        assertEquals( unitB.getCode(), grid.getValue( 0, 2 ) );
        assertEquals( unitB.getDescription(), grid.getValue( 0, 3 ) );
        assertEquals( 11, grid.getValue( 0, 4 ) );
        assertEquals( 12, grid.getValue( 0, 5 ) );
        assertEquals( 13, grid.getValue( 0, 6 ) );
        assertEquals( 14, grid.getValue( 0, 7 ) );
        
        assertEquals( unitC.getUid(), grid.getValue( 1, 0 ) );
        assertEquals( unitC.getDisplayProperty( property ), grid.getValue( 1, 1 ) );
        assertEquals( unitC.getCode(), grid.getValue( 1, 2 ) );
        assertEquals( unitC.getDescription(), grid.getValue( 1, 3 ) );
        assertEquals( 21, grid.getValue( 1, 4 ) );
        assertEquals( 22, grid.getValue( 1, 5 ) );
        assertEquals( 23, grid.getValue( 1, 6 ) );
        assertEquals( 24, grid.getValue( 1, 7 ) );
    }

    @Test
    public void testGetGridHideEmptyColumns()
    {
        ReportTable reportTable = new ReportTable( "Grid table",
            dataElements, new ArrayList<>(), new ArrayList<>(), periods, Lists.newArrayList( unitA, unitB ),
            true, true, false, null, null, null );
        
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitA.getDimensionItem(), 11 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 21 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitA.getDimensionItem(), 13 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 23 );

        reportTable.init( null, null, null, null, null, i18nFormat );

        Grid grid = reportTable.getGrid( new ListGrid(), valueMap, DisplayProperty.NAME, false );
        
        assertEquals( 8, grid.getWidth() );
        
        reportTable.setHideEmptyColumns( true );
        
        grid = reportTable.getGrid( new ListGrid(), valueMap, DisplayProperty.NAME, false );

        assertEquals( 5, grid.getWidth() ); // Removed description column and two data columns
    }
    
    @Test
    public void testGetGridShowHierarchy()
    {
        ReportTable reportTable = new ReportTable( "Grid table",
            dataElements, new ArrayList<>(), new ArrayList<>(), periods, Lists.newArrayList( unitB, unitC ),
            true, true, false, null, null, null );
        
        reportTable.setShowHierarchy( true );
        reportTable.init( null, null, null, null, null, i18nFormat );
        
        Map<Object, List<?>> ancestorMap = new HashMap<>();
        ancestorMap.put( unitB.getUid(), unitB.getAncestorNames( null, true ) );
        ancestorMap.put( unitC.getUid(), unitC.getAncestorNames( null, true ) );
        
        Map<String, Object> metaData = new HashMap<>();
        
        Map<String, Object> internalMetaData = new HashMap<>();
        internalMetaData.put( AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS.getKey(), ancestorMap );
        
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 11 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 21 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 12 );
        valueMap.put( dataElementA.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 22 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 13 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodA.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 23 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitB.getDimensionItem(), 14 );
        valueMap.put( dataElementB.getDimensionItem() + DIMENSION_SEP + periodB.getDimensionItem() + DIMENSION_SEP + unitC.getDimensionItem(), 24 );
        
        DisplayProperty property = DisplayProperty.NAME;
        
        Grid grid = reportTable.getGrid( new ListGrid( metaData, internalMetaData ), valueMap, property, false );
        
        assertEquals( 10, grid.getWidth() );
        assertEquals( 10, grid.getHeaders().size() );
        assertEquals( 2, grid.getHeight() );
        
        assertEquals( unitA.getDisplayName(), grid.getValue( 0, 0 ) );
        assertEquals( unitB.getDisplayName(), grid.getValue( 0, 1 ) );
        assertEquals( unitB.getUid(), grid.getValue( 0, 2 ) );
        assertEquals( unitB.getDisplayProperty( property ), grid.getValue( 0, 3 ) );
        assertEquals( unitB.getCode(), grid.getValue( 0, 4 ) );

        assertEquals( unitA.getDisplayName(), grid.getValue( 1, 0 ) );
        assertEquals( unitC.getDisplayName(), grid.getValue( 1, 1 ) );
        assertEquals( unitC.getUid(), grid.getValue( 1, 2 ) );
        assertEquals( unitC.getDisplayProperty( property ), grid.getValue( 1, 3 ) );
        assertEquals( unitC.getCode(), grid.getValue( 1, 4 ) );
    }

    @Test
    public void testOrgUnitGroupSetDimensionReportTable()
    {
        ReportTable table = new ReportTable();
        
        OrganisationUnitGroupSetDimension ougsd = new OrganisationUnitGroupSetDimension();
        ougsd.setDimension( ouGroupSetA );
        ougsd.getItems().addAll( Lists.newArrayList( ouGroupA, ouGroupB ) );
        
        table.addDataDimensionItem( dataElementA );
        table.addDataDimensionItem( dataElementB );
        table.getPeriods().add( periodA );
        table.getPeriods().add( periodB );
        table.getOrganisationUnitGroupSetDimensions().add( ougsd );
        
        table.setColumnDimensions( Lists.newArrayList( DimensionalObject.DATA_X_DIM_ID, DimensionalObject.PERIOD_DIM_ID ) );
        table.setRowDimensions( Lists.newArrayList( ougsd.getDimension().getDimension() ) );

        table.init( null, null, null, null, null, i18nFormat );

        assertEquals( 4, table.getGridColumns().size() );
        assertEquals( 2, table.getGridRows().size() );
        
        assertTrue( table.getGridRows().contains( Lists.newArrayList( ouGroupA ) ) );
        assertTrue( table.getGridRows().contains( Lists.newArrayList( ouGroupB ) ) );
    }
    
    @Test
    public void testDataElementGroupSetDimensionReportTable()
    {
        ReportTable table = new ReportTable();
        
        DataElementGroupSetDimension degsd = new DataElementGroupSetDimension();
        degsd.setDimension( deGroupSetA );
        degsd.getItems().addAll( Lists.newArrayList( deGroupA, deGroupB ) );

        table.addDataDimensionItem( dataElementA );
        table.addDataDimensionItem( dataElementB );
        table.getPeriods().add( periodA );
        table.getPeriods().add( periodB );
        table.getDataElementGroupSetDimensions().add( degsd );

        table.setColumnDimensions( Lists.newArrayList( DimensionalObject.DATA_X_DIM_ID ) );
        table.setRowDimensions( Lists.newArrayList( DimensionalObject.PERIOD_DIM_ID, degsd.getDimension().getDimension() ) );

        table.init( null, null, null, null, null, i18nFormat );

        assertEquals( 2, table.getGridColumns().size() );
        assertEquals( 4, table.getGridRows().size() );

        assertTrue( table.getGridRows().contains( Lists.newArrayList( periodA, deGroupA ) ) );
        assertTrue( table.getGridRows().contains( Lists.newArrayList( periodA, deGroupB ) ) );
        assertTrue( table.getGridRows().contains( Lists.newArrayList( periodB, deGroupA ) ) );
        assertTrue( table.getGridRows().contains( Lists.newArrayList( periodB, deGroupB ) ) );        
    }
    
    @Test
    public void testIndicatorReportTableA()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), indicators, new ArrayList<>(), periods, units,
            true, true, false, relatives, null, "january_2000" );

        reportTable.init( null, null, null, null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();

        assertNotNull( columns ); 
        assertEquals( 8, columns.size() );
        
        assertTrue( columns.contains( Lists.newArrayList( indicatorA, periodA  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorA, periodB  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorA, periodC  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorA, periodD  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorB, periodA  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorB, periodB  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorB, periodC  ) ) );
        assertTrue( columns.contains( Lists.newArrayList( indicatorB, periodD  ) ) );
            
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );        
        assertEquals( 8, columnNames.size() );
        
        assertTrue( columnNames.contains( "indicatorshorta_reporting_month" ) );
        assertTrue( columnNames.contains( "indicatorshorta_year" ) );
        assertTrue( columnNames.contains( "indicatorshortb_reporting_month" ) );
        assertTrue( columnNames.contains( "indicatorshortb_year" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 2, rows.size() );
                
        assertTrue( rows.contains( Lists.newArrayList( unitB ) ) );
        assertTrue( rows.contains( Lists.newArrayList( unitB ) ) );
    }

    @Test
    public void testIndicatorReportTableB()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), indicators, new ArrayList<>(), periods, units,
            false, false, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null, null,  null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 1, columnDims.size() );        
        assertEquals( 2, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 2, columns.size() );

        assertTrue( columns.contains( Lists.newArrayList( unitA ) ) );
        assertTrue( columns.contains( Lists.newArrayList( unitB ) ) );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 2, columnNames.size() );
        
        assertTrue( columnNames.contains( "organisationunitshorta" ) );
        assertTrue( columnNames.contains( "organisationunitshortb" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 8, rows.size() );

        assertTrue( rows.contains( Lists.newArrayList( indicatorA, periodA  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( indicatorA, periodB  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( indicatorA, periodC  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( indicatorA, periodD  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( indicatorB, periodA  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( indicatorB, periodB  ) ) );  
        assertTrue( rows.contains( Lists.newArrayList( indicatorB, periodC  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( indicatorB, periodD  ) ) );                    
    }

    @Test
    public void testIndicatorReportTableC()
    {        
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), indicators, new ArrayList<>(), periods, units,
            true, false, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 4, columns.size() );

        assertTrue( columns.contains( Arrays.asList( indicatorA, unitA ) ) );
        assertTrue( columns.contains( Arrays.asList( indicatorA, unitB ) ) );
        assertTrue( columns.contains( Arrays.asList( indicatorB, unitA ) ) );
        assertTrue( columns.contains( Arrays.asList( indicatorB, unitB ) ) );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 4, columnNames.size() );
        
        assertTrue( columnNames.contains( "indicatorshorta_organisationunitshorta" ) );
        assertTrue( columnNames.contains( "indicatorshorta_organisationunitshortb" ) );
        assertTrue( columnNames.contains( "indicatorshortb_organisationunitshorta" ) );
        assertTrue( columnNames.contains( "indicatorshortb_organisationunitshortb" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 4, rows.size() );

        assertTrue( rows.contains( Lists.newArrayList( periodA  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( periodB  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( periodC  ) ) );
        assertTrue( rows.contains( Lists.newArrayList( periodD  ) ) );
    }
    
    @Test
    public void testIndicatorReportTableColumnsOnly()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), indicators, new ArrayList<>(), periods, units,
            true, true, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 3, columnDims.size() );        
        assertEquals( 0, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 16, columns.size() );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();

        assertNotNull( rows );
        assertEquals( 1, rows.size() );
    }

    @Test
    public void testIndicatorReportTableRowsOnly()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), indicators, new ArrayList<>(), periods, units,
            false, false, false, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 0, columnDims.size() );        
        assertEquals( 3, rowDims.size() );

        assertTrue( rowDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 1, columns.size() );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();

        assertNotNull( rows );
        assertEquals( 16, rows.size() );
    }

    @Test
    public void testDataElementReportTableA()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            dataElements, new ArrayList<>(), new ArrayList<>(), periods, units,
            true, true, false, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 8, columns.size() );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 8, columnNames.size() );
        
        assertTrue( columnNames.contains( "dataelementshorta_year" ) );
        assertTrue( columnNames.contains( "dataelementshorta_reporting_month" ) );
        assertTrue( columnNames.contains( "dataelementshortb_year" ) );
        assertTrue( columnNames.contains( "dataelementshortb_reporting_month" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 2, rows.size() );
    }

    @Test
    public void testDataElementReportTableB()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            dataElements, new ArrayList<>(), new ArrayList<>(), periods, units,
            false, false, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 1, columnDims.size() );        
        assertEquals( 2, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 2, columns.size() );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 2, columnNames.size() );
        
        assertTrue( columnNames.contains( "organisationunitshorta" ) );
        assertTrue( columnNames.contains( "organisationunitshortb" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 8, rows.size() );        
    }

    @Test
    public void testDataElementReportTableC()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            dataElements, new ArrayList<>(), new ArrayList<>(), periods, units,
            true, false, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 4, columns.size() );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 4, columnNames.size() );
        
        assertTrue( columnNames.contains( "dataelementshorta_organisationunitshorta" ) );
        assertTrue( columnNames.contains( "dataelementshorta_organisationunitshortb" ) );
        assertTrue( columnNames.contains( "dataelementshortb_organisationunitshorta" ) );
        assertTrue( columnNames.contains( "dataelementshortb_organisationunitshortb" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 4, rows.size() );
    }

    @Test
    public void testDataSetReportTableA()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), new ArrayList<>(), reportingRates, periods, units,
            true, true, false, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 8, columns.size() );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 8, columnNames.size() );
        
        assertTrue( columnNames.contains( "datasetshorta reporting rate_year" ) );
        assertTrue( columnNames.contains( "datasetshorta reporting rate_reporting_month" ) );
        assertTrue( columnNames.contains( "datasetshortb reporting rate_year" ) );
        assertTrue( columnNames.contains( "datasetshortb reporting rate_reporting_month" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 2, rows.size() );
    }

    @Test
    public void testDataSetReportTableB()
    {
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), new ArrayList<>(), reportingRates, periods, units,
            false, false, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 1, columnDims.size() );        
        assertEquals( 2, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 2, columns.size() );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 2, columnNames.size() );
        
        assertTrue( columnNames.contains( "organisationunitshorta" ) );
        assertTrue( columnNames.contains( "organisationunitshortb" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 8, rows.size() );
    }

    @Test
    public void testDataSetReportTableC()
    {        
        ReportTable reportTable = new ReportTable( "Embezzlement",
            new ArrayList<>(), new ArrayList<>(), reportingRates, periods, units,
            true, false, true, relatives, null, "january_2000" );

        reportTable.init( null, null, null,  null, null, i18nFormat );

        List<String> columnDims = reportTable.getColumnDimensions();
        List<String> rowDims = reportTable.getRowDimensions();
        
        assertEquals( 2, columnDims.size() );        
        assertEquals( 1, rowDims.size() );

        assertTrue( columnDims.contains( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( columnDims.contains( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertTrue( rowDims.contains( DimensionalObject.PERIOD_DIM_ID ) );
        
        List<List<DimensionalItemObject>> columns = reportTable.getGridColumns();
        
        assertNotNull( columns );
        assertEquals( 4, columns.size() );
        
        List<String> columnNames = getColumnNames( reportTable.getGridColumns() );
        
        assertNotNull( columnNames );
        assertEquals( 4, columnNames.size() );
        
        assertTrue( columnNames.contains( "datasetshorta reporting rate_organisationunitshorta" ) );
        assertTrue( columnNames.contains( "datasetshorta reporting rate_organisationunitshortb" ) );
        assertTrue( columnNames.contains( "datasetshortb reporting rate_organisationunitshorta" ) );
        assertTrue( columnNames.contains( "datasetshortb reporting rate_organisationunitshortb" ) );
        
        List<List<DimensionalItemObject>> rows = reportTable.getGridRows();
        
        assertNotNull( rows );
        assertEquals( 4, rows.size() );
    }
}
