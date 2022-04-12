/*
 * Copyright (c) 2004-2022, University of Oslo
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
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.cache.AnalyticsCacheSettings;
import org.hisp.dhis.analytics.data.handler.DataAggregator;
import org.hisp.dhis.analytics.data.handler.DataHandler;
import org.hisp.dhis.analytics.data.handler.HeaderHandler;
import org.hisp.dhis.analytics.data.handler.MetadataHandler;
import org.hisp.dhis.analytics.data.handler.SchemaIdResponseMapper;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.resolver.ExpressionResolvers;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( { MockitoExtension.class } )
abstract class AnalyticsServiceBaseTest
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
    private OrganisationUnitService organisationUnitService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    protected EventAnalyticsService eventAnalyticsService;

    @Mock
    private DataQueryService dataQueryService;

    @Mock
    private SchemaIdResponseMapper schemaIdResponseMapper;

    @Mock
    private DhisConfigurationProvider dhisConfig;

    @Mock
    private AnalyticsCache analyticsCache;

    @Mock
    private AnalyticsCacheSettings analyticsCacheSettings;

    @Mock
    private ExpressionResolvers resolvers;

    @Mock
    private NestedIndicatorCyclicDependencyInspector nestedIndicatorCyclicDependencyInspector;

    @Mock
    private ExecutionPlanStore executionPlanStore;

    DataAggregator target;

    @BeforeEach
    public void baseSetUp()
    {
        DefaultQueryValidator queryValidator = new DefaultQueryValidator( systemSettingManager );

        HeaderHandler headerHandler = new HeaderHandler();
        MetadataHandler metadataHandler = new MetadataHandler( dataQueryService, schemaIdResponseMapper );
        DataHandler dataHandler = new DataHandler( eventAnalyticsService, rawAnalyticsManager,
            resolvers, expressionService, queryPlanner, queryValidator, systemSettingManager, analyticsManager,
            organisationUnitService, executionPlanStore );

        target = new DataAggregator( headerHandler, metadataHandler, dataHandler );
        target.feedHandlers();

        when( systemSettingManager.getBooleanSetting( SettingKey.ANALYTICS_MAINTENANCE_MODE ) )
            .thenReturn( false );
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
