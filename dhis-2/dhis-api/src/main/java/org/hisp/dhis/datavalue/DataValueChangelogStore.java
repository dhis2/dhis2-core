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
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.UID;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
public interface DataValueChangelogStore extends GenericStore<DataValueChangelog> {

  /**
   * Deletes all data value audits for the given organisation unit.
   *
   * @param orgUnit the organisation unit.
   */
  void deleteByOrgUnit(@Nonnull UID orgUnit);

  /**
   * Deletes all data value audits for the given data element.
   *
   * @param dataElement the data element.
   */
  void deleteByDataElement(@Nonnull UID dataElement);

  /**
   * Deletes all data value audits for the given category option combo. Both properties:
   * categoryOptionCombo & attributeOptionCombo are checked for a match.
   *
   * @param categoryOptionCombo the categoryOptionCombo.
   */
  void deleteByOptionCombo(@Nonnull UID categoryOptionCombo);

  /**
   * Returns data value audits for the given query that the current user may read.
   *
   * @param params the {@link DataValueChangelogQueryParams}.
   * @return a list of {@link DataValueChangelog}; paged, one more than the page size when there is
   *     a next page
   */
  List<DataValueChangelog> getEntries(DataValueChangelogQueryParams params);

  /**
   * Gets all audit entries for a single value (all dimensions are fully specified) that the current
   * user may read. If COC and/or AOC are unspecified in the parameters the default is used.
   *
   * @param params the key to the value
   * @return the audit events for the value stored most recent to oldest
   */
  List<DataValueChangelogEntry> getEntries(@Nonnull DataValueQueryParams params);

  /**
   * Whether the current user may read the data of a single value (all dimensions are fully
   * specified). If COC and/or AOC are unspecified in the parameters the default is used.
   *
   * @param params the key to the value
   * @return true, if the current user may read the data of the value
   */
  boolean isReadable(@Nonnull DataValueQueryParams params);

  /**
   * Gets all audit entries for a single value (all dimensions are fully specified) that the current
   * user may read. If COC and/or AOC are unspecified in the parameters the default is used.
   *
   * @param key the key to the value
   * @return the audit events for the value stored most recent to oldest
   */
  List<DataValueChangelogEntry> getEntries(@Nonnull DataValueKey key);

  /**
   * Lookup of the data sets the current user may not read data of, decided like the data read check
   * applied by {@link #getEntries(DataValueChangelogQueryParams)}.
   *
   * @param dataSets the data sets to check
   * @return UIDs of the given data sets the current user may not read data of
   */
  List<String> getDataSetsNoDataReadAccess(@Nonnull Collection<UID> dataSets);

  /**
   * Counts data value audits for the given query that the current user may read.
   *
   * @param params the {@link DataValueChangelogQueryParams}.
   * @return a list of {@link DataValueChangelog}.
   */
  int countEntries(DataValueChangelogQueryParams params);

  /** Enables the log by creating a trigger. Can be called even if audit is already enabled. */
  void enableAudit();

  /** Disables the log by removing the trigger. Can be called even if audit is already disabled. */
  void disableAudit();
}
