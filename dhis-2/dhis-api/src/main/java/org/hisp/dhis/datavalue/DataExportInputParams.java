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

import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.util.DateUtils.getSqlDateString;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;

/**
 * All query parameters to read data value sets.
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataExportInputParams {

  // file output details
  private Compression compression;

  @OpenApi.Description("Optional user defined result file name")
  private String attachment;

  @OpenApi.Description("`json` or `xml` or `csv` or `adx+xml` to define the file output format")
  private String format;

  private Set<String> dataSet;
  private Set<String> dataElement;
  private Set<String> dataElementGroup;
  private Set<String> period;
  private Set<String> orgUnit;
  private Set<String> orgUnitGroup;
  private Set<String> categoryOptionCombo;
  private Set<String> attributeOptionCombo;
  private UID attributeCombo;
  private Set<UID> attributeOptions;
  private Date startDate;
  private Date endDate;
  private boolean children;

  @OpenApi.Since(43)
  @Maturity.Beta
  @OpenApi.Description(
      "Can be used in addition to `orgUnit` and/or `orgUnitGroup` to narrow the scope to a specific level.")
  private Integer level;

  private boolean includeDeleted;
  private Date lastUpdated;
  private String lastUpdatedDuration;
  private Integer limit;
  private Integer offset;

  @OpenApi.Description(
      "If used it has the effect of adding `PE` to `order` as 1st (most significant) order property.")
  @Deprecated(since = "2.43")
  private boolean orderByPeriod;

  @OpenApi.Since(43)
  @Maturity.Beta
  private List<DataExportParams.Order> order;

  /*
   * Input IdSchemes
   */

  /**
   * @since 2.43
   */
  @OpenApi.Description(
      """
    When `true` and input IDs are expected to be `UID` due to being the default
    the decoding will attempt decoding as `CODE` for IDs that cannot be resolved as `UID`.""")
  private Boolean inputUseCodeFallback;

  private IdentifiableProperty inputIdScheme;
  private IdentifiableProperty inputOrgUnitIdScheme;
  private IdentifiableProperty inputDataElementIdScheme;
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
  private String trackedEntityAttributeIdScheme;
  private String dataSetIdScheme;
  private String attributeOptionComboIdScheme;

  @OpenApi.Description(
      """
    When `true`, the category option combos and attribute option combos
    are exported as pairs of category and category option values using
    `categoryIdScheme` and `categoryOptionIdScheme`.
    """)
  @Maturity.Alpha
  private Boolean unfoldOptionCombos;

  @OpenApi.Ignore
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
    setNonNull(
        schemes, trackedEntityAttributeIdScheme, IdSchemes::setTrackedEntityAttributeIdScheme);
    setNonNull(schemes, dataSetIdScheme, IdSchemes::setDataSetIdScheme);
    setNonNull(schemes, attributeOptionComboIdScheme, IdSchemes::setAttributeOptionComboIdScheme);
    return schemes;
  }

  private static void setNonNull(
      IdSchemes schemes, String property, BiFunction<IdSchemes, String, IdSchemes> setter) {
    if (property != null) {
      setter.apply(schemes, property);
    }
  }

  @Nonnull
  public Compression getCompression() {
    return compression == null ? Compression.NONE : compression;
  }

  @OpenApi.Ignore
  public String getFilename() {
    String comp =
        switch (getCompression()) {
          default -> "";
          case GZIP -> ".gz";
          case ZIP -> ".zip";
        };
    String fmt = format == null ? ".json" : "." + format.replace('+', '.');
    String name = attachment == null ? "dataValues" : attachment;
    if (name.endsWith(comp)) name = name.substring(0, name.length() - comp.length());
    if (name.endsWith(fmt)) name = name.substring(0, name.length() - fmt.length());
    if (startDate != null) name += "_" + getSqlDateString(startDate);
    if (endDate != null) name += "_" + getSqlDateString(endDate);
    return replace("${name}${fmt}${comp}", Map.of("name", name, "fmt", fmt, "comp", comp));
  }
}
