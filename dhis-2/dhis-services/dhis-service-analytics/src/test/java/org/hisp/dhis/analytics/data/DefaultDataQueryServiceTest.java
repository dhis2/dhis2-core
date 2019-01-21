package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
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

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultDataQueryService target;

    @Before
    public void setUp()
    {
        target = new DefaultDataQueryService( idObjectManager, organisationUnitService, dimensionService,
            securityManager, systemSettingManager, aclService, currentUserService, i18nManager );

    }

    @Test
    public void convertAnalyticsRequestWithOuLevelToDataQueryParam()
    {

        when( dimensionService.getDataDimensionalItemObject( UID, "fbfJHSPpUQD" ) )
                .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( UID, "cYeuwXTCPkU" ) )
                .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( UID, "Jtf34kNZhzP" ) )
                .thenReturn( new DataElement() );
        when( organisationUnitService.getOrganisationUnitLevelByLevel( 2 ) )
            .thenReturn( buildOrgUnitLevel( 2, "level2UID", "District", null ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "2" ) ).thenReturn( 2 );
        when( organisationUnitService.getOrganisationUnitsAtLevels( any( Collection.class ), any( Collection.class ) ) )
            .thenReturn( Lists.newArrayList( new OrganisationUnit(), new OrganisationUnit() ) );

        DataQueryRequest request = DataQueryRequest.newBuilder().filter( Sets.newHashSet( "ou:LEVEL-2;ImspTQPwCqd" ) )
            .dimension( Sets.newHashSet( "dx:fbfJHSPpUQD;cYeuwXTCPkU;Jtf34kNZhzP", "pe:LAST_12_MONTHS;LAST_YEAR" ) )
            .build();
        DataQueryParams params = target.getFromRequest( request );

        DimensionalObject filter = params.getFilters().get( 0 );
        assertThat( filter.getDimensionalAggregation().getGroupBy(), hasSize( 1 ) );
        assertThat( filter.getDimensionalAggregation().getGroupBy().get( 0 ),
            allOf( hasProperty( "name", is( "District" ) ), hasProperty( "uid", is( "level2UID" ) ),
                hasProperty( "code", is( nullValue() ) ) ) );
    }

    @Test
    public void convertAnalyticsRequestWithMultipleOuLevelToDataQueryParam()
    {

        when( dimensionService.getDataDimensionalItemObject( UID, "fbfJHSPpUQD" ) )
            .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( UID, "cYeuwXTCPkU" ) )
            .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( UID, "Jtf34kNZhzP" ) )
            .thenReturn( new DataElement() );
        when( organisationUnitService.getOrganisationUnitLevelByLevel( 2 ) )
            .thenReturn( buildOrgUnitLevel( 2, "level2UID", "District", null ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevel( 3 ) )
                .thenReturn( buildOrgUnitLevel( 3, "level3UID", "Chiefdom", null ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "3" ) ).thenReturn( 3 );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( "2" ) ).thenReturn( 2 );

        when( organisationUnitService.getOrganisationUnitsAtLevels( any( Collection.class ), any( Collection.class ) ) )
            .thenReturn( Lists.newArrayList( new OrganisationUnit(), new OrganisationUnit() ) );

        DataQueryRequest request = DataQueryRequest.newBuilder()
            .filter( Sets.newHashSet( "ou:LEVEL-2;LEVEL-3;ImspTQPwCqd" ) )
            .dimension( Sets.newHashSet( "dx:fbfJHSPpUQD;cYeuwXTCPkU;Jtf34kNZhzP", "pe:LAST_12_MONTHS;LAST_YEAR" ) )
            .build();
        DataQueryParams params = target.getFromRequest( request );

        DimensionalObject filter = params.getFilters().get( 0 );
        assertThat( filter.getDimensionalAggregation().getGroupBy(), hasSize( 2 ) );

        assertThat( filter.getDimensionalAggregation().getGroupBy(),
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        allOf( hasProperty( "name", is( "District" ) ),
                               hasProperty( "uid", is( "level2UID" ) ),
                               hasProperty( "code", is( nullValue() ) ) ) ,
                        allOf( hasProperty( "name", is( "Chiefdom" ) ),
                               hasProperty( "uid", is( "level3UID" ) ),
                               hasProperty( "code", is( nullValue() ) ) )));
    }

    @Test
    public void convertAnalyticsRequestWithIndicatorGroup()
    {

        when( dimensionService.getDataDimensionalItemObject( UID, "cYeuwXTCPkU" ) ).thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( UID, "Jtf34kNZhzP" ) ).thenReturn( new DataElement() );

        IndicatorGroup indicatorGroup = new IndicatorGroup( "dummy" );
        indicatorGroup.setUid("oehv9EO3vP7");
        indicatorGroup.setCode("CODE_10");
        indicatorGroup.setMembers(Sets.newHashSet(new Indicator(), new Indicator()));
        when( idObjectManager.getObject( IndicatorGroup.class, UID, "oehv9EO3vP7" ) ).thenReturn( indicatorGroup );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "goRUwCHPg1M" ) ).thenReturn( new OrganisationUnit("aaa") );
        when( idObjectManager.getObject( OrganisationUnit.class, UID, "fdc6uOvgoji" ) ).thenReturn( new OrganisationUnit("bbb") );

        when( organisationUnitService.getOrganisationUnitsAtLevels( any( Collection.class ), any( Collection.class ) ) )
            .thenReturn( Lists.newArrayList( new OrganisationUnit(), new OrganisationUnit() ) );

        DataQueryRequest request = DataQueryRequest.newBuilder()
            .filter( Sets.newHashSet( "ou:goRUwCHPg1M;fdc6uOvgoji" ) )
            .dimension(
                Sets.newHashSet( "dx:IN_GROUP-oehv9EO3vP7;cYeuwXTCPkU;Jtf34kNZhzP", "pe:LAST_12_MONTHS;LAST_YEAR" ) )
            .build();
        DataQueryParams params = target.getFromRequest( request );
        DimensionalObject dimension = params.getDimension("dx");
        assertThat( dimension.getDimensionalAggregation().getGroupBy(), hasSize( 1 ) );

        BaseIdentifiableObject aggregation = dimension.getDimensionalAggregation().getGroupBy().get(0);
        assertThat(aggregation.getUid(), is(indicatorGroup.getUid()));
        assertThat(aggregation.getCode(), is(indicatorGroup.getCode()));
        assertThat(aggregation.getName(), is(indicatorGroup.getName()));
    }

    private OrganisationUnitLevel buildOrgUnitLevel( int level, String uid, String name, String code )
    {

        OrganisationUnitLevel oul = new OrganisationUnitLevel( level, name );
        oul.setUid( uid );
        oul.setCode( code );
        return oul;
    }

}