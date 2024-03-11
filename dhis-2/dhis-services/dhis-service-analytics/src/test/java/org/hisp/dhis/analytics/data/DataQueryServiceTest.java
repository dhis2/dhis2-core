package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.dataelement.DataElement;
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
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    private ProgramDataElementDimensionItem pdA;
    private ProgramDataElementDimensionItem pdB;

    private CategoryOptionCombo cocA;

    private ReportingRate rrA;
    private ReportingRate rrB;
    private ReportingRate rrC;

    private IndicatorType itA;

    private Indicator inA;
    private Indicator inB;

    private TrackedEntityAttribute atA;
    private TrackedEntityAttribute atB;

    private ProgramTrackedEntityAttributeDimensionItem patA;
    private ProgramTrackedEntityAttributeDimensionItem patB;

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
    private CategoryService categoryService;

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

        pdA = new ProgramDataElementDimensionItem( prA, deE );
        pdB = new ProgramDataElementDimensionItem( prA, deF );

        cocA = categoryService.getDefaultCategoryOptionCombo();

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

        patA = new ProgramTrackedEntityAttributeDimensionItem( prA, atA );
        patB = new ProgramTrackedEntityAttributeDimensionItem( prA, atB );

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
        ouGroupA.setPublicAccess( AccessStringHelper.FULL );
        ouGroupB = createOrganisationUnitGroup( 'B' );
        ouGroupB.setPublicAccess( AccessStringHelper.FULL );
        ouGroupC = createOrganisationUnitGroup( 'C' );
        ouGroupC.setPublicAccess( AccessStringHelper.FULL );

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

        deGroupA.getGroupSets().add( deGroupSetA );
        deGroupB.getGroupSets().add( deGroupSetA );
        deGroupC.getGroupSets().add( deGroupSetA );

        deGroupA.getGroupSets().add( deGroupSetA );
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

        UserAuthorityGroup role = createUserAuthorityGroup( 'A', "ALL" );

        userService.addUserAuthorityGroup( role );

        User user = createUser( 'A' );
        user.addOrganisationUnit( ouA );
        user.getUserCredentials().getUserAuthorityGroups().add( role );
        saveAndInjectUserSecurityContext( user );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetDimensionalObjects()
    {
        Set<String> dimensionParams = new LinkedHashSet<>();
        dimensionParams.add( DimensionalObject.DATA_X_DIM_ID + DIMENSION_NAME_SEP + deA.getDimensionItem() + OPTION_SEP + deB.getDimensionItem() + OPTION_SEP + rrA.getDimensionItem() );
        dimensionParams.add( DimensionalObject.ORGUNIT_DIM_ID + DIMENSION_NAME_SEP + ouA.getDimensionItem() + OPTION_SEP + ouB.getDimensionItem() );

        List<DimensionalObject> dimensionalObject = dataQueryService.getDimensionalObjects( dimensionParams, null, null, null, false, IdScheme.UID );

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

        List<DimensionalObject> dimensionalObject = dataQueryService.getDimensionalObjects( dimensionParams, null, null, null, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemCodes, null, null, null, false, false, IdScheme.CODE );

        assertEquals( DimensionalObject.DATA_X_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.DATA_X, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDisplayName() );
        assertEquals( items, actual.getItems() );
    }

    @Test
    public void testGetOrgUnitGroupSetDimensionByCode()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( ouGroupA, ouGroupB, ouGroupC );

        List<String> itemCodes = Lists.newArrayList( ouGroupA.getCode(), ouGroupB.getCode(), ouGroupC.getCode() );

        DimensionalObject actual = dataQueryService.getDimension( ouGroupSetA.getCode(), itemCodes, null, null, null, false, false, IdScheme.CODE );

        assertEquals( ouGroupSetA.getDimension(), actual.getDimension() );
        assertEquals( DimensionType.ORGANISATION_UNIT_GROUP_SET, actual.getDimensionType() );
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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.ORGUNIT_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.ORGUNIT_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.DATA_X_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.PERIOD_DIM_ID, itemUids, null, null, null, false, false, IdScheme.UID );

        assertEquals( DimensionalObject.PERIOD_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.PERIOD, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_PERIOD, actual.getDisplayName() );
        assertEquals( 7, actual.getItems().size() );
    }

    @Test
    public void testGetDimensionPeriodAndStartEndDates()
    {
        DimensionalObject actual = dataQueryService.getDimension( DimensionalObject.PERIOD_DIM_ID, Lists.newArrayList(), null, null, null, false, true, IdScheme.UID );

        assertEquals( DimensionalObject.PERIOD_DIM_ID, actual.getDimension() );
        assertEquals( DimensionType.PERIOD, actual.getDimensionType() );
        assertEquals( DataQueryParams.DISPLAY_NAME_PERIOD, actual.getDisplayName() );
        assertEquals( 0, actual.getItems().size() );
    }

    @Test
    public void testGetDimensionOrgUnitGroupSet()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( ouGroupA, ouGroupB );

        List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds( items );

        DimensionalObject actual = dataQueryService.getDimension( ouGroupSetA.getUid(), itemUids, null, null, null, false, false, IdScheme.UID );

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

        DimensionalObject actual = dataQueryService.getDimension( deGroupSetA.getUid(), itemUids, null, null, null, false, false, IdScheme.UID );

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
        dimensionParams
            .add( ouGroupSetA.getUid() + ":" + ouGroupA.getUid() + ";" + ouGroupB.getUid() + ";" + ouGroupC.getUid() );

        Set<String> filterParams = new HashSet<>();
        filterParams.add(
            "ou:" + ouA.getUid() + ";" + ouB.getUid() + ";" + ouC.getUid() + ";" + ouD.getUid() + ";" + ouE.getUid() );
        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getProgramAttributes().size() );
        assertEquals( 1, params.getFilterOrganisationUnits().size() );
    }

    @Test
    @Ignore // TODO Not working for composite identifiers with non-UID identifier schemes
    public void testGetFromUrlWithCodeA()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getCode() + ";" + deB.getCode() + ";" + patA.getDimensionItem( IdScheme.CODE ) + ";" + patB.getDimensionItem( IdScheme.CODE ) );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getCode() );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams )
            .inputIdScheme( IdScheme.CODE ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 2, params.getProgramAttributes().size() );
        assertEquals( 1, params.getFilterOrganisationUnits().size() );
    }

    @Test
    public void testGetFromUrlWithCodeB()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getCode() + ";" + deB.getCode() + ";" + inA.getCode() );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getCode() );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams )
            .inputIdScheme( IdScheme.CODE ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

        assertEquals( 2, params.getDataElements().size() );
        assertEquals( 1, params.getIndicators().size() );
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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

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

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        dataQueryService.getFromRequest( dataQueryRequest );
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlNoPeriods()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D" );
        dimensionParams.add( "pe" );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        dataQueryService.getFromRequest( dataQueryRequest );
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlNoOrganisationUnits()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D" );
        dimensionParams.add( "ou" );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        dataQueryService.getFromRequest( dataQueryRequest );
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetFromUrlInvalidDimension()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D" );
        dimensionParams.add( "yebo:2012,2012S1,2012S2" );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams ).build();
        dataQueryService.getFromRequest( dataQueryRequest );
    }

    @Test
    public void testGetFromUrlPeriodOrder()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid() );
        dimensionParams.add( "pe:2013;2012Q4;2012S2" );

        Set<String> filterParams = new HashSet<>();
        filterParams.add( "ou:" + ouA.getUid() );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .filter( filterParams ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

        List<DimensionalItemObject> periods = params.getPeriods();

        assertEquals( 3, periods.size() );
        assertEquals( "2013", periods.get( 0 ).getUid() );
        assertEquals( "2012Q4", periods.get( 1 ).getUid() );
        assertEquals( "2012S2", periods.get( 2 ).getUid() );
    }

    @Test
    public void testGetFromUrlNoPeriodsAllowAllPeriods()
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid() );
        dimensionParams.add( "pe" );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .allowAllPeriods( true ).build();
        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

        assertEquals( 0, params.getPeriods().size() );
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

        OrganisationUnitGroupSetDimension ouGroupSetDim = new OrganisationUnitGroupSetDimension();
        ouGroupSetDim.setDimension( ouGroupSetA );
        ouGroupSetDim.setItems( Lists.newArrayList( ouGroupA, ouGroupB, ouGroupC ) );
        chart.getOrganisationUnitGroupSetDimensions().add( ouGroupSetDim );

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

        OrganisationUnitGroupSetDimension ouGroupSetDim = new OrganisationUnitGroupSetDimension();
        ouGroupSetDim.setDimension( ouGroupSetA );
        ouGroupSetDim.setItems( Lists.newArrayList( ouGroupA, ouGroupB, ouGroupC ) );
        chart.getOrganisationUnitGroupSetDimensions().add( ouGroupSetDim );

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
