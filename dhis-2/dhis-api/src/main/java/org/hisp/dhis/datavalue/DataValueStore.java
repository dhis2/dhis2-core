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

import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.common.DimensionalItemObject;
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
     * Deletes all data values for the given organisation unit.
     *
     * @param organisationUnit the organisation unit.
     */
    void deleteDataValues( OrganisationUnit organisationUnit );

    /**
     * Deletes all data values for the given data element.
     *
     * @param dataElement the data element.
     */
    void deleteDataValues( DataElement dataElement );
    
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
     * Returns a soft deleted DataValue.
     * 
     * @param dataValue the DataValue to use as parameters.
     * @return the DataValue which corresponds to the given parameters, or null
     *         if no match.
     */
    DataValue getSoftDeletedDataValue( DataValue dataValue );
    
    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    /**
     * Returns data values for the given data export parameters.
     * 
     * @param params the data export parameters.
     * @return a list of data values.
     */
    List<DataValue> getDataValues( DataExportParams params );
    
    /**
     * Returns all DataValues.
     * 
     * @return a list of all DataValues.
     */
    List<DataValue> getAllDataValues();
    
    /**
     * Returns all DataValues for a given Source, Period, collection of
     * DataElements and DataElementCategoryOptionCombo.
     * 
     * @param source the Source of the DataValues.
     * @param period the Period of the DataValues.
     * @param dataElements the DataElements of the DataValues.
     * @param attributeOptionCombo the DataElementCategoryCombo.
     * @return a list of all DataValues which match the given Source,
     *         Period, and any of the DataElements, or an empty collection if no
     *         values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, Period period, 
        Collection<DataElement> dataElements, DataElementCategoryOptionCombo attributeOptionCombo );
    
    /**
     * Returns values for a collection of DataElementOperands, where each operand
     * may include a specific CategoryOptionCombo, or may speicify a null COC if
     * all CategoryOptionCombos are to be summed.
     *
     * Returns values within the periods specified, for the organisation unit
     * specified or any of the organisation unit's descendants.
     *
     * Returns the values mapped by period, then attribute option combo UID,
     * then DimensionalItemObject (containing the DataElementOperand.)
     *
     * @param dataElementOperands the DataElementOperands.
     * @param periods the Periods of the DataValues.
     * @param orgUnit the root of the OrganisationUnit tree to include.
     * @return the map of values
     */
    MapMapMap<Period, String, DimensionalItemObject, Double> getDataElementOperandValues(
        Collection<DataElementOperand> dataElementOperands, Collection<Period> periods,
        OrganisationUnit orgUnit );

    /**
     * Gets the number of DataValues which have been updated between the given 
     * start and end date. The <pre>startDate</pre> and <pre>endDate</pre> parameters
     * can both be null but one must be defined.
     * 
     * @param startDate the start date to compare against data value last updated.
     * @param endDate the end date to compare against data value last updated.
     * @param includeDeleted whether to include deleted data values.
     * @return the number of DataValues.
     */
    int getDataValueCountLastUpdatedBetween( Date startDate, Date endDate, boolean includeDeleted );

    /**
     * Returns a map of values for each attribute option combo found.
     * <p>
     * In the (unlikely) event that the same dataElement/optionCombo is found in
     * more than one period for the same organisationUnit, date, and attribute
     * combo, the value is returned from the period with the shortest duration.
     *
     * @param dataElementOperandsToGet DataElementOperands to fetch
     * @param date date which must be present in the period
     * @param source OrganisationUnit for which to fetch the values
     * @param periodTypes allowable period types in which to find the data
     * @param attributeCombo the attribute combo to check (if restricted)
     * @param lastUpdatedMap map in which to return the lastUpdated date for each value
     * @return map of values by attribute option combo UID, then DataElementOperand
     */
    MapMap<String, DimensionalItemObject, Double> getDataValueMapByAttributeCombo(
        SetMap<String, DataElementOperand> dataElementOperandsToGet, Date date,
        OrganisationUnit source, Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
        Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints,
        MapMap<String, DataElementOperand, Date> lastUpdatedMap );

}
