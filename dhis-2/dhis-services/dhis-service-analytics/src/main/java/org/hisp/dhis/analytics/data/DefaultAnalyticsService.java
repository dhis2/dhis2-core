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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getDataValueSetFromGrid;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.isTableLayout;
import static org.hisp.dhis.commons.collection.ListUtils.removeEmptys;
import static org.hisp.dhis.visualization.Visualization.addListIfEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.data.handler.DataAggregator;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.AnalyticsService" )
public class DefaultAnalyticsService
    implements AnalyticsService
{
    private final AnalyticsSecurityManager securityManager;

    private final QueryValidator queryValidator;

    private final DataQueryService dataQueryService;

    private final AnalyticsCache analyticsCache;

    private final DataAggregator dataAggregator;

    // -------------------------------------------------------------------------
    // AnalyticsService implementation
    // -------------------------------------------------------------------------

    @Autowired
    public DefaultAnalyticsService( AnalyticsSecurityManager securityManager, QueryValidator queryValidator,
        DataQueryService dataQueryService, AnalyticsCache analyticsCache, DataAggregator dataAggregator )
    {
        checkNotNull( securityManager );
        checkNotNull( queryValidator );
        checkNotNull( dataQueryService );
        checkNotNull( analyticsCache );
        checkNotNull( dataAggregator );

        this.securityManager = securityManager;
        this.queryValidator = queryValidator;
        this.dataQueryService = dataQueryService;
        this.analyticsCache = analyticsCache;
        this.dataAggregator = dataAggregator;
    }

    @Override
    public Grid getAggregatedDataValues( DataQueryParams params )
    {
        params = checkSecurityConstraints( params );

        queryValidator.validate( params );

        if ( analyticsCache.isEnabled() && !params.analyzeOnly() )
        {
            final DataQueryParams immutableParams = newBuilder( params ).build();

            return analyticsCache.getOrFetch( params,
                p -> dataAggregator.getAggregatedDataValueGrid( immutableParams ) );
        }

        return dataAggregator.getAggregatedDataValueGrid( params );
    }

    @Override
    public Grid getAggregatedDataValues( DataQueryParams params, List<String> columns, List<String> rows )
    {
        return isTableLayout( columns, rows ) ? getAggregatedDataValuesTableLayout( params, columns, rows )
            : getAggregatedDataValues( params );
    }

    @Override
    public Grid getRawDataValues( DataQueryParams params )
    {
        params = checkSecurityConstraints( params );

        queryValidator.validate( params );

        return dataAggregator.getRawDataGrid( params );
    }

    @Override
    public DataValueSet getAggregatedDataValueSet( DataQueryParams params )
    {
        Grid grid = getAggregatedDataValueSetAsGrid( params );

        return getDataValueSetFromGrid( params, grid );
    }

    @Override
    public Grid getAggregatedDataValueSetAsGrid( DataQueryParams params )
    {
        DataQueryParams query = newBuilder( params )
            .withSkipMeta( false )
            .withSkipData( false )
            .withIncludeNumDen( false )
            .withOutputFormat( DATA_VALUE_SET )
            .build();

        return dataAggregator.getAggregatedDataValueGrid( query );
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
        Grid grid = getAggregatedDataValues( newBuilder( params )
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
     * Check the common security constraints that should be applied to the given
     * params. Decide access, add constraints and validate.
     *
     * @param params
     * @return the params after the security constraints appliance.
     */
    private DataQueryParams checkSecurityConstraints( DataQueryParams params )
    {
        securityManager.decideAccess( params );

        params = securityManager.withDataApprovalConstraints( params );
        params = securityManager.withUserConstraints( params );

        return params;
    }

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

        removeEmptys( columns );
        removeEmptys( rows );

        queryValidator.validateTableLayout( params, columns, rows );
        queryValidator.validate( params );

        final Visualization visualization = new Visualization();

        List<List<DimensionalItemObject>> tableColumns = new ArrayList<>();
        List<List<DimensionalItemObject>> tableRows = new ArrayList<>();

        if ( columns != null )
        {
            for ( String dimension : columns )
            {
                visualization.addDimensionDescriptor( dimension, params.getDimension( dimension ).getDimensionType() );

                visualization.getColumnDimensions().add( dimension );
                tableColumns.add( params.getDimensionItemsExplodeCoc( dimension ) );
            }
        }

        if ( rows != null )
        {
            for ( String dimension : rows )
            {
                visualization.addDimensionDescriptor( dimension, params.getDimension( dimension ).getDimensionType() );

                visualization.getRowDimensions().add( dimension );
                tableRows.add( params.getDimensionItemsExplodeCoc( dimension ) );
            }
        }

        visualization
            .setGridTitle( IdentifiableObjectUtils.join( params.getFilterItems() ) )
            .setGridColumns( CombinationGenerator.newInstance( tableColumns ).getCombinations() )
            .setGridRows( CombinationGenerator.newInstance( tableRows ).getCombinations() );

        addListIfEmpty( visualization.getGridColumns() );
        addListIfEmpty( visualization.getGridRows() );

        visualization.setHideEmptyRows( params.isHideEmptyRows() );
        visualization.setHideEmptyColumns( params.isHideEmptyColumns() );
        visualization.setShowHierarchy( params.isShowHierarchy() );

        Map<String, Object> valueMap = AnalyticsUtils.getAggregatedDataValueMapping( grid );

        return visualization.getGrid( new ListGrid( grid.getMetaData(), grid.getInternalMetaData() ), valueMap,
            params.getDisplayProperty(), false );
    }
}
