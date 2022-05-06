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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_AGGREGATE;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data value service implementation. Note that data values are softly deleted,
 * which implies having the deleted property set to true and updated.
 *
 * @author Kristian Nordal
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.datavalue.DataValueService" )
public class DefaultDataValueService
    implements DataValueService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final DataValueStore dataValueStore;

    private final DataValueAuditService dataValueAuditService;

    private final CurrentUserService currentUserService;

    private final CategoryService categoryService;

    private final DhisConfigurationProvider config;

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public boolean addDataValue( DataValue dataValue )
    {
        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        if ( dataValue == null || dataValue.isNullValue() )
        {
            log.info( "Data value is null" );
            return false;
        }

        String result = dataValueIsValid( dataValue.getValue(), dataValue.getDataElement() );

        if ( result != null )
        {
            log.info( "Data value is not valid: " + result );
            return false;
        }

        boolean zeroInsignificant = dataValueIsZeroAndInsignificant( dataValue.getValue(), dataValue.getDataElement() );

        if ( zeroInsignificant )
        {
            log.info( "Data value is zero and insignificant" );
            return false;
        }

        // ---------------------------------------------------------------------
        // Set default category option combo if null
        // ---------------------------------------------------------------------

        if ( dataValue.getCategoryOptionCombo() == null )
        {
            dataValue.setCategoryOptionCombo( categoryService.getDefaultCategoryOptionCombo() );
        }

        if ( dataValue.getAttributeOptionCombo() == null )
        {
            dataValue.setAttributeOptionCombo( categoryService.getDefaultCategoryOptionCombo() );
        }

        dataValue.setCreated( new Date() );
        dataValue.setLastUpdated( new Date() );

        // ---------------------------------------------------------------------
        // Check and restore soft deleted value
        // ---------------------------------------------------------------------

        DataValue softDelete = dataValueStore.getSoftDeletedDataValue( dataValue );

        if ( softDelete != null )
        {
            softDelete.mergeWith( dataValue );
            softDelete.setDeleted( false );

            dataValueStore.updateDataValue( softDelete );
        }
        else
        {
            dataValueStore.addDataValue( dataValue );
        }

        return true;
    }

    @Override
    @Transactional
    public void updateDataValue( DataValue dataValue )
    {
        if ( dataValue.isNullValue() ||
            dataValueIsZeroAndInsignificant( dataValue.getValue(), dataValue.getDataElement() ) )
        {
            deleteDataValue( dataValue );
        }
        else if ( dataValueIsValid( dataValue.getValue(), dataValue.getDataElement() ) == null )
        {
            dataValue.setLastUpdated( new Date() );

            DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getAuditValue(),
                dataValue.getStoredBy(), AuditType.UPDATE );

            if ( config.isEnabled( CHANGELOG_AGGREGATE ) )
            {
                dataValueAuditService.addDataValueAudit( dataValueAudit );
            }

            dataValueStore.updateDataValue( dataValue );
        }
    }

    @Override
    @Transactional
    public void updateDataValues( List<DataValue> dataValues )
    {
        if ( dataValues != null && !dataValues.isEmpty() )
        {
            for ( DataValue dataValue : dataValues )
            {
                updateDataValue( dataValue );
            }
        }
    }

    @Override
    @Transactional
    public void deleteDataValue( DataValue dataValue )
    {
        DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getAuditValue(),
            currentUserService.getCurrentUsername(), AuditType.DELETE );

        if ( config.isEnabled( CHANGELOG_AGGREGATE ) )
        {
            dataValueAuditService.addDataValueAudit( dataValueAudit );
        }

        dataValue.setLastUpdated( new Date() );
        dataValue.setDeleted( true );

        dataValueStore.updateDataValue( dataValue );
    }

    @Override
    @Transactional
    public void deleteDataValues( OrganisationUnit organisationUnit )
    {
        dataValueStore.deleteDataValues( organisationUnit );
    }

    @Override
    @Transactional
    public void deleteDataValues( DataElement dataElement )
    {
        dataValueStore.deleteDataValues( dataElement );
    }

    @Override
    @Transactional( readOnly = true )
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo )
    {
        CategoryOptionCombo defaultOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        return dataValueStore.getDataValue( dataElement, period, source, categoryOptionCombo, defaultOptionCombo );
    }

    @Override
    @Transactional( readOnly = true )
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo )
    {
        return dataValueStore.getDataValue( dataElement, period, source, categoryOptionCombo, attributeOptionCombo );
    }

    @Override
    @Transactional( readOnly = true )
    public DataValue getAndValidateDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo )
    {
        DataValue dataValue = dataValueStore.getDataValue(
            dataElement, period, source, categoryOptionCombo, attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new IllegalQueryException( ErrorCode.E2032 );
        }

        return dataValue;
    }

    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<DataValue> getDataValues( DataExportParams params )
    {
        validate( params );

        return dataValueStore.getDataValues( params );
    }

    @Override
    public void validate( DataExportParams params )
        throws IllegalQueryException
    {
        ErrorMessage error = null;

        if ( params == null )
        {
            throw new IllegalQueryException( ErrorCode.E2000 );
        }

        if ( !params.hasDataElements() && !params.hasDataSets() && !params.hasDataElementGroups() )
        {
            error = new ErrorMessage( ErrorCode.E2001 );
        }

        if ( !params.hasPeriods() && !params.hasStartEndDate() && !params.hasLastUpdated()
            && !params.hasLastUpdatedDuration() )
        {
            error = new ErrorMessage( ErrorCode.E2002 );
        }

        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            error = new ErrorMessage( ErrorCode.E2003 );
        }

        if ( params.hasStartEndDate() && params.getStartDate().after( params.getEndDate() ) )
        {
            error = new ErrorMessage( ErrorCode.E2004 );
        }

        if ( params.hasLastUpdatedDuration() && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            error = new ErrorMessage( ErrorCode.E2005 );
        }

        if ( !params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups() )
        {
            error = new ErrorMessage( ErrorCode.E2006 );
        }

        if ( params.isIncludeDescendants() && params.hasOrganisationUnitGroups() )
        {
            error = new ErrorMessage( ErrorCode.E2007 );
        }

        if ( params.isIncludeDescendants() && !params.hasOrganisationUnits() )
        {
            error = new ErrorMessage( ErrorCode.E2008 );
        }

        if ( params.hasLimit() && params.getLimit() < 0 )
        {
            error = new ErrorMessage( ErrorCode.E2009, params.getLimit() );
        }

        if ( error != null )
        {
            log.warn( "Validation failed: " + error );

            throw new IllegalQueryException( error );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValue> getAllDataValues()
    {
        return dataValueStore.getAllDataValues();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DeflatedDataValue> getDeflatedDataValues( DataExportParams params )
    {
        return dataValueStore.getDeflatedDataValues( params );
    }

    @Override
    @Transactional( readOnly = true )
    public int getDataValueCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return dataValueStore.getDataValueCountLastUpdatedBetween( cal.getTime(), null, false );
    }

    @Override
    @Transactional( readOnly = true )
    public int getDataValueCountLastUpdatedAfter( Date date, boolean includeDeleted )
    {
        return dataValueStore.getDataValueCountLastUpdatedBetween( date, null, includeDeleted );
    }

    @Override
    @Transactional( readOnly = true )
    public int getDataValueCountLastUpdatedBetween( Date startDate, Date endDate, boolean includeDeleted )
    {
        return dataValueStore.getDataValueCountLastUpdatedBetween( startDate, endDate, includeDeleted );
    }
}
