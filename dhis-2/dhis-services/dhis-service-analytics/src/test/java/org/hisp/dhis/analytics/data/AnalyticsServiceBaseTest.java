package org.hisp.dhis.analytics.data;

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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.cache.AnalyticsCacheSettings;
import org.hisp.dhis.analytics.data.handling.DataAggregator;
import org.hisp.dhis.analytics.data.handling.DataHandler;
import org.hisp.dhis.analytics.data.handling.HeaderHandler;
import org.hisp.dhis.analytics.data.handling.MetadataHandler;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.resolver.ExpressionResolver;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Luciano Fiandesio
 */
@RunWith( MockitoJUnitRunner.Silent.class )
public abstract class AnalyticsServiceBaseTest
{

    @Mock
    protected AnalyticsManager analyticsManager;

    @Mock
    private RawAnalyticsManager rawAnalyticsManager;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    private QueryPlanner queryPlanner;

    @Mock
    private ExpressionService expressionService;

    @Mock
    private ConstantService constantService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    protected EventAnalyticsService eventAnalyticsService;

    @Mock
    private DataQueryService dataQueryService;

    @Mock
    private DhisConfigurationProvider dhisConfig;

    @Mock
    private AnalyticsCache analyticsCache;

    @Mock
    private AnalyticsCacheSettings analyticsCacheSettings;

    @Mock
    private ExpressionResolver resolver;

    @Mock
    private NestedIndicatorCyclicDependencyInspector nestedIndicatorCyclicDependencyInspector;

    DataAggregator target;

    @Before
    public void baseSetUp()
    {
        DefaultQueryValidator queryValidator = new DefaultQueryValidator( systemSettingManager,
            nestedIndicatorCyclicDependencyInspector );

        HeaderHandler headerHandler = new HeaderHandler();
        MetadataHandler metadataHandler = new MetadataHandler( dataQueryService );
        DataHandler dataHandler = new DataHandler( eventAnalyticsService, rawAnalyticsManager, constantService,
            resolver, expressionService, queryPlanner, queryValidator, systemSettingManager, analyticsManager,
            organisationUnitService );

        target = new DataAggregator( headerHandler, metadataHandler, dataHandler );
        target.feedHandlers();

        when( systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAINTENANCE_MODE ) ).thenReturn( false );
        when( analyticsCacheSettings.fixedExpirationTimeOrDefault() ).thenReturn( 0L );
    }

    void initMock( DataQueryParams params )
    {
        when( securityManager.withDataApprovalConstraints( Mockito.any( DataQueryParams.class ) ) )
            .thenReturn( params );
        when( securityManager.withUserConstraints( any( DataQueryParams.class ) ) ).thenReturn( params );
        when( queryPlanner.planQuery( any( DataQueryParams.class ), any( QueryPlannerParams.class ) ) ).thenReturn(
            DataQueryGroups.newBuilder().withQueries( newArrayList( DataQueryParams.newBuilder().build() ) ).build() );
    }
}
