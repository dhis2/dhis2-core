package org.hisp.dhis.dimension;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.*;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_12_MONTHS;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class DimensionServiceTest
    extends DhisSpringTest
{
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;

    private CategoryOptionCombo cocA;

    private DataSet dsA;

    private Program prA;
    private ProgramStage psA;

    private TrackedEntityAttribute atA;

    private ProgramIndicator piA;

    private Period peA;
    private Period peB;

    private DimensionalItemObject peLast12Months;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private OrganisationUnit ouD;
    private OrganisationUnit ouE;

    private DimensionalItemObject ouUser;
    private DimensionalItemObject ouLevel2;

    private DataElementGroupSet deGroupSetA;

    private DataElementGroup deGroupA;
    private DataElementGroup deGroupB;
    private DataElementGroup deGroupC;

    private OrganisationUnitGroupSet ouGroupSetA;

    private OrganisationUnitGroup ouGroupA;
    private OrganisationUnitGroup ouGroupB;
    private OrganisationUnitGroup ouGroupC;

    private DimensionalItemObject itemObjectA;
    private DimensionalItemObject itemObjectB;
    private DimensionalItemObject itemObjectC;
    private DimensionalItemObject itemObjectD;
    private DimensionalItemObject itemObjectE;
    private DimensionalItemObject itemObjectF;
    private DimensionalItemObject itemObjectG;
    private DimensionalItemObject itemObjectH;

    private DimensionalItemId itemIdA;
    private DimensionalItemId itemIdB;
    private DimensionalItemId itemIdC;
    private DimensionalItemId itemIdD;
    private DimensionalItemId itemIdE;
    private DimensionalItemId itemIdF;
    private DimensionalItemId itemIdG;
    private DimensionalItemId itemIdH;

    private Set<DimensionalItemId> itemIds;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DimensionService dimensionService;

    @Override
    public void setUpTest()
    {
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deC.setDomainType( DataElementDomain.TRACKER );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );

        cocA = categoryService.getDefaultCategoryOptionCombo();

        dsA = createDataSet( 'A' );

        dataSetService.addDataSet( dsA );

        prA = createProgram( 'A' );

        idObjectManager.save( prA );

        psA = createProgramStage( 'A', 1 );

        idObjectManager.save( psA );

        atA = createTrackedEntityAttribute( 'A' );

        idObjectManager.save( atA );

        piA = createProgramIndicator( 'A', prA, null, null );

        idObjectManager.save( piA );

        peA = createPeriod( "201201" );
        peB = createPeriod( "201202" );
        peLast12Months = new BaseDimensionalItemObject( LAST_12_MONTHS.toString() );

        peA.setUid( "201201" );
        peB.setUid( "201202" );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        ouD = createOrganisationUnit( 'D' );
        ouE = createOrganisationUnit( 'E' );

        ouB.updateParent( ouA );
        ouC.updateParent( ouA );
        ouD.updateParent( ouB );
        ouE.updateParent( ouB );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );

        String level2 = KEY_LEVEL + 2;

        ouUser = new BaseDimensionalItemObject( KEY_USER_ORGUNIT );
        ouLevel2 = new BaseDimensionalItemObject( level2 );

        deGroupSetA = createDataElementGroupSet( 'A' );

        dataElementService.addDataElementGroupSet( deGroupSetA );

        deGroupA = createDataElementGroup( 'A' );
        deGroupB = createDataElementGroup( 'B' );
        deGroupC = createDataElementGroup( 'C' );

        deGroupA.getGroupSets().add( deGroupSetA );
        deGroupB.getGroupSets().add( deGroupSetA );
        deGroupC.getGroupSets().add( deGroupSetA );

        dataElementService.addDataElementGroup( deGroupA );
        dataElementService.addDataElementGroup( deGroupB );
        dataElementService.addDataElementGroup( deGroupC );

        deGroupSetA.getMembers().add( deGroupA );
        deGroupSetA.getMembers().add( deGroupB );
        deGroupSetA.getMembers().add( deGroupC );

        dataElementService.updateDataElementGroupSet( deGroupSetA );

        ouGroupSetA = createOrganisationUnitGroupSet( 'A' );

        organisationUnitGroupService.addOrganisationUnitGroupSet( ouGroupSetA );

        ouGroupA = createOrganisationUnitGroup( 'A' );
        ouGroupB = createOrganisationUnitGroup( 'B' );
        ouGroupC = createOrganisationUnitGroup( 'C' );

        ouGroupA.getGroupSets().add( ouGroupSetA );
        ouGroupB.getGroupSets().add( ouGroupSetA );
        ouGroupC.getGroupSets().add( ouGroupSetA );

        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupC );

        ouGroupSetA.getOrganisationUnitGroups().add( ouGroupA );
        ouGroupSetA.getOrganisationUnitGroups().add( ouGroupB );
        ouGroupSetA.getOrganisationUnitGroups().add( ouGroupC );

        organisationUnitGroupService.updateOrganisationUnitGroupSet( ouGroupSetA );

        itemObjectA = deA;
        itemObjectB = new DataElementOperand( deA, cocA );
        itemObjectC = new DataElementOperand( deA, null, cocA );
        itemObjectD = new DataElementOperand( deA, cocA, cocA );
        itemObjectE = new ReportingRate( dsA );
        itemObjectF = new ProgramDataElementDimensionItem( prA, deA );
        itemObjectG = new ProgramTrackedEntityAttributeDimensionItem( prA, atA );
        itemObjectH = piA;

        itemIdA = new DimensionalItemId( DATA_ELEMENT, deA.getUid() );
        itemIdB = new DimensionalItemId( DATA_ELEMENT_OPERAND, deA.getUid(), cocA.getUid() );
        itemIdC = new DimensionalItemId( DATA_ELEMENT_OPERAND, deA.getUid(), null, cocA.getUid() );
        itemIdD = new DimensionalItemId( DATA_ELEMENT_OPERAND, deA.getUid(), cocA.getUid(), cocA.getUid() );
        itemIdE = new DimensionalItemId( REPORTING_RATE, dsA.getUid(), ReportingRateMetric.REPORTING_RATE.name() );
        itemIdF = new DimensionalItemId( PROGRAM_DATA_ELEMENT, prA.getUid(), deA.getUid() );
        itemIdG = new DimensionalItemId( PROGRAM_ATTRIBUTE, prA.getUid(), atA.getUid() );
        itemIdH = new DimensionalItemId( PROGRAM_INDICATOR, piA.getUid() );

        itemIds = new HashSet<>();
        itemIds.add( itemIdA );
        itemIds.add( itemIdB );
        itemIds.add( itemIdC );
        itemIds.add( itemIdD );
        itemIds.add( itemIdE );
        itemIds.add( itemIdF );
        itemIds.add( itemIdG );
        itemIds.add( itemIdH );
    }

    @Test
    public void testMergeAnalyticalObjectA()
    {
        ReportTable reportTable = new ReportTable();

        reportTable.getColumns().add( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) ) );
        reportTable.getRows().add( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouA, ouB, ouC, ouD, ouE ) ) );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 2, reportTable.getDataDimensionItems().size() );
        assertEquals( 2, reportTable.getPeriods().size() );
        assertEquals( 5, reportTable.getOrganisationUnits().size() );
    }

    @Test
    public void testMergeAnalyticalObjectB()
    {
        ReportTable reportTable = new ReportTable();
        BaseDimensionalObject deCDim = new BaseDimensionalObject( deC.getUid(), DimensionType.PROGRAM_DATA_ELEMENT, null, null, null, psA, "EQ:uidA" );

        reportTable.getColumns().add( deCDim );
        reportTable.getRows().add( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouA, ouB, ouC ) ) );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 1, reportTable.getDataElementDimensions().size() );
        assertEquals( 2, reportTable.getPeriods().size() );
        assertEquals( 3, reportTable.getOrganisationUnits().size() );

        TrackedEntityDataElementDimension teDeDim = reportTable.getDataElementDimensions().get( 0 );

        assertEquals( deC, teDeDim.getDataElement() );
        assertEquals( psA, teDeDim.getProgramStage() );
    }

    @Test
    public void testMergeAnalyticalObjectUserOrgUnit()
    {
        ReportTable reportTable = new ReportTable();

        reportTable.getColumns().add( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) ) );
        reportTable.getRows().add( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouUser ) ) );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 2, reportTable.getDataDimensionItems().size() );
        assertEquals( 1, reportTable.getPeriods().size() );
        assertEquals( 0, reportTable.getOrganisationUnits().size() );
        assertTrue( reportTable.isUserOrganisationUnit() );
    }

    @Test
    public void testMergeAnalyticalObjectOrgUnitLevel()
    {
        ReportTable reportTable = new ReportTable();

        reportTable.getColumns().add( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) ) );
        reportTable.getRows().add( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouLevel2, ouA ) ) );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 2, reportTable.getDataDimensionItems().size() );
        assertEquals( 1, reportTable.getPeriods().size() );
        assertEquals( 1, reportTable.getOrganisationUnits().size() );
        assertEquals( Integer.valueOf( 2 ), reportTable.getOrganisationUnitLevels().get( 0 ) );
    }

    @Test
    public void testMergeAnalyticalObjectRelativePeriods()
    {
        ReportTable reportTable = new ReportTable();

        reportTable.getColumns().add( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) ) );
        reportTable.getRows().add( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouA, ouB, ouC, ouD, ouE ) ) );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peLast12Months ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 2, reportTable.getDataDimensionItems().size() );
        assertEquals( 0, reportTable.getPeriods().size() );
        assertTrue( reportTable.getRelatives().isLast12Months() );
        assertEquals( 5, reportTable.getOrganisationUnits().size() );
    }

    @Test
    public void testMergeAnalyticalObjectOrgUnitGroupSet()
    {
        ReportTable reportTable = new ReportTable();

        reportTable.getColumns().add( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) ) );
        reportTable.getRows().add( ouGroupSetA );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 2, reportTable.getDataDimensionItems().size() );
        assertEquals( 2, reportTable.getPeriods().size() );
        assertEquals( 1, reportTable.getOrganisationUnitGroupSetDimensions().size() );
        assertEquals( 3, reportTable.getOrganisationUnitGroupSetDimensions().get( 0 ).getItems().size() );
    }

    @Test
    public void testMergeAnalyticalObjectDataElementGroupSet()
    {
        ReportTable reportTable = new ReportTable();

        reportTable.getColumns().add( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList( deA, deB ) ) );
        reportTable.getRows().add( deGroupSetA );
        reportTable.getFilters().add( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB ) ) );

        dimensionService.mergeAnalyticalObject( reportTable );

        assertEquals( 2, reportTable.getDataDimensionItems().size() );
        assertEquals( 2, reportTable.getPeriods().size() );
        assertEquals( 1, reportTable.getDataElementGroupSetDimensions().size() );
        assertEquals( 3, reportTable.getDataElementGroupSetDimensions().get( 0 ).getItems().size() );
    }

    @Test
    public void testGetDimensionalItemObject()
    {
        String idA = deA.getUid();
        String idB = prA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + deA.getUid();
        String idC = prA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + atA.getUid();
        String idD = dsA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + ReportingRateMetric.REPORTING_RATE.name();
        String idE = dsA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + "UNKNOWN_METRIC";
        String idF = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + cocA.getUid();
        String idG = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + SYMBOL_WILDCARD;
        String idH = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + "UNKNOWN_SYMBOL";
        String idI = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + cocA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + cocA.getUid();
        String idJ = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + cocA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + SYMBOL_WILDCARD;
        String idK = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + SYMBOL_WILDCARD + COMPOSITE_DIM_OBJECT_PLAIN_SEP + cocA.getUid();

        ProgramDataElementDimensionItem pdeA = new ProgramDataElementDimensionItem( prA, deA );
        ProgramTrackedEntityAttributeDimensionItem ptaA = new ProgramTrackedEntityAttributeDimensionItem( prA, atA );
        ReportingRate rrA = new ReportingRate( dsA, ReportingRateMetric.REPORTING_RATE );
        DataElementOperand deoA = new DataElementOperand( deA, cocA );
        DataElementOperand deoB = new DataElementOperand( deA, null );
        DataElementOperand deoC = new DataElementOperand( deA, cocA, cocA );
        DataElementOperand deoD = new DataElementOperand( deA, cocA, null );
        DataElementOperand deoE = new DataElementOperand( deA, null, cocA );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idA ) );
        assertEquals( deA, dimensionService.getDataDimensionalItemObject( idA ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idB ) );
        assertEquals( pdeA, dimensionService.getDataDimensionalItemObject( idB ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idC ) );
        assertEquals( ptaA, dimensionService.getDataDimensionalItemObject( idC ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idD ) );
        assertEquals( rrA, dimensionService.getDataDimensionalItemObject( idD ) );

        assertNull( dimensionService.getDataDimensionalItemObject( idE ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idF ) );
        assertEquals( deoA, dimensionService.getDataDimensionalItemObject( idF ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idG ) );
        assertEquals( deoB, dimensionService.getDataDimensionalItemObject( idG ) );

        assertNull( dimensionService.getDataDimensionalItemObject( idH ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idI ) );
        assertEquals( deoC, dimensionService.getDataDimensionalItemObject( idI ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idJ ) );
        assertEquals( deoD, dimensionService.getDataDimensionalItemObject( idJ ) );

        assertNotNull( dimensionService.getDataDimensionalItemObject( idK ) );
        assertEquals( deoE, dimensionService.getDataDimensionalItemObject( idK ) );
    }

    @Test
    public void testGetDataDimensionalItemObjects()
    {
        Set<DimensionalItemObject> objects = dimensionService.getDataDimensionalItemObjects( new HashSet<>() );

        assertNotNull( objects );
        assertEquals( 0, objects.size() );

        objects = dimensionService.getDataDimensionalItemObjects( itemIds );

        assertNotNull( objects );
        assertEquals( 8, objects.size() );

        assertTrue( objects.contains( itemObjectA ) );
        assertTrue( objects.contains( itemObjectB ) );
        assertTrue( objects.contains( itemObjectC ) );
        assertTrue( objects.contains( itemObjectD ) );
        assertTrue( objects.contains( itemObjectE ) );
        assertTrue( objects.contains( itemObjectF ) );
        assertTrue( objects.contains( itemObjectG ) );
        assertTrue( objects.contains( itemObjectH ) );
    }

    @Test
    public void testGetDataDimensionalItemObjectsWithOffsetValue()
    {
        // Given
        int offset = 1;
        Set<DimensionalItemId> dimensionalItemIds = new HashSet<>();

        DimensionalItemId itemIdA = new DimensionalItemId( DATA_ELEMENT, deA.getUid(), offset );
        DimensionalItemId itemIdB = new DimensionalItemId( DATA_ELEMENT, deB.getUid(), offset );
        DimensionalItemId itemIdC = new DimensionalItemId( DATA_ELEMENT, deC.getUid(), offset );

        dimensionalItemIds.add( itemIdA );
        dimensionalItemIds.add( itemIdB );
        dimensionalItemIds.add( itemIdC );

        // When
        Set<DimensionalItemObject> objects = dimensionService.getDataDimensionalItemObjects( dimensionalItemIds );

        // Then
        assertEquals( 3, objects.size() );
        for ( DimensionalItemObject object : objects )
        {
            assertThat( object.getPeriodOffset(), is( 1 ) );
        }
    }

    @Test
    public void testGetDataDimensionalItemObjectsReturnsItemWithOffsetLargerThanZero()
    {
        // Given
        int offset = 1;
        Set<DimensionalItemId> dimensionalItemIds = new HashSet<>();

        dimensionalItemIds.add( new DimensionalItemId( DATA_ELEMENT, deA.getUid() ) );
        dimensionalItemIds.add( new DimensionalItemId( DATA_ELEMENT, deB.getUid() ) );
        dimensionalItemIds.add( new DimensionalItemId( DATA_ELEMENT, deC.getUid() ) );
        dimensionalItemIds.add( new DimensionalItemId( DATA_ELEMENT, deA.getUid(), offset ) );
        dimensionalItemIds.add( new DimensionalItemId( DATA_ELEMENT, deB.getUid(), offset ) );
        dimensionalItemIds.add( new DimensionalItemId( DATA_ELEMENT, deC.getUid(), offset ) );

        // When
        Set<DimensionalItemObject> objects = dimensionService.getDataDimensionalItemObjects( dimensionalItemIds );

        // Then
        assertEquals( 3, objects.size() );
        for ( DimensionalItemObject object : objects )
        {
            assertThat( object.getPeriodOffset(), is( 1 ) );
        }
    }

    @Test
    public void testGetDataDimensionalItemObjectMap()
    {
        Map<DimensionalItemId, DimensionalItemObject> map = dimensionService
            .getDataDimensionalItemObjectMap( new HashSet<>() );

        assertNotNull( map );
        assertEquals( 0, map.size() );

        map = dimensionService.getDataDimensionalItemObjectMap( itemIds );

        assertNotNull( map );
        assertEquals( 8, map.size() );

        assertEquals( itemObjectA, map.get( itemIdA ) );
        assertEquals( itemObjectB, map.get( itemIdB ) );
        assertEquals( itemObjectC, map.get( itemIdC ) );
        assertEquals( itemObjectD, map.get( itemIdD ) );
        assertEquals( itemObjectE, map.get( itemIdE ) );
        assertEquals( itemObjectF, map.get( itemIdF ) );
        assertEquals( itemObjectG, map.get( itemIdG ) );
        assertEquals( itemObjectH, map.get( itemIdH ) );
    }

    @Test
    public void testGetDimensionalObjectEventReport()
    {
        EventReport report = new EventReport();
        report.setAutoFields();

        DataElement deA = createDataElement( 'A' );
        LegendSet lsA = createLegendSet( 'A' );
        ProgramStage psA = createProgramStage( 'A', 1 );

        TrackedEntityDataElementDimension teDeDim = new TrackedEntityDataElementDimension( deA, lsA, psA, "EQ:1" );

        report.addTrackedEntityDataElementDimension( teDeDim );
        report.getOrganisationUnits().addAll( Lists.newArrayList( ouA, ouB, ouC ) );

        report.getColumnDimensions().add( deA.getUid() );
        report.getRowDimensions().add( DimensionalObject.ORGUNIT_DIM_ID );

        report.populateAnalyticalProperties();

        assertEquals( 1, report.getColumns().size() );
        assertEquals( 1, report.getRows().size() );

        DimensionalObject dim = report.getColumns().get( 0 );

        assertEquals( lsA, dim.getLegendSet() );
        assertEquals( psA, dim.getProgramStage() );
    }
}
