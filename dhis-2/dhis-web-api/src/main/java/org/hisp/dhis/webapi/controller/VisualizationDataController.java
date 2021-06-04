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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;
import static org.springframework.beans.BeanUtils.copyProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.chart.Series;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.visualization.Axis;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@AllArgsConstructor
public class VisualizationDataController
{
    @NonNull
    private OrganisationUnitService organisationUnitService;

    @NonNull
    private ContextUtils contextUtils;

    @NonNull
    private VisualizationService visualizationService;

    @NonNull
    private ChartService chartService;

    @NonNull
    private DataElementService dataElementService;

    @NonNull
    private CategoryService categoryService;

    @NonNull
    private IndicatorService indicatorService;

    @NonNull
    private I18nManager i18nManager;

    // --------------------------------------------------------------------------
    // GET - ReportTable data
    // --------------------------------------------------------------------------

    @RequestMapping( value = "/reportTables/{uid}/data", method = RequestMethod.GET )
    public @ResponseBody Grid getReportTableData( @PathVariable( "uid" ) String uid, Model model,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date )
    {
        return getReportTableGrid( uid, organisationUnitUid, date );
    }

    @RequestMapping( value = "/reportTables/{uid}/data.html", method = RequestMethod.GET )
    public void getReportTableHtml( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".html";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = "/reportTables/{uid}/data.html+css", method = RequestMethod.GET )
    public void getReportTableHtmlCss( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".html";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @RequestMapping( value = "/reportTables/{uid}/data.xml", method = RequestMethod.GET )
    public void getReportTableXml( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".xml";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/reportTables/{uid}/data.pdf", method = RequestMethod.GET )
    public void getReportTablePdf( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".pdf";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/reportTables/{uid}/data.xls", method = RequestMethod.GET )
    public void getReportTableXls( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".xls";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, true );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/reportTables/{uid}/data.csv", method = RequestMethod.GET )
    public void getReportTableCsv( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".csv";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, true );

        GridUtils.toCsv( grid, response.getWriter() );
    }

    private Grid getReportTableGrid( String uid, String organisationUnitUid, Date date )
    {
        Visualization visualization = visualizationService.getVisualizationNoAcl( uid );

        if ( organisationUnitUid == null && visualization.hasReportingParams()
            && visualization.getReportingParams().isOrganisationUnitSet() )
        {
            organisationUnitUid = organisationUnitService.getRootOrganisationUnits().iterator().next().getUid();
        }

        date = date != null ? date : new Date();

        return visualizationService.getVisualizationGrid( uid, date, organisationUnitUid );
    }

    // --------------------------------------------------------------------------
    // GET Chart data
    // --------------------------------------------------------------------------

    @RequestMapping( value = { "/charts/{uid}/data", "/{uid}/data.png" }, method = RequestMethod.GET )
    public void getChart(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "date", required = false ) Date date,
        @RequestParam( value = "ou", required = false ) String ou,
        @RequestParam( value = "width", defaultValue = "800", required = false ) int width,
        @RequestParam( value = "height", defaultValue = "500", required = false ) int height,
        @RequestParam( value = "attachment", required = false ) boolean attachment,
        HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        final Visualization visualization = visualizationService.getVisualizationNoAcl( uid );
        final Chart chart = convertToChart( visualization );

        if ( chart == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Chart does not exist: " + uid ) );
        }

        OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit( ou ) : null;

        JFreeChart jFreeChart = chartService.getJFreeChart( chart, date, unit, i18nManager.getI18nFormat() );

        String filename = CodecUtils.filenameEncode( chart.getName() ) + ".png";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, attachment );

        ChartUtils.writeChartAsPNG( response.getOutputStream(), jFreeChart, width, height );
    }

    @RequestMapping( value = { "/charts/data", "/data.png" }, method = RequestMethod.GET )
    public void getChart(
        @RequestParam( value = "in" ) String indicatorUid,
        @RequestParam( value = "ou" ) String organisationUnitUid,
        @RequestParam( value = "periods", required = false ) boolean periods,
        @RequestParam( value = "width", defaultValue = "800", required = false ) int width,
        @RequestParam( value = "height", defaultValue = "500", required = false ) int height,
        @RequestParam( value = "skipTitle", required = false ) boolean skipTitle,
        @RequestParam( value = "attachment", required = false ) boolean attachment,
        HttpServletResponse response )
        throws IOException
    {
        Indicator indicator = indicatorService.getIndicator( indicatorUid );
        OrganisationUnit unit = organisationUnitService.getOrganisationUnit( organisationUnitUid );

        JFreeChart chart;

        if ( periods )
        {
            chart = chartService.getJFreePeriodChart( indicator, unit, !skipTitle, i18nManager.getI18nFormat() );
        }
        else
        {
            chart = chartService.getJFreeOrganisationUnitChart( indicator, unit, !skipTitle,
                i18nManager.getI18nFormat() );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "chart.png", attachment );

        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, width, height );
    }

    @RequestMapping( value = { "/charts/history/data", "/history/data.png" }, method = RequestMethod.GET )
    public void getHistoryChart(
        @RequestParam String de,
        @RequestParam String co,
        @RequestParam String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( defaultValue = "525", required = false ) int width,
        @RequestParam( defaultValue = "300", required = false ) int height,
        HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        DataElement dataElement = dataElementService.getDataElement( de );

        if ( dataElement == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data element does not exist: " + de ) );
        }

        CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo( co );

        if ( categoryOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Category option combo does not exist: " + co ) );
        }

        CategoryOptionCombo attributeOptionCombo = categoryService.getCategoryOptionCombo( cp );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Category option combo does not exist: " + cp ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period does not exist: " + pe ) );
        }

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit does not exist: " + ou ) );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "chart.png", false );

        JFreeChart chart = chartService.getJFreeChartHistory( dataElement, categoryOptionCombo, attributeOptionCombo,
            period, organisationUnit, 13, i18nManager.getI18nFormat() );

        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, width, height );
    }

    /**
     * This method converts a Visualization into a Chart. It's required only
     * during the merging process of Visualization. Even though this method
     * could be broken down into smaller ones is preferable to leave it
     * self-contained as this is just a temporary mapping.
     *
     * @param visualization
     * @return a Chart object from the given Visualization
     */
    @Deprecated
    private Chart convertToChart( final Visualization visualization )
    {
        final Chart chart = new Chart();

        if ( visualization != null )
        {
            copyProperties( visualization, chart, "type" );

            // Set the correct type
            if ( visualization.getType() != null && !"PIVOT_TABLE".equalsIgnoreCase( visualization.getType().name() ) )
            {
                chart.setType( ChartType.valueOf( visualization.getType().name() ) );
            }

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
        }
        return chart;
    }
}
