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

import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Defines the functionality for persisting DataValues.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: DataValueStore.java 5715 2008-09-17 14:05:28Z larshelg $
 */
public interface DataValueStore
{
    String ID = DataValueStore.class.getName();

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    /**
     * Adds a DataValue.
     * 
     * @param dataValue the DataValue to add.
     */
    void addDataValue( DataValue dataValue );

    /**
     * Updates a DataValue.
     * 
     * @param dataValue the DataValue to update.
     */
    void updateDataValue( DataValue dataValue );

    /**
     * Deletes a DataValue.
     * 
     * @param dataValue the DataValue to delete.
     */
    void deleteDataValue( DataValue dataValue );
    
    /**
     * Deletes all data values for the given organisation unit.
     * 
     * @param organisationUnit the organisation unit.
     */
    void deleteDataValues( OrganisationUnit organisationUnit );
    
    /**
     * Returns a DataValue.
     * 
     * @param dataElement the DataElement of the DataValue.
     * @param period the Period of the DataValue.
     * @param source the Source of the DataValue.
     * @param categoryOptionCombo the category option combo.
     * @param attributeOptionCombo the attribute option combo.
     * @return the DataValue which corresponds to the given parameters, or null
     *         if no match.
     */
    DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source, 
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Returns a non-persisted DataValue.
     * 
     * @param dataElementId data element id
     * @param periodId period id
     * @param sourceId source id
     * @param categoryOptionComboId category option combo id
     * @param attributeOptionComboId attribute option combo id
     */
    DataValue getDataValue( int dataElementId, int periodId, int sourceId, int categoryOptionComboId, int attributeOptionComboId );
    
    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    /**
     * Returns all DataValues.
     * 
     * @return a collection of all DataValues.
     */
    List<DataValue> getAllDataValues();
    
    /**
     * Returns all DataValues for a given Source and Period.
     * 
     * @param source the Source of the DataValues.
     * @param period the Period of the DataValues.
     * @return a collection of all DataValues which match the given Source and
     *         Period, or an empty collection if no values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, Period period );
    
    /**
     * Returns all DataValues for a given Source and DataElement.
     * 
     * @param source the Source of the DataValues.
     * @param dataElement the DataElement of the DataValues.
     * @return a collection of all DataValues which match the given Source and
     *         DataElement, or an empty collection if no values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, DataElement dataElement );

    /**
     * Returns all DataValues for a given collection of Sources and a
     * DataElement.
     * 
     * @param sources the Sources of the DataValues.
     * @param dataElement the DataElement of the DataValues.
     * @return a collection of all DataValues which match any of the given
     *         Sources and the DataElement, or an empty collection if no values
     *         match.
     */
    List<DataValue> getDataValues( Collection<OrganisationUnit> sources, DataElement dataElement );

    /**
     * Returns all DataValues for a given Source, Period, and collection of
     * DataElements.
     * 
     * @param source the Source of the DataValues.
     * @param period the Period of the DataValues.
     * @param dataElements the DataElements of the DataValues.
     * @return a collection of all DataValues which match the given Source,
     *         Period, and any of the DataElements, or an empty collection if no
     *         values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, Period period, Collection<DataElement> dataElements );

    /**
     * Returns all DataValues for a given Source, Period, collection of
     * DataElements and DataElementCategoryOptionCombo.
     * 
     * @param source the Source of the DataValues.
     * @param period the Period of the DataValues.
     * @param dataElements the DataElements of the DataValues.
     * @param attributeOptionCombo the DataElementCategoryCombo.
     * @return a collection of all DataValues which match the given Source,
     *         Period, and any of the DataElements, or an empty collection if no
     *         values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, Period period, 
        Collection<DataElement> dataElements, DataElementCategoryOptionCombo attributeOptionCombo );
    
    /**
     * Returns all DataValues for a given Source, Period, collection of
     * DataElements and collection of optioncombos.
     * 
     * @param source the Source of the DataValues.
     * @param period the Period of the DataValues.
     * @param dataElements the DataElements of the DataValues.
     * @return a collection of all DataValues which match the given Source,
     *         Period, and any of the DataElements, or an empty collection if no
     *         values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, Period period, Collection<DataElement> dataElements, Collection<DataElementCategoryOptionCombo> categoryOptionCombos );
    
    /**
     * Returns all DataValues for a given DataElement, Period, and collection of 
     * Sources.
     * 
     * @param dataElement the DataElements of the DataValues.
     * @param period the Period of the DataValues.
     * @param sources the Sources of the DataValues.
     * @return a collection of all DataValues which match the given DataElement,
     *         Period, and Sources.
     */
    List<DataValue> getDataValues( DataElement dataElement, Period period, Collection<OrganisationUnit> sources );

