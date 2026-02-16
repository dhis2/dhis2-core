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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.period.Period;

/**
 * Un-encoded representation of a data value group (using UIDs).
 *
 * <p>This is the basis for encoding groups to {@link Output}s but also often used in operations
 * that do not require encoding.
 *
 * @param dataSet if all values belong to a dataset, otherwise null
 * @param period if all values belong to a common period, otherwise null
 * @param orgUnit if all values belong to a common org unit, otherwise null
 * @param attributeOptionCombo if all values belong to a common AOC, otherwise null
 * @param values the values in the group (as a stream to allow stream writing them directly to
 *     outputs without having to materialize the entire list in memory)
 * @implNote It is essential that the {@link #values()} are represented as {@link Stream} to allow
 *     convenient export to files without materializing the entire list in memory. However, this
 *     does mean a caller has to make sure that the stream is consumed within the transaction
 *     boundaries as this stream might be backed by a feed from DB.
 * @author Jan Bernitt
 * @since 2.43
 */
public record DataExportGroup(
    @CheckForNull UID dataSet,
    @CheckForNull Period period,
    @CheckForNull UID orgUnit,
    @CheckForNull UID attributeOptionCombo,
    @Nonnull Stream<DataExportValue> values) {

  /**
   * A group of values {@link DataExportValue.Output}. This is how data values are exposed in the
   * API. Users can ask to use the {@link IdProperty} they prefer to encode a groups {@link Ids}.
   *
   * <p>If any key-dimensions are given for the group these are common (the same) for all values.
   *
   * @param ids what type of ID is used for the fields in the group (common) or values (individual)
   * @param dataSet if all values belong to the same dataset this is its ID, otherwise null
   * @param period if all values belong to the same period, otherwise null
   * @param orgUnit if all values belong to the same org unit this is its ID, otherwise null
   * @param attributeOptionCombo if all values belong to the same AOC this is its ID, otherwise null
   * @param attributeOptions alternative to common {@link #attributeOptionCombo()} where the AOC is
   *     encoded as pairs of category and option
   */
  public record Output(
      @Nonnull Ids ids,
      @CheckForNull String dataSet,
      @CheckForNull String period,
      @CheckForNull String orgUnit,
      @CheckForNull String attributeOptionCombo,
      @CheckForNull Map<String, String> attributeOptions,
      @CheckForNull Scope deletion,
      @Nonnull Stream<DataExportValue.Output> values) {

    public Output {
      requireNonNull(ids);
      requireNonNull(values);
    }

    public Output withIds(@Nonnull Ids ids) {
      return new Output(
          ids, dataSet, period, orgUnit, attributeOptionCombo, attributeOptions, deletion, values);
    }

    public Output withDeletion(@CheckForNull Scope deletion) {
      return new Output(
          ids, dataSet, period, orgUnit, attributeOptionCombo, attributeOptions, deletion, values);
    }
  }

  public record Scope(
      @Nonnull List<String> orgUnits,
      @Nonnull List<String> periods,
      @Nonnull List<Element> elements) {

    public Scope {
      requireNonNull(orgUnits);
      requireNonNull(periods);
      requireNonNull(elements);
    }

    public record Element(
        @Nonnull String dataElement,
        @CheckForNull String categoryOptionCombo,
        @CheckForNull String attributeOptionCombo) {

      public Element {
        requireNonNull(dataElement);
      }
    }
  }

  /**
   * The identifier properties that can be specified for a {@link DataExportGroup.Output}.
   *
   * @implNote This by nature is symmetric to {@link DataEntryGroup.Ids} but their requirements
   *     might develop independently so these should not share code.
   */
  public record Ids(
      @Nonnull IdProperty dataSets,
      @Nonnull IdProperty dataElements,
      @Nonnull IdProperty orgUnits,
      @Nonnull IdProperty categoryOptionCombos,
      @Nonnull IdProperty attributeOptionCombos,
      @Nonnull IdProperty categoryOptions,
      @Nonnull IdProperty categories) {

    public Ids() {
      this(
          IdProperty.UID,
          IdProperty.UID,
          IdProperty.UID,
          IdProperty.UID,
          IdProperty.UID,
          IdProperty.UID,
          IdProperty.UID);
    }

    public static DataExportGroup.Ids of(IdSchemes schemes) {
      return schemes == null
          ? null
          : new DataExportGroup.Ids(
              IdProperty.of(schemes.getDataSetIdScheme()),
              IdProperty.of(schemes.getDataElementIdScheme()),
              IdProperty.of(schemes.getOrgUnitIdScheme()),
              IdProperty.of(schemes.getCategoryOptionComboIdScheme()),
              IdProperty.of(schemes.getAttributeOptionComboIdScheme()),
              IdProperty.of(schemes.getCategoryOptionIdScheme()),
              IdProperty.of(schemes.getCategoryIdScheme()));
    }
  }
}
