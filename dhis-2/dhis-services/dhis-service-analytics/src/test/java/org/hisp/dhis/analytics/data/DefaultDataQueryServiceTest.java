package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nManager;
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

        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "fbfJHSPpUQD" ) )
                .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "cYeuwXTCPkU" ) )
                .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "Jtf34kNZhzP" ) )
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

        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "fbfJHSPpUQD" ) )
            .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "cYeuwXTCPkU" ) )
            .thenReturn( new DataElement() );
        when( dimensionService.getDataDimensionalItemObject( IdScheme.UID, "Jtf34kNZhzP" ) )
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

    private OrganisationUnitLevel buildOrgUnitLevel( int level, String uid, String name, String code )
    {

        OrganisationUnitLevel oul = new OrganisationUnitLevel( level, name );
        oul.setUid( uid );
        oul.setCode( code );
        return oul;
    }

}