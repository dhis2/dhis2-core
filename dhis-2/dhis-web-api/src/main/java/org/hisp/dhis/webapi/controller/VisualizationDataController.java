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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.visualization.ChartService;
import org.hisp.dhis.visualization.PlotData;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationGridService;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.Tags( "ui" )
@RestController
@RequiredArgsConstructor
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class VisualizationDataController
{
    @Nonnull
    private final OrganisationUnitService organisationUnitService;

    @Nonnull
    private final ContextUtils contextUtils;

    @Nonnull
    private final VisualizationService visualizationService;

    @Nonnull
    private final VisualizationGridService visualizationGridService;

    @Nonnull
    private final ChartService chartService;

    @Nonnull
    private final DataElementService dataElementService;

    @Nonnull
    private final CategoryService categoryService;

    @Nonnull
    private final IndicatorService indicatorService;

    @Nonnull
    private final I18nManager i18nManager;

    @Nonnull
    private final CurrentUserService currentUserService;

    @Nonnull
    private final RenderService renderService;

    @GetMapping( value = "/visualizations/{uid}/data.html" )
    public @ResponseBody Grid getVisualizationDataHtml( @PathVariable( "uid" ) String uid, Model model,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date )
    {
        return getVisualizationGrid( uid, organisationUnitUid, date );
    }

    @GetMapping( value = "/visualizations/{uid}/data.html+css" )
    public void getVisualizationDataHtmlCss( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getVisualizationGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".html";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @GetMapping( value = "/visualizations/{uid}/data.xml" )
    public void getVisualizationDataXml( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getVisualizationGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".xml";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @GetMapping( value = "/visualizations/{uid}/data.pdf" )
    public void getVisualizationDataPdf( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getVisualizationGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".pdf";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    @GetMapping( value = "/visualizations/{uid}/data.xls" )
    public void getVisualizationDataXls( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getVisualizationGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".xls";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, true );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @GetMapping( value = "/visualizations/{uid}/data.csv" )
    public void getVisualizationDataCsv( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response )
        throws Exception
    {
        Grid grid = getVisualizationGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".csv";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, true );

        GridUtils.toCsv( grid, response.getWriter() );
    }

    @GetMapping( value = { "/visualizations/{uid}/data", "/visualizations/{uid}/data.png" } )
    public void getVisualizationData(
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
        Visualization visualization = visualizationService.getVisualizationNoAcl( uid );

        if ( visualization == null )
        {
            throw new WebMessageException( notFound( "Visualization does not exist: " + uid ) );
        }

        if ( visualization.isChart() && ChartService.SUPPORTED_TYPES.contains( visualization.getType() ) )
        {
            OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit( ou ) : null;

            JFreeChart jFreeChart = chartService.getJFreeChart( new PlotData( visualization ), date, unit,
                i18nManager.getI18nFormat(), currentUserService.getCurrentUser() );

            String filename = CodecUtils.filenameEncode( visualization.getName() ) + ".png";

            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG,
                CacheStrategy.RESPECT_SYSTEM_SETTING,
                filename, attachment );

            ChartUtils.writeChartAsPNG( response.getOutputStream(), jFreeChart, width, height );
        }
        else
        {
            response.setContentType( CONTENT_TYPE_JSON );
            renderService.toJson( response.getOutputStream(), getVisualizationGrid( uid, ou, date ) );
        }
    }

    @GetMapping( value = { "/visualizations/data", "/visualizations/data.png" } )
    public void getVisualizationChartData(
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

    @GetMapping( value = { "/visualizations/history/data", "/visualizations/history/data.png" } )
    public void getVisualizationChartHistory(
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
            throw new WebMessageException( conflict( "Data element does not exist: " + de ) );
        }

        CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo( co );

        if ( categoryOptionCombo == null )
        {
            throw new WebMessageException( conflict( "Category option combo does not exist: " + co ) );
        }

        CategoryOptionCombo attributeOptionCombo = categoryService.getCategoryOptionCombo( cp );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( conflict( "Category option combo does not exist: " + cp ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( conflict( "Period does not exist: " + pe ) );
        }

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( conflict( "Organisation unit does not exist: " + ou ) );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING,
            "chart.png", false );

        JFreeChart chart = chartService.getJFreeChartHistory( dataElement, categoryOptionCombo, attributeOptionCombo,
            period, organisationUnit, 13, i18nManager.getI18nFormat() );

        ChartUtils.writeChartAsPNG( response.getOutputStream(), chart, width, height );
    }

    /**
     * Returns a visualization as a {@link Grid}.
     *
     * @param uid the visualization identifier.
     * @param organisationUnitUid the organisation unit identifier.
     * @param date the relative date.
     * @return a {@link Grid}.
     */
    private Grid getVisualizationGrid( String uid, String organisationUnitUid, Date date )
    {
        Visualization visualization = visualizationService.getVisualizationNoAcl( uid );

        if ( organisationUnitUid == null && visualization.hasReportingParams()
            && visualization.getReportingParams().isOrganisationUnitSet() )
        {
            organisationUnitUid = organisationUnitService.getRootOrganisationUnits().iterator().next().getUid();
        }

        date = ObjectUtils.firstNonNull( date, new Date() );

        return visualizationGridService.getVisualizationGrid( uid, date, organisationUnitUid );
    }
}
