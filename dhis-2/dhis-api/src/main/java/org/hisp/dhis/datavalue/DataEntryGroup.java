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
import static org.hisp.dhis.commons.util.TextUtils.replace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.log.TimeExecution;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Service API data structure to enter multiple values for the same dataset. This set is either
 * specified explicitly using {@link #dataSet()} or inferred implicitly from the data elements. If
 * not all data elements belong to a single unique set this is an error that requires explicit
 * specification of the target dataset.
 *
 * @param dataSet explicit target dataset (leave null for auto-detect)
 * @param values the raw values
 */
public record DataEntryGroup(
    @TimeExecution.Include @CheckForNull UID dataSet,
    @TimeExecution.Include @Nonnull List<DataEntryValue> values) {

  public DataEntryGroup {
    requireNonNull(values);
  }

  public String describe() {
    // Needs to use HashMap because of null values
    Map<String, String> vars = new HashMap<>();
    vars.put("ds", dataSet == null ? null : dataSet.getValue());
    vars.put("count", "" + values.size());
    return replace("ds=${ds:?} (${count:0} values)", vars);
  }

  /**
   * How a {@link DataEntryGroup} is provided as user input in the web API.
   *
   * <p>Mainly differs from the {@link DataEntryGroup} by allowing other forms of identifiers to be
   * used instead of {@link UID}s.
   */
  @OpenApi.Shared(name = "DataEntryGroup")
  public record Input(
      @CheckForNull Ids ids,
      @CheckForNull @OpenApi.Property({UID.class, DataSet.class}) String dataSet,
      // common dimensions (optional)
      @CheckForNull @OpenApi.Property({UID.class, DataElement.class}) String dataElement,
      @CheckForNull @OpenApi.Property({UID.class, OrganisationUnit.class}) String orgUnit,
      @CheckForNull String period,
      @CheckForNull @OpenApi.Property({UID.class, CategoryOptionCombo.class})
          String attributeOptionCombo,
      @CheckForNull
          @OpenApi.Description(
              """
            Alternative to the `attributeOptionCombo` the defining which category option (value) is chosen for which category (key)
            for the category combo of the `dataSet`. Can only be used when `dataSet` is provided as well.
            Will only be considered if `attributeOptionCombo` is not present.
            """)
          Map<String, String> attributeOptions,
      @Nonnull List<DataEntryValue.Input> values) {

    public Input {
      requireNonNull(values);
    }

    public Input(String dataSet, List<DataEntryValue.Input> values) {
      this(null, dataSet, values);
    }

    public Input(List<DataEntryValue.Input> values) {
      this(null, null, values);
    }

    public Input(Ids ids, String dataSet, List<DataEntryValue.Input> values) {
      this(ids, dataSet, null, null, null, null, null, values);
    }

    public String describe() {
      // Needs to use HashMap because of null values
      Map<String, String> vars = new HashMap<>();
      vars.put("ds", dataSet);
      vars.put("de", dataElement);
      vars.put("ou", orgUnit);
      vars.put("pe", period);
      vars.put("aoc", attributeOptionCombo);
      vars.put("count", "" + values.size());
      return replace(
          "ds=${ds:?} [de=${de:} ou=${ou:} pe=${pe:} aoc=${aoc:}](${count:0} values)", vars);
    }

    public boolean isSameDsAoc(Input other) {
      return dataSet != null
          && dataSet.equals(other.dataSet)
          && Objects.equals(attributeOptionCombo, other.attributeOptionCombo)
          && Objects.equals(attributeOptions, other.attributeOptions);
    }

    public Input mergedSameDsAoc(Input other) {
      List<DataEntryValue.Input> merged = new ArrayList<>(values.size() + other.values.size());
      merged.addAll(values);
      merged.addAll(other.values);
      String de = dataElement;
      String ou = orgUnit;
      String pe = period;
      // note: tries to maintain the common group properties if there are some
      if (!Objects.equals(de, other.dataElement)) de = null;
      if (!Objects.equals(ou, other.orgUnit)) ou = null;
      if (!Objects.equals(pe, other.period)) pe = null;
      return new Input(ids, dataSet, de, ou, pe, attributeOptionCombo, attributeOptions, merged);
    }
  }

  /**
   * Options for the import. By default, all are {@code false}.
   *
   * @param dryRun essentially all the validation and preparation but without actually doing the
   *     upsert
   * @param atomic then true, any validation error (including value validation) aborts the entire
   *     import
   * @param force when true, any timeliness validation is skipped (only possible as superuser) to
   *     allow out-of-time (early/late) entry of data e.g. as part of a data synchronisation or
   *     repair
   */
  public record Options(boolean dryRun, boolean atomic, boolean force) {

    public Options() {
      this(false, false, false);
    }
  }

  /** The identifier properties that can be specified for a {@link DataEntryGroup.Input}. */
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

    public static Ids of(IdSchemes schemes) {
      return schemes == null
          ? null
          : new Ids(
              IdProperty.of(schemes.getDataSetIdScheme()),
              IdProperty.of(schemes.getDataElementIdScheme()),
              IdProperty.of(schemes.getOrgUnitIdScheme()),
              IdProperty.of(schemes.getCategoryOptionComboIdScheme()),
              IdProperty.of(schemes.getAttributeOptionComboIdScheme()),
              IdProperty.of(schemes.getCategoryOptionIdScheme()),
              IdProperty.of(schemes.getCategoryIdScheme()));
    }

    public Ids dataElements(IdProperty dataElements) {
      return new Ids(
          dataSets,
          dataElements,
          orgUnits,
          categoryOptionCombos,
          attributeOptionCombos,
          categoryOptions,
          categories);
    }

    public Ids orgUnits(IdProperty orgUnits) {
      return new Ids(
          dataSets,
          dataElements,
          orgUnits,
          categoryOptionCombos,
          attributeOptionCombos,
          categoryOptions,
          categories);
    }
  }
}
