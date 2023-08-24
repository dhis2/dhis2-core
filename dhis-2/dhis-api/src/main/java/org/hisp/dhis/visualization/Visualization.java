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
package org.hisp.dhis.visualization;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static com.google.common.base.Verify.verify;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PRETTY_NAMES;
import static org.hisp.dhis.common.DimensionalObjectUtils.NAME_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getSortedKeysMap;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.visualization.CompatibilityGuard.keepAxesReadingCompatibility;
import static org.hisp.dhis.visualization.CompatibilityGuard.keepLegendReadingCompatibility;
import static org.hisp.dhis.visualization.DimensionDescriptor.getDimensionIdentifierFor;
import static org.hisp.dhis.visualization.VisualizationType.PIVOT_TABLE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hisp.dhis.analytics.NumberType;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.FontSize;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Icon.IconType;
import org.springframework.util.Assert;

@JacksonXmlRootElement(localName = "visualization", namespace = DXF_2_0)
public class Visualization extends BaseAnalyticalObject implements MetadataObject {

  public static final String REPORTING_MONTH_COLUMN_NAME = "reporting_month_name";

  public static final String PARAM_ORGANISATIONUNIT_COLUMN_NAME = "param_organisationunit_name";

  public static final String ORGANISATION_UNIT_IS_PARENT_COLUMN_NAME =
      "organisation_unit_is_parent";

  public static final String SPACE = " ";

  public static final String TOTAL_COLUMN_NAME = "total";

  public static final String TOTAL_COLUMN_PRETTY_NAME = "Total";

  public static final String EMPTY = "";

  /** Sorting constant that represents "no" sorting. */
  private static final int NONE = 0;

  private static final String ILLEGAL_FILENAME_CHARS_REGEX = "[/\\?%*:|\"'<>.]";

  public static final Map<String, String> COLUMN_NAMES =
      Map.of(
          DATA_X_DIM_ID, "data",
          CATEGORYOPTIONCOMBO_DIM_ID, "categoryoptioncombo",
          PERIOD_DIM_ID, "period",
          ORGUNIT_DIM_ID, "organisationunit");

  // -------------------------------------------------------------------------
  // Common attributes
  // -------------------------------------------------------------------------

  /** The type of this visualization object. */
  private VisualizationType type;

  /** The object responsible to hold parameters related to reporting. */
  private ReportingParams reportingParams;

  /** Indicates the criteria to apply to data measures. */
  private String measureCriteria;

  /** Dimensions to cross tabulate / use as columns. */
  private List<String> columnDimensions = new ArrayList<>();

  /** Dimensions to use as rows. */
  private List<String> rowDimensions = new ArrayList<>();

  /** The number type. */
  private NumberType numberType;

  /**
   * List of {@link Series}. Refers to the dimension items in the first dimension of the "columns"
   * list by dimension item identifier.
   */
  private List<Series> series = new ArrayList<>();

  /** Outlier analysis settings. */
  private OutlierAnalysis outlierAnalysis;

  // -------------------------------------------------------------------------
  // Display definitions
  // -------------------------------------------------------------------------

  /** The list of optional axes for this visualization. */
  private List<Axis> optionalAxes = new ArrayList<>();

  /** The font style for various components of the visualization. */
  private VisualizationFontStyle fontStyle;

  /** The key of the color set to use for visualization items, like columns and bars. */
  private String colorSet;

  /**
   * The collection of {@link Icon} objects associated. Should be unique for each {@link IconType}.
   */
  private Set<Icon> icons = new HashSet<>();

  // -------------------------------------------------------------------------
  // Display items for graphics/charts
  // -------------------------------------------------------------------------

  private SeriesKey seriesKey;

  private List<AxisV2> axes = new ArrayList<>();

  /**
   * The period of years of this visualization. See RelativePeriodEnum for a valid list of enum
   * based strings.
   */
  private List<String> yearlySeries = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Flags
  // -------------------------------------------------------------------------

  /**
   * Indicates whether the visualization contains regression columns. More likely to be applicable
   * to pivot and reports.
   */
  private boolean regression;

  /** Indicates whether to hide columns with no data values. */
  private boolean hideEmptyColumns;

  /** Fixes (or not) the pivot table column headers. */
  private boolean fixColumnHeaders;

  /** Fixes (or not) the pivot table row headers. */
  private boolean fixRowHeaders;

  // -------------------------------------------------------------------------
  // Non-persisted attributes, used for internal operation/rendering phase
  // -------------------------------------------------------------------------

