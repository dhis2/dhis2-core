package org.hisp.dhis.datavalue;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
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
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<DataValue> getAllDataValues()
    {
        return dataValueStore.getAllDataValues();
    }
    
    @Override
    public List<DataValue> getDataValues( Collection<DataElement> dataElements, 
        Collection<Period> periods, Collection<OrganisationUnit> organisationUnits )
    {
        return dataValueStore.getDataValues( dataElements, periods, organisationUnits );
    }
    
    @Override
    public List<DataValue> getDataValues( OrganisationUnit source, Period period,
        Collection<DataElement> dataElements, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        return dataValueStore.getDataValues( source, period, dataElements, attributeOptionCombo );
    }

    @Override
    public List<DataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources )
    {
        List<DeflatedDataValue> dataValues = dataValueStore.getDeflatedDataValues( dataElement, categoryOptionCombo, periods, sources );
        List<DataValue> result = new ArrayList<DataValue>();

        Map<Integer, Period> periodIds = new HashMap<Integer, Period>();
        Map<Integer, OrganisationUnit> sourceIds = new HashMap<Integer, OrganisationUnit>();

        for ( Period period : periods )
        {
            periodIds.put( period.getId(), period );
        }

        for ( OrganisationUnit source : sources )
        {
            sourceIds.put( source.getId(), source );
        }

        for ( DeflatedDataValue ddv : dataValues )
        {
            DataValue dv = new DataValue( dataElement, periodIds.get( ddv.getPeriodId() ),
                sourceIds.get( ddv.getSourceId() ), getCategoryOptionCombo( ddv.getCategoryOptionComboId() ),
                getCategoryOptionCombo( ddv.getAttributeOptionComboId() ) );

            dv.setValue( ddv.getValue() );

            result.add( dv );
        }

        return result;
    }

    /**
     * Gets deflated (non-persisted) dataValues for a dataElement, period, and sources and
     * will get the values recursively for children of the sources if they don't exist for any
     * periods or attribute option combos.
     */
    @Override
    public List<DataValue> getRecursiveDeflatedDataValues(
        DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources )
    {
        List<DataValue> result = new ArrayList<DataValue>();
        DataElementCategoryOptionCombo dcoc = categoryService.getDefaultDataElementCategoryOptionCombo();
        DataElementCategoryOptionCombo coc = categoryOptionCombo == null || categoryOptionCombo == dcoc ? 
            null : categoryOptionCombo;

        Map<Integer, Period> periodIds = new HashMap<Integer, Period>();
        Map<Integer, OrganisationUnit> sourceIds = new HashMap<Integer, OrganisationUnit>();

        for ( Period period : periods )
        {
            periodIds.put( period.getId(), period );
        }

        for ( OrganisationUnit source : sources )
        {
            sourceIds.put( source.getId(), source );
        }

        for ( OrganisationUnit source: sources )
        {
            List<DeflatedDataValue> dataValues =
                dataValueStore.sumRecursiveDeflatedDataValues( dataElement, coc, periods, source );
            
            ListMap<Integer, Integer> period2aoc = new ListMap<Integer, Integer>();

            MapMap<Integer, Integer, DataValue> accumulatedValues = new MapMap<Integer, Integer, DataValue>();

            for ( DeflatedDataValue ddv : dataValues )
            {
                Integer periodId = ddv.getPeriodId();
                Integer aoc = ddv.getAttributeOptionComboId();
                
                if ( !( period2aoc.containsValue( periodId, aoc ) ) )
                {
                    DataValue dv = accumulatedValues.getValue( periodId, aoc );
                    
                    if ( dv == null )
                    {
                        dv = new DataValue( dataElement, periodIds.get( periodId ), 
                            source, dcoc, getCategoryOptionCombo( aoc ) );

                        dv.setValue( ddv.getValue() );
                        accumulatedValues.putEntry( periodId, aoc, dv );

                        result.add( dv );
                    }
                    else
                    {
                        Double value = Double.valueOf( ddv.getValue() );
                        Double cv = Double.valueOf( dv.getValue() );
                        Double nv = value + cv;
                        dv.setValue( nv.toString() );
                    }
                }
            }
        }

        return result;
    }

    private DataElementCategoryOptionCombo getCategoryOptionCombo( Integer id )
    {
        return categoryService.getDataElementCategoryOptionCombo( id );
    }

    @Override
    public int getDataValueCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return dataValueStore.getDataValueCountLastUpdatedAfter( cal.getTime() );
    }

    @Override
    public int getDataValueCountLastUpdatedAfter( Date date )
    {
        return dataValueStore.getDataValueCountLastUpdatedAfter( date );
    }

    @Override
    public MapMap<Integer, DataElementOperand, Double> getDataValueMapByAttributeCombo( Collection<DataElement> dataElements, Date date,
        OrganisationUnit source, Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
        Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap )
    {
        return dataValueStore.getDataValueMapByAttributeCombo( dataElements, date, source, periodTypes, attributeCombo,
            cogDimensionConstraints, coDimensionConstraints, lastUpdatedMap );
    }
}
