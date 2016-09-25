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
 * The DataValueService interface defines how to work with data values.
 * 
 * @author Kristian Nordal
 * @version $Id: DataValueService.java 5715 2008-09-17 14:05:28Z larshelg $
 */
public interface DataValueService
{
    String ID = DataValueService.class.getName();

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    /**
     * Adds a DataValue. If both the value and the comment properties of the
     * specified DataValue object are null, then the object should not be
     * persisted. The value will be validated and not be saved if not passing
     * validation.
     * 
     * @param dataValue the DataValue to add.
     * @return false whether the data value is null or invalid, true if value is
     *         valid and attempted to be saved.
     */
    boolean addDataValue( DataValue dataValue );

    /**
     * Updates a DataValue. If both the value and the comment properties of the
     * specified DataValue object are null, then the object should be deleted
     * from the underlying storage.
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
     * @param optionCombo the category option combo.
     * @return the DataValue which corresponds to the given parameters, or null
     *         if no match.
     */
    DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source, DataElementCategoryOptionCombo optionCombo );

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
    
    // -------------------------------------------------------------------------
    // Lists of DataValues
    // -------------------------------------------------------------------------

    /**
     * Returns all DataValues.
     * 
     * @return a collection of all DataValues.
     */
    List<DataValue> getAllDataValues();

    /**
     * Returns data values for the given arguments collections. Argument
     * collections might be empty, if so the argument is not applied to the
     * query. At least one argument collection must be non-empty.
     * 
     * @param dataElements the data elements.
     * @param periods the periods.
     * @param organisationUnits the organisation units.
     * @return a list of data values.
     */
    List<DataValue> getDataValues( Collection<DataElement> dataElements, 
        Collection<Period> periods, Collection<OrganisationUnit> organisationUnits );
    
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
    List<DataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources );

    /**
     * Returns all DataValues for a given DataElement, DataElementCategoryOptionCombo,
     * collection of Periods, and collection of Sources.
     * This also returns values for all of the children of the designated org units
     * The values returned by this
     * function are not persisted and are typically fetched outside of the hibernation
     * layer. If categoryOptionCombo is null, all categoryOptionCombo values are returned.
     *
     * @param dataElement         the DataElements of the DataValues.
     * @param categoryOptionCombo the DataElementCategoryOptionCombo of the DataValues.
     * @param periods             the Periods of the DataValues.
     * @param sources             the Sources of the DataValues.
     * @return a collection of all DataValues which match the given DataElement,
     * Periods, and Sources.
     */
    List<DataValue> getRecursiveDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Collection<OrganisationUnit> sources );

    /**
     * Gets the number of DataValues persisted since the given number of days.
     * 
     * @param days the number of days since now to include in the count.
     * @return the number of DataValues.
     */
    int getDataValueCount( int days );

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
