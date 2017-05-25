package org.hisp.dhis.chart.impl;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.MathRuntimeException;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.chart.BaseChart;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.ValueDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.TableOrder;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.commons.collection.ListUtils.getArray;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultChartService
    extends GenericAnalyticalObjectService<Chart>
    implements ChartService
{
    private static final Font TITLE_FONT = new Font( Font.SANS_SERIF, Font.BOLD, 12 );
    private static final Font SUB_TITLE_FONT = new Font( Font.SANS_SERIF, Font.PLAIN, 11 );
    private static final Font LABEL_FONT = new Font( Font.SANS_SERIF, Font.PLAIN, 10 );

    private static final String TREND_PREFIX = "Trend - ";

    private static final Color[] COLORS = { Color.decode( "#88be3b" ), Color.decode( "#3b6286" ),
        Color.decode( "#b7404c" ), Color.decode( "#ff9f3a" ), Color.decode( "#968f8f" ), Color.decode( "#b7409f" ),
        Color.decode( "#ffda64" ), Color.decode( "#4fbdae" ), Color.decode( "#b78040" ), Color.decode( "#676767" ),
        Color.decode( "#6a33cf" ), Color.decode( "#4a7833" ) };

    private static final Color COLOR_TRANSPARENT = new Color( 255, 255, 255, 0 );
    private static final Color COLOR_LIGHT_GRAY = Color.decode( "#dddddd" );
    private static final Color COLOR_LIGHTER_GRAY = Color.decode( "#eeeeee" );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private AnalyticalObjectStore<Chart> chartStore;

    public void setChartStore( AnalyticalObjectStore<Chart> chartStore )
    {
        this.chartStore = chartStore;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private MinMaxDataElementService minMaxDataElementService;

    public void setMinMaxDataElementService( MinMaxDataElementService minMaxDataElementService )
    {
        this.minMaxDataElementService = minMaxDataElementService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private AnalyticsService analyticsService;

    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    private EventAnalyticsService eventAnalyticsService;

    public void setEventAnalyticsService( EventAnalyticsService eventAnalyticsService )
    {
        this.eventAnalyticsService = eventAnalyticsService;
    }

    // -------------------------------------------------------------------------
    // ChartService implementation
    // -------------------------------------------------------------------------

    @Override
    protected AnalyticalObjectStore<Chart> getAnalyticalObjectStore()
    {
        return chartStore;
    }

    @Override
    public JFreeChart getJFreeChart( int id, I18nFormat format )
    {
        Chart chart = getChart( id );

        return chart != null ? getJFreeChart( chart, format ) : null;
    }

    @Override
    public JFreeChart getJFreeChart( BaseChart chart, I18nFormat format )
    {
        return getJFreeChart( chart, null, null, format );
    }

    @Override
    public JFreeChart getJFreeChart( BaseChart chart, Date date, OrganisationUnit organisationUnit, I18nFormat format )
    {
        return getJFreeChart( chart, date, organisationUnit, format, currentUserService.getCurrentUser() );
    }

    @Override
    public JFreeChart getJFreeChart( BaseChart chart, Date date, OrganisationUnit organisationUnit, I18nFormat format, User currentUser )
    {
        User user = (currentUser != null ? currentUser : currentUserService.getCurrentUser());

        if ( organisationUnit == null && user != null )
        {
            organisationUnit = user.getOrganisationUnit();
        }

        List<OrganisationUnit> atLevels = new ArrayList<>();
        List<OrganisationUnit> inGroups = new ArrayList<>();

        if ( chart.hasOrganisationUnitLevels() )
        {
            atLevels.addAll( organisationUnitService.getOrganisationUnitsAtLevels( chart.getOrganisationUnitLevels(), chart.getOrganisationUnits() ) );
        }

        if ( chart.hasItemOrganisationUnitGroups() )
        {
            inGroups.addAll( organisationUnitService.getOrganisationUnits( chart.getItemOrganisationUnitGroups(), chart.getOrganisationUnits() ) );
        }

        chart.init( user, date, organisationUnit, atLevels, inGroups, format );

        JFreeChart resultChart = getJFreeChart( chart );

        chart.clearTransientState();

        return resultChart;
    }

    // -------------------------------------------------------------------------
    // Specific chart methods
    // -------------------------------------------------------------------------

    @Override
    public JFreeChart getJFreePeriodChart( Indicator indicator, OrganisationUnit unit, boolean title, I18nFormat format )
    {
        List<Period> periods = periodService.reloadPeriods(
            new RelativePeriods().setLast12Months( true ).getRelativePeriods( format, true ) );

        Chart chart = new Chart();

        if ( title )
        {
            chart.setName( indicator.getName() );
        }

        chart.setType( ChartType.LINE );
        chart.setDimensions( DimensionalObject.DATA_X_DIM_ID, DimensionalObject.PERIOD_DIM_ID, DimensionalObject.ORGUNIT_DIM_ID );
        chart.setHideLegend( true );
        chart.addDataDimensionItem( indicator );
        chart.setPeriods( periods );
        chart.getOrganisationUnits().add( unit );
        chart.setHideSubtitle( title );
        chart.setFormat( format );

        return getJFreeChart( chart );
    }

    @Override
    public JFreeChart getJFreeOrganisationUnitChart( Indicator indicator, OrganisationUnit parent, boolean title,
        I18nFormat format )
    {
        List<Period> periods = periodService.reloadPeriods(
            new RelativePeriods().setThisYear( true ).getRelativePeriods( format, true ) );

        Chart chart = new Chart();

        if ( title )
        {
            chart.setName( indicator.getName() );
        }

        chart.setType( ChartType.COLUMN );
        chart.setDimensions( DimensionalObject.DATA_X_DIM_ID, DimensionalObject.ORGUNIT_DIM_ID, DimensionalObject.PERIOD_DIM_ID );
        chart.setHideLegend( true );
        chart.addDataDimensionItem( indicator );
        chart.setPeriods( periods );
        chart.setOrganisationUnits( parent.getSortedChildren() );
        chart.setHideSubtitle( title );
        chart.setFormat( format );

        return getJFreeChart( chart );
    }

    @Override
    public JFreeChart getJFreeChart( String name, PlotOrientation orientation, CategoryLabelPositions labelPositions,
        Map<String, Double> categoryValues )
    {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        for ( Entry<String, Double> entry : categoryValues.entrySet() )
        {
            dataSet.addValue( entry.getValue(), name, entry.getKey() );
        }

        CategoryPlot plot = getCategoryPlot( dataSet, getBarRenderer(), orientation, labelPositions );

        JFreeChart jFreeChart = getBasicJFreeChart( plot );
        jFreeChart.setTitle( name );

        return jFreeChart;
    }

    @Override
    public JFreeChart getJFreeChartHistory( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        DataElementCategoryOptionCombo attributeOptionCombo, Period lastPeriod, OrganisationUnit organisationUnit,
        int historyLength, I18nFormat format )
    {
        lastPeriod = periodService.reloadPeriod( lastPeriod );

        List<Period> periods = periodService.getPeriods( lastPeriod, historyLength );

        MinMaxDataElement minMax = minMaxDataElementService.getMinMaxDataElement( organisationUnit, dataElement,
            categoryOptionCombo );

        UnivariateInterpolator interpolator = new SplineInterpolator();

        Integer periodCount = 0;
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        // ---------------------------------------------------------------------
        // DataValue, MinValue and MaxValue DataSets
        // ---------------------------------------------------------------------

        DefaultCategoryDataset dataValueDataSet = new DefaultCategoryDataset();
        DefaultCategoryDataset metaDataSet = new DefaultCategoryDataset();

        for ( Period period : periods )
        {
            ++periodCount;

            period.setName( format.formatPeriod( period ) );

            DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit,
                categoryOptionCombo, attributeOptionCombo );

            double value = 0;

            if ( dataValue != null && dataValue.getValue() != null && MathUtils.isNumeric( dataValue.getValue() ) )
            {
                value = Double.parseDouble( dataValue.getValue() );

                x.add( periodCount.doubleValue() );
                y.add( value );
            }

            dataValueDataSet.addValue( value, dataElement.getShortName(), period.getName() );

            if ( minMax != null )
            {
                metaDataSet.addValue( minMax.getMin(), "Min value", period.getName() );
                metaDataSet.addValue( minMax.getMax(), "Max value", period.getName() );
            }
        }

        // ---------------------------------------------------------------------
        // Interpolation DataSet
        // ---------------------------------------------------------------------

        if ( x.size() >= 3 ) // minimum 3 points required for interpolation
        {
            periodCount = 0;

            double[] xa = getArray( x );

            int min = MathUtils.getMin( xa ).intValue();
            int max = MathUtils.getMax( xa ).intValue();

            try
            {
                UnivariateFunction function = interpolator.interpolate( xa, getArray( y ) );

                for ( Period period : periods )
                {
                    if ( ++periodCount >= min && periodCount <= max )
                    {
                        metaDataSet.addValue( function.value( periodCount ), "Regression value", period.getName() );
                    }
                }
            }
            catch ( MathRuntimeException ex )
            {
                throw new RuntimeException( "Failed to interpolate", ex );
            }
        }

        // ---------------------------------------------------------------------
        // Plots
        // ---------------------------------------------------------------------

        CategoryPlot plot = getCategoryPlot( dataValueDataSet, getBarRenderer(), PlotOrientation.VERTICAL,
            CategoryLabelPositions.UP_45 );

        plot.setDataset( 1, metaDataSet );
        plot.setRenderer( 1, getLineRenderer() );

        JFreeChart jFreeChart = getBasicJFreeChart( plot );

        return jFreeChart;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a basic JFreeChart.
     */
    private JFreeChart getBasicJFreeChart( CategoryPlot plot )
    {
        JFreeChart jFreeChart = new JFreeChart( null, TITLE_FONT, plot, false );

        jFreeChart.setBackgroundPaint( Color.WHITE );
        jFreeChart.setAntiAlias( true );

        return jFreeChart;
    }

    /**
     * Returns a CategoryPlot.
     */
    private CategoryPlot getCategoryPlot( CategoryDataset dataSet, CategoryItemRenderer renderer,
        PlotOrientation orientation, CategoryLabelPositions labelPositions )
    {
        CategoryPlot plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), renderer );

        plot.setDatasetRenderingOrder( DatasetRenderingOrder.FORWARD );
        plot.setOrientation( orientation );

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions( labelPositions );

        return plot;
    }

    /**
     * Returns a bar renderer.
     */
    private BarRenderer getBarRenderer()
    {
        BarRenderer renderer = new BarRenderer();

        renderer.setMaximumBarWidth( 0.07 );

        for ( int i = 0; i < COLORS.length; i++ )
        {
            renderer.setSeriesPaint( i, COLORS[i] );
            renderer.setShadowVisible( false );
        }

        return renderer;
    }

    /**
     * Returns a line and shape renderer.
     */
    private LineAndShapeRenderer getLineRenderer()
    {
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();

        for ( int i = 0; i < COLORS.length; i++ )
        {
            renderer.setSeriesPaint( i, COLORS[i] );
        }

        return renderer;
    }

    /**
     * Returns a stacked bar renderer.
     */
    private StackedBarRenderer getStackedBarRenderer()
    {
        StackedBarRenderer renderer = new StackedBarRenderer();

        for ( int i = 0; i < COLORS.length; i++ )
        {
            renderer.setSeriesPaint( i, COLORS[i] );
            renderer.setShadowVisible( false );
        }

        return renderer;
    }

    /**
     * Returns a stacked area renderer.
     */
    private AreaRenderer getStackedAreaRenderer()
    {
        StackedAreaRenderer renderer = new StackedAreaRenderer();

        for ( int i = 0; i < COLORS.length; i++ )
        {
            renderer.setSeriesPaint( i, COLORS[i] );
        }

        return renderer;
    }

    /**
     * Returns a horizontal line marker for the given x value and label.
     */
    private Marker getMarker( Double value, String label )
    {
        Marker marker = new ValueMarker( value );
        marker.setPaint( Color.BLACK );
        marker.setStroke( new BasicStroke( 1.1f ) );
        marker.setLabel( label );
        marker.setLabelOffset( new RectangleInsets( -10, 50, 0, 0 ) );
        marker.setLabelFont( SUB_TITLE_FONT );

        return marker;
    }

    /**
     * Returns a JFreeChart of type defined in the chart argument.
     */
    private JFreeChart getJFreeChart( BaseChart chart )
    {
        final CategoryDataset[] dataSets = getCategoryDataSet( chart );
        final CategoryDataset dataSet = dataSets[0];

        final BarRenderer barRenderer = getBarRenderer();
        final LineAndShapeRenderer lineRenderer = getLineRenderer();

        // ---------------------------------------------------------------------
        // Plot
        // ---------------------------------------------------------------------

        CategoryPlot plot = null;

        if ( chart.isType( ChartType.LINE ) )
        {
            plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), lineRenderer );
            plot.setOrientation( PlotOrientation.VERTICAL );
        }
        else if ( chart.isType( ChartType.COLUMN ) )
        {
            plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), barRenderer );
            plot.setOrientation( PlotOrientation.VERTICAL );
        }
        else if ( chart.isType( ChartType.BAR ) )
        {
            plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), barRenderer );
            plot.setOrientation( PlotOrientation.HORIZONTAL );
        }
        else if ( chart.isType( ChartType.AREA ) )
        {
            return getStackedAreaChart( chart, dataSet );
        }
        else if ( chart.isType( ChartType.PIE ) )
        {
            return getMultiplePieChart( chart, dataSets );
        }
        else if ( chart.isType( ChartType.STACKED_COLUMN ) )
        {
            return getStackedBarChart( chart, dataSet, false );
        }
        else if ( chart.isType( ChartType.STACKED_BAR ) )
        {
            return getStackedBarChart( chart, dataSet, true );
        }
        else if ( chart.isType( ChartType.RADAR ) )
        {
            return getRadarChart( chart, dataSet );
        }
        else if ( chart.isType( ChartType.GAUGE ) )
        {
            Number number = dataSet.getValue( 0, 0 );
            ValueDataset valueDataSet = new DefaultValueDataset( number );

            return getGaugeChart( chart, valueDataSet );
        }
        else
        {
            throw new IllegalArgumentException( "Illegal or no chart type: " + chart.getType() );
        }

        if ( chart.isRegression() )
        {
            plot.setDataset( 1, dataSets[1] );
            plot.setRenderer( 1, lineRenderer );
        }

        JFreeChart jFreeChart = new JFreeChart( chart.getName(), TITLE_FONT, plot, !chart.isHideLegend() );

        setBasicConfig( jFreeChart, chart );

        if ( chart.isTargetLine() )
        {
            plot.addRangeMarker( getMarker( chart.getTargetLineValue(), chart.getTargetLineLabel() ) );
        }

        if ( chart.isBaseLine() )
        {
            plot.addRangeMarker( getMarker( chart.getBaseLineValue(), chart.getBaseLineLabel() ) );
        }

        if ( chart.isHideSubtitle() )
        {
            jFreeChart.addSubtitle( getSubTitle( chart ) );
        }

        plot.setDatasetRenderingOrder( DatasetRenderingOrder.FORWARD );

        // ---------------------------------------------------------------------
        // Category label positions
        // ---------------------------------------------------------------------

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        domainAxis.setLabel( chart.getDomainAxisLabel() );

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLabel( chart.getRangeAxisLabel() );

        return jFreeChart;
    }

    private JFreeChart getStackedAreaChart( BaseChart chart, CategoryDataset dataSet )
    {
        JFreeChart stackedAreaChart = ChartFactory.createStackedAreaChart( chart.getName(), chart.getDomainAxisLabel(),
            chart.getRangeAxisLabel(), dataSet, PlotOrientation.VERTICAL, !chart.isHideLegend(), false, false );

        setBasicConfig( stackedAreaChart, chart );

        CategoryPlot plot = (CategoryPlot) stackedAreaChart.getPlot();
        plot.setOrientation( PlotOrientation.VERTICAL );
        plot.setRenderer( getStackedAreaRenderer() );

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        xAxis.setLabelFont( LABEL_FONT );

        return stackedAreaChart;
    }

    private JFreeChart getRadarChart( BaseChart chart, CategoryDataset dataSet )
    {
        SpiderWebPlot plot = new SpiderWebPlot( dataSet, TableOrder.BY_ROW );
        plot.setLabelFont( LABEL_FONT );

        JFreeChart radarChart = new JFreeChart( chart.getName(), TITLE_FONT, plot, !chart.isHideLegend() );

        setBasicConfig( radarChart, chart );

        return radarChart;
    }

    private JFreeChart getStackedBarChart( BaseChart chart, CategoryDataset dataSet, boolean horizontal )
    {
        JFreeChart stackedBarChart = ChartFactory.createStackedBarChart( chart.getName(), chart.getDomainAxisLabel(),
            chart.getRangeAxisLabel(), dataSet, PlotOrientation.VERTICAL, !chart.isHideLegend(), false, false );

        setBasicConfig( stackedBarChart, chart );

        CategoryPlot plot = (CategoryPlot) stackedBarChart.getPlot();
        plot.setOrientation( horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL );
        plot.setRenderer( getStackedBarRenderer() );

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );

        return stackedBarChart;
    }

    private JFreeChart getMultiplePieChart( BaseChart chart, CategoryDataset[] dataSets )
    {
        JFreeChart multiplePieChart = ChartFactory.createMultiplePieChart( chart.getName(), dataSets[0], TableOrder.BY_ROW,
            !chart.isHideLegend(), false, false );

        setBasicConfig( multiplePieChart, chart );

        if ( multiplePieChart.getLegend() != null )
        {
            multiplePieChart.getLegend().setItemFont( SUB_TITLE_FONT );
        }

        MultiplePiePlot multiplePiePlot = (MultiplePiePlot) multiplePieChart.getPlot();
        JFreeChart pieChart = multiplePiePlot.getPieChart();
        pieChart.setBackgroundPaint( COLOR_TRANSPARENT );
        pieChart.getTitle().setFont( SUB_TITLE_FONT );

        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setBackgroundPaint( COLOR_TRANSPARENT );
        piePlot.setOutlinePaint( COLOR_TRANSPARENT );
        piePlot.setLabelFont( LABEL_FONT );
        piePlot.setLabelGenerator( new StandardPieSectionLabelGenerator( "{2}" ) );
        piePlot.setSimpleLabels( true );
        piePlot.setIgnoreZeroValues( true );
        piePlot.setIgnoreNullValues( true );
        piePlot.setShadowXOffset( 0d );
        piePlot.setShadowYOffset( 0d );

        for ( int i = 0; i < dataSets[0].getColumnCount(); i++ )
        {
            piePlot.setSectionPaint( dataSets[0].getColumnKey( i ), COLORS[(i % COLORS.length)] );
        }

        return multiplePieChart;
    }

    private JFreeChart getGaugeChart( BaseChart chart, ValueDataset dataSet )
    {
        MeterPlot meterPlot = new MeterPlot( dataSet );

        meterPlot.setUnits( "" );
        meterPlot.setRange( new Range( 0.0d, 100d ) );

        for ( int i = 0; i < 10; i++ )
        {
            double start = i * 10;
            double end = start + 10;
            String label = String.valueOf( start );

            meterPlot.addInterval( new MeterInterval( label, new Range( start, end ), COLOR_LIGHT_GRAY, null, COLOR_LIGHT_GRAY ) );
        }

        meterPlot.setMeterAngle(180);
        meterPlot.setDialBackgroundPaint( COLOR_LIGHT_GRAY );
        meterPlot.setDialShape( DialShape.CHORD );
        meterPlot.setNeedlePaint( COLORS[0] );
        meterPlot.setTickLabelsVisible( true );
        meterPlot.setTickLabelFont( LABEL_FONT );
        meterPlot.setTickLabelPaint( Color.BLACK );
        meterPlot.setTickPaint( COLOR_LIGHTER_GRAY );
        meterPlot.setValueFont( TITLE_FONT );
        meterPlot.setValuePaint( Color.BLACK );

        JFreeChart meterChart = new JFreeChart( chart.getName(), meterPlot );
        setBasicConfig( meterChart, chart );
        meterChart.removeLegend();

        return meterChart;
    }

    /**
     * Sets basic configuration including title font, subtitle, background paint and
     * anti-alias on the given JFreeChart.
     */
    private void setBasicConfig( JFreeChart jFreeChart, BaseChart chart)
    {
        jFreeChart.getTitle().setFont( TITLE_FONT );

        jFreeChart.setBackgroundPaint( COLOR_TRANSPARENT );
        jFreeChart.setAntiAlias( true );

        if ( !chart.isHideTitle() )
        {
            jFreeChart.addSubtitle( getSubTitle( chart ) );
        }

        Plot plot = jFreeChart.getPlot();
        plot.setBackgroundPaint( COLOR_TRANSPARENT );
        plot.setOutlinePaint( COLOR_TRANSPARENT );
    }

    private TextTitle getSubTitle( BaseChart chart )
    {
        TextTitle textTitle = new TextTitle();

        String title = chart.hasTitle() ? chart.getTitle() : chart.generateTitle();

        textTitle.setFont( SUB_TITLE_FONT );
        textTitle.setText( title );

        return textTitle;
    }

    private CategoryDataset[] getCategoryDataSet( BaseChart chart )
    {
        Map<String, Object> valueMap = new HashMap<>();

        if ( chart.isAnalyticsType( AnalyticsType.AGGREGATE ) )
        {
            valueMap = analyticsService.getAggregatedDataValueMapping( chart );
        }
        else if ( chart.isAnalyticsType( AnalyticsType.EVENT ) )
        {
            Grid grid = eventAnalyticsService.getAggregatedEventData( chart );

            chart.setDataItemGrid( grid );

            valueMap = GridUtils.getMetaValueMapping( grid, ( grid.getWidth() - 1 ) );
        }

        DefaultCategoryDataset regularDataSet = new DefaultCategoryDataset();
        DefaultCategoryDataset regressionDataSet = new DefaultCategoryDataset();

        SimpleRegression regression = new SimpleRegression();

        BaseAnalyticalObject.sortKeys( valueMap );

        List<NameableObject> seriez = new ArrayList<>( chart.series() );
        List<NameableObject> categories = new ArrayList<>( chart.category() );

        if ( chart.hasSortOrder() )
        {
            categories = getSortedCategories( categories, chart, valueMap );
        }

        for ( NameableObject series : seriez )
        {
            double categoryIndex = 0;

            for ( NameableObject category : categories )
            {
                categoryIndex++;

                String key = getKey( series, category, chart.getAnalyticsType() );

                Object object = valueMap.get( key );

                Number value = object != null && object instanceof Number ? (Number) object : null;

                regularDataSet.addValue( value, series.getShortName(), category.getShortName() );

                if ( chart.isRegression() && value != null && value instanceof Double && !MathUtils.isEqual( (Double) value, MathUtils.ZERO ) )
                {
                    regression.addData( categoryIndex, (Double) value );
                }
            }

            if ( chart.isRegression() ) // Period must be category
            {
                categoryIndex = 0;

                for ( NameableObject category : chart.category() )
                {
                    final double value = regression.predict( categoryIndex++ );

                    // Enough values must exist for regression

                    if ( !Double.isNaN( value ) )
                    {
                        regressionDataSet.addValue( value, TREND_PREFIX + series.getShortName(), category.getShortName() );
                    }
                }
            }
        }

        return new CategoryDataset[]{ regularDataSet, regressionDataSet };
    }

    /**
     * Creates a key based on the given input. Sorts the key on its components
     * to remove significance of column order.
     */
    private String getKey( NameableObject series, NameableObject category, AnalyticsType analyticsType )
    {
        String key = series.getUid() + DIMENSION_SEP + category.getUid();

        // Replace potential operand separator with dimension separator

        key = AnalyticsType.AGGREGATE.equals( analyticsType ) ? key.replace( DataElementOperand.SEPARATOR, DIMENSION_SEP ) : key;

        // TODO fix issue with keys including -.

        return BaseAnalyticalObject.sortKey( key );
    }

    /**
     * Returns a list of sorted nameable objects. Sorting is defined per the
     * corresponding value in the given value map.
     */
    private List<NameableObject> getSortedCategories( List<NameableObject> categories, BaseChart chart, Map<String, Object> valueMap )
    {
        NameableObject series = chart.series().get( 0 );

        int sortOrder = chart.getSortOrder();

        List<NumericSortWrapper<NameableObject>> list = new ArrayList<>();

        for ( NameableObject category : categories )
        {
            String key = getKey( series, category, chart.getAnalyticsType() );

            Object value = valueMap.get( key );

            if ( value != null && value instanceof Number )
            {
                list.add( new NumericSortWrapper<NameableObject>( category, (Double ) value, sortOrder ) );
            }
        }

        Collections.sort( list );

        return NumericSortWrapper.getObjectList( list );
    }

    // -------------------------------------------------------------------------
    // CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int addChart( Chart chart )
    {
        chartStore.save( chart );

        return chart.getId();
    }

    @Override
    public void updateChart( Chart chart )
    {
        chartStore.update( chart );
    }

    @Override
    public Chart getChart( int id )
    {
        return chartStore.get( id );
    }

    @Override
    public Chart getChart( String uid )
    {
        return chartStore.getByUid( uid );
    }

    @Override
    public Chart getChartNoAcl( String uid )
    {
        return chartStore.getByUidNoAcl( uid );
    }

    @Override
    public void deleteChart( Chart chart )
    {
        chartStore.delete( chart );
    }

    @Override
    public List<Chart> getAllCharts()
    {
        return chartStore.getAll();
    }

    @Override
    public Chart getChartByName( String name )
    {
        return chartStore.getByName( name );
    }

    @Override
    public int getChartCount()
    {
        return chartStore.getCount();
    }

    @Override
    public int getChartCountByName( String name )
    {
        return chartStore.getCountLikeName( name );
    }

    @Override
    public List<Chart> getChartsBetween( int first, int max )
    {
        return chartStore.getAllOrderedName( first, max );
    }

    @Override
    public List<Chart> getChartsBetweenByName( String name, int first, int max )
    {
        return chartStore.getAllLikeName( name, first, max );
    }
}
