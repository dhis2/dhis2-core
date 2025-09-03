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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Defines the functionality for persisting DataValues.
 *
 * @author Torgeir Lorange Ostby
 */
public interface DataValueStore {

  /** Special {@see DeflatedDataValue} to signal "End of file" for queued DDVs. */
  DeflatedDataValue END_OF_DDV_DATA = new DeflatedDataValue();

  /**
   * Timeout value for {@see DeflatedDataValue} queue, to prevent waiting forever if the other
   * thread has aborted.
   */
  int DDV_QUEUE_TIMEOUT_VALUE = 10;

  /**
   * Timeout unit for {@see DeflatedDataValue} queue, to prevent waiting forever if the other thread
   * has aborted.
   */
  TimeUnit DDV_QUEUE_TIMEOUT_UNIT = TimeUnit.MINUTES;

  // -------------------------------------------------------------------------
  // Basic DataValue
  // -------------------------------------------------------------------------

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
   * Deletes all data values for the given data element.
   *
   * @param dataElement the dataElement.
   */
  void deleteDataValues(@Nonnull Collection<DataElement> dataElement);

  /**
   * Deletes all data values for the given category option combos.
   *
   * @param categoryOptionCombos the categoryOptionCombos.
   */
  void deleteDataValuesByCategoryOptionCombo(
      @Nonnull Collection<CategoryOptionCombo> categoryOptionCombos);

  /**
   * Deletes all data values for the given attribute option combos.
   *
   * @param attributeOptionCombos the attributeOptionCombos.
   */
  void deleteDataValuesByAttributeOptionCombo(
      @Nonnull Collection<CategoryOptionCombo> attributeOptionCombos);

  /**
   * Returns deflated data values for the given data export parameters.
   *
   * @param params the data export parameters.
   * @return a list of deflated data values.
   */
  List<DeflatedDataValue> getDeflatedDataValues(DataExportStoreParams params);

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

  /**
   * Checks if any data values exist for the provided {@link DataElement} {@link UID}.
   *
   * @param uid the {@link DataElement} {@link UID} to check
   * @return true, if any values exist, otherwise false
   */
  boolean dataValueExistsForDataElement(String uid);

  /**
   * SQL for handling merging {@link DataValue}s. There may be multiple potential {@link DataValue}
   * duplicates. Duplicate {@link DataValue}s with the latest {@link DataValue#getLastUpdated()}
   * values are kept, the rest are deleted. Only one of these entries can exist due to the composite
   * key constraint. <br>
   * The 3 execution paths are:
   *
   * <p>1. If the source {@link DataValue} is not a duplicate, it simply gets its {@link
   * DataValue#getCategoryOptionCombo()} updated to that of the target.
   *
   * <p>2. If the source {@link DataValue} is a duplicate and has an earlier {@link
   * DataValue#getLastUpdated()} value, it is deleted.
   *
   * <p>3. If the source {@link DataValue} is a duplicate and has a later {@link
   * DataValue#getLastUpdated()} value, the target {@link DataValue} is deleted. The source is kept
   * and has its {@link DataValue#getCategoryOptionCombo()} updated to that of the target.
   *
   * @param target target {@link CategoryOptionCombo}
   * @param sources source {@link CategoryOptionCombo}s
   */
  void mergeDataValuesWithCategoryOptionCombos(long target, @Nonnull Set<Long> sources);

  /**
   * SQL for handling merging {@link DataValue}s. There may be multiple potential {@link DataValue}
   * duplicates. Duplicate {@link DataValue}s with the latest {@link DataValue#getLastUpdated()}
   * values are kept, the rest are deleted. Only one of these entries can exist due to the composite
   * key constraint. <br>
   * The 3 execution paths are:
   *
   * <p>1. If the source {@link DataValue} is not a duplicate, it simply gets its {@link
   * DataValue#getAttributeOptionCombo()} updated to that of the target.
   *
   * <p>2. If the source {@link DataValue} is a duplicate and has an earlier {@link
   * DataValue#getLastUpdated()} value, it is deleted.
   *
   * <p>3. If the source {@link DataValue} is a duplicate and has a later {@link
   * DataValue#getLastUpdated()} value, the target {@link DataValue} is deleted. The source is kept
   * and has its {@link DataValue#getAttributeOptionCombo()} updated to that of the target.
   *
   * @param target target {@link CategoryOptionCombo} id
   * @param sources source {@link CategoryOptionCombo} ids
   */
  void mergeDataValuesWithAttributeOptionCombos(long target, @Nonnull Set<Long> sources);

  /**
   * SQL for handling merging {@link DataValue}s. There may be multiple potential {@link DataValue}
   * duplicates. Duplicate {@link DataValue}s with the latest {@link DataValue#getLastUpdated()}
   * values are kept, the rest are deleted. Only one of these entries can exist due to the composite
   * key constraint. <br>
   * The 3 execution paths are:
   *
   * <p>1. If the source {@link DataValue} is not a duplicate, it simply gets its {@link
   * DataValue#getDataElement()} updated to that of the target.
   *
   * <p>2. If the source {@link DataValue} is a duplicate and has an earlier {@link
   * DataValue#getLastUpdated()} value, it is deleted.
   *
   * <p>3. If the source {@link DataValue} is a duplicate and has a later {@link
   * DataValue#getLastUpdated()} value, the target {@link DataValue} is deleted. The source is kept
   * and has its {@link DataValue#getDataElement()} updated to that of the target.
   *
   * @param target target {@link DataElement} id
   * @param sources source {@link DataElement} ids
   */
  void mergeDataValuesWithDataElements(long target, @Nonnull Set<Long> sources);
}
