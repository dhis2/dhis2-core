package org.hisp.dhis.analytics.data;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.DimensionalAggregation;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AnalyticsServiceMetadataTest
{
    @Mock
    private AnalyticsManager analyticsManager;

    @Mock
    private RawAnalyticsManager rawAnalyticsManager;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    private QueryPlanner queryPlanner;

    @Spy
    private DefaultQueryValidator queryValidator;

    @Mock
    private ExpressionService expressionService;

    @Mock
    private ConstantService constantService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private EventAnalyticsService eventAnalyticsService;

    @Mock
    private DataQueryService dataQueryService;

    private AnalyticsService target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        target = new DefaultAnalyticsService( analyticsManager, rawAnalyticsManager, securityManager, queryPlanner,
            queryValidator, expressionService, constantService, organisationUnitService, systemSettingManager,
            eventAnalyticsService, dataQueryService );
        doNothing().when( queryValidator ).validateMaintenanceMode();
    }

    @Test
    public void metadataContainsOuLevelData()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
                // PERIOD
            .withPeriod( new Period( YearlyPeriodType.getPeriodFromIsoString( "2017W10" ) ) )
                // DATA ELEMENTS
            .withDataElements( newArrayList( createDataElement( 'A', new CategoryCombo() ) ) ).withIgnoreLimit( true )
                // FILTERS (OU)
            .withFilters(Collections.singletonList(new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, null,
                            DISPLAY_NAME_ORGUNIT, new DimensionalAggregation(
                    Lists.newArrayList(buildOrgUnitLevel(2, "wjP19dkFeIk", "District", null), buildOrgUnitLevel(1, "tTUf91fCytl", "Chiefdom", "OU_12345"))),
                    ImmutableList.of(new OrganisationUnit("aaa", "aaa", "OU_1", null, null, "c1"), new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")))))
                .build();

        when( securityManager.withDataApprovalConstraints( any( DataQueryParams.class ) ) ).thenReturn( params );
        when( securityManager.withDimensionConstraints( any( DataQueryParams.class ) ) ).thenReturn( params );
        when( queryPlanner.planQuery( any( DataQueryParams.class ), any( QueryPlannerParams.class ) ) ).thenReturn(
            DataQueryGroups.newBuilder().withQueries( newArrayList( DataQueryParams.newBuilder().build() ) ).build() );
        Map<String, Object> aggregatedValues = new HashMap<>();
        when( analyticsManager.getAggregatedDataValues( any( DataQueryParams.class ),
            eq( AnalyticsTableType.DATA_VALUE ), eq( 0 ) ) )
                .thenReturn( CompletableFuture.completedFuture( aggregatedValues ) );

        Grid grid = target.getAggregatedDataValues( params );


        Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");
        assertThat(items.get("wjP19dkFeIk"), allOf(hasProperty("name", is("District")),
                hasProperty("uid", is("wjP19dkFeIk")),
                hasProperty("code", is(nullValue()))));
        assertThat(items.get("tTUf91fCytl"), allOf(hasProperty("name", is("Chiefdom")),
                hasProperty("uid", is("tTUf91fCytl")),
                hasProperty("code", is("OU_12345"))));

    }

    private OrganisationUnitLevel buildOrgUnitLevel(int level, String uid, String name, String code) {

        OrganisationUnitLevel oul = new OrganisationUnitLevel(level, name);
        oul.setUid(uid);
        oul.setCode(code);
        return oul;
    }
}
