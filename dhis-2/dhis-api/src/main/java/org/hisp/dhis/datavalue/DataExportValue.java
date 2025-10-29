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

import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;

/**
 * A flat data value using UIDs and ISO period values for the keys.
 *
 * <p>This is mostly how data values should be consumed when reading them to expose in an API or
 * export. Hence, all columns that are non-null in DB are also required in this record.
 *
 * @since 2.43
 */
public record DataExportValue(
    @Nonnull UID dataElement,
    @Nonnull String period,
    @Nonnull UID orgUnit,
    @Nonnull UID categoryOptionCombo,
    @Nonnull UID attributeOptionCombo,
    @Nonnull ValueType type,
    @CheckForNull String value,
    @CheckForNull String comment,
    @CheckForNull Boolean followUp,
    @CheckForNull String storedBy,
    @CheckForNull Date created,
    @CheckForNull Date lastUpdated,
    boolean deleted) {

  public DataExportValue {
    // enforce correct nullability by construction
    requireNonNull(dataElement);
    requireNonNull(period);
    requireNonNull(orgUnit);
    requireNonNull(categoryOptionCombo);
    requireNonNull(attributeOptionCombo);
    requireNonNull(type);
  }

  /**
   * Create records from DB rows. Assumes types based on DB column types and persistence mapping.
   *
   * @param row a row as returned by the DB
   * @return the row as {@link DataExportValue} record
   */
  public static DataExportValue of(Object[] row) {
    return new DataExportValue(
        UID.of((String) row[0]),
        (String) row[1],
        UID.of((String) row[2]),
        UID.of((String) row[3]),
        UID.of((String) row[4]),
        ValueType.valueOf((String) row[5]),
        (String) row[6],
        (String) row[7],
        (Boolean) row[8],
        (String) row[9],
        (Date) row[10],
        (Date) row[11],
        (Boolean) row[12]);
  }

  public boolean isFollowUp() {
    return followUp != null && followUp;
  }

  public DataEntryKey toKey() {
    return new DataEntryKey(
        dataElement, orgUnit, categoryOptionCombo, attributeOptionCombo, period);
  }

  /**
   * A data value as used during export where identifiers might have been substituted with other
   * identifiers than UIDs. The {@link #categoryOptionCombo} might also be given in terms of the
   * {@link #categoryOptions}. Key-dimensions might also be null in case they are common for a group
   * and therefore should not be written out again.
   */
  public record Output(
      @Nonnull String dataElement,
      @CheckForNull String period,
      @CheckForNull String orgUnit,
      @CheckForNull String categoryOptionCombo,
      @CheckForNull Map<String, String> categoryOptions,
      @CheckForNull String attributeOptionCombo,
      @Nonnull ValueType type,
      @CheckForNull String value,
      @CheckForNull String comment,
      @CheckForNull Boolean followUp,
      @CheckForNull String storedBy,
      @CheckForNull Date created,
      @CheckForNull Date lastUpdated,
      boolean deleted) {

    public Output {
      requireNonNull(dataElement);
      requireNonNull(type);
      if (categoryOptionCombo == null && categoryOptions == null)
        requireNonNull(categoryOptionCombo); // fail - one is required
    }
  }
}
