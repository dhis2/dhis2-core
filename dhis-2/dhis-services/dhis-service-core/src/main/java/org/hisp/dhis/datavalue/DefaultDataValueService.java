package org.hisp.dhis.datavalue;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;

/**
 * Data value service implementation. Note that data values are softly deleted,
 * which implies having the deleted property set to true and updated.
 * 
 * @author Kristian Nordal
 * @author Halvdan Hoem Grelland
 */
@Transactional
public class DefaultDataValueService
    implements DataValueService
{
    private static final Log log = LogFactory.getLog( DefaultDataValueService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataValueStore dataValueStore;

    public void setDataValueStore( DataValueStore dataValueStore )
    {
        this.dataValueStore = dataValueStore;
    }

    private DataValueAuditService dataValueAuditService;

    public void setDataValueAuditService( DataValueAuditService dataValueAuditService )
    {
        this.dataValueAuditService = dataValueAuditService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private FileResourceService fileResourceService;

    public void setFileResourceService( FileResourceService fileResourceService )
    {
        this.fileResourceService = fileResourceService;
    }

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    @Override
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
            dataValue.setCategoryOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
        }

        if ( dataValue.getAttributeOptionCombo() == null )
        {
            dataValue.setAttributeOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
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
        if ( dataValue.isNullValue() || dataValueIsZeroAndInsignificant( dataValue.getValue(), dataValue.getDataElement() ) )
        {
            deleteDataValue( dataValue );
        }
        else if ( dataValueIsValid( dataValue.getValue(), dataValue.getDataElement() ) == null )
        {
            dataValue.setLastUpdated( new Date() );
            
            DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getAuditValue(), dataValue.getStoredBy(), AuditType.UPDATE );

            dataValueAuditService.addDataValueAudit( dataValueAudit );
            dataValueStore.updateDataValue( dataValue );
        }
    }

    @Override
    @Transactional
    public void deleteDataValue( DataValue dataValue )
    {
        DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getAuditValue(), currentUserService.getCurrentUsername(), AuditType.DELETE );

        dataValueAuditService.addDataValueAudit( dataValueAudit );

        if ( dataValue.getDataElement().isFileType() )
        {
            fileResourceService.deleteFileResource( dataValue.getValue() );
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
    public void deleteDataValues( DataElement dataElement )
    {
        dataValueStore.deleteDataValues( dataElement );
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        DataElementCategoryOptionCombo defaultOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        return dataValueStore.getDataValue( dataElement, period, source, categoryOptionCombo, defaultOptionCombo );
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        return dataValueStore.getDataValue( dataElement, period, source, categoryOptionCombo, attributeOptionCombo );
    }

    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Override
    public List<DataValue> getDataValues( DataExportParams params )
    {
        validate( params );
        
        return dataValueStore.getDataValues( params );
    }

    @Override
    public void validate( DataExportParams params )
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalArgumentException( "Params cannot be null" );
        }

        if ( params.getDataElements().isEmpty() && params.getDataSets().isEmpty() && params.getDataElementGroups().isEmpty() )
        {
            violation = "At least one valid data set or data element group must be specified";
        }

        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            violation = "Both periods and start/end date cannot be specified";
        }

        if ( params.hasStartEndDate() && params.getStartDate().after( params.getEndDate() ) )
        {
            violation = "Start date must be before end date";
        }

        if ( params.hasLastUpdatedDuration() && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getLastUpdatedDuration();
        }

        if ( params.isIncludeChildren() && params.hasOrganisationUnitGroups() )
        {
            violation = "Children cannot be included for organisation unit groups";
        }

        if ( params.isIncludeChildren() && !params.hasOrganisationUnits() )
        {
            violation = "At least one valid organisation unit must be specified when children is included";
        }

        if ( params.hasLimit() && params.getLimit() < 0 )
        {
            violation = "Limit cannot be less than zero: " + params.getLimit();
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public List<DataValue> getAllDataValues()
    {
        return dataValueStore.getAllDataValues();
    }
    
    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, Period period,
        Collection<DataElement> dataElements, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        return dataValueStore.getDataValues( source, period, dataElements, attributeOptionCombo );
    }

    @Override
    public MapMapMap<Period, String, DimensionalItemObject, Double> getDataElementOperandValues(
        Collection<DataElementOperand> dataElementOperands, Collection<Period> periods,
        OrganisationUnit orgUnit )
    {
        return dataValueStore.getDataElementOperandValues( dataElementOperands,
            periods, orgUnit );
    }

    @Override
    public int getDataValueCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return dataValueStore.getDataValueCountLastUpdatedBetween( cal.getTime(), null, false );
    }

    @Override
    public int getDataValueCountLastUpdatedAfter( Date date, boolean includeDeleted )
    {
        return dataValueStore.getDataValueCountLastUpdatedBetween( date, null, includeDeleted );
    }

    @Override
    public int getDataValueCountLastUpdatedBetween( Date startDate, Date endDate, boolean includeDeleted )
    {
        return dataValueStore.getDataValueCountLastUpdatedBetween( startDate, endDate, includeDeleted );
    }

    @Override
    public MapMap<String, DimensionalItemObject, Double> getDataValueMapByAttributeCombo(
        SetMap<String, DataElementOperand> dataElementOperandsToGet, Date date, OrganisationUnit source,
        Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
        Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints,
        MapMap<String, DataElementOperand, Date> lastUpdatedMap )
    {
        return dataValueStore.getDataValueMapByAttributeCombo( dataElementOperandsToGet, date, source,
            periodTypes, attributeCombo, cogDimensionConstraints, coDimensionConstraints, lastUpdatedMap );
    }
}
