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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component( "org.hisp.dhis.analytics.event.EventQueryValidator" )
@RequiredArgsConstructor
public class DefaultEventQueryValidator
    implements EventQueryValidator
{
    private final QueryValidator queryValidator;

    private final SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // EventQueryValidator implementation
    // -------------------------------------------------------------------------

    @Override
    public void validate( EventQueryParams params )
        throws IllegalQueryException,
        MaintenanceModeException
    {
        queryValidator.validateMaintenanceMode();

        ErrorMessage error = validateForErrorMessage( params );

        if ( error != null )
        {
            log.warn( String.format(
                "Event analytics validation failed, code: '%s', message: '%s'",
                error.getErrorCode(), error.getMessage() ) );

            throw new IllegalQueryException( error );
        }
    }

    @Override
    public ErrorMessage validateForErrorMessage( EventQueryParams params )
    {
        ErrorMessage error = null;

        if ( params == null )
        {
            throw new IllegalQueryException( ErrorCode.E7100 );
        }
        else if ( !params.hasOrganisationUnits() )
        {
            error = new ErrorMessage( ErrorCode.E7200 );
        }
        else if ( !params.getDuplicateDimensions().isEmpty() )
        {
            error = new ErrorMessage( ErrorCode.E7201, params.getDuplicateDimensions() );
        }
        else if ( !params.getDuplicateQueryItems().isEmpty() )
        {
            error = new ErrorMessage( ErrorCode.E7202, params.getDuplicateQueryItems() );
        }
        else if ( params.hasValueDimension() && params.getDimensionalObjectItems().contains( params.getValue() ) )
        {
            error = new ErrorMessage( ErrorCode.E7203 );
        }
        else if ( params.hasAggregationType() && !(params.hasValueDimension() || params.isAggregateData()) )
        {
            error = new ErrorMessage( ErrorCode.E7204 );
        }
        else if ( !params.hasPeriods() && (params.getStartDate() == null || params.getEndDate() == null) )
        {
            error = new ErrorMessage( ErrorCode.E7205 );
        }
        else if ( params.getStartDate() != null && params.getEndDate() != null
            && params.getStartDate().after( params.getEndDate() ) )
        {
            error = new ErrorMessage( ErrorCode.E7206, getMediumDateString( params.getStartDate() ),
                getMediumDateString( params.getEndDate() ) );
        }
        else if ( params.getPage() != null && params.getPage() <= 0 )
        {
            error = new ErrorMessage( ErrorCode.E7207, params.getPage() );
        }
        else if ( params.getPageSize() != null && params.getPageSize() < 0 )
        {
            error = new ErrorMessage( ErrorCode.E7208, params.getPageSize() );
        }
        else if ( params.hasLimit() && getMaxLimit() > 0 && params.getLimit() > getMaxLimit() )
        {
            error = new ErrorMessage( ErrorCode.E7209, params.getLimit(), getMaxLimit() );
        }
        else if ( params.hasTimeField() && !params.timeFieldIsValid() )
        {
            error = new ErrorMessage( ErrorCode.E7210, params.getTimeField() );
        }
        else if ( params.hasOrgUnitField() && !params.orgUnitFieldIsValid() )
        {
            error = new ErrorMessage( ErrorCode.E7211, params.getOrgUnitField() );
        }
        else if ( params.hasClusterSize() && params.getClusterSize() <= 0 )
        {
            error = new ErrorMessage( ErrorCode.E7212, params.getClusterSize() );
        }
        else if ( params.hasBbox() && !ValidationUtils.bboxIsValid( params.getBbox() ) )
        {
            error = new ErrorMessage( ErrorCode.E7213, params.getBbox() );
        }
        else if ( (params.hasBbox() || params.hasClusterSize()) && params.getCoordinateField() == null )
        {
            error = new ErrorMessage( ErrorCode.E7214 );
        }
        else if ( params.getFallbackCoordinateField() != null && !params.fallbackCoordinateFieldIsValid() )
        {
            error = new ErrorMessage( ErrorCode.E7228, params.getFallbackCoordinateField() );
        }

        for ( QueryItem item : params.getItemsAndItemFilters() )
        {
            if ( item.hasLegendSet() && item.hasOptionSet() )
            {
                error = new ErrorMessage( ErrorCode.E7215, item.getItemId() );
            }
            else if ( params.isAggregateData() && !item.getAggregationType().isAggregatable() )
            {
                error = new ErrorMessage( ErrorCode.E7216, item.getItemId() );
            }

            for ( QueryFilter queryFilter : item.getFilters() )
            {
                if ( !queryFilter.getOperator().isNullAllowed() && queryFilter.getFilter().contains( NV ) )
                {
                    error = new ErrorMessage( ErrorCode.E7229, queryFilter.getOperator().getValue() );
                }
            }
        }

        // TODO validate coordinate field

        return error;
    }

    @Override
    public void validateTableLayout( EventQueryParams params, List<String> columns, List<String> rows )
    {
        ErrorMessage violation = null;

        if ( columns != null )
        {
            for ( String column : columns )
            {
                if ( !params.hasDimension( column ) )
                {
                    violation = new ErrorMessage( ErrorCode.E7126, column );
                }
            }
        }

        if ( rows != null )
        {
            for ( String row : rows )
            {
                if ( !params.hasDimension( row ) )
                {
                    violation = new ErrorMessage( ErrorCode.E7127, row );
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
        return systemSettingManager.getIntSetting( SettingKey.ANALYTICS_MAX_LIMIT );
    }
}
