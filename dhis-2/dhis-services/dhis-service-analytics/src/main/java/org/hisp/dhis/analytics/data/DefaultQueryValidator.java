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

import static org.hisp.dhis.analytics.DataQueryParams.COMPLETENESS_DIMENSION_TYPES;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class DefaultQueryValidator
    implements QueryValidator
{
    private static final Log log = LogFactory.getLog( DefaultQueryValidator.class );

    @Autowired
    private SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // QueryValidator implementation
    // -------------------------------------------------------------------------

    @Override
    public void validate( DataQueryParams params )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        final List<DimensionalItemObject> dataElements = Lists.newArrayList( params.getDataElements() );
        params.getProgramDataElements().stream().forEach( pde -> dataElements.add( ((ProgramDataElementDimensionItem) pde).getDataElement() ) );        
        final List<DataElement> nonAggDataElements = FilterUtils.inverseFilter( asTypedList( dataElements ), AggregatableDataElementFilter.INSTANCE );

        if ( params.getDimensions().isEmpty() )
        {
            violation = "At least one dimension must be specified";
        }

        if ( !params.getDimensionsAsFilters().isEmpty() )
        {
            violation = "Dimensions cannot be specified as dimension and filter simultaneously: " + params.getDimensionsAsFilters();
        }

        if ( !params.hasPeriods() && !params.isSkipPartitioning() && !params.hasStartEndDate() )
        {
            violation = "At least one period must be specified as dimension or filter";
        }
        
        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            violation = "Periods and start and end dates cannot be specified simultaneously";
        }

        if ( !params.getFilterIndicators().isEmpty() && params.getFilterOptions( DATA_X_DIM_ID ).size() > 1 )
        {
            violation = "Only a single indicator can be specified as filter";
        }

        if ( !params.getFilterReportingRates().isEmpty() && params.getFilterOptions( DATA_X_DIM_ID ).size() > 1 )
        {
            violation = "Only a single reporting rate can be specified as filter";
        }

        if ( params.getFilters().contains( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID ) ) )
        {
            violation = "Category option combos cannot be specified as filter";
        }

        if ( !params.getDuplicateDimensions().isEmpty() )
        {
            violation = "Dimensions cannot be specified more than once: " + params.getDuplicateDimensions();
        }
        
        if ( !params.getAllReportingRates().isEmpty() && !params.containsOnlyDimensionsAndFilters( COMPLETENESS_DIMENSION_TYPES ) )
        {
            violation = "Reporting rates can only be specified together with dimensions of type: " + COMPLETENESS_DIMENSION_TYPES;
        }

        if ( params.hasDimensionOrFilter( CATEGORYOPTIONCOMBO_DIM_ID ) && params.getAllDataElements().isEmpty() )
        {
            violation = "Assigned categories cannot be specified when data elements are not specified";
        }

        if ( params.hasDimensionOrFilter( CATEGORYOPTIONCOMBO_DIM_ID ) && ( params.getAllDataElements().size() != params.getAllDataDimensionItems().size() ) )
        {
            violation = "Assigned categories can only be specified together with data elements, not indicators or reporting rates";
        }
        
        if ( !nonAggDataElements.isEmpty() )
        {
            violation = "Data elements must be of a value and aggregation type that allow aggregation: " + getUids( nonAggDataElements );
        }
        
        if ( params.isOutputFormat( OutputFormat.DATA_VALUE_SET ) )
        {
            if ( !params.hasDimension( DATA_X_DIM_ID ) )
            {
                violation = "A data dimension 'dx' must be specified when output format is DATA_VALUE_SET";
            }
            
            if ( !params.hasDimension( PERIOD_DIM_ID ) )
            {
                violation = "A period dimension 'pe' must be specified when output format is DATA_VALUE_SET";
            }
                        
            if ( !params.hasDimension( ORGUNIT_DIM_ID ) )
            {
                violation = "An organisation unit dimension 'ou' must be specified when output format is DATA_VALUE_SET";
            }
        }

        if ( violation != null )
        {
            log.warn( String.format( "Analytics validation failed: %s", violation ) );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public void validateTableLayout( DataQueryParams params, List<String> columns, List<String> rows )
    {
        String violation = null;

        if ( columns != null )
        {
            for ( String column : columns )
            {
                if ( !params.hasDimension( column ) )
                {
                    violation = "Column must be present as dimension in query: " + column;
                }
            }
        }

        if ( rows != null )
        {
            for ( String row : rows )
            {
                if ( !params.hasDimension( row ) )
                {
                    violation = "Row must be present as dimension in query: " + row;
                }
            }
        }

        if ( violation != null )
        {
            log.warn( String.format( "Validation failed: %s", violation ) );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public void validateMaintenanceMode()
        throws MaintenanceModeException
    {
        boolean maintenance = (Boolean) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAINTENANCE_MODE );

        if ( maintenance )
        {
            throw new MaintenanceModeException( "Analytics engine is in maintenance mode, try again later" );
        }
    }

}
