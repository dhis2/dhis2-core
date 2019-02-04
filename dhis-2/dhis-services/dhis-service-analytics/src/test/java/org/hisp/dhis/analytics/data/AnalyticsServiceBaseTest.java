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

package org.hisp.dhis.analytics.data;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expressionparser.ExpressionParserService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsServiceBaseTest
{

    @Mock
    protected AnalyticsManager analyticsManager;

    @Mock
    private RawAnalyticsManager rawAnalyticsManager;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    protected QueryPlanner queryPlanner;

    @Spy
    private DefaultQueryValidator queryValidator;

    @Mock
    private ExpressionParserService expressionParserService;

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

    @Mock
    private DhisConfigurationProvider dhisConfig;

    @Mock
    private CacheProvider cacheProvider;

    protected AnalyticsService target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        target = new DefaultAnalyticsService( analyticsManager, rawAnalyticsManager, securityManager, queryPlanner,
            queryValidator, expressionParserService, constantService, organisationUnitService, systemSettingManager,
            eventAnalyticsService, dataQueryService, dhisConfig, cacheProvider );

        doNothing().when( queryValidator ).validateMaintenanceMode();
        when( dhisConfig.getAnalyticsCacheExpiration() ).thenReturn( 0L );
    }

    protected void initMock( DataQueryParams params )
    {
        when( securityManager.withDataApprovalConstraints( any( DataQueryParams.class ) ) ).thenReturn( params );
        when( securityManager.withDimensionConstraints( any( DataQueryParams.class ) ) ).thenReturn( params );
        when( queryPlanner.planQuery( any( DataQueryParams.class ), any( QueryPlannerParams.class ) ) ).thenReturn(
            DataQueryGroups.newBuilder().withQueries( newArrayList( DataQueryParams.newBuilder().build() ) ).build() );
        Map<String, Object> aggregatedValues = new HashMap<>();
        when( analyticsManager.getAggregatedDataValues( any( DataQueryParams.class ),
            eq( AnalyticsTableType.DATA_VALUE ), eq( 0 ) ) )
                .thenReturn( CompletableFuture.completedFuture( aggregatedValues ) );

    }
}