  private transient List<Period> relativePeriodsList = new ArrayList<>();

  /** The name of the visualization (monthly based). */
  private transient String visualizationPeriodName;

  /** The title for a possible tabulated data. */
  private transient String gridTitle;

  /*
   * Collections mostly used for analytics tabulated data, like pivots or
   * reports.
   */
  private transient List<List<DimensionalItemObject>> gridColumns = new ArrayList<>();

  private transient List<List<DimensionalItemObject>> gridRows = new ArrayList<>();

  private transient List<DimensionDescriptor> dimensionDescriptors = new ArrayList<>();

  public Visualization() {}

  public Visualization(String name) {
    this();
    this.name = name;
  }

  /**
   * Default constructor.
   *
   * @param name the name.
   * @param dataElements the data elements.
   * @param indicators the indicators.
   * @param reportingRates the reporting rates.
   * @param periods the periods. Cannot have the name property set.
   * @param organisationUnits the organisation units.
   * @param doIndicators indicating whether indicators should be cross-tabulated.
   * @param doPeriods indicating whether periods should be cross-tabulated.
   * @param doUnits indicating whether organisation units should be cross-tabulated.
   */
  public Visualization(
      String name,
      List<DataElement> dataElements,
      List<Indicator> indicators,
      List<ReportingRate> reportingRates,
      List<Period> periods,
      List<OrganisationUnit> organisationUnits,
      boolean doIndicators,
      boolean doPeriods,
      boolean doUnits) {
    this.name = name;
    addAllDataDimensionItems(dataElements);
    addAllDataDimensionItems(indicators);
    addAllDataDimensionItems(reportingRates);
    this.periods = periods;
    this.organisationUnits = organisationUnits;

    if (doIndicators) {
      columnDimensions.add(DATA_X_DIM_ID);
    } else {
      rowDimensions.add(DATA_X_DIM_ID);
    }

    if (doPeriods) {
      columnDimensions.add(PERIOD_DIM_ID);
    } else {
      rowDimensions.add(PERIOD_DIM_ID);
    }

    if (doUnits) {
      columnDimensions.add(ORGUNIT_DIM_ID);
    } else {
      rowDimensions.add(ORGUNIT_DIM_ID);
    }
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "columnDimensions", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "columnDimension", namespace = DXF_2_0)
  public List<String> getColumnDimensions() {
    return columnDimensions;
  }

  public void setColumnDimensions(List<String> columnDimensions) {
    this.columnDimensions = columnDimensions;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "rowDimensions", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "rowDimension", namespace = DXF_2_0)
  public List<String> getRowDimensions() {
    return rowDimensions;
  }

