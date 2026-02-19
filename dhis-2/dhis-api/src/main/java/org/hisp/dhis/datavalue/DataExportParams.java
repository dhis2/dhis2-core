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

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * Details to describe what slice of aggregate data to export.
 *
 * @author Lars Helge Overland (original)
 * @author Jan Bernitt (revised version for 2.43)
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

  public boolean isPeriodOverSpecified() {
    return notEmpty(periods) && startDate != null && endDate != null;
  }

  public boolean isDateRangeOutOfBounds() {
    return startDate != null && endDate != null && startDate.after(endDate);
  }

  public boolean isLimitOutOfBounds() {
    return limit != null && limit < 0;
  }

  public boolean isOrgUnitGroupsOverSpecified() {
    return notEmpty(organisationUnitGroups) && includeDescendants;
  }

  private static boolean notEmpty(Collection<?> c) {
    return c != null && !c.isEmpty();
  }

  /** The parameters adjusting the encoding process of the export. */
  public record EncodingParams(
      boolean unfoldOptionCombos, boolean excludeDefaultCoc, boolean excludeDefaultAoc) {}

  /**
   * All query parameters to read data value sets as provided by user input to create {@link
   * DataExportParams}.
   *
   * @author Jan Bernitt
   */
  @Getter
  @Setter
  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @OpenApi.Shared(name = "DataExportParams")
  public static class Input {

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
    private List<Order> order;

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

    private IdProperty idScheme;
    private IdProperty dataElementIdScheme;
    private IdProperty categoryOptionComboIdScheme;
    private IdProperty categoryOptionIdScheme;
    private IdProperty categoryIdScheme;
    private IdProperty orgUnitIdScheme;
    private IdProperty dataSetIdScheme;
    private IdProperty attributeOptionComboIdScheme;

    @OpenApi.Description(
        """
      When `true`, the category option combos and attribute option combos
      are exported as pairs of category and category option values using
      `categoryIdScheme` and `categoryOptionIdScheme`.
      """)
    @Maturity.Alpha
    private Boolean unfoldOptionCombos;

    @OpenApi.Description(
        "When `true`, data values with default category option combo skip the ID in the export output.")
    @Maturity.Beta
    private boolean excludeDefaultCoc;

    @OpenApi.Description(
        "When `true`, data values with default attribute option combo skip the ID in the export output.")
    @Maturity.Beta
    private boolean excludeDefaultAoc;

    @OpenApi.Ignore
    public DataExportGroup.Ids getOutputIdSchemes() {
      return DataExportGroup.Ids.of(
          idScheme,
          dataSetIdScheme,
          dataElementIdScheme,
          orgUnitIdScheme,
          categoryOptionComboIdScheme,
          attributeOptionComboIdScheme,
          categoryOptionIdScheme,
          categoryIdScheme);
    }

    public EncodingParams geEncodingParams() {
      return new EncodingParams(
          Boolean.TRUE.equals(unfoldOptionCombos), excludeDefaultCoc, excludeDefaultAoc);
    }

    @Nonnull
    public Compression getCompression() {
      return compression == null ? Compression.NONE : compression;
    }

    @OpenApi.Ignore
    public String getFilename() {
      String comp =
          switch (getCompression()) {
            case GZIP -> ".gz";
            case ZIP -> ".zip";
            default -> "";
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
}
