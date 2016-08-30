package org.hisp.dhis.analytics.data;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElement;
import org.hisp.dhis.program.ProgramDataElementStore;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hisp.dhis.common.DimensionalObject.*;

/**
 * @author Lars Helge Overland
 */
public class DataQueryServiceTest
    extends DhisSpringTest
{
    private Program prA;
    
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    
    private ProgramDataElement pdA;
    private ProgramDataElement pdB;
    
    private DataElementCategoryOptionCombo cocA;
    
    private ReportingRate rrA;
    private ReportingRate rrB;
    private ReportingRate rrC;
    
    private IndicatorType itA;
    
    private Indicator inA;
    private Indicator inB;
    
    private TrackedEntityAttribute atA;
    private TrackedEntityAttribute atB;
    
    private ProgramTrackedEntityAttribute patA;
    private ProgramTrackedEntityAttribute patB;
    
    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private OrganisationUnit ouD;
    private OrganisationUnit ouE;
    
    private OrganisationUnitGroup ouGroupA;
    private OrganisationUnitGroup ouGroupB;
    private OrganisationUnitGroup ouGroupC;
    
    private OrganisationUnitGroupSet ouGroupSetA;
    
    private IndicatorGroup inGroupA;
    
    private DataElementGroup deGroupA;
    private DataElementGroup deGroupB;
    private DataElementGroup deGroupC;
        
    private DataElementGroupSet deGroupSetA;
    
    private PeriodType monthly = PeriodType.getPeriodTypeByName( MonthlyPeriodType.NAME );
    
    @Autowired
    private DataQueryService dataQueryService;
        
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    @Autowired
    private DataSetService dataSetService;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;
    
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private ProgramDataElementStore programDataElementStore;
    
    @Autowired
    private UserService internalUserService;
    
    @Override
    public void setUpTest()
    {
        super.userService = internalUserService;
        
        prA = createProgram( 'A' );
        
        programService.addProgram( prA );
        
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        
        DataElement deE = createDataElement( 'E' );
        DataElement deF = createDataElement( 'F' );
        
        deE.setDomainType( DataElementDomain.TRACKER );
        deF.setDomainType( DataElementDomain.TRACKER );
        
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deE );
        dataElementService.addDataElement( deF );
        
        pdA = new ProgramDataElement( prA, deE );
        pdB = new ProgramDataElement( prA, deF );
        
        programDataElementStore.save( pdA );
        programDataElementStore.save( pdB );
        
        cocA = categoryService.getDefaultDataElementCategoryOptionCombo();

        DataSet dsA = createDataSet( 'A', monthly );
        DataSet dsB = createDataSet( 'B', monthly );
        
        dataSetService.addDataSet( dsA );
        dataSetService.addDataSet( dsB );
        
        rrA = new ReportingRate( dsA, ReportingRateMetric.REPORTING_RATE );
        rrB = new ReportingRate( dsB, ReportingRateMetric.REPORTING_RATE );
        rrC = new ReportingRate( dsB, ReportingRateMetric.ACTUAL_REPORTS );
        
        itA = createIndicatorType( 'A' );
        
        idObjectManager.save( itA );
        
        inA = createIndicator( 'A', itA );
        inB = createIndicator( 'B', itA );
        
        idObjectManager.save( inA );
        idObjectManager.save( inB );
        
        inGroupA = createIndicatorGroup( 'A' );
        inGroupA.getMembers().add( inA );
        inGroupA.getMembers().add( inB );
        
        idObjectManager.save( inGroupA );
        
        atA = createTrackedEntityAttribute( 'A' );
        atB = createTrackedEntityAttribute( 'B' );
        
        idObjectManager.save( atA );
        idObjectManager.save( atB );
        
        patA = new ProgramTrackedEntityAttribute( prA, atA );
        patB = new ProgramTrackedEntityAttribute( prA, atB );

        patA.setCode( "ProgramTrackedEntityCodeA" );
        patB.setCode( "ProgramTrackedEntityCodeB" );
        
        idObjectManager.save( patA );
        idObjectManager.save( patB );
        
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

        ouGroupSetA = createOrganisationUnitGroupSet( 'A' );
        
        organisationUnitGroupService.addOrganisationUnitGroupSet( ouGroupSetA );
        
        ouGroupA = createOrganisationUnitGroup( 'A' );
        ouGroupB = createOrganisationUnitGroup( 'B' );
        ouGroupC = createOrganisationUnitGroup( 'C' );
                
        ouGroupA.addOrganisationUnit( ouA );
        ouGroupA.addOrganisationUnit( ouB );
        ouGroupA.addOrganisationUnit( ouC );
        
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupC );
        
        ouGroupSetA.addOrganisationUnitGroup( ouGroupA );
        ouGroupSetA.addOrganisationUnitGroup( ouGroupB );
        ouGroupSetA.addOrganisationUnitGroup( ouGroupC );
        
        organisationUnitGroupService.updateOrganisationUnitGroupSet( ouGroupSetA );

        deGroupSetA = createDataElementGroupSet( 'A' );
        
        dataElementService.addDataElementGroupSet( deGroupSetA );
        
        deGroupA = createDataElementGroup( 'A' );
        deGroupB = createDataElementGroup( 'B' );
        deGroupC = createDataElementGroup( 'C' );
        
        deGroupA.setGroupSet( deGroupSetA );
        deGroupB.setGroupSet( deGroupSetA );
        deGroupC.setGroupSet( deGroupSetA );
        
        deGroupA.setGroupSet( deGroupSetA );
        deGroupA.addDataElement( deA );
        deGroupA.addDataElement( deB );
        deGroupA.addDataElement( deC );
                
        dataElementService.addDataElementGroup( deGroupA );
        dataElementService.addDataElementGroup( deGroupB );
        dataElementService.addDataElementGroup( deGroupC );
        
        deGroupSetA.addDataElementGroup( deGroupA );
        deGroupSetA.addDataElementGroup( deGroupB );
        deGroupSetA.addDataElementGroup( deGroupC );
        
        dataElementService.updateDataElementGroupSet( deGroupSetA );

        // ---------------------------------------------------------------------
        // Inject user
        // ---------------------------------------------------------------------

        User user = createUser( 'A' );
        user.addOrganisationUnit( ouA );
        saveAndInjectUserSecurityContext( user );
    }

    @Test
    public void testGetDimensionalObjects()
    {
        Set<String> dimensionParams = new LinkedHashSet<>();
        dimensionParams.add( DimensionalObject.DATA_X_DIM_ID + DIMENSION_NAME_SEP + deA.getDimensionItem() + OPTION_SEP + deB.getDimensionItem() + OPTION_SEP + rrA.getDimensionItem() );
        dimensionParams.add( DimensionalObject.ORGUNIT_DIM_ID + DIMENSION_NAME_SEP + ouA.getDimensionItem() + OPTION_SEP + ouB.getDimensionItem() );
        
        List<DimensionalObject> dimensionalObject = dataQueryService.getDimensionalObjects( dimensionParams, null, null, null, IdScheme.UID );
        
        DimensionalObject dxObject = dimensionalObject.get( 0 );
        DimensionalObject ouObject = dimensionalObject.get( 1 );

        List<DimensionalItemObject> dxItems = Lists.newArrayList( deA, deB, rrA );
        List<DimensionalItemObject> ouItems = Lists.newArrayList( ouA, ouB );

        assertEquals( DimensionalObject.DATA_X_DIM_ID, dxObject.getDimension() );
        assertEquals( DimensionType.DATA_X, dxObject.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, dxObject.getDisplayName() );
        assertEquals( dxItems, dxObject.getItems() );

        assertEquals( DimensionalObject.ORGUNIT_DIM_ID, ouObject.getDimension() );
        assertEquals( DimensionType.ORGANISATION_UNIT, ouObject.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_ORGUNIT, ouObject.getDisplayName() );
        assertEquals( ouItems, ouObject.getItems() );
    }

    @Test
    public void testGetDimensionalObjectsReportingRates()
    {
        Set<String> dimensionParams = new LinkedHashSet<>();
        dimensionParams.add( DimensionalObject.DATA_X_DIM_ID + DIMENSION_NAME_SEP + deA.getDimensionItem() + 
            OPTION_SEP + rrA.getDimensionItem() + OPTION_SEP + rrB.getDimensionItem() + OPTION_SEP + rrC.getDimensionItem() );
        dimensionParams.add( DimensionalObject.ORGUNIT_DIM_ID + DIMENSION_NAME_SEP + ouA.getDimensionItem() );
        
        List<DimensionalObject> dimensionalObject = dataQueryService.getDimensionalObjects( dimensionParams, null, null, null, IdScheme.UID );
        
        DimensionalObject dxObject = dimensionalObject.get( 0 );
        DimensionalObject ouObject = dimensionalObject.get( 1 );

        List<DimensionalItemObject> dxItems = Lists.newArrayList( deA, rrA, rrB, rrC );
        List<DimensionalItemObject> ouItems = Lists.newArrayList( ouA );

        assertEquals( DimensionalObject.DATA_X_DIM_ID, dxObject.getDimension() );
        assertEquals( DimensionType.DATA_X, dxObject.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, dxObject.getDisplayName() );
        assertEquals( dxItems, dxObject.getItems() );

        assertEquals( DimensionalObject.ORGUNIT_DIM_ID, ouObject.getDimension() );
        assertEquals( DimensionType.ORGANISATION_UNIT, ouObject.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_ORGUNIT, ouObject.getDisplayName() );
        assertEquals( ouItems, ouObject.getItems() );
    }
    
    @Test
    public void testGetDimensionData()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( deA, deB, deC, rrA, rrB );
        
        List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds( items );
        
        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, 
            itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.DATA_X_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.DATA_X, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDisplayName() );
        assertEquals( items, actual.getItems() );
    }

    @Test
    public void testGetDimensionDataByCode()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( deA, deB, deC );

        List<String> itemCodes = Lists.newArrayList( deA.getCode(), deB.getCode(), deC.getCode() );

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemCodes, null, null, null, false, IdScheme.CODE );

        assertEquals( DimensionalObject.DATA_X_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.DATA_X, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDisplayName() );
        assertEquals( items, actual.getItems() );
    }

    @Test
    public void testGetDimensionOperand()
    {
        DataElementOperand opA = new DataElementOperand( deA, cocA );
        DataElementOperand opB = new DataElementOperand( deB, cocA );
        DataElementOperand opC = new DataElementOperand( deC, cocA );
        
        List<DimensionalItemObject> items = Lists.newArrayList( opA, opB, opC );
        
        List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds( items );
        
        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.DATA_X_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.DATA_X, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDisplayName() );
        assertEquals( items, actual.getItems() );
    }
    
    @Test
    public void testGetDimensionOrgUnit()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( ouA, ouB, ouC );
        
        List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds( items );

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.ORGUNIT_DIM_ID, itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.ORGUNIT_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.ORGANISATION_UNIT, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_ORGUNIT, actual.getDisplayName() );
        assertEquals( items, actual.getItems() );
    }

    @Test
    public void testGetDimensionOrgUnitGroup()
    {        
        String ouGroupAUid = OrganisationUnit.KEY_ORGUNIT_GROUP + ouGroupA.getUid();
        
        List<String> itemUids = Lists.newArrayList( ouGroupAUid );

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.ORGUNIT_DIM_ID, itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.ORGUNIT_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.ORGANISATION_UNIT, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_ORGUNIT, actual.getDisplayName() );
        assertEquals( ouGroupA.getMembers(), Sets.newHashSet( actual.getItems() ) );
    }
    
    @Test
    public void testGetDimensionDataElementGroup()
    {
        String deGroupAId = DataQueryParams.KEY_DE_GROUP + deGroupA.getUid();
        
        List<String> itemUids = Lists.newArrayList( deGroupAId );
        
        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.DATA_X_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.DATA_X, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDisplayName() );
        assertEquals( deGroupA.getMembers(), Sets.newHashSet( actual.getItems() ) );
    }
    
    @Test
    public void testGetDimensionIndicatorGroup()
    {
        String inGroupAId = DataQueryParams.KEY_IN_GROUP + inGroupA.getUid();
        
        List<String> itemUids = Lists.newArrayList( inGroupAId );
        
        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.DATA_X_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.DATA_X, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDisplayName() );
        assertEquals( inGroupA.getMembers(), Sets.newHashSet( actual.getItems() ) );
    }
    
    @Test
    public void testGetDimensionPeriod()
    {
        List<String> itemUids = Lists.newArrayList( "199501", "1999", 
            RelativePeriodEnum.LAST_4_QUARTERS.toString(), RelativePeriodEnum.THIS_YEAR.toString() );

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.PERIOD_DIM_ID, itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( DimensionalObject.PERIOD_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.PERIOD, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_PERIOD, actual.getDisplayName() );
        assertEquals( 7, actual.getItems().size() );
    }
    
    @Test
    public void testGetDimensionOrgUnitGroupSet()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( ouGroupA, ouGroupB );
        
        List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds( items );
        
        DimensionalObject actual = dataQueryService.getDimension( ouGroupSetA.getUid(), itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( ouGroupSetA.getUid(), actual.getDimension() );
        assertEquals( DimensionType.ORGANISATION_UNIT_GROUP_SET, actual.getDimensionType() );
        assertEquals( ouGroupSetA.getName(), actual.getDisplayName() );
        assertEquals( items, actual.getItems() );  
    }

    @Test
    public void testGetDimensionDataElementGroupSet()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( deGroupA, deGroupB );
        
        List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds( items );
        
        DimensionalObject actual = dataQueryService.getDimension( deGroupSetA.getUid(), itemUids, null, null, null, false, IdScheme.UID );
        
        assertEquals( deGroupSetA.getUid(), actual.getDimension() );
        assertEquals( DimensionType.DATA_ELEMENT_GROUP_SET, actual.getDimensionType() );
        assertEquals( deGroupSetA.getName(), actual.getDisplayName() );
        assertEquals( items, actual.getItems() );  
    }
    
    @Test
    public void testGetFromUrlA()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid() );
        dimensionParams.add( "pe:2012;2012S1;2012S2" );
        dimensionParams.add( ouGroupSetA.getUid() + ":" + ouGroupA.getUid() + ";" + ouGroupB.getUid() + ";" + ouGroupC.getUid() );
        
        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getUid() + ";" + ouB.getUid() + ";" + ouC.getUid() + ";" + ouD.getUid() + ";" + ouE.getUid() );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 4, params.getDataElements().size() );
        assertEquals( 3, params.getPeriods().size() );
        assertEquals( 5, params.getFilterOrganisationUnits().size() );
        assertEquals( 3, params.getDimensionOptions( ouGroupSetA.getUid() ).size() );
    }

    @Test
    public void testGetFromUrlB()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() + ";" + deC.getDimensionItem() + ";" + deD.getUid() );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getUid() );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 4, params.getDataElements().size() );
        assertEquals( 1, params.getFilterOrganisationUnits().size() );
    }

    @Test
    public void testGetFromUrlC()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() + ";" + pdA.getDimensionItem() + ";" + pdB.getDimensionItem() );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getDimensionItem() );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getProgramDataElements().size() );
        assertEquals( 1, params.getFilterOrganisationUnits().size() );
    }

    @Test
    public void testGetFromUrlD()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() + ";" + patA.getDimensionItem() + ";" + patB.getDimensionItem() );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getDimensionItem() );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getProgramAttributes().size() );
        assertEquals( 1, params.getFilterOrganisationUnits().size() );
    }

    @Test
    public void testGetFromUrlWithCode()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getCode() + ";" + deB.getCode() + ";" + patA.getCode() + ";" + patB.getCode() );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getCode() );

        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null,
            false, false, false, false, false, false, false, false, false, null, null, IdScheme.CODE, null, null, null, null );

        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getProgramAttributes().size() );
        assertEquals( 1, params.getFilterOrganisationUnits().size() );
    }

    @Test
    public void testGetFromUrlOrgUnitGroupSetAllItems()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() + ";" + deC.getDimensionItem() );
        dimensionParams.add( "pe:2012;2012S1" );
        dimensionParams.add( ouGroupSetA.getUid() );
        
        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getDimensionItem() + ";" + ouB.getDimensionItem() + ";" + ouC.getDimensionItem() );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 3, params.getDataElements().size() );
        assertEquals( 2, params.getPeriods().size() );
        assertEquals( 3, params.getFilterOrganisationUnits().size() );
        assertEquals( 3, params.getDimensionOptions( ouGroupSetA.getUid() ).size() );
    }

    @Test
    public void testGetFromUrlRelativePeriods()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() + ";" + deC.getDimensionItem() + ";" + deD.getDimensionItem() );
        dimensionParams.add( "pe:LAST_12_MONTHS" );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getDimensionItem() + ";" + ouB.getDimensionItem() );

        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 4, params.getDataElements().size() );
        assertEquals( 12, params.getPeriods().size() );
        assertEquals( 2, params.getFilterOrganisationUnits().size() );
    }
    
    @Test
    public void testGetFromUrlUserOrgUnit()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "ou:" + OrganisationUnit.KEY_USER_ORGUNIT );
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() );
        dimensionParams.add( "pe:2011;2012" );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 1, params.getOrganisationUnits().size() );  
        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getPeriods().size() );      
    }

    @Test
    public void testGetFromUrlOrgUnitGroup()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "ou:OU_GROUP-" + ouGroupA.getUid() );
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() );
        dimensionParams.add( "pe:2011;2012" );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 3, params.getOrganisationUnits().size() );  
        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getPeriods().size() ); 
    }

    @Test
    public void testGetFromUrlOrgUnitLevel()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "ou:LEVEL-2" );
        dimensionParams.add( "dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem() );
        dimensionParams.add( "pe:2011;2012" );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        assertEquals( 2, params.getOrganisationUnits().size() );  
        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getPeriods().size() ); 
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlNoDx()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx" );
        dimensionParams.add( "pe:2012,2012S1,2012S2" );
        
        dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
    }
    
    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlNoPeriods()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D" );
        dimensionParams.add( "pe" );

        dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlNoOrganisationUnits()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D" );
        dimensionParams.add( "ou" );
        
        dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlInvalidDimension()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D" );
        dimensionParams.add( "yebo:2012,2012S1,2012S2" );
        
        dataQueryService.getFromUrl( dimensionParams, null, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
    }

    @Test
    public void testGetFromUrlPeriodOrder()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid() );
        dimensionParams.add( "pe:2013;2012Q4;2012S2" );
        
        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getUid() );
        
        DataQueryParams params = dataQueryService.getFromUrl( dimensionParams, filterParams, null, null, 
            false, false, false, false, false, false, false, false, false, null, null, null, null, null, null, null );
        
        List<DimensionalItemObject> periods = params.getPeriods();
        
        assertEquals( 3, periods.size() );
        assertEquals( "2013", periods.get( 0 ).getUid() );
        assertEquals( "2012Q4", periods.get( 1 ).getUid() );
        assertEquals( "2012S2", periods.get( 2 ).getUid() );        
    }

    @Test
    public void testGetFromAnalyticalObjectA()
    {
        Chart chart = new Chart();
        chart.setSeries( DimensionalObject.DATA_X_DIM_ID );
        chart.setCategory( DimensionalObject.ORGUNIT_DIM_ID );
        chart.getFilterDimensions().add( DimensionalObject.PERIOD_DIM_ID );
        
        chart.addDataDimensionItem( deA );
        chart.addDataDimensionItem( deB );
        chart.addDataDimensionItem( deC );
        
        chart.getOrganisationUnits().add( ouA );
        chart.getOrganisationUnits().add( ouB );
        
        chart.getPeriods().add( PeriodType.getPeriodFromIsoString( "2012" ) );
        
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( chart );
        
        assertNotNull( params );
        assertEquals( 3, params.getDataElements().size() );
        assertEquals( 2, params.getOrganisationUnits().size() );
        assertEquals( 1, params.getFilterPeriods().size() );
        assertEquals( 2, params.getDimensions().size() );
        assertEquals( 1, params.getFilters().size() );
    }
    
    @Test
    public void testGetFromAnalyticalObjectB()
    {
        Chart chart = new Chart();
        chart.setSeries( DimensionalObject.DATA_X_DIM_ID );
        chart.setCategory( ouGroupSetA.getUid() );
        chart.getFilterDimensions().add( DimensionalObject.PERIOD_DIM_ID );
        
        chart.addDataDimensionItem( deA );
        chart.addDataDimensionItem( deB );
        chart.addDataDimensionItem( deC );
        
        chart.getOrganisationUnitGroups().add( ouGroupA );
        chart.getOrganisationUnitGroups().add( ouGroupB );
        chart.getOrganisationUnitGroups().add( ouGroupC );
        
        chart.getPeriods().add( PeriodType.getPeriodFromIsoString( "2012" ) );
        
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( chart );
        
        assertNotNull( params );
        assertEquals( 3, params.getDataElements().size() );
        assertEquals( 1, params.getFilterPeriods().size() );
        assertEquals( 2, params.getDimensions().size() );
        assertEquals( 1, params.getFilters().size() );
        assertEquals( 3, params.getDimensionOptions( ouGroupSetA.getUid() ).size() );
    }
    
    @Test
    public void testGetFromAnalyticalObjectC()
    {
        Chart chart = new Chart();
        chart.setSeries( DimensionalObject.DATA_X_DIM_ID );
        chart.setCategory( ouGroupSetA.getUid() );
        chart.getFilterDimensions().add( DimensionalObject.PERIOD_DIM_ID );
        
        chart.addDataDimensionItem( deA );
        chart.addDataDimensionItem( pdA );
        chart.addDataDimensionItem( pdB );
        
        chart.getOrganisationUnitGroups().add( ouGroupA );
        chart.getOrganisationUnitGroups().add( ouGroupB );
        chart.getOrganisationUnitGroups().add( ouGroupC );
        
        chart.getPeriods().add( PeriodType.getPeriodFromIsoString( "2012" ) );
        
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( chart );
        
        assertNotNull( params );
        assertEquals( 1, params.getDataElements().size() );
        assertEquals( 2, params.getProgramDataElements().size() );
        assertEquals( 1, params.getFilterPeriods().size() );
        assertEquals( 2, params.getDimensions().size() );
        assertEquals( 1, params.getFilters().size() );
        assertEquals( 3, params.getDimensionOptions( ouGroupSetA.getUid() ).size() );
    }
    
    @Test
    public void testGetUserOrgUnits()
    {
        String ouParam = ouA.getUid() + ";" + ouB.getUid();
        
        List<OrganisationUnit> expected = Lists.newArrayList( ouA, ouB );
        
        assertEquals( expected, dataQueryService.getUserOrgUnits( null, ouParam ) );
    }
}
