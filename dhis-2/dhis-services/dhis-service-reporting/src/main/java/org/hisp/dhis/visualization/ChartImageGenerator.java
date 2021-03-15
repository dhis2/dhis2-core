/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.NumericSortWrapper;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChartImageGenerator
{

    private static final Font TITLE_FONT = new Font( Font.SANS_SERIF, Font.BOLD, 12 );

    private static final Font SUB_TITLE_FONT = new Font( Font.SANS_SERIF, Font.PLAIN, 11 );

    private static final Font LABEL_FONT = new Font( Font.SANS_SERIF, Font.PLAIN, 10 );

    private static final String TREND_PREFIX = "Trend - ";

    private static final Color[] COLORS = { Color.decode( "#88be3b" ), Color.decode( "#3b6286" ),
        Color.decode( "#b7404c" ), Color.decode( "#ff9f3a" ), Color.decode( "#968f8f" ), Color.decode( "#b7409f" ),
        Color.decode( "#ffda64" ), Color.decode( "#4fbdae" ), Color.decode( "#b78040" ), Color.decode( "#676767" ),
        Color.decode( "#6a33cf" ), Color.decode( "#4a7833" ) };

    private static final Color COLOR_LIGHT_GRAY = Color.decode( "#dddddd" );

    private static final Color COLOR_LIGHTER_GRAY = Color.decode( "#eeeeee" );

    private static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;

    private final AnalyticsService analyticsService;

    private final OrganisationUnitService organisationUnitService;

    private final CurrentUserService currentUserService;

    public ChartImageGenerator( final AnalyticsService analyticsService,
        final OrganisationUnitService organisationUnitService, final CurrentUserService currentUserService )
    {
        checkNotNull( analyticsService );
        checkNotNull( organisationUnitService );
        checkNotNull( currentUserService );

        this.analyticsService = analyticsService;
        this.organisationUnitService = organisationUnitService;
        this.currentUserService = currentUserService;
    }

    /**
     * Generates a JFreeChart.
     *
     * @param visualization the chart to use as basis for the JFreeChart
     *        generation.
     * @param date the date to use as basis for relative periods, can be null.
     * @param organisationUnit the org unit to use as basis for relative units,
     *        will override the current user org unit if set, can be null.
     * @param format the i18n format.
     * @param currentUser the current logged-in user.
     * @return a JFreeChart object.
     */
    @Transactional( readOnly = true )
    public JFreeChart getJFreeChart( final Visualization visualization, final Date date,
        OrganisationUnit organisationUnit, final I18nFormat format, final User currentUser )
    {
        User user = (currentUser != null ? currentUser : currentUserService.getCurrentUser());

        if ( organisationUnit == null && user != null )
        {
            organisationUnit = user.getOrganisationUnit();
        }

        List<OrganisationUnit> atLevels = new ArrayList<>();
        List<OrganisationUnit> inGroups = new ArrayList<>();

        if ( visualization.hasOrganisationUnitLevels() )
        {
            atLevels.addAll( organisationUnitService.getOrganisationUnitsAtLevels(
                visualization.getOrganisationUnitLevels(), visualization.getOrganisationUnits() ) );
        }

        if ( visualization.hasItemOrganisationUnitGroups() )
        {
            inGroups.addAll( organisationUnitService.getOrganisationUnits(
                visualization.getItemOrganisationUnitGroups(), visualization.getOrganisationUnits() ) );
        }

        visualization.init( user, date, organisationUnit, atLevels, inGroups, format );

        JFreeChart resultChart = getJFreeChart( visualization );

        visualization.clearTransientState();

        return resultChart;
    }

    /**
     * Returns a JFreeChart of type defined in the chart argument.
     */
    private JFreeChart getJFreeChart( final Visualization visualization )
    {
        final CategoryDataset[] dataSets = getCategoryDataSet( visualization );
        final CategoryDataset dataSet = dataSets[0];

        final BarRenderer barRenderer = getBarRenderer();
        final LineAndShapeRenderer lineRenderer = getLineRenderer();

        // ---------------------------------------------------------------------
        // Plot
        // ---------------------------------------------------------------------

        CategoryPlot plot;

        if ( visualization.isType( VisualizationType.LINE ) )
        {
            plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), lineRenderer );
            plot.setOrientation( PlotOrientation.VERTICAL );
        }
        else if ( visualization.isType( VisualizationType.COLUMN ) )
        {
            plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), barRenderer );
            plot.setOrientation( PlotOrientation.VERTICAL );
        }
        else if ( visualization.isType( VisualizationType.BAR ) )
        {
            plot = new CategoryPlot( dataSet, new CategoryAxis(), new NumberAxis(), barRenderer );
            plot.setOrientation( PlotOrientation.HORIZONTAL );
        }
        else if ( visualization.isType( VisualizationType.AREA ) )
        {
            return getStackedAreaChart( visualization, dataSet );
        }
        else if ( visualization.isType( VisualizationType.PIE ) )
        {
            return getMultiplePieChart( visualization, dataSets );
        }
        else if ( visualization.isType( VisualizationType.STACKED_COLUMN ) )
        {
            return getStackedBarChart( visualization, dataSet, false );
        }
        else if ( visualization.isType( VisualizationType.STACKED_BAR ) )
        {
            return getStackedBarChart( visualization, dataSet, true );
        }
        else if ( visualization.isType( VisualizationType.RADAR ) )
        {
            return getRadarChart( visualization, dataSet );
        }
        else if ( visualization.isType( VisualizationType.GAUGE ) )
        {
            Number number = dataSet.getValue( 0, 0 );
            ValueDataset valueDataSet = new DefaultValueDataset( number );

            return getGaugeChart( visualization, valueDataSet );
        }
        else
        {
            throw new IllegalArgumentException( "Illegal or no chart type: " + visualization.getType() );
        }

        if ( visualization.isRegression() )
        {
            plot.setDataset( 1, dataSets[1] );
            plot.setRenderer( 1, lineRenderer );
        }

        JFreeChart jFreeChart = new JFreeChart( visualization.getName(), TITLE_FONT, plot,
            !visualization.isHideLegend() );

        setBasicConfig( jFreeChart, visualization );

        if ( visualization.isTargetLine() )
        {
            plot.addRangeMarker( getMarker( visualization.getTargetLineValue(), visualization.getTargetLineLabel() ) );
        }

        if ( visualization.isBaseLine() )
        {
            plot.addRangeMarker( getMarker( visualization.getBaseLineValue(), visualization.getBaseLineLabel() ) );
        }

        if ( visualization.isHideSubtitle() )
        {
            jFreeChart.addSubtitle( getSubTitle( visualization ) );
        }

        plot.setDatasetRenderingOrder( DatasetRenderingOrder.FORWARD );

        // ---------------------------------------------------------------------
        // Category label positions
        // ---------------------------------------------------------------------

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        domainAxis.setLabel( visualization.getDomainAxisLabel() );

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLabel( visualization.getRangeAxisLabel() );

        return jFreeChart;
    }

    private JFreeChart getStackedAreaChart( final Visualization visualization, CategoryDataset dataSet )
    {
        JFreeChart stackedAreaChart = ChartFactory.createStackedAreaChart( visualization.getName(),
            visualization.getDomainAxisLabel(),
            visualization.getRangeAxisLabel(), dataSet, PlotOrientation.VERTICAL, !visualization.isHideLegend(), false,
            false );

        setBasicConfig( stackedAreaChart, visualization );

        CategoryPlot plot = (CategoryPlot) stackedAreaChart.getPlot();
        plot.setOrientation( PlotOrientation.VERTICAL );
        plot.setRenderer( getStackedAreaRenderer() );

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );
        xAxis.setLabelFont( LABEL_FONT );

        return stackedAreaChart;
    }

    private JFreeChart getRadarChart( final Visualization visualization, CategoryDataset dataSet )
    {
        SpiderWebPlot plot = new SpiderWebPlot( dataSet, TableOrder.BY_ROW );
        plot.setLabelFont( LABEL_FONT );

        JFreeChart radarChart = new JFreeChart( visualization.getName(), TITLE_FONT, plot,
            !visualization.isHideLegend() );

        setBasicConfig( radarChart, visualization );

        return radarChart;
    }

    private JFreeChart getStackedBarChart( final Visualization visualization, CategoryDataset dataSet,
        boolean horizontal )
    {
        JFreeChart stackedBarChart = ChartFactory.createStackedBarChart( visualization.getName(),
            visualization.getDomainAxisLabel(),
            visualization.getRangeAxisLabel(), dataSet, PlotOrientation.VERTICAL, !visualization.isHideLegend(), false,
            false );

        setBasicConfig( stackedBarChart, visualization );

        CategoryPlot plot = (CategoryPlot) stackedBarChart.getPlot();
        plot.setOrientation( horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL );
        plot.setRenderer( getStackedBarRenderer() );

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_45 );

        return stackedBarChart;
    }

    private JFreeChart getMultiplePieChart( final Visualization visualization, CategoryDataset[] dataSets )
    {
        JFreeChart multiplePieChart = ChartFactory.createMultiplePieChart( visualization.getName(), dataSets[0],
            TableOrder.BY_ROW,
            !visualization.isHideLegend(), false, false );

        setBasicConfig( multiplePieChart, visualization );

        if ( multiplePieChart.getLegend() != null )
        {
            multiplePieChart.getLegend().setItemFont( SUB_TITLE_FONT );
        }

        MultiplePiePlot multiplePiePlot = (MultiplePiePlot) multiplePieChart.getPlot();
        JFreeChart pieChart = multiplePiePlot.getPieChart();
        pieChart.setBackgroundPaint( DEFAULT_BACKGROUND_COLOR );
        pieChart.getTitle().setFont( SUB_TITLE_FONT );

        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setBackgroundPaint( DEFAULT_BACKGROUND_COLOR );
        piePlot.setOutlinePaint( DEFAULT_BACKGROUND_COLOR );
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

    private JFreeChart getGaugeChart( final Visualization visualization, ValueDataset dataSet )
    {
        MeterPlot meterPlot = new MeterPlot( dataSet );

        meterPlot.setUnits( "" );
        meterPlot.setRange( new Range( 0.0d, 100d ) );

        for ( int i = 0; i < 10; i++ )
        {
            double start = i * 10d;
            double end = start + 10d;
            String label = String.valueOf( start );

            meterPlot.addInterval(
                new MeterInterval( label, new Range( start, end ), COLOR_LIGHT_GRAY, null, COLOR_LIGHT_GRAY ) );
        }

        meterPlot.setMeterAngle( 180 );
        meterPlot.setDialBackgroundPaint( COLOR_LIGHT_GRAY );
        meterPlot.setDialShape( DialShape.CHORD );
        meterPlot.setNeedlePaint( COLORS[0] );
        meterPlot.setTickLabelsVisible( true );
        meterPlot.setTickLabelFont( LABEL_FONT );
        meterPlot.setTickLabelPaint( Color.BLACK );
        meterPlot.setTickPaint( COLOR_LIGHTER_GRAY );
        meterPlot.setValueFont( TITLE_FONT );
        meterPlot.setValuePaint( Color.BLACK );

        JFreeChart meterChart = new JFreeChart( visualization.getName(), meterPlot );
        setBasicConfig( meterChart, visualization );
        meterChart.removeLegend();

        return meterChart;
    }

    /**
     * Sets basic configuration including title font, subtitle, background paint
     * and anti-alias on the given JFreeChart.
     */
    private void setBasicConfig( JFreeChart jFreeChart, final Visualization visualization )
    {
        jFreeChart.getTitle().setFont( TITLE_FONT );

        jFreeChart.setBackgroundPaint( DEFAULT_BACKGROUND_COLOR );
        jFreeChart.setAntiAlias( true );

        if ( !visualization.isHideTitle() )
        {
            jFreeChart.addSubtitle( getSubTitle( visualization ) );
        }

        Plot plot = jFreeChart.getPlot();
        plot.setBackgroundPaint( DEFAULT_BACKGROUND_COLOR );
        plot.setOutlinePaint( DEFAULT_BACKGROUND_COLOR );
    }

    private TextTitle getSubTitle( final Visualization visualization )
    {
        TextTitle textTitle = new TextTitle();

        String title = visualization.hasTitle() ? visualization.getTitle() : visualization.generateTitle();

        textTitle.setFont( SUB_TITLE_FONT );
        textTitle.setText( title );

        return textTitle;
    }

    private CategoryDataset[] getCategoryDataSet( final Visualization visualization )
    {
        Map<String, Object> valueMap = analyticsService.getAggregatedDataValueMapping( visualization );

        DefaultCategoryDataset regularDataSet = new DefaultCategoryDataset();
        DefaultCategoryDataset regressionDataSet = new DefaultCategoryDataset();

        SimpleRegression regression = new SimpleRegression();

        valueMap = DimensionalObjectUtils.getSortedKeysMap( valueMap );

        List<NameableObject> seriez = new ArrayList<>( visualization.chartSeries() );
        List<NameableObject> categories = new ArrayList<>(
            defaultIfNull( visualization.chartCategory(), emptyList() ) );

        if ( visualization.hasSortOrder() )
        {
            categories = getSortedCategories( categories, visualization, valueMap );
        }

        for ( NameableObject series : seriez )
        {
            double categoryIndex = 0;

            for ( NameableObject category : categories )
            {
                categoryIndex++;

                String key = getKey( series, category, AnalyticsType.AGGREGATE );

                Object object = valueMap.get( key );

                Number value = object != null && object instanceof Number ? (Number) object : null;

                if ( value != null )
                {
                    regularDataSet.addValue( value, series.getShortName(), category.getShortName() );
                }

                if ( visualization.isRegression() && value != null && value instanceof Double
                    && !MathUtils.isEqual( (Double) value, MathUtils.ZERO ) )
                {
                    regression.addData( categoryIndex, (Double) value );
                }
            }

            if ( visualization.isRegression() ) // Period must be category
            {
                categoryIndex = 0;

                for ( NameableObject category : visualization.getRows() )
                {
                    final double value = regression.predict( categoryIndex++ );

                    // Enough values must exist for regression

                    if ( !Double.isNaN( value ) )
                    {
                        regressionDataSet.addValue( value, TREND_PREFIX + series.getShortName(),
                            category.getShortName() );
                    }
                }
            }
        }

        return new CategoryDataset[] { regularDataSet, regressionDataSet };
    }

    /**
     * Creates a key based on the given input. Sorts the key on its components
     * to remove significance of column order.
     */
    private String getKey( NameableObject series, NameableObject category, AnalyticsType analyticsType )
    {
        String key = series.getUid() + DIMENSION_SEP + category.getUid();

        // Replace potential operand separator with dimension separator

        key = AnalyticsType.AGGREGATE.equals( analyticsType )
            ? key.replace( DataElementOperand.SEPARATOR, DIMENSION_SEP )
            : key;

        // TODO fix issue with keys including -.

        return DimensionalObjectUtils.sortKey( key );
    }

    /**
     * Returns a list of sorted nameable objects. Sorting is defined per the
     * corresponding value in the given value map.
     */
    private List<NameableObject> getSortedCategories( List<NameableObject> categories,
        final Visualization visualization, Map<String, Object> valueMap )
    {
        NameableObject series = visualization.getColumns().get( 0 );

        int sortOrder = visualization.getSortOrder();

        List<NumericSortWrapper<NameableObject>> list = new ArrayList<>();

        for ( NameableObject category : categories )
        {
            String key = getKey( series, category, AnalyticsType.AGGREGATE );

            Object value = valueMap.get( key );

            if ( value instanceof Number )
            {
                list.add( new NumericSortWrapper<>( category, (Double) value, sortOrder ) );
            }
        }

        Collections.sort( list );

        return NumericSortWrapper.getObjectList( list );
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
}
