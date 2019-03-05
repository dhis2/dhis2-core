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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.data.pipeline.*;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.expressionparser.ExpressionParserService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.DataQueryParams.Builder;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.reporttable.ReportTable.IRT2D;
import static org.hisp.dhis.reporttable.ReportTable.addListIfEmpty;

/**
 * @author Lars Helge Overland
 */
public class DefaultAnalyticsService
    implements AnalyticsService {
    private static final Log log = LogFactory.getLog(DefaultAnalyticsService.class);

    private static final int MAX_QUERIES = 8;
    private static final int MAX_CACHE_ENTRIES = 20000;
    private static final String CACHE_REGION = "analyticsQueryResponse";

    private final AnalyticsManager analyticsManager;

    private final RawAnalyticsManager rawAnalyticsManager;

    private final AnalyticsSecurityManager securityManager;

    private final QueryPlanner queryPlanner;

    private final QueryValidator queryValidator;

    private final ExpressionParserService expressionParserService;

    private final ConstantService constantService;

    private final OrganisationUnitService organisationUnitService;

    private final SystemSettingManager systemSettingManager;

    private final EventAnalyticsService eventAnalyticsService;

    private final DataQueryService dataQueryService;

    private final DhisConfigurationProvider dhisConfig;

    private final CacheProvider cacheProvider;

    private final Environment environment;

    // -------------------------------------------------------------------------
    // AnalyticsService implementation
    // -------------------------------------------------------------------------

    private Cache<Grid> queryCache;

    @PostConstruct
    public void init()
    {
        Long expiration = dhisConfig.getAnalyticsCacheExpiration();
        boolean enabled = expiration > 0 && !SystemUtils.isTestRun( this.environment.getActiveProfiles() );

        queryCache = cacheProvider.newCacheBuilder( Grid.class ).forRegion( CACHE_REGION )
            .expireAfterWrite( expiration, TimeUnit.SECONDS ).withMaximumSize( enabled ? MAX_CACHE_ENTRIES : 0 ).build();

        log.info( String.format( "Analytics server-side cache is enabled: %b with expiration: %d s", enabled, expiration ) );
    }

    @Autowired
    public DefaultAnalyticsService( AnalyticsManager analyticsManager, RawAnalyticsManager rawAnalyticsManager,
        AnalyticsSecurityManager securityManager, QueryPlanner queryPlanner, QueryValidator queryValidator,
        ExpressionParserService expressionParserService, ConstantService constantService,
        OrganisationUnitService organisationUnitService, SystemSettingManager systemSettingManager,
        EventAnalyticsService eventAnalyticsService, DataQueryService dataQueryService,
        DhisConfigurationProvider dhisConfig, CacheProvider cacheProvider, Environment environment)
    {
        checkNotNull( analyticsManager );
        checkNotNull( rawAnalyticsManager );
        checkNotNull( securityManager );
        checkNotNull( queryPlanner );
        checkNotNull( queryValidator );
        checkNotNull( expressionParserService );
        checkNotNull( constantService );
        checkNotNull( organisationUnitService );
        checkNotNull( systemSettingManager );
        checkNotNull( eventAnalyticsService );
        checkNotNull( dataQueryService );
        checkNotNull( dhisConfig );
        checkNotNull( cacheProvider );
        checkNotNull( environment );

        this.analyticsManager = analyticsManager;
        this.rawAnalyticsManager = rawAnalyticsManager;
        this.securityManager = securityManager;
        this.queryPlanner = queryPlanner;
        this.queryValidator = queryValidator;
        this.expressionParserService = expressionParserService;
        this.constantService = constantService;
        this.organisationUnitService = organisationUnitService;
        this.systemSettingManager = systemSettingManager;
        this.eventAnalyticsService = eventAnalyticsService;
        this.dataQueryService = dataQueryService;
        this.dhisConfig = dhisConfig;
        this.cacheProvider = cacheProvider;
        this.environment = environment;
    }

    private Function<DataQueryParams, Grid> getAggregatedDataValueGridInternal = params -> {

        params = preHandleQuery( params );
        Grid grid = new ListGrid();

        getAnalyticsPipeline()
                .init(params, grid)
                .add(new addHeaderStep())
                .add(new addIndicatorValueStep(this.getAggregatedDataValueGridInternal))
                .add(new addDataElementValuesStep())
                .add(new addDataElementOperandValuesStep())
                .add(new addReportingRatesStep())
                .add(new addProgramDataElementAttributeIndicatorValuesStep(getEventAnalyticsService()))
                .add(new addDynamicDimensionValuesStep())
                .add(new addValidationResultValuesStep())
                .add(new addMetaDataStep(getSecurityManager()))
                .add(new handleDataValueSetStep())
                .add(new applyIdSchemeStep())
                .process();

        postHandleGrid(params, grid);
        return grid;
    };

    @Override
    public Grid getAggregatedDataValues( DataQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Security and validation
        // ---------------------------------------------------------------------

        securityManager.decideAccess( params );

        params = securityManager.withDataApprovalConstraints( params );
        params = securityManager.withDimensionConstraints( params );

        queryValidator.validate( params );

        if ( dhisConfig.isAnalyticsCacheEnabled() )
        {
            final DataQueryParams query = DataQueryParams.newBuilder( params ).build();
            return queryCache.get( params.getKey(), key -> getAggregatedDataValueGridInternal.apply( query ) ).orElseGet( ListGrid::new );
        }

        return getAggregatedDataValueGridInternal.apply( params );
    }

    @Override
    public Grid getAggregatedDataValues( DataQueryParams params, List<String> columns, List<String> rows )
    {
        return AnalyticsUtils.isTableLayout( columns, rows ) ?
            getAggregatedDataValuesTableLayout( params, columns, rows ) :
            getAggregatedDataValues( params );
    }

    @Override
    public Grid getRawDataValues( DataQueryParams params )
    {
        securityManager.decideAccess( params );

        params = securityManager.withDataApprovalConstraints( params );
        params = securityManager.withDimensionConstraints( params );

        queryValidator.validate( params );

        return getRawDataGrid( params );
    }

    @Override
    public DataValueSet getAggregatedDataValueSet( DataQueryParams params )
    {
        DataQueryParams query = DataQueryParams.newBuilder( params )
            .withSkipMeta( false )
            .withSkipData( false )
            .withIncludeNumDen( false )
            .withOutputFormat( OutputFormat.DATA_VALUE_SET )
            .build();

        Grid grid = getAggregatedDataValueGridInternal.apply( query );

        return AnalyticsUtils.getDataValueSetFromGrid( params, grid );
    }

    @Override
    public Grid getAggregatedDataValues( AnalyticalObject object )
    {
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( object );

        return getAggregatedDataValues( params );
    }

    @Override
    public Map<String, Object> getAggregatedDataValueMapping( DataQueryParams params )
    {
        Grid grid = getAggregatedDataValues( DataQueryParams.newBuilder( params )
            .withIncludeNumDen( false ).build() );

        return AnalyticsUtils.getAggregatedDataValueMapping( grid );
    }

    @Override
    public Map<String, Object> getAggregatedDataValueMapping( AnalyticalObject object )
    {
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( object );

        return getAggregatedDataValueMapping( params );
    }

    // -------------------------------------------------------------------------
    // Private business logic methods
    // -------------------------------------------------------------------------

    /**
     * Returns a Grid with aggregated data in table layout.
     *
     * @param params the {@link DataQueryParams}.
     * @param columns the column dimensions.
     * @param rows the row dimensions.
     * @return a Grid with aggregated data in table layout.
     */
    private Grid getAggregatedDataValuesTableLayout( DataQueryParams params, List<String> columns, List<String> rows )
    {
        params.setOutputIdScheme( null );

        Grid grid = getAggregatedDataValues( params );

        ListUtils.removeEmptys( columns );
        ListUtils.removeEmptys( rows );

        queryValidator.validateTableLayout( params, columns, rows );

        ReportTable reportTable = new ReportTable();

        List<DimensionalItemObject[]> tableColumns = new ArrayList<>();
        List<DimensionalItemObject[]> tableRows = new ArrayList<>();

        if ( columns != null )
        {
            for ( String dimension : columns )
            {
                reportTable.getColumnDimensions().add( dimension );
                tableColumns.add( params.getDimensionItemArrayExplodeCoc( dimension ) );
            }
        }

        if ( rows != null )
        {
            for ( String dimension : rows )
            {
                reportTable.getRowDimensions().add( dimension );
                tableRows.add( params.getDimensionItemArrayExplodeCoc( dimension ) );
            }
        }

        reportTable
            .setGridTitle( IdentifiableObjectUtils.join( params.getFilterItems() ) )
            .setGridColumns( new CombinationGenerator<>( tableColumns.toArray( IRT2D ) ).getCombinations() )
            .setGridRows( new CombinationGenerator<>( tableRows.toArray( IRT2D ) ).getCombinations() );

        addListIfEmpty( reportTable.getGridColumns() );
        addListIfEmpty( reportTable.getGridRows() );

        reportTable.setHideEmptyRows( params.isHideEmptyRows() );
        reportTable.setHideEmptyColumns( params.isHideEmptyColumns() );
        reportTable.setShowHierarchy( params.isShowHierarchy() );

        Map<String, Object> valueMap = AnalyticsUtils.getAggregatedDataValueMapping( grid );
        return reportTable.getGrid( new ListGrid( grid.getMetaData(), grid.getInternalMetaData() ), valueMap, params.getDisplayProperty(), false );
    }

    /**
     * Returns headers, raw data and meta data as a grid.
     *
     * @param params the {@link DataQueryParams}.
     * @return a grid.
     */
    private Grid getRawDataGrid(DataQueryParams params) {
        Grid grid = new ListGrid();

        params = preHandleRawDataQuery( params );

        getAnalyticsPipeline()
                .init(params, grid)
                .add(new addHeaderStep())
                .add(new addRawDataStep(getRawAnalyticsManager()))
                .add(new addMetaDataStep(getSecurityManager()))
                .add(new applyIdSchemeStep())
                .process();
        
        return grid;
    }

    /**
     * Prepares the given data query parameters.
     *
     * @param params the {@link DataQueryParams}.
     */
    private DataQueryParams preHandleRawDataQuery( DataQueryParams params )
    {
        Builder builder = DataQueryParams.newBuilder( params )
            .withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions()
            .withIncludePeriodStartEndDates( true );

        if ( params.isShowHierarchy() )
        {
            builder.withOrgUnitLevels( organisationUnitService.getFilledOrganisationUnitLevels() );
        }

        return builder.build();
    }

    /**
     * Performs pre-handling of the given query and returns the immutable,
     * handled query. If the query has a single indicator as item for the data
     * filter, the filter is set as a dimension and removed as a filter.
     *
     * @param params the {@link DataQueryParams}.
     * @return a {@link DataQueryParams}.
     */
    private DataQueryParams preHandleQuery( DataQueryParams params )
    {
        if ( params.hasSingleIndicatorAsDataFilter() || params.hasSingleReportingRateAsDataFilter() )
        {
            DimensionalObject dx = params.getFilter( DATA_X_DIM_ID );

            params = DataQueryParams.newBuilder( params )
                    .addDimension( dx )
                    .removeFilter( DATA_X_DIM_ID )
                    .addProcessingHint( ProcessingHint.SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM ).build();
        }

        return params;
    }

    /**
     * Performs post-handling of the given grid. If the query has the single
     * indicator as data filter item, the column at the data dimension index is
     * removed. If the query has sorting order, then the grid is ordered on the
     * value column based on the sorting specified.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void postHandleGrid( DataQueryParams params, Grid grid )
    {
        if ( params.hasProcessingHint( ProcessingHint.SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM ) )
        {
            grid.removeColumn( DataQueryParams.DX_INDEX );
        }

        if ( params.hasOrder() && grid.getIndexOfHeader( VALUE_ID ) >= 0 )
        {
            int orderInt = params.getOrder().equals( SortOrder.ASC ) ? -1 : 1;
            grid.sortGrid( grid.getIndexOfHeader( VALUE_ID ) + 1, orderInt );
        }
    }

    private AnalyticsPipeline getAnalyticsPipeline()
    {
        return new AnalyticsPipeline( getSystemSettingManager(), getExpressionParserService(), getQueryValidator(),
            getQueryPlanner(), getAnalyticsManager(), getConstantService(), MAX_QUERIES );
    }
    
    private SystemSettingManager getSystemSettingManager()
    {
        return systemSettingManager;
    }

    private ExpressionParserService getExpressionParserService()
    {
        return expressionParserService;
    }

    private QueryValidator getQueryValidator()
    {
        return queryValidator;
    }

    private QueryPlanner getQueryPlanner()
    {
        return queryPlanner;
    }

    private AnalyticsManager getAnalyticsManager()
    {
        return analyticsManager;
    }

    private ConstantService getConstantService()
    {
        return constantService;
    }

    private AnalyticsSecurityManager getSecurityManager()
    {
        return securityManager;
    }

    private EventAnalyticsService getEventAnalyticsService()
    {
        return eventAnalyticsService;
    }

    private RawAnalyticsManager getRawAnalyticsManager()
    {
        return rawAnalyticsManager;
    }
}