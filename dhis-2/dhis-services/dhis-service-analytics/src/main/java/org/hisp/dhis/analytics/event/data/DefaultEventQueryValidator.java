/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.List;

<<<<<<< HEAD
=======
import lombok.extern.slf4j.Slf4j;

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.stereotype.Component;

<<<<<<< HEAD
import lombok.extern.slf4j.Slf4j;

=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
@Slf4j
@Component( "org.hisp.dhis.analytics.event.EventQueryValidator" )
public class DefaultEventQueryValidator
    implements EventQueryValidator
{
    private final QueryValidator queryValidator;

    private final SystemSettingManager systemSettingManager;

    public DefaultEventQueryValidator( QueryValidator queryValidator, SystemSettingManager systemSettingManager )
    {
        checkNotNull( queryValidator );
        checkNotNull( systemSettingManager );

        this.queryValidator = queryValidator;
        this.systemSettingManager = systemSettingManager;
    }

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
<<<<<<< HEAD
            log.warn( String.format( "Event analytics validation failed, code: '%s', message: '%s'", error.getErrorCode(), error.getMessage() ) );
=======
            log.warn( String.format( "Event analytics validation failed, code: '%s', message: '%s'",
                error.getErrorCode(), error.getMessage() ) );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

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

        if ( !params.hasOrganisationUnits() )
        {
            error = new ErrorMessage( ErrorCode.E7200 );
        }

        if ( !params.getDuplicateDimensions().isEmpty() )
        {
            error = new ErrorMessage( ErrorCode.E7201, params.getDuplicateDimensions() );
        }

        if ( !params.getDuplicateQueryItems().isEmpty() )
        {
            error = new ErrorMessage( ErrorCode.E7202, params.getDuplicateQueryItems() );
        }

        if ( params.hasValueDimension() && params.getDimensionalObjectItems().contains( params.getValue() ) )
        {
            error = new ErrorMessage( ErrorCode.E7203 );
        }

        if ( params.hasAggregationType() && !(params.hasValueDimension() || params.isAggregateData()) )
        {
            error = new ErrorMessage( ErrorCode.E7204 );
        }

        if ( !params.hasPeriods() && (params.getStartDate() == null || params.getEndDate() == null) )
        {
            error = new ErrorMessage( ErrorCode.E7205 );
        }

        if ( params.getStartDate() != null && params.getEndDate() != null
            && params.getStartDate().after( params.getEndDate() ) )
        {
<<<<<<< HEAD
            error = new ErrorMessage( ErrorCode.E7206, getMediumDateString( params.getStartDate() ), getMediumDateString( params.getEndDate() ) );
=======
            error = new ErrorMessage( ErrorCode.E7206, getMediumDateString( params.getStartDate() ),
                getMediumDateString( params.getEndDate() ) );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        }

        if ( params.getPage() != null && params.getPage() <= 0 )
        {
            error = new ErrorMessage( ErrorCode.E7207, params.getPage() );
        }

        if ( params.getPageSize() != null && params.getPageSize() < 0 )
        {
            error = new ErrorMessage( ErrorCode.E7208, params.getPageSize() );
        }

        if ( params.hasLimit() && getMaxLimit() > 0 && params.getLimit() > getMaxLimit() )
        {
            error = new ErrorMessage( ErrorCode.E7209, params.getLimit(), getMaxLimit() );
        }

        if ( params.hasTimeField() && !params.timeFieldIsValid() )
        {
            error = new ErrorMessage( ErrorCode.E7210, params.getTimeField() );
        }

        if ( params.hasOrgUnitField() && !params.orgUnitFieldIsValid() )
        {
            error = new ErrorMessage( ErrorCode.E7211, params.getOrgUnitField() );
        }

        if ( params.hasClusterSize() && params.getClusterSize() <= 0 )
        {
            error = new ErrorMessage( ErrorCode.E7212, params.getClusterSize() );
        }

        if ( params.hasBbox() && !ValidationUtils.bboxIsValid( params.getBbox() ) )
        {
            error = new ErrorMessage( ErrorCode.E7213, params.getBbox() );
        }

        // TODO validate coordinate field

<<<<<<< HEAD
        if ( ( params.hasBbox() || params.hasClusterSize() ) && params.getCoordinateField() == null )
=======
        if ( (params.hasBbox() || params.hasClusterSize()) && params.getCoordinateField() == null )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        {
<<<<<<< HEAD
            error = new ErrorMessage( ErrorCode.E7214 );;
=======
            error = new ErrorMessage( ErrorCode.E7214 );
            ;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        }

        for ( QueryItem item : params.getItemsAndItemFilters() )
        {
            if ( item.hasLegendSet() && item.hasOptionSet() )
            {
                error = new ErrorMessage( ErrorCode.E7215, item.getItemId() );
            }

            if ( params.isAggregateData() && !item.getAggregationType().isAggregateable() )
            {
                error = new ErrorMessage( ErrorCode.E7216, item.getItemId() );
            }
        }

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
        return (Integer) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAX_LIMIT );
    }
}
