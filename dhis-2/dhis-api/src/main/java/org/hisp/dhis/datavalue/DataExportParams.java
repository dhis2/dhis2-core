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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class DataExportParams {

  /* what DEs to include */
  private List<UID> dataSets;
  private List<UID> dataElements;
  private List<UID> dataElementGroups;

  /* what OUs to include */
  private List<UID> organisationUnits;
  private List<UID> organisationUnitGroups;
  private Integer orgUnitLevel;
  private boolean includeDescendants;

  /* what timeframe to include */
  private List<Period> periods;
  private Set<PeriodType> periodTypes;
  private Date startDate;
  private Date endDate;
  private Date includedDate;

  private List<UID> categoryOptionCombos = new ArrayList<>();
  private List<UID> attributeOptionCombos = new ArrayList<>();

  private boolean orderByOrgUnitPath;
  private boolean orderByPeriod;
  private boolean orderForSync;
  private boolean includeDeleted;

  private Date lastUpdated;
  private String lastUpdatedDuration;

  /* Paging */
  private Integer limit;
  private Integer offset;

  private IdSchemes outputIdSchemes;

  public boolean hasDataElements() {
    return dataElements != null && !dataElements.isEmpty();
  }

  public boolean hasDataSets() {
    return dataSets != null && !dataSets.isEmpty();
  }

  public boolean hasDataElementGroups() {
    return dataElementGroups != null && !dataElementGroups.isEmpty();
  }

  public boolean hasPeriods() {
    return periods != null && !periods.isEmpty();
  }

  public boolean hasStartEndDate() {
    return startDate != null && endDate != null;
  }

  public boolean hasOrganisationUnits() {
    return organisationUnits != null && !organisationUnits.isEmpty();
  }

  public boolean hasOrganisationUnitGroups() {
    return organisationUnitGroups != null && !organisationUnitGroups.isEmpty();
  }

  public boolean hasAttributeOptionCombos() {
    return attributeOptionCombos != null && !attributeOptionCombos.isEmpty();
  }

  public boolean hasLastUpdated() {
    return lastUpdated != null;
  }

  public boolean hasLastUpdatedDuration() {
    return lastUpdatedDuration != null;
  }

  public boolean hasLimit() {
    return limit != null;
  }


}
