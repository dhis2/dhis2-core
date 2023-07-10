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
package org.hisp.dhis.chart.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.commons.collection.ListUtils.getArray;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.MathRuntimeException;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.NumericSortWrapper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.ChartService;
import org.hisp.dhis.visualization.PlotData;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationType;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.ValueDataset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.visualization.ChartService")
public class DefaultChartService implements ChartService {
  private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

  private static final Font SUB_TITLE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

  private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

  private static final String TREND_PREFIX = "Trend - ";

  private static final Color[] COLORS = {
    Color.decode("#88be3b"),
    Color.decode("#3b6286"),
    Color.decode("#b7404c"),
    Color.decode("#ff9f3a"),
    Color.decode("#968f8f"),
    Color.decode("#b7409f"),
    Color.decode("#ffda64"),
    Color.decode("#4fbdae"),
    Color.decode("#b78040"),
    Color.decode("#676767"),
    Color.decode("#6a33cf"),
    Color.decode("#4a7833")
  };

  private static final Color COLOR_LIGHT_GRAY = Color.decode("#dddddd");

  private static final Color COLOR_LIGHTER_GRAY = Color.decode("#eeeeee");

  private static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final PeriodService periodService;

  private final DataValueService dataValueService;

  private final MinMaxDataElementService minMaxDataElementService;

  private final CurrentUserService currentUserService;

  private final OrganisationUnitService organisationUnitService;

  private final AnalyticsService analyticsService;

  private final EventAnalyticsService eventAnalyticsService;

