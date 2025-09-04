/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * The DataValueService interface defines how to work with data values.
 *
 * @author Kristian Nordal
 */
public interface DataValueService {

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
   * Returns deflated data values for the given data export parameters.
   *
   * @param params the data export parameters.
   * @return a list of deflated data values.
   */
  List<DeflatedDataValue> getDeflatedDataValues(DataExportStoreParams params);

  /**
   * Gets the number of DataValues persisted since the given number of days.
   *
   * @param days the number of days since now to include in the count.
   * @return the number of DataValues.
   */
  int getDataValueCount(int days);

  /**
   * Gets the number of DataValues which have been updated after the given date time.
   *
   * @param date the date time.
   * @param includeDeleted whether to include deleted data values.
   * @return the number of DataValues.
   */
  int getDataValueCountLastUpdatedAfter(Date date, boolean includeDeleted);

  /**
   * Gets the number of DataValues which have been updated between the given start and end date. The
   * {@code startDate} and {@code endDate} parameters can both be null but one must be defined.
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

  /**
   * Checks if any data values exist for the provided {@link DataElement} {@link UID}.
   *
   * @param uid the {@link DataElement} {@link UID} to check
   * @return true, if any values exist, otherwise false
   */
  boolean dataValueExistsForDataElement(UID uid);

  /**
   * If a CategoryCombo is being updated, check whether it has any associated data, which may become
   * inaccessible if new CategoryOptionCombos are created.
   *
   * @param entity existing entity
   * @param newEntity new/updated entity
   */
  void checkNoDataValueBecomesInaccessible(CategoryCombo entity, CategoryCombo newEntity)
      throws ConflictException;
}