    /**
     * Returns all DataValues for a given DataElement, collection of Periods, and
     * collection of Sources.
     *
     * @param dataElement the DataElements of the DataValues.
     * @param periods the Periods of the DataValues.
     * @param sources the Sources of the DataValues.
     * @return a collection of all DataValues which match the given DataElement,
     *         Periods, and Sources.
     */
    List<DataValue> getDataValues( DataElement dataElement, Collection<Period> periods,
        Collection<OrganisationUnit> sources );

    /**
     * Returns all DataValues for a given DataElement, DataElementCategoryOptionCombo,
     * collection of Periods, and collection of Sources.
     *
     * @param dataElement the DataElements of the DataValues.
     * @param categoryOptionCombo the DataElementCategoryOptionCombo of the DataValues.
     * @param periods the Periods of the DataValues.
     * @param sources the Sources of the DataValues.
     * @return a collection of all DataValues which match the given DataElement,
     *         Periods, and Sources.
     */
    List<DataValue> getDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources );

    /**
     * Returns all DataValues for a given collection of DataElementCategoryOptionCombos.
     * 
     * @param categoryOptionCombos the DataElementCategoryOptionCombos of the DataValue.
     * @return a collection of all DataValues which match the given collection of
     *         DataElementCategoryOptionCombos.
     */
    List<DataValue> getDataValues( Collection<DataElementCategoryOptionCombo> categoryOptionCombos );
    
    /**
     * Returns all DataValues for a given collection of DataElements.
     * 
     * @param dataElement the DataElements of the DataValue.
     * @return a collection of all DataValues which mach the given collection of DataElements.
     */
    List<DataValue> getDataValues( DataElement dataElement );  
    
    /**
     * Returns Latest DataValues for a given DataElement, PeriodType and OrganisationUnit
     * 
     * @param dataElement the DataElements of the DataValue.
     * @param periodType the Period Type of period of the DataValue
     * @param organisationUnit the Organisation Unit of the DataValue
     * @return a Latest DataValue 
     */   
    
    DataValue getLatestDataValues( DataElement dataElement, PeriodType periodType, OrganisationUnit organisationUnit );


    /**
     * Returns all DataValues for a given DataElement, DataElementCategoryOptionCombo,
     * collection of Periods, and collection of Sources. The values returned by this
     * function are not persisted and are typically fetched outside of the hibernation
     * layer. If categoryOptionCombo is null, all categoryOptionCombo values are returned.
     *
     * @param dataElement the DataElements of the DataValues.
     * @param categoryOptionCombo the DataElementCategoryOptionCombo of the DataValues.
     * @param periods the Periods of the DataValues.
     * @param sources the Sources of the DataValues.
     * @return a collection of all DataValues which match the given DataElement,
     *         Periods, and Sources.
     */
    List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources );

    /**
     * Gets a Collection of DeflatedDataValues.
     *
     * @param dataElementId the DataElement identifier.
     * @param periodId      the Period identifier.
     * @param sourceIds     the Collection of Source foreign key (Integer) identifiers.
     */
    Collection<DeflatedDataValue> getDeflatedDataValues( int dataElementId, int periodId, Collection<Integer> sourceIds );

    /**
     * Returns all DataValues for a given DataElement, DataElementCategoryOptionCombo,
     * collection of Periods, and collection of Sources.
     * This also returns DataValues for the children (in the orgunit hierarchy) of the
     * designated sources.
     * The values returned by this function are not persisted and are typically fetched
     * outside of the hibernation layer. If categoryOptionCombo is null, all categoryOptionCombo
     * values are returned.
     *
     * @param dataElement the DataElements of the DataValues.
     * @param categoryOptionCombo the DataElementCategoryOptionCombo of the DataValues.
     * @param periods the Periods of the DataValues.
     * @param source the root of the OrganisationUnit tree to include
     * @return a collection of all DataValues which match the given DataElement,
     *         Periods, and Sources.
     */
    List<DeflatedDataValue> sumRecursiveDeflatedDataValues(
        DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, OrganisationUnit source );

    /**
     * Gets the number of DataValues which have been updated after the given 
     * date time.
     * 
     * @param date the date time.
     * @return the number of DataValues.
     */
    int getDataValueCountLastUpdatedAfter( Date date );

    /**
     * Returns a map of values for each attribute option combo found.
     * <p>
     * In the (unlikely) event that the same dataElement/optionCombo is found in
     * more than one period for the same organisationUnit, date, and attribute
     * combo, the value is returned from the period with the shortest duration.
     * 
     * @param dataElements collection of DataElements to fetch for
     * @param date date which must be present in the period
     * @param source OrganisationUnit for which to fetch the values
     * @param periodTypes allowable period types in which to find the data
     * @param attributeCombo the attribute combo to check (if restricted)
     * @param lastUpdatedMap map in which to return the lastUpdated date for each value
     * @return map of values by attribute option combo id, then DataElementOperand
     */
    MapMap<Integer, DataElementOperand, Double> getDataValueMapByAttributeCombo( Collection<DataElement> dataElements, Date date,
        OrganisationUnit source, Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
        Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap );

}
