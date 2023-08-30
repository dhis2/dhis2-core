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

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * Defines the functionality for persisting DataValues.
 *
 * @author Torgeir Lorange Ostby
 */
public interface DataValueStore {
  String ID = DataValueStore.class.getName();

  /** Special {@see DeflatedDataValue} to signal "End of file" for queued DDVs. */
  public static final DeflatedDataValue END_OF_DDV_DATA = new DeflatedDataValue();

  /**
   * Timeout value for {@see DeflatedDataValue} queue, to prevent waiting forever if the other
   * thread has aborted.
   */
  public static final int DDV_QUEUE_TIMEOUT_VALUE = 10;

  /**
   * Timeout unit for {@see DeflatedDataValue} queue, to prevent waiting forever if the other thread
   * has aborted.
   */
  public static final TimeUnit DDV_QUEUE_TIMEOUT_UNIT = TimeUnit.MINUTES;

  // -------------------------------------------------------------------------
  // Basic DataValue
  // -------------------------------------------------------------------------

  /**
   * Adds a DataValue.
   *
   * @param dataValue the DataValue to add.
   */
  void addDataValue(DataValue dataValue);

  /**
   * Updates a DataValue.
   *
   * @param dataValue the DataValue to update.
   */
  void updateDataValue(DataValue dataValue);

  /**
   * Deletes all data values for the given organisation unit.
   *
   * @param organisationUnit the organisation unit.
   */
  void deleteDataValues(OrganisationUnit organisationUnit);

  /**
   * Deletes all data values for the given data element.
   *
   * @param dataElement the data element.
   */
  void deleteDataValues(DataElement dataElement);

  /**
   * Returns a DataValue.
   *
   * @param dataElement the DataElement of the DataValue.
   * @param period the Period of the DataValue.
   * @param source the Source of the DataValue.
   * @param categoryOptionCombo the category option combo.
   * @param attributeOptionCombo the attribute option combo.
   * @return the DataValue which corresponds to the given parameters, or null if no match.
   */
  DataValue getDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo);

  /**
   * Returns a DataValue.
   *
   * @param dataElement the DataElement of the DataValue.
   * @param period the Period of the DataValue.
   * @param source the Source of the DataValue.
   * @param categoryOptionCombo the category option combo.
   * @param attributeOptionCombo the attribute option combo.
   * @param includeDeleted Include deleted data values
   * @return the DataValue which corresponds to the given parameters, or null if no match.
   */
  DataValue getDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      boolean includeDeleted);

  /**
   * Returns a soft deleted DataValue.
   *
   * @param dataValue the DataValue to use as parameters.
   * @return the DataValue which corresponds to the given parameters, or null if no match.
   */
  DataValue getSoftDeletedDataValue(DataValue dataValue);

  // -------------------------------------------------------------------------
  // Collections of DataValues
  // -------------------------------------------------------------------------

  /**
   * Returns data values for the given data export parameters.
   *
   * @param params the data export parameters.
   * @return a list of data values.
   */
  List<DataValue> getDataValues(DataExportParams params);

  /**
   * Returns all DataValues.
   *
   * @return a list of all DataValues.
   */
  List<DataValue> getAllDataValues();

  /**
   * Returns deflated data values for the given data export parameters.
   *
   * @param params the data export parameters.
   * @return a list of deflated data values.
   */
  List<DeflatedDataValue> getDeflatedDataValues(DataExportParams params);

  /**
   * Gets the number of DataValues which have been updated between the given start and end date.
   * Either the start or end date can be null, but they cannot both be null.
   *
   * @param startDate the start date to compare against data value last updated.
   * @param endDate the end date to compare against data value last updated.
   * @param includeDeleted whether to include deleted data values.
   * @return the number of DataValues.
   */
  int getDataValueCountLastUpdatedBetween(Date startDate, Date endDate, boolean includeDeleted);

  /**
   * Checks if any data values exist for the provided {@link CategoryCombo}.
   *
   * @param combo the combo to check
   * @return true, if any value exist, otherwise false
   */
  boolean dataValueExists(CategoryCombo combo);
}