  public void setRowDimensions(List<String> rowDimensions) {
    this.rowDimensions = rowDimensions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ReportingParams getReportingParams() {
    return reportingParams;
  }

  public void setReportingParams(ReportingParams reportingParams) {
    this.reportingParams = reportingParams;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getMeasureCriteria() {
    return measureCriteria;
  }

  public void setMeasureCriteria(String measureCriteria) {
    this.measureCriteria = measureCriteria;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isRegression() {
    return regression;
  }

  public void setRegression(boolean regression) {
    this.regression = regression;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isHideEmptyColumns() {
    return hideEmptyColumns;
  }

  public void setHideEmptyColumns(boolean hideEmptyColumns) {
    this.hideEmptyColumns = hideEmptyColumns;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isFixColumnHeaders() {
    return fixColumnHeaders;
  }

  public void setFixColumnHeaders(boolean fixColumnHeaders) {
    this.fixColumnHeaders = fixColumnHeaders;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isFixRowHeaders() {
    return fixRowHeaders;
  }

  public void setFixRowHeaders(boolean fixRowHeaders) {
    this.fixRowHeaders = fixRowHeaders;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DisplayDensity getDisplayDensity() {
    return DefaultValue.defaultIfNull(displayDensity);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontSize getFontSize() {
    return DefaultValue.defaultIfNull(fontSize);
  }

  @JsonProperty("optionalAxes")
  @JacksonXmlElementWrapper(localName = "optionalAxes", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "axis", namespace = DXF_2_0)
  public List<Axis> getOptionalAxes() {
    return optionalAxes;
  }

  public void setOptionalAxes(List<Axis> optionalAxes) {
    this.optionalAxes = optionalAxes;
  }

  @JsonProperty("icons")
  @JacksonXmlElementWrapper(localName = "icons", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "icons", namespace = DXF_2_0)
  public Set<Icon> getIcons() {
    return icons;
  }

  public void setIcons(Set<Icon> icons) {
    this.icons = icons;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public NumberType getNumberType() {
    return numberType;
  }

  public void setNumberType(NumberType numberType) {
    this.numberType = numberType;
  }

  public void setFormat(I18nFormat format) {
    this.format = format;
  }

  @JsonIgnore
  public List<Period> getRelativePeriodsList() {
    return relativePeriodsList;
  }

  public void setRelativePeriodsList(List<Period> relativePeriodsList) {
    this.relativePeriodsList = relativePeriodsList;
  }

  public void setDataItemGrid(Grid dataItemGrid) {
    this.dataItemGrid = dataItemGrid;
  }

  @JsonIgnore
  public List<List<DimensionalItemObject>> getGridColumns() {
    return gridColumns;
  }

  public Visualization setGridColumns(List<List<DimensionalItemObject>> gridColumns) {
    this.gridColumns = gridColumns;
    return this;
  }

  @JsonIgnore
  public List<List<DimensionalItemObject>> getGridRows() {
    return gridRows;
  }

  public Visualization setGridRows(List<List<DimensionalItemObject>> gridRows) {
    this.gridRows = gridRows;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getVisualizationPeriodName() {
    return visualizationPeriodName;
  }

  public void setVisualizationPeriodName(String visualizationPeriodName) {
    this.visualizationPeriodName = visualizationPeriodName;
  }

  @JsonIgnore
  public String getGridTitle() {
    return gridTitle;
  }

  public Visualization setGridTitle(String gridTitle) {
    this.gridTitle = gridTitle;
    return this;
  }

  @JsonProperty("series")
  @JacksonXmlElementWrapper(localName = "series", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "seriesItem", namespace = DXF_2_0)
  public List<Series> getSeries() {
    return series;
  }

  public void setSeries(List<Series> series) {
    this.series = series;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getDomainAxisLabel() {
    return domainAxisLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Translatable(propertyName = "domainAxisLabel")
  public String getDisplayDomainAxisLabel() {
    return getTranslation("domainAxisLabel", getDomainAxisLabel());
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getRangeAxisLabel() {
    return rangeAxisLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Translatable(propertyName = "rangeAxisLabel")
  public String getDisplayRangeAxisLabel() {
    return getTranslation("rangeAxisLabel", getRangeAxisLabel());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public VisualizationType getType() {
    return type;
  }

  public void setType(VisualizationType type) {
    this.type = type;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getBaseLineLabel() {
    return baseLineLabel;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getTargetLineLabel() {
    return targetLineLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public VisualizationFontStyle getFontStyle() {
    return fontStyle;
  }

  public void setFontStyle(VisualizationFontStyle fontStyle) {
    this.fontStyle = fontStyle;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getColorSet() {
    return colorSet;
  }

  public void setColorSet(String colorSet) {
    this.colorSet = colorSet;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @PropertyRange(min = -Double.MAX_VALUE)
  public Double getTargetLineValue() {
    return targetLineValue;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @PropertyRange(min = -Double.MAX_VALUE)
  public Double getBaseLineValue() {
    return baseLineValue;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @PropertyRange(min = -Double.MAX_VALUE)
  public Double getRangeAxisMaxValue() {
    return rangeAxisMaxValue;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @PropertyRange(min = -Double.MAX_VALUE)
  public Double getRangeAxisMinValue() {
    return rangeAxisMinValue;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getRangeAxisSteps() {
    return rangeAxisSteps;
  }

  @JsonProperty(access = READ_ONLY)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getRangeAxisDecimals() {
    return rangeAxisDecimals;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "yearlySeries", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "yearlySerie", namespace = DXF_2_0)
  public List<String> getYearlySeries() {
    return yearlySeries;
  }

  public void setYearlySeries(List<String> yearlySeries) {
    this.yearlySeries = yearlySeries;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public OutlierAnalysis getOutlierAnalysis() {
    return outlierAnalysis;
  }

  public void setOutlierAnalysis(OutlierAnalysis outlierAnalysis) {
    this.outlierAnalysis = outlierAnalysis;
  }

  @JsonProperty(value = "seriesKey")
  @JacksonXmlProperty(localName = "seriesKey", namespace = DXF_2_0)
  public SeriesKey getSeriesKey() {
    return seriesKey;
  }

  public void setSeriesKey(SeriesKey seriesKey) {
    this.seriesKey = seriesKey;

    keepLegendReadingCompatibility(this);
  }

  @JsonProperty(value = "axes")
  @JacksonXmlProperty(localName = "axes", namespace = DXF_2_0)
  public List<AxisV2> getAxes() {
    return axes;
  }

  public void setAxes(List<AxisV2> axes) {
    this.axes = axes;

    keepAxesReadingCompatibility(this);
  }

  /**
   * Returns the list of DimensionDescriptor held internally to the current Visualization object.
   * See {@link #addDimensionDescriptor}.
   *
   * @return the list of DimensionDescriptor's held.
   */
  public List<DimensionDescriptor> getDimensionDescriptors() {
    return dimensionDescriptors;
  }

  /**
   * This method will hold the mapping of a dimension and its respective formal type.
   *
   * @param dimension the dimension, which should also be found in "{@link #columnDimensions}" and
   *     "{@link #rowDimensions}".
   * @param dimensionType the formal dimension type. See {@link DimensionType}
   */
  public void addDimensionDescriptor(String dimension, DimensionType dimensionType) {
    this.dimensionDescriptors.add(new DimensionDescriptor(dimension, dimensionType));
  }

  @Override
  public void init(
      final User user,
      final Date date,
      final OrganisationUnit orgUnit,
      final List<OrganisationUnit> organisationUnitsAtLevel,
      final List<OrganisationUnit> organisationUnitsInGroups,
      final I18nFormat format) {
    if (type == PIVOT_TABLE) {
      initializePivotTable(
          user, date, orgUnit, organisationUnitsAtLevel, organisationUnitsInGroups, format);
    } else {
      initializeChart(
          user, date, orgUnit, organisationUnitsAtLevel, organisationUnitsInGroups, format);
    }
  }

  private void initializePivotTable(
      User user,
      Date periodDate,
      OrganisationUnit organisationUnit,
      List<OrganisationUnit> organisationUnitsAtLevel,
      List<OrganisationUnit> organisationUnitsInGroups,
      I18nFormat format) {
    verify(
        (periods != null && !periods.isEmpty()) || hasRelativePeriods(),
        "Must contain periods or relative periods");

    this.relativePeriodDate = periodDate;
    this.relativeOrganisationUnit = organisationUnit;

    if (organisationUnit != null
        && hasReportingParams()
        && reportingParams.isParentOrganisationUnit()) {
      organisationUnit.setCurrentParent(true);
      addTransientOrganisationUnits(organisationUnit.getChildren());
      addTransientOrganisationUnit(organisationUnit);
    }

    if (organisationUnit != null && hasReportingParams() && reportingParams.isOrganisationUnit()) {
      addTransientOrganisationUnit(organisationUnit);
    }

    // Handle special dimension.
    if (isDimensional()) {
      transientCategoryOptionCombos.addAll(
          Objects.requireNonNull(getFirstCategoryCombo()).getSortedOptionCombos());
      verify(
          nonEmptyLists(transientCategoryOptionCombos) == 1,
          "Category option combos size must be larger than 0");
    }

    // Populate grid.
    this.populateGridColumnsAndRows(
        periodDate, user, organisationUnitsAtLevel, organisationUnitsInGroups, format);
  }

  private void initializeChart(
      User user,
      Date periodDate,
      OrganisationUnit organisationUnit,
      List<OrganisationUnit> organisationUnitsAtLevel,
      List<OrganisationUnit> organisationUnitsInGroups,
      I18nFormat format) {
    this.relativeUser = user;
    this.relativePeriodDate = periodDate;
    this.relativeOrganisationUnit = organisationUnit;
    this.organisationUnitsAtLevel = organisationUnitsAtLevel;
    this.organisationUnitsInGroups = organisationUnitsInGroups;
    this.format = format;
  }

  /**
   * Some Visualizations may not have columnDimensions.
   *
   * <p>PIE, GAUGE and others don't have rowsDimensions.
   */
  @Override
  public void populateAnalyticalProperties() {
    super.populateDimensions(columnDimensions, columns);
    super.populateDimensions(rowDimensions, rows);
    super.populateDimensions(filterDimensions, filters);
  }

  @Override
  protected void clearTransientStateProperties() {
    format = null;
    relativePeriodsList = new ArrayList<>();
    relativeUser = null;
    organisationUnitsAtLevel = new ArrayList<>();
    organisationUnitsInGroups = new ArrayList<>();
    itemOrganisationUnitGroups = new ArrayList<>();
    dataItemGrid = null;
    visualizationPeriodName = null;
    gridTitle = null;
    columns = new ArrayList<>();
    rows = new ArrayList<>();
    filters = new ArrayList<>();
    parentGraphMap = new HashMap<>();
    gridColumns = new ArrayList<>();
    gridRows = new ArrayList<>();
  }

  // -------------------------------------------------------------------------
  // Business logic
  // -------------------------------------------------------------------------

  /**
   * Based on the Chart dimension, this method will bring the collection of child items related to
   * its series.
   *
   * @return a list of DimensionalItemObject representing the Chart series
   */
  public List<DimensionalItemObject> chartSeries() {
    // Chart must have one column dimension (series).
    if (isEmpty(columnDimensions) || isBlank(columnDimensions.get(0))) {
      return null;
    }

    return getDimensionalItemObjects(columnDimensions.get(0));
  }

  /**
   * Based on the Chart dimension, this method will bring the collection of child items related to
   * its category.
   *
   * @return a list of DimensionalItemObject representing the Chart category
   */
  public List<DimensionalItemObject> chartCategory() {
    // Chart must have one row dimension (category)
    if (isEmpty(rowDimensions) || isBlank(rowDimensions.get(0))) {
      return null;
    }

    return getDimensionalItemObjects(rowDimensions.get(0));
  }

  /**
   * Returns a list of dimensional items based on the given dimension and internal attributes of the
   * current Visualization object.
   *
   * @param dimension a given dimension
   * @return the list of DimensionalItemObject's
   */
  private List<DimensionalItemObject> getDimensionalItemObjects(String dimension) {
    DimensionalObject object =
        getDimensionalObject(
            dimension,
            relativePeriodDate,
            relativeUser,
            true,
            organisationUnitsAtLevel,
            organisationUnitsInGroups,
            format);

    return object != null ? object.getItems() : null;
  }

  /**
   * Based on the given arguments, this method will populate the current "gridColumns" and
   * "gridRows" objects. It also sets the title of the grid ("gridTitle").
   *
   * @param periodDate the {@link Date} related to the period.
   * @param user the current {@link User}.
   * @param organisationUnitsAtLevel the list of org. units at level.
   * @param organisationUnitsInGroups the list of org. units in groups.
   * @param format the current i18n format {@link I18nFormat}.
   */
  public void populateGridColumnsAndRows(
      Date periodDate,
      User user,
      List<OrganisationUnit> organisationUnitsAtLevel,
      List<OrganisationUnit> organisationUnitsInGroups,
      I18nFormat format) {
    List<List<DimensionalItemObject>> tableColumns = new ArrayList<>();
    List<List<DimensionalItemObject>> tableRows = new ArrayList<>();
    List<DimensionalItemObject> filterItems = new ArrayList<>();

    for (String dimension : columnDimensions) {
      if (dimension != null) {
        tableColumns.add(
            getDimensionalObject(
                    dimension,
                    periodDate,
                    user,
                    false,
                    organisationUnitsAtLevel,
                    organisationUnitsInGroups,
                    format)
                .getItems());
      }
    }

    for (String dimension : rowDimensions) {
      if (dimension != null) {
        tableRows.add(
            getDimensionalObject(
                    dimension,
                    periodDate,
                    user,
                    true,
                    organisationUnitsAtLevel,
                    organisationUnitsInGroups,
                    format)
                .getItems());
      }
    }

    for (String filter : filterDimensions) {
      if (filter != null) {
        filterItems.addAll(
            getDimensionalObject(
                    filter,
                    periodDate,
                    user,
                    true,
                    organisationUnitsAtLevel,
                    organisationUnitsInGroups,
                    format)
                .getItems());
      }
    }

    gridColumns = CombinationGenerator.newInstance(tableColumns).getCombinations();
    gridRows = CombinationGenerator.newInstance(tableRows).getCombinations();

    addListIfEmpty(gridColumns);
    addListIfEmpty(gridRows);

    gridTitle = IdentifiableObjectUtils.join(filterItems);
  }

  public List<OrganisationUnit> getAllOrganisationUnits() {
    if (transientOrganisationUnits != null && !transientOrganisationUnits.isEmpty()) {
      return transientOrganisationUnits;
    } else {
      return organisationUnits;
    }
  }

  public List<Period> getAllPeriods() {
    List<Period> list = new ArrayList<>(relativePeriodsList);

    for (Period period : periods) {
      if (!list.contains(period)) {
        list.add(period);
      }
    }

    return list;
  }

  // -------------------------------------------------------------------------
  // Display and supportive methods
  // -------------------------------------------------------------------------

  /** Returns the category combo of the first data element. */
  private CategoryCombo getFirstCategoryCombo() {
    if (!getDataElements().isEmpty()) {
      return getDataElements().get(0).getCategoryCombos().iterator().next();
    }

    return null;
  }

  /** Returns the number of non-empty lists among the argument lists. */
  private static int nonEmptyLists(List<?>... lists) {
    int nonEmpty = 0;

    for (List<?> list : lists) {
      if (list != null && !list.isEmpty()) {
        ++nonEmpty;
      }
    }

    return nonEmpty;
  }

  /** Tests whether this visualization has reporting parameters. */
  public boolean hasReportingParams() {
    return reportingParams != null;
  }

  public boolean isTargetLine() {
    return targetLineValue != null;
  }

  public boolean isBaseLine() {
    return baseLineValue != null;
  }

  /** Adds an empty list of DimensionalItemObjects to the given list if empty. */
  public static void addListIfEmpty(final List<List<DimensionalItemObject>> list) {
    if (list != null && list.isEmpty()) {
      list.add(asList(new DimensionalItemObject[0]));
    }
  }

  /**
   * Generates a grid for this visualization based on the given aggregate value map.
   *
   * @param grid the grid, should be empty and not null.
   * @param valueMap the mapping of identifiers to aggregate values.
   * @param displayProperty the display property to use for metadata.
   * @param reportParamColumns whether to include report parameter columns.
   * @return a grid.
   */
  public Grid getGrid(
      Grid grid,
      Map<String, Object> valueMap,
      DisplayProperty displayProperty,
      boolean reportParamColumns) {
    valueMap = getSortedKeysMap(valueMap);

    // Set titles.
    if (name != null) {
      grid.setTitle(name);
      grid.setSubtitle(gridTitle);
    } else {
      grid.setTitle(gridTitle);
    }

    // Add headers.
    addHeadersForRows(grid);
    addHeadersForReport(grid, reportParamColumns);

    final int startColumnIndex = grid.getHeaders().size();
    final int numberOfColumns = getGridColumns().size();

    addHeadersForColumns(grid, displayProperty);

    // Add values.
    for (List<DimensionalItemObject> rows : gridRows) {
      grid.addRow();

      addValuesForMetadata(grid, displayProperty, rows);
      addValuesForReport(grid, reportParamColumns, rows);
      addValuesForDimensions(grid, valueMap, rows);

      // TODO hide empty columns
    }

    // Apply boolean flags in the resulting grid.
    applyFlags(grid, startColumnIndex, numberOfColumns);

    return grid;
  }

  /**
   * Applies the logics, related to the boolean flags supported, on top of the given {@link Grid}.
   *
   * @param grid the {@link Grid}.
   * @param startColumnIndex the index required by one of the boolean flags.
   * @param numberOfColumns the number of columns required by one of the flags.
   */
  private void applyFlags(Grid grid, int startColumnIndex, int numberOfColumns) {
    if (hideEmptyColumns) {
      grid.removeEmptyColumns();
    }

    if (regression) {
      grid.addRegressionToGrid(startColumnIndex, numberOfColumns);
    }

    if (cumulativeValues) {
      grid.addCumulativesToGrid(startColumnIndex, numberOfColumns);
    }

    if (sortOrder != NONE) {
      grid.sortGrid(grid.getWidth(), sortOrder);
    }

    if (topLimit > 0) {
      grid.limitGrid(topLimit);
    }

    if (showHierarchy
        && rowDimensions.contains(ORGUNIT_DIM_ID)
        && grid.hasInternalMetaDataKey(ORG_UNIT_ANCESTORS.getKey())) {
      int ouIdColumnIndex = rowDimensions.indexOf(ORGUNIT_DIM_ID) * 4;

      addHierarchyColumns(grid, ouIdColumnIndex);
    }
  }

  /**
   * Adds the values to the given {@link Grid}, based on the map of values and list of row.
   *
   * @param grid the {@link Grid}.
   * @param valueMap the map of values.
   * @param rows the list of {@link DimensionalItemObject}.
   */
  private void addValuesForDimensions(
      Grid grid, Map<String, Object> valueMap, List<DimensionalItemObject> rows) {
    boolean hasValue = false;

    for (List<DimensionalItemObject> column : gridColumns) {
      String key = DimensionalObjectUtils.getKey(column, rows);
      Object value = valueMap.get(key);

      grid.addValue(value);

      hasValue = hasValue || value != null;
    }

    if (hideEmptyRows && !hasValue) {
      grid.removeCurrentWriteRow();
    }
  }

  /**
   * Adds values, into the given {@link Grid}, related to reporting, if requested.
   *
   * @param grid the {@link Grid}.
   * @param reportParamColumns if true, the values are added to the grid.
   * @param rows the list of rows necessary to evaluate one of the values.
   */
  private void addValuesForReport(
      Grid grid, boolean reportParamColumns, List<DimensionalItemObject> rows) {
    if (reportParamColumns) {
      grid.addValue(visualizationPeriodName);
      grid.addValue(getParentOrganisationUnitName());
      grid.addValue(isCurrentParent(rows) ? "Yes" : "No");
    }
  }

  /**
   * Adds headers into the given {@link Grid} based on the given "rows".
   *
   * @param grid the {@link Grid}.
   * @param displayProperty the current {@link DisplayProperty}.
   * @param rows the rows where the values are living.
   */
  private static void addValuesForMetadata(
      Grid grid, DisplayProperty displayProperty, List<DimensionalItemObject> rows) {
    for (DimensionalItemObject object : rows) {
      grid.addValue(object.getDimensionItem());
      grid.addValue(object.getDisplayProperty(displayProperty));
      grid.addValue(object.getCode());
      grid.addValue(object.getDisplayDescription());
    }
  }

  /**
   * Adds headers into the given {@link Grid} based on the current list of "gridColumns".
   *
   * @param grid the {@link Grid}.
   */
  private void addHeadersForColumns(Grid grid, DisplayProperty displayProperty) {
    for (List<DimensionalItemObject> columns : gridColumns) {
      grid.addHeader(
          new GridHeader(
              getColumnName(columns),
              getPrettyColumnName(columns, displayProperty),
              NUMBER,
              false,
              false));
    }
  }

  /**
   * Adds static headers into the given {@link Grid} based on the report params flag.
   *
   * @param reportParamColumns if true, add headers for report params.
   * @param grid the {@link Grid}.
   */
  private static void addHeadersForReport(Grid grid, boolean reportParamColumns) {
    if (reportParamColumns) {
      grid.addHeader(
          new GridHeader("Reporting month", REPORTING_MONTH_COLUMN_NAME, TEXT, true, true));
      grid.addHeader(
          new GridHeader(
              "Organisation unit parameter", PARAM_ORGANISATIONUNIT_COLUMN_NAME, TEXT, true, true));
      grid.addHeader(
          new GridHeader(
              "Organisation unit is parent",
              ORGANISATION_UNIT_IS_PARENT_COLUMN_NAME,
              TEXT,
              true,
              true));
    }
  }

  /**
   * Adds headers into the given {@link Grid} based on the current list of "rowDimensions". It also
   * populates the metadata so, it can be used to extract names for the header.
   *
   * @param grid the {@link Grid}.
   */
  private void addHeadersForRows(Grid grid) {
    Map<String, String> metaData = getMetaData();
    metaData.putAll(PRETTY_NAMES);

    for (String dimension : rowDimensions) {
      String dimensionId = getDimensionIdentifierFor(dimension, getDimensionDescriptors());
      String name = defaultIfEmpty(metaData.get(dimensionId), dimensionId);
      String col = defaultIfEmpty(COLUMN_NAMES.get(dimensionId), dimensionId);

      grid.addHeader(new GridHeader(name + " ID", col + "id", TEXT, true, true));
      grid.addHeader(new GridHeader(name, col + "name", TEXT, false, true));
      grid.addHeader(new GridHeader(name + " code", col + "code", TEXT, true, true));
      grid.addHeader(new GridHeader(name + " description", col + "description", TEXT, true, true));
    }
  }

  /** Indicates whether this visualization is multidimensional. */
  public boolean isDimensional() {
    return !getDataElements().isEmpty()
        && (columnDimensions.contains(CATEGORYOPTIONCOMBO_DIM_ID)
            || rowDimensions.contains(CATEGORYOPTIONCOMBO_DIM_ID));
  }

  /** Indicates whether this visualization is a chart. */
  public boolean isChart() {
    return type.isChart();
  }

  /**
   * Generates a pretty column name based on the given display property of the argument objects.
   * Null arguments are ignored in the name.
   */
  public static String getPrettyColumnName(
      List<DimensionalItemObject> objects, DisplayProperty displayProperty) {
    StringBuilder builder = new StringBuilder();

    for (DimensionalItemObject object : objects) {
      builder.append(object != null ? (object.getDisplayProperty(displayProperty) + SPACE) : EMPTY);
    }
    return builder.length() > 0
        ? builder.substring(0, builder.lastIndexOf(SPACE))
        : TOTAL_COLUMN_PRETTY_NAME;
  }

  /**
   * Generates a column name based on short-names of the argument objects. Null arguments are
   * ignored in the name.
   *
   * <p>The period column name must be static when on columns, so it can be re-used in reports,
   * hence the name property is used which will be formatted only when the period dimension is on
   * rows.
   */
  public static String getColumnName(List<DimensionalItemObject> objects) {
    StringBuilder sb = new StringBuilder();

    for (DimensionalItemObject object : objects) {
      if (object instanceof Period) {
        sb.append(object.getName()).append(NAME_SEP);
      } else {
        sb.append(object != null ? (object.getShortName() + NAME_SEP) : EMPTY);
      }
    }

    String column = columnEncode(sb.toString());

    return column != null && column.length() > 0
        ? column.substring(0, column.lastIndexOf(NAME_SEP))
        : TOTAL_COLUMN_NAME;
  }

  /** Generates a string which is acceptable as a filename. */
  public static String columnEncode(String string) {
    if (string != null) {
      string = string.replace("<", "_lt");
      string = string.replace(">", "_gt");
      string = string.replaceAll(ILLEGAL_FILENAME_CHARS_REGEX, EMPTY);
      string = string.length() > 255 ? string.substring(0, 255) : string;
      string = string.toLowerCase();
    }
    return string;
  }

  /**
   * Checks whether the given List of IdentifiableObjects contains an object which is an
   * OrganisationUnit and has the currentParent property set to true.
   *
   * @param objects the List of IdentifiableObjects.
   */
  public static boolean isCurrentParent(List<? extends IdentifiableObject> objects) {
    for (IdentifiableObject object : objects) {
      if (object instanceof OrganisationUnit organisationUnit) {
        return organisationUnit.isCurrentParent();
      }
    }
    return false;
  }

  /** Returns the name of the parent organisation unit, or an empty string if null. */
  public String getParentOrganisationUnitName() {
    return relativeOrganisationUnit != null ? relativeOrganisationUnit.getName() : EMPTY;
  }

  /** Adds grid columns for each organisation unit level. */
  @SuppressWarnings("unchecked")
  private void addHierarchyColumns(Grid grid, int ouIdColumnIndex) {
    Map<Object, List<?>> ancestorMap =
        (Map<Object, List<?>>) grid.getInternalMetaData().get(ORG_UNIT_ANCESTORS.getKey());

    Assert.notEmpty(
        ancestorMap, "Ancestor map cannot be null or empty when show hierarchy is enabled");

    int newColumns = ancestorMap.values().stream().mapToInt(List::size).max().orElse(0);

    List<GridHeader> headers = new ArrayList<>();

    for (int i = 0; i < newColumns; i++) {
      int level = i + 1;

      String name = String.format("Org unit level %d", level);
      String column = String.format("orgunitlevel%d", level);

      headers.add(new GridHeader(name, column, TEXT, false, true));
    }

    grid.addHeaders(ouIdColumnIndex, headers);
    grid.addAndPopulateColumnsBefore(ouIdColumnIndex, ancestorMap, newColumns);
  }

  public String generateTitle() {
    List<String> titleItems = new ArrayList<>();

    for (String filter : filterDimensions) {
      DimensionalObject object =
          getDimensionalObject(
              filter,
              relativePeriodDate,
              relativeUser,
              true,
              organisationUnitsAtLevel,
              organisationUnitsInGroups,
              format);

      if (object != null) {
        String item = IdentifiableObjectUtils.join(object.getItems());
        String prettyFilter = DimensionalObjectUtils.getPrettyFilter(object.getFilter());

        if (item != null) {
          titleItems.add(item);
        }

        if (prettyFilter != null) {
          titleItems.add(prettyFilter);
        }
      }
    }

    return join(titleItems, DimensionalObjectUtils.TITLE_ITEM_SEP);
  }
}
