/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.datavalueset;

import java.util.Date;
import java.util.Set;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableProperty;

/**
 * All query parameters to read data value sets.
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataValueSetQueryParams {
  private Set<String> dataSet;

  private Set<String> dataElementGroup;

  private Set<String> period;

  private Date startDate;

  private Date endDate;

  private Set<String> orgUnit;

  private boolean children;

  private Set<String> orgUnitGroup;

  private Set<String> attributeOptionCombo;

  private boolean includeDeleted;

  private Date lastUpdated;

  private String lastUpdatedDuration;

  private Integer limit;

  /*
   * Input IdSchemes
   */

  private IdentifiableProperty inputIdScheme;

  private IdentifiableProperty inputOrgUnitIdScheme;

  private IdentifiableProperty inputDataSetIdScheme;

  private IdentifiableProperty inputDataElementGroupIdScheme;

  /*
   * Output IdSchemes (for backwards compatibility not named with prefix
   * output)
   */

  private String idScheme;

  private String dataElementIdScheme;

  private String categoryOptionComboIdScheme;

  private String categoryOptionIdScheme;

  private String categoryIdScheme;

  private String orgUnitIdScheme;

  private String programIdScheme;

  private String programStageIdScheme;

  private String trackedEntityIdScheme;

  private String trackedEntityAttributeIdScheme;

  private String dataSetIdScheme;

  private String attributeOptionComboIdScheme;

  private String programStageInstanceIdScheme;

  public IdSchemes getInputIdSchemes() {
    IdSchemes schemes = new IdSchemes();
    setNonNull(schemes, inputIdScheme, IdSchemes::setIdScheme);
    setNonNull(schemes, inputDataElementGroupIdScheme, IdSchemes::setDataElementGroupIdScheme);
    setNonNull(schemes, inputOrgUnitIdScheme, IdSchemes::setOrgUnitIdScheme);
    setNonNull(schemes, inputDataSetIdScheme, IdSchemes::setDataSetIdScheme);
    return schemes;
  }

  public IdSchemes getOutputIdSchemes() {
    IdSchemes schemes = new IdSchemes();
    setNonNull(schemes, idScheme, IdSchemes::setIdScheme);
    setNonNull(schemes, dataElementIdScheme, IdSchemes::setDataElementIdScheme);
    setNonNull(schemes, categoryOptionComboIdScheme, IdSchemes::setCategoryOptionComboIdScheme);
    setNonNull(schemes, categoryOptionIdScheme, IdSchemes::setCategoryOptionIdScheme);
    setNonNull(schemes, categoryIdScheme, IdSchemes::setCategoryIdScheme);
    setNonNull(schemes, orgUnitIdScheme, IdSchemes::setOrgUnitIdScheme);
    setNonNull(schemes, programIdScheme, IdSchemes::setProgramIdScheme);
    setNonNull(schemes, programStageIdScheme, IdSchemes::setProgramStageIdScheme);
    setNonNull(schemes, trackedEntityIdScheme, IdSchemes::setTrackedEntityIdScheme);
    setNonNull(
        schemes, trackedEntityAttributeIdScheme, IdSchemes::setTrackedEntityAttributeIdScheme);
    setNonNull(schemes, dataSetIdScheme, IdSchemes::setDataSetIdScheme);
    setNonNull(schemes, attributeOptionComboIdScheme, IdSchemes::setAttributeOptionComboIdScheme);
    setNonNull(schemes, programStageInstanceIdScheme, IdSchemes::setProgramStageInstanceIdScheme);
    return schemes;
  }

  private static void setNonNull(
      IdSchemes schemes,
      IdentifiableProperty property,
      BiFunction<IdSchemes, String, IdSchemes> setter) {
    if (property != null) {
      setNonNull(schemes, property.name(), setter);
    }
  }

  private static void setNonNull(
      IdSchemes schemes, String property, BiFunction<IdSchemes, String, IdSchemes> setter) {
    if (property != null) {
      setter.apply(schemes, property);
    }
  }
}
