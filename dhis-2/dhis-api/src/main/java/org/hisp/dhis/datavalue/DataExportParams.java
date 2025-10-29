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

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * @author Lars Helge Overland
 */
@Value
@Builder(toBuilder = true)
@Accessors(chain = true)
@AllArgsConstructor
public class DataExportParams {

  public enum Order {
    DE,
    OU,
    PE,
    CREATED,
    AOC
  }

  /* what DEs to include */
  List<UID> dataSets;
  List<UID> dataElements;
  List<UID> dataElementGroups;

  /* what OUs to include */
  List<UID> organisationUnits;
  List<UID> organisationUnitGroups;
  Integer orgUnitLevel;
  boolean includeDescendants;

  /* what timeframe to include */
  List<Period> periods;
  Set<PeriodType> periodTypes;
  Date startDate;
  Date endDate;
  Date includedDate;

  List<UID> categoryOptionCombos;
  List<UID> attributeOptionCombos;

  boolean includeDeleted;
  List<Order> orders;

  Date lastUpdated;
  Duration lastUpdatedDuration;

  /* Paging */
  Integer limit;
  Integer offset;

  IdSchemes outputIdSchemes;

  public boolean hasDataElementFilters() {
    return notEmpty(dataSets) || notEmpty(dataElements) || notEmpty(dataElementGroups);
  }

  public boolean hasPeriodFilters() {
    return notEmpty(periods)
        || notEmpty(periodTypes)
        || (startDate != null && endDate != null)
        || includedDate != null;
  }

  public boolean hasOrgUnitFilters() {
    return notEmpty(organisationUnits) || notEmpty(organisationUnitGroups);
  }

  /**
   * @return true if the OU filters can only match units specified in {@link #organisationUnits}
   */
  public boolean isExactOrgUnitsFilter() {
    return notEmpty(organisationUnits)
        && !isIncludeDescendants()
        && (organisationUnitGroups == null || organisationUnitGroups.isEmpty());
  }

  private static boolean notEmpty(Collection<?> c) {
    return c != null && !c.isEmpty();
  }
}
