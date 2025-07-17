/*
 * Copyright (c) 2004-2025, University of Oslo
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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.scheduling.JobProgress;

/**
 * Service for entry of aggregate data.
 *
 * <p>The service generally operates on the scope of a {@link DataEntryGroup}. This most importantly
 * means the transaction scope is one group where all values target the same data-set.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
public interface DataEntryService {

  /**
   * Resolves any valid input ID to UIDs.
   *
   * @param group the group data as submitted by a user
   * @throws BadRequestException in case required IDs are missing, no object can be found for a
   *     provided ID or an ID being formally invalid. This should be due to a user input error.
   */
  DataEntryGroup decodeGroup(DataEntryGroup.Input group) throws BadRequestException;

  /**
   * Splits entered data values into groups, one group for each target data set. The data set is
   * selected based on the data element(s). In case of ambiguity the most recently created suitable
   * data set is targeted.
   *
   * @param mixed a group that might contain data that belongs to multiple data sets
   * @return multiple groups each having a target dataset assigned. in case multiple datasets are
   *     possible for a data element the most recently created data set is targeted
   * @throws ConflictException in case there are data elements that are not connected to any data
   *     set
   */
  List<DataEntryGroup> splitGroup(DataEntryGroup mixed) throws ConflictException;

  /**
   * Data entry of a single value. Support for data entry cell by cell as performed in the data
   * entry app.
   *
   * @param force true to skip timeliness validation as super-user
   * @param dataSet when not provided it must be unique for the key combination
   * @param value entered
   * @throws ConflictException in case of validation errors
   * @throws BadRequestException in case the submitted value is formally invalid
   */
  void upsertValue(boolean force, @CheckForNull UID dataSet, @Nonnull DataEntryValue value)
      throws ConflictException, BadRequestException;

  /**
   * Data entry of a single data value deletion. Support for data deletion (mark deleted) cell by
   * cell as performed in the data entry app.
   *
   * @param force true to skip timeliness validation as super-user
   * @param dataSet when not provided it must be unique for the key combination
   * @param key of the data value to soft delete
   * @return true, if a value got soft deleted, false when no such value did exist
   * @throws ConflictException in case of validation errors
   * @throws BadRequestException in case the submitted key is formally invalid
   */
  boolean deleteValue(boolean force, @CheckForNull UID dataSet, DataEntryKey key)
      throws ConflictException, BadRequestException;

  /**
   * Data entry of a group of values belonging to the same data set. If the group does not
   * explicitly define that target data set it is automatically resolved. If multiple datasets exist
   * as possible target for the values based on the data elements this is a conflict.
   *
   * @param options for the entry
   * @param request the data to enter
   * @param progress to track processing progress
   * @return a summary of the import
   * @throws ConflictException in case of validation errors, mainly these are: can the current user
   *     write to the target data set? is the metadata consistent with the references used for the
   *     data values? is the data reported in time? Are values formally correct and within their
   *     limits?
   */
  DataEntrySummary upsertGroup(
      DataEntryGroup.Options options, DataEntryGroup request, JobProgress progress)
      throws ConflictException;
}
