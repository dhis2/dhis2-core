package org.hisp.dhis.datavalue;

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


import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import java.util.Collection;
import java.util.Date;
import java.util.List;


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
     * valid and attempted to be saved.
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
     * Updates multiple DataValues. If both the value and the comment properties of the
     * specified DataValue object are null, then the object should be deleted
     * from the underlying storage.
     *
     * @param dataValues list of DataValues to update.
     */
    void updateDataValues( List<DataValue> dataValues );

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
     * Deletes all data values for the given data element.
     *
     * @param dataElement the data element.
     */
    void deleteDataValues( DataElement dataElement );

    /**
     * Returns a DataValue.
     *
     * @param dataElement the DataElement of the DataValue.
     * @param period      the Period of the DataValue.
     * @param source      the Source of the DataValue.
     * @param optionCombo the category option combo.
     * @return the DataValue which corresponds to the given parameters, or null
     * if no match.
     */
    DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo optionCombo );

    /**
     * Returns a DataValue.
     *
     * @param dataElement          the DataElement of the DataValue.
     * @param period               the Period of the DataValue.
     * @param source               the Source of the DataValue.
     * @param categoryOptionCombo  the category option combo.
     * @param attributeOptionCombo the attribute option combo.
     * @return the DataValue which corresponds to the given parameters, or null
     * if no match.
     */
    DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo );

    // -------------------------------------------------------------------------
    // Lists of DataValues
    // -------------------------------------------------------------------------

    /**
     * Returns data values for the given data export parameters.
     * <p>
     * Example usage:
     * <p>
     * <pre>
     * {@code
     * List<DataValue> dataValues = dataValueService.getDataValues( new DataExportParams()
     *     .setDataElements( dataElements )
     *     .setPeriods( Sets.newHashSet( period ) )
     *     .setOrganisationUnits( orgUnits ) );
     * }
     * </pre>
     *
     * @param params the data export parameters.
     * @return a list of data values.
     * @throws IllegalArgumentException if parameters are invalid.
     */
    List<DataValue> getDataValues( DataExportParams params );

    /**
     * Validates the given data export parameters.
     *
     * @param params the data export parameters.
     * @throws IllegalArgumentException if parameters are invalid.
     */
    void validate( DataExportParams params );

    /**
     * Returns all DataValues.
     *
     * @return a collection of all DataValues.
     */
    List<DataValue> getAllDataValues();

    /**
     * Returns all DataValues for a given Source, Period, collection of
     * DataElements and CategoryOptionCombo.
     *
     * @param source               the Source of the DataValues.
     * @param period               the Period of the DataValues.
     * @param dataElements         the DataElements of the DataValues.
     * @param attributeOptionCombo the CategoryCombo.
     * @return a collection of all DataValues which match the given Source,
     * Period, and any of the DataElements, or an empty collection if no
     * values match.
     */
    List<DataValue> getDataValues( OrganisationUnit source, Period period,
        Collection<DataElement> dataElements, CategoryOptionCombo attributeOptionCombo );

    /**
     * Returns deflated data values for the given data export parameters.
     *
     * @param params the data export parameters.
     * @return a list of deflated data values.
     */
    List<DeflatedDataValue> getDeflatedDataValues( DataExportParams params );

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
     * @param date           the date time.
     * @param includeDeleted whether to include deleted data values.
     * @return the number of DataValues.
     */
    int getDataValueCountLastUpdatedAfter( Date date, boolean includeDeleted );

    /**
     * Gets the number of DataValues which have been updated between the given
     * start and end date. The <pre>startDate</pre> and <pre>endDate</pre> parameters
     * can both be null but one must be defined.
     *
     * @param startDate      the start date to compare against data value last updated.
     * @param endDate        the end date to compare against data value last updated.
     * @param includeDeleted whether to include deleted data values.
     * @return the number of DataValues.
     */
    int getDataValueCountLastUpdatedBetween( Date startDate, Date endDate, boolean includeDeleted );
}