  // -------------------------------------------------------------------------
  // ChartService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public JFreeChart getJFreeChart(
      PlotData plotData, Date date, OrganisationUnit organisationUnit, I18nFormat format) {
    return getJFreeChart(
        plotData, date, organisationUnit, format, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional(readOnly = true)
  public JFreeChart getJFreeChart(
      PlotData plotData,
      Date date,
      OrganisationUnit organisationUnit,
      I18nFormat format,
      User currentUser) {
    User user = (currentUser != null ? currentUser : currentUserService.getCurrentUser());

    if (organisationUnit == null && user != null) {
      organisationUnit = user.getOrganisationUnit();
    }

    List<OrganisationUnit> atLevels = new ArrayList<>();
    List<OrganisationUnit> inGroups = new ArrayList<>();

    if (plotData.hasOrganisationUnitLevels()) {
      atLevels.addAll(
          organisationUnitService.getOrganisationUnitsAtLevels(
              plotData.getOrganisationUnitLevels(), plotData.getOrganisationUnits()));
    }

    if (plotData.hasItemOrganisationUnitGroups()) {
      inGroups.addAll(
          organisationUnitService.getOrganisationUnits(
              plotData.getItemOrganisationUnitGroups(), plotData.getOrganisationUnits()));
    }

    plotData.init(user, date, organisationUnit, atLevels, inGroups, format);

    JFreeChart resultChart = getJFreeChart(plotData);

    plotData.clearTransientState();

    return resultChart;
  }

  // -------------------------------------------------------------------------
  // Specific chart methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public JFreeChart getJFreePeriodChart(
      Indicator indicator, OrganisationUnit unit, boolean title, I18nFormat format) {
    List<Period> periods =
        periodService.reloadPeriods(
            new RelativePeriods().setLast12Months(true).getRelativePeriods(format, true));

    Visualization visualization = new Visualization();

    if (title) {
      visualization.setName(indicator.getName());
    }

    visualization.setType(VisualizationType.LINE);
    visualization.setColumnDimensions(Arrays.asList(DimensionalObject.DATA_X_DIM_ID));
    visualization.setRowDimensions(Arrays.asList(DimensionalObject.PERIOD_DIM_ID));
    visualization.setFilterDimensions(Arrays.asList(DimensionalObject.ORGUNIT_DIM_ID));
    visualization.setHideLegend(true);
    visualization.addDataDimensionItem(indicator);
    visualization.setPeriods(periods);
    visualization.getOrganisationUnits().add(unit);
    visualization.setHideSubtitle(title);
    visualization.setFormat(format);

    return getJFreeChart(new PlotData(visualization));
  }

  @Override
  @Transactional(readOnly = true)
  public JFreeChart getJFreeOrganisationUnitChart(
      Indicator indicator, OrganisationUnit parent, boolean title, I18nFormat format) {
    List<Period> periods =
        periodService.reloadPeriods(
            new RelativePeriods().setThisYear(true).getRelativePeriods(format, true));

    Visualization visualization = new Visualization();

    if (title) {
      visualization.setName(indicator.getName());
    }

    visualization.setType(VisualizationType.COLUMN);
    visualization.setColumnDimensions(Arrays.asList(DimensionalObject.DATA_X_DIM_ID));
    visualization.setRowDimensions(Arrays.asList(DimensionalObject.ORGUNIT_DIM_ID));
    visualization.setFilterDimensions(Arrays.asList(DimensionalObject.PERIOD_DIM_ID));
    visualization.setHideLegend(true);
    visualization.addDataDimensionItem(indicator);
    visualization.setPeriods(periods);
    visualization.setOrganisationUnits(parent.getSortedChildren());
    visualization.setHideSubtitle(title);
    visualization.setFormat(format);

    return getJFreeChart(new PlotData(visualization));
  }

  @Override
  @Transactional(readOnly = true)
  public JFreeChart getJFreeChartHistory(
      DataElement dataElement,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      Period lastPeriod,
      OrganisationUnit organisationUnit,
      int historyLength,
      I18nFormat format) {
    lastPeriod = periodService.reloadPeriod(lastPeriod);

    List<Period> periods = periodService.getPeriods(lastPeriod, historyLength);

    MinMaxDataElement minMax =
        minMaxDataElementService.getMinMaxDataElement(
            organisationUnit, dataElement, categoryOptionCombo);

    UnivariateInterpolator interpolator = new SplineInterpolator();

    int periodCount = 0;
    List<Double> x = new ArrayList<>();
    List<Double> y = new ArrayList<>();

    // ---------------------------------------------------------------------
    // DataValue, MinValue and MaxValue DataSets
    // ---------------------------------------------------------------------

    DefaultCategoryDataset dataValueDataSet = new DefaultCategoryDataset();
    DefaultCategoryDataset metaDataSet = new DefaultCategoryDataset();

    for (Period period : periods) {
      ++periodCount;

      period.setName(format.formatPeriod(period));

      DataValue dataValue =
          dataValueService.getDataValue(
              dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo);

      double value = 0;

      if (dataValue != null
          && dataValue.getValue() != null
          && MathUtils.isNumeric(dataValue.getValue())) {
        value = Double.parseDouble(dataValue.getValue());

        x.add((double) periodCount);
        y.add(value);
      }

      dataValueDataSet.addValue(value, dataElement.getShortName(), period.getName());

      if (minMax != null) {
        metaDataSet.addValue(minMax.getMin(), "Min value", period.getName());
        metaDataSet.addValue(minMax.getMax(), "Max value", period.getName());
      }
    }

    // ---------------------------------------------------------------------
    // Interpolation DataSet
    // ---------------------------------------------------------------------

    if (x.size() >= 3) // minimum 3 points required for interpolation
    {
      periodCount = 0;

      double[] xa = getArray(x);

      int min = MathUtils.getMin(xa).intValue();
      int max = MathUtils.getMax(xa).intValue();

      try {
        UnivariateFunction function = interpolator.interpolate(xa, getArray(y));

        for (Period period : periods) {
          if (++periodCount >= min && periodCount <= max) {
            metaDataSet.addValue(function.value(periodCount), "Regression value", period.getName());
          }
        }
      } catch (MathRuntimeException ex) {
        throw new RuntimeException("Failed to interpolate", ex);
      }
    }

    // ---------------------------------------------------------------------
    // Plots
    // ---------------------------------------------------------------------

    CategoryPlot plot =
        getCategoryPlot(
            dataValueDataSet,
            getBarRenderer(),
            PlotOrientation.VERTICAL,
            CategoryLabelPositions.UP_45);

    plot.setDataset(1, metaDataSet);
    plot.setRenderer(1, getLineRenderer());

    return getBasicJFreeChart(plot);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Returns a basic JFreeChart. */
  private JFreeChart getBasicJFreeChart(CategoryPlot plot) {
    JFreeChart jFreeChart = new JFreeChart(null, TITLE_FONT, plot, false);

    jFreeChart.setBackgroundPaint(Color.WHITE);
    jFreeChart.setAntiAlias(true);

    return jFreeChart;
  }

  /** Returns a CategoryPlot. */
  private CategoryPlot getCategoryPlot(
      CategoryDataset dataSet,
      CategoryItemRenderer renderer,
      PlotOrientation orientation,
      CategoryLabelPositions labelPositions) {
    CategoryPlot plot = new CategoryPlot(dataSet, new CategoryAxis(), new NumberAxis(), renderer);

    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    plot.setOrientation(orientation);

    CategoryAxis xAxis = plot.getDomainAxis();
    xAxis.setCategoryLabelPositions(labelPositions);

    return plot;
  }

  /** Returns a bar renderer. */
  private BarRenderer getBarRenderer() {
    BarRenderer renderer = new BarRenderer();

    renderer.setMaximumBarWidth(0.07);

    for (int i = 0; i < COLORS.length; i++) {
      renderer.setSeriesPaint(i, COLORS[i]);
      renderer.setShadowVisible(false);
    }

    return renderer;
  }

  /** Returns a line and shape renderer. */
  private LineAndShapeRenderer getLineRenderer() {
    LineAndShapeRenderer renderer = new LineAndShapeRenderer();

    for (int i = 0; i < COLORS.length; i++) {
      renderer.setSeriesPaint(i, COLORS[i]);
    }

    return renderer;
  }

  /** Returns a stacked bar renderer. */
  private StackedBarRenderer getStackedBarRenderer() {
    StackedBarRenderer renderer = new StackedBarRenderer();

    for (int i = 0; i < COLORS.length; i++) {
      renderer.setSeriesPaint(i, COLORS[i]);
      renderer.setShadowVisible(false);
    }

    return renderer;
  }

  /** Returns a stacked area renderer. */
  private AreaRenderer getStackedAreaRenderer() {
    StackedAreaRenderer renderer = new StackedAreaRenderer();

    for (int i = 0; i < COLORS.length; i++) {
      renderer.setSeriesPaint(i, COLORS[i]);
    }

    return renderer;
  }

  /** Returns a horizontal line marker for the given x value and label. */
  private Marker getMarker(Double value, String label) {
    Marker marker = new ValueMarker(value);
    marker.setPaint(Color.BLACK);
    marker.setStroke(new BasicStroke(1.1f));
    marker.setLabel(label);
    marker.setLabelOffset(new RectangleInsets(-10, 50, 0, 0));
    marker.setLabelFont(SUB_TITLE_FONT);

    return marker;
  }

  /** Returns a JFreeChart of type defined in the chart argument. */
  private JFreeChart getJFreeChart(PlotData plotData) {
    final CategoryDataset[] dataSets = getCategoryDataSet(plotData);
    final CategoryDataset dataSet = dataSets[0];

    final BarRenderer barRenderer = getBarRenderer();
    final LineAndShapeRenderer lineRenderer = getLineRenderer();

    // ---------------------------------------------------------------------
    // Plot
    // ---------------------------------------------------------------------

    CategoryPlot plot;

    if (plotData.isType(VisualizationType.LINE.name())) {
      plot = new CategoryPlot(dataSet, new CategoryAxis(), new NumberAxis(), lineRenderer);
      plot.setOrientation(PlotOrientation.VERTICAL);
    } else if (plotData.isType(VisualizationType.COLUMN.name())) {
      plot = new CategoryPlot(dataSet, new CategoryAxis(), new NumberAxis(), barRenderer);
      plot.setOrientation(PlotOrientation.VERTICAL);
    } else if (plotData.isType(VisualizationType.BAR.name())) {
      plot = new CategoryPlot(dataSet, new CategoryAxis(), new NumberAxis(), barRenderer);
      plot.setOrientation(PlotOrientation.HORIZONTAL);
    } else if (plotData.isType(VisualizationType.AREA.name())) {
      return getStackedAreaChart(plotData, dataSet);
    } else if (plotData.isType(VisualizationType.PIE.name())) {
      return getMultiplePieChart(plotData, dataSets);
    } else if (plotData.isType(VisualizationType.STACKED_COLUMN.name())) {
      return getStackedBarChart(plotData, dataSet, false);
    } else if (plotData.isType(VisualizationType.STACKED_BAR.name())) {
      return getStackedBarChart(plotData, dataSet, true);
    } else if (plotData.isType(VisualizationType.RADAR.name())) {
      return getRadarChart(plotData, dataSet);
    } else if (plotData.isType(VisualizationType.GAUGE.name())) {
      Number number = dataSet.getValue(0, 0);
      ValueDataset valueDataSet = new DefaultValueDataset(number);

      return getGaugeChart(plotData, valueDataSet);
    } else {
      throw new IllegalArgumentException("Illegal or no chart type: " + plotData.getType());
    }

    if (plotData.isRegression()) {
      plot.setDataset(1, dataSets[1]);
      plot.setRenderer(1, lineRenderer);
    }

    JFreeChart jFreeChart =
        new JFreeChart(plotData.getName(), TITLE_FONT, plot, !plotData.isHideLegend());

    setBasicConfig(jFreeChart, plotData);

    if (plotData.isTargetLine()) {
      plot.addRangeMarker(getMarker(plotData.getTargetLineValue(), plotData.getTargetLineLabel()));
    }

    if (plotData.isBaseLine()) {
      plot.addRangeMarker(getMarker(plotData.getBaseLineValue(), plotData.getBaseLineLabel()));
    }

    if (plotData.isHideSubtitle()) {
      jFreeChart.addSubtitle(getSubTitle(plotData));
    }

    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // ---------------------------------------------------------------------
    // Category label positions
    // ---------------------------------------------------------------------

    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    domainAxis.setLabel(plotData.getDomainAxisLabel());

    ValueAxis rangeAxis = plot.getRangeAxis();
    rangeAxis.setLabel(plotData.getRangeAxisLabel());

    return jFreeChart;
  }

  private JFreeChart getStackedAreaChart(PlotData plotData, CategoryDataset dataSet) {
    JFreeChart stackedAreaChart =
        ChartFactory.createStackedAreaChart(
            plotData.getName(),
            plotData.getDomainAxisLabel(),
            plotData.getRangeAxisLabel(),
            dataSet,
            PlotOrientation.VERTICAL,
            !plotData.isHideLegend(),
            false,
            false);

    setBasicConfig(stackedAreaChart, plotData);

    CategoryPlot plot = (CategoryPlot) stackedAreaChart.getPlot();
    plot.setOrientation(PlotOrientation.VERTICAL);
    plot.setRenderer(getStackedAreaRenderer());

    CategoryAxis xAxis = plot.getDomainAxis();
    xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    xAxis.setLabelFont(LABEL_FONT);

    return stackedAreaChart;
  }

  private JFreeChart getRadarChart(PlotData plotData, CategoryDataset dataSet) {
    SpiderWebPlot plot = new SpiderWebPlot(dataSet, TableOrder.BY_ROW);
    plot.setLabelFont(LABEL_FONT);

    JFreeChart radarChart =
        new JFreeChart(plotData.getName(), TITLE_FONT, plot, !plotData.isHideLegend());

    setBasicConfig(radarChart, plotData);

    return radarChart;
  }

  private JFreeChart getStackedBarChart(
      PlotData plotData, CategoryDataset dataSet, boolean horizontal) {
    JFreeChart stackedBarChart =
        ChartFactory.createStackedBarChart(
            plotData.getName(),
            plotData.getDomainAxisLabel(),
            plotData.getRangeAxisLabel(),
            dataSet,
            PlotOrientation.VERTICAL,
            !plotData.isHideLegend(),
            false,
            false);

    setBasicConfig(stackedBarChart, plotData);

    CategoryPlot plot = (CategoryPlot) stackedBarChart.getPlot();
    plot.setOrientation(horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL);
    plot.setRenderer(getStackedBarRenderer());

    CategoryAxis xAxis = plot.getDomainAxis();
    xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

    return stackedBarChart;
  }

  private JFreeChart getMultiplePieChart(PlotData plotData, CategoryDataset[] dataSets) {
    JFreeChart multiplePieChart =
        ChartFactory.createMultiplePieChart(
            plotData.getName(),
            dataSets[0],
            TableOrder.BY_ROW,
            !plotData.isHideLegend(),
            false,
            false);

    setBasicConfig(multiplePieChart, plotData);

    if (multiplePieChart.getLegend() != null) {
      multiplePieChart.getLegend().setItemFont(SUB_TITLE_FONT);
    }

    MultiplePiePlot multiplePiePlot = (MultiplePiePlot) multiplePieChart.getPlot();
    JFreeChart pieChart = multiplePiePlot.getPieChart();
    pieChart.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
    pieChart.getTitle().setFont(SUB_TITLE_FONT);

    PiePlot<?> piePlot = (PiePlot<?>) pieChart.getPlot();
    piePlot.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
    piePlot.setOutlinePaint(DEFAULT_BACKGROUND_COLOR);
    piePlot.setLabelFont(LABEL_FONT);
    piePlot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}"));
    piePlot.setSimpleLabels(true);
    piePlot.setIgnoreZeroValues(true);
    piePlot.setIgnoreNullValues(true);
    piePlot.setShadowXOffset(0d);
    piePlot.setShadowYOffset(0d);

    for (int i = 0; i < dataSets[0].getColumnCount(); i++) {
      piePlot.setSectionPaint(dataSets[0].getColumnKey(i), COLORS[(i % COLORS.length)]);
    }

    return multiplePieChart;
  }

  private JFreeChart getGaugeChart(PlotData plotData, ValueDataset dataSet) {
    MeterPlot meterPlot = new MeterPlot(dataSet);

    meterPlot.setUnits("");
    meterPlot.setRange(new Range(0.0d, 100d));

    for (int i = 0; i < 10; i++) {
      double start = i * 10d;
      double end = start + 10d;
      String label = String.valueOf(start);

      meterPlot.addInterval(
          new MeterInterval(
              label, new Range(start, end), COLOR_LIGHT_GRAY, null, COLOR_LIGHT_GRAY));
    }

    meterPlot.setMeterAngle(180);
    meterPlot.setDialBackgroundPaint(COLOR_LIGHT_GRAY);
    meterPlot.setDialShape(DialShape.CHORD);
    meterPlot.setNeedlePaint(COLORS[0]);
    meterPlot.setTickLabelsVisible(true);
    meterPlot.setTickLabelFont(LABEL_FONT);
    meterPlot.setTickLabelPaint(Color.BLACK);
    meterPlot.setTickPaint(COLOR_LIGHTER_GRAY);
    meterPlot.setValueFont(TITLE_FONT);
    meterPlot.setValuePaint(Color.BLACK);

    JFreeChart meterChart = new JFreeChart(plotData.getName(), meterPlot);
    setBasicConfig(meterChart, plotData);
    meterChart.removeLegend();

    return meterChart;
  }

  /**
   * Sets basic configuration including title font, subtitle, background paint and anti-alias on the
   * given JFreeChart.
   */
  private void setBasicConfig(JFreeChart jFreeChart, PlotData plotData) {
    jFreeChart.getTitle().setFont(TITLE_FONT);

    jFreeChart.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
    jFreeChart.setAntiAlias(true);

    if (!plotData.isHideTitle()) {
      jFreeChart.addSubtitle(getSubTitle(plotData));
    }

    Plot plot = jFreeChart.getPlot();
    plot.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
    plot.setOutlinePaint(DEFAULT_BACKGROUND_COLOR);
  }

  private TextTitle getSubTitle(PlotData plotData) {
    TextTitle textTitle = new TextTitle();

    String title = plotData.hasTitle() ? plotData.getDisplayTitle() : plotData.generateTitle();

    textTitle.setFont(SUB_TITLE_FONT);
    textTitle.setText(title);

    return textTitle;
  }

  private CategoryDataset[] getCategoryDataSet(PlotData plotData) {
    Map<String, Object> valueMap;

    if (plotData.isAggregate()) {
      valueMap = analyticsService.getAggregatedDataValueMapping(plotData.getVisualization());
    } else {
      if (plotData.getEventChart() != null) {
        Grid grid = eventAnalyticsService.getAggregatedEventData(plotData.getEventChart());

        plotData.getEventChart().setDataItemGrid(grid);

        valueMap = GridUtils.getMetaValueMapping(grid, (grid.getWidth() - 1));
      } else {
        Grid grid = eventAnalyticsService.getAggregatedEventData(plotData.getEventVisualization());

        plotData.getEventVisualization().setDataItemGrid(grid);

        valueMap = GridUtils.getMetaValueMapping(grid, (grid.getWidth() - 1));
      }
    }

    DefaultCategoryDataset regularDataSet = new DefaultCategoryDataset();
    DefaultCategoryDataset regressionDataSet = new DefaultCategoryDataset();

    SimpleRegression regression = new SimpleRegression();

    valueMap = DimensionalObjectUtils.getSortedKeysMap(valueMap);

    List<NameableObject> seriez = new ArrayList<>(plotData.series());
    List<NameableObject> categories =
        new ArrayList<>(defaultIfNull(plotData.category(), emptyList()));

    if (plotData.hasSortOrder()) {
      categories = getSortedCategories(categories, plotData, valueMap);
    }

    for (NameableObject series : seriez) {
      double categoryIndex = 0;

      for (NameableObject category : categories) {
        categoryIndex++;

        String key = getKey(series, category, plotData.getAnalyticsType());

        Object object = valueMap.get(key);

        Number value = object != null && object instanceof Number ? (Number) object : null;

        regularDataSet.addValue(value, series.getShortName(), category.getShortName());

        if (plotData.isRegression()
            && value != null
            && value instanceof Double
            && !MathUtils.isEqual((Double) value, MathUtils.ZERO)) {
          regression.addData(categoryIndex, (Double) value);
        }
      }

      if (plotData.isRegression()) // Period must be category
      {
        categoryIndex = 0;

        for (NameableObject category : plotData.category()) {
          final double value = regression.predict(categoryIndex++);

          // Enough values must exist for regression

          if (!Double.isNaN(value)) {
            regressionDataSet.addValue(
                value, TREND_PREFIX + series.getShortName(), category.getShortName());
          }
        }
      }
    }

    return new CategoryDataset[] {regularDataSet, regressionDataSet};
  }

  /**
   * Creates a key based on the given input. Sorts the key on its components to remove significance
   * of column order.
   */
  private String getKey(
      NameableObject series, NameableObject category, AnalyticsType analyticsType) {
    String key = series.getUid() + DIMENSION_SEP + category.getUid();

    // Replace potential operand separator with dimension separator

    key =
        AnalyticsType.AGGREGATE.equals(analyticsType)
            ? key.replace(DataElementOperand.SEPARATOR, DIMENSION_SEP)
            : key;

    // TODO fix issue with keys including -.

    return DimensionalObjectUtils.sortKey(key);
  }

  /**
   * Returns a list of sorted nameable objects. Sorting is defined per the corresponding value in
   * the given value map.
   */
  private List<NameableObject> getSortedCategories(
      List<NameableObject> categories, PlotData plotData, Map<String, Object> valueMap) {
    NameableObject series = plotData.series().get(0);

    int sortOrder = plotData.getSortOrder();

    List<NumericSortWrapper<NameableObject>> list = new ArrayList<>();

    for (NameableObject category : categories) {
      String key = getKey(series, category, plotData.getAnalyticsType());

      Object value = valueMap.get(key);

      if (value instanceof Number) {
        list.add(new NumericSortWrapper<>(category, (Double) value, sortOrder));
      }
    }

    Collections.sort(list);

    return NumericSortWrapper.getObjectList(list);
  }
}
