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

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.visualization.VisualizationType.PIVOT_TABLE;
import static org.hisp.dhis.visualization.VisualizationType.valueOf;
import static org.springframework.beans.BeanUtils.copyProperties;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.chart.Series;
import org.hisp.dhis.reporttable.ReportParams;
import org.hisp.dhis.reporttable.ReportTable;

/**
 * This is just a helper class responsible for converting from/to Visualization.
 * Such conversions are needed to keep backward compatibility across
 * ReportTable, Chart and Visualization. It's just a temporary class an should
 * be removed as soon as ReportTable and Chart are remove from the codebase.
 *
 * @author maikel arabori
 */
@Deprecated
public class ConversionHelper
{
    public static List<Chart> convertToChartList( List<Visualization> entities )
    {
        final List<Chart> charts = new ArrayList<>();

        if ( isNotEmpty( entities ) )
        {
            for ( final Visualization visualization : entities )
            {
                final Chart chart = new Chart();
                copyProperties( visualization, chart, "type" );

                // Consider only Visualization type that is a Chart
                if ( visualization.getType() != null )
                {
                    // Set the correct type
                    chart.setType( ChartType.valueOf( visualization.getType().name() ) );

                    // Copy seriesItems
                    if ( isNotEmpty( visualization.getOptionalAxes() ) )
                    {
                        final List<Series> seriesItems = new ArrayList<>();
                        final List<Axis> axes = visualization.getOptionalAxes();

                        for ( final Axis axis : axes )
                        {
                            final Series series = new Series();
                            series.setSeries( axis.getDimensionalItem() );
                            series.setAxis( axis.getAxis() );
                            series.setId( axis.getId() );

                            seriesItems.add( series );
                        }
                        chart.setSeriesItems( seriesItems );
                    }

                    // Copy column into series
                    if ( isNotEmpty( visualization.getColumnDimensions() ) )
                    {
                        final List<String> columns = visualization.getColumnDimensions();
                        chart.setSeries( columns.get( 0 ) );
                    }

                    // Copy rows into category
                    if ( isNotEmpty( visualization.getRowDimensions() ) )
                    {
                        final List<String> rows = visualization.getRowDimensions();
                        chart.setCategory( rows.get( 0 ) );
                    }

                    chart.setCumulativeValues( visualization.isCumulativeValues() );
                    charts.add( chart );
                }
            }
        }

        return charts;
    }

    public static Visualization convertToVisualization( final Chart chart )
    {
        final Visualization visualization = new Visualization();

        if ( chart != null )
        {
            copyProperties( chart, visualization );

            if ( chart.getType() != null )
            {
                visualization.setType( valueOf( chart.getType().name() ) );
            }

            // Copy seriesItems
            if ( isNotEmpty( chart.getSeriesItems() ) )
            {
                final List<Series> seriesItems = chart.getSeriesItems();
                final List<Axis> axes = visualization.getOptionalAxes();

                for ( final Series seriesItem : seriesItems )
                {
                    final Axis axis = new Axis();
                    axis.setDimensionalItem( seriesItem.getSeries() );
                    axis.setAxis( seriesItem.getAxis() );
                    axis.setId( seriesItem.getId() );

                    axes.add( axis );
                }
                visualization.setOptionalAxes( axes );
            }

            // Add series into columns
            if ( !StringUtils.isEmpty( chart.getSeries() ) )
            {
                if ( visualization.getColumnDimensions() != null )
                {
                    visualization.getColumnDimensions().add( chart.getSeries() );
                }
                else
                {
                    visualization.setColumnDimensions( asList( chart.getSeries() ) );
                }
            }

            // Add category into rows
            if ( !StringUtils.isEmpty( chart.getCategory() ) )
            {
                if ( visualization.getRowDimensions() != null )
                {
                    visualization.getRowDimensions().add( chart.getCategory() );
                }
                else
                {
                    visualization.setRowDimensions( asList( chart.getCategory() ) );
                }
            }

            visualization.setCumulativeValues( chart.isCumulativeValues() );
        }

        return visualization;
    }

    public static List<ReportTable> convertToReportTableList( List<Visualization> entities )
    {
        List<ReportTable> reportTables = new ArrayList<>();

        if ( isNotEmpty( entities ) )
        {
            for ( final Visualization visualization : entities )
            {
                if ( visualization.getType() != null )
                {
                    final ReportTable reportTable = new ReportTable();
                    copyProperties( visualization, reportTable );

                    // Copy report params
                    if ( visualization.hasReportingParams() )
                    {
                        final ReportingParams reportingParams = visualization.getReportingParams();
                        final ReportParams reportParams = new ReportParams();

                        reportParams
                            .setParamGrandParentOrganisationUnit( reportingParams.isGrandParentOrganisationUnit() );
                        reportParams.setParamOrganisationUnit( reportingParams.isOrganisationUnit() );
                        reportParams.setParamParentOrganisationUnit( reportingParams.isParentOrganisationUnit() );
                        reportParams.setParamReportingMonth( reportingParams.isReportingPeriod() );

                        reportTable.setReportParams( reportParams );
                    }

                    reportTables.add( reportTable );
                }
            }
        }

        return reportTables;
    }

    public static Visualization convertToVisualization( final ReportTable reportTable )
    {
        final Visualization visualization = new Visualization();

        if ( reportTable != null )
        {
            copyProperties( reportTable, visualization );
            visualization.setType( PIVOT_TABLE );

            // Copy report params
            if ( reportTable.hasReportParams() )
            {
                final ReportingParams reportingParams = new ReportingParams();
                final ReportParams reportParams = reportTable.getReportParams();

                reportingParams.setGrandParentOrganisationUnit( reportParams.isParamGrandParentOrganisationUnit() );
                reportingParams.setOrganisationUnit( reportParams.isParamOrganisationUnit() );
                reportingParams.setParentOrganisationUnit( reportParams.isParamParentOrganisationUnit() );
                reportingParams.setReportingPeriod( reportParams.isParamReportingMonth() );

                visualization.setReportingParams( reportingParams );
            }
        }

        return visualization;
    }
}
