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
import org.hisp.dhis.common.UID;
import org.hisp.dhis.log.TimeExecution;

public record DviUpsertRequest(
    @TimeExecution.Include @CheckForNull UID dataSet,
    // common dimensions (optional)
    @CheckForNull UID dataElement,
    @CheckForNull UID orgUnit,
    @CheckForNull String period,
    @CheckForNull UID attrOptionCombo,
    @TimeExecution.Include @JsonAlias("dataValues") List<DviValue> values) {

  public DviUpsertRequest(UID ds, List<DviValue> values) {
    this(ds, null, null, null, null, values);
  }

  public DviUpsertRequest(List<DviValue> values) {
    this(null, values);
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
   * @param group automatically find and group values by data set, when multiple data sets exist for
   *     a data element use the most recently created one
   */
  public record Options(boolean dryRun, boolean atomic, boolean force, boolean group) {

    public Options() {
      this(false, false, false, false);
    }
  }
}
