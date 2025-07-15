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

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import javax.annotation.CheckForNull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.log.TimeExecution;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * The data of a data entry bulk operation in the service API.
 *
 * @param dataSet common dataset (if known)
 * @param dataElement common data element of all values (if common)
 * @param orgUnit common organisation unit of all values (if common)
 * @param period common period of all values (if common)
 * @param attrOptionCombo common AOC of all values (if common)
 * @param values the raw values
 */
public record DataEntryRequest(
    @TimeExecution.Include @CheckForNull UID dataSet,
    // common dimensions (optional)
    @CheckForNull UID dataElement,
    @CheckForNull UID orgUnit,
    @CheckForNull String period,
    @CheckForNull UID attrOptionCombo,
    @TimeExecution.Include @JsonAlias("dataValues") List<DataEntryValue> values) {

  public DataEntryRequest(UID ds, List<DataEntryValue> values) {
    this(ds, null, null, null, null, values);
  }

  public DataEntryRequest(List<DataEntryValue> values) {
    this(null, values);
  }

  /**
   * How a {@link DataEntryRequest} is provided as user input in the web API.
   *
   * <p>Mainly differs from the {@link DataEntryRequest} by allowing other forms of identifiers to
   * be used instead of {@link UID}s.
   */
  @OpenApi.Shared(name = "DataEntryRequest")
  public record Input(
      @OpenApi.Property({UID.class, DataSet.class}) @CheckForNull String dataSet,
      // common dimensions (optional)
      @OpenApi.Property({UID.class, DataElement.class}) @CheckForNull String dataElement,
      @OpenApi.Property({UID.class, OrganisationUnit.class}) @CheckForNull String orgUnit,
      @CheckForNull String period,
      @OpenApi.Property({UID.class, CategoryOptionCombo.class}) @CheckForNull
          String attrOptionCombo,
      // TODO add ID schemes
      @JsonAlias("dataValues") List<DataEntryValue.Input> values) {}

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
   * @param group automatically find and group values by data set, when multiple data sets exist for
   *     a data element use the most recently created one
   */
  public record Options(boolean dryRun, boolean atomic, boolean force, boolean group) {

    public Options() {
      this(false, false, false, false);
    }
  }
}
