package org.hisp.dhis.analytics.data;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.UserOrgUnitType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalKeywords;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class DefaultDataQueryServiceTest
{
    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DimensionService dimensionService;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private AclService aclService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private I18nManager i18nManager;

    @Mock
    private I18n i18n;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultDataQueryService target;

    private final static DataElement DATA_ELEMENT_1 = buildDataElement( "fbfJHSPpUQD", "D1" );

    private final static DataElement DATA_ELEMENT_2 = buildDataElement( "cYeuwXTCPkU", "D2" );

    private final static DataElement DATA_ELEMENT_3 = buildDataElement( "Jtf34kNZhzP", "D3" );

    private final static String PERIOD_DIMENSION = "LAST_12_MONTHS;LAST_YEAR";

    private RequestBuilder rb;

    private OrganisationUnit rootOu;

    private BeanRandomizer beanRandomizer;

    @Before
    public void setUp()
    {
        target = new DefaultDataQueryService( idObjectManager, organisationUnitService, dimensionService,
            securityManager, systemSettingManager, aclService, currentUserService, i18nManager );

        rb = new RequestBuilder();

        Mockito.lenient().when( i18nManager.getI18n() ).thenReturn( i18n );
        Mockito.lenient().when( i18n.getString( "LAST_12_MONTHS") ).thenReturn( "Last 12 months" );

        rootOu = new OrganisationUnit( "Sierra Leone" );
        rootOu.setUid( CodeGenerator.generateUid() );
        rootOu.setCode( "OU_525" );

        beanRandomizer = new BeanRandomizer();
    }

    private void mockDimensionService()
    {
        when( dimensionService.getDataDimensionalItemObject( UID, DATA_ELEMENT_1.getUid() ) )
            .thenReturn( DATA_ELEMENT_1 );
        when( dimensionService.getDataDimensionalItemObject( UID, DATA_ELEMENT_2.getUid() ) )
            .thenReturn( DATA_ELEMENT_2 );
        when( dimensionService.getDataDimensionalItemObject( UID, DATA_ELEMENT_3.getUid() ) )
            .thenReturn( DATA_ELEMENT_3 );
    }

    @Test
    public void convertAnalyticsRequestWithOuLevelToDataQueryParam()
    {
        mockDimensionService();

        when( organisationUnitService.getOrganisationUnitLevelByLevel( 2 ) )
            .thenReturn( buildOrgUnitLevel( 2, "level2UID", "District", null ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "2" ) ).thenReturn( 2 );
        when( organisationUnitService.getOrganisationUnitsAtLevels( Mockito.anyList(), Mockito.anyList() ) )
            .thenReturn( Lists.newArrayList( new OrganisationUnit(), new OrganisationUnit() ) );

        rb.addOuFilter( "LEVEL-2;ImspTQPwCqd" );
        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
            .dimension( rb.getDimensionParams() ).build();

        DataQueryParams params = target.getFromRequest( request );

        DimensionalObject filter = params.getFilters().get( 0 );
        assertThat( filter.getDimensionalKeywords().getGroupBy(), hasSize( 1 ) );
        assertThat( filter.getDimensionalKeywords().getGroupBy().get( 0 ),
            allOf( hasProperty( "name", is( "District" ) ), hasProperty( "uid", is( "level2UID" ) ),
                hasProperty( "code", is( nullValue() ) ) ) );
    }

    @Test
    public void convertAnalyticsRequestWithMultipleOuLevelToDataQueryParam()
    {
        mockDimensionService();

        when( organisationUnitService.getOrganisationUnitLevelByLevel( 2 ) )
            .thenReturn( buildOrgUnitLevel( 2, "level2UID", "District", null ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevel( 3 ) )
            .thenReturn( buildOrgUnitLevel( 3, "level3UID", "Chiefdom", null ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "3" ) )
            .thenReturn( 3 );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "2" ) )
            .thenReturn( 2 );

        when( organisationUnitService.getOrganisationUnitsAtLevels( Mockito.anyList(), Mockito.anyList() ) )
            .thenReturn( Lists.newArrayList( new OrganisationUnit(), new OrganisationUnit() ) );

        rb.addOuFilter( "LEVEL-2;LEVEL-3;ImspTQPwCqd" );
        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder()
            .filter( rb.getFilterParams() )
            .dimension( rb.getDimensionParams() ).build();

        DataQueryParams params = target.getFromRequest( request );

        DimensionalObject filter = params.getFilters().get( 0 );
        assertThat( filter.getDimensionalKeywords().getGroupBy(), hasSize( 2 ) );

        assertThat( filter.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf( hasProperty( "name", is( "District" ) ), hasProperty( "uid", is( "level2UID" ) ),
                    hasProperty( "code", is( nullValue() ) ) ),
                allOf( hasProperty( "name", is( "Chiefdom" ) ), hasProperty( "uid", is( "level3UID" ) ),
                    hasProperty( "code", is( nullValue() ) ) ) ) );
    }

    @Test
    public void convertAnalyticsRequestWithIndicatorGroup()
    {
        final String INDICATOR_GROUP_UID = "oehv9EO3vP7";

        when( dimensionService.getDataDimensionalItemObject( UID, "cYeuwXTCPkU" ) ).thenReturn( new DataElement() );

        IndicatorGroup indicatorGroup = new IndicatorGroup( "dummy" );
        indicatorGroup.setUid( INDICATOR_GROUP_UID );
        indicatorGroup.setCode( "CODE_10" );
        indicatorGroup.setMembers( Sets.newHashSet( new Indicator(), new Indicator() ) );
        when( idObjectManager.getObject( IndicatorGroup.class, UID, INDICATOR_GROUP_UID ) )
            .thenReturn( indicatorGroup );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "goRUwCHPg1M" ) )
            .thenReturn( new OrganisationUnit( "aaa" ) );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "fdc6uOvgoji" ) )
            .thenReturn( new OrganisationUnit( "bbb" ) );

        rb.addOuFilter( "goRUwCHPg1M;fdc6uOvgoji" );
        rb.addDimension( "IN_GROUP-" + INDICATOR_GROUP_UID + ";cYeuwXTCPkU;Jtf34kNZhz" );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
            .dimension( rb.getDimensionParams() ).build();
        DataQueryParams params = target.getFromRequest( request );
        DimensionalObject dimension = params.getDimension( "dx" );
        assertThat( dimension.getDimensionalKeywords().getGroupBy(), hasSize( 1 ) );

        DimensionalKeywords.Keyword aggregation = dimension.getDimensionalKeywords().getGroupBy().get( 0 );

        assertThat( aggregation.getUid(), is( indicatorGroup.getUid() ) );
        assertThat( aggregation.getCode(), is( indicatorGroup.getCode() ) );
        assertThat( aggregation.getName(), is( indicatorGroup.getName() ) );
    }

    @Test
    public void convertAnalyticsRequestWithOrgUnitGroup()
    {
        mockDimensionService();

        final String ouGroupUID = "gzcv65VyaGq";

        initOrgUnitGroup( ouGroupUID );

        rb.addPeDimension( PERIOD_DIMENSION );
        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addOuDimension( "OU_GROUP-" + ouGroupUID + ";" + rootOu.getUid() );
        DataQueryRequest request = DataQueryRequest.newBuilder()
            .dimension( rb.getDimensionParams() )
            .build();
        DataQueryParams params = target.getFromRequest( request );

        assertOrgUnitGroup( ouGroupUID, params.getDimension( "ou" ) );
    }

    @Test
    public void convertAnalyticsRequestWithOrgUnitGroupAsFilter()
    {
        mockDimensionService();

        final String ouGroupUID = "gzcv65VyaGq";

        initOrgUnitGroup( ouGroupUID );

        rb.addPeDimension( PERIOD_DIMENSION );
        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addOuFilter( "OU_GROUP-" + ouGroupUID + ";" + rootOu.getUid() );

        DataQueryRequest request = DataQueryRequest.newBuilder().dimension( rb.getDimensionParams() )
            .filter( rb.getFilterParams() ).build();
        DataQueryParams params = target.getFromRequest( request );

        assertOrgUnitGroup( ouGroupUID, params.getFilter( "ou" ) );
    }

    @Test
    public void convertAnalyticsRequestWithOrgUnitLevelAsFilter()
    {
        OrganisationUnit level2OuA = new OrganisationUnit( "Bo" );
        OrganisationUnit level2OuB = new OrganisationUnit( "Bombali" );

        mockDimensionService();

        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "wjP19dkFeIk" ) ).thenReturn( 2 );

        when( idObjectManager.getObject( OrganisationUnit.class, UID, "ImspTQPwCqd" ) ).thenReturn( rootOu );

        when( organisationUnitService.getOrganisationUnitsAtLevels( Mockito.anyList(),
            Mockito.anyList() ) ).thenReturn( Lists.newArrayList( level2OuA, level2OuB ) );

        when( organisationUnitService.getOrganisationUnitLevelByLevel( 2 ) )
            .thenReturn( buildOrgUnitLevel( 2, "level2UID", "District", null ) );

        rb.addOuFilter( "LEVEL-wjP19dkFeIk;ImspTQPwCqd" );
        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
            .dimension( rb.getDimensionParams() ).build();

        DataQueryParams params = target.getFromRequest( request );
        DimensionalObject filter = params.getFilters().get( 0 );

        assertThat( filter.getDimensionalKeywords().getGroupBy(), hasSize( 2 ) );
        assertThat( filter.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf( hasProperty( "name", is( "District" ) ), hasProperty( "uid", is( "level2UID" ) ),
                        hasProperty( "code", is( nullValue() ) ) ),
                allOf( hasProperty( "name", is( "Sierra Leone" ) ), hasProperty( "uid", is( rootOu.getUid() ) ),
                        hasProperty( "code", is( rootOu.getCode() ) ) ) ) );

    }

    @Test
    public void convertAnalyticsRequestWithOrgUnitLevelAndOrgUnitGroupAsFilter()
    {
        OrganisationUnit level2OuA = new OrganisationUnit( "Bo" );
        OrganisationUnit level2OuB = new OrganisationUnit( "Bombali" );

        OrganisationUnit ou1Group = new OrganisationUnit( "ou1-group" );
        OrganisationUnit ou2Group = new OrganisationUnit( "ou2-group" );

        OrganisationUnitGroup groupOu = beanRandomizer.randomObject(OrganisationUnitGroup.class, "geometry");


        mockDimensionService();

        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "wjP19dkFeIk" ) ).thenReturn( 2 );

        when( idObjectManager.getObject( OrganisationUnit.class, UID, "ImspTQPwCqd" ) ).thenReturn( rootOu );
        when( idObjectManager.getObject( OrganisationUnitGroup.class, UID, "tDZVQ1WtwpA" ) ).thenReturn( groupOu );

        when( organisationUnitService.getOrganisationUnitsAtLevels( Mockito.anyList(),
                Mockito.anyList() ) ).thenReturn( Lists.newArrayList( level2OuA, level2OuB ) );

        when( organisationUnitService.getOrganisationUnitLevelByLevel( 2 ) )
                .thenReturn( buildOrgUnitLevel( 2, "level2UID", "District", null ) );

        when( organisationUnitService.getOrganisationUnits( Lists.newArrayList( groupOu ), Lists.newArrayList( rootOu ) ) )
            .thenReturn( Lists.newArrayList( ou1Group, ou2Group ) );

        rb.addOuFilter( "LEVEL-wjP19dkFeIk;OU_GROUP-tDZVQ1WtwpA;ImspTQPwCqd" );
        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
                .dimension( rb.getDimensionParams() ).build();

        DataQueryParams params = target.getFromRequest( request );
        DimensionalObject filter = params.getFilters().get( 0 );

        assertThat( filter.getDimensionalKeywords().getGroupBy(), hasSize( 3 ) );
        assertThat( filter.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf( hasProperty( "name", is( "District" ) ), hasProperty( "uid", is( "level2UID" ) ),
                    hasProperty( "code", is( nullValue() ) ) ),
                allOf( hasProperty( "name", is( groupOu.getName() ) ), hasProperty( "uid", is( groupOu.getUid() ) ),
                    hasProperty( "code", is( groupOu.getCode() ) ) ),
                allOf( hasProperty( "name", is( "Sierra Leone" ) ), hasProperty( "uid", is( rootOu.getUid() ) ),
                    hasProperty( "code", is( rootOu.getCode() ) ) ) ) );

    }

    @Test
    public void convertAnalyticsRequestWithDataElementGroup()
    {
        when( dimensionService.getDataDimensionalItemObject( UID, DATA_ELEMENT_2.getUid() ) )
                .thenReturn( DATA_ELEMENT_2 );
        final String DATA_ELEMENT_GROUP_UID = "oehv9EO3vP7";
        when( dimensionService.getDataDimensionalItemObject( UID, "cYeuwXTCPkU" ) ).thenReturn( new DataElement() );

        DataElementGroup dataElementGroup = new DataElementGroup( "dummy" );
        dataElementGroup.setUid( DATA_ELEMENT_GROUP_UID );
        dataElementGroup.setCode( "CODE_10" );
        dataElementGroup.setMembers( Sets.newHashSet( new DataElement(), new DataElement() ) );

        when( idObjectManager.getObject( DataElementGroup.class, UID, DATA_ELEMENT_GROUP_UID ) )
            .thenReturn( dataElementGroup );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "goRUwCHPg1M" ) )
            .thenReturn( new OrganisationUnit( "aaa" ) );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "fdc6uOvgoji" ) )
            .thenReturn( new OrganisationUnit( "bbb" ) );

        rb.addOuFilter( "goRUwCHPg1M;fdc6uOvgoji" );
        rb.addDimension( "DE_GROUP-" + DATA_ELEMENT_GROUP_UID + ";cYeuwXTCPkU;Jtf34kNZhz" );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
            .dimension( rb.getDimensionParams() ).build();
        DataQueryParams params = target.getFromRequest( request );
        DimensionalObject dimension = params.getDimension( "dx" );
        assertThat( dimension.getDimensionalKeywords().getGroupBy(), hasSize( 1 ) );

        DimensionalKeywords.Keyword aggregation = dimension.getDimensionalKeywords().getGroupBy().get( 0 );

        assertThat( aggregation.getUid(), is( dataElementGroup.getUid() ) );
        assertThat( aggregation.getCode(), is( dataElementGroup.getCode() ) );
        assertThat( aggregation.getName(), is( dataElementGroup.getName() ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertAnalyticsRequestWithDataElementGroupAndIndicatorGroup()
    {
        final String DATA_ELEMENT_GROUP_UID = "oehv9EO3vP7";
        final String INDICATOR_GROUP_UID = "iezv4GO4vD9";

        when( dimensionService.getDataDimensionalItemObject( UID, "cYeuwXTCPkU" ) ).thenReturn( new DataElement() );

        DataElementGroup dataElementGroup = new DataElementGroup( "dummyEG" );
        dataElementGroup.setUid( DATA_ELEMENT_GROUP_UID );
        dataElementGroup.setCode( "CODE_10" );
        dataElementGroup.setMembers( Sets.newHashSet( new DataElement(), new DataElement() ) );

        IndicatorGroup indicatorGroup = new IndicatorGroup( "dummyIG" );
        indicatorGroup.setUid( INDICATOR_GROUP_UID );
        indicatorGroup.setCode( "CODE_10" );
        indicatorGroup.setMembers( Sets.newHashSet( new Indicator(), new Indicator() ) );

        when( idObjectManager.getObject( DataElementGroup.class, UID, DATA_ELEMENT_GROUP_UID ) )
            .thenReturn( dataElementGroup );
        when( idObjectManager.getObject( IndicatorGroup.class, UID, INDICATOR_GROUP_UID ) )
            .thenReturn( indicatorGroup );

        when( idObjectManager.getObject( OrganisationUnit.class, UID, "goRUwCHPg1M" ) )
            .thenReturn( new OrganisationUnit( "aaa" ) );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "fdc6uOvgoji" ) )
            .thenReturn( new OrganisationUnit( "bbb" ) );

        rb.addOuFilter( "goRUwCHPg1M;fdc6uOvgoji" );
        rb.addDimension(
            "DE_GROUP-" + DATA_ELEMENT_GROUP_UID + ";cYeuwXTCPkU;Jtf34kNZhz;IN_GROUP-" + INDICATOR_GROUP_UID );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
            .dimension( rb.getDimensionParams() ).build();
        DataQueryParams params = target.getFromRequest( request );
        DimensionalObject dimension = params.getDimension( "dx" );
        assertThat( dimension.getDimensionalKeywords().getGroupBy(), hasSize( 2 ) );

        assertThat( dimension.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf( hasProperty( "name", is( indicatorGroup.getName() ) ),
                    hasProperty( "uid", is( indicatorGroup.getUid() ) ),
                    hasProperty( "code", is( indicatorGroup.getCode() ) ) ),
                allOf( hasProperty( "name", is( dataElementGroup.getName() ) ),
                    hasProperty( "uid", is( dataElementGroup.getUid() ) ),
                    hasProperty( "code", is( dataElementGroup.getCode() ) ) ) ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertAnalyticsRequestWithRelativePeriod()
    {
        mockDimensionService();
        when (i18n.getString( "LAST_YEAR")).thenReturn("Last year");

        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addPeDimension( PERIOD_DIMENSION );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
                .dimension( rb.getDimensionParams() ).build();

        DataQueryParams params = target.getFromRequest( request );

        DimensionalObject dimension = params.getDimension(PERIOD_DIM_ID);
        assertThat( dimension.getDimensionalKeywords().getGroupBy(), hasSize( 2 ) );
        assertThat( dimension.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf(
                    hasProperty( "name", is( "Last year" ) ),
                    hasProperty( "key", is( "LAST_YEAR" ) ),
                    hasProperty( "code", is( nullValue() ) ) ),
                allOf(
                    hasProperty( "name", is( "Last 12 months" ) ),
                    hasProperty( "key", is( "LAST_12_MONTHS" ) ),
                    hasProperty( "code", is( nullValue() ) ) ) ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertAnalyticsRequestWithRelativePeriodAsFilter()
    {
        mockDimensionService();
        when (i18n.getString( "LAST_YEAR")).thenReturn("Last year");

        rb.addDimension( concatenateUuid( DATA_ELEMENT_1, DATA_ELEMENT_2, DATA_ELEMENT_3 ) );
        rb.addPeDimension( PERIOD_DIMENSION );
        rb.addFilter("pe", "QUARTERS_THIS_YEAR");

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( rb.getFilterParams() )
                .dimension( rb.getDimensionParams() ).build();

        DataQueryParams params = target.getFromRequest( request );

        DimensionalObject dimension = params.getDimension(PERIOD_DIM_ID);
        assertThat( dimension.getDimensionalKeywords().getGroupBy(), hasSize( 2 ) );
        assertThat( dimension.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf(
                    hasProperty( "name", is( "Last year" ) ),
                    hasProperty( "key", is( "LAST_YEAR" ) ),
                    hasProperty( "code", is( nullValue() ) ) ),
                allOf(
                    hasProperty( "name", is( "Last 12 months" ) ),
                    hasProperty( "key", is( "LAST_12_MONTHS" ) ),
                    hasProperty( "code", is( nullValue() ) ) ) ) );
    }

    @Test
    public void verifyGetOrgUnitsBasedOnUserOrgUnitType()
    {
        testGetUserOrgUnits( UserOrgUnitType.DATA_CAPTURE );
        testGetUserOrgUnits( UserOrgUnitType.TEI_SEARCH );
        testGetUserOrgUnits( UserOrgUnitType.DATA_OUTPUT );
    }

    private void testGetUserOrgUnits( UserOrgUnitType userOrgUnitType )
    {
        int orgUnitSize = 10;
        User user = new User();

        Set<OrganisationUnit> orgUnits = new HashSet<>(
            beanRandomizer.randomObjects( OrganisationUnit.class, orgUnitSize, "geometry", "parent", "groups", "children" ) );

        switch ( userOrgUnitType )
        {
        case DATA_CAPTURE:
            user.setOrganisationUnits( orgUnits );
            break;
        case DATA_OUTPUT:
            user.setDataViewOrganisationUnits( orgUnits );
            break;
        case TEI_SEARCH:
            user.setTeiSearchOrganisationUnits( orgUnits );
            break;
        }

        DataQueryRequest request = DataQueryRequest.newBuilder().userOrgUnitType( userOrgUnitType ).build();

        DataQueryParams params = target.getFromRequest( request );

        when( securityManager.getCurrentUser( params ) ).thenReturn( user );

        List<OrganisationUnit> result = target.getUserOrgUnits( params, null );
        assertThat( result, hasSize( orgUnitSize ) );

        // Check collection is sorted
        assertTrue( Ordering.natural().isOrdered( result ) );
    }

    private void initOrgUnitGroup( String ouGroupUID )
    {
        when( idObjectManager.getObject( OrganisationUnitGroup.class, UID, ouGroupUID ) )
            .thenReturn( buildOrganizationalUnitGroup( ouGroupUID, "Chiefdom", "CODE_001" ) );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, this.rootOu.getUid() ) )
            .thenReturn( rootOu );
        when( organisationUnitService.getOrganisationUnits( Mockito.anyList(), Mockito.anyList() ) )
            .thenReturn( Lists.newArrayList( new OrganisationUnit(), new OrganisationUnit() ) );
    }

    private void assertOrgUnitGroup( String ouGroupUID, DimensionalObject dimension )
    {
        assertThat( dimension.getDimensionalKeywords().getGroupBy(), hasSize( 2 ) );

        assertThat( dimension.getDimensionalKeywords().getGroupBy(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf( hasProperty( "name", is( "Chiefdom" ) ), hasProperty( "uid", is( ouGroupUID ) ),
                        hasProperty( "code", is( "CODE_001" ) ) ),
                allOf( hasProperty( "name", is( "Sierra Leone" ) ), hasProperty( "uid", is( rootOu.getUid() ) ),
                        hasProperty( "code", is( rootOu.getCode() ) ) ) ) );
    }

    private OrganisationUnitLevel buildOrgUnitLevel( int level, String uid, String name, String code )
    {

        OrganisationUnitLevel oul = new OrganisationUnitLevel( level, name );
        oul.setUid( uid );
        oul.setCode( code );
        return oul;
    }

    private static DataElement buildDataElement( String uid, String name )
    {

        DataElement d = new DataElement( name );
        d.setUid( uid );
        return d;
    }

    private OrganisationUnitGroup buildOrganizationalUnitGroup( String uid, String name, String code )
    {
        OrganisationUnitGroup oug = new OrganisationUnitGroup( name );
        oug.setUid( uid );
        oug.setCode( code );
        return oug;
    }

    private String concatenateUuid( DataElement required, DataElement... additional )
    {
        Stream.Builder<DataElement> builder = Stream.<DataElement> builder().add( required );
        for ( DataElement s : additional )
        {
            builder.add( s );
        }

        return builder.build().map( BaseIdentifiableObject::getUid ).collect( Collectors.joining( ";" ) );
    }

    class RequestBuilder
    {
        private Set<String> dimensionParams = new HashSet<>();
        private Set<String> filterParams = new HashSet<>();

        void addDimension( String key, String value )
        {
            dimensionParams.add( key + ":" + value );
        }

        void addOuDimension( String value )
        {
            addDimension( "ou", value );
        }

        void addDimension( String value )
        {
            addDimension( "dx", value );
        }

        void addPeDimension( String value )
        {
            addDimension( "pe", value );
        }

        void addFilter( String key, String value )
        {
            filterParams.add( key + ":" + value );
        }

        void addOuFilter( String value )
        {
            addFilter( "ou", value );
        }

        Set<String> getDimensionParams()
        {
            return dimensionParams;
        }

        Set<String> getFilterParams()
        {
            return filterParams;
        }
    }
}