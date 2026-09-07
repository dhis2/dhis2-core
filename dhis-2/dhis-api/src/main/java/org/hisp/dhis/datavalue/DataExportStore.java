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

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UsageTestOnly;

/**
 * Support for bulk data value export.
 *
 * @author Jan Bernitt
 */
public interface DataExportStore {

  /*
  Decode support
   */
  @CheckForNull
  UID getAttributeOptionCombo(
      @CheckForNull UID categoryCombo, @Nonnull Stream<UID> categoryOptions);

  /**
   * Resolves the default attribute option combo for an export that is scoped to data set(s), but
   * only when every given data set uses the default attribute category combo.
   *
   * <p>In that case the only valid attribute option combo is the default one, so it can be applied
   * as an explicit filter. This allows the export query to skip the (expensive) per-row attribute
   * option combo data-sharing check that would otherwise scan the entire {@code categoryoptioncombo}
   * table.
   *
   * @param dataSets the data sets that scope the export
   * @return the UID of the default {@link org.hisp.dhis.category.CategoryOptionCombo} when every
   *     given data set uses the default attribute {@link org.hisp.dhis.category.CategoryCombo};
   *     {@code null} when any given data set uses a non-default attribute category combo, or when
   *     none of the given data sets exist
   */
  @CheckForNull
  UID getDefaultAttributeOptionComboForDataSets(@Nonnull Stream<UID> dataSets);

  /*
  Export
   */

  @CheckForNull
  DataExportValue exportValue(@Nonnull DataValueKey key);

  /**
   * Returns data values for the given data export parameters.
   *
   * @param params the data export parameters.
   * @return a list of data values.
   */
  @Nonnull
  Stream<DataExportValue> exportValues(@Nonnull DataExportParams params);

  /*
  Validation support
   */

  /**
   * @param dataSets scope of the check
   * @return The UIDs of the given datasets that the current user does not have data read access to.
   *     Meaning, if the current user has access to all given datasets an empty list should be
   *     returned.
   */
  @Nonnull
  List<String> getDataSetsNoDataReadAccess(@Nonnull Stream<UID> dataSets);

  @Nonnull
  List<String> getAocNoDataReadAccess(@Nonnull Stream<UID> attributeOptionCombos);

  @Nonnull
  List<String> getOrgUnitsNotInUserHierarchy(@Nonnull Stream<UID> orgUnits);

  /*
  Test support
  */

  /**
   * Returns all DataValues.
   *
   * @return a list of all DataValues.
   */
  @UsageTestOnly
  List<DataExportValue> getAllDataValues();
}
