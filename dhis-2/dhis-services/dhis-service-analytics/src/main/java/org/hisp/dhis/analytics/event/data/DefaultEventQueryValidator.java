package org.hisp.dhis.analytics.event.data;

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
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DefaultEventQueryValidator
    implements EventQueryValidator
{
    private static final Log log = LogFactory.getLog( DefaultEventQueryValidator.class );
    
    @Autowired
    private QueryValidator queryValidator;
    
    @Autowired
    private SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // EventQueryValidator implementation
    // -------------------------------------------------------------------------

    @Override
    public void validate( EventQueryParams params )
        throws IllegalQueryException, MaintenanceModeException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }
        
        queryValidator.validateMaintenanceMode();
        
        if ( !params.hasOrganisationUnits() )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( !params.getDuplicateDimensions().isEmpty() )
        {
            violation = "Dimensions cannot be specified more than once: " + params.getDuplicateDimensions();
        }
        
        if ( !params.getDuplicateQueryItems().isEmpty() )
        {
            violation = "Query items cannot be specified more than once: " + params.getDuplicateQueryItems();
        }
        
        if ( params.hasValueDimension() && params.getDimensionalObjectItems().contains( params.getValue() ) )
        {
            violation = "Value dimension cannot also be specified as an item or item filter";
        }
        
        if ( params.hasAggregationType() && !( params.hasValueDimension() || params.isAggregateData() ) )
        {
            violation = "Value dimension or aggregate data must be specified when aggregation type is specified";
        }
        
        if ( !params.hasPeriods() && ( params.getStartDate() == null || params.getEndDate() == null ) )
        {
            violation = "Start and end date or at least one period must be specified";
        }
        
        if ( params.getStartDate() != null && params.getEndDate() != null && params.getStartDate().after( params.getEndDate() ) )
        {
            violation = "Start date is after end date: " + params.getStartDate() + " - " + params.getEndDate();
        }

        if ( params.getPage() != null && params.getPage() <= 0 )
        {
            violation = "Page number must be a positive number: " + params.getPage();
        }
        
        if ( params.getPageSize() != null && params.getPageSize() < 0 )
        {
            violation = "Page size must be zero or a positive number: " + params.getPageSize();
        }
        
        if ( params.hasLimit() && getMaxLimit() > 0 && params.getLimit() > getMaxLimit() )
        {
            violation = "Limit of: " + params.getLimit() + " is larger than max limit: " + getMaxLimit();
        }
        
        if ( params.hasClusterSize() && params.getClusterSize() <= 0 )
        {
            violation = "Cluster size must be a positive number: " + params.getClusterSize();
        }
        
        if ( params.hasBbox() && !ValidationUtils.bboxIsValid( params.getBbox() ) )
        {
            violation = "Bbox is invalid: " + params.getBbox() + ", must be on format: 'min-lng,min-lat,max-lng,max-lat'";
        }
        
        if ( ( params.hasBbox() || params.hasClusterSize() ) && params.getCoordinateField() == null )
        {
            violation = "Cluster field must be specified when bbox or cluster size are specified";
        }

        for ( QueryItem item : params.getItemsAndItemFilters() )
        {
            if ( item.hasLegendSet() && item.hasOptionSet() )
            {
                violation = "Query item cannot specify both legend set and option set: " + item.getItemId();
            }
            
            if ( params.isAggregateData() && !item.getAggregationType().isAggregateable() )
            {
                violation = "Query item must be aggregateable when used in aggregate query: " + item.getItemId();
            }
        }
        
        if ( violation != null )
        {
            log.warn( String.format( "Event analytics validation failed: %s", violation ) );
            
            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public void validateTableLayout( EventQueryParams params, List<String> columns, List<String> rows )
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
    public int getMaxLimit()
    {
        return (Integer) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAX_LIMIT );
    }
}
